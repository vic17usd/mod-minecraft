package com.vic17usd.galaxycraft.menu;

import com.vic17usd.galaxycraft.equipment.SpaceEquipment;
import com.vic17usd.galaxycraft.registry.ModEquipment;
import com.vic17usd.galaxycraft.item.OxygenTankItem;
import com.vic17usd.galaxycraft.survival.SpaceSurvival;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Ocho ranuras personales: cuatro piezas, dos tanques y dos módulos; transacciones vanilla de contenedores. */
public final class SpaceEquipmentMenu extends AbstractContainerMenu {
    public SpaceEquipmentMenu(int id, Inventory playerInventory) {
        super(ModEquipment.MENU.get(), id);
        for (int i = 0; i < 8; i++) addSlot(new SlotItemHandler(SpaceEquipment.inventory(playerInventory.player), i, 10 + i * 20, 38));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 92 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 150));
    }
    @Override public boolean stillValid(Player player) { return player.isAlive(); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || player.level().isClientSide() || !stillValid(player) || SpaceSurvival.isAirless(player.level())) return false;
        for (var stack : SpaceEquipment.tanks(player)) stack.set(com.vic17usd.galaxycraft.registry.ModDataComponents.OXYGEN_LEVEL.get(), OxygenTankItem.oxygen(stack).refill());
        SpaceEquipment.inventory(player).markDirty(); broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        if (index < 8) { if (!moveItemStackTo(stack, 8, 44, true)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack, 0, 8, false)) {
            if (index < 35) { if (!moveItemStackTo(stack, 35, 44, false)) return ItemStack.EMPTY; }
            else if (!moveItemStackTo(stack, 8, 35, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
}
