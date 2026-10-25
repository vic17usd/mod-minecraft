package com.vic17usd.galaxycraft.expansion;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.vic17usd.galaxycraft.quest.*;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import com.vic17usd.galaxycraft.station.OrbitStations;
import com.vic17usd.galaxycraft.station.StationData;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

/** Contratos puros: fugas, límites de trabajo, progreso persistente y datos orbitales completos. */
class ExpansionTest {
  private RoomSealing.Cell box(BlockPos pos) {
    return Math.abs(pos.getX()) == 2 || Math.abs(pos.getY()) == 2 || Math.abs(pos.getZ()) == 2
        ? RoomSealing.Cell.WALL
        : RoomSealing.Cell.OPEN;
  }

  @Test
  void recintoCerradoConservaExactamenteSuVolumenInterior() {
    var result = RoomSealing.scan(BlockPos.ZERO, this::box, 27, 12);
    assertTrue(result.sealed());
    assertEquals(27, result.cells().size());
    assertThrows(UnsupportedOperationException.class, () -> result.cells().clear());
  }

  @Test
  void puertaAbiertaChunkDesconocidoYLimitesNuncaPresurizan() {
    assertFalse(
        RoomSealing.scan(
                BlockPos.ZERO,
                pos -> pos.equals(new BlockPos(2, 0, 0)) ? RoomSealing.Cell.OPEN : box(pos),
                1024,
                12)
            .sealed());
    assertFalse(
        RoomSealing.scan(
                BlockPos.ZERO,
                pos -> pos.equals(new BlockPos(1, 0, 0)) ? RoomSealing.Cell.UNKNOWN : box(pos),
                1024,
                12)
            .sealed());
    assertFalse(RoomSealing.scan(BlockPos.ZERO, this::box, 26, 12).sealed());
    assertFalse(RoomSealing.scan(BlockPos.ZERO, pos -> RoomSealing.Cell.OPEN, 1024, 2).sealed());
    assertFalse(RoomSealing.scan(BlockPos.ZERO, pos -> RoomSealing.Cell.WALL, 1024, 12).sealed());
  }

  @Test
  void progresoSeGuardaSinCacheVisualYNoDuplicaRecompensas() {
    var state = new QuestState();
    assertTrue(state.completeOnce("first_ore"));
    assertFalse(state.completeOnce("first_ore"));
    state.visited.add("galaxycraft:moon");
    state.built.add("launch_pad");
    state.drivenMillimeters = 32123;
    state.views.put("first_ore", new QuestState.View(8, 8, 3));
    var saved = state.serializeNBT(RegistryAccess.EMPTY);
    assertFalse(saved.contains("Views"));
    var loaded = new QuestState();
    loaded.deserializeNBT(RegistryAccess.EMPTY, saved);
    assertEquals(state.completed, loaded.completed);
    assertEquals(state.visited, loaded.visited);
    assertEquals(state.built, loaded.built);
    assertEquals(32123, loaded.drivenMillimeters);
    assertTrue(loaded.views.isEmpty());
    saved.putInt("Driven", Integer.MAX_VALUE);
    loaded.deserializeNBT(RegistryAccess.EMPTY, saved);
    assertEquals(1000000, loaded.drivenMillimeters);
    saved.putInt("Driven", -1);
    loaded.deserializeNBT(RegistryAccess.EMPTY, saved);
    assertEquals(0, loaded.drivenMillimeters);
  }

  @Test
  void contratosTienenDependenciasAciclicasYRecompensasPositivas() {
    var seen = new HashSet<String>();
    var phases = new HashSet<Integer>();
    assertEquals(10, QuestCatalog.ALL.size());
    for (var quest : QuestCatalog.ALL) {
      assertTrue(quest.prerequisite().isEmpty() || seen.contains(quest.prerequisite()), quest.id());
      assertTrue(seen.add(quest.id()));
      assertTrue(quest.phase() >= 0 && quest.phase() < 4);
      phases.add(quest.phase());
      assertTrue(quest.count() > 0);
      assertTrue(quest.xp() > 0);
      assertFalse(quest.rewards().isEmpty());
      for (var reward : quest.rewards()) {
        assertTrue(reward.count() > 0 && reward.count() <= 64);
        assertTrue(reward.item().startsWith("galaxycraft:"));
      }
    }
    assertEquals(4, phases.size());
    assertTrue(QuestCatalog.find("arbitrary_client_quest").isEmpty());
  }

  @Test
  void cadaCuerpoTieneOrbitaVaciaYTipoCompatibleConCeroG() throws Exception {
    for (var body : SolarSystem.BODIES) {
      assertEquals("orbit/" + body.id(), OrbitStations.dimension(body).location().getPath());
      try (var stream =
          getClass()
              .getResourceAsStream("/data/galaxycraft/dimension/orbit/" + body.id() + ".json")) {
        assertNotNull(stream, body.id());
        var json =
            JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertEquals("galaxycraft:orbital", json.get("type").getAsString());
        var settings = json.getAsJsonObject("generator").getAsJsonObject("settings");
        assertEquals("galaxycraft:orbital_void", settings.get("biome").getAsString());
        assertTrue(settings.getAsJsonArray("layers").isEmpty());
      }
    }
  }

  @Test
  void paquetesConservanSesionYNoTransportanRecompensasDelCliente() {
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    try {
      var submit = new ExpansionNetworking.Submit(17, "lunar_samples");
      ExpansionNetworking.Submit.CODEC.encode(buffer, submit);
      assertEquals(submit, ExpansionNetworking.Submit.CODEC.decode(buffer));
      var controls = new ExpansionNetworking.Controls(123, 1, -1, 63);
      ExpansionNetworking.Controls.CODEC.encode(buffer, controls);
      assertEquals(controls, ExpansionNetworking.Controls.CODEC.decode(buffer));
      var tag = new CompoundTag();
      tag.putInt("Temperature", 465);
      var env = new ExpansionNetworking.EnvironmentSnapshot(tag);
      ExpansionNetworking.EnvironmentSnapshot.CODEC.encode(buffer, env);
      assertEquals(env, ExpansionNetworking.EnvironmentSnapshot.CODEC.decode(buffer));
    } finally {
      buffer.release();
    }
  }

  @Test
  void parcelasYRegresosNoSeSolapanYSeConservanAlReiniciar() {
    var state = new StationData();
    var owner = java.util.UUID.randomUUID();
    var first = state.allocate();
    state.put(owner, "earth", first, new BlockPos(10, 70, 20));
    var positions = new HashSet<BlockPos>();
    positions.add(first);
    for (int i = 0; i < 300; i++) assertTrue(positions.add(state.allocate()));
    var saved = state.save(new CompoundTag(), RegistryAccess.EMPTY);
    var loaded = StationData.load(saved, RegistryAccess.EMPTY);
    assertEquals(state.find(owner, "earth"), loaded.find(owner, "earth"));
    assertNotEquals(loaded.allocate(), state.find(owner, "earth").position());
    assertFalse(positions.contains(loaded.allocate()));
  }
}
