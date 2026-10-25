package com.vic17usd.galaxycraft.frontier;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.combat.*;
import com.vic17usd.galaxycraft.logistics.*;
import com.vic17usd.galaxycraft.registry.*;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.registries.*;

/** Registros comunes de combate y logística; no importa clases de renderizado del cliente. */
public final class ModFrontiers {
  public static final Map<String, DeferredItem<? extends Item>> ITEMS = new LinkedHashMap<>();
  public static final Map<String, DeferredBlock<? extends Block>> BLOCKS = new LinkedHashMap<>();
  public static final DeferredItem<Item> LUNAR_CORE = item("lunar_guardian_core");
  public static final DeferredItem<Item> QUEEN_HEART = item("martian_queen_heart");
  public static final DeferredItem<Item> METEOR_FRAGMENT = item("meteor_fragment");
  public static final DeferredItem<Item> PLASMA = item("plasma_shard");
  public static final DeferredItem<Item> THRUSTER = item("stabilized_thruster");
  public static final DeferredItem<Item> NAV_CORE = item("advanced_navigation_core");

  private static DeferredItem<Item> item(String id) {
    var item = ModItems.ITEMS.registerSimpleItem(id);
    ITEMS.put(id, item);
    return item;
  }

  public static final DeferredBlock<Block> METEORITE =
      block(
          "meteorite",
          () ->
              new Block(BlockBehaviour.Properties.of().strength(5).requiresCorrectToolForDrops()));
  public static final DeferredBlock<BossAnchorBlock> LUNAR_ANCHOR =
      block(
          "lunar_boss_anchor",
          () -> new BossAnchorBlock(BlockBehaviour.Properties.of().strength(-1, 3600000), false));
  public static final DeferredBlock<BossAnchorBlock> MARTIAN_ANCHOR =
      block(
          "martian_boss_anchor",
          () -> new BossAnchorBlock(BlockBehaviour.Properties.of().strength(-1, 3600000), true));
  public static final DeferredBlock<LogisticsBlock> CABLE = machine("energy_cable", 0),
      OXYGEN_PIPE = machine("oxygen_conduit", 1),
      FUEL_PIPE = machine("fuel_pipe", 2),
      FUEL_TANK = machine("bulk_fuel_tank", 3),
      BATTERY = machine("energy_bank", 4),
      REFINERY = machine("fuel_refinery", 5),
      OXYGEN_TANK = machine("oxygen_reservoir", 6);

  private static DeferredBlock<LogisticsBlock> machine(String id, int kind) {
    return block(
        id,
        () ->
            new LogisticsBlock(
                BlockBehaviour.Properties.of().strength(3).sound(SoundType.METAL), kind));
  }

  private static <T extends Block> DeferredBlock<T> block(
      String id, java.util.function.Supplier<T> factory) {
    var block = ModBlocks.BLOCKS.register(id, factory);
    BLOCKS.put(id, block);
    return block;
  }

  public static void registerItems() {
    BLOCKS.forEach((id, block) -> ITEMS.put(id, ModItems.ITEMS.registerSimpleBlockItem(id, block)));
  }

  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsMachine>>
      MACHINE =
          BLOCK_ENTITIES.register(
              "logistics_machine",
              () ->
                  BlockEntityType.Builder.of(
                          LogisticsMachine::new,
                          CABLE.get(),
                          OXYGEN_PIPE.get(),
                          FUEL_PIPE.get(),
                          FUEL_TANK.get(),
                          BATTERY.get(),
                          REFINERY.get(),
                          OXYGEN_TANK.get())
                      .build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BossAnchor>> ANCHOR =
      BLOCK_ENTITIES.register(
          "boss_anchor",
          () ->
              BlockEntityType.Builder.of(BossAnchor::new, LUNAR_ANCHOR.get(), MARTIAN_ANCHOR.get())
                  .build(null));
  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<MenuType<?>, MenuType<LogisticsMenu>> MENU =
      MENUS.register("logistics", () -> IMenuTypeExtension.create(LogisticsMenu::new));
  public static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.ENTITY_TYPE, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<EntityType<?>, EntityType<PlanetBoss>>
      GUARDIAN = boss("lunar_guardian", 1.6F, 2.8F),
      QUEEN = boss("martian_queen", 2.0F, 2.8F);

  private static DeferredHolder<EntityType<?>, EntityType<PlanetBoss>> boss(
      String id, float width, float height) {
    return ENTITIES.register(
        id,
        () ->
            EntityType.Builder.<PlanetBoss>of(PlanetBoss::new, MobCategory.MONSTER)
                .sized(width, height)
                .fireImmune()
                .clientTrackingRange(12)
                .build(id));
  }

  public static final Map<String, DeferredHolder<EntityType<?>, EntityType<PlanetaryRaider>>>
      RAIDERS = new LinkedHashMap<>();

  static {
    for (var body : SolarSystem.BODIES)
      if (!body.id().equals("earth")) {
        String id = body.id() + "_raider";
        RAIDERS.put(
            body.id(),
            ENTITIES.register(
                id,
                () ->
                    EntityType.Builder.<PlanetaryRaider>of(
                            PlanetaryRaider::new, MobCategory.MONSTER)
                        .sized(.6F, 1.95F)
                        .fireImmune()
                        .build(id)));
      }
  }

  public static final DeferredHolder<EntityType<?>, EntityType<SpaceProjectile>>
      PLASMA_BOLT = projectile("plasma_bolt"),
      METEOR = projectile("falling_meteor");

  private static DeferredHolder<EntityType<?>, EntityType<SpaceProjectile>> projectile(String id) {
    return ENTITIES.register(
        id,
        () ->
            EntityType.Builder.<SpaceProjectile>of(SpaceProjectile::new, MobCategory.MISC)
                .sized(.45F, .45F)
                .clientTrackingRange(12)
                .updateInterval(1)
                .build(id));
  }

  public static void attributes(EntityAttributeCreationEvent event) {
    event.put(GUARDIAN.get(), PlanetBoss.attributes(false).build());
    event.put(QUEEN.get(), PlanetBoss.attributes(true).build());
    RAIDERS.forEach(
        (name, type) -> {
          var body =
              SolarSystem.BODIES.stream()
                  .filter(value -> value.id().equals(name))
                  .findFirst()
                  .orElseThrow();
          event.put(
              type.get(),
              PlanetaryRaider.createAttributes()
                  .add(Attributes.MAX_HEALTH, 22 + body.tier() * 4)
                  .add(Attributes.ATTACK_DAMAGE, 3 + body.tier())
                  .add(Attributes.MOVEMENT_SPEED, .26)
                  .build());
        });
  }

  public static void placements(RegisterSpawnPlacementsEvent event) {
    RAIDERS
        .values()
        .forEach(
            type ->
                event.register(
                    type.get(),
                    SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Monster::checkAnyLightMonsterSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE));
  }

  private ModFrontiers() {}
}
