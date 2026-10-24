package com.vic17usd.galaxycraft.rocket;

import com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
import com.vic17usd.galaxycraft.item.OxygenTankItem;
import com.vic17usd.galaxycraft.registry.ModBlocks;
import com.vic17usd.galaxycraft.registry.ModItems;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import com.vic17usd.galaxycraft.survival.SpaceSurvival;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.sounds.SoundSource;

/** Viaje instantáneo de un jugador con traslado del cohete; no simula vuelo orbital. */
public final class RocketTravel {
    public static boolean launch(ServerPlayer player, RocketBlockEntity source) {
        if (source.isRemoved() || source.getLevel() != player.level() || player.distanceToSqr(
                source.getBlockPos().getX() + .5, source.getBlockPos().getY() + .5, source.getBlockPos().getZ() + .5) > 64) return false;
        var body = SolarSystem.destination(source.destination());
        if (source.readiness() != 3 || !SolarSystem.accessible(source.tier(), body)
                || body.dimension().equals(player.level().dimension())) return false;
        ServerLevel target = player.server.getLevel(body.dimension());
        if (target == null || !(source.getLevel() instanceof ServerLevel origin)) return false;
        if (!player.isCreative() && SpaceSurvival.isAirless(target) && !equipped(player)) return false;
        BlockPos pad = findLanding(target, player);
        if (pad == null) return false;

        // El plan solo ocupa aire; no reemplaza terreno, bases ni contenedores del jugador.
        List<BlockPos> placed = new ArrayList<>();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos pos = pad.offset(x, 0, z);
            if (!target.setBlockAndUpdate(pos, ModBlocks.LAUNCH_PAD.get().defaultBlockState())) {
                rollback(target, placed); return false;
            }
            placed.add(pos);
        }
        BlockPos rocketPos = pad.above();
        if (!target.setBlockAndUpdate(rocketPos, source.getBlockState())) { rollback(target, placed); return false; }
        placed.add(rocketPos);
        if (!(target.getBlockEntity(rocketPos) instanceof RocketBlockEntity arrived)) { rollback(target, placed); return false; }
        player.closeContainer();
        if (!player.teleportTo(target, pad.getX() + 1.5, pad.getY() + 1, pad.getZ() + .5,
                Set.of(), player.getYRot(), player.getXRot()) || player.serverLevel() != target) {
            rollback(target, placed); return false;
        }

        // Tras confirmar el traslado, mueve objetos y consume toda la reserva del origen.
        // Solo se emiten efectos tras confirmar el viaje; un lanzamiento rechazado permanece silencioso.
        var exhaust = source.getBlockPos();
        origin.sendParticles(ModPresentation.EXHAUST.get(), exhaust.getX() + .5, exhaust.getY() + .2,
                exhaust.getZ() + .5, 45, .5, .15, .5, .08);
        origin.sendParticles(ModPresentation.SMOKE.get(), exhaust.getX() + .5, exhaust.getY(),
                exhaust.getZ() + .5, 55, 1, .2, 1, .03);
        origin.playSound(null, exhaust, ModPresentation.LAUNCH.get(), SoundSource.BLOCKS, 1.5F, 1F);
        target.sendParticles(ModPresentation.DUST.get(), rocketPos.getX() + .5, rocketPos.getY(),
                rocketPos.getZ() + .5, 35, 1, .15, 1, .02);
        target.playSound(null, rocketPos, ModPresentation.LAUNCH.get(), SoundSource.BLOCKS, .5F, .8F);
        for (int slot = 0; slot < 2; slot++) {
            arrived.inventory.setStackInSlot(slot, source.inventory.getStackInSlot(slot).copy());
            source.inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
        source.fuel.drain(RocketFueling.CAPACITY, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        origin.removeBlock(source.getBlockPos(), false);
        arrived.setChanged();
        player.fallDistance = 0;
        player.setDeltaMovement(0, 0, 0);
        player.displayClientMessage(Component.translatable("message.galaxycraft.arrived", body.name()), false);
        return true;
    }

    private static boolean equipped(ServerPlayer player) {
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SPACE_HELMET.get())) return false;
        if (player.getOffhandItem().getItem() instanceof OxygenTankItem && OxygenTankItem.oxygen(player.getOffhandItem()).current() > 0) return true;
        return player.getInventory().items.stream().anyMatch(stack -> stack.getItem() instanceof OxygenTankItem
                && OxygenTankItem.oxygen(stack).current() > 0);
    }

    private static BlockPos findLanding(ServerLevel level, ServerPlayer player) {
        int cell = Math.floorMod(player.getUUID().hashCode(), 256);
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = (cell % 16) * 32 + attempt * 16, z = (cell / 16) * 32;
            int y = level.getMinBuildHeight();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                // La consulta genera/carga únicamente el área necesaria para llegar.
                level.getChunk((x + dx) >> 4, (z + dz) >> 4);
                y = Math.max(y, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) + 2);
            }
            if (y + 4 >= level.getMaxBuildHeight()) continue;
            BlockPos center = new BlockPos(x, y, z);
            boolean clear = true;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 4; dy++) {
                BlockPos pos = center.offset(dx, dy, dz);
                if (!level.getWorldBorder().isWithinBounds(pos) || !level.isEmptyBlock(pos)) clear = false;
            }
            if (clear) return center;
        }
        return null;
    }
    private static void rollback(ServerLevel level, List<BlockPos> positions) {
        // Solo revierte los bloques que acabamos de colocar y que siguen siendo nuestros.
        for (BlockPos pos : positions.reversed()) {
            if (level.getBlockState(pos).getBlock() instanceof com.vic17usd.galaxycraft.block.RocketBlock
                    || level.getBlockState(pos).is(ModBlocks.LAUNCH_PAD.get())) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
    private RocketTravel() {}
}
