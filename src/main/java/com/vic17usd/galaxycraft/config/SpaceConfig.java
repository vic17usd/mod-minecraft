package com.vic17usd.galaxycraft.config;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Configuración de servidor, con posibilidad de sobrescribirla por mundo. */
public final class SpaceConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SIMULATE_VACUUM;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> AIRLESS_DIMENSIONS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SIMULATE_VACUUM = builder.comment("Simula vacío en todas las dimensiones. Solo para pruebas.")
                .define("simulateVacuum", false);
        AIRLESS_DIMENSIONS = builder.comment("Identificadores de dimensiones sin atmósfera; no crea dimensiones.")
                .defineListAllowEmpty("airlessDimensions", List.of("galaxycraft:moon"),
                        () -> "galaxycraft:moon",
                        value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
        SPEC = builder.build();
    }

    private SpaceConfig() {}
}
