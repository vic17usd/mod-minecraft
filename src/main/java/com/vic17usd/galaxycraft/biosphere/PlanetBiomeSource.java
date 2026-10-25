package com.vic17usd.galaxycraft.biosphere;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

/**
 * Tres hábitats por planeta seleccionados por ruido climático de la semilla, no por cuadrículas.
 */
public final class PlanetBiomeSource extends BiomeSource {
  public static final MapCodec<PlanetBiomeSource> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      LivingProfiles.BODY_CODEC.fieldOf("body").forGetter(source -> source.body),
                      Biome.CODEC.fieldOf("plains").forGetter(source -> source.plains),
                      Biome.CODEC.fieldOf("extreme").forGetter(source -> source.extreme),
                      Biome.CODEC.fieldOf("grove").forGetter(source -> source.grove))
                  .apply(instance, PlanetBiomeSource::new));
  private final String body;
  private final Holder<Biome> plains, extreme, grove;

  public PlanetBiomeSource(
      String body, Holder<Biome> plains, Holder<Biome> extreme, Holder<Biome> grove) {
    this.body = body;
    this.plains = plains;
    this.extreme = extreme;
    this.grove = grove;
  }

  @Override
  protected MapCodec<? extends BiomeSource> codec() {
    return CODEC;
  }

  @Override
  protected Stream<Holder<Biome>> collectPossibleBiomes() {
    return Stream.of(plains, extreme, grove);
  }

  @Override
  public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
    var climate = sampler.sample(x + LivingProfiles.get(body).seed() % 4096, 0, z);
    return climate.weirdness() > 2400 ? extreme : climate.humidity() > 500 ? grove : plains;
  }
}
