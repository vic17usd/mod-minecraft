package com.vic17usd.galaxycraft.astronomy;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Opciones gráficas: tamaño físico por defecto, sin alterar distancias o combustible. */
public final class SkySettings {
  public static final ModConfigSpec SPEC;
  public static final ModConfigSpec.IntValue SCATTER_SAMPLES;
  public static final ModConfigSpec.DoubleValue MAGNIFICATION;

  static {
    var b = new ModConfigSpec.Builder();
    SCATTER_SAMPLES =
        b.comment("Muestras de dispersión simple: 0 desactiva la atmósfera; 2-8 ajusta calidad.")
            .defineInRange("atmosphereSamples", 4, 0, 8);
    MAGNIFICATION =
        b.comment("1 = tamaño angular físico; aumentar es una licencia artística.")
            .defineInRange("celestialMagnification", 1.0, 1.0, 8.0);
    SPEC = b.build();
  }

  private SkySettings() {}
}
