package com.vic17usd.galaxycraft.expedition;

import com.vic17usd.galaxycraft.biosphere.ModBiosphere;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Acceso vertical de 5×5 y apoyos de 3×3: nunca escribe fuera del chunk en proceso. */
public final class ExpeditionAccessPiece extends StructurePiece {
  private final String site;
  private final BlockPos origin;
  private final int top;
  private final boolean support;

  public ExpeditionAccessPiece(String site, BlockPos origin, int top, boolean support) {
    super(
        ModExpeditions.ACCESS.get(),
        0,
        new BoundingBox(
            origin.getX() - 2,
            support ? origin.getY() - 32 : origin.getY(),
            origin.getZ() - 2,
            origin.getX() + 2,
            top,
            origin.getZ() + 2));
    this.site = site;
    this.origin = origin;
    this.top = top;
    this.support = support;
  }

  public ExpeditionAccessPiece(StructurePieceSerializationContext context, CompoundTag tag) {
    super(ModExpeditions.ACCESS.get(), tag);
    site = tag.getString("Site");
    ExpeditionCatalog.get(site);
    origin = BlockPos.of(tag.getLong("Origin"));
    top = tag.getInt("Top");
    support = tag.getBoolean("Support");
  }

  @Override
  protected void addAdditionalSaveData(
      StructurePieceSerializationContext context, CompoundTag tag) {
    tag.putString("Site", site);
    tag.putLong("Origin", origin.asLong());
    tag.putInt("Top", top);
    tag.putBoolean("Support", support);
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
    var design = ExpeditionCatalog.get(site);
    if (!ExpeditionDimensions.matches(design.body(), level.getLevel().dimension())) return;
    var panel = ModBiosphere.PANELS.get(design.body() + "_vault_panel").get().defaultBlockState();
    if (support) {
      for (int x = -1; x <= 1; x++)
        for (int z = -1; z <= 1; z++)
          for (int down = 1; down <= 32; down++) {
            var pos = origin.offset(x, -down, z);
            if (!clip.isInside(pos) || level.isOutsideBuildHeight(pos)) continue;
            if (!level.getBlockState(pos).isAir()
                && level.getBlockState(pos).getFluidState().isEmpty()) break;
            level.setBlock(pos, panel, 2);
          }
      return;
    }
    for (int y = origin.getY() + 1; y <= top; y++)
      for (int x = -2; x <= 2; x++)
        for (int z = -2; z <= 2; z++) {
          var pos = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
          if (!clip.isInside(pos) || level.isOutsideBuildHeight(pos)) continue;
          boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2;
          // Cada nivel conecta por oeste/este con el recibidor, sin sellar al jugador en el pozo.
          boolean opening =
              Math.abs(x) == 2
                  && z == 0
                  && (((y - origin.getY()) % 12 >= 1 && (y - origin.getY()) % 12 <= 3)
                      || y >= top - 5 && y <= top - 3);
          var state =
              wall && !opening
                  ? panel
                  : x == 0 && z == 1
                      ? Blocks.LADDER
                          .defaultBlockState()
                          .setValue(LadderBlock.FACING, Direction.NORTH)
                      : Blocks.AIR.defaultBlockState();
          level.setBlock(pos, state, 2);
        }
  }
}
