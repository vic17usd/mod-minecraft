package com.vic17usd.galaxycraft.biosphere;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.vic17usd.galaxycraft.civilization.*;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

/** Contratos de relieve, cobertura planetaria, recursos originales y protocolo de colonias. */
class BiosphereTest {
  private JsonObject json(String file) throws Exception {
    try (var input = getClass().getResourceAsStream("/" + file)) {
      assertNotNull(input, file);
      return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
          .getAsJsonObject();
    }
  }

  @Test
  void todosLosPerfilesSonCompletosYValidanSuIdentidad() {
    assertEquals(29, LivingProfiles.ALL.size());
    var seeds = new HashSet<Integer>();
    for (var profile : LivingProfiles.ALL) {
      assertTrue(seeds.add(profile.seed()));
      assertEquals(3, new HashSet<>(profile.flora()).size());
      assertTrue(profile.fogDistance() >= 40);
      assertFalse(profile.resource().isEmpty());
      assertTrue(
          LivingProfiles.BODY_CODEC
              .parse(JsonOps.INSTANCE, new JsonPrimitive(profile.body()))
              .result()
              .isPresent());
    }
    assertTrue(
        LivingProfiles.BODY_CODEC
            .parse(JsonOps.INSTANCE, new JsonPrimitive("inventado"))
            .error()
            .isPresent());
  }

  @Test
  void relieveEsDeterministaAcotadoYLaSemillaCambiaElMundo() {
    for (var profile : LivingProfiles.ALL) {
      var heights = new HashSet<Integer>();
      boolean changed = false;
      for (int x = -1024; x <= 1024; x += 64)
        for (int z = -512; z <= 512; z += 64) {
          int h = TerrainMath.height(profile, x, z, 12345);
          assertTrue(h >= 18 && h <= 232);
          assertEquals(h, TerrainMath.height(profile, x, z, 12345));
          heights.add(h);
          changed |= h != TerrainMath.height(profile, x, z, 98765);
        }
      assertTrue(heights.size() > 5, profile.body());
      assertTrue(changed, profile.body());
    }
  }

  @Test
  void losCincoRelievesProducenRangosDistintos() {
    var craters = LivingProfiles.get("moon");
    var rift = LivingProfiles.get("venus");
    var dunes = LivingProfiles.get("mars");
    int craterMin = 256, ridgeMax = 0;
    boolean distinct = false;
    for (int x = -512; x <= 512; x += 16)
      for (int z = -512; z <= 512; z += 16) {
        craterMin = Math.min(craterMin, TerrainMath.height(craters, x, z, 47));
        ridgeMax = Math.max(ridgeMax, TerrainMath.height(LivingProfiles.get("ganymede"), x, z, 47));
        distinct |= TerrainMath.height(rift, x, z, 47) != TerrainMath.height(dunes, x, z, 47);
      }
    assertTrue(craterMin < 80, "Faltan depresiones profundas: " + craterMin);
    assertTrue(ridgeMax > 145, "Faltan picos: " + ridgeMax);
    assertTrue(distinct);
  }

  @Test
  void materialesYPlantasTienenModelosTraduccionesYBotin() throws Exception {
    for (var profile : LivingProfiles.ALL) {
      for (String kind : List.of("observatory", "wreck", "sanctuary", "geode", "vault")) {
        String id = profile.body() + "_" + kind + "_panel";
        json("assets/galaxycraft/blockstates/" + id + ".json");
        json("data/galaxycraft/recipe/" + id + ".json");
      }
      for (String style : profile.flora()) {
        String id = profile.body() + "_" + style;
        assertEquals(
            3,
            json("assets/galaxycraft/blockstates/" + id + ".json")
                .getAsJsonObject("variants")
                .size());
        assertTrue(
            json("data/galaxycraft/loot_table/blocks/" + id + ".json")
                .toString()
                .contains(profile.body() + "_biosample"));
      }
    }
  }

  @Test
  void estructurasConservanCincoFamiliasYUsanSuFachada() throws Exception {
    for (var profile : LivingProfiles.ALL)
      for (String kind : List.of("observatory", "wreck", "sanctuary", "geode", "vault"))
        try (var stream =
            getClass()
                .getResourceAsStream(
                    "/data/galaxycraft/structure/expedition/"
                        + profile.body()
                        + "_"
                        + kind
                        + ".nbt")) {
          var nbt = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
          assertEquals(
              "galaxycraft:" + profile.body() + "_" + kind + "_panel",
              nbt.getList("palette", 10).getCompound(0).getString("Name"));
        }
  }

  @Test
  void cadaPlanetaTieneColoniaYBiomasSinReemplazarLaTierra() throws Exception {
    assertNull(getClass().getResource("/data/galaxycraft/dimension/earth.json"));
    for (var profile : LivingProfiles.ALL) {
      var colony = json("data/galaxycraft/worldgen/structure/colony_" + profile.body() + ".json");
      assertEquals(profile.body(), colony.get("body").getAsString());
      json("data/galaxycraft/worldgen/structure_set/colony_" + profile.body() + ".json");
      if (profile.body().equals("earth")) continue;
      var dim = json("data/galaxycraft/dimension/" + profile.body() + ".json");
      assertEquals(
          "galaxycraft:extreme_planet", dim.getAsJsonObject("generator").get("type").getAsString());
      for (String suffix : List.of("", "_rift", "_grove"))
        json("data/galaxycraft/worldgen/biome/" + profile.body() + suffix + ".json");
      assertFalse(
          json("data/galaxycraft/dimension_type/" + profile.body() + ".json").has("fixed_time"));
    }
  }

  @Test
  void todosLosRegistrosVivosTienenPielesYDiscosCelestesOriginales() throws Exception {
    var hashes = new HashSet<Integer>();
    for (var profile : LivingProfiles.ALL) {
      for (String suffix :
          List.of("crystal_bovine", "lumen_sheep", "plated_boar", "colonist", "stalker"))
        try (var input =
            getClass()
                .getResourceAsStream(
                    "/assets/galaxycraft/textures/entity/"
                        + profile.body()
                        + "_"
                        + suffix
                        + ".png")) {
          assertNotNull(input);
          var bytes = input.readAllBytes();
          assertTrue(hashes.add(Arrays.hashCode(bytes)));
          assertEquals(256, ImageIO.read(new java.io.ByteArrayInputStream(bytes)).getWidth());
        }
      try (var input =
          getClass()
              .getResourceAsStream(
                  "/assets/galaxycraft/textures/gui/sky/" + profile.body() + ".png")) {
        assertNotNull(input);
        assertEquals(128, ImageIO.read(input).getHeight());
      }
    }
  }

  @Test
  void contratoPersistenteYPaqueteSoloTransportanIntenciones() {
    var contracts = new ColonyContracts();
    contracts.accepted.add("moon");
    contracts.completed.add("moon");
    var loaded = new ColonyContracts();
    loaded.deserializeNBT(RegistryAccess.EMPTY, contracts.serializeNBT(RegistryAccess.EMPTY));
    assertEquals(contracts.accepted, loaded.accepted);
    assertEquals(contracts.completed, loaded.completed);
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    try {
      var action = new ColonyNetworking.Action(73, 3);
      ColonyNetworking.Action.CODEC.encode(buffer, action);
      assertEquals(action, ColonyNetworking.Action.CODEC.decode(buffer));
    } finally {
      buffer.release();
    }
  }
}
