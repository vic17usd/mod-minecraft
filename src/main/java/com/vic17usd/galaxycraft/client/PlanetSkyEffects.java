package com.vic17usd.galaxycraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.vic17usd.galaxycraft.astronomy.*;
import com.vic17usd.galaxycraft.biosphere.LivingProfiles;
import com.vic17usd.galaxycraft.expansion.ModExpansion;
import com.vic17usd.galaxycraft.morphology.ModMorphology;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import com.vic17usd.galaxycraft.station.OrbitStations;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Cubemap direccional y discos esféricos iluminados por el Sol, sin modificar la luz del mundo. */
public class PlanetSkyEffects extends DimensionSpecialEffects {
  private final LivingProfiles.Profile profile;
  private final boolean orbital;

  public PlanetSkyEffects(LivingProfiles.Profile profile) {
    this(profile, false);
  }

  protected PlanetSkyEffects(LivingProfiles.Profile profile, boolean orbital) {
    super(Float.NaN, false, SkyType.NORMAL, false, false);
    this.profile = profile;
    this.orbital = orbital;
  }

  @Override
  public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
    return orbital
        ? Vec3.ZERO
        : new Vec3(
                (profile.fog() >> 16 & 255) / 255.0,
                (profile.fog() >> 8 & 255) / 255.0,
                (profile.fog() & 255) / 255.0)
            .scale(.15 + .85 * brightness);
  }

  @Override
  public boolean isFoggyAt(int x, int z) {
    return !orbital && profile.body().equals("venus");
  }

  @Override
  public boolean renderClouds(
      ClientLevel level,
      int ticks,
      float partial,
      PoseStack pose,
      double x,
      double y,
      double z,
      Matrix4f view,
      Matrix4f projection) {
    return true;
  }

  private record Seen(OrbitalMechanics.Orbit orbit, Vec3 position, double distance) {}

  @Override
  public boolean renderSky(
      ClientLevel level,
      int ticks,
      float partial,
      Matrix4f view,
      Camera camera,
      Matrix4f projection,
      boolean foggy,
      Runnable setupFog) {
    setupFog.run();
    if (camera.getFluidInCamera() != FogType.NONE) return true;
    if (camera.getEntity() instanceof LivingEntity living
        && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)))
      return true;
    String observer = orbital ? OrbitStations.parent(level).id() : profile.body();
    var player = Minecraft.getInstance().player;
    double days =
        player != null && player.hasData(ModMorphology.CLOCK)
            ? player.getData(ModMorphology.CLOCK).at(level.getGameTime() + partial)
            : (level.getDayTime() + partial) / 24000;
    var eye = camera.getPosition();
    double altitude = orbital ? 400 + eye.y / 1000 : Math.max(0, eye.y - 64) / 1000;
    Vec3 origin =
        OrbitalMechanics.position(observer, days)
            .add(OrbitalMechanics.surfaceOffset(observer, days, eye.x, eye.z, altitude * 1000));
    Vec3 toSun = origin.scale(-1).normalize(),
        sunLocal = OrbitalMechanics.localDirection(observer, toSun, days, eye.x, eye.z);
    RenderSystem.depthMask(false);
    RenderSystem.disableCull();
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.setShaderColor(1, 1, 1, 1);
    try {
      cube(view, observer, days, eye, sunLocal, altitude * 1000);
      var visible = new ArrayList<Seen>();
      for (var target : OrbitalMechanics.ALL)
        if (orbital || !target.id().equals(observer)) {
          Vec3 position = OrbitalMechanics.position(target.id(), days);
          double distance = position.distanceTo(origin);
          if (distance > target.radius()) visible.add(new Seen(target, position, distance));
        }
      visible.sort(Comparator.comparingDouble(Seen::distance).reversed());
      for (var target : visible) {
        Vec3 direction = target.position().subtract(origin).normalize(),
            local = OrbitalMechanics.localDirection(observer, direction, days, eye.x, eye.z);
        double angle =
            OrbitalMechanics.angularRadius(target.orbit().radius(), target.distance())
                * SkySettings.MAGNIFICATION.get();
        if (!orbital && local.y < -.025 - Math.sin(angle)) continue;
        if (angle < .000018) continue;
        float opacity = transmission(observer, local.y, orbital);
        if (opacity < .002F) continue;
        disk(
            view,
            observer,
            days,
            eye,
            target,
            origin,
            local,
            Math.min(1.45, angle),
            opacity,
            false);
        if (target.orbit().id().equals("earth"))
          disk(
              view,
              observer,
              days,
              eye,
              target,
              origin,
              local,
              Math.min(1.45, angle),
              opacity,
              true);
        if (target.orbit().id().equals("saturn"))
          rings(view, observer, days, eye, target, origin, opacity);
      }
      int weather = player == null ? 0 : player.getData(ModExpansion.EXPOSURE).weather;
      streaks(
          view,
          level.getGameTime() + partial,
          weather == 2 ? 9 : 1,
          profile == null ? 0 : profile.seed());
      if (weather == 3 && sunLocal.y > -.02)
        flare(
            view,
            sunLocal,
            (float) (level.getGameTime() + partial),
            transmission(observer, sunLocal.y, orbital));
    } finally {
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
      setupFog.run();
    }
    return true;
  }

  private void cube(
      Matrix4f view, String observer, double days, Vec3 eye, Vec3 sun, double height) {
    float[][][] faces = {
      {{-1, -1, -1}, {-1, -1, 1}, {-1, 1, 1}, {-1, 1, -1}},
      {{1, -1, 1}, {1, -1, -1}, {1, 1, -1}, {1, 1, 1}},
      {{-1, -1, -1}, {1, -1, -1}, {1, -1, 1}, {-1, -1, 1}},
      {{-1, 1, 1}, {1, 1, 1}, {1, 1, -1}, {-1, 1, -1}},
      {{1, -1, -1}, {-1, -1, -1}, {-1, 1, -1}, {1, 1, -1}},
      {{-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}}
    };
    var shader = AtmosphereShader.prepare(observer, sun, height, orbital);
    for (int i = 0; i < 6; i++) {
      RenderSystem.setShader(
          shader == null ? GameRenderer::getPositionTexColorShader : () -> shader);
      RenderSystem.setShaderTexture(
          0, ModPresentation.id("textures/astronomy/deep_space_" + i + ".png"));
      var buffer =
          Tesselator.getInstance()
              .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      int vertex = 0;
      for (var p : faces[i]) {
        Vec3 d =
            OrbitalMechanics.localDirection(
                observer, new Vec3(p[0], p[1], p[2]), days, eye.x, eye.z);
        var point =
            buffer
                .addVertex(view, (float) d.x * 90, (float) d.y * 90, (float) d.z * 90)
                .setUv(vertex == 1 || vertex == 2 ? 1 : 0, vertex < 2 ? 1 : 0);
        if (shader != null)
          point.setColor(
              (float) (d.x / Math.sqrt(3) + 1) * .5F,
              (float) (d.y / Math.sqrt(3) + 1) * .5F,
              (float) (d.z / Math.sqrt(3) + 1) * .5F,
              1);
        else point.setColor(1, 1, 1, 1);
        vertex++;
      }
      BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
  }

  private static float transmission(String body, double elevation, boolean orbit) {
    if (orbit) return 1;
    var medium = AtmosphereOptics.forBody(body);
    double path = medium.mieScale() / Math.max(.035, elevation + .04);
    return (float) AtmosphereOptics.transmittance(medium, path).length() / 1.7320508F;
  }

  private static void disk(
      Matrix4f view,
      String observer,
      double days,
      Vec3 eye,
      Seen target,
      Vec3 origin,
      Vec3 local,
      double angle,
      float opacity,
      boolean night) {
    boolean sun = target.orbit().id().equals("sun");
    Vec3 toward = origin.subtract(target.position()).normalize(),
        axis = Math.abs(toward.y) > .95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0),
        right = toward.cross(axis).normalize(),
        up = right.cross(toward).normalize();
    Vec3 light = target.position().scale(-1).normalize();
    Vec3 localRight = OrbitalMechanics.localDirection(observer, right, days, eye.x, eye.z),
        localUp = OrbitalMechanics.localDirection(observer, up, days, eye.x, eye.z);
    double radius = 90 * Math.tan(angle);
    String texture = target.orbit().id() + (night ? "_night" : "");
    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    RenderSystem.setShaderTexture(0, ModPresentation.id("textures/astronomy/" + texture + ".png"));
    var buffer =
        Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
    int rings = 12, sectors = 48;
    for (int ring = 0; ring < rings; ring++)
      for (int sector = 0; sector < sectors; sector++) {
        float[] us = new float[4], vs = new float[4], brightness = new float[4];
        Vec3[] points = new Vec3[4];
        for (int c = 0; c < 4; c++) {
          double r = (ring + (c == 1 || c == 2 ? 1 : 0)) / (double) rings,
              a = (sector + (c >= 2 ? 1 : 0)) * Math.PI * 2 / sectors,
              x = r * Math.cos(a),
              y = r * Math.sin(a);
          Vec3 normal =
              toward.scale(Math.sqrt(Math.max(0, 1 - r * r))).add(right.scale(x)).add(up.scale(y));
          Vec3 fixed =
              OrbitalMechanics.rotateY(
                  OrbitalMechanics.rotateX(normal, -Math.toRadians(target.orbit().tilt())),
                  -OrbitalMechanics.spin(target.orbit(), days));
          us[c] = (float) (Math.atan2(fixed.z, fixed.x) / (Math.PI * 2) + .5);
          vs[c] = (float) (Math.acos(Math.clamp(fixed.y, -1, 1)) / Math.PI);
          double lambert = Math.max(0, normal.dot(light));
          brightness[c] =
              (float)
                  (sun
                      ? 1
                      : night ? Math.pow(1 - lambert, 5) : .006 + .994 * Math.pow(lambert, .75));
          points[c] =
              local.scale(90).add(localRight.scale(x * radius)).add(localUp.scale(y * radius));
        }
        float min = 1, max = 0;
        for (float u : us) {
          min = Math.min(min, u);
          max = Math.max(max, u);
        }
        if (max - min > .5) for (int c = 0; c < 4; c++) if (us[c] < .5) us[c] += 1;
        for (int c = 0; c < 4; c++)
          buffer
              .addVertex(view, (float) points[c].x, (float) points[c].y, (float) points[c].z)
              .setUv(us[c], vs[c])
              .setColor(brightness[c], brightness[c], brightness[c], opacity);
      }
    BufferUploader.drawWithShader(buffer.buildOrThrow());
  }

  private static void rings(
      Matrix4f view,
      String observer,
      double days,
      Vec3 eye,
      Seen target,
      Vec3 origin,
      float opacity) {
    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    RenderSystem.setShaderTexture(0, ModPresentation.id("textures/astronomy/rings.png"));
    var buffer =
        Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
    boolean any = false;
    for (int sector = 0; sector < 128; sector++) {
      double mid = (sector + .5) * Math.PI * 2 / 128;
      Vec3 midPoint =
          target
              .position()
              .add(
                  OrbitalMechanics.rotateX(
                      new Vec3(Math.cos(mid), 0, Math.sin(mid))
                          .scale(target.orbit().radius() * 1.8),
                      Math.toRadians(target.orbit().tilt())));
      Vec3 ray = midPoint.subtract(origin), center = target.position().subtract(origin);
      double closest = center.dot(ray.normalize());
      if (closest > 0
          && closest < ray.length()
          && center.subtract(ray.normalize().scale(closest)).length() < target.orbit().radius())
        continue;
      for (int c = 0; c < 4; c++) {
        double radius = target.orbit().radius() * (c == 1 || c == 2 ? 2.3 : 1.25),
            a = (sector + (c >= 2 ? 1 : 0)) * Math.PI * 2 / 128;
        Vec3 position =
            target
                .position()
                .add(
                    OrbitalMechanics.rotateX(
                        new Vec3(Math.cos(a) * radius, 0, Math.sin(a) * radius),
                        Math.toRadians(target.orbit().tilt())));
        Vec3 local =
            OrbitalMechanics.localDirection(
                    observer, position.subtract(origin).normalize(), days, eye.x, eye.z)
                .scale(90);
        buffer
            .addVertex(view, (float) local.x, (float) local.y, (float) local.z)
            .setUv(c == 1 || c == 2 ? 1 : 0, c >= 2 ? 1 : 0)
            .setColor(.7F, .65F, .5F, opacity);
        any = true;
      }
    }
    if (any) BufferUploader.drawWithShader(buffer.buildOrThrow());
    else {
      var empty = buffer.build();
      if (empty != null) empty.close();
    }
  }

  /**
   * Estelas cosméticas: los impactos y recompensas pertenecen al sistema meteorológico servidor.
   */
  private static void streaks(Matrix4f view, double time, int count, int seed) {
    double cycle = time % 400;
    if (cycle > 25) return;
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    var buffer =
        Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    for (int i = 0; i < count; i++) {
      double a = (seed + i * 2.4 + Math.floor(time / 400) * 1.7);
      Vec3 start = new Vec3(Math.cos(a) * 70, 35 + i * 3, Math.sin(a) * 70),
          end = start.add(cycle * .6, -cycle * .23, 0),
          tail = end.add(-4, 1.5, 0);
      for (var v : List.of(tail, end, end.add(0, .06, .03), tail.add(0, .06, .03)))
        buffer
            .addVertex(view, (float) v.x, (float) v.y, (float) v.z)
            .setColor(.7F, .85F, 1, (float) Math.sin(cycle / 25 * Math.PI) * .65F);
    }
    BufferUploader.drawWithShader(buffer.buildOrThrow());
  }

  private static void flare(Matrix4f view, Vec3 sun, float time, float opacity) {
    Vec3 right = sun.cross(new Vec3(0, 1, 0)).normalize(), up = right.cross(sun).normalize();
    if (right.lengthSqr() < .01) return;
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    var buffer =
        Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    double size = 1 + Math.sin(time * .07) * .3;
    for (int i = 0; i < 16; i++) {
      double a = i * Math.PI / 8;
      Vec3 radial = right.scale(Math.cos(a)).add(up.scale(Math.sin(a))),
          tangent = right.scale(-Math.sin(a)).add(up.scale(Math.cos(a)));
      for (var p :
          List.of(
              sun.scale(88).add(radial.scale(size)),
              sun.scale(88).add(radial.scale(size * 2)),
              sun.scale(88).add(radial.scale(size * 2)).add(tangent.scale(.035)),
              sun.scale(88).add(radial.scale(size)).add(tangent.scale(.08))))
        buffer
            .addVertex(view, (float) p.x, (float) p.y, (float) p.z)
            .setColor(1, .7F, .25F, opacity * .12F);
    }
    BufferUploader.drawWithShader(buffer.buildOrThrow());
  }
}
