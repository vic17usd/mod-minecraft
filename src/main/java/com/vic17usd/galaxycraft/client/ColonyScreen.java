package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.biosphere.LivingProfiles;
import com.vic17usd.galaxycraft.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Diálogo de comercio/contrato; los botones se actualizan después de cada snapshot de menú. */
public final class ColonyScreen extends AbstractContainerScreen<ColonyMenu> {
  private final Button[] actions = new Button[6];

  public ColonyScreen(ColonyMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
  }

  @Override
  protected void init() {
    imageWidth = Math.min(340, width - 12);
    imageHeight = 226;
    super.init();
    String[] names = {"buy", "sell", "accept", "claim", "supplies", "blueprint"};
    for (int i = 0; i < 6; i++) {
      final int action = i;
      actions[i] =
          addRenderableWidget(
              Button.builder(
                      Component.translatable("colony.galaxycraft." + names[i]),
                      button ->
                          PacketDistributor.sendToServer(
                              new ColonyNetworking.Action(menu.containerId, action)))
                  .bounds(
                      leftPos + 8 + (i % 2) * (imageWidth / 2 - 4),
                      topPos + 49 + (i / 2) * 39,
                      imageWidth / 2 - 12,
                      34)
                  .tooltip(
                      Tooltip.create(
                          Component.translatable(
                              "colony.galaxycraft."
                                  + (i < 2 || i >= 4 ? names[i] + "_tip" : "contract_tip"))))
                  .build());
    }
  }

  @Override
  public void render(GuiGraphics gui, int x, int y, float partial) {
    actions[0].active = menu.value(0) > 0 && menu.value(4) >= 4;
    actions[1].active = menu.value(0) > 0 && menu.value(3) >= 8;
    actions[2].active = menu.value(1) == 0 && menu.value(2) == 0;
    actions[3].active = menu.value(1) > 0 && menu.value(2) == 0 && menu.value(3) >= 16;
    actions[4].active = menu.value(0) > 0 && menu.value(5) > 0;
    actions[5].active = menu.value(0) > 0 && menu.value(5) >= 4;
    super.render(gui, x, y, partial);
    renderTooltip(gui, x, y);
  }

  @Override
  protected void renderLabels(GuiGraphics gui, int x, int y) {}

  @Override
  protected void renderBg(GuiGraphics gui, float partial, int x, int y) {
    gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFA101F32);
    gui.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF559AAB);
    var name = LivingProfiles.get(menu.body()).celestial().name();
    gui.drawString(
        font,
        Component.translatable("colony.galaxycraft.title", name),
        leftPos + 10,
        topPos + 10,
        0x96E9F1,
        false);
    gui.drawString(
        font,
        Component.translatable("colony.galaxycraft.stock", menu.value(0)),
        leftPos + 10,
        topPos + 29,
        0xD0DFE6,
        false);
    gui.drawString(
        font,
        Component.translatable(
            menu.value(2) > 0 ? "colony.galaxycraft.done" : "colony.galaxycraft.progress",
            menu.value(3)),
        leftPos + 10,
        topPos + 187,
        0x99DFB8,
        false);
    gui.fill(leftPos + 10, topPos + 205, leftPos + imageWidth - 10, topPos + 209, 0xFF354559);
    gui.fill(
        leftPos + 10,
        topPos + 205,
        leftPos + 10 + (imageWidth - 20) * Math.min(16, menu.value(3)) / 16,
        topPos + 209,
        0xFF5FBBAA);
  }
}
