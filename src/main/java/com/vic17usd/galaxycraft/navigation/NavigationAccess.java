package com.vic17usd.galaxycraft.navigation;

import com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
import com.vic17usd.galaxycraft.menu.RocketMenu;
import com.vic17usd.galaxycraft.menu.NavigationMenu;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;

/** Apertura contextual sin posiciones arbitrarias enviadas por el cliente ni carga forzada de chunks. */
public final class NavigationAccess {
    public static RocketBlockEntity currentRocket(ServerPlayer player) {
        if (player.containerMenu instanceof NavigationMenu menu && menu.stillValid(player)) return menu.rocket();
        if (player.containerMenu instanceof RocketMenu menu && menu.stillValid(player)) {
            var pos = menu.rocketPosition();
            return pos != null && player.level().getBlockEntity(pos) instanceof RocketBlockEntity rocket ? rocket : null;
        }
        if (player.containerMenu != player.inventoryMenu) return null;
        RocketBlockEntity nearest = null; double distance = 16;
        var origin = player.blockPosition();
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = -7; y <= 1; y++) {
            BlockPos pos = origin.offset(x,y,z);
            if (!player.serverLevel().getChunkSource().hasChunk(pos.getX() >> 4,pos.getZ() >> 4)) continue;
            if (player.level().getBlockEntity(pos) instanceof RocketBlockEntity rocket) {
                double candidate = player.distanceToSqr(pos.getX() + .5,pos.getY() + .5,pos.getZ() + .5);
                if (candidate <= distance) { distance = candidate; nearest = rocket; }
            }
        }
        return nearest;
    }
    public static void open(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.containerMenu instanceof NavigationMenu) return;
        var rocket = currentRocket(player);
        if (rocket == null) { player.displayClientMessage(Component.translatable("navigation.galaxycraft.no_computer"),true); return; }
        openRocket(player,rocket);
    }
    public static void openRocket(ServerPlayer player,RocketBlockEntity rocket) {
        if (!player.isAlive() || player.isSpectator() || rocket.isRemoved() || rocket.getLevel()!=player.level()
                || player.distanceToSqr(rocket.getBlockPos().getX()+.5,rocket.getBlockPos().getY()+.5,rocket.getBlockPos().getZ()+.5)>64) return;
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new NavigationMenu(id, inventory, rocket),
                Component.translatable("navigation.galaxycraft.title")), rocket.getBlockPos());
    }
    public static void controls(ServerPlayer player) {
        if (player.containerMenu instanceof NavigationMenu menu && menu.stillValid(player)) player.openMenu(menu.rocket(), menu.rocket().getBlockPos());
    }
    private NavigationAccess() {}
}
