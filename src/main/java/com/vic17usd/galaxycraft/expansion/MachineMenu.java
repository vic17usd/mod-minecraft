package com.vic17usd.galaxycraft.expansion;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;

/**
 * Energía, sellado y oxígeno usan propiedades de menú, no valores proporcionados por el cliente.
 */
public final class MachineMenu extends AbstractContainerMenu {
  private final ExpansionMachine machine;
  private final ContainerData data;

  public MachineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
    this(id, inventory, null, new SimpleContainerData(4), buffer.readBlockPos());
  }

  public MachineMenu(int id, Inventory inventory, ExpansionMachine machine) {
    this(
        id,
        inventory,
        machine,
        new ContainerData() {
          public int get(int i) {
            return switch (i) {
              case 0 -> machine.energy.getEnergyStored();
              case 1 -> machine.kind();
              case 2 -> machine.kind() == 0 && machine.room().sealed() ? 1 : 0;
              default ->
                  machine.kind() == 0
                      ? com.vic17usd.galaxycraft.item.OxygenTankItem.oxygen(
                              machine.inventory.getStackInSlot(0))
                          .current()
                      : BatteryItem.charge(machine.inventory.getStackInSlot(0));
            };
          }

          public void set(int i, int value) {}

          public int getCount() {
            return 4;
          }
        },
        machine.getBlockPos());
  }

  private final net.minecraft.core.BlockPos position;

  private MachineMenu(
      int id,
      Inventory player,
      ExpansionMachine machine,
      ContainerData data,
      net.minecraft.core.BlockPos pos) {
    super(ModExpansion.MACHINE_MENU.get(), id);
    this.machine = machine;
    this.data = data;
    position = pos;
    addSlot(
        new SlotItemHandler(
            machine == null ? new ItemStackHandler(1) : machine.inventory, 0, 80, 44));
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 100 + row * 18));
    for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 158));
    addDataSlots(data);
  }

  public int value(int index) {
    return data.get(index);
  }

  @Override
  public boolean stillValid(Player player) {
    return player.isAlive()
        && player.distanceToSqr(position.getX() + .5, position.getY() + .5, position.getZ() + .5)
            <= 64
        && (machine == null
            || !machine.isRemoved() && player.level().getBlockEntity(position) == machine);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
    var slot = slots.get(index);
    if (!slot.hasItem()) return ItemStack.EMPTY;
    var stack = slot.getItem();
    var copy = stack.copy();
    if (index == 0) {
      if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
    } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    slot.onTake(player, stack);
    return copy;
  }
}
