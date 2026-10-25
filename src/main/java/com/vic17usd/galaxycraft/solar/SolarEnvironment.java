package com.vic17usd.galaxycraft.solar;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Modificadores transitorios: no sobrescriben los atributos base ni los de otros mods. */
public final class SolarEnvironment {
    private static final ResourceLocation GRAVITY = ResourceLocation.fromNamespaceAndPath("galaxycraft", "celestial_gravity");
    private static void modify(AttributeInstance attribute, double factor) {
        if (attribute == null) return;
        var previous = attribute.getModifier(GRAVITY);
        if (previous != null && previous.amount() == factor - 1) return;
        attribute.removeModifier(GRAVITY);
        if (factor != 1) attribute.addTransientModifier(new AttributeModifier(GRAVITY, factor - 1,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    private static void gravity(LivingEntity entity) {
        var body = SolarSystem.body(entity.level());
        // Un mínimo jugable evita caídas de varios minutos en lunas diminutas.
        double factor = com.vic17usd.galaxycraft.station.OrbitStations.isOrbit(entity.level())
                ? com.vic17usd.galaxycraft.expansion.ExpansionNetworks.gravity(entity.level(),entity.blockPosition())?1:0
                : body == null ? 1 : Math.max(.05, body.gravity());
        modify(entity.getAttribute(Attributes.GRAVITY), factor);
        modify(entity.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), factor);
    }
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if(!(event.getEntity() instanceof net.minecraft.world.entity.player.Player))com.vic17usd.galaxycraft.expansion.AdvancedSurvival.physics(event.getEntity());
        if (event.getEntity() instanceof LivingEntity living) gravity(living);
    }
    public static void onPlayerTick(PlayerTickEvent.Pre event) { gravity(event.getEntity());com.vic17usd.galaxycraft.expansion.AdvancedSurvival.physics(event.getEntity()); }
    private SolarEnvironment() {}
}
