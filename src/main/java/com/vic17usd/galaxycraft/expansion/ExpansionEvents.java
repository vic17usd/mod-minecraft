package com.vic17usd.galaxycraft.expansion;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.quest.*;
import com.vic17usd.galaxycraft.station.OrbitStations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Evidencias de misión procedentes del servidor y limpieza de índices al descargar mundos. */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID)
public final class ExpansionEvents {
  @SubscribeEvent
  public static void login(
      net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      recordVisit(player);
      QuestService.sync(player);
    }
  }

  @SubscribeEvent
  public static void dimension(
      net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) recordVisit(player);
  }

  private static void recordVisit(ServerPlayer player) {
    var state = QuestService.state(player);
    state.visited.add(player.level().dimension().location().toString());
    if (OrbitStations.isOrbit(player.level())) state.visited.add("orbit");
  }

  @SubscribeEvent
  public static void tick(PlayerTickEvent.Post event) {
    if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
    AdvancedSurvival.tick(player);
    if (player.tickCount % 20 != 0) return;
    var state = QuestService.state(player);
    state.visited.add(player.level().dimension().location().toString());
    if (OrbitStations.isOrbit(player.level())) state.visited.add("orbit");
    var body = com.vic17usd.galaxycraft.solar.SolarSystem.body(player.level());
    if (body != null && body.tier() == 3) state.visited.add("outer");
    if (ExpansionNetworks.room(player) != null) state.built.add("sealed_room");
    if (!player.isSpectator())
      for (var quest : QuestCatalog.ALL)
        if (!quest.type().equals("deliver")) QuestService.finish(player, quest, false);
    if (player.containerMenu instanceof QuestMenu) QuestService.sync(player);
  }

  @SubscribeEvent
  public static void placed(BlockEvent.EntityPlaceEvent event) {
    if (event.getLevel() instanceof Level level) ExpansionNetworks.invalidate(level);
    if (event.getEntity() instanceof ServerPlayer player) {
      var placed = event.getPos();
      for (int x = -1; x <= 1; x++)
        for (int z = -1; z <= 1; z++)
          if (com.vic17usd.galaxycraft.block.LaunchPadBlock.isComplete(
              player.level(), placed.offset(x, 0, z)))
            QuestService.state(player).built.add("launch_pad");
    }
  }

  @SubscribeEvent
  public static void changed(BlockEvent.NeighborNotifyEvent event) {
    if (event.getLevel() instanceof Level level) ExpansionNetworks.invalidate(level);
  }

  @SubscribeEvent
  public static void broken(BlockEvent.BreakEvent event) {
    if (event.getLevel() instanceof Level level) ExpansionNetworks.invalidate(level);
  }

  @SubscribeEvent
  public static void unload(LevelEvent.Unload event) {
    if (event.getLevel() instanceof Level level) ExpansionNetworks.unload(level);
  }

  private ExpansionEvents() {}
}
