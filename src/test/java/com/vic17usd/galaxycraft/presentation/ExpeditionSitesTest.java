package com.vic17usd.galaxycraft.presentation;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import com.vic17usd.galaxycraft.worldgen.PlanetarySiteStructure;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Compatibilidad de las 145 expediciones históricas; su generación natural queda desactivada. */
class ExpeditionSitesTest {
  static JsonObject json(String file) {
    try (var stream = ExpeditionSitesTest.class.getResourceAsStream("/" + file)) {
      assertNotNull(stream, file);
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
          .getAsJsonObject();
    } catch (Exception error) {
      throw new AssertionError(file, error);
    }
  }

  static Stream<JsonObject> sites() {
    try (var stream = ExpeditionSitesTest.class.getResourceAsStream("/expedition_sites.json")) {
      var array =
          JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
              .getAsJsonArray();
      return array.asList().stream().map(entry -> entry.getAsJsonObject()).toList().stream();
    } catch (Exception error) {
      throw new AssertionError(error);
    }
  }

  @Test
  void cadaCuerpoConservaSusCincoSitiosHistoricosParaLeerMundos() {
    var entries = sites().toList();
    assertEquals(145, entries.size());
    var ids = new HashSet<String>();
    var schemas = new HashSet<String>();
    for (var body : SolarSystem.BODIES) {
      var selected =
          entries.stream()
              .filter(site -> site.get("body").getAsString().equals(body.id()))
              .toList();
      assertEquals(5, selected.size(), body.id());
      assertEquals(
          2, selected.stream().filter(site -> site.get("underground").getAsBoolean()).count());
      for (var site : selected) {
        assertTrue(ids.add(site.get("id").getAsString()));
        assertTrue(schemas.add(site.get("schematic").getAsString()));
      }
    }
    for (String legacy :
        new String[] {"lunar_research_module", "martian_alien_ruins", "orbital_satellite"}) {
      assertEquals(
          0,
          json("data/galaxycraft/worldgen/structure_set/" + legacy + ".json")
              .getAsJsonObject("placement")
              .get("frequency")
              .getAsDouble());
      assertTrue(
          json("data/galaxycraft/tags/worldgen/biome/has_structure/" + legacy + ".json")
              .getAsJsonArray("values")
              .isEmpty());
    }
  }

  @ParameterizedTest
  @MethodSource("sites")
  void plantillaBotinYRecetaSeEnlazanSinCofresInaccesibles(JsonObject site) throws Exception {
    String name = site.get("id").getAsString(),
        template = site.get("template").getAsString().split(":")[1];
    var structure = json("data/galaxycraft/worldgen/structure/" + name + ".json");
    assertEquals("galaxycraft:planetary_site", structure.get("type").getAsString());
    assertEquals(site.get("underground"), structure.get("underground"));
    var recipe = json("data/galaxycraft/recipe/expedition/" + name + ".json");
    assertEquals(
        site.get("schematic"), recipe.getAsJsonObject("key").getAsJsonObject("S").get("item"));
    var loot = json("data/galaxycraft/loot_table/chests/expedition/" + name + ".json");
    assertTrue(loot.toString().contains(site.get("schematic").getAsString()));
    var set = json("data/galaxycraft/worldgen/structure_set/" + name + ".json");
    assertEquals(
        "galaxycraft:" + name,
        set.getAsJsonArray("structures").get(0).getAsJsonObject().get("structure").getAsString());
    assertEquals(0, set.getAsJsonObject("placement").get("frequency").getAsDouble());
    try (var input =
        getClass().getResourceAsStream("/data/galaxycraft/structure/" + template + ".nbt")) {
      assertNotNull(input);
      var nbt = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
      assertEquals(21, nbt.getList("size", 3).getInt(0));
      assertEquals(21, nbt.getList("size", 3).getInt(2));
      var palette = nbt.getList("palette", 10);
      var blocks = nbt.getList("blocks", 10);
      var states = new java.util.HashMap<String, Integer>();
      net.minecraft.nbt.CompoundTag chest = null;
      for (var tag : blocks) {
        var block = (net.minecraft.nbt.CompoundTag) tag;
        var pos = block.getList("pos", 3);
        for (int axis = 0; axis < 3; axis++)
          assertTrue(
              pos.getInt(axis) >= 0 && pos.getInt(axis) < nbt.getList("size", 3).getInt(axis));
        int state = block.getInt("state");
        assertTrue(state >= 0 && state < palette.size());
        assertNull(
            states.put(pos.getInt(0) + "," + pos.getInt(1) + "," + pos.getInt(2), state),
            "Bloque duplicado");
        if (block.contains("nbt") && block.getCompound("nbt").contains("LootTable")) {
          assertNull(chest);
          chest = block;
          assertEquals(
              "galaxycraft:chests/expedition/" + name,
              chest.getCompound("nbt").getString("LootTable"));
        }
      }
      assertNotNull(chest);
      var pos = chest.getList("pos", 3);
      Integer above = states.get(pos.getInt(0) + "," + (pos.getInt(1) + 1) + "," + pos.getInt(2));
      assertTrue(
          above == null || palette.getCompound(above).getString("Name").equals("minecraft:air"),
          "Tapa del cofre bloqueada: " + name);
    }
  }

  @Test
  void generacionSubterraneaRespetaFondoYCoberturaDeTerreno() {
    assertEquals(36, PlanetarySiteStructure.placementY(60, 65, 0, 256, 12, true, 12));
    assertEquals(24, PlanetarySiteStructure.placementY(60, 65, 0, 256, 12, true, 24));
    assertEquals(
        Integer.MIN_VALUE, PlanetarySiteStructure.placementY(30, 35, 0, 256, 12, true, 24));
    assertEquals(
        Integer.MIN_VALUE, PlanetarySiteStructure.placementY(60, 90, 0, 256, 12, false, 0));
    for (int ground = -60; ground < 250; ground++)
      for (int depth : new int[] {12, 24}) {
        int y = PlanetarySiteStructure.placementY(ground, ground, -64, 320, 12, true, depth);
        if (y != Integer.MIN_VALUE) {
          assertTrue(y >= -56);
          assertTrue(y + 12 <= ground - depth);
        }
      }
  }

  @Test
  void losOchentaYCuatroBloquesPlanetariosUsanTexturasDe128Pixeles() throws Exception {
    for (var body : SolarSystem.BODIES)
      if (!body.id().equals("earth"))
        for (String suffix : new String[] {"stone", "regolith", "ore"}) {
          try (var input =
              getClass()
                  .getResourceAsStream(
                      "/assets/galaxycraft/textures/block/" + body.id() + "_" + suffix + ".png")) {
            var image = ImageIO.read(input);
            assertEquals(128, image.getWidth());
            assertEquals(128, image.getHeight());
          }
        }
  }
}
