package com.vic17usd.galaxycraft.quest;

import com.vic17usd.galaxycraft.expansion.ModExpansion;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Menú personal sin ranuras: entregar requiere la sesión correcta y requisitos recalculados. */
public final class QuestMenu extends AbstractContainerMenu {
  public QuestMenu(int id, Inventory inventory) {
    super(ModExpansion.QUEST_MENU.get(), id);
  }

  @Override
  public boolean stillValid(Player player) {
    return player.isAlive() && !player.isSpectator();
  }

  @Override
  public ItemStack quickMoveStack(Player player, int slot) {
    return ItemStack.EMPTY;
  }
}
