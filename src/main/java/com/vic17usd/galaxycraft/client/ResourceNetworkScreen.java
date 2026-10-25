package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.logistics.LogisticsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Lectura exacta de depósitos grandes mediante propiedades de menú divididas en 16 bits. */
public final class ResourceNetworkScreen extends AbstractContainerScreen<LogisticsMenu> {
  public ResourceNetworkScreen(LogisticsMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageHeight = 188;
    inventoryLabelY = 94;
  }

  @Override
  protected void renderBg(GuiGraphics gui, float partial, int x, int y) {
    gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF14283A);
    for (var slot : menu.slots)
      gui.fill(
          leftPos + slot.x - 1,
          topPos + slot.y - 1,
          leftPos + slot.x + 17,
          topPos + slot.y + 17,
          0xFF456379);
    int kind = menu.kind();
    if (kind == 4 || kind == 5)
      gui.drawString(
          font,
          Component.translatable("network.galaxycraft.energy", menu.amount(0)),
          leftPos + 8,
          topPos + 24,
          0x8DE5EF,
          false);
    if (kind == 3 || kind == 5)
      gui.drawString(
          font,
          Component.translatable("network.galaxycraft.fuel", menu.amount(1)),
          leftPos + 8,
          topPos + 38,
          0xFFC778,
          false);
    if (kind == 6)
      gui.drawString(
          font,
          Component.translatable("network.galaxycraft.oxygen", menu.amount(2)),
          leftPos + 8,
          topPos + 38,
          0x8DE5EF,
          false);
    if (kind == 5) {
      gui.fill(leftPos + 8, topPos + 55, leftPos + 168, topPos + 59, 0xFF293849);
      gui.fill(
          leftPos + 8,
          topPos + 55,
          leftPos + 8 + 160 * menu.progress() / 100,
          topPos + 59,
          0xFF72C7A3);
    }
  }

  @Override
  public void render(GuiGraphics gui, int x, int y, float partial) {
    super.render(gui, x, y, partial);
    renderTooltip(gui, x, y);
  }
}
