package com.vic17usd.galaxycraft.testing;

import com.vic17usd.galaxycraft.biosphere.*;
import com.vic17usd.galaxycraft.civilization.ColonyCitizen;
import com.vic17usd.galaxycraft.combat.*;
import com.vic17usd.galaxycraft.expedition.*;
import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;

/** Pruebas con colocación real, dimensiones compartiendo servidor, piezas guardadas y botín. */
@GameTestHolder("galaxycraft")
@PrefixGameTestTemplate(false)
public final class ExpeditionGameTests {
  private static void done(GameTestHelper helper, String id) {
    helper.succeed();
    com.mojang.logging.LogUtils.getLogger().info("GALAXYCRAFT_INTEGRATION_OK {}", id);
  }

  @GameTest(template = "tests/empty", timeoutTicks = 200)
  public static void expedition_dimension_exclusivity(GameTestHelper helper) {
    var server = helper.getLevel().getServer();
    for (var body : SolarSystem.BODIES) {
      var level = server.getLevel(body.dimension());
      helper.assertTrue(level != null, "Falta mundo " + body.id());
      var state = level.getChunkSource().getGeneratorState();
      helper.assertTrue(
          ExpeditionDimensions.allows(state, body.id())
              && ExpeditionDimensions.allows(level.getChunkSource().randomState(), body.id()),
          "No vinculó el mundo " + body.id());
      var set =
          level
              .registryAccess()
              .registryOrThrow(Registries.STRUCTURE_SET)
              .get(ModPresentation.id("expeditions_" + body.id()));
      helper.assertTrue(set.structures().size() == 10, "Catálogo no contiene diez starts");
      var placement = (DesolatePlacement) set.placement();
      var candidate = placement.getPotentialStructureChunk(level.getSeed(), 0, 0);
      helper.assertTrue(
          placement.isStructureChunk(state, candidate.x, candidate.z),
          "Rechazó su propio candidato");
      for (var other : SolarSystem.BODIES)
        if (!other.id().equals(body.id()))
          helper.assertTrue(
              !placement.isStructureChunk(
                  server.getLevel(other.dimension()).getChunkSource().getGeneratorState(),
                  candidate.x,
                  candidate.z),
              "Una estructura pudo cruzar dimensiones");
    }
    done(helper, "expedition_dimension_exclusivity");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 1200)
  public static void expedition_natural_starts(GameTestHelper helper) {
    var server = helper.getLevel().getServer();
    for (String body : List.of("moon", "mars", "enceladus")) {
      var level = server.getLevel(LivingProfiles.get(body).celestial().dimension());
      var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
      var placement =
          (DesolatePlacement) registry.get(ModPresentation.id("expeditions_" + body)).placement();
      boolean found = false;
      for (int region = 2; region < 10 && !found; region++) {
        var candidate =
            placement.getPotentialStructureChunk(level.getSeed(), region * 384, -3 * 384);
        var chunk = level.getChunk(candidate.x, candidate.z, ChunkStatus.STRUCTURE_STARTS);
        int count = 0;
        for (var entry : chunk.getAllStarts().entrySet())
          if (entry.getValue().isValid()
              && entry.getKey() instanceof ExpeditionStructure expedition) {
            count++;
            found = true;
            var site = ExpeditionCatalog.get(expedition.site());
            helper.assertTrue(site.body().equals(body), "Start extranjero");
            helper.assertTrue(
                entry.getValue().getPieces().size() >= site.modules().size(),
                "Complejo incompleto");
            com.mojang.logging.LogUtils.getLogger()
                .info("GALAXYCRAFT_NATURAL_EXPEDITION {} {} {}", body, candidate, site.id());
          }
        helper.assertTrue(count <= 1, "Más de una expedición en la región compartida");
      }
      helper.assertTrue(found, "No generó ningún start natural en " + body);
    }
    done(helper, "expedition_natural_starts");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 200)
  public static void expedition_colonists_roundtrip(GameTestHelper helper) {
    var level = helper.getLevel();
    var site =
        ExpeditionCatalog.ALL.stream()
            .filter(s -> s.body().equals("earth") && s.role().equals("city"))
            .findFirst()
            .orElseThrow();
    var module =
        site.modules().stream().filter(m -> m.role().equals("house")).findFirst().orElseThrow();
    var pos = helper.absolutePos(new BlockPos(2, 2, 2));
    var clip =
        new BoundingBox(
            pos.getX(),
            level.getMinBuildHeight(),
            pos.getZ(),
            pos.getX() + 16,
            Math.min(level.getMaxBuildHeight() - 1, pos.getY() + module.height()),
            pos.getZ() + 16);
    var random = RandomSource.create(9);
    var chunk = new ChunkPos(pos);
    var piece =
        new ExpeditionPiece(
            level.getStructureManager(),
            site.id(),
            ModPresentation.id(module.template()),
            pos,
            Rotation.NONE);
    piece.postProcess(
        level,
        level.structureManager(),
        level.getChunkSource().getGenerator(),
        random,
        clip,
        chunk,
        pos);
    var context = StructurePieceSerializationContext.fromLevel(level);
    var saved = piece.createTag(context);
    var copy = new ExpeditionPiece(context, saved);
    copy.postProcess(
        level,
        level.structureManager(),
        level.getChunkSource().getGenerator(),
        random,
        clip,
        chunk,
        pos);
    var citizens =
        level.getEntitiesOfClass(
            ColonyCitizen.class, new AABB(pos).expandTowards(17, module.height(), 17));
    helper.assertTrue(citizens.size() == 1, "La pieza recargada duplicó o perdió el habitante");
    helper.assertTrue(saved.getInt("Spawned") == 1, "No guardó el marcador consumido");
    citizens.forEach(Entity::discard);
    done(helper, "expedition_colonists_roundtrip");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 1200)
  public static void expedition_city_full_chunks(GameTestHelper helper) {
    var level =
        helper
            .getLevel()
            .getServer()
            .getLevel(LivingProfiles.get("enceladus").celestial().dimension());
    var city =
        ExpeditionCatalog.ALL.stream()
            .filter(s -> s.body().equals("enceladus") && s.role().equals("city"))
            .findFirst()
            .orElseThrow();
    var placement =
        (DesolatePlacement)
            level
                .registryAccess()
                .registryOrThrow(Registries.STRUCTURE_SET)
                .get(ModPresentation.id("expeditions_enceladus"))
                .placement();
    net.minecraft.world.level.levelgen.structure.StructureStart found = null;
    for (int region = 20; region < 70 && found == null; region++) {
      var candidate = placement.getPotentialStructureChunk(level.getSeed(), region * 384, 4 * 384);
      var chunk = level.getChunk(candidate.x, candidate.z, ChunkStatus.STRUCTURE_STARTS);
      for (var entry : chunk.getAllStarts().entrySet())
        if (entry.getValue().isValid()
            && entry.getKey() instanceof ExpeditionStructure s
            && s.site().equals(city.id())) found = entry.getValue();
    }
    helper.assertTrue(found != null, "No encontró ciudad natural en la búsqueda acotada");
    var context = StructurePieceSerializationContext.fromLevel(level);
    var houseTemplates =
        city.modules().stream()
            .filter(m -> m.role().equals("house"))
            .map(m -> "galaxycraft:" + m.template())
            .collect(java.util.stream.Collectors.toSet());
    var house =
        found.getPieces().stream()
            .filter(
                p ->
                    p instanceof ExpeditionPiece
                        && houseTemplates.contains(p.createTag(context).getString("Template")))
            .findFirst()
            .orElseThrow();
    var box = house.getBoundingBox();
    var ownedTickets = new ArrayList<ChunkPos>();
    try {
      for (int cx = Math.floorDiv(box.minX(), 16); cx <= Math.floorDiv(box.maxX(), 16); cx++)
        for (int cz = Math.floorDiv(box.minZ(), 16); cz <= Math.floorDiv(box.maxZ(), 16); cz++) {
          if (level.setChunkForced(cx, cz, true)) ownedTickets.add(new ChunkPos(cx, cz));
          level.getChunk(cx, cz);
        }
    } catch (RuntimeException error) {
      ownedTickets.forEach(p -> level.setChunkForced(p.x, p.z, false));
      throw error;
    }
    var floor = box.getCenter().atY(box.minY());
    helper.assertTrue(
        !level.getBlockState(floor).isAir(), "No colocó el suelo de la ciudad natural");
    com.mojang.logging.LogUtils.getLogger()
        .info(
            "GALAXYCRAFT_CITY_DIAGNOSTIC site={} house={} floor={} floorBlock={} spawned={}",
            city.id(),
            house.createTag(context).getString("Template"),
            box,
            level.getBlockState(floor),
            house.createTag(context).getInt("Spawned"));
    var checkBox =
        new AABB(
            box.minX() - 24,
            box.minY() - 4,
            box.minZ() - 24,
            box.maxX() + 25,
            box.maxY() + 5,
            box.maxZ() + 25);
    helper.runAfterDelay(
        100,
        () -> {
          try {
            com.mojang.logging.LogUtils.getLogger()
                .info(
                    "GALAXYCRAFT_CITY_DIAGNOSTIC afterLoad={} pieceSpawned={}",
                    level.getEntitiesOfClass(ColonyCitizen.class, checkBox).size(),
                    house.createTag(context).getInt("Spawned"));
            helper.assertTrue(
                !level.getEntitiesOfClass(ColonyCitizen.class, checkBox).isEmpty(),
                "No cargó al habitante generado en el protochunk");
            com.mojang.logging.LogUtils.getLogger()
                .info("GALAXYCRAFT_NATURAL_CITY_FULL {} {}", city.id(), floor);
            done(helper, "expedition_city_full_chunks");
          } finally {
            ownedTickets.forEach(p -> level.setChunkForced(p.x, p.z, false));
          }
        });
  }

  @GameTest(template = "tests/empty", timeoutTicks = 200)
  public static void expedition_shaft_access(GameTestHelper helper) {
    var level = helper.getLevel();
    var site =
        ExpeditionCatalog.ALL.stream()
            .filter(s -> s.body().equals("earth") && s.mega())
            .findFirst()
            .orElseThrow();
    var pos = helper.absolutePos(new BlockPos(9, 1, 9));
    int top = pos.getY() + 38;
    var piece = new ExpeditionAccessPiece(site.id(), pos, top, false);
    var clip =
        new BoundingBox(
            pos.getX() - 2, pos.getY(), pos.getZ() - 2, pos.getX() + 2, top, pos.getZ() + 2);
    piece.postProcess(
        level,
        level.structureManager(),
        level.getChunkSource().getGenerator(),
        RandomSource.create(3),
        clip,
        new ChunkPos(pos),
        pos);
    for (int y = 1; y <= 38; y++) {
      helper.assertTrue(
          level.getBlockState(pos.offset(0, y, 1)).is(Blocks.LADDER),
          "Falta escalera en el acceso");
      helper.assertTrue(level.getBlockState(pos.above(y)).isAir(), "Pozo obstruido");
    }
    for (int floor : new int[] {0, 12, 24})
      helper.assertTrue(
          level.getBlockState(pos.offset(2, floor + 2, 0)).isAir(), "Nivel aislado del recibidor");
    helper.assertTrue(
        level.getBlockState(pos.offset(2, 33, 0)).isAir(),
        "Falta la puerta de entrada en superficie");
    var saved = piece.createTag(StructurePieceSerializationContext.fromLevel(level));
    var copy =
        new ExpeditionAccessPiece(StructurePieceSerializationContext.fromLevel(level), saved);
    helper.assertTrue(
        copy.getBoundingBox().equals(piece.getBoundingBox()), "Perdió el acceso al guardar");
    done(helper, "expedition_shaft_access");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 200)
  public static void expedition_warden_authority(GameTestHelper helper) {
    var level = helper.getLevel();
    var pos = helper.absolutePos(new BlockPos(10, 2, 10));
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++)
        for (int y = 0; y <= 5; y++)
          level.setBlockAndUpdate(
              pos.offset(x, y, z), (y == 0 ? Blocks.IRON_BLOCK : Blocks.AIR).defaultBlockState());
    level.setBlockAndUpdate(pos, ModFrontiers.LUNAR_ANCHOR.get().defaultBlockState());
    var anchor = (BossAnchor) level.getBlockEntity(pos);
    var tag = new CompoundTag();
    tag.putString("EncounterBody", "mars");
    anchor.loadWithComponents(tag, level.registryAccess());
    helper.assertTrue(!anchor.wake(), "Despertó un jefe en el mundo incorrecto");
    tag.putString("EncounterBody", "earth");
    anchor.loadWithComponents(tag, level.registryAccess());
    helper.assertTrue(anchor.wake() && !anchor.wake(), "Duplicó el encuentro");
    var bosses = level.getEntitiesOfClass(PlanetBoss.class, new AABB(pos).inflate(8));
    helper.assertTrue(bosses.size() == 1, "No apareció el custodio");
    var boss = bosses.getFirst();
    helper.assertTrue(
        boss.getLootTable()
            .location()
            .equals(ModPresentation.id("entities/expedition_wardens/earth")),
        "Botín no corresponde al planeta");
    boss.hurt(level.damageSources().genericKill(), 10000);
    helper.assertTrue(anchor.defeated, "No guardó la derrota");
    var saved = anchor.saveWithoutMetadata(level.registryAccess());
    anchor.loadWithComponents(saved, level.registryAccess());
    helper.assertTrue(
        anchor.encounterBody().equals("earth") && anchor.defeated && !anchor.wake(),
        "Perdió identidad/derrota al guardar");
    boss.discard();
    level.removeBlock(pos, false);
    done(helper, "expedition_warden_authority");
  }
}
