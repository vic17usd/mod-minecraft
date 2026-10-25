package com.vic17usd.galaxycraft;

import com.vic17usd.galaxycraft.registry.ModBlocks;
import com.vic17usd.galaxycraft.registry.ModItems;
import com.vic17usd.galaxycraft.registry.ModDataComponents;
import com.vic17usd.galaxycraft.registry.ModRocketRegistries;
import com.vic17usd.galaxycraft.registry.ModPlanetContent;
import com.vic17usd.galaxycraft.registry.ModAlienEntities;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import com.vic17usd.galaxycraft.registry.ModEquipment;
import com.vic17usd.galaxycraft.registry.ModSites;
import com.vic17usd.galaxycraft.equipment.EquipmentNetworking;
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
        ModSites.registerItems();
        com.vic17usd.galaxycraft.expansion.ModExpansion.registerBlockItems();
        com.vic17usd.galaxycraft.frontier.ModFrontiers.registerItems();
        com.vic17usd.galaxycraft.biosphere.ModBiosphere.content();
        // Los registros y la pestaña creativa pertenecen al bus del mod.
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModPresentation.ARMOR.register(modEventBus);
        ModPresentation.TABS.register(modEventBus);
        ModPresentation.SOUNDS.register(modEventBus);
        ModPresentation.PARTICLES.register(modEventBus);
        ModEquipment.ATTACHMENTS.register(modEventBus);
        ModEquipment.MENUS.register(modEventBus);
        ModSites.STRUCTURES.register(modEventBus);
        ModSites.PIECES.register(modEventBus);
        modEventBus.addListener(EquipmentNetworking::register);
        modEventBus.addListener(com.vic17usd.galaxycraft.navigation.NavigationNetworking::register);
        com.vic17usd.galaxycraft.expansion.ModExpansion.COMPONENTS.register(modEventBus);
        com.vic17usd.galaxycraft.expansion.ModExpansion.ATTACHMENTS.register(modEventBus);
        com.vic17usd.galaxycraft.expansion.ModExpansion.BLOCK_ENTITIES.register(modEventBus);
        com.vic17usd.galaxycraft.expansion.ModExpansion.MENUS.register(modEventBus);
        com.vic17usd.galaxycraft.expansion.ModExpansion.ENTITIES.register(modEventBus);
        modEventBus.addListener(com.vic17usd.galaxycraft.expansion.ModExpansion::capabilities);
        modEventBus.addListener(com.vic17usd.galaxycraft.expansion.ExpansionNetworking::register);
        ModRocketRegistries.BLOCK_ENTITIES.register(modEventBus);
        ModRocketRegistries.MENUS.register(modEventBus);
        ModAlienEntities.ENTITIES.register(modEventBus);
        com.vic17usd.galaxycraft.biosphere.ModBiosphere.ENTITIES.register(modEventBus);
        com.vic17usd.galaxycraft.biosphere.ModBiosphere.GENERATORS.register(modEventBus);
        com.vic17usd.galaxycraft.biosphere.ModBiosphere.SOURCES.register(modEventBus);
        com.vic17usd.galaxycraft.biosphere.ModBiosphere.FEATURES.register(modEventBus);
        com.vic17usd.galaxycraft.biosphere.ModBiosphere.STRUCTURES.register(modEventBus);
        com.vic17usd.galaxycraft.biosphere.ModBiosphere.PIECES.register(modEventBus);
        com.vic17usd.galaxycraft.biosphere.ModBiosphere.ATTACHMENTS.register(modEventBus);
        com.vic17usd.galaxycraft.biosphere.ModBiosphere.MENUS.register(modEventBus);
        modEventBus.addListener(com.vic17usd.galaxycraft.biosphere.ModBiosphere::attributes);
        modEventBus.addListener(com.vic17usd.galaxycraft.biosphere.ModBiosphere::placements);
        modEventBus.addListener(com.vic17usd.galaxycraft.civilization.ColonyNetworking::register);
        com.vic17usd.galaxycraft.frontier.ModFrontiers.ENTITIES.register(modEventBus);
        com.vic17usd.galaxycraft.frontier.ModFrontiers.BLOCK_ENTITIES.register(modEventBus);
        com.vic17usd.galaxycraft.frontier.ModFrontiers.MENUS.register(modEventBus);
        modEventBus.addListener(com.vic17usd.galaxycraft.frontier.ModFrontiers::attributes);
        modEventBus.addListener(com.vic17usd.galaxycraft.frontier.ModFrontiers::placements);
        modEventBus.addListener(com.vic17usd.galaxycraft.logistics.LogisticsCapabilities::register);
        modEventBus.addListener(ModAlienEntities::attributes);
        modEventBus.addListener(ModAlienEntities::placements);
        modEventBus.addListener(ModRocketRegistries::registerCapabilities);
        modEventBus.addListener(ModItems::addCreativeTabContents);
        // Los ticks del jugador son eventos del juego, no del bus de registro.
        NeoForge.EVENT_BUS.addListener(SpaceSurvival::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SolarEnvironment::onEntityTick);
        NeoForge.EVENT_BUS.addListener(SolarEnvironment::onPlayerTick);
        container.registerConfig(ModConfig.Type.SERVER, SpaceConfig.SPEC);
    }
}
