package com.vic17usd.galaxycraft.biosphere;

import com.mojang.serialization.MapCodec;
import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.civilization.*;
import com.vic17usd.galaxycraft.ecology.*;
import com.vic17usd.galaxycraft.registry.*;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.registries.*;

/** Registros modulares de biosfera y civilización, sin dependencias del cliente. */
public final class ModBiosphere {
  public static final Map<String, DeferredBlock<Block>> PANELS = new LinkedHashMap<>();
  public static final Map<String, DeferredBlock<? extends Block>> VENTS = new LinkedHashMap<>();
  public static final Map<String, DeferredBlock<AlienPlantBlock>> PLANTS = new LinkedHashMap<>();
  public static final Map<String, DeferredItem<? extends Item>> ITEMS = new LinkedHashMap<>();
  public static final Map<String, DeferredItem<Item>> HARVEST = new LinkedHashMap<>();
  public static final DeferredItem<Item> TOKEN = item("colony_token", new Item.Properties());
  public static final DeferredItem<Item> MEAT =
      item(
          "alien_protein",
          new Item.Properties()
              .food(new FoodProperties.Builder().nutrition(4).saturationModifier(.4F).build()));
  public static final DeferredItem<Item> FIBER = item("luminous_fiber", new Item.Properties());

  private static DeferredItem<Item> item(String id, Item.Properties properties) {
    var item = ModItems.ITEMS.registerSimpleItem(id, properties);
    ITEMS.put(id, item);
    return item;
  }

  public static void content() {
    for (var profile : LivingProfiles.ALL) {
      String body = profile.body();
      HARVEST.put(
          body,
          item(
              body + "_biosample",
              new Item.Properties()
                  .food(
                      new FoodProperties.Builder().nutrition(3).saturationModifier(.25F).build())));
      for (String kind : ModSites.KINDS) {
        String name = body + "_" + kind + "_panel";
        var block =
            ModBlocks.BLOCKS.registerSimpleBlock(
                name,
                BlockBehaviour.Properties.of()
                    .strength(kind.equals("vault") ? 6 : 3, 20)
                    .requiresCorrectToolForDrops()
                    .sound(kind.equals("geode") ? SoundType.AMETHYST : SoundType.METAL));
        PANELS.put(name, block);
        ITEMS.put(name, ModItems.ITEMS.registerSimpleBlockItem(name, block));
      }
      for (String style : profile.flora()) {
        String name = body + "_" + style;
        var block =
            ModBlocks.BLOCKS.register(
                name,
                () ->
                    new AlienPlantBlock(
                        BlockBehaviour.Properties.of()
                            .noCollission()
                            .noOcclusion()
                            .instabreak()
                            .randomTicks()
                            .sound(SoundType.CROP)
                            .lightLevel(
                                state ->
                                    style.equals("fungus") || style.equals("crystal_vine")
                                        ? 3 + state.getValue(AlienPlantBlock.AGE) * 4
                                        : 0),
                        body,
                        style));
        PLANTS.put(name, block);
        ITEMS.put(name, ModItems.ITEMS.registerSimpleBlockItem(name, block));
      }
      String name = body + "_geyser";
      var vent =
          ModBlocks.BLOCKS.register(
              name,
              () ->
                  new GeyserBlock(BlockBehaviour.Properties.of().strength(3).randomTicks(), body));
      VENTS.put(body, vent);
      ITEMS.put(name, ModItems.ITEMS.registerSimpleBlockItem(name, vent));
    }
  }

  public static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.ENTITY_TYPE, GalaxyCraft.MOD_ID);
  public static final Map<String, DeferredHolder<EntityType<?>, EntityType<CosmicGrazer>>> FAUNA =
      new LinkedHashMap<>();
  public static final Map<String, DeferredHolder<EntityType<?>, EntityType<ColonyCitizen>>>
      CITIZENS = new LinkedHashMap<>();
  public static final Map<String, DeferredHolder<EntityType<?>, EntityType<HabitatHunter>>>
      HUNTERS = new LinkedHashMap<>();

  static {
    for (var profile : LivingProfiles.ALL) {
      String body = profile.body();
      for (String species : List.of("crystal_bovine", "lumen_sheep", "plated_boar")) {
        String id = body + "_" + species;
        FAUNA.put(
            id,
            ENTITIES.register(
                id,
                () ->
                    EntityType.Builder.<CosmicGrazer>of(CosmicGrazer::new, MobCategory.CREATURE)
                        .sized(.95F, species.equals("crystal_bovine") ? 1.4F : 1.0F)
                        .fireImmune()
                        .build(id)));
      }
      String citizen = body + "_colonist";
      CITIZENS.put(
          body,
          ENTITIES.register(
              citizen,
              () ->
                  EntityType.Builder.<ColonyCitizen>of(ColonyCitizen::new, MobCategory.CREATURE)
                      .sized(.6F, 1.95F)
                      .fireImmune()
                      .build(citizen)));
      String hunter = body + "_stalker";
      HUNTERS.put(
          body,
          ENTITIES.register(
              hunter,
              () ->
                  EntityType.Builder.<HabitatHunter>of(HabitatHunter::new, MobCategory.MONSTER)
                      .sized(1.1F, .95F)
                      .fireImmune()
                      .build(hunter)));
    }
  }

  public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
      DeferredRegister.create(Registries.CHUNK_GENERATOR, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<
          MapCodec<? extends ChunkGenerator>, MapCodec<ExtremePlanetGenerator>>
      EXTREME = GENERATORS.register("extreme_planet", () -> ExtremePlanetGenerator.CODEC);
  public static final DeferredRegister<MapCodec<? extends BiomeSource>> SOURCES =
      DeferredRegister.create(Registries.BIOME_SOURCE, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<PlanetBiomeSource>>
      BIOMES = SOURCES.register("planet_habitats", () -> PlanetBiomeSource.CODEC);
  public static final DeferredRegister<Feature<?>> FEATURES =
      DeferredRegister.create(Registries.FEATURE, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<Feature<?>, EcologyFeature> ECOLOGY =
      FEATURES.register("planet_ecology", EcologyFeature::new);
  public static final DeferredRegister<StructureType<?>> STRUCTURES =
      DeferredRegister.create(Registries.STRUCTURE_TYPE, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<StructureType<?>, StructureType<ColonyStructure>> COLONY =
      STRUCTURES.register("alien_colony", () -> () -> ColonyStructure.CODEC);
  public static final DeferredRegister<StructurePieceType> PIECES =
      DeferredRegister.create(Registries.STRUCTURE_PIECE, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<StructurePieceType, StructurePieceType> COLONY_PIECE =
      PIECES.register("colony_piece", () -> ColonyPiece::new);
  public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
      DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<AttachmentType<?>, AttachmentType<ColonyContracts>> CONTRACTS =
      ATTACHMENTS.register(
          "colony_contracts",
          () -> AttachmentType.serializable(ColonyContracts::new).copyOnDeath().build());
  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<MenuType<?>, MenuType<ColonyMenu>> COLONY_MENU =
      MENUS.register("colony", () -> IMenuTypeExtension.create(ColonyMenu::new));

  public static void attributes(EntityAttributeCreationEvent event) {
    FAUNA
        .values()
        .forEach(
            type ->
                event.put(
                    type.get(),
                    CosmicGrazer.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, 24)
                        .add(Attributes.MOVEMENT_SPEED, .24)
                        .build()));
    CITIZENS
        .values()
        .forEach(
            type ->
                event.put(
                    type.get(),
                    ColonyCitizen.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, 30)
                        .add(Attributes.MOVEMENT_SPEED, .24)
                        .build()));
    HUNTERS
        .values()
        .forEach(
            type ->
                event.put(
                    type.get(),
                    Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 32)
                        .add(Attributes.MOVEMENT_SPEED, .28)
                        .add(Attributes.ATTACK_DAMAGE, 5)
                        .add(Attributes.FOLLOW_RANGE, 24)
                        .build()));
  }

  public static void placements(RegisterSpawnPlacementsEvent event) {
    FAUNA
        .values()
        .forEach(
            type ->
                event.register(
                    type.get(),
                    SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    CosmicGrazer::canSpawn,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE));
    HUNTERS
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

  private ModBiosphere() {}
}
