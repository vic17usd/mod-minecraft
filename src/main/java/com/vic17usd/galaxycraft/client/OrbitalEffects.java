package com.vic17usd.galaxycraft.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/** Cielo negro orbital sin niebla de superficie; la estación no hereda el cielo del Overworld. */
public final class OrbitalEffects extends DimensionSpecialEffects {
  public OrbitalEffects() {
    super(Float.NaN, false, SkyType.NONE, false, false);
  }

  @Override
  public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
    return Vec3.ZERO;
  }

  @Override
  public boolean isFoggyAt(int x, int z) {
    return false;
  }
}
