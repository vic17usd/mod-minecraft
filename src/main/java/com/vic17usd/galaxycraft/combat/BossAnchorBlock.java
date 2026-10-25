package com.vic17usd.galaxycraft.combat;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

/** Sello irrompible en supervivencia: despierta un encuentro una sola vez por bóveda. */
public final class BossAnchorBlock extends BaseEntityBlock {
  public final boolean queen;
  public static final MapCodec<BossAnchorBlock> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      BossAnchorBlock.<BossAnchorBlock>propertiesCodec(),
                      Codec.BOOL.fieldOf("queen").forGetter(block -> block.queen))
                  .apply(instance, BossAnchorBlock::new));

  public BossAnchorBlock(Properties properties, boolean queen) {
    super(properties);
    this.queen = queen;
  }

  @Override
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new BossAnchor(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide()
        ? null
        : createTickerHelper(type, ModFrontiers.ANCHOR.get(), BossAnchor::tick);
  }
}
