package com.vic17usd.galaxycraft.rover;

import com.vic17usd.galaxycraft.expansion.ModExpansion;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;

/**
 * Contenedor validado por identidad de entidad, nivel y distancia; nueve muestras y una batería.
 */
public final class RoverMenu extends AbstractContainerMenu {
  private final RoverEntity rover;
  private final ContainerData data;

  public RoverMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
    this(
        id,
        inventory,
        inventory.player.level().getEntity(buffer.readInt()) instanceof RoverEntity entity
            ? entity
            : null);
  }

  public RoverMenu(int id, Inventory player, RoverEntity rover) {
    super(ModExpansion.ROVER_MENU.get(), id);
    this.rover = rover;
    var inventory = rover == null ? new ItemStackHandler(10) : rover.inventory;
    for (int i = 0; i < 9; i++) addSlot(new SlotItemHandler(inventory, i, 8 + i * 18, 44));
    addSlot(new SlotItemHandler(inventory, 9, 80, 68));
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 105 + row * 18));
    for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 163));
    data =
        rover == null
            ? new SimpleContainerData(1)
            : new ContainerData() {
              public int get(int i) {
                return rover.energy();
              }

              public void set(int i, int v) {}

              public int getCount() {
                return 1;
              }
            };
    addDataSlots(data);
  }

  public int energy() {
    return data.get(0);
  }

  @Override
  public boolean stillValid(Player player) {
    return rover == null
        ? player.level().isClientSide()
        : player.isAlive()
            && !rover.isRemoved()
            && rover.level() == player.level()
            && player.distanceToSqr(rover) <= 64;
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
    var slot = slots.get(index);
    if (!slot.hasItem()) return ItemStack.EMPTY;
    var stack = slot.getItem();
    var copy = stack.copy();
    if (index < 10) {
      if (!moveItemStackTo(stack, 10, 46, true)) return ItemStack.EMPTY;
    } else if (stack.is(ModExpansion.BATTERY.get())) {
      if (!moveItemStackTo(stack, 9, 10, false)) return ItemStack.EMPTY;
    } else if (!moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    slot.onTake(player, stack);
    return copy;
  }
}
