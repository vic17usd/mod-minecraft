package com.vic17usd.galaxycraft.registry;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.equipment.SpaceEquipmentInventory;
import com.vic17usd.galaxycraft.menu.SpaceEquipmentMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** El attachment usa NBT para persistir inventario; cada tanque conserva sus Data Components. */
public final class ModEquipment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GalaxyCraft.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SpaceEquipmentInventory>> SUIT = ATTACHMENTS.register("space_equipment",
            () -> AttachmentType.serializable(SpaceEquipmentInventory::new).copyOnDeath().build());
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GalaxyCraft.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<SpaceEquipmentMenu>> MENU = MENUS.register("space_equipment",
            () -> new MenuType<>(SpaceEquipmentMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    private ModEquipment() {}
}
