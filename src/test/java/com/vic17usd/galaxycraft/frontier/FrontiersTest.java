package com.vic17usd.galaxycraft.frontier;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.vic17usd.galaxycraft.logistics.NetworkGraph;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import com.vic17usd.galaxycraft.weather.SpaceWeatherState;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;

/** Contratos de seguridad de redes, persistencia climática, bóvedas y progresión de combate. */
class FrontiersTest {
  @Test
  void depositosIgualesNoRecirculanYElEquilibrioNoDesborda() {
    assertEquals(
        0,
        com.vic17usd.galaxycraft.logistics.TransferPolicy.balance(1000, 128000, 1000, 128000, 250));
    assertEquals(
        30,
        com.vic17usd.galaxycraft.logistics.TransferPolicy.balance(2400, 128000, 2340, 128000, 60));
    assertEquals(
        512,
        com.vic17usd.galaxycraft.logistics.TransferPolicy.balance(
            1000000, 1000000, 0, 1000000, 512));
    assertEquals(
        0,
        com.vic17usd.galaxycraft.logistics.TransferPolicy.balance(0, 1000000, 1000, 1000000, 512));
  }

  @Test
  void redCiclicaVisitaUnaSolaVezYRespetaElLimite() {
    var nodes = new HashSet<BlockPos>();
    for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) nodes.add(new BlockPos(x, 0, z));
    var graph = NetworkGraph.scan(BlockPos.ZERO, nodes::contains, 256);
    assertTrue(graph.complete());
    assertEquals(256, graph.nodes().size());
    nodes.add(new BlockPos(16, 0, 0));
    assertFalse(NetworkGraph.scan(BlockPos.ZERO, nodes::contains, 256).complete());
  }

  @Test
  void cortarUnTramoSeparaLosExtremos() {
    var nodes = new HashSet<BlockPos>();
    for (int x = 0; x < 7; x++) nodes.add(new BlockPos(x, 0, 0));
    nodes.remove(new BlockPos(3, 0, 0));
    var graph = NetworkGraph.scan(BlockPos.ZERO, nodes::contains, 256);
    assertEquals(3, graph.nodes().size());
    assertFalse(graph.nodes().contains(new BlockPos(6, 0, 0)));
  }

  @Test
  void calendarioNoSeReiniciaAlGuardar() {
    var state = new SpaceWeatherState();
    state.type = 3;
    state.cycle = 7;
    state.next = 99999;
    state.ends = 88888;
    var loaded =
        SpaceWeatherState.load(
            state.save(new CompoundTag(), RegistryAccess.EMPTY), RegistryAccess.EMPTY);
    assertEquals(3, loaded.type);
    assertEquals(7, loaded.cycle);
    assertEquals(99999, loaded.next);
    assertEquals(88888, loaded.ends);
    var invalid = new CompoundTag();
    invalid.putInt("Type", 77);
    invalid.putLong("Ends", -5);
    loaded = SpaceWeatherState.load(invalid, RegistryAccess.EMPTY);
    assertEquals(3, loaded.type);
    assertEquals(0, loaded.ends);
  }

  private com.google.gson.JsonObject json(String file) throws Exception {
    try (var input = getClass().getResourceAsStream("/" + file)) {
      assertNotNull(input, file);
      return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
          .getAsJsonObject();
    }
  }

  @Test
  void todosLosPlanetasTienenHostilPropioYArteValido() throws Exception {
    var hashes = new HashSet<Integer>();
    for (var body : SolarSystem.BODIES)
      if (!body.id().equals("earth")) {
        var spawn = json("data/galaxycraft/neoforge/biome_modifier/" + body.id() + "_raider.json");
        assertEquals("galaxycraft:" + body.id(), spawn.get("biomes").getAsString());
        try (var stream =
            getClass()
                .getResourceAsStream(
                    "/assets/galaxycraft/textures/entity/" + body.id() + "_raider.png")) {
          assertNotNull(stream);
          var bytes = stream.readAllBytes();
          assertTrue(hashes.add(Arrays.hashCode(bytes)));
          var image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
          assertEquals(256, image.getWidth());
        }
        json("data/galaxycraft/loot_table/entities/" + body.id() + "_raider.json");
      }
    assertEquals(28, hashes.size());
  }

  @Test
  void bovedasLunaMarteContienenUnSoloSelloDeJefe() throws Exception {
    for (var body : List.of("moon", "mars"))
      try (var input =
          getClass()
              .getResourceAsStream(
                  "/data/galaxycraft/structure/expedition/" + body + "_vault.nbt")) {
        var template = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        var palette = template.getList("palette", 10);
        int count = 0;
        for (var entry : template.getList("blocks", 10)) {
          var block = (CompoundTag) entry;
          String name = palette.getCompound(block.getInt("state")).getString("Name");
          if (name.endsWith("boss_anchor")) {
            count++;
            assertEquals("galaxycraft:boss_anchor", block.getCompound("nbt").getString("id"));
          }
        }
        assertEquals(1, count);
      }
  }

  @Test
  void progresionDeCohetesRequiereBotinDeAmbosJefes() throws Exception {
    assertTrue(
        json("data/galaxycraft/recipe/tier2_rocket.json")
            .toString()
            .contains("stabilized_thruster"));
    assertTrue(
        json("data/galaxycraft/recipe/stabilized_thruster.json")
            .toString()
            .contains("lunar_guardian_core"));
    assertTrue(
        json("data/galaxycraft/recipe/tier3_rocket.json")
            .toString()
            .contains("advanced_navigation_core"));
    assertTrue(
        json("data/galaxycraft/recipe/advanced_navigation_core.json")
            .toString()
            .contains("martian_queen_heart"));
    assertTrue(
        json("data/galaxycraft/loot_table/entities/lunar_guardian.json")
            .toString()
            .contains("lunar_guardian_core"));
    assertTrue(
        json("data/galaxycraft/loot_table/entities/martian_queen.json")
            .toString()
            .contains("martian_queen_heart"));
  }

  @Test
  void meteoritosSoloReemplazanGeologiaYNoConstruccionIndustrial() throws Exception {
    var tag = json("data/galaxycraft/tags/block/meteor_replaceable.json");
    assertEquals(56, tag.getAsJsonArray("values").size());
    for (var item : tag.getAsJsonArray("values")) {
      String id = item.getAsString();
      assertTrue(id.endsWith("_stone") || id.endsWith("_regolith"));
      assertFalse(id.contains("earth"));
    }
  }
}
