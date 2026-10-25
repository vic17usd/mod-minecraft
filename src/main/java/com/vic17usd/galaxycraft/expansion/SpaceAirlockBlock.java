package com.vic17usd.galaxycraft.expansion;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

/** Escotilla de dos bloques; ambas mitades cerradas cuentan como barrera hermética. */
public final class SpaceAirlockBlock extends DoorBlock {
  public static final MapCodec<SpaceAirlockBlock> CODEC = simpleCodec(SpaceAirlockBlock::new);

  public SpaceAirlockBlock(Properties properties) {
    super(BlockSetType.IRON, properties);
  }

  @Override
  public MapCodec<? extends DoorBlock> codec() {
    return CODEC;
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (!level.isClientSide()) {
      setOpen(player, level, state, pos, !state.getValue(OPEN));
      ExpansionNetworks.invalidate(level);
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
  }
}
