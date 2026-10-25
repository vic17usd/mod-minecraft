package com.vic17usd.galaxycraft.logistics;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Conductos conectables y depósitos sólidos: los conductos no sustituyen paredes herméticas. */
public final class LogisticsBlock extends BaseEntityBlock {
  public final int kind;
  public static final MapCodec<LogisticsBlock> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      LogisticsBlock.<LogisticsBlock>propertiesCodec(),
                      Codec.intRange(0, 6).fieldOf("kind").forGetter(block -> block.kind))
                  .apply(instance, LogisticsBlock::new));

  public LogisticsBlock(Properties properties, int kind) {
    super(properties);
    this.kind = kind;
    var state = stateDefinition.any();
    for (var property : PipeBlock.PROPERTY_BY_DIRECTION.values())
      state = state.setValue(property, false);
    registerDefaultState(state);
  }

  @Override
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    PipeBlock.PROPERTY_BY_DIRECTION.values().forEach(builder::add);
  }

  private boolean connects(LevelReader level, BlockPos pos) {
    if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;
    var state = level.getBlockState(pos);
    return state.getBlock() instanceof LogisticsBlock other
        ? other.kind == kind || other.kind >= 3
        : state.hasBlockEntity()
            || kind == 2 && state.is(com.vic17usd.galaxycraft.registry.ModBlocks.LAUNCH_PAD.get());
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    var state = defaultBlockState();
    if (kind < 3)
      for (var entry : PipeBlock.PROPERTY_BY_DIRECTION.entrySet())
        state =
            state.setValue(
                entry.getValue(),
                connects(context.getLevel(), context.getClickedPos().relative(entry.getKey())));
    return state;
  }

  @Override
  protected BlockState updateShape(
      BlockState state,
      Direction direction,
      BlockState neighbor,
      LevelAccessor level,
      BlockPos pos,
      BlockPos neighborPos) {
    return kind < 3
        ? state.setValue(
            PipeBlock.PROPERTY_BY_DIRECTION.get(direction), connects(level, neighborPos))
        : state;
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    if (kind >= 3) return Shapes.block();
    var shape = Block.box(6, 6, 6, 10, 10, 10);
    for (var direction : Direction.values())
      if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction)))
        shape =
            Shapes.or(
                shape,
                switch (direction) {
                  case DOWN -> Block.box(6, 0, 6, 10, 6, 10);
                  case UP -> Block.box(6, 10, 6, 10, 16, 10);
                  case NORTH -> Block.box(6, 6, 0, 10, 10, 6);
                  case SOUTH -> Block.box(6, 6, 10, 10, 10, 16);
                  case WEST -> Block.box(0, 6, 6, 6, 10, 10);
                  case EAST -> Block.box(10, 6, 6, 16, 10, 10);
                });
    return shape;
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new LogisticsMachine(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide() || kind < 3
        ? null
        : createTickerHelper(type, ModFrontiers.MACHINE.get(), LogisticsMachine::tick);
  }

  @Override
  protected ItemInteractionResult useItemOn(
      ItemStack stack,
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hit) {
    if (kind == 3 && (stack.is(Items.BUCKET) || stack.is(Items.LAVA_BUCKET))) {
      if (level.isClientSide()
          || net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(
              player, hand, level, pos, hit.getDirection()))
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (kind >= 3
        && player instanceof ServerPlayer server
        && level.getBlockEntity(pos) instanceof LogisticsMachine machine)
      server.openMenu(machine, pos);
    return kind >= 3
        ? InteractionResult.sidedSuccess(level.isClientSide())
        : InteractionResult.PASS;
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
    if (!state.is(replacement.getBlock())
        && !level.isClientSide()
        && level.getBlockEntity(pos) instanceof LogisticsMachine machine)
      net.minecraft.world.Containers.dropItemStack(
          level, pos.getX(), pos.getY(), pos.getZ(), machine.inventory.getStackInSlot(0));
    super.onRemove(state, level, pos, replacement, moving);
  }
}
