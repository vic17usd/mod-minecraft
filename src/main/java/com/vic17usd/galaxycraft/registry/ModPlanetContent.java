package com.vic17usd.galaxycraft.registry;

import com.vic17usd.galaxycraft.solar.SolarSystem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** Contenido específico por cuerpo, registrado antes de conectar los DeferredRegister. */
public final class ModPlanetContent {
    public static final Map<String, DeferredBlock<Block>> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> ITEMS = new LinkedHashMap<>();

    public static void register() {
        for (var body : SolarSystem.BODIES) {
            if (body.id().equals("earth")) continue;
            for (String suffix : new String[]{"stone", "regolith", "ore"}) {
                String name = body.id() + "_" + suffix;
                var block = ModBlocks.BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.of()
                        .strength(suffix.equals("ore") ? 4 : 2).sound(SoundType.STONE).requiresCorrectToolForDrops());
                BLOCKS.put(name, block);
                DeferredItem<BlockItem> item = ModItems.ITEMS.registerSimpleBlockItem(name, block);
                ITEMS.put(name, item);
            }
            ITEMS.put(body.resource(), ModItems.ITEMS.registerSimpleItem(body.resource(), new Item.Properties()));
        }
    }
    private ModPlanetContent() {}
}
