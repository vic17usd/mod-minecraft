package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/** Eventos visuales locales: no envían paquetes ni alteran inventarios desde el cliente. */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    @SubscribeEvent
    public static void dust(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (!player.level().isClientSide() || player != Minecraft.getInstance().player || Minecraft.getInstance().isPaused()) return;
        var body = SolarSystem.body(player.level());
        if (body == null || body.gravity() >= .4 || !player.onGround() || player.tickCount % 5 != 0
                || player.getDeltaMovement().horizontalDistanceSqr() < .001) return;
        var random = player.getRandom();
        player.level().addParticle(ModPresentation.DUST.get(), player.getX() + random.nextDouble() - .5,
                player.getY() + .04, player.getZ() + random.nextDouble() - .5, 0, .015, 0);
    }
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        var id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!id.getNamespace().equals(GalaxyCraft.MOD_ID)) return;
        String key = "tooltip.galaxycraft.detail." + id.getPath();
        if (net.minecraft.client.resources.language.I18n.exists(key))
            event.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
    private ClientGameEvents() {}
}
