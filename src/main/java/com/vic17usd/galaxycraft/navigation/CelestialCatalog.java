package com.vic17usd.galaxycraft.navigation;

import com.google.gson.Gson;
import com.vic17usd.galaxycraft.solar.CelestialBody;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;

/** Jerarquía de navegación extensible; los sistemas futuros necesitan destinos reales registrados. */
public final class CelestialCatalog {
    public record StellarSystem(String id, String translation, int minimumTier, boolean enabled, List<String> planets) {}
    public static final List<StellarSystem> SYSTEMS = load();
    private static List<StellarSystem> load() {
        try (var stream = CelestialCatalog.class.getResourceAsStream("/stellar_systems.json")) {
            if (stream == null) throw new IllegalStateException("Falta stellar_systems.json");
            return List.copyOf(Arrays.asList(new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), StellarSystem[].class)));
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
    public static Optional<CelestialBody> body(String id) { return SolarSystem.BODIES.stream().filter(body -> body.id().equals(id)).findFirst(); }
    public static List<CelestialBody> planets(StellarSystem system) { return system.planets().stream().map(CelestialCatalog::body).flatMap(Optional::stream).toList(); }
    public static List<CelestialBody> moons(CelestialBody planet) { return SolarSystem.BODIES.stream().filter(body -> body.parent().equals(planet.id())).toList(); }
    public static Optional<StellarSystem> system(CelestialBody body) {
        return SYSTEMS.stream().filter(system -> system.planets().contains(body.id()) || system.planets().contains(body.parent())).findFirst();
    }
    private CelestialCatalog() {}
}
