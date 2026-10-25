package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.menu.SpaceEquipmentMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Panel separado de armadura, con etiquetas y botón de recarga validado por el servidor. */
public final class SpaceEquipmentScreen extends AbstractContainerScreen<SpaceEquipmentMenu> {
    public SpaceEquipmentScreen(SpaceEquipmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageHeight = 178; inventoryLabelY = 80;
    }
    @Override protected void init() {
        super.init(); addRenderableWidget(Button.builder(Component.translatable("gui.galaxycraft.refill_tanks"), button -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 8, topPos + 60, 160, 18).build());
    }
    @Override protected void renderBg(GuiGraphics gui, float partialTick, int x, int y) {
        gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF101E30);
        for (int i = 0; i < 8; i++) {
            int sx = leftPos + 9 + i * 20;
            gui.fill(sx, topPos + 37, sx + 18, topPos + 55, 0xFF42657A);
            gui.drawString(font, Component.translatable("gui.galaxycraft.slot." + i), sx, topPos + 26, 0x8CDDF2, false);
        }
        for (var slot : menu.slots.subList(8, menu.slots.size())) gui.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF344357);
    }
    @Override public void render(GuiGraphics gui, int x, int y, float partialTick) {
        super.render(gui, x, y, partialTick); renderTooltip(gui, x, y);
    }
}
