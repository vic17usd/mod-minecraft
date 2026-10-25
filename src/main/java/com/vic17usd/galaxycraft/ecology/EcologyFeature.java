package com.vic17usd.galaxycraft.ecology;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vic17usd.galaxycraft.biosphere.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** Manchas nativas de radio dos, con escrituras dentro del chunk de la colocación. */
public final class EcologyFeature extends Feature<EcologyFeature.Config> {
  public record Config(String body) implements FeatureConfiguration {
    public static final Codec<Config> CODEC =
        RecordCodecBuilder.create(
            instance ->
                instance
                    .group(LivingProfiles.BODY_CODEC.fieldOf("body").forGetter(Config::body))
                    .apply(instance, Config::new));
  }

  public EcologyFeature() {
    super(Config.CODEC);
  }

  @Override
  public boolean place(FeaturePlaceContext<Config> context) {
    var level = context.level();
    var origin = context.origin();
    var profile = LivingProfiles.get(context.config().body());
    if (!level.getLevel().dimension().equals(profile.celestial().dimension())) return false;
    boolean placed = false;
    for (int i = 0; i < 6; i++) {
      var pos = origin.offset(context.random().nextInt(5) - 2, 0, context.random().nextInt(5) - 2);
      if ((pos.getX() >> 4) != (origin.getX() >> 4) || (pos.getZ() >> 4) != (origin.getZ() >> 4))
        continue;
      pos =
          level.getHeightmapPos(
              net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, pos);
      if (!level.isEmptyBlock(pos)
          || !AlienPlantBlock.soil(profile.body(), level.getBlockState(pos.below()))) continue;
      if (profile.terrain().equals("GEYSERS") && context.random().nextInt(8) == 0) {
        level.setBlock(
            pos.below(), ModBiosphere.VENTS.get(profile.body()).get().defaultBlockState(), 2);
        placed = true;
        continue;
      }
      String style = profile.flora().get(context.random().nextInt(3));
      var state =
          ModBiosphere.PLANTS
              .get(profile.body() + "_" + style)
              .get()
              .defaultBlockState()
              .setValue(AlienPlantBlock.AGE, context.random().nextInt(3));
      if (state.canSurvive(level, pos)) {
        level.setBlock(pos, state, 2);
        placed = true;
      }
    }
    return placed;
  }
}
