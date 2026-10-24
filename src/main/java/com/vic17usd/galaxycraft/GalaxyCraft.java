package com.vic17usd.galaxycraft;

import com.vic17usd.galaxycraft.registry.ModBlocks;
import com.vic17usd.galaxycraft.registry.ModItems;
import com.vic17usd.galaxycraft.registry.ModDataComponents;
import com.vic17usd.galaxycraft.registry.ModRocketRegistries;
import com.vic17usd.galaxycraft.registry.ModPlanetContent;
import com.vic17usd.galaxycraft.registry.ModAlienEntities;
import com.vic17usd.galaxycraft.solar.SolarEnvironment;
import com.vic17usd.galaxycraft.config.SpaceConfig;
import com.vic17usd.galaxycraft.survival.SpaceSurvival;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Punto de entrada común, compartido por cliente y servidor dedicado. */
@Mod(GalaxyCraft.MOD_ID)
public final class GalaxyCraft {
    // Debe coincidir con mod_id en gradle.properties y con el namespace de los JSON.
    public static final String MOD_ID = "galaxycraft";

    /** NeoForge inyecta el bus del mod al construir esta clase. */
    public GalaxyCraft(IEventBus modEventBus, ModContainer container) {
        ModPlanetContent.register();
        // Los registros y la pestaña creativa pertenecen al bus del mod.
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModRocketRegistries.BLOCK_ENTITIES.register(modEventBus);
        ModRocketRegistries.MENUS.register(modEventBus);
        ModAlienEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(ModAlienEntities::attributes);
        modEventBus.addListener(ModAlienEntities::placements);
        modEventBus.addListener(ModRocketRegistries::registerCapabilities);
        modEventBus.addListener(ModItems::addCreativeTabContents);
        // Los ticks del jugador son eventos del juego, no del bus de registro.
        NeoForge.EVENT_BUS.addListener(SpaceSurvival::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SolarEnvironment::onEntityTick);
        NeoForge.EVENT_BUS.addListener(SolarEnvironment::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SolarEnvironment::onHazardTick);
        container.registerConfig(ModConfig.Type.SERVER, SpaceConfig.SPEC);
    }
}
