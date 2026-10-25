package com.vic17usd.galaxycraft.morphology;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import com.vic17usd.galaxycraft.client.OriginalAlienModel;
import io.netty.buffer.Unpooled;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

/** Valida geometría horneable, atlas emisivos originales y protocolos S2C acotados. */
class MorphologyTest {
  private InputStream resource(String path) {
    var input = getClass().getResourceAsStream("/" + path);
    assertNotNull(input, path);
    return input;
  }

  @Test
  void onceEsqueletosSeHorneanConMuchasArticulaciones() {
    for (var design : CreatureDesign.values()) {
      var root = OriginalAlienModel.layer(design, 713).bakeRoot();
      assertTrue(root.getAllParts().count() >= 30, design.name());
      assertTrue(root.getChild("body").hasChild("head"));
      assertTrue(
          root.getAllParts()
              .allMatch(p -> Float.isFinite(p.x) && Float.isFinite(p.y) && Float.isFinite(p.z)));
    }
  }

  @Test
  void modelosSonFamiliasRealmenteDistintas() {
    var counts = new HashSet<Long>();
    for (var design : CreatureDesign.values())
      counts.add(OriginalAlienModel.layer(design, 713).bakeRoot().getAllParts().count());
    assertTrue(counts.size() >= 9, counts.toString());
  }

  @Test
  void las179CriaturasTienenGeometriaYAtlas256ConMascara() {
    JsonObject manifest;
    try (var input = resource("morphology_manifest.json")) {
      manifest = JsonParser.parseReader(new InputStreamReader(input)).getAsJsonObject();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    var entities = manifest.getAsJsonArray("entities");
    assertEquals(179, entities.size());
    var ids = new HashSet<String>();
    var skins = new HashSet<Integer>();
    for (var entry : entities) {
      String id = entry.getAsString();
      assertTrue(ids.add(id));
      assertNotNull(CreatureDesign.of(id));
      try (var input = resource("assets/galaxycraft/textures/entity/" + id + ".png");
          var glow = resource("assets/galaxycraft/textures/entity/" + id + "_glow.png")) {
        byte[] bytes = input.readAllBytes();
        assertTrue(skins.add(Arrays.hashCode(bytes)));
        var image = ImageIO.read(new ByteArrayInputStream(bytes));
        var mask = ImageIO.read(glow);
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertEquals(256, mask.getWidth());
        assertEquals(0, mask.getRGB(32, 32) >>> 24);
        assertTrue(mask.getRGB(132, 8) >>> 24 > 0);
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
  }

  @Test
  void ningunRenderizadorReutilizaMallasVanilla() throws Exception {
    try (var files =
        Files.walk(
            Path.of(
                System.getProperty("galaxycraft.projectRoot"),
                "src/main/java/com/vic17usd/galaxycraft/client"))) {
      for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file);
        for (String forbidden :
            List.of(
                "SilverfishRenderer",
                "HuskRenderer",
                "CowModel",
                "SheepModel",
                "PigModel",
                "ZombieModel",
                "VillagerModel")) assertFalse(source.contains(forbidden), file + ": " + forbidden);
      }
    }
  }

  @Test
  void posesTienenEnvolventeAcotadaYSeRestablecen() {
    var pose = new AlienPose(AlienPose.ATTACK, 100, 20);
    assertTrue(pose.valid());
    assertEquals(0, pose.envelope(99));
    assertEquals(1, pose.envelope(110), 1e-6);
    assertEquals(0, pose.envelope(121));
    assertFalse(new AlienPose(8, 0, 20).valid());
    assertFalse(new AlienPose(1, 0, 201).valid());
  }

  @Test
  void paquetesConservanIdentidadTiempoYDuracion() {
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    try {
      var pose = new MorphologyNetworking.Pose(42, AlienPose.BURROW, 80000, 24);
      MorphologyNetworking.Pose.CODEC.encode(buffer, pose);
      assertEquals(pose, MorphologyNetworking.Pose.CODEC.decode(buffer));
      assertFalse(buffer.isReadable());
      var clock = new MorphologyNetworking.Clock(200, 50.25, 1);
      MorphologyNetworking.Clock.CODEC.encode(buffer, clock);
      assertEquals(clock, MorphologyNetworking.Clock.CODEC.decode(buffer));
      assertFalse(buffer.isReadable());
    } finally {
      buffer.release();
    }
  }

  @Test
  void mapasHDYSeisCarasSonValidosYDiferentes() throws Exception {
    var hashes = new HashSet<Integer>();
    for (var orbit : com.vic17usd.galaxycraft.astronomy.OrbitalMechanics.ALL)
      try (var input = resource("assets/galaxycraft/textures/astronomy/" + orbit.id() + ".png")) {
        var image = ImageIO.read(input);
        assertTrue(image.getWidth() >= 1024);
        assertEquals(image.getWidth() / 2, image.getHeight());
      }
    for (int i = 0; i < 6; i++)
      try (var input = resource("assets/galaxycraft/textures/astronomy/deep_space_" + i + ".png")) {
        var bytes = input.readAllBytes();
        assertTrue(hashes.add(Arrays.hashCode(bytes)));
        var image = ImageIO.read(new ByteArrayInputStream(bytes));
        assertEquals(1024, image.getWidth());
        assertEquals(1024, image.getHeight());
      }
  }
}
