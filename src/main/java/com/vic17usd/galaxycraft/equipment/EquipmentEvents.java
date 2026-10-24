package com.vic17usd.galaxycraft.equipment;

import com.vic17usd.galaxycraft.GalaxyCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Sincronización y muerte respetan keepInventory; los objetos no se duplican al reaparecer. */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID)
public final class EquipmentEvents {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) EquipmentNetworking.sync(player); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) EquipmentNetworking.sync(player); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { if (event.getEntity() instanceof ServerPlayer player) EquipmentNetworking.sync(player); }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof ServerPlayer target)
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(observer, EquipmentNetworking.snapshot(target));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && SpaceEquipment.inventory(player).takeDirty()) EquipmentNetworking.sync(player);
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || player.isSpectator()) return;
        var inventory = SpaceEquipment.inventory(player);
        for (int i = 0; i < inventory.getSlots(); i++) {
            var stack = inventory.extractItem(i, 1, false);
            if (!stack.isEmpty()) event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
        }
    }
    private EquipmentEvents() {}
}
