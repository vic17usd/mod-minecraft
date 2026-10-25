package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.registry.ModRocketRegistries;
import com.vic17usd.galaxycraft.registry.ModAlienEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import com.vic17usd.galaxycraft.registry.ModEquipment;

/** El registro de pantallas se carga solo en cliente, nunca en servidor dedicado. */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent public static void suitLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new SpaceSuitLayer(renderer));
        }
    }
    @SubscribeEvent public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(EquipmentKey.OPEN);
        event.register(EquipmentKey.NAVIGATE);
        event.register(EquipmentKey.QUESTS);
    }
    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ModPresentation.id("space_hud"), SpaceHud::render);
    }
    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModPresentation.EXHAUST.get(), sprites -> (type, level, x, y, z, vx, vy, vz) ->
                new SpaceParticle(level, x, y, z, vx, vy, vz, sprites, 0));
        event.registerSpriteSet(ModPresentation.SMOKE.get(), sprites -> (type, level, x, y, z, vx, vy, vz) ->
                new SpaceParticle(level, x, y, z, vx, vy, vz, sprites, 1));
        event.registerSpriteSet(ModPresentation.DUST.get(), sprites -> (type, level, x, y, z, vx, vy, vz) ->
                new SpaceParticle(level, x, y, z, vx, vy, vz, sprites, 2));
    }
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        ModAlienEntities.CRAWLERS.values().forEach(type -> event.registerEntityRenderer(type.get(), PlanetaryCrawlerRenderer::new));
        event.registerBlockEntityRenderer(ModRocketRegistries.ROCKET.get(), FullScaleRocketRenderer::new);
        event.registerEntityRenderer(com.vic17usd.galaxycraft.expansion.ModExpansion.ROVER.get(),RoverRenderer::new);
        event.registerEntityRenderer(com.vic17usd.galaxycraft.frontier.ModFrontiers.GUARDIAN.get(),context->new FrontierBossRenderer(context,false));
        event.registerEntityRenderer(com.vic17usd.galaxycraft.frontier.ModFrontiers.QUEEN.get(),context->new FrontierBossRenderer(context,true));
        com.vic17usd.galaxycraft.frontier.ModFrontiers.RAIDERS.values().forEach(type->event.registerEntityRenderer(type.get(),PlanetaryRaiderRenderer::new));
        event.registerEntityRenderer(com.vic17usd.galaxycraft.frontier.ModFrontiers.PLASMA_BOLT.get(),context->new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context,.65F,true));
        event.registerEntityRenderer(com.vic17usd.galaxycraft.frontier.ModFrontiers.METEOR.get(),context->new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context,1.5F,true));
    }
    @SubscribeEvent public static void models(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FullScaleRocketRenderer.LAYER, FullScaleRocketRenderer::layer);
    }
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRocketRegistries.ROCKET_MENU.get(), RocketScreen::new);
        event.register(ModEquipment.MENU.get(), SpaceEquipmentScreen::new);
        event.register(ModRocketRegistries.NAVIGATION_MENU.get(),CelestialMapScreen::new);
        event.register(com.vic17usd.galaxycraft.expansion.ModExpansion.QUEST_MENU.get(),QuestScreen::new);
        event.register(com.vic17usd.galaxycraft.expansion.ModExpansion.MACHINE_MENU.get(),LogisticsScreens.Machine::new);
        event.register(com.vic17usd.galaxycraft.expansion.ModExpansion.ROVER_MENU.get(),LogisticsScreens.Rover::new);
        event.register(com.vic17usd.galaxycraft.frontier.ModFrontiers.MENU.get(),ResourceNetworkScreen::new);
    }
    @SubscribeEvent public static void orbitalEffects(net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event){event.register(ModPresentation.id("orbit"),new OrbitalEffects());}
}
