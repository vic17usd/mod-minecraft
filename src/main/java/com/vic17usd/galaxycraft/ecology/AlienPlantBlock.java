package com.vic17usd.galaxycraft.ecology;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vic17usd.galaxycraft.biosphere.*;
import com.vic17usd.galaxycraft.registry.ModPlanetContent;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Cultivo nativo: tres edades, suelo propio y dimensión/atmósfera del perfil de origen. */
public final class AlienPlantBlock extends BushBlock implements BonemealableBlock {
  public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);
  public static final MapCodec<AlienPlantBlock> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      AlienPlantBlock.<AlienPlantBlock>propertiesCodec(),
                      LivingProfiles.BODY_CODEC.fieldOf("body").forGetter(block -> block.body),
                      Codec.STRING.fieldOf("style").forGetter(block -> block.style))
                  .apply(instance, AlienPlantBlock::new));
  public final String body, style;

  public AlienPlantBlock(Properties properties, String body, String style) {
    super(properties);
    this.body = body;
    this.style = style;
    registerDefaultState(stateDefinition.any().setValue(AGE, 0));
  }

  @Override
  protected MapCodec<? extends BushBlock> codec() {
    return CODEC;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(AGE);
  }

  public static boolean soil(String body, BlockState state) {
    return body.equals("earth")
        ? state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
        : state.is(ModPlanetContent.BLOCKS.get(body + "_regolith").get());
  }

  @Override
  protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
    return soil(body, state);
  }

  @Override
  protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    Level actual =
        level instanceof Level world
            ? world
            : level instanceof WorldGenLevel world ? world.getLevel() : null;
    return actual != null
        && actual.dimension().equals(LivingProfiles.get(body).celestial().dimension())
        && soil(body, level.getBlockState(pos.below()));
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Block.box(2, 0, 2, 14, 6 + state.getValue(AGE) * 5, 14);
  }

  @Override
  protected void randomTick(
      BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (state.getValue(AGE) < 2 && canSurvive(state, level, pos) && random.nextInt(6) == 0)
      level.setBlockAndUpdate(pos, state.cycle(AGE));
  }

  @Override
  public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
    return state.getValue(AGE) < 2 && canSurvive(state, level, pos);
  }

  @Override
  public boolean isBonemealSuccess(
      Level level, RandomSource random, BlockPos pos, BlockState state) {
    return canSurvive(state, level, pos);
  }

  @Override
  public void performBonemeal(
      ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
    if (isValidBonemealTarget(level, pos, state)) level.setBlockAndUpdate(pos, state.cycle(AGE));
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (state.getValue(AGE) != 2) return InteractionResult.PASS;
    if (!level.isClientSide()) {
      Block.popResource(level, pos, new ItemStack(ModBiosphere.HARVEST.get(body).get(), 2));
      level.setBlockAndUpdate(pos, state.setValue(AGE, 0));
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
  }

  @Override
  protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    if (!level.isClientSide()
        && entity.tickCount % 20 == 0
        && entity instanceof LivingEntity living
        && (style.equals("magma_maw") || style.equals("sulfur_thorn")))
      living.hurt(
          style.equals("magma_maw")
              ? living.damageSources().hotFloor()
              : living.damageSources().cactus(),
          1);
  }
}
