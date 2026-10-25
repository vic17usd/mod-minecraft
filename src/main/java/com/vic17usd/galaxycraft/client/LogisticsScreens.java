package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.expansion.MachineMenu;
import com.vic17usd.galaxycraft.rover.RoverMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Pantallas de logística con las mismas posiciones de ranuras que sus menús autoritativos. */
public final class LogisticsScreens {
  private abstract static class Panel<T extends AbstractContainerMenu>
      extends AbstractContainerScreen<T> {
    Panel(T menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      imageHeight = 188;
      inventoryLabelY = 88;
    }

    abstract int energy();

    @Override
    protected void renderBg(GuiGraphics gui, float partial, int x, int y) {
      gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF14283A);
      for (var slot : menu.slots)
        gui.fill(
            leftPos + slot.x - 1,
            topPos + slot.y - 1,
            leftPos + slot.x + 17,
            topPos + slot.y + 17,
            0xFF4B6277);
      gui.drawString(
          font,
          Component.translatable("expansion.galaxycraft.energy", energy()),
          leftPos + 8,
          topPos + 22,
          0x82DCF1,
          false);
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, float partial) {
      super.render(gui, x, y, partial);
      renderTooltip(gui, x, y);
    }
  }

  public static final class Machine extends Panel<MachineMenu> {
    public Machine(MachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
    }

    int energy() {
      return menu.value(0);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partial, int x, int y) {
      super.renderBg(gui, partial, x, y);
      gui.drawString(
          font,
          Component.translatable("expansion.galaxycraft.machine." + menu.value(1)),
          leftPos + 8,
          topPos + 34,
          0xFFFFFF,
          false);
      if (menu.value(1) == 0) {
        gui.drawString(
            font,
            Component.translatable("expansion.galaxycraft.seal." + menu.value(2)),
            leftPos + 8,
            topPos + 69,
            0xA5DCC0,
            false);
        gui.drawString(
            font,
            Component.translatable("expansion.galaxycraft.oxygen", menu.value(3)),
            leftPos + 8,
            topPos + 80,
            0xABD6EF,
            false);
      }
    }
  }

  public static final class Rover extends Panel<RoverMenu> {
    public Rover(RoverMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      inventoryLabelY = 93;
    }

    int energy() {
      return menu.energy();
    }
  }

  private LogisticsScreens() {}
}
