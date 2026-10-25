package com.vic17usd.galaxycraft.morphology;

import com.vic17usd.galaxycraft.biosphere.LivingProfiles;

/** Once esqueletos originales; los identificadores antiguos se conservan por compatibilidad. */
public enum CreatureDesign {
  HEXAPOD,
  LUMEN,
  CARGO,
  WORM,
  RAPTOR,
  LANCER,
  SPITTER,
  MAGMA,
  SAGE,
  GUARDIAN,
  QUEEN;

  public static CreatureDesign of(String id) {
    if (id.equals("lunar_guardian")) return GUARDIAN;
    if (id.equals("martian_queen")) return QUEEN;
    if (id.endsWith("_crystal_bovine")) return HEXAPOD;
    if (id.endsWith("_lumen_sheep")) return LUMEN;
    if (id.endsWith("_plated_boar")) return CARGO;
    if (id.endsWith("_crawler")) return WORM;
    if (id.endsWith("_raider")) return RAPTOR;
    if (id.endsWith("_colonist")) return SAGE;
    if (id.endsWith("_stalker"))
      return switch (LivingProfiles.get(id.substring(0, id.indexOf('_'))).hunter()) {
        case "magma" -> MAGMA;
        case "spitter" -> SPITTER;
        default -> LANCER;
      };
    throw new IllegalArgumentException("Morfología desconocida: " + id);
  }
}
