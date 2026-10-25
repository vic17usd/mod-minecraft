package com.vic17usd.galaxycraft.civilization;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Contratos personales: el servidor conserva aceptación y recompensa única por planeta. */
public final class ColonyContracts implements INBTSerializable<CompoundTag> {
  public final Set<String> accepted = new HashSet<>(), completed = new HashSet<>();

  @Override
  public CompoundTag serializeNBT(HolderLookup.Provider lookup) {
    var tag = new CompoundTag();
    tag.put("Accepted", strings(accepted));
    tag.put("Completed", strings(completed));
    return tag;
  }

  private static ListTag strings(Set<String> values) {
    var list = new ListTag();
    values.stream().sorted().forEach(value -> list.add(StringTag.valueOf(value)));
    return list;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider lookup, CompoundTag tag) {
    accepted.clear();
    completed.clear();
    for (var value : tag.getList("Accepted", 8)) accepted.add(value.getAsString());
    for (var value : tag.getList("Completed", 8)) completed.add(value.getAsString());
  }
}
