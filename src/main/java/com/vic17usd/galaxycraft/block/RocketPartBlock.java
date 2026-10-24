package com.vic17usd.galaxycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** Colisiones del cuerpo y las aletas; al romper cualquier pieza se desmonta un único cohete. */
public final class RocketPartBlock extends Block {
    public static final IntegerProperty X = IntegerProperty.create("offset_x", 0, 2);
    public static final IntegerProperty Y = IntegerProperty.create("offset_y", 0, 6);
    public static final IntegerProperty Z = IntegerProperty.create("offset_z", 0, 2);
    public static final MapCodec<RocketPartBlock> CODEC = simpleCodec(RocketPartBlock::new);
    public RocketPartBlock(Properties properties) { super(properties); }
    @Override public MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(X, Y, Z); }
    public static BlockPos root(BlockState state, BlockPos pos) { return pos.offset(1 - state.getValue(X), -state.getValue(Y), 1 - state.getValue(Z)); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int x = state.getValue(X), z = state.getValue(Z), y = state.getValue(Y);
        if (x == 1 && z == 1) return y == 6 ? Block.box(5,0,5,11,7,11) : Shapes.block();
        if (y >= 2) return Shapes.empty();
        if (z == 1 && x == 0) return Block.box(8,0,6,16,16,10);
        if (z == 1 && x == 2) return Block.box(0,0,6,8,16,10);
        if (x == 1 && z == 0) return Block.box(6,0,8,10,16,16);
        if (x == 1 && z == 2) return Block.box(6,0,0,10,16,8);
        return Shapes.empty();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var root = root(state, pos); var block = level.getBlockState(root);
        if (block.getBlock() instanceof RocketBlock rocket) return rocket.interact(level, root, player);
        return InteractionResult.PASS;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide() && !state.is(replacement.getBlock()) && !RocketParts.CLEANING.get()) {
            var root = root(state, pos);
            if (level.getBlockState(root).getBlock() instanceof RocketBlock) level.destroyBlock(root, true);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
