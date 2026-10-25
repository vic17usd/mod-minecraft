package com.vic17usd.galaxycraft.combat;

import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import com.vic17usd.galaxycraft.weather.MeteorImpact;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/**
 * Proyectiles con vida limitada: plasma sin explosión, meteoritos con impacto validado en servidor.
 */
public final class SpaceProjectile extends ThrowableItemProjectile {
  private boolean resolved;

  public SpaceProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) {
    super(type, level);
  }

  public boolean meteor() {
    return getType() == ModFrontiers.METEOR.get();
  }

  @Override
  protected Item getDefaultItem() {
    return meteor() ? ModFrontiers.METEOR_FRAGMENT.get() : ModFrontiers.PLASMA.get();
  }

  @Override
  protected double getDefaultGravity() {
    return meteor() ? .06 : 0;
  }

  @Override
  public void tick() {
    if (!level().isClientSide()
        && (tickCount > 200
            || !level()
                .getChunkSource()
                .hasChunk(blockPosition().getX() >> 4, blockPosition().getZ() >> 4))) {
      discard();
      return;
    }
    super.tick();
    if (level().isClientSide())
      level()
          .addParticle(
              meteor() ? ParticleTypes.SMOKE : ParticleTypes.ELECTRIC_SPARK,
              getX(),
              getY(),
              getZ(),
              0,
              0,
              0);
  }

  @Override
  protected void onHitEntity(EntityHitResult hit) {
    super.onHitEntity(hit);
    if (level().isClientSide() || resolved) return;
    resolved = true;
    if (!(hit.getEntity() instanceof PlanetBoss))
      hit.getEntity().hurt(damageSources().thrown(this, getOwner()), meteor() ? 8 : 6);
    discard();
  }

  @Override
  protected void onHitBlock(BlockHitResult hit) {
    super.onHitBlock(hit);
    if (level() instanceof ServerLevel server && !resolved) {
      resolved = true;
      if (meteor()) MeteorImpact.impact(server, hit.getBlockPos());
      else
        server.sendParticles(
            ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 8, .2, .2, .2, .03);
      discard();
    }
  }
}
