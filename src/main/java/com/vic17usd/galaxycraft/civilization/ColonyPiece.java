package com.vic17usd.galaxycraft.civilization;

import com.vic17usd.galaxycraft.biosphere.*;
import com.vic17usd.galaxycraft.ecology.AlienPlantBlock;
import com.vic17usd.galaxycraft.expansion.ModExpansion;
import com.vic17usd.galaxycraft.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Casas, plaza, huerto y caminos serializables; escritura y apariciones limitadas al chunk
 * procesado.
 */
public final class ColonyPiece extends StructurePiece {
  private final String body;
  private final BlockPos origin, center;
  private final int kind;
  private int spawned;

  public ColonyPiece(String body, BlockPos origin, int kind, BlockPos center) {
    super(ModBiosphere.COLONY_PIECE.get(), 0, box(origin, kind, center));
    this.body = body;
    this.origin = origin;
    this.kind = kind;
    this.center = center;
  }

  public ColonyPiece(StructurePieceSerializationContext context, CompoundTag tag) {
    super(ModBiosphere.COLONY_PIECE.get(), tag);
    body = tag.getString("Body");
    LivingProfiles.get(body);
    origin = BlockPos.of(tag.getLong("Origin"));
    center = BlockPos.of(tag.getLong("Center"));
    kind = Math.clamp(tag.getInt("Kind"), 0, 3);
    spawned = tag.getInt("Spawned");
  }

  private static BoundingBox box(BlockPos origin, int kind, BlockPos center) {
    return kind == 3
        ? new BoundingBox(
            Math.min(origin.getX(), center.getX()) - 1,
            Math.min(origin.getY(), center.getY()) - 8,
            Math.min(origin.getZ(), center.getZ()) - 1,
            Math.max(origin.getX(), center.getX()) + 1,
            Math.max(origin.getY(), center.getY()) + 2,
            Math.max(origin.getZ(), center.getZ()) + 1)
        : new BoundingBox(
            origin.getX() - 5,
            origin.getY() - 8,
            origin.getZ() - 5,
            origin.getX() + 5,
            origin.getY() + 9,
            origin.getZ() + 5);
  }

  @Override
  protected void addAdditionalSaveData(
      StructurePieceSerializationContext context, CompoundTag tag) {
    tag.putString("Body", body);
    tag.putLong("Origin", origin.asLong());
    tag.putLong("Center", center.asLong());
    tag.putInt("Kind", kind);
    tag.putInt("Spawned", spawned);
  }

  private static void put(WorldGenLevel level, BoundingBox clip, BlockPos pos, BlockState state) {
    if (clip.isInside(pos) && !level.isOutsideBuildHeight(pos)) level.setBlock(pos, state, 2);
  }

  private void floor(WorldGenLevel level, BoundingBox clip, BlockPos pos, BlockState panel) {
    put(level, clip, pos, panel);
    for (int down = 1; down <= 8; down++) {
      var support = pos.below(down);
      if (!clip.isInside(support)
          || !level.getBlockState(support).isAir() && !level.getBlockState(support).liquid()) break;
      put(level, clip, support, panel);
    }
  }

  @Override
  public void postProcess(
      WorldGenLevel level,
      StructureManager structures,
      ChunkGenerator generator,
      RandomSource random,
      BoundingBox clip,
      ChunkPos chunk,
      BlockPos pivot) {
    String role =
        kind == 2
            ? "geode"
            : kind == 0
                ? "observatory"
                : ModSites.KINDS.get(Math.floorMod(origin.getX() / 16 + origin.getZ() / 16, 5));
    var panel = ModBiosphere.PANELS.get(body + "_" + role + "_panel").get().defaultBlockState();
    if (kind == 3) {
      int dx = origin.getX() - center.getX(),
          dz = origin.getZ() - center.getZ(),
          first = Math.abs(dx),
          distance = first + Math.abs(dz);
      for (int i = 0; i <= distance; i++) {
        double t = distance == 0 ? 0 : (double) i / distance;
        int x = center.getX() + Integer.signum(dx) * Math.min(i, first),
            z = center.getZ() + Integer.signum(dz) * Math.max(0, i - first),
            y = (int) Math.round(center.getY() + (origin.getY() - center.getY()) * t);
        for (int d = -1; d <= 1; d++)
          floor(
              level,
              clip,
              new BlockPos(x + (i >= first ? d : 0), y, z + (i >= first ? 0 : d)),
              panel);
      }
      return;
    }
    int radius = kind == 0 ? 5 : 4;
    for (int x = -radius; x <= radius; x++)
      for (int z = -radius; z <= radius; z++) {
        var base = origin.offset(x, 0, z);
        floor(level, clip, base, panel);
        for (int y = 1; y <= 6; y++)
          put(level, clip, base.above(y), Blocks.AIR.defaultBlockState());
        if (kind == 1) {
          for (int y = 1; y <= 5; y++) {
            boolean wall = Math.abs(x) == 4 || Math.abs(z) == 4;
            put(
                level,
                clip,
                base.above(y),
                y == 5
                    ? panel
                    : wall
                        ? (y == 2 && Math.abs(x + z) % 3 == 0
                            ? Blocks.GLASS.defaultBlockState()
                            : panel)
                        : Blocks.AIR.defaultBlockState());
          }
        }
        if (kind == 2 && Math.abs(x) <= 3 && Math.abs(z) <= 3) {
          var soil =
              body.equals("earth")
                  ? Blocks.DIRT.defaultBlockState()
                  : ModPlanetContent.BLOCKS.get(body + "_regolith").get().defaultBlockState();
          put(level, clip, base, soil);
          var profile = LivingProfiles.get(body);
          var plant =
              ModBiosphere.PLANTS
                  .get(body + "_" + profile.flora().get(Math.floorMod(x + z, 3)))
                  .get()
                  .defaultBlockState()
                  .setValue(AlienPlantBlock.AGE, 2);
          put(level, clip, base.above(), plant);
        }
      }
    if (kind == 1) {
      Direction facing =
          origin.getZ() != center.getZ()
              ? origin.getZ() > center.getZ() ? Direction.NORTH : Direction.SOUTH
              : origin.getX() > center.getX() ? Direction.WEST : Direction.EAST;
      var door = origin.relative(facing, 4).above();
      put(
          level,
          clip,
          door,
          ModExpansion.AIRLOCK.get().defaultBlockState().setValue(DoorBlock.FACING, facing));
      put(
          level,
          clip,
          door.above(),
          ModExpansion.AIRLOCK
              .get()
              .defaultBlockState()
              .setValue(DoorBlock.FACING, facing)
              .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
      var chestPos = origin.offset(2, 1, -2);
      put(level, clip, chestPos, Blocks.CHEST.defaultBlockState());
      if (clip.isInside(chestPos)
          && level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest)
        chest.setLootTable(
            ResourceKey.create(
                Registries.LOOT_TABLE, ModPresentation.id("chests/colonies/" + body)),
            random.nextLong());
      var pos = origin.above();
      if ((spawned & 1) == 0 && clip.isInside(pos)) {
        var citizen = ModBiosphere.CITIZENS.get(body).get().create(level.getLevel());
        if (citizen != null) {
          citizen.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
          citizen.home(center);
          if (level.addFreshEntity(citizen)) spawned |= 1;
        }
      }
    }
    if (kind == 2) {
      String[] species = {"crystal_bovine", "lumen_sheep", "plated_boar"};
      for (int i = 0; i < 3; i++) {
        var pos = origin.offset(i * 2 - 2, 1, -1);
        if ((spawned & (1 << i)) != 0 || !clip.isInside(pos)) continue;
        var animal = ModBiosphere.FAUNA.get(body + "_" + species[i]).get().create(level.getLevel());
        if (animal != null) {
          animal.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
          animal.setPersistenceRequired();
          if (level.addFreshEntity(animal)) spawned |= 1 << i;
        }
      }
    }
  }
}
