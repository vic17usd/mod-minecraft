package com.vic17usd.galaxycraft.testing;

import com.mojang.authlib.GameProfile;
import com.vic17usd.galaxycraft.biosphere.*;
import com.vic17usd.galaxycraft.civilization.*;
import com.vic17usd.galaxycraft.ecology.*;
import com.vic17usd.galaxycraft.registry.*;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

/**
 * Pruebas con registros y niveles reales, sin cliente gráfico ni inventarios enviados por el
 * cliente.
 */
@GameTestHolder("galaxycraft")
@PrefixGameTestTemplate(false)
public final class BiosphereGameTests {
  private static void done(GameTestHelper helper, String id) {
    helper.succeed();
    com.mojang.logging.LogUtils.getLogger().info("GALAXYCRAFT_INTEGRATION_OK {}", id);
  }

  @GameTest(template = "tests/empty", timeoutTicks = 200)
  public static void native_crop_rules(GameTestHelper helper) {
    var level = helper.getLevel();
    var profile = LivingProfiles.get("moon");
    var plant = ModBiosphere.PLANTS.get("moon_" + profile.flora().getFirst()).get();
    var state = plant.defaultBlockState();
    var wrong = helper.absolutePos(new BlockPos(6, 2, 6));
    level.setBlockAndUpdate(
        wrong.below(), ModPlanetContent.BLOCKS.get("moon_regolith").get().defaultBlockState());
    helper.assertTrue(
        !state.canSurvive(level, wrong), "Un cultivo lunar sobrevivió a la atmósfera terrestre");
    var moon = level.getServer().getLevel(profile.celestial().dimension());
    var pos = new BlockPos(21000, 240, 21000);
    moon.setBlockAndUpdate(
        pos.below(), ModPlanetContent.BLOCKS.get("moon_regolith").get().defaultBlockState());
    moon.setBlockAndUpdate(pos, state);
    helper.assertTrue(state.canSurvive(moon, pos), "No reconoció el hábitat lunar");
    plant.performBonemeal(moon, RandomSource.create(1), pos, state);
    plant.performBonemeal(moon, RandomSource.create(1), pos, moon.getBlockState(pos));
    helper.assertTrue(
        moon.getBlockState(pos).getValue(AlienPlantBlock.AGE) == 2, "No maduró el cultivo nativo");
    var player = FakePlayerFactory.get(moon, new GameProfile(UUID.randomUUID(), "GCLunarGrower"));
    var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    moon.getBlockState(pos).useWithoutItem(moon, player, hit);
    moon.getBlockState(pos).useWithoutItem(moon, player, hit);
    var drops =
        moon.getEntitiesOfClass(
            ItemEntity.class,
            new AABB(pos).inflate(2),
            item -> item.getItem().is(ModBiosphere.HARVEST.get("moon").get()));
    helper.assertTrue(
        drops.stream().mapToInt(item -> item.getItem().getCount()).sum() == 2
            && moon.getBlockState(pos).getValue(AlienPlantBlock.AGE) == 0,
        "La cosecha se duplicó o no rebrotó");
    drops.forEach(Entity::discard);
    moon.removeBlock(pos, false);
    moon.removeBlock(pos.below(), false);
    done(helper, "native_crop_rules");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void colonial_trade_authority(GameTestHelper helper) {
    var level = helper.getLevel();
    var pos = helper.absolutePos(new BlockPos(6, 2, 6));
    var npc = ModBiosphere.CITIZENS.get("earth").get().create(level);
    npc.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
    level.addFreshEntity(npc);
    var player =
        FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GCColonialTrade"));
    player.setPos(pos.getX() + 1, pos.getY(), pos.getZ());
    var menu = new ColonyMenu(41, player.getInventory(), npc);
    player.containerMenu = menu;
    helper.assertTrue(
        !menu.act(player, 0) && npc.stock() == 8, "Un comercio sin pago alteró existencias");
    player.getInventory().setItem(0, new ItemStack(ModItems.TITANIUM_ORE.get(), 6));
    helper.assertTrue(
        menu.act(player, 0)
            && ColonyCitizen.count(player, ModItems.TITANIUM_ORE.get()) == 2
            && ColonyCitizen.count(player, ModItems.OXYGEN_TANK.get()) == 1
            && npc.stock() == 7,
        "Pago o tanque incorrectos");
    helper.assertTrue(!menu.act(player, 3), "Cobró un contrato no aceptado");
    player.getInventory().setItem(1, new ItemStack(ModBiosphere.HARVEST.get("earth").get(), 16));
    helper.assertTrue(
        menu.act(player, 2) && !menu.act(player, 2) && menu.act(player, 3),
        "Aceptación o entrega inválida");
    int xp = player.totalExperience;
    helper.assertTrue(
        xp == 150 && !menu.act(player, 3) && player.totalExperience == xp, "Recompensa repetida");
    helper.assertTrue(
        menu.act(player, 4) && npc.stock() == 6, "Las fichas no compraron suministros");
    var saved = new net.minecraft.nbt.CompoundTag();
    npc.saveWithoutId(saved);
    var copy = ModBiosphere.CITIZENS.get("earth").get().create(level);
    copy.load(saved);
    helper.assertTrue(copy.stock() == 6, "Se perdió el stock al guardar");
    player.setPos(pos.getX() + 20, pos.getY(), pos.getZ());
    helper.assertTrue(!menu.act(player, 4), "Comerció a distancia");
    player.setPos(pos.getX() + 1, pos.getY(), pos.getZ());
    player.containerMenu = player.inventoryMenu;
    helper.assertTrue(!menu.act(player, 4), "Aceptó una sesión cerrada");
    helper.assertTrue(
        player.getData(ModBiosphere.CONTRACTS.get()).completed.contains("earth"),
        "No guardó el contrato personal");
    done(helper, "colonial_trade_authority");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void native_fauna_breeding(GameTestHelper helper) {
    var level = helper.getLevel();
    var a = ModBiosphere.FAUNA.get("moon_crystal_bovine").get().create(level);
    var b = ModBiosphere.FAUNA.get("moon_crystal_bovine").get().create(level);
    var foreign = ModBiosphere.FAUNA.get("mars_crystal_bovine").get().create(level);
    var sheep = ModBiosphere.FAUNA.get("moon_lumen_sheep").get().create(level);
    a.setInLove(null);
    b.setInLove(null);
    foreign.setInLove(null);
    sheep.setInLove(null);
    helper.assertTrue(
        a.canMate(b) && !a.canMate(foreign) && !a.canMate(sheep), "Cruzó especies o planetas");
    var baby = a.getBreedOffspring(level, b);
    helper.assertTrue(
        baby != null
            && baby.getType() == a.getType()
            && a.isFood(new ItemStack(ModBiosphere.HARVEST.get("moon").get()))
            && !a.isFood(new ItemStack(ModBiosphere.HARVEST.get("mars").get())),
        "Genética o dieta nativa inválida");
    var pos = helper.absolutePos(new BlockPos(6, 2, 6));
    sheep.setPos(pos.getX(), pos.getY(), pos.getZ());
    var player =
        FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GCAlienShepherd"));
    player.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
    sheep.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
    sheep.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
    var drops =
        level.getEntitiesOfClass(
            ItemEntity.class,
            new AABB(pos).inflate(3),
            item -> item.getItem().is(ModBiosphere.FIBER.get()));
    helper.assertTrue(
        drops.stream().mapToInt(item -> item.getItem().getCount()).sum() == 2,
        "La esquila ignoró su enfriamiento");
    done(helper, "native_fauna_breeding");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 400)
  public static void extreme_terrain_columns(GameTestHelper helper) {
    var server = helper.getLevel().getServer();
    for (var profile : LivingProfiles.ALL)
      if (!profile.body().equals("earth")) {
        var level = server.getLevel(profile.celestial().dimension());
        helper.assertTrue(
            level.getChunkSource().getGenerator() instanceof ExtremePlanetGenerator,
            "No activó el generador de " + profile.body());
        var generator = level.getChunkSource().getGenerator();
        helper.assertTrue(
            generator.getBiomeSource().possibleBiomes().size() == 3, "Faltan hábitats");
        var random = level.getChunkSource().randomState();
        int h = generator.getBaseHeight(-321, 777, Heightmap.Types.WORLD_SURFACE_WG, level, random);
        var column = generator.getBaseColumn(-321, 777, level, random);
        helper.assertTrue(
            h > 3 && h < 245 && !column.getBlock(h - 1).isAir() && column.getBlock(h).isAir(),
            "Altura/columna incoherente en " + profile.body());
      }
    for (String id : new String[] {"moon", "venus", "mars", "enceladus"}) {
      var level = server.getLevel(LivingProfiles.get(id).celestial().dimension());
      var chunk = level.getChunk(1400, 1400);
      helper.assertTrue(
          chunk != null && level.getHeight(Heightmap.Types.WORLD_SURFACE, 22400, 22400) > 3,
          "Generó un chunk vacío");
    }
    done(helper, "extreme_terrain_columns");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 200)
  public static void colony_piece_population(GameTestHelper helper) {
    var level = helper.getLevel();
    var root = helper.absolutePos(new BlockPos(10, 2, 10));
    var clip =
        new BoundingBox(
            root.getX() - 8,
            level.getMinBuildHeight(),
            root.getZ() - 8,
            root.getX() + 8,
            root.getY() + 10,
            root.getZ() + 8);
    var piece = new ColonyPiece("earth", root, 1, root);
    var random = RandomSource.create(7);
    var chunk = new net.minecraft.world.level.ChunkPos(root);
    piece.postProcess(
        level,
        level.structureManager(),
        level.getChunkSource().getGenerator(),
        random,
        clip,
        chunk,
        root);
    piece.postProcess(
        level,
        level.structureManager(),
        level.getChunkSource().getGenerator(),
        random,
        clip,
        chunk,
        root);
    var npcs = level.getEntitiesOfClass(ColonyCitizen.class, new AABB(root).inflate(6));
    helper.assertTrue(
        npcs.size() == 1
            && level.getBlockEntity(root.offset(2, 1, -2))
                instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
        "Duplicó colonos o falta cofre");
    npcs.forEach(Entity::discard);
    var garden = new ColonyPiece("earth", root, 2, root);
    garden.postProcess(
        level,
        level.structureManager(),
        level.getChunkSource().getGenerator(),
        random,
        clip,
        chunk,
        root);
    garden.postProcess(
        level,
        level.structureManager(),
        level.getChunkSource().getGenerator(),
        random,
        clip,
        chunk,
        root);
    helper.assertTrue(
        level.getEntitiesOfClass(CosmicGrazer.class, new AABB(root).inflate(6)).size() == 3,
        "El huerto no produjo exactamente tres especies");
    done(helper, "colony_piece_population");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void hunter_combat_variants(GameTestHelper helper) {
    var level = helper.getLevel();
    var root = helper.absolutePos(new BlockPos(6, 2, 6));
    var spitter = ModBiosphere.HUNTERS.get("mars").get().create(level);
    for (int x = -2; x <= 2; x++)
      for (int z = -2; z <= 2; z++)
        level.setBlockAndUpdate(root.offset(x, -1, z), Blocks.IRON_BLOCK.defaultBlockState());
    spitter.setPos(root.getX(), root.getY(), root.getZ());
    level.addFreshEntity(spitter);
    var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GCHunterTarget"));
    player.setPos(root.getX() + 8, root.getY(), root.getZ());
    spitter.performRangedAttack(player, 1);
    spitter.performRangedAttack(player, 1);
    helper.assertTrue(
        level
                .getEntitiesOfClass(
                    com.vic17usd.galaxycraft.combat.SpaceProjectile.class,
                    new AABB(root).inflate(12))
                .size()
            == 1,
        "El escupidor no respetó el enfriamiento");
    var pouncer = ModBiosphere.HUNTERS.get("moon").get().create(level);
    pouncer.setPos(root.getX(), root.getY(), root.getZ());
    pouncer.setTarget(player);
    pouncer.setOnGround(true);
    // Un mob recién creado aún no ha asentado la física; simula el contacto descendente con el suelo.
    pouncer.setDeltaMovement(0,-.08,0);
    pouncer.tickCount = 50;
    pouncer.aiStep();
    helper.assertTrue(pouncer.getDeltaMovement().y > 0, "El saltador no atacó: suelo="+pouncer.onGround()+", objetivo="+(pouncer.getTarget()!=null)+", estilo="+pouncer.style());
    var magma = ModBiosphere.HUNTERS.get("venus").get().create(level);
    var cow = EntityType.COW.create(level);
    cow.setPos(root.getX() + 1, root.getY(), root.getZ());
    helper.assertTrue(
        magma.doHurtTarget(cow) && cow.getRemainingFireTicks() > 0,
        "El cazador ígneo no aplicó su ataque");
    done(helper, "hunter_combat_variants");
  }
}
