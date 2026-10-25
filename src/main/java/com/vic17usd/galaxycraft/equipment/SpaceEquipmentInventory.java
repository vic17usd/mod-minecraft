package com.vic17usd.galaxycraft.equipment;

import com.vic17usd.galaxycraft.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Cuatro piezas, dos tanques y dos módulos independientes de la armadura; filtros por ranura. */
public final class SpaceEquipmentInventory extends ItemStackHandler {
    private boolean dirty = true;
    public SpaceEquipmentInventory() { super(8); }
    @Override public int getSlotLimit(int slot) { return 1; }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(switch (slot) {
            case 0 -> ModItems.SPACE_HELMET.get(); case 1 -> ModItems.SPACE_CHESTPLATE.get();
            case 2 -> ModItems.SPACE_LEGGINGS.get(); case 3 -> ModItems.SPACE_BOOTS.get();
            case 4,5 -> ModItems.OXYGEN_TANK.get();
            case 6 -> com.vic17usd.galaxycraft.expansion.ModExpansion.THERMAL.get();
            case 7 -> com.vic17usd.galaxycraft.expansion.ModExpansion.SHIELD.get();
            default -> net.minecraft.world.item.Items.AIR;
        });
    }
    @Override public void deserializeNBT(net.minecraft.core.HolderLookup.Provider lookup,net.minecraft.nbt.CompoundTag tag) {
        // ItemStackHandler restaura Size del disco: normalizar conserva las seis ranuras antiguas.
        var migrated=tag.copy();migrated.putInt("Size",8);super.deserializeNBT(lookup,migrated);dirty=true;
    }
    @Override protected void onContentsChanged(int slot) { dirty = true; }
    public void markDirty() { dirty = true; }
    public boolean takeDirty() { boolean result = dirty; dirty = false; return result; }
}
