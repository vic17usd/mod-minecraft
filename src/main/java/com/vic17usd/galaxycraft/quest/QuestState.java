package com.vic17usd.galaxycraft.quest;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Progreso personal persistente; una recompensa se marca antes de crear sus objetos o experiencia.
 */
public final class QuestState implements INBTSerializable<CompoundTag> {
  public final Set<String> completed = new HashSet<>(),
      visited = new HashSet<>(),
      built = new HashSet<>();
  public int drivenMillimeters;
  public final Map<String, View> views = new HashMap<>();

  public record View(int current, int goal, int status) {}

  @Override
  public CompoundTag serializeNBT(HolderLookup.Provider lookup) {
    var tag = new CompoundTag();
    tag.put("Completed", strings(completed));
    tag.put("Visited", strings(visited));
    tag.put("Built", strings(built));
    tag.putInt("Driven", drivenMillimeters);
    return tag;
  }

  private static ListTag strings(Set<String> values) {
    var list = new ListTag();
    values.stream().sorted().forEach(value -> list.add(StringTag.valueOf(value)));
    return list;
  }

  private static void read(Set<String> target, ListTag list) {
    target.clear();
    for (var value : list) target.add(value.getAsString());
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider lookup, CompoundTag tag) {
    read(completed, tag.getList("Completed", 8));
    read(visited, tag.getList("Visited", 8));
    read(built, tag.getList("Built", 8));
    drivenMillimeters = Math.clamp(tag.getInt("Driven"), 0, 1000000);
  }

  public boolean completeOnce(String id) {
    return completed.add(id);
  }
}
