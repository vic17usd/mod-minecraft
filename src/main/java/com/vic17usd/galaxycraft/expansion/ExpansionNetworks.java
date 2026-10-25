package com.vic17usd.galaxycraft.expansion;

import com.vic17usd.galaxycraft.item.OxygenTankItem;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Índices de máquinas cargadas; se vacían al descargar el nivel y no cargan chunks para consultar
 * campos.
 */
public final class ExpansionNetworks {
  private static final Map<Level, Set<ExpansionMachine>> MACHINES = new ConcurrentHashMap<>();

  public static void add(ExpansionMachine machine) {
    if (machine.getLevel() != null)
      MACHINES
          .computeIfAbsent(machine.getLevel(), level -> ConcurrentHashMap.newKeySet())
          .add(machine);
  }

  public static void remove(ExpansionMachine machine) {
    var set = MACHINES.get(machine.getLevel());
    if (set != null) set.remove(machine);
  }

  public static void unload(Level level) {
    MACHINES.remove(level);
  }

  public static void invalidate(Level level) {
    for (var machine : machines(level)) if (machine.kind() == 0) machine.invalidateRoom();
  }

  public static Collection<ExpansionMachine> machines(Level level) {
    return MACHINES.getOrDefault(level, Set.of());
  }

  public static boolean gravity(Level level, BlockPos pos) {
    return machines(level).stream()
        .anyMatch(
            machine ->
                !machine.isRemoved()
                    && machine.kind() == 3
                    && machine.getBlockState().getValue(ExpansionMachineBlock.ACTIVE)
                    && machine.getBlockPos().distSqr(pos) <= 144);
  }

  public static ExpansionMachine room(ServerPlayer player) {
    for (var machine : machines(player.level()))
      if (!machine.isRemoved()
          && machine.kind() == 0
          && machine.getBlockPos().distSqr(player.blockPosition()) <= 600
          && machine.energy.getEnergyStored() > 0
          && machine.inventory.getStackInSlot(0).getItem() instanceof OxygenTankItem
          && OxygenTankItem.oxygen(machine.inventory.getStackInSlot(0)).current() > 0
          && machine.room().cells().contains(player.blockPosition())) return machine;
    return null;
  }

  public static boolean breathe(ServerPlayer player) {
    var machine = room(player);
    if (machine == null) return false;
    boolean consumed = OxygenTankItem.consumeOxygen(machine.inventory.getStackInSlot(0));
    if (consumed) machine.setChanged();
    return consumed;
  }

  private ExpansionNetworks() {}
}
