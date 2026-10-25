package com.vic17usd.galaxycraft.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.astronomy.*;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Shader recargable por F3+T. Si no está disponible se mantiene el cubemap sin dispersión. */
@EventBusSubscriber(
    modid = GalaxyCraft.MOD_ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class AtmosphereShader {
  private static ShaderInstance shader;

  @SubscribeEvent
  public static void register(RegisterShadersEvent event) {
    shader = null;
    try {
      event.registerShader(
          new ShaderInstance(
              event.getResourceProvider(),
              ModPresentation.id("atmosphere"),
              DefaultVertexFormat.POSITION_TEX_COLOR),
          loaded -> shader = loaded);
    } catch (java.io.IOException error) {
      com.mojang.logging.LogUtils.getLogger()
          .error("No se pudo cargar la atmósfera; se usará el cielo sin dispersión", error);
    }
  }

  private static void vector(String name, Vec3 v) {
    var uniform = shader.getUniform(name);
    if (uniform != null) uniform.set((float) v.x, (float) v.y, (float) v.z);
  }

  private static void scalar(String name, float value) {
    var uniform = shader.getUniform(name);
    if (uniform != null) uniform.set(value);
  }

  public static ShaderInstance prepare(String body, Vec3 sun, double altitude, boolean orbit) {
    if (shader == null || SkySettings.SCATTER_SAMPLES.get() == 0) return null;
    var medium = orbit ? AtmosphereOptics.VACUUM : AtmosphereOptics.forBody(body);
    vector("SunDirection", sun);
    vector("BetaR", medium.rayleigh());
    vector("BetaM", medium.mie());
    vector("Absorption", medium.absorption());
    scalar("PlanetRadius", (float) OrbitalMechanics.get(body).radius());
    scalar("AtmosphereHeight", (float) medium.height());
    scalar("ObserverHeight", (float) Math.clamp(altitude / 1000, .002, 40));
    scalar("ScaleR", (float) medium.rayleighScale());
    scalar("ScaleM", (float) medium.mieScale());
    scalar("MieG", (float) medium.asymmetry());
    var samples = shader.getUniform("Samples");
    if (samples != null) samples.set(Math.max(2, SkySettings.SCATTER_SAMPLES.get()));
    return shader;
  }

  private AtmosphereShader() {}
}
