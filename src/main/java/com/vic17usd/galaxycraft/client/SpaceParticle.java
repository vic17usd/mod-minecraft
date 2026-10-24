package com.vic17usd.galaxycraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/** Partícula con sprite propio y movimiento lento: fuego, humo o polvo según el proveedor. */
public final class SpaceParticle extends TextureSheetParticle {
    private final boolean glowing;
    public SpaceParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
            SpriteSet sprites, int kind) {
        super(level, x, y, z);
        glowing = kind == 0;
        xd = vx; yd = vy; zd = vz;
        lifetime = kind == 0 ? 18 : kind == 1 ? 40 : 65;
        quadSize = kind == 1 ? .24F : .12F;
        friction = .97F; gravity = kind == 2 ? .002F : -.006F;
        hasPhysics = false;
        pickSprite(sprites);
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    @Override protected int getLightColor(float partialTick) { return glowing ? 0xF000F0 : super.getLightColor(partialTick); }
    @Override public void tick() {
        super.tick();
        alpha = Math.max(0, 1F - (float) age / lifetime);
    }
}
