package com.vic17usd.galaxycraft.ecology;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vic17usd.galaxycraft.biosphere.LivingProfiles;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Respiradero intermitente acotado: partículas y empuje local, sin explosiones ni carga de chunks.
 */
public final class GeyserBlock extends Block {
  public final String body;
  public static final MapCodec<GeyserBlock> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      GeyserBlock.<GeyserBlock>propertiesCodec(),
                      LivingProfiles.BODY_CODEC.fieldOf("body").forGetter(block -> block.body))
                  .apply(instance, GeyserBlock::new));

  public GeyserBlock(Properties properties, String body) {
    super(properties);
    this.body = body;
  }

  @Override
  public MapCodec<? extends Block> codec() {
    return CODEC;
  }

  @Override
  protected void randomTick(
      BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (random.nextInt(3) != 0 || !level.isEmptyBlock(pos.above())) return;
    level.sendParticles(
        ParticleTypes.CLOUD,
        pos.getX() + .5,
        pos.getY() + 1,
        pos.getZ() + .5,
        16,
        .25,
        .6,
        .25,
        .12);
    for (var living :
        level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).expandTowards(0, 4, 0))) {
      living.push(0, .35, 0);
      living.hurtMarked = true;
      if (LivingProfiles.get(body).celestial().heat() > 0)
        living.hurt(living.damageSources().hotFloor(), 2);
    }
  }
}
