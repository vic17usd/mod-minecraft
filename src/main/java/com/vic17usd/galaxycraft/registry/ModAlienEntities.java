package com.vic17usd.galaxycraft.registry;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.entity.PlanetaryCrawler;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Cuatro variantes registradas con atributos y reglas de aparición propios. */
public final class ModAlienEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GalaxyCraft.MOD_ID);
    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<Silverfish>>> CRAWLERS = new LinkedHashMap<>();
    static {
        for (var body : SolarSystem.BODIES) if (body.mob()) {
            String id = body.id() + "_crawler";
            CRAWLERS.put(body.id(), ENTITIES.register(id, () -> EntityType.Builder.<Silverfish>of(
                    PlanetaryCrawler::new, MobCategory.MONSTER).sized(.65F, .4F).fireImmune().build(id)));
        }
    }
    public static void attributes(EntityAttributeCreationEvent event) {
        CRAWLERS.forEach((name, type) -> event.put(type.get(), Silverfish.createAttributes()
                .add(Attributes.MAX_HEALTH, name.equals("venus") ? 24 : 12)
                .add(Attributes.ATTACK_DAMAGE, name.equals("mercury") ? 4 : 2).build()));
    }
    public static void placements(RegisterSpawnPlacementsEvent event) {
        CRAWLERS.values().forEach(type -> event.register(type.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }
    private ModAlienEntities() {}
}
