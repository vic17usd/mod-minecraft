package com.vic17usd.galaxycraft.registry;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import com.vic17usd.galaxycraft.item.SchematicItem;
import com.vic17usd.galaxycraft.worldgen.PlanetarySiteStructure;
import com.vic17usd.galaxycraft.worldgen.PlanetarySitePiece;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Cinco arquetipos por cuerpo, incluida la Tierra; el manifiesto generado se contrasta en pruebas. */
public final class ModSites {
    public static final List<String> KINDS = List.of("observatory", "wreck", "sanctuary", "geode", "vault");
    public static final Map<String, DeferredItem<SchematicItem>> SCHEMATICS = new LinkedHashMap<>();
    public static void registerItems() {
        for (var body : SolarSystem.BODIES) for (String kind : KINDS) {
            String site = body.id() + "_" + kind;
            SCHEMATICS.put(site, ModItems.ITEMS.register(site + "_schematic", () -> new SchematicItem("expedition/" + site)));
        }
    }
    public static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, GalaxyCraft.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<PlanetarySiteStructure>> SITE = STRUCTURES.register("planetary_site", () -> () -> PlanetarySiteStructure.CODEC);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, GalaxyCraft.MOD_ID);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PIECE = PIECES.register("planetary_site", () -> PlanetarySitePiece::new);
    private ModSites() {}
}
