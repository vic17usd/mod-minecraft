package com.vic17usd.galaxycraft.testing;

import com.mojang.authlib.GameProfile;
import com.vic17usd.galaxycraft.biosphere.ModBiosphere;
import com.vic17usd.galaxycraft.civilization.*;
import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import com.vic17usd.galaxycraft.morphology.*;
import com.vic17usd.galaxycraft.registry.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

/**
 * Integración sin GPU: poses autoritativas, embestidas/planeo y planos con pago y sesión válidos.
 */
@GameTestHolder("galaxycraft")
@PrefixGameTestTemplate(false)
public final class MorphologyGameTests {
  private static void done(GameTestHelper helper, String id) {
    helper.succeed();
    com.mojang.logging.LogUtils.getLogger().info("GALAXYCRAFT_INTEGRATION_OK {}", id);
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void alien_pose_authority(GameTestHelper helper) {
    var level = helper.getLevel();
    var mob = ModBiosphere.HUNTERS.get("mars").get().create(level);
    var pos = helper.absolutePos(new BlockPos(4, 2, 4));
    mob.setPos(pos.getX(), pos.getY(), pos.getZ());
    level.addFreshEntity(mob);
    MorphologyNetworking.animate(mob, AlienPose.SPIT, 22);
    var pose = mob.getData(ModMorphology.POSE);
    helper.assertTrue(
        pose.action() == AlienPose.SPIT
            && pose.start() == level.getGameTime()
            && pose.duration() == 22,
        "No sincronizó pose del servidor");
    var saved = new net.minecraft.nbt.CompoundTag();
    mob.saveWithoutId(saved);
    var copy = ModBiosphere.HUNTERS.get("mars").get().create(level);
    copy.load(saved);
    helper.assertTrue(
        copy.getData(ModMorphology.POSE).action() == AlienPose.IDLE,
        "Persistió una animación transitoria");
    mob.discard();
    done(helper, "alien_pose_authority");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void native_worm_and_glider(GameTestHelper helper) {
    var level = helper.getLevel();
    var pos = helper.absolutePos(new BlockPos(7, 2, 7));
    for (int x = -3; x <= 3; x++)
      for (int z = -3; z <= 3; z++)
        level.setBlockAndUpdate(pos.offset(x, -1, z), Blocks.IRON_BLOCK.defaultBlockState());
    var target = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GCGliderTarget"));
    target.setPos(pos.getX() + 5, pos.getY(), pos.getZ());
    var worm = ModAlienEntities.CRAWLERS.get("moon").get().create(level);
    worm.setPos(pos.getX(), pos.getY(), pos.getZ());
    level.addFreshEntity(worm);
    worm.setTarget(target);
    worm.setOnGround(true);
    worm.setDeltaMovement(0, -.08, 0);
    worm.tickCount = 80;
    worm.aiStep();
    helper.assertTrue(
        worm.getData(ModMorphology.POSE).action() == AlienPose.BURROW
            && worm.getDeltaMovement().horizontalDistance() > .1,
        "El gusano no embistió");
    var raider = ModFrontiers.RAIDERS.get("mars").get().create(level);
    raider.setPos(pos.getX(), pos.getY(), pos.getZ());
    level.addFreshEntity(raider);
    raider.setTarget(target);
    raider.setOnGround(true);
    raider.setDeltaMovement(0, -.08, 0);
    raider.tickCount = 65;
    raider.aiStep();
    helper.assertTrue(
        raider.getData(ModMorphology.POSE).action() == AlienPose.GLIDE
            && raider.getDeltaMovement().y > 0,
        "El raptor no desplegó alas");
    raider.setTarget(null);
    raider.setOnGround(false);
    raider.setDeltaMovement(0, -1, 0);
    raider.aiStep();
    helper.assertTrue(raider.getDeltaMovement().y >= -.121, "Caída sin planeo");
    helper.assertTrue(
        level.getBlockState(pos.below()).is(Blocks.IRON_BLOCK), "La embestida destruyó terreno");
    worm.discard();
    raider.discard();
    done(helper, "native_worm_and_glider");
  }

  @GameTest(template = "tests/empty", timeoutTicks = 100)
  public static void colony_blueprint_payment(GameTestHelper helper) {
    var level = helper.getLevel();
    var pos = helper.absolutePos(new BlockPos(5, 2, 5));
    var npc = ModBiosphere.CITIZENS.get("earth").get().create(level);
    npc.setPos(pos.getX(), pos.getY(), pos.getZ());
    level.addFreshEntity(npc);
    var player =
        FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "GCBlueprintBuyer"));
    player.setPos(npc.position());
    var menu = new ColonyMenu(62, player.getInventory(), npc);
    player.containerMenu = menu;
    helper.assertTrue(!menu.act(player, 5) && npc.stock() == 8, "Plano sin pago");
    player.getInventory().setItem(0, new ItemStack(ModBiosphere.TOKEN.get(), 4));
    helper.assertTrue(
        menu.act(player, 5)
            && npc.stock() == 7
            && ColonyCitizen.count(player, ModBiosphere.TOKEN.get()) == 0
            && ColonyCitizen.count(
                    player,
                    ModSites.SCHEMATICS
                        .get(
                            com.vic17usd.galaxycraft.expedition.ExpeditionCatalog.ALL.stream()
                                .filter(
                                    site ->
                                        site.body().equals("earth")
                                            && site.role().equals("observatory"))
                                .findFirst()
                                .orElseThrow()
                                .id())
                        .get())
                == 1,
        "Plano o pago incorrecto");
    helper.assertTrue(
        npc.getData(ModMorphology.POSE).action() == AlienPose.TRADE, "No animó el comercio");
    player.getInventory().setItem(0, new ItemStack(ModBiosphere.TOKEN.get(), 4));
    player.containerMenu = player.inventoryMenu;
    helper.assertTrue(
        !menu.act(player, 5) && ColonyCitizen.count(player, ModBiosphere.TOKEN.get()) == 4,
        "Comerció con sesión cerrada");
    npc.discard();
    done(helper, "colony_blueprint_payment");
  }
}
