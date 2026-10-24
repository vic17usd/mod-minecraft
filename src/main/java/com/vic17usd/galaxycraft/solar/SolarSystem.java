package com.vic17usd.galaxycraft.solar;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Catálogo inmutable: la Tierra reutiliza el Overworld y conserva los mundos anteriores. */
public final class SolarSystem {
    public static final List<CelestialBody> BODIES = load();
    private static final Map<ResourceKey<Level>, CelestialBody> BY_DIMENSION = BODIES.stream()
            .collect(Collectors.toUnmodifiableMap(CelestialBody::dimension, body -> body));
    private static List<CelestialBody> load() {
        var stream = SolarSystem.class.getResourceAsStream("/solar_system.json");
        if (stream == null) throw new IllegalStateException("Falta el catálogo del Sistema Solar");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return List.copyOf(Arrays.asList(new Gson().fromJson(reader, CelestialBody[].class)));
        } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
    public static CelestialBody body(Level level) {
        return BY_DIMENSION.get(level.dimension());
    }
    public static CelestialBody destination(int index) { return BODIES.get(Math.floorMod(index, BODIES.size())); }
    public static int indexOf(String id) {
        for (int i = 0; i < BODIES.size(); i++) if (BODIES.get(i).id().equals(id)) return i;
        return 0;
    }
    public static boolean accessible(int tier, CelestialBody body) { return tier >= body.tier() && tier >= 1 && tier <= 3; }
    private SolarSystem() {}
}
