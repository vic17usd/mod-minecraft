package com.vic17usd.galaxycraft.expedition;

import com.google.gson.Gson;
import com.vic17usd.galaxycraft.biosphere.LivingProfiles;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Catálogo cerrado: diez diseños por cuerpo, sin referencias a plantillas de otros mundos. */
public final class ExpeditionCatalog {
  public record Module(
      String template,
      int x,
      int y,
      int z,
      int width,
      int height,
      int depth,
      String role,
      int ports,
      boolean foundation) {}

  public record Site(
      String id,
      String body,
      String name,
      String role,
      String theme,
      boolean underground,
      boolean mega,
      int width,
      int height,
      int depth,
      int burial,
      int entranceX,
      int entranceZ,
      String geometryHash,
      List<Module> modules) {}

  public static final int SPACING = 384, SEPARATION = 192, MAX_RADIUS = 112;
  public static final List<Site> ALL = load();
  private static final Map<String, Site> BY_ID = new LinkedHashMap<>();

  static {
    for (var site : ALL)
      if (BY_ID.put(site.id(), site) != null)
        throw new IllegalStateException("Expedición duplicada: " + site.id());
  }

  private static List<Site> load() {
    try (var input = ExpeditionCatalog.class.getResourceAsStream("/expeditions_v2.json")) {
      if (input == null)
        throw new IllegalStateException(
            "Falta expeditions_v2.json; ejecutar el generador de recursos");
      var sites =
          List.of(
              new Gson()
                  .fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Site[].class));
      var hashes = new HashSet<String>();
      var templates = new HashSet<String>();
      for (var site : sites) {
        LivingProfiles.get(site.body());
        if (!site.id().startsWith(site.body() + "_")
            || !hashes.add(site.geometryHash())
            || site.modules().isEmpty()
            || site.modules().size() > 96)
          throw new IllegalStateException("Diseño inválido/repetido: " + site.id());
        for (var module : site.modules())
          if (!module.template().startsWith("expedition_v2/" + site.id() + "/")
              || !templates.add(module.template())
              || module.width() > 64
              || module.depth() > 64
              || module.height() > 64)
            throw new IllegalStateException("Plantilla compartida o demasiado grande");
      }
      for (var body : LivingProfiles.ALL) {
        var selected = sites.stream().filter(s -> s.body().equals(body.body())).toList();
        if (selected.size() != 10
            || selected.stream().filter(Site::underground).count() != 5
            || selected.stream().filter(Site::mega).count() != 1
            || selected.stream().filter(s -> s.role().equals("city")).count() != 1)
          throw new IllegalStateException(
              "Deben existir 5+5 expediciones y una arena: " + body.body());
      }
      return sites;
    } catch (java.io.IOException error) {
      throw new IllegalStateException(error);
    }
  }

  public static Site get(String id) {
    var site = BY_ID.get(id);
    if (site == null) throw new IllegalArgumentException("Expedición desconocida: " + id);
    return site;
  }

  private ExpeditionCatalog() {}
}
