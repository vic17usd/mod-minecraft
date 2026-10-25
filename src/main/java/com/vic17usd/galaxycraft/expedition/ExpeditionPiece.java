package com.vic17usd.galaxycraft.expedition;

import com.vic17usd.galaxycraft.biosphere.ModBiosphere;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Pieza NBT rotada y serializable; sus habitantes son finitos, persistentes y nativos. */
public final class ExpeditionPiece extends TemplateStructurePiece {
  private final String site;
  private int spawned;

  private static StructurePlaceSettings settings(Rotation rotation) {
    return new StructurePlaceSettings()
        .setRotation(rotation)
        .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
        .setIgnoreEntities(true);
  }

  public ExpeditionPiece(
      StructureTemplateManager manager,
      String site,
      ResourceLocation template,
      BlockPos origin,
      Rotation rotation) {
    super(
        ModExpeditions.PIECE.get(),
        0,
        manager,
        template,
        template.toString(),
        settings(rotation),
        origin);
    this.site = site;
    ExpeditionCatalog.get(site);
  }

  public ExpeditionPiece(StructurePieceSerializationContext context, CompoundTag tag) {
    super(
        ModExpeditions.PIECE.get(),
        tag,
        context.structureTemplateManager(),
        t -> settings(Rotation.valueOf(tag.getString("Rot"))));
    site = tag.getString("Site");
    ExpeditionCatalog.get(site);
    spawned = tag.getInt("Spawned");
  }

  @Override
  protected void addAdditionalSaveData(
      StructurePieceSerializationContext context, CompoundTag tag) {
    super.addAdditionalSaveData(context, tag);
    tag.putString("Rot", placeSettings.getRotation().name());
    tag.putString("Site", site);
    tag.putInt("Spawned", spawned);
  }

  @Override
  public void postProcess(
      net.minecraft.world.level.WorldGenLevel level,
      net.minecraft.world.level.StructureManager structures,
      net.minecraft.world.level.chunk.ChunkGenerator generator,
      RandomSource random,
      BoundingBox clip,
      net.minecraft.world.level.ChunkPos chunk,
      BlockPos pivot) {
    if (ExpeditionDimensions.matches(
        ExpeditionCatalog.get(site).body(), level.getLevel().dimension()))
      super.postProcess(level, structures, generator, random, clip, chunk, pivot);
  }

  @Override
  protected void handleDataMarker(
      String marker,
      BlockPos pos,
      ServerLevelAccessor level,
      RandomSource random,
      BoundingBox clip) {
    var design = ExpeditionCatalog.get(site);
    if (!clip.isInside(pos)
        || !ExpeditionDimensions.matches(design.body(), level.getLevel().dimension())) return;
    String[] parts = marker.split(":");
    if (parts.length != 2) return;
    int bit;
    try {
      bit = Integer.parseInt(parts[1]);
    } catch (NumberFormatException ignored) {
      return;
    }
    if (bit < 0 || bit >= 24 || (spawned & (1 << bit)) != 0) return;
    Mob mob =
        switch (parts[0]) {
          case "citizen" -> ModBiosphere.CITIZENS.get(design.body()).get().create(level.getLevel());
          case "fauna" ->
              ModBiosphere.FAUNA.get(design.body() + "_lumen_sheep").get().create(level.getLevel());
          default -> null;
        };
    if (mob == null) return;
    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    mob.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
    mob.setPersistenceRequired();
    if (mob instanceof com.vic17usd.galaxycraft.civilization.ColonyCitizen citizen)
      citizen.home(pos);
    // Consultar la región en generación, no pedir FULL al ServerLevel desde su propio trabajo.
    if (level.noCollision(mob) && level.addFreshEntity(mob)) spawned |= 1 << bit;
  }
}
