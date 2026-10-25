package com.vic17usd.galaxycraft.expansion;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Detector, panel solar, módulo de acoplamiento y generador comparten ciclo de máquina y
 * persistencia.
 */
public final class ExpansionMachineBlock extends BaseEntityBlock {
  public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
  public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
  public static final MapCodec<ExpansionMachineBlock> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      ExpansionMachineBlock.<ExpansionMachineBlock>propertiesCodec(),
                      Codec.intRange(0, 3).fieldOf("kind").forGetter(block -> block.kind))
                  .apply(instance, ExpansionMachineBlock::new));
  public final int kind;

  public ExpansionMachineBlock(Properties properties, int kind) {
    super(properties);
    this.kind = kind;
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(ACTIVE, false)
            .setValue(FACING, net.minecraft.core.Direction.NORTH));
  }

  @Override
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(ACTIVE, FACING);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new ExpansionMachine(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide()
        ? null
        : createTickerHelper(type, ModExpansion.MACHINE.get(), ExpansionMachine::tick);
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (player instanceof ServerPlayer server
        && level.getBlockEntity(pos) instanceof ExpansionMachine machine)
      server.openMenu(machine, pos);
    return InteractionResult.sidedSuccess(level.isClientSide());
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
    if (!state.is(replacement.getBlock())
        && !level.isClientSide()
        && level.getBlockEntity(pos) instanceof ExpansionMachine machine) {
      net.minecraft.world.Containers.dropItemStack(
          level, pos.getX(), pos.getY(), pos.getZ(), machine.inventory.getStackInSlot(0));
      ExpansionNetworks.invalidate(level);
    }
    super.onRemove(state, level, pos, replacement, moving);
  }
}
