package com.vic17usd.galaxycraft.worldgen;

import com.vic17usd.galaxycraft.registry.ModSites;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.List;

/** Pieza serializable y rotada. El modificador de erosión solo cambia roca de la propia plantilla. */
public final class PlanetarySitePiece extends TemplateStructurePiece {
    public PlanetarySitePiece(StructureTemplateManager manager, ResourceLocation template, BlockPos origin, Rotation rotation) {
        super(ModSites.PIECE.get(), 0, manager, template, template.toString(), settings(template, rotation), origin);
    }
    public PlanetarySitePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModSites.PIECE.get(), tag, context.structureTemplateManager(), template -> settings(template, Rotation.valueOf(tag.getString("Rot"))));
    }
    private static StructurePlaceSettings settings(ResourceLocation template, Rotation rotation) {
        String body = template.getPath().split("/")[1].split("_")[0];
        var settings = new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(10, 0, 10))
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        if (!body.equals("earth")) {
            var rock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("galaxycraft", body + "_stone"));
            var soil = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("galaxycraft", body + "_regolith"));
            settings.addProcessor(new RuleProcessor(List.of(new ProcessorRule(new RandomBlockMatchTest(rock, .06F),
                    AlwaysTrueTest.INSTANCE, soil.defaultBlockState()))));
        }
        return settings;
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag); tag.putString("Rot", placeSettings.getRotation().name());
    }
    @Override protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        // Los cofres ya contienen LootTable en NBT; no hacen falta marcadores especiales.
    }
}
