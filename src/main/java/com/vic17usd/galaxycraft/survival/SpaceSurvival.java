package com.vic17usd.galaxycraft.survival;

import com.vic17usd.galaxycraft.config.SpaceConfig;
import com.vic17usd.galaxycraft.item.OxygenTankItem;
import com.vic17usd.galaxycraft.registry.ModItems;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.sounds.SoundSource;

/** Toda la autoridad sobre respiración y daño permanece en el servidor. */
public final class SpaceSurvival {
    public static boolean isAirless(Level level) {
        var body = SolarSystem.body(level);
        return (body != null && !body.breathable()) || SpaceConfig.SIMULATE_VACUUM.get()
                || SpaceConfig.AIRLESS_DIMENSIONS.get().contains(level.dimension().location().toString());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !player.isAlive() || player.isCreative()
                || player.isSpectator() || player.tickCount % 20 != 0 || !isAirless(player.level())) return;

        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SPACE_HELMET.get())) {
            // Usa un solo tanque por segundo. No inspecciona contenedores anidados.
            for (ItemStack stack : player.getInventory().items) {
                if (OxygenTankItem.consumeOxygen(stack)) return;
            }
            if (OxygenTankItem.consumeOxygen(player.getOffhandItem())) return;
        }

        // La falta de casco también impide respirar aunque haya tanques llenos.
        player.hurt(player.damageSources().drown(), 2.0F);
        // Una alarma como máximo cada cinco segundos por jugador sin suministro.
        if (player.tickCount % 100 == 0) player.level().playSound(null, player.blockPosition(),
                ModPresentation.DEPRESSURIZE.get(), SoundSource.PLAYERS, .65F, 1F);
        player.displayClientMessage(Component.translatable("message.galaxycraft.no_oxygen"), true);
    }

    private SpaceSurvival() {}
}
