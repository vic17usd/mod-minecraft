package com.vic17usd.galaxycraft.civilization;

import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * C2S mínimo: sesión y acción. Stock/progreso viajan solo S2C mediante propiedades del menú
 * vanilla.
 */
public final class ColonyNetworking {
  public record Action(int menuId, int action) implements CustomPacketPayload {
    public static final Type<Action> TYPE = new Type<>(ModPresentation.id("colony_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Action::menuId,
            ByteBufCodecs.VAR_INT,
            Action::action,
            Action::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent event) {
    event
        .registrar("1")
        .playToServer(
            Action.TYPE,
            Action.CODEC,
            (packet, context) ->
                context.enqueueWork(
                    () -> {
                      if (context.player() instanceof ServerPlayer player
                          && player.containerMenu instanceof ColonyMenu menu
                          && menu.containerId == packet.menuId()
                          && packet.action() >= 0
                          && packet.action() < 6) menu.act(player, packet.action());
                    }));
  }

  private ColonyNetworking() {}
}
