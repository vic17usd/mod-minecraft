package com.vic17usd.galaxycraft.weather;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.config.SpaceConfig;
import com.vic17usd.galaxycraft.expansion.ExpansionNetworks;
import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import com.vic17usd.galaxycraft.station.OrbitStations;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Clima local, periódico y con presupuesto; nunca genera chunks ni utiliza explosiones
 * destructivas.
 */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID)
public final class SpaceWeather {
  public static final int NONE = 0, DUST = 1, METEORS = 2, SOLAR = 3;

  public static boolean eligible(Level level) {
    var body = SolarSystem.body(level);
    return OrbitStations.isOrbit(level) || body != null && !body.id().equals("earth");
  }

  public static int active(Level level) {
    if (!(level instanceof ServerLevel server)
        || !eligible(level)
        || !SpaceConfig.SPACE_EVENTS.get()) return 0;
    var state = SpaceWeatherState.get(server);
    return state.ends > level.getGameTime() ? state.type : 0;
  }

  public static int radiation(Level level) {
    return active(level) == SOLAR ? 8 : 0;
  }

  public static int solarOutput(Level level) {
    return switch (active(level)) {
      case DUST -> 8;
      case SOLAR -> 0;
      default -> 40;
    };
  }

  public static boolean start(ServerLevel level, int type, int duration) {
    if (!eligible(level)
        || type < 0
        || type > 3
        || type == DUST
            && (SolarSystem.body(level) == null || !SolarSystem.body(level).id().equals("mars"))
        || type == METEORS && OrbitStations.isOrbit(level)) return false;
    var state = SpaceWeatherState.get(level);
    state.type = type;
    state.ends = level.getGameTime() + Math.clamp(duration, 20, 12000);
    state.next = state.ends + SpaceConfig.WEATHER_INTERVAL.get();
    state.setDirty();
    return true;
  }

  @SubscribeEvent
  public static void tick(LevelTickEvent.Post event) {
    if (!(event.getLevel() instanceof ServerLevel level)
        || level.getGameTime() % 20 != 0
        || !eligible(level)
        || !SpaceConfig.SPACE_EVENTS.get()
        || level.players().isEmpty()) return;
    var state = SpaceWeatherState.get(level);
    long now = level.getGameTime();
    if (state.next == 0) {
      state.next = now + SpaceConfig.WEATHER_INTERVAL.get();
      state.setDirty();
    }
    if (state.ends <= now && state.type != 0) {
      state.type = 0;
      state.setDirty();
    }
    if (state.type == 0 && now >= state.next) {
      int type =
          OrbitStations.isOrbit(level)
              ? SOLAR
              : SolarSystem.body(level).id().equals("mars")
                  ? new int[] {DUST, METEORS, SOLAR}[state.cycle % 3]
                  : state.cycle % 2 == 0 ? METEORS : SOLAR;
      state.cycle++;
      start(level, type, type == DUST ? 2400 : type == METEORS ? 1200 : 1800);
    }
    int active = active(level);
    int spawned = 0;
    for (var player : level.players()) {
      if (!player.isAlive() || player.isSpectator()) continue;
      if (active == DUST
          && ExpansionNetworks.room(player) == null
          && level.canSeeSky(player.blockPosition())) {
        if (!player.isCreative())
          player.addEffect(
              new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false));
        level.sendParticles(
            ModPresentation.DUST.get(),
            player.getX(),
            player.getY() + 1,
            player.getZ(),
            18,
            5,
            2,
            5,
            .18);
      }
      if (active == METEORS && now % 100 == 0 && spawned < 2) {
        var pos =
            player
                .blockPosition()
                .offset(
                    level.random.nextBoolean()
                        ? 12 + level.random.nextInt(16)
                        : -12 - level.random.nextInt(16),
                    0,
                    level.random.nextInt(33) - 16);
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
        int y =
            level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                pos.getX(),
                pos.getZ());
        var ground = new BlockPos(pos.getX(), y - 1, pos.getZ());
        if (!MeteorImpact.allowed(level, ground)) continue;
        var meteor = ModFrontiers.METEOR.get().create(level);
        if (meteor == null) continue;
        meteor.setPos(
            pos.getX() + .5, Math.min(y + 32, level.getMaxBuildHeight() - 2), pos.getZ() + .5);
        meteor.setDeltaMovement(0, -.6, 0);
        if (level.addFreshEntity(meteor)) spawned++;
      }
    }
  }

  @SubscribeEvent
  public static void commands(RegisterCommandsEvent event) {
    var root = Commands.literal("galaxyweather").requires(source -> source.hasPermission(2));
    String[] names = {"clear", "dust", "meteors", "solar"};
    for (int i = 0; i < names.length; i++) {
      final int type = i;
      root.then(
          Commands.literal(names[i])
              .executes(
                  context -> {
                    boolean started =
                        start(
                            context.getSource().getLevel(),
                            type,
                            type == DUST ? 2400 : type == METEORS ? 1200 : 1800);
                    context
                        .getSource()
                        .sendSuccess(
                            () ->
                                Component.translatable(
                                    started
                                        ? "weather.galaxycraft.changed"
                                        : "weather.galaxycraft.invalid"),
                            false);
                    return started ? 1 : 0;
                  }));
    }
    event.getDispatcher().register(root);
  }

  private SpaceWeather() {}
}
