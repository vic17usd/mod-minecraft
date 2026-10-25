package com.vic17usd.galaxycraft.weather;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Calendario por dimensión guardado en disco: reiniciar no reinicia la duración ni duplica
 * impactos.
 */
public final class SpaceWeatherState extends SavedData {
  public int type, cycle;
  public long ends, next;

  public static SpaceWeatherState get(ServerLevel level) {
    return level
        .getDataStorage()
        .computeIfAbsent(
            new SavedData.Factory<>(SpaceWeatherState::new, SpaceWeatherState::load),
            "galaxycraft_space_weather");
  }

  public static SpaceWeatherState load(CompoundTag tag, HolderLookup.Provider lookup) {
    var state = new SpaceWeatherState();
    state.type = Math.clamp(tag.getInt("Type"), 0, 3);
    state.cycle = Math.max(0, tag.getInt("Cycle"));
    state.ends = Math.max(0, tag.getLong("Ends"));
    state.next = Math.max(0, tag.getLong("Next"));
    return state;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
    tag.putInt("Type", type);
    tag.putInt("Cycle", cycle);
    tag.putLong("Ends", ends);
    tag.putLong("Next", next);
    return tag;
  }
}
