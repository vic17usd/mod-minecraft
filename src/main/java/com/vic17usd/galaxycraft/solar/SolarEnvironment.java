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
        double factor = body == null ? 1 : Math.max(.05, body.gravity());
        modify(entity.getAttribute(Attributes.GRAVITY), factor);
        modify(entity.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), factor);
    }
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity living) gravity(living);
    }
    public static void onPlayerTick(PlayerTickEvent.Pre event) { gravity(event.getEntity()); }
    public static void onHazardTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide() || !player.isAlive() || player.isCreative() || player.isSpectator()
                || player.tickCount % 50 != 0) return;
        var body = SolarSystem.body(player.level());
        if (body == null) return;
        // El casco de oxígeno no protege del calor ni de la radiación; son peligros independientes.
        if (body.heat() > 0 && player.tickCount % 100 == 0) player.hurt(player.damageSources().hotFloor(), body.heat());
        if (body.radiation() > 0 && player.tickCount % 100 == 50) player.hurt(player.damageSources().magic(), body.radiation());
    }
    private SolarEnvironment() {}
}
