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
import com.vic17usd.galaxycraft.equipment.SpaceEquipment;

/** Overlay de lectura: los Data Components sincronizados siguen siendo la única fuente de oxígeno. */
public final class SpaceHud {
    public static void render(GuiGraphics gui, DeltaTracker time) {
        var mc = Minecraft.getInstance(); var player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || !SpaceSurvival.isAirless(player.level())) return;
        var body = SolarSystem.body(player.level());
        var exposure=player.getData(com.vic17usd.galaxycraft.expansion.ModExpansion.EXPOSURE.get());
        // Long evita desbordamientos si un administrador crea tanques de capacidad extrema.
        long current = 0, maximum = 0;
        for (var stack : SpaceEquipment.tanks(player)) {
            var oxygen = OxygenTankItem.oxygen(stack); current += oxygen.current(); maximum += oxygen.maximum();
        }
        boolean helmet = SpaceEquipment.hasHelmet(player);
        if(exposure.sheltered){current=exposure.roomOxygen;maximum=exposure.roomCapacity;}
        int color = !exposure.sheltered&&!helmet || current == 0 ? 0xFF6666 : 0x70E4FF;
        // Esquina superior izquierda: no tapa vida, hambre ni barra rápida.
        int width = Math.min(238, gui.guiWidth() - 12);
        gui.fill(6, 6, 6 + width, 65, 0xBB091322);
        gui.drawString(mc.font, body == null ? player.level().dimension().location().toString()
                : Component.translatable("celestial.galaxycraft." + body.id()).getString(), 12, 11, 0xFFFFFF);
        gui.drawString(mc.font, Component.translatable(exposure.sheltered?"expansion.galaxycraft.room_reserve":"hud.galaxycraft.oxygen", current / 60, current % 60), 12, 23, color);
        gui.fill(12, 35, width, 39, 0xFF293341);
        int filled = maximum == 0 ? 0 : (int) ((width - 12) * ((double) current / maximum));
        gui.fill(12, 35, 12 + filled, 39, 0xFF000000 | color);
        // Presión nominal de diseño; no se presenta como una simulación meteorológica.
        String pressure = exposure.sheltered?"101.30":body == null ? com.vic17usd.galaxycraft.station.OrbitStations.isOrbit(player.level())?"0.00":"?" : String.format(Locale.ROOT, "%.2f", nominalPressure(body.id()));
        gui.drawString(mc.font, Component.translatable("hud.galaxycraft.environment", player.blockPosition().getY(), pressure), 12, 43, 0xCCCCCC);
        if (!helmet&&!exposure.sheltered) gui.drawString(mc.font, Component.translatable("hud.galaxycraft.helmet"), 12, 54, color);
        gui.fill(6,68,6+width,92,0xBB091322);
        gui.drawString(mc.font,Component.translatable("expansion.galaxycraft.environment",exposure.temperature,exposure.radiation),12,72,0xD5DEE8);
        gui.drawString(mc.font,Component.translatable("expansion.galaxycraft.shelter."+exposure.sheltered),12,83,exposure.sheltered?0x78DCA4:0xE2AC79);
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
