package com.vic17usd.galaxycraft.expedition;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.biosphere.LivingProfiles;
import java.util.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Asociación por instancia, segura entre hilos y servidores; no confunde bioma con dimensión. */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID)
public final class ExpeditionDimensions {
  private static final Map<Object, ResourceKey<Level>> WORLDS =
      Collections.synchronizedMap(new WeakHashMap<>());

  public static boolean matches(String body, ResourceKey<Level> dimension) {
    return LivingProfiles.get(body).celestial().dimension().equals(dimension);
  }

  public static boolean allows(Object generationState, String body) {
    return matches(body, WORLDS.get(generationState));
  }

  public static void bind(ServerLevel level) {
    WORLDS.put(level.getChunkSource().getGeneratorState(), level.dimension());
    WORLDS.put(level.getChunkSource().randomState(), level.dimension());
  }

  @SubscribeEvent
  public static void load(LevelEvent.Load event) {
    if (event.getLevel() instanceof ServerLevel level) bind(level);
  }

  @SubscribeEvent
  public static void unload(LevelEvent.Unload event) {
    if (event.getLevel() instanceof ServerLevel level) {
      WORLDS.remove(level.getChunkSource().getGeneratorState());
      WORLDS.remove(level.getChunkSource().randomState());
    }
  }

  private ExpeditionDimensions() {}
}
