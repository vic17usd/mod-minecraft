package com.vic17usd.galaxycraft.testing;

import com.mojang.authlib.GameProfile;
import com.vic17usd.galaxycraft.component.OxygenData;
import com.vic17usd.galaxycraft.equipment.*;
import com.vic17usd.galaxycraft.expansion.*;
import com.vic17usd.galaxycraft.quest.*;
import com.vic17usd.galaxycraft.registry.*;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import com.vic17usd.galaxycraft.station.*;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

/**
 * Pruebas de integración con registros, capacidades, componentes y dimensiones reales del servidor.
 */
@GameTestHolder("galaxycraft")
@PrefixGameTestTemplate(false)
public final class ExpansionGameTests {
  private static void done(GameTestHelper helper, String id) {
    helper.succeed();
    com.mojang.logging.LogUtils.getLogger().info("GALAXYCRAFT_INTEGRATION_OK {}", id);
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void quests_and_modules(GameTestHelper helper) {
    var level = helper.getLevel();
    var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GCQuestTest"));
    for (var contract : QuestCatalog.ALL) {
      for (var reward : contract.rewards())
        helper.assertTrue(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(
                net.minecraft.resources.ResourceLocation.parse(reward.item())),
            "Recompensa desconocida: " + reward.item());
      if (contract.type().equals("deliver"))
        helper.assertTrue(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(
                net.minecraft.resources.ResourceLocation.parse(contract.target())),
            "Entrega desconocida: " + contract.target());
    }
    var original = SpaceEquipment.inventory(player);
    var tank = new ItemStack(ModItems.OXYGEN_TANK.get());
    tank.set(ModDataComponents.OXYGEN_LEVEL.get(), new OxygenData(7, 20));
    original.setStackInSlot(4, tank);
    var old = original.serializeNBT(level.registryAccess());
    old.putInt("Size", 6);
    var restored = new SpaceEquipmentInventory();
    restored.deserializeNBT(level.registryAccess(), old);
    helper.assertTrue(
        restored.getSlots() == 8
            && restored.getStackInSlot(4).get(ModDataComponents.OXYGEN_LEVEL.get()).current() == 7,
        "Migración de seis ranuras perdió equipo");
    helper.assertTrue(
        restored.isItemValid(6, new ItemStack(ModExpansion.THERMAL.get()))
            && !restored.isItemValid(6, new ItemStack(ModExpansion.SHIELD.get())),
        "Filtros de módulos incorrectos");
    var quest = QuestCatalog.find("first_ore").orElseThrow();
    player.getInventory().clearContent();
    player.getInventory().setItem(0, new ItemStack(ModItems.TITANIUM_ORE.get(), 7));
    helper.assertTrue(
        !QuestService.finish(player, quest, true)
            && QuestService.count(player, "galaxycraft:titanium_ore") == 7,
        "Entrega insuficiente consumió recursos");
    player.getInventory().setItem(0, new ItemStack(ModItems.TITANIUM_ORE.get(), 10));
    helper.assertTrue(
        !QuestService.finish(player, quest, false),
        "Una misión de entrega se cobró sin consentimiento");
    helper.assertTrue(QuestService.finish(player, quest, true), "No cobró una entrega válida");
    int xp = player.totalExperience;
    helper.assertTrue(
        QuestService.count(player, "galaxycraft:titanium_ore") == 2
            && QuestService.count(player, "galaxycraft:oxygen_tank") == 1
            && xp == quest.xp(),
        "Consumo o recompensa incorrectos");
    helper.assertTrue(
        !QuestService.finish(player, quest, true) && player.totalExperience == xp,
        "Repetición duplicó recompensas");
    var progress = new QuestState();
    progress.deserializeNBT(
        level.registryAccess(), QuestService.state(player).serializeNBT(level.registryAccess()));
    helper.assertTrue(progress.completed.contains("first_ore"), "No persistió la misión");
    done(helper, "quests_and_modules");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void exposure_and_flight(GameTestHelper helper) {
    var server = helper.getLevel().getServer();
    var venus =
        server.getLevel(
            net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, ModPresentation.id("venus")));
    var player = FakePlayerFactory.get(venus, new GameProfile(UUID.randomUUID(), "GCExposure"));
    player.tickCount = 100;
    var equipment = SpaceEquipment.inventory(player);
    equipment.setStackInSlot(0, new ItemStack(ModItems.SPACE_HELMET.get()));
    equipment.setStackInSlot(1, new ItemStack(ModItems.SPACE_CHESTPLATE.get()));
    equipment.setStackInSlot(2, new ItemStack(ModItems.SPACE_LEGGINGS.get()));
    equipment.setStackInSlot(3, new ItemStack(ModItems.SPACE_BOOTS.get()));
    equipment.setStackInSlot(4, new ItemStack(ModItems.OXYGEN_TANK.get()));
    equipment.setStackInSlot(6, new ItemStack(ModExpansion.THERMAL.get()));
    AdvancedSurvival.tick(player);
    var exposure = player.getData(ModExpansion.EXPOSURE.get());
    helper.assertTrue(
        exposure.temperature == 465
            && exposure.thermal == 0
            && equipment.getStackInSlot(6).getDamageValue() == 1,
        "Módulo térmico no protegió o no se desgastó");
    equipment.setStackInSlot(4, ItemStack.EMPTY);
    AdvancedSurvival.tick(player);
    helper.assertTrue(exposure.thermal == 4, "Un módulo sin oxígeno siguió funcionando");
    var orbit = server.getLevel(OrbitStations.dimension(SolarSystem.BODIES.getFirst()));
    var astronaut = FakePlayerFactory.get(orbit, new GameProfile(UUID.randomUUID(), "GCZeroG"));
    astronaut.setPos(16000, 150, 16000);
    AdvancedSurvival.physics(astronaut);
    var flight = astronaut.getAttribute(net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT);
    var id = ModPresentation.id("orbital_flight");
    helper.assertTrue(
        flight.hasModifier(id) && astronaut.mayFly(),
        "La flotación no evita el control de vuelo del servidor");
    var state = astronaut.getData(ModExpansion.EXPOSURE.get());
    state.input = 16;
    state.inputAt = orbit.getGameTime();
    AdvancedSurvival.physics(astronaut);
    helper.assertTrue(astronaut.getDeltaMovement().y > 0, "El empuje vertical no responde");
    var pos = astronaut.blockPosition().below();
    orbit.setBlockAndUpdate(pos, ModExpansion.GRAVITY.get().defaultBlockState());
    var machine = (ExpansionMachine) orbit.getBlockEntity(pos);
    ExpansionNetworks.add(machine);
    machine.energy.setStored(10);
    ExpansionMachine.tick(orbit, pos, machine.getBlockState(), machine);
    AdvancedSurvival.physics(astronaut);
    helper.assertTrue(
        !flight.hasModifier(id), "No retiró su propio permiso de vuelo al activar gravedad");
    orbit.removeBlock(pos, false);
    done(helper, "exposure_and_flight");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void sealed_room_resources(GameTestHelper helper) {
    var level = helper.getLevel();
    var center = helper.absolutePos(new BlockPos(6, 2, 6));
    for (int x = -2; x <= 2; x++)
      for (int z = -2; z <= 2; z++)
        for (int y = 0; y <= 4; y++)
          level.setBlockAndUpdate(
              center.offset(x, y, z),
              (y == 0 || y == 4 || Math.abs(x) == 2 || Math.abs(z) == 2
                      ? Blocks.IRON_BLOCK
                      : Blocks.AIR)
                  .defaultBlockState());
    var door = center.offset(-2, 1, 0);
    level.setBlockAndUpdate(door, ModExpansion.AIRLOCK.get().defaultBlockState());
    level.setBlockAndUpdate(
        door.above(),
        ModExpansion.AIRLOCK
            .get()
            .defaultBlockState()
            .setValue(SpaceAirlockBlock.HALF, DoubleBlockHalf.UPPER));
    var pos = center.offset(2, 2, 0);
    level.setBlockAndUpdate(
        pos,
        ModExpansion.DETECTOR
            .get()
            .defaultBlockState()
            .setValue(ExpansionMachineBlock.FACING, Direction.WEST));
    var detector = (ExpansionMachine) level.getBlockEntity(pos);
    ExpansionNetworks.add(detector);
    detector.energy.setStored(12345);
    detector.inventory.setStackInSlot(0, new ItemStack(ModItems.OXYGEN_TANK.get()));
    helper.assertTrue(
        detector.room().sealed() && detector.room().cells().size() == 27,
        "Sala o escotilla cerrada no hermética");
    var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GCAirTest"));
    player.setPos(center.getX() + .5, center.getY() + 1, center.getZ() + .5);
    int oxygen = 1200;
    helper.assertTrue(ExpansionNetworks.breathe(player), "No suministró aire sin casco");
    helper.assertTrue(
        detector.inventory.getStackInSlot(0).get(ModDataComponents.OXYGEN_LEVEL.get()).current()
            == oxygen - 1,
        "La sala creó oxígeno gratuito");
    ExpansionMachine.tick(level, pos, detector.getBlockState(), detector);
    helper.assertTrue(detector.energy.getEnergyStored() == 12344, "No gastó FE");
    var saved = detector.saveWithoutMetadata(level.registryAccess());
    detector.energy.setStored(0);
    detector.loadWithComponents(saved, level.registryAccess());
    helper.assertTrue(detector.energy.getEnergyStored() == 12344, "Persistencia truncó FE");
    level.setBlockAndUpdate(door, level.getBlockState(door).setValue(SpaceAirlockBlock.OPEN, true));
    detector.invalidateRoom();
    helper.assertTrue(
        !detector.room().sealed() && !ExpansionNetworks.breathe(player),
        "Una fuga siguió suministrando aire");
    done(helper, "sealed_room_resources");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 120)
  public static void rover_authority(GameTestHelper helper) {
    var level = helper.getLevel();
    var base = helper.absolutePos(new BlockPos(6, 2, 6));
    for (int x = -3; x <= 3; x++)
      for (int z = -3; z <= 5; z++)
        level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.IRON_BLOCK.defaultBlockState());
    var rover = ModExpansion.ROVER.get().create(level);
    rover.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
    level.addFreshEntity(rover);
    var intruder = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GCRoverOther"));
    // FakePlayer prohíbe montar por diseño; este ServerPlayer usa su conexión nula de pruebas.
    var pilot =
        new net.minecraft.server.level.ServerPlayer(
            level.getServer(),
            level,
            new GameProfile(UUID.randomUUID(), "GCRoverTest"),
            net.minecraft.server.level.ClientInformation.createDefault());
    pilot.connection = intruder.connection;
    helper.assertTrue(!rover.input(intruder, 1, 0), "Aceptó control remoto sin piloto");
    helper.assertTrue(pilot.startRiding(rover, true), "No pudo montar al piloto de pruebas");
    helper.assertTrue(
        !rover.input(pilot, Float.NaN, 0) && !rover.input(pilot, 2, 0),
        "Aceptó controles no finitos o fuera de rango");
    var battery = new ItemStack(ModExpansion.BATTERY.get());
    rover.inventory.setStackInSlot(9, battery);
    rover.inventory.setStackInSlot(0, new ItemStack(ModExpansion.LEAD.get(), 3));
    rover.tick();
    helper.assertTrue(
        rover.energy() == 200 && BatteryItem.charge(battery) == 5800,
        "La batería duplicó o perdió FE");
    var saved = new net.minecraft.nbt.CompoundTag();
    rover.saveWithoutId(saved);
    var copy = ModExpansion.ROVER.get().create(level);
    copy.load(saved);
    helper.assertTrue(
        copy.energy() == 200
            && copy.inventory.getStackInSlot(0).getCount() == 3
            && BatteryItem.charge(copy.inventory.getStackInSlot(9)) == 5800,
        "No persistió el rover y su carga");
    helper.assertTrue(rover.input(pilot, 1, 0), "No aceptó al piloto real");
    double before = rover.getZ();
    rover.tick();
    helper.assertTrue(
        rover.getZ() > before && QuestService.state(pilot).drivenMillimeters > 0,
        "El rover no avanzó ni registró distancia");
    rover.hurt(level.damageSources().generic(), 100);
    var drops = level.getEntitiesOfClass(ItemEntity.class, rover.getBoundingBox().inflate(3));
    helper.assertTrue(
        drops.stream()
                .filter(item -> item.getItem().is(ModExpansion.ROVER_ITEM.get()))
                .mapToInt(item -> item.getItem().getCount())
                .sum()
            == 1,
        "El vehículo no devolvió exactamente un kit");
    done(helper, "rover_authority");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 120)
  public static void station_round_trip(GameTestHelper helper) {
    var level = helper.getLevel();
    var center = helper.absolutePos(new BlockPos(6, 2, 6));
    for (int x = -3; x <= 3; x++)
      for (int z = -3; z <= 3; z++)
        level.setBlockAndUpdate(center.offset(x, -1, z), Blocks.IRON_BLOCK.defaultBlockState());
    var connection =
        FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GCStationStub"))
            .connection;
    // NeoForge consulta atributos del canal durante el viaje, incluso sin cliente gráfico.
    if (connection.getConnection().channel() == null)
      new io.netty.channel.embedded.EmbeddedChannel(connection.getConnection());
    var player =
        new net.minecraft.server.level.ServerPlayer(
            level.getServer(),
            level,
            new GameProfile(UUID.randomUUID(), "GCStationTrip"),
            net.minecraft.server.level.ClientInformation.createDefault());
    player.connection = connection;
    player.setPos(center.getX() + .5, center.getY(), center.getZ() + .5);
    helper.assertTrue(
        !StationTerminalItem.travel(player), "Permitió crear una estación sin kit ni traje");
    var equipment = SpaceEquipment.inventory(player);
    equipment.setStackInSlot(0, new ItemStack(ModItems.SPACE_HELMET.get()));
    equipment.setStackInSlot(4, new ItemStack(ModItems.OXYGEN_TANK.get()));
    player.getInventory().setItem(0, new ItemStack(ModExpansion.STATION_KIT.get()));
    var body = OrbitStations.parent(level);
    var data = StationData.get(level.getServer());
    try {
      helper.assertTrue(
          StationTerminalItem.travel(player) && OrbitStations.isOrbit(player.level()),
          "Falló el acceso orbital real");
      helper.assertTrue(
          player.getInventory().getItem(0).isEmpty()
              && data.find(player.getUUID(), body.id()) != null,
          "No debitó el kit o no guardó el ancla");
      helper.assertTrue(
          StationTerminalItem.travel(player) && player.level() == level,
          "No volvió a la dimensión de origen");
      helper.assertTrue(
          StationTerminalItem.travel(player) && OrbitStations.isOrbit(player.level()),
          "Exigió otro kit para una estación existente");
      helper.assertTrue(
          StationTerminalItem.travel(player) && player.level() == level,
          "Falló el segundo regreso");
    } finally {
      var anchor = data.find(player.getUUID(), body.id());
      if (anchor != null) {
        var orbit = level.getServer().getLevel(OrbitStations.dimension(body));
        for (int x = -4; x <= 4; x++)
          for (int z = -4; z <= 4; z++)
            for (int y = 0; y <= 6; y++)
              orbit.removeBlock(anchor.position().offset(x, y, z), false);
        data.anchors.remove(data.key(player.getUUID(), body.id()));
        data.setDirty();
      }
      player.discard();
    }
    done(helper, "station_round_trip");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 120)
  public static void orbital_station(GameTestHelper helper) {
    var server = helper.getLevel().getServer();
    for (var body : SolarSystem.BODIES)
      helper.assertTrue(
          server.getLevel(OrbitStations.dimension(body)) != null, "Falta órbita de " + body.id());
    var body = SolarSystem.BODIES.getFirst();
    var orbit = server.getLevel(OrbitStations.dimension(body));
    var center = new BlockPos(14000, 96, 14000);
    // Parcela exclusivamente de prueba: nunca reemplazar construcciones del mundo de desarrollo.
    if (!orbit.isEmptyBlock(center)) center = center.offset(32, 0, 0);
    helper.assertTrue(StationTerminalItem.build(orbit, center), "No construyó la estación inicial");
    var detector = (ExpansionMachine) orbit.getBlockEntity(center.offset(4, 4, 0));
    ExpansionNetworks.add(detector);
    helper.assertTrue(detector.room().sealed(), "La estación inicial tiene una fuga");
    var gravity = (ExpansionMachine) orbit.getBlockEntity(center.offset(3, 5, 0));
    ExpansionNetworks.add(gravity);
    ExpansionMachine.tick(orbit, gravity.getBlockPos(), gravity.getBlockState(), gravity);
    var item =
        new ItemEntity(
            orbit,
            center.getX() + .5,
            center.getY() + 1,
            center.getZ() + .5,
            new ItemStack(Items.IRON_INGOT));
    helper.assertTrue(!AdvancedSurvival.zeroG(item), "El generador no creó gravedad local");
    orbit.removeBlock(gravity.getBlockPos(), false);
    AdvancedSurvival.physics(item);
    helper.assertTrue(item.isNoGravity(), "Los objetos no flotan sin generador");
    orbit.setBlockAndUpdate(gravity.getBlockPos(), ModExpansion.GRAVITY.get().defaultBlockState());
    gravity = (ExpansionMachine) orbit.getBlockEntity(gravity.getBlockPos());
    ExpansionNetworks.add(gravity);
    gravity.energy.setStored(10);
    ExpansionMachine.tick(orbit, gravity.getBlockPos(), gravity.getBlockState(), gravity);
    AdvancedSurvival.physics(item);
    helper.assertTrue(!item.isNoGravity(), "El campo no restauró la gravedad original del objeto");
    // Se retiran solamente los bloques creados por esta prueba, no otros terrenos ni entidades.
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++)
        for (int y = 0; y <= 6; y++) orbit.removeBlock(center.offset(x, y, z), false);
    done(helper, "orbital_station");
  }
}
