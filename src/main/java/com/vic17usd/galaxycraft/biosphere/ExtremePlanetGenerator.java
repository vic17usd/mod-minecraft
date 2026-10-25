package com.vic17usd.galaxycraft.biosphere;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vic17usd.galaxycraft.registry.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.*;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

/** Generador completo 0..255; alturas, columnas y estructuras consultan la misma función pura. */
public final class ExtremePlanetGenerator extends ChunkGenerator {
  public static final MapCodec<ExtremePlanetGenerator> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      LivingProfiles.BODY_CODEC
                          .fieldOf("body")
                          .forGetter(generator -> generator.body),
                      BiomeSource.CODEC
                          .fieldOf("biome_source")
                          .forGetter(ExtremePlanetGenerator::getBiomeSource))
                  .apply(instance, ExtremePlanetGenerator::new));
  private final String body;
  private final LivingProfiles.Profile profile;

  public ExtremePlanetGenerator(String body, BiomeSource biomes) {
    super(biomes);
    this.body = body;
    profile = LivingProfiles.get(body);
    if (body.equals("earth"))
      throw new IllegalArgumentException("La Tierra conserva su generador vanilla");
  }

  public String body() {
    return body;
  }

  public long terrainSeed(RandomState random) {
    return random
        .getOrCreateRandomFactory(ModPresentation.id("biosphere_terrain"))
        .at(0, 0, 0)
        .nextLong();
  }

  private BlockState state(int x, int y, int z, long seed, int height) {
    if (y < 0 || y >= 256) return Blocks.AIR.defaultBlockState();
    if (y <= 2) return Blocks.BEDROCK.defaultBlockState();
    boolean magma = TerrainMath.magma(profile, height);
    if (y > height)
      return magma && y <= 44 ? Blocks.LAVA.defaultBlockState() : Blocks.AIR.defaultBlockState();
    if (y > 8 && y < height - 10 && TerrainMath.cave(x, y, z, seed ^ profile.seed()))
      return y < 22 && profile.terrain().equals("RIFT")
          ? Blocks.LAVA.defaultBlockState()
          : Blocks.AIR.defaultBlockState();
    return ModPlanetContent.BLOCKS
        .get(body + (y >= height - 2 && !magma ? "_regolith" : "_stone"))
        .get()
        .defaultBlockState();
  }

  @Override
  protected MapCodec<? extends ChunkGenerator> codec() {
    return CODEC;
  }

  @Override
  public CompletableFuture<ChunkAccess> fillFromNoise(
      Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
    long seed = terrainSeed(random);
    var pos = new BlockPos.MutableBlockPos();
    var surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
    var floor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
    for (int x = 0; x < 16; x++)
      for (int z = 0; z < 16; z++) {
        int wx = chunk.getPos().getMinBlockX() + x,
            wz = chunk.getPos().getMinBlockZ() + z,
            height = TerrainMath.height(profile, wx, wz, seed),
            top = TerrainMath.magma(profile, height) ? Math.max(height, 44) : height;
        for (int y = 0; y <= top; y++) {
          var state = state(wx, y, wz, seed, height);
          if (!state.isAir()) {
            chunk.setBlockState(pos.set(wx, y, wz), state, false);
            surface.update(x, y, z, state);
            floor.update(x, y, z, state);
          }
        }
      }
    return CompletableFuture.completedFuture(chunk);
  }

  @Override
  public int getBaseHeight(
      int x, int z, Heightmap.Types type, LevelHeightAccessor accessor, RandomState random) {
    long seed = terrainSeed(random);
    int height = TerrainMath.height(profile, x, z, seed);
    for (int y = Math.min(255, accessor.getMaxBuildHeight() - 1);
        y >= Math.max(0, accessor.getMinBuildHeight());
        y--) if (type.isOpaque().test(state(x, y, z, seed, height))) return y + 1;
    return accessor.getMinBuildHeight();
  }

  @Override
  public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor accessor, RandomState random) {
    long seed = terrainSeed(random);
    int height = TerrainMath.height(profile, x, z, seed), bottom = accessor.getMinBuildHeight();
    var states = new BlockState[accessor.getHeight()];
    for (int i = 0; i < states.length; i++) states[i] = state(x, bottom + i, z, seed, height);
    return new NoiseColumn(bottom, states);
  }

  @Override
  public void buildSurface(
      WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
    /* Superficie ya aplicada en fillFromNoise. */
  }

  @Override
  public void applyCarvers(
      WorldGenRegion region,
      long seed,
      RandomState random,
      BiomeManager biomes,
      StructureManager structures,
      ChunkAccess chunk,
      GenerationStep.Carving step) {
    /* Cavernas coherentes incluidas en la función de columna. */
  }

  @Override
  public void spawnOriginalMobs(WorldGenRegion region) {
    var chunk = region.getCenter();
    var random = new WorldgenRandom(new LegacyRandomSource(0));
    random.setDecorationSeed(region.getSeed(), chunk.getMinBlockX(), chunk.getMinBlockZ());
    NaturalSpawner.spawnMobsForChunkGeneration(
        region, region.getBiome(chunk.getWorldPosition().atY(80)), chunk, random);
  }

  @Override
  public int getMinY() {
    return 0;
  }

  @Override
  public int getGenDepth() {
    return 256;
  }

  @Override
  public int getSeaLevel() {
    return 0;
  }

  @Override
  public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
    info.add(
        "GalaxyCraft "
            + body
            + " / "
            + profile.terrain()
            + " / y="
            + TerrainMath.height(profile, pos.getX(), pos.getZ(), terrainSeed(random)));
  }
}
