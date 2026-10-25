package com.vic17usd.galaxycraft.station;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Anclas y puntos de regreso guardados globalmente en el Overworld; asignación sin colisiones de
 * parcelas.
 */
public final class StationData extends SavedData {
  public record Anchor(BlockPos position, BlockPos returnPosition) {}

  public final Map<String, Anchor> anchors = new HashMap<>();
  private int nextPlot;

  public static StationData get(MinecraftServer server) {
    return server
        .overworld()
        .getDataStorage()
        .computeIfAbsent(
            new SavedData.Factory<>(StationData::new, StationData::load), "galaxycraft_stations");
  }

  public String key(UUID owner, String body) {
    return owner + "/" + body;
  }

  public BlockPos allocate() {
    int cell = nextPlot++;
    setDirty();
    return new BlockPos((cell % 256) * 96, 96, (cell / 256) * 96);
  }

  public void put(UUID owner, String body, BlockPos pos, BlockPos back) {
    anchors.put(key(owner, body), new Anchor(pos, back));
    setDirty();
  }

  public Anchor find(UUID owner, String body) {
    return anchors.get(key(owner, body));
  }

  public static StationData load(CompoundTag tag, HolderLookup.Provider lookup) {
    var data = new StationData();
    data.nextPlot = Math.max(0, tag.getInt("Next"));
    for (var entry : tag.getList("Anchors", 10)) {
      var value = (CompoundTag) entry;
      data.anchors.put(
          value.getString("Key"),
          new Anchor(BlockPos.of(value.getLong("Position")), BlockPos.of(value.getLong("Return"))));
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
    tag.putInt("Next", nextPlot);
    var list = new ListTag();
    anchors.forEach(
        (key, anchor) -> {
          var value = new CompoundTag();
          value.putString("Key", key);
          value.putLong("Position", anchor.position().asLong());
          value.putLong("Return", anchor.returnPosition().asLong());
          list.add(value);
        });
    tag.put("Anchors", list);
    return tag;
  }
}
