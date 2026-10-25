package com.vic17usd.galaxycraft.logistics;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.expansion.ExpansionMachine;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Redes pasivas sin carga de chunks. Presupuestos por componente y por tick evitan recorridos
 * ilimitados.
 */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID)
public final class ResourceNetworks {
  private static final Map<Level, Set<LogisticsMachine>> LOADED = new ConcurrentHashMap<>();

  public static void add(LogisticsMachine machine) {
    if (machine.getLevel() != null)
      LOADED
          .computeIfAbsent(machine.getLevel(), ignored -> ConcurrentHashMap.newKeySet())
          .add(machine);
  }

  public static boolean protectedArea(Level level, BlockPos pos) {
    return LOADED.getOrDefault(level, Set.of()).stream()
        .anyMatch(
            machine ->
                !machine.isRemoved()
                    && machine.kind() >= 3
                    && machine.getBlockPos().distSqr(pos) <= 144);
  }

  public static void remove(LogisticsMachine machine) {
    var set = LOADED.get(machine.getLevel());
    if (set != null) set.remove(machine);
  }

  @SubscribeEvent
  public static void unload(LevelEvent.Unload event) {
    LOADED.remove(event.getLevel());
  }

  @SubscribeEvent
  public static void tick(LevelTickEvent.Post event) {
    if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 5 == 0)
      transfer(level);
  }

  private record Contact(BlockPos pos, Direction side) {}

  public static void transfer(ServerLevel level) {
    var roots =
        LOADED.getOrDefault(level, Set.of()).stream()
            .filter(machine -> !machine.isRemoved() && machine.kind() < 3)
            .sorted(Comparator.comparingLong(machine -> machine.getBlockPos().asLong()))
            .toList();
    if (roots.isEmpty()) return;
    var processed = new HashSet<BlockPos>();
    int work = 4096, components = 0, start = (int) ((level.getGameTime() / 5) % roots.size());
    for (int i = 0; i < roots.size() && work > 256 && components < 32; i++) {
      var root = roots.get((start + i) % roots.size());
      if (processed.contains(root.getBlockPos())) continue;
      int kind = root.kind();
      var graph =
          NetworkGraph.scan(
              root.getBlockPos(),
              pos ->
                  loaded(level, pos)
                      && level.getBlockState(pos).getBlock() instanceof LogisticsBlock block
                      && block.kind == kind,
              256);
      processed.addAll(graph.nodes());
      work -= graph.nodes().size();
      components++;
      if (!graph.complete()) continue;
      var contacts = new LinkedHashMap<BlockPos, Contact>();
      for (var pos :
          graph.nodes().stream().sorted(Comparator.comparingLong(BlockPos::asLong)).toList())
        for (var direction : Direction.values()) {
          var next = pos.relative(direction);
          if (!graph.nodes().contains(next) && loaded(level, next))
            contacts.putIfAbsent(next, new Contact(next, direction.getOpposite()));
        }
      var endpoints = new ArrayList<>(contacts.values());
      if (kind == 0) energy(level, endpoints);
      else if (kind == 1) oxygen(level, endpoints);
      else fuel(level, endpoints);
    }
  }

  private static boolean loaded(Level level, BlockPos pos) {
    return level.isInWorldBounds(pos)
        && level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
  }

  private static boolean powerSource(ServerLevel level, BlockPos pos) {
    var block = level.getBlockEntity(pos);
    return block instanceof ExpansionMachine machine
        ? machine.kind() == 1 || machine.kind() == 2
        : !(block instanceof LogisticsMachine storage) || storage.kind() == 4;
  }

  private static boolean powerSink(ServerLevel level, BlockPos pos) {
    return !(level.getBlockEntity(pos) instanceof ExpansionMachine machine) || machine.kind() != 1;
  }

  private record Power(Contact contact, IEnergyStorage storage, int initial) {}

  private static void energy(ServerLevel level, List<Contact> contacts) {
    var values = new ArrayList<Power>();
    for (var contact : contacts) {
      var storage =
          level.getCapability(Capabilities.EnergyStorage.BLOCK, contact.pos(), contact.side());
      if (storage != null) values.add(new Power(contact, storage, storage.getEnergyStored()));
    }
    var outgoing = new IdentityHashMap<IEnergyStorage, Integer>();
    values.sort(
        Comparator.comparingInt(value -> powerSource(level, value.contact().pos()) ? 1 : 0));
    for (var source : values) outgoing.put(source.storage(), Math.min(512, source.initial()));
    for (var source : values) {
      if (!powerSource(level, source.contact().pos()) || !source.storage().canExtract()) continue;
      for (var target : values) {
        if (source.storage() == target.storage() || !powerSink(level, target.contact().pos()))
          continue;
        int max = outgoing.get(source.storage());
        if (max <= 0) break;
        if (powerSource(level, target.contact().pos()))
          max =
              TransferPolicy.balance(
                  source.initial(),
                  source.storage().getMaxEnergyStored(),
                  target.initial(),
                  target.storage().getMaxEnergyStored(),
                  max);
        int accepted = target.storage().receiveEnergy(max, true);
        int extracted = source.storage().extractEnergy(accepted, false);
        int received = target.storage().receiveEnergy(extracted, false);
        if (received < extracted) source.storage().receiveEnergy(extracted - received, false);
        outgoing.put(source.storage(), outgoing.get(source.storage()) - received);
      }
    }
  }

  private record Air(IOxygenStorage storage, int initial, boolean consumer) {}

  private static void oxygen(ServerLevel level, List<Contact> contacts) {
    var values = new ArrayList<Air>();
    for (var contact : contacts) {
      var storage = level.getCapability(LogisticsCapabilities.OXYGEN, contact.pos(), null);
      if (storage != null)
        values.add(
            new Air(
                storage,
                storage.stored(),
                level.getBlockEntity(contact.pos()) instanceof ExpansionMachine));
    }
    values.sort(Comparator.comparingInt(value -> value.consumer() ? 0 : 1));
    for (var source : values) {
      int remaining = Math.min(60, source.initial());
      for (var target : values) {
        if (source.storage() == target.storage() || remaining <= 0) continue;
        int allowed =
            target.consumer()
                ? remaining
                : TransferPolicy.balance(
                    source.initial(),
                    source.storage().capacity(),
                    target.initial(),
                    target.storage().capacity(),
                    remaining);
        int accepted = target.storage().receive(allowed, true);
        int moved = source.storage().extract(accepted, false);
        int received = target.storage().receive(moved, false);
        if (received < moved) source.storage().receive(moved - received, false);
        remaining -= received;
      }
    }
  }

  private record Fuel(
      IFluidHandler storage, int initial, int capacity, boolean rocket, boolean producer) {}

  private static void fuel(ServerLevel level, List<Contact> contacts) {
    var values = new ArrayList<Fuel>();
    for (var contact : contacts) {
      var storage =
          level.getCapability(Capabilities.FluidHandler.BLOCK, contact.pos(), contact.side());
      if (storage != null && storage.getTanks() > 0) {
        boolean rocket =
            storage instanceof LaunchPadFuelPort
                || level.getBlockEntity(contact.pos())
                    instanceof com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
        boolean producer =
            level.getBlockEntity(contact.pos()) instanceof LogisticsMachine machine
                && machine.kind() == 5;
        values.add(
            new Fuel(
                storage,
                rocket
                    ? 0
                    : storage
                        .drain(
                            new FluidStack(Fluids.LAVA, Integer.MAX_VALUE),
                            IFluidHandler.FluidAction.SIMULATE)
                        .getAmount(),
                storage.getTankCapacity(0),
                rocket,
                producer));
      }
    }
    values.sort(Comparator.comparingInt(value -> value.rocket() ? 0 : 1));
    for (var source : values) {
      int remaining = Math.min(250, source.initial());
      for (var target : values) {
        if (source.storage() == target.storage() || remaining <= 0 || target.producer()) continue;
        int allowed =
            target.rocket()
                ? remaining
                : TransferPolicy.balance(
                    source.initial(),
                    source.capacity(),
                    target.initial(),
                    target.capacity(),
                    remaining);
        var offered =
            source
                .storage()
                .drain(new FluidStack(Fluids.LAVA, allowed), IFluidHandler.FluidAction.SIMULATE);
        int accepted = target.storage().fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) continue;
        var drained =
            source
                .storage()
                .drain(offered.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        int received = target.storage().fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (received < drained.getAmount())
          source
              .storage()
              .fill(
                  drained.copyWithAmount(drained.getAmount() - received),
                  IFluidHandler.FluidAction.EXECUTE);
        remaining -= received;
      }
    }
  }

  private ResourceNetworks() {}
}
