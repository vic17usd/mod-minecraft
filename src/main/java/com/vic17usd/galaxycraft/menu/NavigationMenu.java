package com.vic17usd.galaxycraft.menu;

import com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
import com.vic17usd.galaxycraft.navigation.CelestialCatalog;
import com.vic17usd.galaxycraft.navigation.NavigationRules;
import com.vic17usd.galaxycraft.registry.ModRocketRegistries;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

/** Menú sin ranuras: sincroniza telemetría y selección del ordenador que el jugador tiene abierto. */
public final class NavigationMenu extends AbstractContainerMenu {
    private final RocketBlockEntity rocket;
    private final BlockPos position;
    private final ContainerData data;
    public NavigationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        super(ModRocketRegistries.NAVIGATION_MENU.get(),id);
        rocket = null; position = buffer.readBlockPos(); data = new SimpleContainerData(6); addDataSlots(data);
    }
    public NavigationMenu(int id, Inventory inventory, RocketBlockEntity rocket) {
        super(ModRocketRegistries.NAVIGATION_MENU.get(),id); this.rocket = rocket; position = rocket.getBlockPos();
        data = new ContainerData() {
            @Override public int get(int index) { return switch(index) {
                case 0 -> rocket.fuel.getFluidAmount(); case 1 -> rocket.readiness(); case 2 -> rocket.tier(); case 3 -> rocket.destination();
                case 4 -> NavigationRules.reason((ServerPlayer)inventory.player,rocket,SolarSystem.destination(rocket.destination())).ordinal();
                default -> rocket.launchStarted() >= 0 ? 1 : 0;
            }; }
            @Override public void set(int index,int value) { /* La autoridad es el servidor. */ }
            @Override public int getCount() { return 6; }
        }; addDataSlots(data);
    }
    public RocketBlockEntity rocket() { return rocket; }
    public int fuel() { return data.get(0); }
    public int tier() { return data.get(2); }
    public int destination() { return data.get(3); }
    public NavigationRules.BlockReason reason() {
        int value = data.get(4); return value >= 0 && value < NavigationRules.BlockReason.values().length
                ? NavigationRules.BlockReason.values()[value] : NavigationRules.BlockReason.UNAVAILABLE;
    }
    public boolean select(ServerPlayer player, ResourceLocation destination) {
        if (player.containerMenu != this || !stillValid(player) || !destination.getNamespace().equals("galaxycraft") || rocket.launchStarted() >= 0) return false;
        var body = CelestialCatalog.body(destination.getPath());
        if (body.isEmpty()) return false;
        boolean accepted = rocket.selectDestination(body.get().id());
        if (accepted) broadcastChanges(); return accepted;
    }
    public boolean launch(ServerPlayer player, ResourceLocation confirmed) {
        if (player.containerMenu != this || !stillValid(player) || !confirmed.getNamespace().equals("galaxycraft")
                || !SolarSystem.destination(rocket.destination()).id().equals(confirmed.getPath())
                || NavigationRules.reason(player,rocket,SolarSystem.destination(rocket.destination())) != NavigationRules.BlockReason.READY) return false;
        return rocket.beginLaunch(player);
    }
    @Override public boolean stillValid(Player player) {
        return player.isAlive() && !player.isSpectator() && player.distanceToSqr(position.getX() + .5,position.getY() + .5,position.getZ() + .5) <= 64
                && (rocket == null || !rocket.isRemoved() && rocket.getLevel() == player.level() && player.level().getBlockEntity(position) == rocket);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) { return ItemStack.EMPTY; }
}
