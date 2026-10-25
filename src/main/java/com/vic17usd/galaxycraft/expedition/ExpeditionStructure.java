package com.vic17usd.galaxycraft.expedition;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;

/** Ensambla NBT originales modulares, muestrea toda la huella y proporciona acceso al subsuelo. */
public final class ExpeditionStructure extends Structure {
  public static final Codec<String> SITE_CODEC =
      Codec.STRING.validate(
          id -> {
            try {
              ExpeditionCatalog.get(id);
              return DataResult.success(id);
            } catch (IllegalArgumentException e) {
              return DataResult.error(e::getMessage);
            }
          });
  public static final MapCodec<ExpeditionStructure> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(settingsCodec(i), SITE_CODEC.fieldOf("site").forGetter(s -> s.site))
                  .apply(i, ExpeditionStructure::new));
  private final String site;

  public ExpeditionStructure(StructureSettings settings, String site) {
    super(settings);
    this.site = site;
  }

  public String site() {
    return site;
  }

  public static int baseY(
      int minimum, int maximum, int bottom, int top, int height, boolean underground, int burial) {
    int y = underground ? minimum - height - burial : maximum;
    return (!underground && maximum - minimum > 32) || y < bottom + 8 || y + height >= top - 6
        ? Integer.MIN_VALUE
        : y;
  }

  @Override
  public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
    var design = ExpeditionCatalog.get(site);
    if (!ExpeditionDimensions.allows(context.randomState(), design.body())) return Optional.empty();
    Rotation rotation = Rotation.getRandom(context.random());
    var anchor =
        new BlockPos(context.chunkPos().getMiddleBlockX(), 0, context.chunkPos().getMiddleBlockZ());
    int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
    for (var module : design.modules())
      for (int dx : new int[] {0, module.width() / 2, module.width() - 1})
        for (int dz : new int[] {0, module.depth() / 2, module.depth() - 1}) {
          var sample =
              new BlockPos(module.x() + dx, 0, module.z() + dz).rotate(rotation).offset(anchor);
          int height =
              context
                  .chunkGenerator()
                  .getFirstFreeHeight(
                      sample.getX(),
                      sample.getZ(),
                      Heightmap.Types.WORLD_SURFACE_WG,
                      context.heightAccessor(),
                      context.randomState());
          if (!context
              .chunkGenerator()
              .getBaseColumn(
                  sample.getX(), sample.getZ(), context.heightAccessor(), context.randomState())
              .getBlock(height - 1)
              .getFluidState()
              .isEmpty()) return Optional.empty();
          min = Math.min(min, height);
          max = Math.max(max, height);
        }
    int base =
        baseY(
            min,
            max,
            context.heightAccessor().getMinBuildHeight(),
            context.heightAccessor().getMaxBuildHeight(),
            design.height(),
            design.underground(),
            design.burial());
    if (base == Integer.MIN_VALUE) return Optional.empty();
    var origin = anchor.atY(base);
    var entry =
        new BlockPos(design.entranceX(), 0, design.entranceZ()).rotate(rotation).offset(anchor);
    int ground =
        context
            .chunkGenerator()
            .getFirstFreeHeight(
                entry.getX(),
                entry.getZ(),
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState());
    if (design.underground() && ground + 5 >= context.heightAccessor().getMaxBuildHeight())
      return Optional.empty();
    return Optional.of(
        new GenerationStub(
            origin,
            builder -> {
              for (var module : design.modules()) {
                var template =
                    ResourceLocation.fromNamespaceAndPath("galaxycraft", module.template());
                if (context.structureTemplateManager().get(template).isEmpty())
                  throw new IllegalStateException("Falta módulo " + template);
                builder.addPiece(
                    new ExpeditionPiece(
                        context.structureTemplateManager(),
                        site,
                        template,
                        new BlockPos(module.x(), module.y(), module.z())
                            .rotate(rotation)
                            .offset(origin),
                        rotation));
              }
              if (design.underground())
                builder.addPiece(
                    new ExpeditionAccessPiece(site, entry.atY(base), ground + 5, false));
              else
                for (var module : design.modules())
                  if (module.foundation())
                    for (int dx : new int[] {2, module.width() - 3})
                      for (int dz : new int[] {2, module.depth() - 3})
                        builder.addPiece(
                            new ExpeditionAccessPiece(
                                site,
                                new BlockPos(module.x() + dx, 0, module.z() + dz)
                                    .rotate(rotation)
                                    .offset(origin),
                                base,
                                true));
            }));
  }

  @Override
  public StructureType<?> type() {
    return ModExpeditions.STRUCTURE.get();
  }
}
