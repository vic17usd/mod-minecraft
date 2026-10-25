package com.vic17usd.galaxycraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.vic17usd.galaxycraft.biosphere.*;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Cielo procedural original, estrellas diurnas y discos de vecinos; no modifica tiempo ni física.
 */
public final class PlanetSkyEffects extends DimensionSpecialEffects {
  private final LivingProfiles.Profile profile;

  public PlanetSkyEffects(LivingProfiles.Profile profile) {
    super(Float.NaN, false, SkyType.NORMAL, false, false);
    this.profile = profile;
  }

  @Override
  public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
    return new Vec3(
            (profile.fog() >> 16 & 255) / 255.0,
            (profile.fog() >> 8 & 255) / 255.0,
            (profile.fog() & 255) / 255.0)
        .scale(.2 + .8 * brightness);
  }

  @Override
  public boolean isFoggyAt(int x, int z) {
    return profile.body().equals("venus");
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
    float day = (float) Math.max(0, Math.cos(level.getTimeOfDay(partial) * Math.PI * 2)),
        brightness = .12F + day * .88F;
    var matrix = new Matrix4f(view);
    float r = (profile.sky() >> 16 & 255) / 255F * brightness,
        g = (profile.sky() >> 8 & 255) / 255F * brightness,
        b = (profile.sky() & 255) / 255F * brightness;
    RenderSystem.depthMask(false);
    RenderSystem.disableCull();
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.setShaderColor(1, 1, 1, 1);
    try {
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      var vertices =
          Tesselator.getInstance()
              .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      float[][][] faces = {
        {{-96, -96, -96}, {96, -96, -96}, {96, 96, -96}, {-96, 96, -96}},
        {{96, -96, 96}, {-96, -96, 96}, {-96, 96, 96}, {96, 96, 96}},
        {{-96, -96, 96}, {-96, -96, -96}, {-96, 96, -96}, {-96, 96, 96}},
        {{96, -96, -96}, {96, -96, 96}, {96, 96, 96}, {96, 96, -96}},
        {{-96, 96, -96}, {96, 96, -96}, {96, 96, 96}, {-96, 96, 96}},
        {{-96, -96, 96}, {96, -96, 96}, {96, -96, -96}, {-96, -96, -96}}
      };
      for (var face : faces)
        for (var point : face) {
          float light = point[1] < 0 ? .4F : 1;
          vertices
              .addVertex(matrix, point[0], point[1], point[2])
              .setColor(r * light, g * light, b * light, 1);
        }
      BufferUploader.drawWithShader(vertices.buildOrThrow());
      var celestial =
          new Matrix4f(view)
              .rotateY(
                  (float) ((level.getDayTime() % 24000 + partial) / 24000 * Math.PI * 2)
                      + profile.seed());
      billboard(
          celestial,
          "nebula",
          -55,
          9,
          55,
          53,
          -83,
          profile.body().equals("venus") ? .04F : .18F + (1 - day) * .5F);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      var stars =
          Tesselator.getInstance()
              .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      float alpha =
          profile.body().equals("venus")
              ? .04F
              : profile.celestial().atmosphere().equals("none")
                  ? 1
                  : profile.body().equals("mars") ? .45F + (1 - day) * .5F : .1F + (1 - day) * .8F;
      for (int side = 0; side < 4; side++) {
        var sky = new Matrix4f(celestial).rotateY(side * (float) Math.PI / 2);
        for (int i = 0; i < 80; i++) {
          float x = (float) (TerrainMath.hash(i, side, profile.seed()) * 170 - 85),
              y = (float) (TerrainMath.hash(side, i, profile.seed() + 1) * 85 + 4),
              size = .08F + (float) TerrainMath.hash(i, side, 51) * .13F;
          for (float[] point :
              new float[][] {
                {x - size, y - size},
                {x + size, y - size},
                {x + size, y + size},
                {x - size, y + size}
              }) stars.addVertex(sky, point[0], point[1], -85).setColor(.8F, .9F, 1, alpha);
        }
      }
      BufferUploader.drawWithShader(stars.buildOrThrow());
      var solar =
          new Matrix4f(view)
              .rotateX((float) (level.getTimeOfDay(partial) * Math.PI * 2 + Math.PI / 2));
      billboard(solar, "sun", -6, -6, 6, 6, -80, profile.body().equals("venus") ? .08F : 1);
      String neighbor =
          profile.celestial().parent().equals("sun")
              ? SolarSystem.BODIES.stream()
                  .filter(body -> body.parent().equals(profile.body()))
                  .map(body -> body.id())
                  .findFirst()
                  .orElse(null)
              : profile.celestial().parent();
      if (neighbor != null) {
        float size = profile.celestial().parent().equals("sun") ? 9 : 19;
        billboard(celestial, neighbor, -size, 17 - size, size, 17 + size, -78, 1);
      }
    } finally {
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
      setupFog.run();
    }
    return true;
  }

  private static void billboard(
      Matrix4f matrix,
      String name,
      float left,
      float bottom,
      float right,
      float top,
      float depth,
      float alpha) {
    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    RenderSystem.setShaderTexture(0, ModPresentation.id("textures/gui/sky/" + name + ".png"));
    var vertices =
        Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
    vertices.addVertex(matrix, left, bottom, depth).setUv(0, 1).setColor(1, 1, 1, alpha);
    vertices.addVertex(matrix, right, bottom, depth).setUv(1, 1).setColor(1, 1, 1, alpha);
    vertices.addVertex(matrix, right, top, depth).setUv(1, 0).setColor(1, 1, 1, alpha);
    vertices.addVertex(matrix, left, top, depth).setUv(0, 0).setColor(1, 1, 1, alpha);
    BufferUploader.drawWithShader(vertices.buildOrThrow());
  }
}
