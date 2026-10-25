package com.vic17usd.galaxycraft.morphology;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.astronomy.CelestialClock;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Protocolo exclusivamente S2C: las poses no conceden al cliente autoridad sobre la IA. */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID)
public final class MorphologyNetworking {
  public record Pose(int entity, int action, long start, int duration)
      implements CustomPacketPayload {
    public static final Type<Pose> TYPE = new Type<>(ModPresentation.id("alien_pose"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Pose> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Pose::entity,
            ByteBufCodecs.VAR_INT,
            Pose::action,
            ByteBufCodecs.VAR_LONG,
            Pose::start,
            ByteBufCodecs.VAR_INT,
            Pose::duration,
            Pose::new);

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record Clock(long tick, double days, double rate) implements CustomPacketPayload {
    public static final Type<Clock> TYPE = new Type<>(ModPresentation.id("celestial_clock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Clock> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            Clock::tick,
            ByteBufCodecs.DOUBLE,
            Clock::days,
            ByteBufCodecs.DOUBLE,
            Clock::rate,
            Clock::new);

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent event) {
    var registrar = event.registrar("1");
    registrar.playToClient(
        Pose.TYPE,
        Pose.CODEC,
        (packet, context) ->
            context.enqueueWork(
                () -> {
                  var entity = context.player().level().getEntity(packet.entity());
                  var pose = new AlienPose(packet.action(), packet.start(), packet.duration());
                  if (entity instanceof Mob mob && ours(mob) && pose.valid())
                    mob.setData(ModMorphology.POSE, pose);
                }));
    registrar.playToClient(
        Clock.TYPE,
        Clock.CODEC,
        (packet, context) ->
            context.enqueueWork(
                () -> {
                  var clock = new CelestialClock(packet.tick(), packet.days(), packet.rate());
                  if (clock.valid()) context.player().setData(ModMorphology.CLOCK, clock);
                }));
  }

  public static boolean ours(Mob mob) {
    return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
        .getKey(mob.getType())
        .getNamespace()
        .equals(GalaxyCraft.MOD_ID);
  }

  public static void animate(Mob mob, int action, int duration) {
    if (mob.level().isClientSide() || !ours(mob)) return;
    var pose = new AlienPose(action, mob.level().getGameTime(), duration);
    if (!pose.valid()) throw new IllegalArgumentException("Pose inválida");
    mob.setData(ModMorphology.POSE, pose);
    PacketDistributor.sendToPlayersTrackingEntity(
        mob, new Pose(mob.getId(), action, pose.start(), duration));
  }

  private static void clock(ServerPlayer player) {
    double rate = ModMorphology.ORBIT_RATE.get();
    PacketDistributor.sendToPlayer(
        player,
        new Clock(
            player.level().getGameTime(),
            player.server.overworld().getDayTime() / 24000.0 * rate,
            rate));
  }

  @SubscribeEvent
  public static void tick(PlayerTickEvent.Post event) {
    if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 100 == 0)
      clock(player);
  }

  @SubscribeEvent
  public static void login(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) clock(player);
  }

  @SubscribeEvent
  public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) clock(player);
  }

  @SubscribeEvent
  public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) clock(player);
  }

  @SubscribeEvent
  public static void tracking(PlayerEvent.StartTracking event) {
    if (event.getEntity() instanceof ServerPlayer player
        && event.getTarget() instanceof Mob mob
        && ours(mob)) {
      var pose = mob.getData(ModMorphology.POSE);
      if (pose.start() + pose.duration() > mob.level().getGameTime())
        PacketDistributor.sendToPlayer(
            player, new Pose(mob.getId(), pose.action(), pose.start(), pose.duration()));
    }
  }

  private MorphologyNetworking() {}
}
