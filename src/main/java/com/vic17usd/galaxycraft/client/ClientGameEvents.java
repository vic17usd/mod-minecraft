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

/** Visuales y controles locales: envían intenciones, nunca inventarios, recompensas ni posiciones. */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    @SubscribeEvent public static void equipmentKey(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        while(EquipmentKey.QUESTS.consumeClick())if(Minecraft.getInstance().player!=null&&Minecraft.getInstance().screen==null)
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.vic17usd.galaxycraft.expansion.ExpansionNetworking.OpenQuests());
        var mc=Minecraft.getInstance();
        if(mc.player!=null&&mc.player.tickCount%3==0){
            boolean controls=mc.screen==null;float forward=controls?(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0):0;
            float turn=controls?(mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0):0;
            int thrust=controls?(mc.options.keyUp.isDown()?1:0)|(mc.options.keyDown.isDown()?2:0)|(mc.options.keyLeft.isDown()?4:0)|(mc.options.keyRight.isDown()?8:0)|(mc.options.keyJump.isDown()?16:0)|(mc.options.keyShift.isDown()?32:0):0;
            if(mc.player.getVehicle() instanceof com.vic17usd.galaxycraft.rover.RoverEntity rover)
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.vic17usd.galaxycraft.expansion.ExpansionNetworking.Controls(rover.getId(),forward,turn,0));
            else if(com.vic17usd.galaxycraft.expansion.AdvancedSurvival.zeroG(mc.player))
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.vic17usd.galaxycraft.expansion.ExpansionNetworking.Controls(-1,0,0,thrust));
        }
        while (EquipmentKey.NAVIGATE.consumeClick()) if (Minecraft.getInstance().player != null
                && (Minecraft.getInstance().screen == null || Minecraft.getInstance().screen instanceof RocketScreen))
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.vic17usd.galaxycraft.navigation.NavigationNetworking.Open());
        while (EquipmentKey.OPEN.consumeClick()) if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null)
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.vic17usd.galaxycraft.equipment.EquipmentNetworking.Open());
    }
    @SubscribeEvent public static void equipmentButton(net.neoforged.neoforge.client.event.ScreenEvent.Init.Post event) {
        var screen = event.getScreen();
        if (screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)
            event.addListener(net.minecraft.client.gui.components.Button.builder(Component.translatable("gui.galaxycraft.suit_button"), button ->
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.vic17usd.galaxycraft.equipment.EquipmentNetworking.Open()))
                    .bounds(screen.width / 2 + 90, screen.height / 2 - 80, 52, 20).build());
    }
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
