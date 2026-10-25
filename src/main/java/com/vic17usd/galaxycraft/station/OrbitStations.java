package com.vic17usd.galaxycraft.station;

import com.vic17usd.galaxycraft.solar.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.Level;

/**
 * Dimensiones orbitales compartidas; cada propietario ocupa una parcela independiente y
 * persistente.
 */
public final class OrbitStations {
  public static boolean isOrbit(Level level) {
    var id = level.dimension().location();
    return id.getNamespace().equals("galaxycraft")
        && id.getPath().startsWith("orbit/")
        && CelestialBodyId(id.getPath().substring(6)) != null;
  }

  public static CelestialBody CelestialBodyId(String id) {
    return SolarSystem.BODIES.stream()
        .filter(body -> body.id().equals(id))
        .findFirst()
        .orElse(null);
  }

  public static CelestialBody parent(Level level) {
    return isOrbit(level)
        ? CelestialBodyId(level.dimension().location().getPath().substring(6))
        : SolarSystem.body(level);
  }

  public static ResourceKey<Level> dimension(CelestialBody body) {
    return ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath("galaxycraft", "orbit/" + body.id()));
  }

  private OrbitStations() {}
}
