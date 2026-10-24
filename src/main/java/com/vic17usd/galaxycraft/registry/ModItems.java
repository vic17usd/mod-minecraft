package com.vic17usd.galaxycraft.registry;

import com.vic17usd.galaxycraft.GalaxyCraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import com.vic17usd.galaxycraft.item.SchematicItem;
import com.vic17usd.galaxycraft.item.EquipmentConsoleItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
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
    public static final DeferredItem<BlockItem> REINFORCED_PANEL = ITEMS.registerSimpleBlockItem("reinforced_panel",ModBlocks.REINFORCED_PANEL);
    public static final DeferredItem<BlockItem> VENT_GRATE = ITEMS.registerSimpleBlockItem("vent_grate",ModBlocks.VENT_GRATE);
    public static final DeferredItem<BlockItem> ENERGY_CELL = ITEMS.registerSimpleBlockItem("energy_cell",ModBlocks.ENERGY_CELL);

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

    public static final DeferredItem<Item> GALAXY_EMBLEM = ITEMS.registerSimpleItem("galaxy_emblem");
    public static final DeferredItem<Item> THRUSTER = ITEMS.registerSimpleItem("thruster");
    public static final DeferredItem<Item> NAVIGATION_CORE = ITEMS.registerSimpleItem("navigation_core");
    public static final DeferredItem<SchematicItem> LUNAR_SCHEMATIC = ITEMS.register("lunar_schematic", () -> new SchematicItem("thruster"));
    public static final DeferredItem<SchematicItem> MARTIAN_SCHEMATIC = ITEMS.register("martian_schematic", () -> new SchematicItem("navigation_core"));
    public static final DeferredItem<EquipmentConsoleItem> EQUIPMENT_CONSOLE = ITEMS.register("equipment_console", EquipmentConsoleItem::new);
    public static final DeferredItem<PickaxeItem> TITANIUM_PICKAXE = ITEMS.register("titanium_pickaxe", () -> new PickaxeItem(Tiers.DIAMOND,
            new Item.Properties().attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 1, -2.8F))));
    public static final DeferredItem<ArmorItem> SPACE_CHESTPLATE = ITEMS.register("space_chestplate", () -> suitPiece(ArmorItem.Type.CHESTPLATE, 240));
    public static final DeferredItem<ArmorItem> SPACE_LEGGINGS = ITEMS.register("space_leggings", () -> suitPiece(ArmorItem.Type.LEGGINGS, 225));
    public static final DeferredItem<ArmorItem> SPACE_BOOTS = ITEMS.register("space_boots", () -> suitPiece(ArmorItem.Type.BOOTS, 195));
    private static ArmorItem suitPiece(ArmorItem.Type type, int durability) {
        return new ArmorItem(ModPresentation.SPACE_ARMOR, type, new Item.Properties().durability(durability));
    }

    // La capa del casco equipado también utiliza una textura propia del mod.
    public static final DeferredItem<ArmorItem> SPACE_HELMET = ITEMS.register("space_helmet",
            () -> new ArmorItem(ModPresentation.SPACE_ARMOR, ArmorItem.Type.HELMET,
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
