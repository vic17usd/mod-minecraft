package com.vic17usd.galaxycraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;

/** Espora emisiva tenue, sin colisión, que asciende y se desvanece con un sprite original. */
public final class BioSporeParticle extends TextureSheetParticle {
  public BioSporeParticle(
      ClientLevel level,
      double x,
      double y,
      double z,
      double vx,
      double vy,
      double vz,
      SpriteSet sprites) {
    super(level, x, y, z);
    xd = vx;
    yd = vy;
    zd = vz;
    hasPhysics = false;
    gravity = -.001F;
    friction = .98F;
    lifetime = 50 + random.nextInt(30);
    quadSize = .025F + random.nextFloat() * .02F;
    rCol = .4F;
    gCol = .95F;
    bCol = .8F;
    pickSprite(sprites);
  }

  @Override
  public ParticleRenderType getRenderType() {
    return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
  }

  @Override
  protected int getLightColor(float partial) {
    return 0xF000F0;
  }

  @Override
  public void tick() {
    super.tick();
    alpha = Math.max(0, 1 - (float) age / lifetime);
    xd += Math.sin(age * .12) * .0004;
  }
}
