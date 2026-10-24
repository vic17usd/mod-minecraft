package com.vic17usd.galaxycraft.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vic17usd.galaxycraft.registry.ModSites;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.block.Rotation;
import java.util.Optional;

/** Colocación consciente del relieve: evita mazmorras al aire libre o recortadas por el fondo del mundo. */
public final class PlanetarySiteStructure extends Structure {
    public static final MapCodec<PlanetarySiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance), ResourceLocation.CODEC.fieldOf("template").forGetter(site -> site.template),
            Codec.BOOL.fieldOf("underground").forGetter(site -> site.underground),
            Codec.intRange(0, 64).fieldOf("burial_depth").forGetter(site -> site.depth)
    ).apply(instance, PlanetarySiteStructure::new));
    private final ResourceLocation template;
    private final boolean underground;
    private final int depth;
    public PlanetarySiteStructure(StructureSettings settings, ResourceLocation template, boolean underground, int depth) {
        super(settings); this.template = template; this.underground = underground; this.depth = depth;
    }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMinBlockX(), z = context.chunkPos().getMinBlockZ();
        int minimum = Integer.MAX_VALUE, maximum = Integer.MIN_VALUE;
        // Nueve muestras cubren la huella completa de 21 × 21, no solo el centro.
        for (int dx : new int[]{0, 10, 20}) for (int dz : new int[]{0, 10, 20}) {
            int height = context.chunkGenerator().getFirstFreeHeight(x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState());
            minimum = Math.min(minimum, height); maximum = Math.max(maximum, height);
        }
        var size = context.structureTemplateManager().getOrCreate(template).getSize();
        if (size.getX() != 21 || size.getZ() != 21 || size.getY() > 12) return Optional.empty();
        int y = placementY(minimum, maximum, context.heightAccessor().getMinBuildHeight(),
                context.heightAccessor().getMaxBuildHeight(), size.getY(), underground, depth);
        if (y == Integer.MIN_VALUE) return Optional.empty();
        BlockPos origin = new BlockPos(x, y, z);
        Rotation rotation = Rotation.getRandom(context.random());
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(
                new PlanetarySitePiece(context.structureTemplateManager(), template, origin, rotation))));
    }
    /** Función pura utilizada también para probar límites en mundos con diferentes alturas. */
    public static int placementY(int minimum, int maximum, int bottom, int top, int height, boolean underground, int depth) {
        if (maximum - minimum > 12) return Integer.MIN_VALUE;
        int y = underground ? minimum - depth - height : minimum - 1;
        if (y < bottom + 8 || y + height >= top - 4) return Integer.MIN_VALUE;
        return y;
    }
    @Override public StructureType<?> type() { return ModSites.SITE.get(); }
}
