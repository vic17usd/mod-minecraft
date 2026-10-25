package com.vic17usd.galaxycraft.expansion;

import com.vic17usd.galaxycraft.item.OxygenTankItem;
import com.vic17usd.galaxycraft.rover.RoverEntity;
import com.vic17usd.galaxycraft.station.OrbitStations;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Energía NeoForge real, tanque finito y escaneo acotado. Ninguna comprobación genera terreno
 * remoto.
 */
public final class ExpansionMachine extends BlockEntity implements MenuProvider {
  public final class StoredEnergy extends EnergyStorage {
    private StoredEnergy() {
      super(30000, 1000, 1000);
    }

    public void setStored(int value) {
      energy = Math.clamp(value, 0, 30000);
      setChanged();
    }

    @Override
    public int receiveEnergy(int value, boolean simulate) {
      int amount = super.receiveEnergy(value, simulate);
      if (amount > 0 && !simulate) setChanged();
      return amount;
    }

    @Override
    public int extractEnergy(int value, boolean simulate) {
      int amount = super.extractEnergy(value, simulate);
      if (amount > 0 && !simulate) setChanged();
      return amount;
    }
  }

  public final StoredEnergy energy = new StoredEnergy();
  public final ItemStackHandler inventory =
      new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
          return kind() == 0
              ? stack.getItem() instanceof OxygenTankItem
              : stack.is(ModExpansion.BATTERY.get());
        }

        @Override
        public int getSlotLimit(int slot) {
          return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
          setChanged();
        }
      };
  private long scanned = -1000;
  private RoomSealing.Result room = new RoomSealing.Result(false, java.util.Set.of());

  public ExpansionMachine(BlockPos pos, BlockState state) {
    super(ModExpansion.MACHINE.get(), pos, state);
  }

  public int kind() {
    return ((ExpansionMachineBlock) getBlockState().getBlock()).kind;
  }

  @Override
  public void onLoad() {
    super.onLoad();
    ExpansionNetworks.add(this);
  }

  @Override
  public void setRemoved() {
    ExpansionNetworks.remove(this);
    super.setRemoved();
  }

  public void invalidateRoom() {
    scanned = -1000;
  }

  public RoomSealing.Result room() {
    if (level == null || level.isClientSide()) return room;
    if (level.getGameTime() - scanned < 10) return room;
    scanned = level.getGameTime();
    var start = worldPosition.relative(getBlockState().getValue(ExpansionMachineBlock.FACING));
    room =
        RoomSealing.scan(
            start,
            pos -> {
              if (!level.isInWorldBounds(pos)
                  || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
                return RoomSealing.Cell.UNKNOWN;
              var state = level.getBlockState(pos);
              boolean wall =
                  state.getBlock() instanceof SpaceAirlockBlock
                      ? !state.getValue(SpaceAirlockBlock.OPEN)
                      : state.isCollisionShapeFullBlock(level, pos);
              return wall ? RoomSealing.Cell.WALL : RoomSealing.Cell.OPEN;
            },
            1024,
            12);
    return room;
  }

  public static void tick(Level level, BlockPos pos, BlockState state, ExpansionMachine machine) {
    boolean operating = false;
    int kind = machine.kind();
    if (kind == 1) {
      boolean sunlight =
          OrbitStations.isOrbit(level)
              ? level.getHeight(
                      net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                      pos.getX(),
                      pos.getZ())
                  <= pos.getY() + 1
              : level.canSeeSky(pos.above()) && level.isDay() && !level.isRaining();
      if (sunlight) {
        machine.energy.receiveEnergy(40, false);
        operating = true;
      }
    }
    if (kind == 0 || kind == 3) {
      operating =
          machine.energy.getEnergyStored() > 0
              && (kind == 3
                  || machine.room().sealed()
                      && OxygenTankItem.oxygen(machine.inventory.getStackInSlot(0)).current() > 0
                      && machine.inventory.getStackInSlot(0).getItem() instanceof OxygenTankItem);
      if (operating) machine.energy.extractEnergy(1, false);
    }
    if (kind == 1 || kind == 2) {
      for (var direction : Direction.values()) {
        var other = pos.relative(direction);
        if (!level.getChunkSource().hasChunk(other.getX() >> 4, other.getZ() >> 4)) continue;
        if (level.getBlockEntity(other) instanceof ExpansionMachine target
            && (kind == 1 ? target.kind() != 1 : target.kind() == 0 || target.kind() == 3)) {
          var storage =
              level.getCapability(Capabilities.EnergyStorage.BLOCK, other, direction.getOpposite());
          if (storage != null) {
            int transferred = storage.receiveEnergy(machine.energy.extractEnergy(80, true), false);
            machine.energy.extractEnergy(transferred, false);
          }
        }
      }
      if (kind == 2)
        for (var rover :
            level.getEntitiesOfClass(
                RoverEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(4))) {
          int charge = rover.battery.receiveEnergy(machine.energy.extractEnergy(80, true), false);
          machine.energy.extractEnergy(charge, false);
        }
      if (kind == 2) operating = machine.energy.getEnergyStored() > 0;
    }
    var stack = machine.inventory.getStackInSlot(0);
    if (kind != 0 && stack.is(ModExpansion.BATTERY.get())) {
      int charge =
          Math.min(6000 - BatteryItem.charge(stack), machine.energy.extractEnergy(100, true));
      if (charge > 0) {
        machine.energy.extractEnergy(charge, false);
        BatteryItem.charge(stack, BatteryItem.charge(stack) + charge);
        machine.setChanged();
      }
    }
    if (state.getValue(ExpansionMachineBlock.ACTIVE) != operating)
      level.setBlockAndUpdate(pos, state.setValue(ExpansionMachineBlock.ACTIVE, operating));
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    tag.putInt("Energy", energy.getEnergyStored());
    tag.put("Inventory", inventory.serializeNBT(lookup));
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.loadAdditional(tag, lookup);
    energy.setStored(tag.getInt("Energy"));
    var items = tag.getCompound("Inventory").copy();
    items.putInt("Size", 1);
    inventory.deserializeNBT(lookup, items);
    invalidateRoom();
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("expansion.galaxycraft.machine");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new MachineMenu(id, inventory, this);
  }
}
