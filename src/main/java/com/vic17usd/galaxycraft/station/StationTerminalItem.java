package com.vic17usd.galaxycraft.station;

import com.vic17usd.galaxycraft.block.*;
import com.vic17usd.galaxycraft.equipment.SpaceEquipment;
import com.vic17usd.galaxycraft.expansion.*;
import com.vic17usd.galaxycraft.registry.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Crea con un kit una estación propia sobre el cuerpo actual; desde órbita regresa al planeta. */
public final class StationTerminalItem extends Item {
  public StationTerminalItem() {
    super(new Properties().stacksTo(1));
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    if (player instanceof ServerPlayer server) travel(server);
    return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
  }

  public static boolean travel(ServerPlayer player) {
    if (!player.isAlive() || player.isSpectator() || player.isPassenger()) return false;
    var body = OrbitStations.parent(player.level());
    if (body == null) return fail(player, "station_unknown");
    var data = StationData.get(player.server);
    var anchor = data.find(player.getUUID(), body.id());
    ServerLevel target =
        player.server.getLevel(
            OrbitStations.isOrbit(player.level())
                ? body.dimension()
                : OrbitStations.dimension(body));
    if (target == null) return fail(player, "station_unknown");
    if (!player.isCreative()
        && com.vic17usd.galaxycraft.survival.SpaceSurvival.isAirless(target)
        && (!SpaceEquipment.hasHelmet(player) || !SpaceEquipment.hasOxygen(player)))
      return fail(player, "station_suit");
    if (!OrbitStations.isOrbit(player.level())) {
      if (anchor == null) {
        var kit =
            player.getInventory().items.stream()
                .filter(stack -> stack.is(ModExpansion.STATION_KIT.get()))
                .findFirst()
                .orElse(ItemStack.EMPTY);
        if (kit.isEmpty() && !player.isCreative()) return fail(player, "station_kit");
        var pos = data.allocate();
        if (!build(target, pos)) return fail(player, "station_blocked");
        if (!player.isCreative()) kit.shrink(1);
        data.put(player.getUUID(), body.id(), pos, player.blockPosition());
        anchor = data.find(player.getUUID(), body.id());
      } else data.put(player.getUUID(), body.id(), anchor.position(), player.blockPosition());
    } else if (anchor == null) return fail(player, "station_unknown");
    BlockPos arrival =
        OrbitStations.isOrbit(player.level()) ? anchor.returnPosition() : anchor.position().above();
    // Nunca excava ni reemplaza una base modificada para fabricar una llegada segura.
    BlockPos safe = null;
    for (int dx = -2; dx <= 2 && safe == null; dx++)
      for (int dz = -2; dz <= 2 && safe == null; dz++) {
        var pos = arrival.offset(dx, 0, dz);
        target.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (target.isEmptyBlock(pos)
            && target.isEmptyBlock(pos.above())
            && !target.getBlockState(pos.below()).getCollisionShape(target, pos.below()).isEmpty())
          safe = pos;
      }
    if (safe == null) return fail(player, "station_blocked");
    player.closeContainer();
    if (!player.teleportTo(
            target,
            safe.getX() + .5,
            safe.getY(),
            safe.getZ() + .5,
            Set.of(),
            player.getYRot(),
            player.getXRot())
        || player.serverLevel() != target) return false;
    player.fallDistance = 0;
    player.setDeltaMovement(0, 0, 0);
    return true;
  }

  public static boolean build(ServerLevel level, BlockPos center) {
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++)
        for (int y = 0; y <= 6; y++)
          if (!level.isInWorldBounds(center.offset(x, y, z))
              || !level.getWorldBorder().isWithinBounds(center.offset(x, y, z))
              || !level.isEmptyBlock(center.offset(x, y, z))) return false;
    var placed = new ArrayList<BlockPos>();
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++)
        for (int y = 0; y <= 5; y++)
          if (y == 0 || y == 5 || Math.abs(x) == 4 || Math.abs(z) == 4) {
            var pos = center.offset(x, y, z);
            if (!level.setBlockAndUpdate(
                pos,
                (y == 5
                        ? net.minecraft.world.level.block.Blocks.GLASS
                        : ModBlocks.REINFORCED_PANEL.get())
                    .defaultBlockState())) {
              for (var own : placed) level.removeBlock(own, false);
              return false;
            }
            placed.add(pos);
          }
    var dock = center.offset(4, 5, 0);
    level.setBlockAndUpdate(dock, ModExpansion.DOCK.get().defaultBlockState());
    level.setBlockAndUpdate(dock.above(), ModExpansion.SOLAR.get().defaultBlockState());
    var gravity = center.offset(3, 5, 0);
    level.setBlockAndUpdate(gravity, ModExpansion.GRAVITY.get().defaultBlockState());
    var detector = center.offset(4, 4, 0);
    level.setBlockAndUpdate(
        detector,
        ModExpansion.DETECTOR
            .get()
            .defaultBlockState()
            .setValue(ExpansionMachineBlock.FACING, Direction.WEST));
    for (var pos : List.of(dock, gravity, detector))
      if (level.getBlockEntity(pos) instanceof ExpansionMachine machine) {
        machine.energy.setStored(2000);
        if (pos.equals(detector))
          machine.inventory.setStackInSlot(0, new ItemStack(ModItems.OXYGEN_TANK.get()));
      }
    return true;
  }

  private static boolean fail(ServerPlayer player, String reason) {
    player.displayClientMessage(Component.translatable("expansion.galaxycraft." + reason), true);
    return false;
  }
}
