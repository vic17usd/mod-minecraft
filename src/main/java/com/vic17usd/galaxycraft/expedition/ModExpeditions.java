package com.vic17usd.galaxycraft.expedition;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.registry.ModSites;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.neoforge.registries.*;

/** Nuevos codecs; los de 0.8 siguen registrados para leer starts y piezas ya guardados. */
public final class ModExpeditions {
  public static final DeferredHolder<StructureType<?>, StructureType<ExpeditionStructure>>
      STRUCTURE =
          ModSites.STRUCTURES.register(
              "exclusive_expedition", () -> () -> ExpeditionStructure.CODEC);
  public static final DeferredHolder<StructurePieceType, StructurePieceType> PIECE =
      ModSites.PIECES.register("expedition_module", () -> ExpeditionPiece::new);
  public static final DeferredHolder<StructurePieceType, StructurePieceType> ACCESS =
      ModSites.PIECES.register("expedition_access", () -> ExpeditionAccessPiece::new);
  public static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS =
      DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<
          StructurePlacementType<?>, StructurePlacementType<DesolatePlacement>>
      PLACEMENT = PLACEMENTS.register("desolate_spread", () -> () -> DesolatePlacement.CODEC);

  public static void register(net.neoforged.bus.api.IEventBus bus) {
    PLACEMENTS.register(bus);
  }

  private ModExpeditions() {}
}
