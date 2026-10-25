package com.vic17usd.galaxycraft.logistics;

import com.vic17usd.galaxycraft.expansion.*;
import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import com.vic17usd.galaxycraft.item.OxygenTankItem;
import com.vic17usd.galaxycraft.registry.ModDataComponents;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Almacenamiento persistente; la refinería térmica convierte carbón y FE en lava compatible con
 * cohetes.
 */
public final class LogisticsMachine extends BlockEntity implements MenuProvider {
  public int progress;

  public final class StoredEnergy extends EnergyStorage {
    private StoredEnergy() {
      super(1000000, 4000, 4000);
    }

    public void restore(int value) {
      energy = Math.clamp(value, 0, 1000000);
      setChanged();
    }

    @Override
    public int receiveEnergy(int max, boolean simulate) {
      int result = super.receiveEnergy(max, simulate);
      if (result > 0 && !simulate) setChanged();
      return result;
    }

    @Override
    public int extractEnergy(int max, boolean simulate) {
      int result = super.extractEnergy(max, simulate);
      if (result > 0 && !simulate) setChanged();
      return result;
    }
  }

  public final StoredEnergy energy = new StoredEnergy();
  public final FluidTank fuel =
      new FluidTank(128000, stack -> stack.is(Fluids.LAVA)) {
        @Override
        protected void onContentsChanged() {
          setChanged();
        }
      };
  private int oxygenStored;
  public final IOxygenStorage oxygen =
      new IOxygenStorage() {
        public int stored() {
          return oxygenStored;
        }

        public int capacity() {
          return 128000;
        }

        public int receive(int max, boolean simulate) {
          int moved = Math.clamp(max, 0, capacity() - stored());
          if (!simulate && moved > 0) {
            oxygenStored += moved;
            setChanged();
          }
          return moved;
        }

        public int extract(int max, boolean simulate) {
          int moved = Math.clamp(max, 0, stored());
          if (!simulate && moved > 0) {
            oxygenStored -= moved;
            setChanged();
          }
          return moved;
        }
      };
  public final ItemStackHandler inventory =
      new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
          return switch (kind()) {
            case 4 -> stack.is(ModExpansion.BATTERY.get());
            case 5 -> stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
            case 6 -> stack.getItem() instanceof OxygenTankItem;
            default -> false;
          };
        }

        @Override
        public int getSlotLimit(int slot) {
          return kind() == 5 ? 64 : 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
          setChanged();
        }
      };

  public LogisticsMachine(BlockPos pos, BlockState state) {
    super(ModFrontiers.MACHINE.get(), pos, state);
  }

  public int kind() {
    return ((LogisticsBlock) getBlockState().getBlock()).kind;
  }

  @Override
  public void onLoad() {
    super.onLoad();
    if (level != null && !level.isClientSide()) ResourceNetworks.add(this);
  }

  @Override
  public void setRemoved() {
    ResourceNetworks.remove(this);
    super.setRemoved();
  }

  public static void tick(Level level, BlockPos pos, BlockState state, LogisticsMachine machine) {
    var stack = machine.inventory.getStackInSlot(0);
    int kind = machine.kind();
    if (kind == 4 && stack.is(ModExpansion.BATTERY.get())) {
      int amount =
          Math.min(6000 - BatteryItem.charge(stack), machine.energy.extractEnergy(100, true));
      if (amount > 0) {
        machine.energy.extractEnergy(amount, false);
        BatteryItem.charge(stack, BatteryItem.charge(stack) + amount);
        machine.setChanged();
      }
    }
    if (kind == 6 && stack.getItem() instanceof OxygenTankItem) {
      var oxygen = OxygenTankItem.oxygen(stack);
      int moved = machine.oxygen.receive(Math.min(60, oxygen.current()), false);
      if (moved > 0) {
        stack.set(ModDataComponents.OXYGEN_LEVEL.get(), oxygen.consume(moved));
        machine.setChanged();
      }
    }
    if (kind == 5
        && !stack.isEmpty()
        && machine.inventory.isItemValid(0, stack)
        && machine.energy.getEnergyStored() >= 20
        && machine.fuel.getSpace() >= 1000) {
      machine.energy.extractEnergy(20, false);
      machine.progress++;
      if (machine.progress >= 100) {
        stack.shrink(1);
        machine.fuel.fill(
            new FluidStack(Fluids.LAVA, 1000),
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        machine.progress = 0;
      }
      machine.setChanged();
    }
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    tag.putInt("Energy", energy.getEnergyStored());
    tag.putInt("Oxygen", oxygenStored);
    tag.putInt("Progress", progress);
    tag.put("Fuel", fuel.writeToNBT(lookup, new CompoundTag()));
    tag.put("Inventory", inventory.serializeNBT(lookup));
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.loadAdditional(tag, lookup);
    energy.restore(tag.getInt("Energy"));
    oxygenStored = Math.clamp(tag.getInt("Oxygen"), 0, 128000);
    progress = Math.clamp(tag.getInt("Progress"), 0, 99);
    fuel.readFromNBT(lookup, tag.getCompound("Fuel"));
    if (!fuel.isEmpty() && !fuel.isFluidValid(fuel.getFluid())) fuel.setFluid(FluidStack.EMPTY);
    else if (fuel.getFluidAmount() > 128000) fuel.setFluid(fuel.getFluid().copyWithAmount(128000));
    var items = tag.getCompound("Inventory").copy();
    items.putInt("Size", 1);
    inventory.deserializeNBT(lookup, items);
  }

  @Override
  public Component getDisplayName() {
    return getBlockState().getBlock().getName();
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new LogisticsMenu(id, inventory, this);
  }
}
