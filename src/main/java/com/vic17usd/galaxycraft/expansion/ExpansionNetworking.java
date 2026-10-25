package com.vic17usd.galaxycraft.expansion;

import com.vic17usd.galaxycraft.quest.*;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import com.vic17usd.galaxycraft.rover.RoverEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Paquetes mínimos y unidireccionales; ningún cliente proporciona recompensas, carga o datos de
 * progreso.
 */
public final class ExpansionNetworking {
  public record OpenQuests() implements CustomPacketPayload {
    public static final Type<OpenQuests> TYPE = new Type<>(ModPresentation.id("quests_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenQuests> CODEC =
        StreamCodec.unit(new OpenQuests());

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record Submit(int menuId, String quest) implements CustomPacketPayload {
    public static final Type<Submit> TYPE = new Type<>(ModPresentation.id("quest_submit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Submit> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Submit::menuId,
            ByteBufCodecs.stringUtf8(80),
            Submit::quest,
            Submit::new);

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record QuestSnapshot(CompoundTag value) implements CustomPacketPayload {
    public static final Type<QuestSnapshot> TYPE =
        new Type<>(ModPresentation.id("quests_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestSnapshot> CODEC =
        StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, QuestSnapshot::value, QuestSnapshot::new);

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record EnvironmentSnapshot(CompoundTag value) implements CustomPacketPayload {
    public static final Type<EnvironmentSnapshot> TYPE =
        new Type<>(ModPresentation.id("environment_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnvironmentSnapshot> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, EnvironmentSnapshot::value, EnvironmentSnapshot::new);

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record Controls(int entityId, float forward, float steer, int thrust)
      implements CustomPacketPayload {
    public static final Type<Controls> TYPE = new Type<>(ModPresentation.id("vehicle_controls"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Controls> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Controls::entityId,
            ByteBufCodecs.FLOAT,
            Controls::forward,
            ByteBufCodecs.FLOAT,
            Controls::steer,
            ByteBufCodecs.VAR_INT,
            Controls::thrust,
            Controls::new);

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent event) {
    var packets = event.registrar("1");
    packets.playToServer(
        OpenQuests.TYPE,
        OpenQuests.CODEC,
        (packet, context) ->
            context.enqueueWork(
                () -> {
                  if (context.player() instanceof ServerPlayer player) QuestService.open(player);
                }));
    packets.playToServer(
        Submit.TYPE,
        Submit.CODEC,
        (packet, context) ->
            context.enqueueWork(
                () -> {
                  if (context.player() instanceof ServerPlayer player
                      && player.containerMenu instanceof QuestMenu menu
                      && menu.containerId == packet.menuId()
                      && menu.stillValid(player))
                    QuestCatalog.find(packet.quest())
                        .ifPresent(quest -> QuestService.finish(player, quest, true));
                }));
    packets.playToClient(
        QuestSnapshot.TYPE,
        QuestSnapshot.CODEC,
        (packet, context) ->
            context.enqueueWork(
                () -> {
                  var state = context.player().getData(ModExpansion.QUESTS.get());
                  state.deserializeNBT(context.player().registryAccess(), packet.value());
                  state.views.clear();
                  for (var entry : packet.value().getList("Views", 10)) {
                    var value = (CompoundTag) entry;
                    state.views.put(
                        value.getString("id"),
                        new QuestState.View(
                            value.getInt("current"), value.getInt("goal"), value.getInt("status")));
                  }
                }));
    packets.playToClient(
        EnvironmentSnapshot.TYPE,
        EnvironmentSnapshot.CODEC,
        (packet, context) ->
            context.enqueueWork(
                () -> {
                  var value = context.player().getData(ModExpansion.EXPOSURE.get());
                  value.temperature = packet.value().getInt("Temperature");
                  value.thermal = packet.value().getInt("Thermal");
                  value.radiation = packet.value().getInt("Radiation");
                  value.pressure = packet.value().getInt("Pressure");
                  value.sheltered = packet.value().getBoolean("Sheltered");
                  value.roomOxygen = packet.value().getInt("RoomOxygen");
                  value.roomCapacity = packet.value().getInt("RoomCapacity");
                  value.weather=packet.value().getInt("Weather");value.weatherSeconds=packet.value().getInt("WeatherSeconds");
                }));
    packets.playToServer(
        Controls.TYPE,
        Controls.CODEC,
        (packet, context) ->
            context.enqueueWork(
                () -> {
                  if (!(context.player() instanceof ServerPlayer player)
                      || !player.isAlive()
                      || player.isSpectator()) return;
                  if (player.getVehicle() instanceof RoverEntity rover
                      && rover.getId() == packet.entityId())
                    rover.input(player, packet.forward(), packet.steer());
                  if (AdvancedSurvival.zeroG(player)
                      && packet.thrust() >= 0
                      && packet.thrust() < 64) {
                    var value = player.getData(ModExpansion.EXPOSURE.get());
                    value.input = packet.thrust();
                    value.inputAt = player.level().getGameTime();
                  }
                }));
  }

  private ExpansionNetworking() {}
}
