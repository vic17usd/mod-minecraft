package com.vic17usd.galaxycraft.quest;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Contratos declarativos incluidos con el mod; ningún paquete cliente define recompensas. */
public final class QuestCatalog {
  public record Reward(String item, int count) {}

  public record Quest(
      String id,
      int phase,
      String prerequisite,
      String type,
      String target,
      int count,
      List<Reward> rewards,
      int xp) {}

  public static final List<Quest> ALL = load();

  private static List<Quest> load() {
    try (var stream = QuestCatalog.class.getResourceAsStream("/quests.json")) {
      if (stream == null) throw new IllegalStateException("Falta quests.json");
      return List.copyOf(
          Arrays.asList(
              new Gson()
                  .fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Quest[].class)));
    } catch (java.io.IOException error) {
      throw new IllegalStateException(error);
    }
  }

  public static Optional<Quest> find(String id) {
    return ALL.stream().filter(quest -> quest.id().equals(id)).findFirst();
  }

  private QuestCatalog() {}
}
