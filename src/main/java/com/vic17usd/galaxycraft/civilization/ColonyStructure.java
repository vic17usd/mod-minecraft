package com.vic17usd.galaxycraft.civilization;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vic17usd.galaxycraft.biosphere.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;

/** Asentamiento modular de superficie: cada casa consulta el terreno, con pendientes acotadas. */
public final class ColonyStructure extends Structure {
  public static final MapCodec<ColonyStructure> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      settingsCodec(instance),
                      LivingProfiles.BODY_CODEC.fieldOf("body").forGetter(colony -> colony.body))
                  .apply(instance, ColonyStructure::new));
  private final String body;

  public ColonyStructure(StructureSettings settings, String body) {
    super(settings);
    this.body = body;
  }

  @Override
  public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
    int x = context.chunkPos().getMinBlockX() + 8, z = context.chunkPos().getMinBlockZ() + 8;
    var positions = new ArrayList<BlockPos>();
    int[][] offsets = {{0, 0}, {-16, 0}, {16, 0}, {0, -16}, {0, 16}, {16, 16}, {-16, -16}};
    for (var offset : offsets) {
      int wx = x + offset[0],
          wz = z + offset[1],
          height =
              context
                  .chunkGenerator()
                  .getBaseHeight(
                      wx,
                      wz,
                      Heightmap.Types.WORLD_SURFACE_WG,
                      context.heightAccessor(),
                      context.randomState());
      int min = height, max = height;
      for (int dx : new int[] {-4, 4})
        for (int dz : new int[] {-4, 4}) {
          int sample =
              context
                  .chunkGenerator()
                  .getBaseHeight(
                      wx + dx,
                      wz + dz,
                      Heightmap.Types.WORLD_SURFACE_WG,
                      context.heightAccessor(),
                      context.randomState());
          min = Math.min(min, sample);
          max = Math.max(max, sample);
        }
      if (max - min > 8
          || height < context.heightAccessor().getMinBuildHeight() + 12
          || height + 10 >= context.heightAccessor().getMaxBuildHeight()
          || !context
              .chunkGenerator()
              .getBaseColumn(wx, wz, context.heightAccessor(), context.randomState())
              .getBlock(height - 1)
              .getFluidState()
              .isEmpty()) return Optional.empty();
      positions.add(new BlockPos(wx, height - 1, wz));
    }
    var center = positions.getFirst();
    if (positions.stream().anyMatch(pos -> Math.abs(pos.getY() - center.getY()) > 12))
      return Optional.empty();
    boolean fifth = context.random().nextBoolean();
    return Optional.of(
        new GenerationStub(
            center,
            builder -> {
              builder.addPiece(new ColonyPiece(body, center, 0, center));
              for (int i = 1; i <= 4; i++) {
                builder.addPiece(new ColonyPiece(body, positions.get(i), 1, center));
                builder.addPiece(new ColonyPiece(body, positions.get(i), 3, center));
              }
              builder.addPiece(new ColonyPiece(body, positions.get(5), 2, center));
              if (fifth) {
                builder.addPiece(new ColonyPiece(body, positions.get(6), 1, center));
                builder.addPiece(new ColonyPiece(body, positions.get(6), 3, center));
              }
            }));
  }

  @Override
  public StructureType<?> type() {
    return ModBiosphere.COLONY.get();
  }
}
