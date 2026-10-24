package com.vic17usd.galaxycraft.equipment;

import com.vic17usd.galaxycraft.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Cuatro piezas independientes de la armadura vanilla y dos tanques; nunca acepta objetos genéricos. */
public final class SpaceEquipmentInventory extends ItemStackHandler {
    private boolean dirty = true;
    public SpaceEquipmentInventory() { super(6); }
    @Override public int getSlotLimit(int slot) { return 1; }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(switch (slot) {
            case 0 -> ModItems.SPACE_HELMET.get(); case 1 -> ModItems.SPACE_CHESTPLATE.get();
            case 2 -> ModItems.SPACE_LEGGINGS.get(); case 3 -> ModItems.SPACE_BOOTS.get();
            default -> ModItems.OXYGEN_TANK.get();
        });
    }
    @Override protected void onContentsChanged(int slot) { dirty = true; }
    public void markDirty() { dirty = true; }
    public boolean takeDirty() { boolean result = dirty; dirty = false; return result; }
}
