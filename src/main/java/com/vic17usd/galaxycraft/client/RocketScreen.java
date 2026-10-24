package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.menu.RocketMenu;
import com.vic17usd.galaxycraft.rocket.RocketFueling;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;
import com.vic17usd.galaxycraft.solar.SolarSystem;

/** Interfaz provisional dibujada sin texturas externas de GUI. */
public final class RocketScreen extends AbstractContainerScreen<RocketMenu> {
    private Button launch;
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.galaxycraft.next"), button ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)).bounds(leftPos + 8, topPos + 198, 100, 20).build());
        launch = addRenderableWidget(Button.builder(Component.translatable("gui.galaxycraft.launch"), button ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1)).bounds(leftPos + 112, topPos + 198, 56, 20).build());
    }
    public RocketScreen(RocketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 224;
        inventoryLabelY = 102;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        launch.active = menu.readiness() == 3 && SolarSystem.accessible(menu.tier(), SolarSystem.destination(menu.destination()));
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFBBC2CA);
        for (var slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1,
                    leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF555B63);
        }
        graphics.fill(leftPos + 79, topPos + 21, leftPos + 97, topPos + 57, 0xFF30343A);
        int height = 34 * Math.clamp(menu.fuelAmount(), 0, RocketFueling.CAPACITY) / RocketFueling.CAPACITY;
        graphics.fill(leftPos + 80, topPos + 56 - height, leftPos + 96, topPos + 56, 0xFFFF9B20);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.galaxycraft.fuel", menu.fuelAmount(), RocketFueling.CAPACITY), 8, 63, 0x303030, false);
        graphics.drawString(font, Component.translatable("gui.galaxycraft.status." + menu.readiness()), 8, 78, 0x303030, false);
        graphics.drawString(font, Component.translatable("gui.galaxycraft.destination",
                SolarSystem.destination(menu.destination()).name(), menu.tier()), 8, 90, 0x303030, false);
    }
}
