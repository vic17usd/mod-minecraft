package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.registry.ModRocketRegistries;
import com.vic17usd.galaxycraft.registry.ModAlienEntities;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** El registro de pantallas se carga solo en cliente, nunca en servidor dedicado. */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        ModAlienEntities.CRAWLERS.values().forEach(type -> event.registerEntityRenderer(type.get(), SilverfishRenderer::new));
    }
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRocketRegistries.ROCKET_MENU.get(), RocketScreen::new);
    }
}
