package com.vic17usd.galaxycraft.equipment;

import com.vic17usd.galaxycraft.item.OxygenTankItem;
import com.vic17usd.galaxycraft.registry.ModItems;
import com.vic17usd.galaxycraft.registry.ModEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Punto único de consulta usado por supervivencia, navegación, HUD y recarga. */
public final class SpaceEquipment {
    public static SpaceEquipmentInventory inventory(Player player) { return player.getData(ModEquipment.SUIT.get()); }
    public static boolean hasHelmet(Player player) {
        return inventory(player).getStackInSlot(0).is(ModItems.SPACE_HELMET.get())
                || player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SPACE_HELMET.get());
    }
    public static boolean fullSuit(Player player) {
        var inventory = inventory(player);
        for (int i = 0; i < 4; i++) if (inventory.getStackInSlot(i).isEmpty()) return false;
        return true;
    }
    public static List<ItemStack> tanks(Player player) {
        List<ItemStack> result = new ArrayList<>();
        var equipment = inventory(player);
        for (int i = 4; i < 6; i++) if (equipment.getStackInSlot(i).getItem() instanceof OxygenTankItem) result.add(equipment.getStackInSlot(i));
        // Compatibilidad con mundos anteriores: después de los tanques equipados se usa la mochila.
        for (var stack : player.getInventory().items) if (stack.getItem() instanceof OxygenTankItem) result.add(stack);
        if (player.getOffhandItem().getItem() instanceof OxygenTankItem) result.add(player.getOffhandItem());
        return result;
    }
    public static boolean hasOxygen(Player player) { return tanks(player).stream().anyMatch(stack -> OxygenTankItem.oxygen(stack).current() > 0); }
    public static boolean consume(Player player) {
        for (var stack : tanks(player)) if (OxygenTankItem.consumeOxygen(stack)) {
            inventory(player).markDirty(); return true;
        }
        return false;
    }
    private SpaceEquipment() {}
}
