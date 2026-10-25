package com.vic17usd.galaxycraft.expedition;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Reglas estrictas: 290 diseños, 5+5, exclusividad, colocación conjunta, NBT y acceso transitables.
 */
class ExpeditionRebuildTest {
  static JsonObject json(String name) {
    try (var input = ExpeditionRebuildTest.class.getResourceAsStream("/" + name)) {
      assertNotNull(input, name);
      return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
          .getAsJsonObject();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  static Stream<ExpeditionCatalog.Site> sites() {
    return ExpeditionCatalog.ALL.stream();
  }

  @Test
  void catalogoContieneExactamenteDiezDisenosPorCuerpo() {
    assertEquals(290, ExpeditionCatalog.ALL.size());
    var ids = new HashSet<String>();
    var templates = new HashSet<String>();
    var geometry = new HashSet<String>();
    for (var body : SolarSystem.BODIES) {
      var sites = ExpeditionCatalog.ALL.stream().filter(s -> s.body().equals(body.id())).toList();
      assertEquals(10, sites.size());
      assertEquals(5, sites.stream().filter(s -> !s.underground()).count());
      assertEquals(5, sites.stream().filter(ExpeditionCatalog.Site::underground).count());
      assertEquals(1, sites.stream().filter(ExpeditionCatalog.Site::mega).count());
      assertEquals(1, sites.stream().filter(s -> s.role().equals("city")).count());
      for (var site : sites) {
        assertTrue(ids.add(site.id()));
        assertTrue(geometry.add(site.geometryHash()));
        assertTrue(site.modules().size() >= 11);
        assertTrue(site.width() >= 49 || site.depth() >= 49);
        for (var module : site.modules()) {
          assertTrue(
              templates.add(module.template()), "Plantilla compartida: " + module.template());
          assertTrue(module.template().startsWith("expedition_v2/" + site.id() + "/"));
          assertTrue(Math.abs(module.x()) <= 112 && Math.abs(module.z()) <= 112);
        }
      }
    }
  }

  @Test
  void plantasSonDistintasInclusoIgnorandoNombrePaletaYRotacion() {
    var unique = new HashSet<String>();
    for (var site : ExpeditionCatalog.ALL) {
      var shapes = new ArrayList<String>();
      for (int rotation = 0; rotation < 8; rotation++) {
        var points = new ArrayList<int[]>();
        for (var m : site.modules())
          if (!m.role().equals("boss_arena")) {
            int x = (m.x() + 8) / 16 * (rotation < 4 ? 1 : -1), z = (m.z() + 8) / 16;
            for (int r = 0; r < rotation % 4; r++) {
              int old = x;
              x = -z;
              z = old;
            }
            points.add(new int[] {x, m.y(), z});
          }
        int minX = points.stream().mapToInt(p -> p[0]).min().orElseThrow(),
            minZ = points.stream().mapToInt(p -> p[2]).min().orElseThrow();
        points.forEach(
            p -> {
              p[0] -= minX;
              p[2] -= minZ;
            });
        points.sort(
            Comparator.<int[]>comparingInt(p -> p[1])
                .thenComparingInt(p -> p[0])
                .thenComparingInt(p -> p[2]));
        shapes.add(points.stream().map(Arrays::toString).reduce("", String::concat));
      }
      Collections.sort(shapes);
      assertTrue(unique.add(shapes.getFirst()), "Misma planta recoloreada: " + site.id());
    }
  }

  @Test
  void cadaMundoComparteUnSoloSetParaLosDiezTipos() {
    for (var body : SolarSystem.BODIES) {
      var set = json("data/galaxycraft/worldgen/structure_set/expeditions_" + body.id() + ".json");
      assertEquals(10, set.getAsJsonArray("structures").size());
      var placement = set.getAsJsonObject("placement");
      assertEquals("galaxycraft:desolate_spread", placement.get("type").getAsString());
      assertEquals(body.id(), placement.get("body").getAsString());
      assertEquals(384, placement.get("spacing").getAsInt());
      assertEquals(192, placement.get("separation").getAsInt());
      var found = new HashSet<String>();
      for (var entry : set.getAsJsonArray("structures")) {
        var id = entry.getAsJsonObject().get("structure").getAsString();
        assertTrue(found.add(id));
        assertEquals(
            body.id(), ExpeditionCatalog.get(id.substring("galaxycraft:".length())).body());
      }
      assertEquals(
          0,
          json("data/galaxycraft/worldgen/structure_set/colony_" + body.id() + ".json")
              .getAsJsonObject("placement")
              .get("frequency")
              .getAsDouble());
    }
  }

  @Test
  void separacionRealSuperaTresMilBloquesConSemillasYCoordenadasNegativas() {
    for (long seed : new long[] {0, 1, -1, 723417, Long.MAX_VALUE})
      for (var body : SolarSystem.BODIES) {
        int salt = 5800127 + SolarSystem.indexOf(body.id()) * 7919;
        var placement = new DesolatePlacement(body.id(), 384, 192, salt);
        for (int x = -4; x <= 4; x++)
          for (int z = -4; z <= 4; z++) {
            var a = placement.getPotentialStructureChunk(seed, x * 384, z * 384);
            for (int dx = -1; dx <= 1; dx++)
              for (int dz = -1; dz <= 1; dz++)
                if (dx != 0 || dz != 0) {
                  var b =
                      placement.getPotentialStructureChunk(seed, (x + dx) * 384, (z + dz) * 384);
                  assertTrue(Math.hypot(a.x - b.x, a.z - b.z) * 16 >= 3088);
                  assertTrue(Math.hypot(a.x - b.x, a.z - b.z) * 16 - 224 > 2800);
                }
          }
      }
  }

  @Test
  void codecRechazaSeparacionesInvalidasYDimensionEsExacta() {
    var valid =
        JsonParser.parseString("{\"body\":\"moon\",\"spacing\":384,\"separation\":192,\"salt\":1}");
    assertTrue(DesolatePlacement.CODEC.codec().parse(JsonOps.INSTANCE, valid).result().isPresent());
    valid.getAsJsonObject().addProperty("separation", 384);
    assertTrue(DesolatePlacement.CODEC.codec().parse(JsonOps.INSTANCE, valid).error().isPresent());
    for (var a : SolarSystem.BODIES)
      for (var b : SolarSystem.BODIES)
        assertEquals(a.id().equals(b.id()), ExpeditionDimensions.matches(a.id(), b.dimension()));
    assertFalse(ExpeditionDimensions.allows(new Object(), "earth"));
  }

  @Test
  void colocacionTieneCoberturaSubterraneaYLimitaPendienteSuperficial() {
    assertEquals(46, ExpeditionStructure.baseY(92, 110, 0, 256, 36, true, 10));
    assertEquals(110, ExpeditionStructure.baseY(92, 110, 0, 256, 30, false, 8));
    assertEquals(Integer.MIN_VALUE, ExpeditionStructure.baseY(40, 70, 0, 256, 36, true, 10));
    assertEquals(Integer.MIN_VALUE, ExpeditionStructure.baseY(70, 110, 0, 256, 30, false, 8));
    for (int min = 18; min < 232; min++)
      for (int height : new int[] {12, 24, 36, 50}) {
        int y = ExpeditionStructure.baseY(min, min + 10, 0, 256, height, true, 8);
        if (y != Integer.MIN_VALUE) {
          assertTrue(y >= 8);
          assertTrue(y + height <= min - 8);
        }
      }
  }

  @ParameterizedTest
  @MethodSource("sites")
  void cadaSitioEnlazaNbtBotinPlanoYUnSoloJefe(ExpeditionCatalog.Site site) throws Exception {
    var definition = json("data/galaxycraft/worldgen/structure/" + site.id() + ".json");
    assertEquals("galaxycraft:exclusive_expedition", definition.get("type").getAsString());
    assertEquals(site.id(), definition.get("site").getAsString());
    assertTrue(
        json("data/galaxycraft/loot_table/chests/expedition_v2/" + site.id() + ".json")
            .toString()
            .contains(site.id() + "_schematic"));
    assertTrue(
        json("data/galaxycraft/recipe/expedition_v2/" + site.id() + ".json")
            .toString()
            .contains(site.id() + "_schematic"));
    int anchors = 0, chests = 0, citizens = 0;
    for (var module : site.modules())
      try (var input =
          getClass()
              .getResourceAsStream("/data/galaxycraft/structure/" + module.template() + ".nbt")) {
        assertNotNull(input);
        var nbt = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        var size = nbt.getList("size", 3);
        assertEquals(module.width(), size.getInt(0));
        assertEquals(module.height(), size.getInt(1));
        assertEquals(module.depth(), size.getInt(2));
        var palette = nbt.getList("palette", 10);
        var blocks = nbt.getList("blocks", 10);
        var positions = new HashMap<String, String>();
        for (var paletteTag : palette) {
          String paletteName = ((CompoundTag) paletteTag).getString("Name");
          for (var other : SolarSystem.BODIES)
            if (!other.id().equals(site.body()) && !paletteName.endsWith("boss_anchor"))
              assertFalse(
                  paletteName.startsWith("galaxycraft:" + other.id() + "_"),
                  "Paleta de otro planeta: " + site.id() + "/" + paletteName);
        }
        for (var entry : blocks) {
          var block = (CompoundTag) entry;
          var pos = block.getList("pos", 3);
          for (int axis = 0; axis < 3; axis++)
            assertTrue(pos.getInt(axis) >= 0 && pos.getInt(axis) < size.getInt(axis));
          int state = block.getInt("state");
          assertTrue(state >= 0 && state < palette.size());
          String name = palette.getCompound(state).getString("Name"),
              key = pos.getInt(0) + "," + pos.getInt(1) + "," + pos.getInt(2);
          assertNull(positions.put(key, name));
          if (name.endsWith("boss_anchor")) {
            anchors++;
            assertEquals(site.body(), block.getCompound("nbt").getString("EncounterBody"));
            assertEquals("boss_arena", module.role());
          }
          if (block.getCompound("nbt").getString("metadata").startsWith("citizen:")) citizens++;
        }
        for (var entry : blocks) {
          var block = (CompoundTag) entry;
          if (!block.getCompound("nbt").contains("LootTable")) continue;
          chests++;
          assertEquals(
              "galaxycraft:chests/expedition_v2/" + site.id(),
              block.getCompound("nbt").getString("LootTable"));
          var p = block.getList("pos", 3);
          assertEquals(
              "minecraft:air",
              positions.get(p.getInt(0) + "," + (p.getInt(1) + 1) + "," + p.getInt(2)));
        }
      }
    assertEquals(site.mega() ? 1 : 0, anchors);
    assertTrue(chests >= 2);
    assertEquals(site.role().equals("city"), citizens > 0);
  }

  @Test
  void modificadoresNeoForgeSoloUsanGuardianesDelCuerpo() {
    for (var body : SolarSystem.BODIES) {
      var modifier =
          json(
              "data/galaxycraft/neoforge/structure_modifier/expedition_v2/"
                  + body.id()
                  + "_guards.json");
      assertEquals("neoforge:add_spawns", modifier.get("type").getAsString());
      assertTrue(modifier.toString().contains(body.id() + "_stalker"));
      assertTrue(
          json("data/galaxycraft/loot_table/entities/expedition_wardens/" + body.id() + ".json")
              .toString()
              .contains("_schematic"));
    }
  }
}
