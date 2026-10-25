package com.vic17usd.galaxycraft.weather;

import com.vic17usd.galaxycraft.config.SpaceConfig;
import com.vic17usd.galaxycraft.expansion.ExpansionNetworks;
import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Cráter superficial de cinco celdas como máximo, solo en geología etiquetada y lejos de máquinas.
 */
public final class MeteorImpact {
  public static final TagKey<Block> NATURAL =
      TagKey.create(Registries.BLOCK, ModPresentation.id("meteor_replaceable"));

  public static boolean allowed(ServerLevel level, BlockPos pos) {
    return level.isInWorldBounds(pos)
        && level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
        && level.getBlockState(pos).is(NATURAL)
        && level.getBlockEntity(pos) == null
        && level.structureManager().getAllStructuresAt(pos).values().stream()
            .allMatch(java.util.Set::isEmpty)
        && ExpansionNetworks.machines(level).stream()
            .noneMatch(machine -> !machine.isRemoved() && machine.getBlockPos().distSqr(pos) < 256)
        && !com.vic17usd.galaxycraft.logistics.ResourceNetworks.protectedArea(level, pos);
  }

  public static boolean impact(ServerLevel level, BlockPos center) {
    if (!allowed(level, center)) return false;
    level.sendParticles(
        ParticleTypes.EXPLOSION,
        center.getX() + .5,
        center.getY() + 1,
        center.getZ() + .5,
        1,
        0,
        0,
        0,
        0);
    level.playSound(
        null,
        center,
        net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
        net.minecraft.sounds.SoundSource.WEATHER,
        .7F,
        1.2F);
    if (!SpaceConfig.METEOR_IMPACTS.get()
        || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
      Block.popResource(
          level,
          center.above(),
          new net.minecraft.world.item.ItemStack(ModFrontiers.METEOR_FRAGMENT.get(), 2));
      return true;
    }
    for (var direction : Direction.Plane.HORIZONTAL) {
      var pos = center.relative(direction);
      if (allowed(level, pos)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
    level.setBlockAndUpdate(center, ModFrontiers.METEORITE.get().defaultBlockState());
    return true;
  }

  private MeteorImpact() {}
}
