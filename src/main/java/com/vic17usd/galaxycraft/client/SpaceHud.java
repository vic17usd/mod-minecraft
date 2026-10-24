package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.item.OxygenTankItem;
import com.vic17usd.galaxycraft.registry.ModItems;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import com.vic17usd.galaxycraft.survival.SpaceSurvival;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.Locale;

/** Overlay de lectura: los Data Components sincronizados siguen siendo la única fuente de oxígeno. */
public final class SpaceHud {
    public static void render(GuiGraphics gui, DeltaTracker time) {
        var mc = Minecraft.getInstance(); var player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || !SpaceSurvival.isAirless(player.level())) return;
        var body = SolarSystem.body(player.level());
        // Long evita desbordamientos si un administrador crea tanques de capacidad extrema.
        long current = 0, maximum = 0;
        for (var stack : player.getInventory().items) if (stack.getItem() instanceof OxygenTankItem) {
            var oxygen = OxygenTankItem.oxygen(stack); current += oxygen.current(); maximum += oxygen.maximum();
        }
        if (player.getOffhandItem().getItem() instanceof OxygenTankItem) {
            var oxygen = OxygenTankItem.oxygen(player.getOffhandItem()); current += oxygen.current(); maximum += oxygen.maximum();
        }
        boolean helmet = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SPACE_HELMET.get());
        int color = !helmet || current == 0 ? 0xFF6666 : 0x70E4FF;
        // Esquina superior izquierda: no tapa vida, hambre ni barra rápida.
        int width = Math.min(238, gui.guiWidth() - 12);
        gui.fill(6, 6, 6 + width, 65, 0xBB091322);
        gui.drawString(mc.font, body == null ? player.level().dimension().location().toString()
                : Component.translatable("celestial.galaxycraft." + body.id()).getString(), 12, 11, 0xFFFFFF);
        gui.drawString(mc.font, Component.translatable("hud.galaxycraft.oxygen", current / 60, current % 60), 12, 23, color);
        gui.fill(12, 35, width, 39, 0xFF293341);
        int filled = maximum == 0 ? 0 : (int) ((width - 12) * ((double) current / maximum));
        gui.fill(12, 35, 12 + filled, 39, 0xFF000000 | color);
        // Presión nominal de diseño; no se presenta como una simulación meteorológica.
        String pressure = body == null ? "?" : String.format(Locale.ROOT, "%.2f", nominalPressure(body.id()));
        gui.drawString(mc.font, Component.translatable("hud.galaxycraft.environment", player.blockPosition().getY(), pressure), 12, 43, 0xCCCCCC);
        if (!helmet) gui.drawString(mc.font, Component.translatable("hud.galaxycraft.helmet"), 12, 54, color);
    }
    public static double nominalPressure(String body) {
        return switch (body) {
            case "earth" -> 101.3; case "venus" -> 9200; case "mars" -> .61; case "titan" -> 146.7;
            case "jupiter", "saturn", "uranus", "neptune" -> 100;
            default -> 0;
        };
    }
    private SpaceHud() {}
}
