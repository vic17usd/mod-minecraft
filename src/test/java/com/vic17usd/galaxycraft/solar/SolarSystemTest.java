package com.vic17usd.galaxycraft.solar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/** Comprueba contratos entre catálogo, dimensiones, biomas, minerales y progresión. */
class SolarSystemTest {
    static Stream<CelestialBody> destinations() { return SolarSystem.BODIES.stream().filter(body -> !body.id().equals("earth")); }
    private JsonObject json(String path) {
        var stream = getClass().getResourceAsStream("/" + path);
        assertNotNull(stream, "Falta recurso: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) { throw new AssertionError(exception); }
    }
    private JsonObject data(String path) { return json("data/galaxycraft/" + path + ".json"); }

    @ParameterizedTest @MethodSource("destinations")
    void referenciasDeGeneracionYBotinSonCoherentes(CelestialBody body) {
        String id = "galaxycraft:" + body.id();
        var dimension = data("dimension/" + body.id());
        assertEquals(id, dimension.get("type").getAsString());
        var generator = dimension.getAsJsonObject("generator");
        assertEquals(id, generator.get("settings").getAsString());
        assertEquals(id, generator.getAsJsonObject("biome_source").get("biome").getAsString());
        var type = data("dimension_type/" + body.id());
        var noise = data("worldgen/noise_settings/" + body.id());
        assertEquals(type.get("height"), noise.getAsJsonObject("noise").get("height"));
        assertEquals(type.get("min_y"), noise.getAsJsonObject("noise").get("min_y"));
        assertEquals(0, type.get("height").getAsInt() % 16);
        var biome = data("worldgen/biome/" + body.id());
        for (var step : biome.getAsJsonArray("features")) for (var reference : step.getAsJsonArray()) {
            String placed = reference.getAsString().substring("galaxycraft:".length());
            var feature = data("worldgen/placed_feature/" + placed);
            data("worldgen/configured_feature/" + feature.get("feature").getAsString().substring("galaxycraft:".length()));
        }
        var ore = data("worldgen/configured_feature/" + body.id() + "_ore");
        assertEquals(id + "_stone", ore.getAsJsonObject("config").getAsJsonArray("targets").get(0)
                .getAsJsonObject().getAsJsonObject("target").get("block").getAsString());
        var loot = data("loot_table/blocks/" + body.id() + "_ore");
        assertEquals("galaxycraft:" + body.resource(), loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
        for (String suffix : new String[]{"stone", "regolith", "ore"}) {
            String name = body.id() + "_" + suffix;
            json("assets/galaxycraft/blockstates/" + name + ".json");
            json("assets/galaxycraft/models/block/" + name + ".json");
            json("assets/galaxycraft/models/item/" + name + ".json");
        }
    }

    @Test void catalogoNoDuplicaDimensionesNiRecursosYConservaLaTierra() {
        Set<String> ids = new HashSet<>(), resources = new HashSet<>();
        for (var body : SolarSystem.BODIES) {
            assertTrue(ids.add(body.id()));
            assertTrue(Double.isFinite(body.gravity()) && body.gravity() > 0);
            assertTrue(body.tier() >= 0 && body.tier() <= 3);
            if (!body.id().equals("earth")) assertTrue(resources.add(body.resource()));
        }
        for (var body : SolarSystem.BODIES) assertTrue(body.parent().equals("sun") || ids.contains(body.parent()));
        assertEquals(Level.OVERWORLD, SolarSystem.BODIES.getFirst().dimension());
        assertTrue(SolarSystem.BODIES.getFirst().breathable());
        assertTrue(destinations().noneMatch(CelestialBody::breathable));
    }

    @Test void nivelesImpidenSaltarLaProgresionPlanetaria() {
        for (var body : SolarSystem.BODIES) {
            assertTrue(SolarSystem.accessible(3, body));
            if (body.tier() > 1) assertFalse(SolarSystem.accessible(1, body));
            if (body.tier() == 3) assertFalse(SolarSystem.accessible(2, body));
        }
        assertFalse(SolarSystem.accessible(0, SolarSystem.BODIES.getFirst()));
        assertFalse(SolarSystem.accessible(4, SolarSystem.BODIES.getFirst()));
    }

    @Test void recetasExigenRecursosDeDestinosAlcanzablesConElNivelAnterior() {
        Set<String> available = new HashSet<>(Set.of("titanium_ore", "tier1_rocket", "tier2_rocket"));
        for (int tier = 2; tier <= 3; tier++) {
            for (var body : SolarSystem.BODIES) if (SolarSystem.accessible(tier - 1, body)) available.add(body.resource());
            var recipe = data("recipe/tier" + tier + "_rocket");
            for (var ingredient : recipe.getAsJsonObject("key").entrySet()) {
                String item = ingredient.getValue().getAsJsonObject().get("item").getAsString().substring("galaxycraft:".length());
                assertTrue(available.contains(item), "Recurso inaccesible antes de fabricar nivel " + tier + ": " + item);
            }
        }
        assertTrue(data("recipe/tier2_rocket").toString().contains("lunar_crystal"));
        assertTrue(data("recipe/tier3_rocket").toString().contains("martian_metal"));
        assertTrue(data("recipe/tier3_rocket").toString().contains("venusian_sulfur"));
        assertTrue(data("recipe/tier3_rocket").toString().contains("mercurian_cobalt"));
    }
}
