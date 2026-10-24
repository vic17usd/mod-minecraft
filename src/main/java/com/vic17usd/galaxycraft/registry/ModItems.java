package com.vic17usd.galaxycraft.registry;

import com.vic17usd.galaxycraft.GalaxyCraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import com.vic17usd.galaxycraft.item.OxygenTankItem;
import com.vic17usd.galaxycraft.item.RocketItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registra los ítems y decide su presentación en el inventario creativo. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(GalaxyCraft.MOD_ID);

    // El BlockItem permite transportar y colocar el bloque desde un ItemStack.
    // El helper resuelve el bloque cuando el registro está preparado, sin un get() prematuro.
    public static final DeferredItem<BlockItem> TITANIUM_ORE =
            ITEMS.registerSimpleBlockItem("titanium_ore", ModBlocks.TITANIUM_ORE);

    public static final DeferredItem<BlockItem> LAUNCH_PAD = ITEMS.registerSimpleBlockItem("launch_pad", ModBlocks.LAUNCH_PAD);
    public static final DeferredItem<RocketItem> TIER1_ROCKET = ITEMS.register("tier1_rocket",
            () -> new RocketItem(ModBlocks.TIER1_ROCKET.get(), new Item.Properties()));
    public static final DeferredItem<RocketItem> TIER2_ROCKET = ITEMS.register("tier2_rocket",
            () -> new RocketItem(ModBlocks.TIER2_ROCKET.get(), new Item.Properties()));
    public static final DeferredItem<RocketItem> TIER3_ROCKET = ITEMS.register("tier3_rocket",
            () -> new RocketItem(ModBlocks.TIER3_ROCKET.get(), new Item.Properties()));

    public static final DeferredItem<OxygenTankItem> OXYGEN_TANK = ITEMS.register(
            "oxygen_tank", () -> new OxygenTankItem(new Item.Properties()));

    // Casco funcional con material y textura de hierro provisionales, sin código cliente.
    public static final DeferredItem<ArmorItem> SPACE_HELMET = ITEMS.register("space_helmet",
            () -> new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(165)));

    /** Se conecta como listener al bus del mod desde GalaxyCraft. */
    public static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(TITANIUM_ORE);
            event.accept(LAUNCH_PAD);
            ModPlanetContent.ITEMS.values().forEach(event::accept);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(OXYGEN_TANK);
            event.accept(TIER1_ROCKET);
            event.accept(TIER2_ROCKET);
            event.accept(TIER3_ROCKET);
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) event.accept(SPACE_HELMET);
    }

    private ModItems() {
        // Clase de registros: no necesita instancias.
    }
}
