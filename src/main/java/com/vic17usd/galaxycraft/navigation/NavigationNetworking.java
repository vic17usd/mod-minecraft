package com.vic17usd.galaxycraft.navigation;

import com.vic17usd.galaxycraft.menu.NavigationMenu;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Peticiones acotadas al menú vigente: no se aceptan coordenadas, combustible ni niveles del cliente. */
public final class NavigationNetworking {
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ModPresentation.id("navigation_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC = StreamCodec.unit(new Open());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Controls() implements CustomPacketPayload {
        public static final Type<Controls> TYPE = new Type<>(ModPresentation.id("navigation_controls"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Controls> CODEC = StreamCodec.unit(new Controls());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Select(int menuId,ResourceLocation destination) implements CustomPacketPayload {
        public static final Type<Select> TYPE = new Type<>(ModPresentation.id("navigation_select"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Select> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,Select::menuId,ResourceLocation.STREAM_CODEC,Select::destination,Select::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Launch(int menuId,ResourceLocation destination) implements CustomPacketPayload {
        public static final Type<Launch> TYPE = new Type<>(ModPresentation.id("navigation_launch"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Launch> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,Launch::menuId,ResourceLocation.STREAM_CODEC,Launch::destination,Launch::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Open.TYPE,Open.CODEC,(packet,context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) NavigationAccess.open(player);
        }));
        registrar.playToServer(Controls.TYPE,Controls.CODEC,(packet,context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) NavigationAccess.controls(player);
        }));
        registrar.playToServer(Select.TYPE,Select.CODEC,(packet,context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof NavigationMenu menu
                    && menu.containerId == packet.menuId()) menu.select(player,packet.destination());
        }));
        registrar.playToServer(Launch.TYPE,Launch.CODEC,(packet,context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof NavigationMenu menu
                    && menu.containerId == packet.menuId()) menu.launch(player,packet.destination());
        }));
    }
    private NavigationNetworking() {}
}
