package com.vic17usd.galaxycraft.logistics;

import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;

/**
 * Divide cantidades grandes en dos palabras de 16 bits para no truncarlas en el protocolo vanilla.
 */
public final class LogisticsMenu extends AbstractContainerMenu {
  private final LogisticsMachine machine;
  private final BlockPos position;
  private final ContainerData data;

  public LogisticsMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
    this(id, inventory, null, buffer.readBlockPos());
  }

  public LogisticsMenu(int id, Inventory inventory, LogisticsMachine machine) {
    this(id, inventory, machine, machine.getBlockPos());
  }

  private LogisticsMenu(int id, Inventory player, LogisticsMachine machine, BlockPos pos) {
    super(ModFrontiers.MENU.get(), id);
    this.machine = machine;
    position = pos;
    data =
        machine == null
            ? new SimpleContainerData(8)
            : new ContainerData() {
              public int get(int i) {
                if (i == 6) return machine.kind();
                if (i == 7) return machine.progress;
                int value =
                    switch (i / 2) {
                      case 0 -> machine.energy.getEnergyStored();
                      case 1 -> machine.fuel.getFluidAmount();
                      default -> machine.oxygen.stored();
                    };
                return (i % 2 == 0 ? value : value >>> 16) & 65535;
              }

              public void set(int i, int value) {}

              public int getCount() {
                return 8;
              }
            };
    addSlot(
        new SlotItemHandler(
            machine == null ? new ItemStackHandler(1) : machine.inventory, 0, 80, 68));
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 106 + row * 18));
    for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 164));
    addDataSlots(data);
  }

  public int amount(int index) {
    return (data.get(index * 2) & 65535) | ((data.get(index * 2 + 1) & 65535) << 16);
  }

  public int kind() {
    return data.get(6);
  }

  public int progress() {
    return data.get(7);
  }

  @Override
  public boolean stillValid(Player player) {
    return player.isAlive()
        && player.distanceToSqr(position.getCenter()) <= 64
        && (machine == null
            ? player.level().isClientSide()
            : !machine.isRemoved()
                && player.level() == machine.getLevel()
                && player.level().getBlockEntity(position) == machine);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
    var slot = slots.get(index);
    if (!slot.hasItem()) return ItemStack.EMPTY;
    var stack = slot.getItem();
    var copy = stack.copy();
    if (!moveItemStackTo(stack, index == 0 ? 1 : 0, index == 0 ? 37 : 1, index == 0))
      return ItemStack.EMPTY;
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    slot.onTake(player, stack);
    return copy;
  }
}
