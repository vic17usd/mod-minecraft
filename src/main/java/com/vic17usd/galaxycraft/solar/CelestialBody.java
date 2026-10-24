package com.vic17usd.galaxycraft.solar;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

/** Perfil compartido por registros, física, oxígeno, generación y navegación. */
public record CelestialBody(String id, String name, String parent, double gravity, String atmosphere,
        int tier, int heat, int radiation, String resource, boolean mob) {
    public ResourceKey<Level> dimension() {
        return id.equals("earth") ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("galaxycraft", id));
    }
    public boolean breathable() { return atmosphere.equals("breathable"); }
}
