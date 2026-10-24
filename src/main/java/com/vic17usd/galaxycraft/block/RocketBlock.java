package com.vic17usd.galaxycraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
import com.vic17usd.galaxycraft.registry.ModRocketRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Raíz del cohete multibloque. El BER representa el vehículo completo y las piezas reservan su volumen. */
public final class RocketBlock extends BaseEntityBlock {
    private final int tier;
    public static final MapCodec<RocketBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RocketBlock.<RocketBlock>propertiesCodec(), Codec.intRange(1, 3).optionalFieldOf("tier", 1)
                    .forGetter(RocketBlock::tier)).apply(instance, RocketBlock::new));

    public RocketBlock(Properties properties) { this(properties, 1); }
    public RocketBlock(Properties properties, int tier) { super(properties); this.tier = tier; }
    public int tier() { return tier; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RocketBlockEntity(pos, state); }

    public static boolean hasClearance(Level level, BlockPos pos) {
        return RocketParts.clear(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        return interact(level, pos, player);
    }
    public InteractionResult interact(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof RocketBlockEntity rocket) {
            // La posición viaja en el paquete de apertura; el menú valida distancia y bloque.
            com.vic17usd.galaxycraft.navigation.NavigationAccess.openRocket(serverPlayer,rocket);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!state.is(previous.getBlock())) RocketParts.install(level, pos);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModRocketRegistries.ROCKET.get(), RocketBlockEntity::tick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide()) {
            RocketParts.remove(level, pos);
            if (level.getBlockEntity(pos) instanceof RocketBlockEntity rocket) {
            // Devuelve los objetos una sola vez. El fluido se pierde al desmontar el prototipo.
            for (int slot = 0; slot < rocket.inventory.getSlots(); slot++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), rocket.inventory.getStackInSlot(slot));
            }
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
