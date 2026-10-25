package com.vic17usd.galaxycraft.biosphere;

import com.google.gson.Gson;
import com.mojang.serialization.*;
import com.vic17usd.galaxycraft.solar.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Catálogo único y validado de hábitats; compartido por registros, datos de mundo y cliente. */
public final class LivingProfiles {
  public record Profile(
      String body,
      String terrain,
      List<String> flora,
      int sky,
      int fog,
      int color,
      int fogDistance,
      int seed,
      String hunter) {
    public CelestialBody celestial() {
      return SolarSystem.BODIES.stream()
          .filter(value -> value.id().equals(body))
          .findFirst()
          .orElseThrow();
    }

    public String resource() {
      return celestial().resource().isEmpty() ? "titanium_ore" : celestial().resource();
    }
  }

  public static final List<Profile> ALL = load();
  public static final Codec<String> BODY_CODEC =
      Codec.STRING.validate(
          id ->
              ALL.stream().anyMatch(profile -> profile.body().equals(id))
                  ? DataResult.success(id)
                  : DataResult.error(() -> "Cuerpo celeste desconocido: " + id));

  private static List<Profile> load() {
    try (var input = LivingProfiles.class.getResourceAsStream("/living_worlds.json")) {
      if (input == null) throw new IllegalStateException("Falta living_worlds.json");
      var profiles =
          List.of(
              new Gson()
                  .fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Profile[].class));
      var ids = new HashSet<String>();
      for (var profile : profiles)
        if (!ids.add(profile.body()) || profile.flora().size() != 3)
          throw new IllegalStateException("Perfil biológico duplicado/incompleto");
      if (!ids.equals(new HashSet<>(SolarSystem.BODIES.stream().map(CelestialBody::id).toList())))
        throw new IllegalStateException("Faltan perfiles planetarios");
      return profiles;
    } catch (java.io.IOException error) {
      throw new IllegalStateException(error);
    }
  }

  public static Profile get(String id) {
    return ALL.stream()
        .filter(profile -> profile.body().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(id));
  }

  public static Profile of(net.minecraft.world.level.Level level) {
    var body = SolarSystem.body(level);
    return body == null ? null : get(body.id());
  }

  private LivingProfiles() {}
}
