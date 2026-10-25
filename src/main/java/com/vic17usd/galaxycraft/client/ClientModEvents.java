package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.registry.ModAlienEntities;
import com.vic17usd.galaxycraft.registry.ModEquipment;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import com.vic17usd.galaxycraft.registry.ModRocketRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** El registro de pantallas se carga solo en cliente, nunca en servidor dedicado. */
@EventBusSubscriber(
    modid = GalaxyCraft.MOD_ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class ClientModEvents {
  @SubscribeEvent
  public static void suitLayers(EntityRenderersEvent.AddLayers event) {
    for (var skin : event.getSkins()) {
      net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
      if (renderer != null) renderer.addLayer(new SpaceSuitLayer(renderer));
    }
  }

  @SubscribeEvent
  public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
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
    event.registerSpriteSet(
        com.vic17usd.galaxycraft.morphology.ModMorphology.SPORE.get(),
        sprites ->
            (type, level, x, y, z, vx, vy, vz) ->
                new BioSporeParticle(level, x, y, z, vx, vy, vz, sprites));
    event.registerSpriteSet(
        ModPresentation.EXHAUST.get(),
        sprites ->
            (type, level, x, y, z, vx, vy, vz) ->
                new SpaceParticle(level, x, y, z, vx, vy, vz, sprites, 0));
    event.registerSpriteSet(
        ModPresentation.SMOKE.get(),
        sprites ->
            (type, level, x, y, z, vx, vy, vz) ->
                new SpaceParticle(level, x, y, z, vx, vy, vz, sprites, 1));
    event.registerSpriteSet(
        ModPresentation.DUST.get(),
        sprites ->
            (type, level, x, y, z, vx, vy, vz) ->
                new SpaceParticle(level, x, y, z, vx, vy, vz, sprites, 2));
  }

  @SubscribeEvent
  public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
    ModAlienEntities.CRAWLERS.forEach(
        (body, type) ->
            event.registerEntityRenderer(
                type.get(), context -> new OriginalAlienRenderer<>(context, body + "_crawler")));
    event.registerBlockEntityRenderer(
        ModRocketRegistries.ROCKET.get(), FullScaleRocketRenderer::new);
    event.registerEntityRenderer(
        com.vic17usd.galaxycraft.expansion.ModExpansion.ROVER.get(), RoverRenderer::new);
    event.registerEntityRenderer(
        com.vic17usd.galaxycraft.frontier.ModFrontiers.GUARDIAN.get(),
        context -> new OriginalAlienRenderer<>(context, "lunar_guardian"));
    event.registerEntityRenderer(
        com.vic17usd.galaxycraft.frontier.ModFrontiers.QUEEN.get(),
        context -> new OriginalAlienRenderer<>(context, "martian_queen"));
    com.vic17usd.galaxycraft.frontier.ModFrontiers.RAIDERS.forEach(
        (body, type) ->
            event.registerEntityRenderer(
                type.get(), context -> new OriginalAlienRenderer<>(context, body + "_raider")));
    com.vic17usd.galaxycraft.biosphere.ModBiosphere.FAUNA.forEach(
        (id, type) ->
            event.registerEntityRenderer(
                type.get(), context -> new OriginalAlienRenderer<>(context, id)));
    com.vic17usd.galaxycraft.biosphere.ModBiosphere.CITIZENS.forEach(
        (id, type) ->
            event.registerEntityRenderer(
                type.get(), context -> new OriginalAlienRenderer<>(context, id + "_colonist")));
    com.vic17usd.galaxycraft.biosphere.ModBiosphere.HUNTERS.forEach(
        (id, type) ->
            event.registerEntityRenderer(
                type.get(), context -> new OriginalAlienRenderer<>(context, id + "_stalker")));
    event.registerEntityRenderer(
        com.vic17usd.galaxycraft.frontier.ModFrontiers.PLASMA_BOLT.get(),
        context ->
            new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, .65F, true));
    event.registerEntityRenderer(
        com.vic17usd.galaxycraft.frontier.ModFrontiers.METEOR.get(),
        context ->
            new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 1.5F, true));
  }

  @SubscribeEvent
  public static void models(EntityRenderersEvent.RegisterLayerDefinitions event) {
    event.registerLayerDefinition(FullScaleRocketRenderer.LAYER, FullScaleRocketRenderer::layer);
  }

  @SubscribeEvent
  public static void registerScreens(RegisterMenuScreensEvent event) {
    event.register(ModRocketRegistries.ROCKET_MENU.get(), RocketScreen::new);
    event.register(ModEquipment.MENU.get(), SpaceEquipmentScreen::new);
    event.register(ModRocketRegistries.NAVIGATION_MENU.get(), CelestialMapScreen::new);
    event.register(
        com.vic17usd.galaxycraft.expansion.ModExpansion.QUEST_MENU.get(), QuestScreen::new);
    event.register(
        com.vic17usd.galaxycraft.expansion.ModExpansion.MACHINE_MENU.get(),
        LogisticsScreens.Machine::new);
    event.register(
        com.vic17usd.galaxycraft.expansion.ModExpansion.ROVER_MENU.get(),
        LogisticsScreens.Rover::new);
    event.register(
        com.vic17usd.galaxycraft.frontier.ModFrontiers.MENU.get(), ResourceNetworkScreen::new);
    event.register(
        com.vic17usd.galaxycraft.biosphere.ModBiosphere.COLONY_MENU.get(), ColonyScreen::new);
  }

  @SubscribeEvent
  public static void orbitalEffects(
      net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
    event.register(ModPresentation.id("orbit"), new OrbitalEffects());
  }

  @SubscribeEvent
  public static void planetaryEffects(
      net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
    for (var profile : com.vic17usd.galaxycraft.biosphere.LivingProfiles.ALL)
      if (!profile.body().equals("earth"))
        event.register(ModPresentation.id("sky/" + profile.body()), new PlanetSkyEffects(profile));
  }
}
