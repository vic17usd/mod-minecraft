package com.vic17usd.galaxycraft.registry;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.block.LaunchPadBlock;
import com.vic17usd.galaxycraft.block.RocketBlock;
import com.vic17usd.galaxycraft.block.RocketPartBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Agrupa exclusivamente los registros de bloques de GalaxyCraft. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(GalaxyCraft.MOD_ID);
    public static final DeferredBlock<RocketPartBlock> ROCKET_PART = BLOCKS.register("rocket_part",
            () -> new RocketPartBlock(BlockBehaviour.Properties.of().strength(4).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<LaunchPadBlock> LAUNCH_PAD = BLOCKS.register("launch_pad",
            () -> new LaunchPadBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(4.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredBlock<RocketBlock> TIER1_ROCKET = BLOCKS.register("tier1_rocket",
            () -> new RocketBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(4.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));
    public static final DeferredBlock<RocketBlock> TIER2_ROCKET = BLOCKS.register("tier2_rocket",
            () -> new RocketBlock(BlockBehaviour.Properties.of().strength(4).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops(), 2));
    public static final DeferredBlock<RocketBlock> TIER3_ROCKET = BLOCKS.register("tier3_rocket",
            () -> new RocketBlock(BlockBehaviour.Properties.of().strength(4).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops(), 3));

    // El proveedor se ejecuta durante el registro, no al cargar la clase.
    public static final DeferredBlock<Block> TITANIUM_ORE = BLOCKS.register(
            "titanium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    // Las etiquetas determinan qué herramienta cumple este requisito.
                    .requiresCorrectToolForDrops())
    );

    private ModBlocks() {
        // Clase de registros: no necesita instancias.
    }
}
