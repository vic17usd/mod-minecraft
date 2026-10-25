package com.vic17usd.galaxycraft.quest;

import com.vic17usd.galaxycraft.expansion.ExpansionNetworking;
import com.vic17usd.galaxycraft.expansion.ModExpansion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

/**
 * Autoridad sobre entregas y recompensas; la mochila llena entrega el excedente a los pies del
 * jugador.
 */
public final class QuestService {
  public static QuestState state(ServerPlayer player) {
    return player.getData(ModExpansion.QUESTS.get());
  }

  public static int count(ServerPlayer player, String id) {
    var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    int total = 0;
    for (var stack : player.getInventory().items) if (stack.is(item)) total += stack.getCount();
    if (player.getOffhandItem().is(item)) total += player.getOffhandItem().getCount();
    return total;
  }

  public static boolean unlocked(QuestState state, QuestCatalog.Quest quest) {
    return quest.prerequisite().isEmpty() || state.completed.contains(quest.prerequisite());
  }

  public static int progress(ServerPlayer player, QuestCatalog.Quest quest) {
    var state = state(player);
    return Math.min(
        quest.count(),
        switch (quest.type()) {
          case "deliver" -> count(player, quest.target());
          case "visit" -> state.visited.contains(quest.target()) ? 1 : 0;
          case "build" -> state.built.contains(quest.target()) ? 1 : 0;
          case "drive" -> state.drivenMillimeters / 1000;
          default -> 0;
        });
  }

  public static boolean finish(ServerPlayer player, QuestCatalog.Quest quest, boolean delivery) {
    var state = state(player);
    if (state.completed.contains(quest.id())
        || !unlocked(state, quest)
        || progress(player, quest) < quest.count()
        || quest.type().equals("deliver") && !delivery) return false;
    if (!state.completeOnce(quest.id())) return false;
    if (quest.type().equals("deliver")) {
      int remaining = quest.count();
      var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(quest.target()));
      for (var stack : player.getInventory().items)
        if (stack.is(item) && remaining > 0) {
          int used = Math.min(remaining, stack.getCount());
          stack.shrink(used);
          remaining -= used;
        }
      if (remaining > 0) player.getOffhandItem().shrink(remaining);
    }
    for (var reward : quest.rewards()) {
      var stack =
          new ItemStack(
              BuiltInRegistries.ITEM.get(ResourceLocation.parse(reward.item())), reward.count());
      if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
    player.giveExperiencePoints(quest.xp());
    player.inventoryMenu.broadcastChanges();
    player.displayClientMessage(
        Component.translatable(
            "quest.galaxycraft.completed",
            Component.translatable("quest.galaxycraft." + quest.id())),
        false);
    sync(player);
    return true;
  }

  public static void sync(ServerPlayer player) {
    var tag = state(player).serializeNBT(player.registryAccess());
    var list = new ListTag();
    for (var quest : QuestCatalog.ALL) {
      var value = new CompoundTag();
      value.putString("id", quest.id());
      value.putInt(
          "current",
          state(player).completed.contains(quest.id()) ? quest.count() : progress(player, quest));
      value.putInt("goal", quest.count());
      value.putInt(
          "status",
          state(player).completed.contains(quest.id())
              ? 3
              : !unlocked(state(player), quest)
                  ? 0
                  : progress(player, quest) >= quest.count() ? 2 : 1);
      list.add(value);
    }
    tag.put("Views", list);
    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
        player, new ExpansionNetworking.QuestSnapshot(tag));
  }

  public static void open(ServerPlayer player) {
    if (!player.isAlive() || player.isSpectator() || player.containerMenu != player.inventoryMenu)
      return;
    player.openMenu(
        new SimpleMenuProvider(
            (id, inventory, owner) -> new QuestMenu(id, inventory),
            Component.translatable("quest.galaxycraft.title")));
    sync(player);
  }

  private QuestService() {}
}
