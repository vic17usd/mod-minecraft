package com.vic17usd.galaxycraft.expedition;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vic17usd.galaxycraft.biosphere.LivingProfiles;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.*;

/** Un candidato por región compartido por las diez entradas; mantiene soporte nativo de /locate. */
public final class DesolatePlacement extends RandomSpreadStructurePlacement {
  public static final MapCodec<DesolatePlacement> CODEC =
      RecordCodecBuilder.<DesolatePlacement>mapCodec(
              i ->
                  i.group(
                          LivingProfiles.BODY_CODEC.fieldOf("body").forGetter(p -> p.body),
                          Codec.intRange(256, 4096)
                              .fieldOf("spacing")
                              .forGetter(DesolatePlacement::spacing),
                          Codec.intRange(128, 4095)
                              .fieldOf("separation")
                              .forGetter(DesolatePlacement::separation),
                          Codec.intRange(0, Integer.MAX_VALUE)
                              .fieldOf("salt")
                              .forGetter(DesolatePlacement::salt))
                      .apply(i, DesolatePlacement::new))
          .validate(
              p ->
                  p.spacing() > p.separation()
                      ? DataResult.success(p)
                      : DataResult.error(() -> "spacing debe superar separation"));
  private final String body;

  public DesolatePlacement(String body, int spacing, int separation, int salt) {
    super(spacing, separation, RandomSpreadType.LINEAR, salt);
    this.body = body;
  }

  @Override
  protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
    return ExpeditionDimensions.allows(state, body) && super.isPlacementChunk(state, x, z);
  }

  @Override
  public StructurePlacementType<?> type() {
    return ModExpeditions.PLACEMENT.get();
  }
}
