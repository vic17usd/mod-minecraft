package com.vic17usd.galaxycraft.equipment;

import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Snapshot unidireccional: el cliente nunca puede reemplazar inventarios mediante este paquete. */
public final class EquipmentNetworking {
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ModPresentation.id("open_equipment"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.unit(new Open());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Snapshot(int entityId, CompoundTag inventory) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ModPresentation.id("equipment_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Snapshot::entityId, ByteBufCodecs.COMPOUND_TAG, Snapshot::inventory, Snapshot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(Open.TYPE, Open.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu == player.inventoryMenu)
                com.vic17usd.galaxycraft.item.EquipmentConsoleItem.open(player);
        }));
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (packet, context) -> context.enqueueWork(() -> {
            var entity = context.player().level().getEntity(packet.entityId());
            if (entity instanceof net.minecraft.world.entity.player.Player player)
                SpaceEquipment.inventory(player).deserializeNBT(player.registryAccess(), packet.inventory());
        }));
    }
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, snapshot(player));
    }
    public static Snapshot snapshot(ServerPlayer player) { return new Snapshot(player.getId(), SpaceEquipment.inventory(player).serializeNBT(player.registryAccess())); }
    private EquipmentNetworking() {}
}
