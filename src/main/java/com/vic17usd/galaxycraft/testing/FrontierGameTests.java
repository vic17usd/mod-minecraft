package com.vic17usd.galaxycraft.testing;

import com.mojang.authlib.GameProfile;
import com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
import com.vic17usd.galaxycraft.combat.*;
import com.vic17usd.galaxycraft.component.OxygenData;
import com.vic17usd.galaxycraft.expansion.*;
import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import com.vic17usd.galaxycraft.logistics.*;
import com.vic17usd.galaxycraft.registry.*;
import com.vic17usd.galaxycraft.weather.*;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.*;

/**
 * Integración real de capacidades, conservación, botín, generación de jefes y refugios climáticos.
 */
@GameTestHolder("galaxycraft")
@PrefixGameTestTemplate(false)
public final class FrontierGameTests {
  private static void done(GameTestHelper helper, String id) {
    helper.succeed();
    com.mojang.logging.LogUtils.getLogger().info("GALAXYCRAFT_INTEGRATION_OK {}", id);
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void parallel_storage_priority(GameTestHelper helper) {
    var level = helper.getLevel();
    for (int kind = 0; kind < 3; kind++) {
      var root = helper.absolutePos(new BlockPos(3, 2, 3 + kind * 5));
      var storageBlock =
          kind == 0
              ? ModFrontiers.BATTERY
              : kind == 1 ? ModFrontiers.OXYGEN_TANK : ModFrontiers.FUEL_TANK;
      var pipeBlock =
          kind == 0
              ? ModFrontiers.CABLE
              : kind == 1 ? ModFrontiers.OXYGEN_PIPE : ModFrontiers.FUEL_PIPE;
      level.setBlockAndUpdate(root, storageBlock.get().defaultBlockState());
      var second = root.offset(2, 0, 1);
      level.setBlockAndUpdate(second, storageBlock.get().defaultBlockState());
      var a = (LogisticsMachine) level.getBlockEntity(root);
      var b = (LogisticsMachine) level.getBlockEntity(second);
      for (int x = 1; x <= 3; x++) {
        var pos = root.offset(x, 0, 0);
        level.setBlockAndUpdate(pos, pipeBlock.get().defaultBlockState());
        ResourceNetworks.add((LogisticsMachine) level.getBlockEntity(pos));
      }
      if (kind == 0) {
        a.energy.restore(1000);
        b.energy.restore(1000);
        var target = root.offset(4, 0, 0);
        level.setBlockAndUpdate(target, ModExpansion.GRAVITY.get().defaultBlockState());
        var consumer = (ExpansionMachine) level.getBlockEntity(target);
        ResourceNetworks.transfer(level);
        helper.assertTrue(
            consumer.energy.getEnergyStored() > 0
                && a.energy.getEnergyStored()
                        + b.energy.getEnergyStored()
                        + consumer.energy.getEnergyStored()
                    == 2000,
            "Dos bancos dejaron sin energía al consumidor");
      }
      if (kind == 1) {
        a.oxygen.receive(1200, false);
        b.oxygen.receive(1200, false);
        var target = root.offset(4, 0, 0);
        level.setBlockAndUpdate(target, ModExpansion.DETECTOR.get().defaultBlockState());
        var consumer = (ExpansionMachine) level.getBlockEntity(target);
        var tank = new ItemStack(ModItems.OXYGEN_TANK.get());
        tank.set(ModDataComponents.OXYGEN_LEVEL.get(), new OxygenData(0, 1200));
        consumer.inventory.setStackInSlot(0, tank);
        ResourceNetworks.transfer(level);
        int oxygen = tank.get(ModDataComponents.OXYGEN_LEVEL.get()).current();
        helper.assertTrue(
            oxygen > 0 && a.oxygen.stored() + b.oxygen.stored() + oxygen == 2400,
            "Dos depósitos no priorizaron la respiración");
      }
      if (kind == 2) {
        a.fuel.fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
        b.fuel.fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
        var pad = root.offset(4, 1, 0);
        for (int x = -1; x <= 1; x++)
          for (int z = -1; z <= 1; z++)
            level.setBlockAndUpdate(
                pad.offset(x, 0, z), ModBlocks.LAUNCH_PAD.get().defaultBlockState());
        level.setBlockAndUpdate(pad.above(), ModBlocks.TIER1_ROCKET.get().defaultBlockState());
        var rocket = (RocketBlockEntity) level.getBlockEntity(pad.above());
        ResourceNetworks.transfer(level);
        helper.assertTrue(
            rocket.fuel.getFluidAmount() > 0
                && a.fuel.getFluidAmount() + b.fuel.getFluidAmount() + rocket.fuel.getFluidAmount()
                    == 16000,
            "Dos tanques no priorizaron el cohete");
      }
    }
    done(helper, "parallel_storage_priority");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void energy_cable_conservation(GameTestHelper helper) {
    var level = helper.getLevel();
    var root = helper.absolutePos(new BlockPos(2, 2, 3));
    level.setBlockAndUpdate(root, ModFrontiers.BATTERY.get().defaultBlockState());
    var source = (LogisticsMachine) level.getBlockEntity(root);
    source.energy.restore(10000);
    for (int x = 1; x <= 3; x++) {
      var pos = root.offset(x, 0, 0);
      level.setBlockAndUpdate(pos, ModFrontiers.CABLE.get().defaultBlockState());
      ResourceNetworks.add((LogisticsMachine) level.getBlockEntity(pos));
    }
    var targetPos = root.offset(4, 0, 0);
    level.setBlockAndUpdate(targetPos, ModExpansion.GRAVITY.get().defaultBlockState());
    var target = (ExpansionMachine) level.getBlockEntity(targetPos);
    ResourceNetworks.transfer(level);
    helper.assertTrue(
        target.energy.getEnergyStored() > 0
            && source.energy.getEnergyStored() + target.energy.getEnergyStored() == 10000,
        "El cable perdió o creó FE");
    int received = target.energy.getEnergyStored(), remaining = source.energy.getEnergyStored();
    level.removeBlock(root.offset(2, 0, 0), false);
    ResourceNetworks.transfer(level);
    helper.assertTrue(
        target.energy.getEnergyStored() == received && source.energy.getEnergyStored() == remaining,
        "Un cable cortado siguió transmitiendo");
    done(helper, "energy_cable_conservation");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void oxygen_and_launchpad_pipes(GameTestHelper helper) {
    var level = helper.getLevel();
    var root = helper.absolutePos(new BlockPos(2, 2, 2));
    level.setBlockAndUpdate(root, ModFrontiers.OXYGEN_TANK.get().defaultBlockState());
    var oxygen = (LogisticsMachine) level.getBlockEntity(root);
    oxygen.oxygen.receive(2400, false);
    for (int x = 1; x <= 3; x++) {
      var pos = root.offset(x, 0, 0);
      level.setBlockAndUpdate(pos, ModFrontiers.OXYGEN_PIPE.get().defaultBlockState());
      ResourceNetworks.add((LogisticsMachine) level.getBlockEntity(pos));
    }
    var detectorPos = root.offset(4, 0, 0);
    level.setBlockAndUpdate(detectorPos, ModExpansion.DETECTOR.get().defaultBlockState());
    var detector = (ExpansionMachine) level.getBlockEntity(detectorPos);
    var tank = new ItemStack(ModItems.OXYGEN_TANK.get());
    tank.set(ModDataComponents.OXYGEN_LEVEL.get(), new OxygenData(0, 1200));
    detector.inventory.setStackInSlot(0, tank);
    ResourceNetworks.transfer(level);
    int received = tank.get(ModDataComponents.OXYGEN_LEVEL.get()).current();
    helper.assertTrue(
        received > 0 && oxygen.oxygen.stored() + received == 2400, "La tubería creó oxígeno");
    detector.inventory.setStackInSlot(0, ItemStack.EMPTY);
    int before = oxygen.oxygen.stored();
    ResourceNetworks.transfer(level);
    helper.assertTrue(oxygen.oxygen.stored() == before, "Inventó un tanque receptor");
    var pad = root.offset(7, 0, 7);
    for (int x = -1; x <= 1; x++)
      for (int z = -1; z <= 1; z++)
        level.setBlockAndUpdate(
            pad.offset(x, 0, z), ModBlocks.LAUNCH_PAD.get().defaultBlockState());
    level.setBlockAndUpdate(pad.above(), ModBlocks.TIER1_ROCKET.get().defaultBlockState());
    var rocket = (RocketBlockEntity) level.getBlockEntity(pad.above());
    var storagePos = pad.offset(-4, -1, 0);
    level.setBlockAndUpdate(storagePos, ModFrontiers.FUEL_TANK.get().defaultBlockState());
    var storage = (LogisticsMachine) level.getBlockEntity(storagePos);
    storage.fuel.fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
    for (int x = -3; x <= 0; x++) {
      var pos = pad.offset(x, -1, 0);
      level.setBlockAndUpdate(pos, ModFrontiers.FUEL_PIPE.get().defaultBlockState());
      ResourceNetworks.add((LogisticsMachine) level.getBlockEntity(pos));
    }
    ResourceNetworks.transfer(level);
    helper.assertTrue(
        rocket.fuel.getFluidAmount() > 0
            && rocket.fuel.getFluidAmount() + storage.fuel.getFluidAmount() == 8000,
        "El puerto no repostó o duplicó combustible");
    var port = level.getCapability(Capabilities.FluidHandler.BLOCK, pad, Direction.DOWN);
    helper.assertTrue(
        port != null && port.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
        "El puerto vació el cohete");
    done(helper, "oxygen_and_launchpad_pipes");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void refinery_and_bulk_persistence(GameTestHelper helper) {
    var level = helper.getLevel();
    var pos = helper.absolutePos(new BlockPos(6, 2, 6));
    level.setBlockAndUpdate(pos, ModFrontiers.REFINERY.get().defaultBlockState());
    var refinery = (LogisticsMachine) level.getBlockEntity(pos);
    refinery.energy.restore(2000);
    refinery.inventory.setStackInSlot(0, new ItemStack(Items.COAL));
    for (int i = 0; i < 100; i++)
      LogisticsMachine.tick(level, pos, refinery.getBlockState(), refinery);
    helper.assertTrue(
        refinery.energy.getEnergyStored() == 0
            && refinery.inventory.getStackInSlot(0).isEmpty()
            && refinery.fuel.getFluidAmount() == 1000,
        "La refinería ignoró costes o duplicó combustible");
    refinery.energy.restore(999999);
    refinery.fuel.fill(new FluidStack(Fluids.LAVA, 127000), IFluidHandler.FluidAction.EXECUTE);
    refinery.oxygen.receive(128000, false);
    var saved = refinery.saveWithoutMetadata(level.registryAccess());
    refinery.energy.restore(0);
    refinery.loadWithComponents(saved, level.registryAccess());
    var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GCBulkUI"));
    var menu = new LogisticsMenu(7, player.getInventory(), refinery);
    helper.assertTrue(
        menu.amount(0) == 999999 && menu.amount(1) == 128000 && menu.amount(2) == 128000,
        "El guardado o menú truncó valores de 32 bits");
    helper.assertTrue(
        refinery.fuel.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE)
            == 0,
        "Aceptó agua como combustible");
    done(helper, "refinery_and_bulk_persistence");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 160)
  public static void unique_boss_encounters(GameTestHelper helper) {
    var level = helper.getLevel();
    var pos = helper.absolutePos(new BlockPos(6, 2, 6));
    var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GCBossTarget"));
    player.setPos(pos.getX() + 8, pos.getY() + 1, pos.getZ());
    for (boolean queen : new boolean[] {false, true}) {
      level.setBlockAndUpdate(
          pos,
          (queen ? ModFrontiers.MARTIAN_ANCHOR : ModFrontiers.LUNAR_ANCHOR)
              .get()
              .defaultBlockState());
      var anchor = (BossAnchor) level.getBlockEntity(pos);
      helper.assertTrue(anchor.wake() && !anchor.wake(), "El sello no creó un único jefe");
      var bosses =
          level.getEntitiesOfClass(
              PlanetBoss.class, new net.minecraft.world.phys.AABB(pos).inflate(5));
      helper.assertTrue(bosses.size() == 1, "Encuentro duplicado");
      var boss = bosses.getFirst();
      helper.assertTrue(
          boss.getMaxHealth() == (queen ? 360 : 300), "Atributos de jefe no registrados");
      boss.performRangedAttack(player, 1);
      helper.assertTrue(
          !level
              .getEntitiesOfClass(
                  SpaceProjectile.class, new net.minecraft.world.phys.AABB(pos).inflate(12))
              .isEmpty(),
          "El jefe no disparó");
      boss.hurt(level.damageSources().genericKill(), 10000);
      helper.assertTrue(anchor.defeated && !anchor.wake(), "Una derrota permitió recrear el jefe");
      var drops =
          level.getEntitiesOfClass(
              ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(5));
      helper.assertTrue(
          drops.stream()
              .anyMatch(
                  item ->
                      item.getItem()
                          .is((queen ? ModFrontiers.QUEEN_HEART : ModFrontiers.LUNAR_CORE).get())),
          "Falta componente exclusivo");
      var saved = anchor.saveWithoutMetadata(level.registryAccess());
      anchor.loadWithComponents(saved, level.registryAccess());
      helper.assertTrue(
          anchor.awakened && anchor.defeated, "El sello perdió la derrota al guardar");
      boss.discard();
      level.removeBlock(pos, false);
    }
    done(helper, "unique_boss_encounters");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 160)
  public static void solar_storm_shelter(GameTestHelper helper) {
    var server = helper.getLevel().getServer();
    var mars =
        server.getLevel(ResourceKey.create(Registries.DIMENSION, ModPresentation.id("mars")));
    var center = new BlockPos(19200, 192, 19200);
    for (int x = -2; x <= 2; x++)
      for (int z = -2; z <= 2; z++)
        for (int y = 0; y <= 4; y++)
          mars.setBlockAndUpdate(
              center.offset(x, y, z),
              (y == 0 || y == 4 || Math.abs(x) == 2 || Math.abs(z) == 2
                      ? Blocks.IRON_BLOCK
                      : Blocks.AIR)
                  .defaultBlockState());
    var pos = center.offset(2, 2, 0);
    mars.setBlockAndUpdate(
        pos,
        ModExpansion.DETECTOR
            .get()
            .defaultBlockState()
            .setValue(ExpansionMachineBlock.FACING, Direction.WEST));
    var detector = (ExpansionMachine) mars.getBlockEntity(pos);
    ExpansionNetworks.add(detector);
    detector.energy.setStored(1000);
    detector.inventory.setStackInSlot(0, new ItemStack(ModItems.OXYGEN_TANK.get()));
    var player = FakePlayerFactory.get(mars, new GameProfile(UUID.randomUUID(), "GCSolarShelter"));
    player.tickCount = 20;
    player.setPos(center.getX() + 8, center.getY() + 1, center.getZ());
    try {
      helper.assertTrue(
          SpaceWeather.start(mars, SpaceWeather.SOLAR, 200), "No activó radiación dinámica");
      AdvancedSurvival.tick(player);
      var exposure = player.getData(ModExpansion.EXPOSURE.get());
      int outside = exposure.radiation;
      helper.assertTrue(
          outside >= 8 && !exposure.sheltered, "La tormenta no aumentó exposición exterior");
      player.setPos(center.getX() + .5, center.getY() + 1, center.getZ() + .5);
      AdvancedSurvival.tick(player);
      helper.assertTrue(
          exposure.sheltered && exposure.temperature == 22 && exposure.radiation < outside,
          "La sala hermética no protegió del evento solar");
      var equipment = com.vic17usd.galaxycraft.equipment.SpaceEquipment.inventory(player);
      equipment.setStackInSlot(0, new ItemStack(ModItems.SPACE_HELMET.get()));
      equipment.setStackInSlot(1, new ItemStack(ModItems.SPACE_CHESTPLATE.get()));
      equipment.setStackInSlot(2, new ItemStack(ModItems.SPACE_LEGGINGS.get()));
      equipment.setStackInSlot(3, new ItemStack(ModItems.SPACE_BOOTS.get()));
      equipment.setStackInSlot(4, new ItemStack(ModItems.OXYGEN_TANK.get()));
      equipment.setStackInSlot(7, new ItemStack(ModExpansion.SHIELD.get()));
      player.setPos(center.getX() + 8, center.getY() + 1, center.getZ());
      exposure.radiation = 0;
      AdvancedSurvival.tick(player);
      helper.assertTrue(
          exposure.radiation > 0 && exposure.radiation < outside,
          "El plomo no redujo parcialmente una tormenta fuerte");
    } finally {
      SpaceWeather.start(mars, SpaceWeather.NONE, 20);
      for (int x = -2; x <= 2; x++)
        for (int z = -2; z <= 2; z++)
          for (int y = 0; y <= 4; y++) mars.removeBlock(center.offset(x, y, z), false);
    }
    done(helper, "solar_storm_shelter");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 160)
  public static void space_weather_and_meteors(GameTestHelper helper) {
    var level = helper.getLevel();
    var mars =
        level
            .getServer()
            .getLevel(ResourceKey.create(Registries.DIMENSION, ModPresentation.id("mars")));
    helper.assertTrue(
        !SpaceWeather.start(level, SpaceWeather.SOLAR, 200),
        "El clima espacial afectó al Overworld");
    helper.assertTrue(
        SpaceWeather.start(mars, SpaceWeather.DUST, 200) && SpaceWeather.solarOutput(mars) == 8,
        "Polvo marciano no redujo energía solar");
    helper.assertTrue(
        SpaceWeather.start(mars, SpaceWeather.SOLAR, 200)
            && SpaceWeather.radiation(mars) == 8
            && SpaceWeather.solarOutput(mars) == 0,
        "La tormenta solar no alteró radiación/paneles");
    SpaceWeather.start(mars, SpaceWeather.NONE, 20);
    var center = helper.absolutePos(new BlockPos(6, 2, 6));
    var natural = ModPlanetContent.BLOCKS.get("moon_stone").get().defaultBlockState();
    for (int x = -1; x <= 1; x++)
      for (int z = -1; z <= 1; z++) level.setBlockAndUpdate(center.offset(x, 0, z), natural);
    helper.assertTrue(
        MeteorImpact.allowed(level, center),
        "Impacto rechazado en "
            + center
            + " geología="
            + level.getBlockState(center).is(MeteorImpact.NATURAL)
            + " referencias="
            + level.structureManager().getAllStructuresAt(center)
            + " máquinas="
            + ExpansionNetworks.machines(level).size());
    boolean griefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    try {
      level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, level.getServer());
      helper.assertTrue(
          MeteorImpact.impact(level, center)
              && level.getBlockState(center).is(ModFrontiers.METEORITE.get()),
          "No depositó el meteorito");
      helper.assertTrue(
          level.getBlockState(center.offset(1, 0, 1)).equals(natural),
          "El cráter excedió cinco celdas");
      level.setBlockAndUpdate(center, ModBlocks.REINFORCED_PANEL.get().defaultBlockState());
      helper.assertTrue(
          !MeteorImpact.impact(level, center)
              && level.getBlockState(center).is(ModBlocks.REINFORCED_PANEL.get()),
          "Un impacto destruyó construcción industrial");
    } finally {
      level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(griefing, level.getServer());
    }
    done(helper, "space_weather_and_meteors");
  }
}
