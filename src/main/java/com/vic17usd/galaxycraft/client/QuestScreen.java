package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.expansion.*;
import com.vic17usd.galaxycraft.quest.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Libro de contratos: explora fases bloqueadas, ve progreso y entrega solo con sesión validada. */
public final class QuestScreen extends AbstractContainerScreen<QuestMenu> {
  private int phase;
  private final Map<String, Button> deliver = new HashMap<>();

  public QuestScreen(QuestMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
  }

  @Override
  protected void init() {
    imageWidth = Math.min(420, width - 12);
    imageHeight = Math.min(250, height - 12);
    super.init();
    rebuild();
  }

  private void rebuild() {
    clearWidgets();
    deliver.clear();
    for (int i = 0; i < 4; i++) {
      final int selected = i;
      addRenderableWidget(
          Button.builder(
                  Component.translatable("quest.galaxycraft.phase_short." + i),
                  button -> {
                    phase = selected;
                    rebuild();
                  })
              .bounds(
                  leftPos + 6 + i * (imageWidth - 12) / 4,
                  topPos + 25,
                  (imageWidth - 12) / 4 - 3,
                  20)
              .tooltip(Tooltip.create(Component.translatable("quest.galaxycraft.phase." + i)))
              .build());
    }
    int row = 0;
    for (var quest : QuestCatalog.ALL)
      if (quest.phase() == phase) {
        int y = topPos + 55 + row++ * 39;
        var button =
            addRenderableWidget(
                Button.builder(
                        Component.translatable("quest.galaxycraft.deliver"),
                        pressed ->
                            PacketDistributor.sendToServer(
                                new ExpansionNetworking.Submit(menu.containerId, quest.id())))
                    .bounds(leftPos + imageWidth - 72, y + 3, 64, 18)
                    .build());
        var tooltip =
            Component.translatable("quest.galaxycraft.requirement." + quest.id())
                .append("\n")
                .append(Component.translatable("quest.galaxycraft.reward", quest.xp()));
        for (var reward : quest.rewards())
          tooltip
              .append("\n")
              .append(
                  Component.translatable(
                      "quest.galaxycraft.reward_item",
                      reward.count(),
                      new net.minecraft.world.item.ItemStack(
                              net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                                  net.minecraft.resources.ResourceLocation.parse(reward.item())))
                          .getHoverName()));
        button.setTooltip(Tooltip.create(tooltip));
        deliver.put(quest.id(), button);
      }
  }

  private QuestState.View view(String id) {
    return minecraft
        .player
        .getData(ModExpansion.QUESTS.get())
        .views
        .getOrDefault(id, new QuestState.View(0, 1, 0));
  }

  @Override
  public void render(GuiGraphics gui, int x, int y, float partial) {
    for (var quest : QuestCatalog.ALL) {
      var button = deliver.get(quest.id());
      if (button != null) {
        var value = view(quest.id());
        button.active = value.status() == 2 && quest.type().equals("deliver");
        button.setMessage(
            Component.translatable(
                "quest.galaxycraft."
                    + (value.status() == 3
                        ? "done"
                        : value.status() == 0
                            ? "locked"
                            : quest.type().equals("deliver") ? "deliver" : "automatic")));
      }
    }
    super.render(gui, x, y, partial);
    renderTooltip(gui, x, y);
  }

  @Override
  protected void renderLabels(GuiGraphics gui, int x, int y) {}

  @Override
  protected void renderBg(GuiGraphics gui, float partial, int x, int y) {
    gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFA0C1C2C);
    gui.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF60A4BC);
    gui.drawString(font, title, leftPos + 8, topPos + 8, 0xA9ECFF, false);
    int row = 0;
    for (var quest : QuestCatalog.ALL)
      if (quest.phase() == phase) {
        int sy = topPos + 55 + row++ * 39;
        var value = view(quest.id());
        gui.fill(leftPos + 6, sy, leftPos + imageWidth - 6, sy + 35, 0xFF193347);
        gui.drawString(
            font,
            font.plainSubstrByWidth(
                Component.translatable("quest.galaxycraft." + quest.id()).getString(),
                imageWidth - 88),
            leftPos + 12,
            sy + 3,
            value.status() == 3 ? 0x7FE0A1 : 0xEDF6FF,
            false);
        gui.drawString(
            font,
            Component.translatable("quest.galaxycraft.progress", value.current(), quest.count()),
            leftPos + 12,
            sy + 16,
            0xA8BACB,
            false);
        int bar = imageWidth - 96;
        gui.fill(leftPos + 12, sy + 28, leftPos + 12 + bar, sy + 31, 0xFF283B4B);
        gui.fill(
            leftPos + 12,
            sy + 28,
            leftPos
                + 12
                + bar * Math.min(value.current(), quest.count()) / Math.max(1, quest.count()),
            sy + 31,
            value.status() == 3 ? 0xFF5BCFA0 : 0xFF65BBDF);
      }
  }
}
