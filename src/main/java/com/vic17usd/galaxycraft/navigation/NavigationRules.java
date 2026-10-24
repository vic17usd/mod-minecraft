package com.vic17usd.galaxycraft.navigation;

import com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
import com.vic17usd.galaxycraft.equipment.SpaceEquipment;
import com.vic17usd.galaxycraft.rocket.RocketFueling;
import com.vic17usd.galaxycraft.solar.CelestialBody;
import com.vic17usd.galaxycraft.survival.SpaceSurvival;
import net.minecraft.server.level.ServerPlayer;

/** Reglas compartidas y motivos explicables; el servidor las recalcula justo antes del encendido. */
public final class NavigationRules {
    public enum BlockReason { READY, TIER, FUEL, PAD, CLEARANCE, SAME_WORLD, OXYGEN, UNAVAILABLE, IGNITING, PILOT }
    public static int fuelCost(CelestialBody body) { return RocketFueling.CAPACITY; }
    public static BlockReason evaluate(int tier, int requiredTier, int fuel, int cost, int readiness,
            boolean sameWorld, boolean oxygen, boolean available, boolean igniting, boolean pilot) {
        if (!pilot) return BlockReason.PILOT;
        if (igniting) return BlockReason.IGNITING;
        if (!available) return BlockReason.UNAVAILABLE;
        if (sameWorld) return BlockReason.SAME_WORLD;
        if (tier < requiredTier || tier < 1 || tier > 3) return BlockReason.TIER;
        if (readiness == 0) return BlockReason.PAD;
        if (readiness == 1) return BlockReason.CLEARANCE;
        if (fuel < cost) return BlockReason.FUEL;
        if (!oxygen) return BlockReason.OXYGEN;
        return BlockReason.READY;
    }
    public static BlockReason reason(ServerPlayer player, RocketBlockEntity rocket, CelestialBody body) {
        var system = CelestialCatalog.system(body);
        var target = player.server.getLevel(body.dimension());
        int tier = Math.max(body.tier(), system.map(CelestialCatalog.StellarSystem::minimumTier).orElse(4));
        boolean oxygen = player.isCreative() || target == null || !SpaceSurvival.isAirless(target)
                || SpaceEquipment.hasHelmet(player) && SpaceEquipment.hasOxygen(player);
        return evaluate(rocket.tier(), tier, rocket.fuel.getFluidAmount(), fuelCost(body), rocket.readiness(),
                player.level().dimension().equals(body.dimension()), oxygen,
                target != null && system.map(CelestialCatalog.StellarSystem::enabled).orElse(false), rocket.launchStarted() >= 0,
                player.isAlive() && !player.isSpectator());
    }
    private NavigationRules() {}
}
