package com.vic17usd.galaxycraft.testing;

import com.mojang.authlib.GameProfile;
import com.vic17usd.galaxycraft.component.OxygenData;
import com.vic17usd.galaxycraft.equipment.SpaceEquipment;
import com.vic17usd.galaxycraft.equipment.SpaceEquipmentInventory;
import com.vic17usd.galaxycraft.equipment.EquipmentEvents;
import com.vic17usd.galaxycraft.equipment.EquipmentNetworking;
import com.vic17usd.galaxycraft.registry.ModItems;
import com.vic17usd.galaxycraft.registry.ModBlocks;
import com.vic17usd.galaxycraft.registry.ModDataComponents;
import com.vic17usd.galaxycraft.block.RocketParts;
import com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
import com.vic17usd.galaxycraft.menu.SpaceEquipmentMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.UUID;

/** Pruebas con registros NeoForge reales; GameTestHooks las deshabilita automáticamente en producción. */
@GameTestHolder("galaxycraft")
@PrefixGameTestTemplate(false)
public final class GalaxyCraftGameTests {
    @GameTest(template = "tests/empty",timeoutTicks = 80)
    public static void navigation_authority(GameTestHelper helper) {
        var level=helper.getLevel();var root=helper.absolutePos(new BlockPos(6,2,6));
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setBlockAndUpdate(root.offset(x,-1,z),ModBlocks.LAUNCH_PAD.get().defaultBlockState());
        level.setBlockAndUpdate(root,ModBlocks.TIER1_ROCKET.get().defaultBlockState());
        var rocket=(RocketBlockEntity)level.getBlockEntity(root);
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"GCNavigation"));player.setPos(root.getX()+2.5,root.getY(),root.getZ()+.5);
        var menu=new com.vic17usd.galaxycraft.menu.NavigationMenu(37,player.getInventory(),rocket);player.containerMenu=menu;
        helper.assertTrue(com.vic17usd.galaxycraft.navigation.NavigationAccess.currentRocket(player)==rocket,"No encontró el ordenador abierto");
        var moon=com.vic17usd.galaxycraft.registry.ModPresentation.id("moon");var mars=com.vic17usd.galaxycraft.registry.ModPresentation.id("mars");
        helper.assertTrue(!menu.select(player,net.minecraft.resources.ResourceLocation.withDefaultNamespace("moon")),"Aceptó un namespace ajeno");
        helper.assertTrue(!menu.select(player,com.vic17usd.galaxycraft.registry.ModPresentation.id("unknown")),"Aceptó destino inexistente");
        helper.assertTrue(menu.select(player,mars),"No permitió previsualizar Marte");
        helper.assertTrue(menu.reason()==com.vic17usd.galaxycraft.navigation.NavigationRules.BlockReason.TIER,"No bloqueó Marte con cohete N1");
        helper.assertTrue(!menu.launch(player,mars),"Saltó el requisito de nivel");
        helper.assertTrue(menu.select(player,moon),"No seleccionó la Luna");
        helper.assertTrue(menu.reason()==com.vic17usd.galaxycraft.navigation.NavigationRules.BlockReason.FUEL,"No identificó falta de combustible");
        rocket.fuel.fill(new FluidStack(Fluids.LAVA,4000),IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(menu.reason()==com.vic17usd.galaxycraft.navigation.NavigationRules.BlockReason.OXYGEN,"No bloqueó la falta de equipo");
        SpaceEquipment.inventory(player).setStackInSlot(0,new ItemStack(ModItems.SPACE_HELMET.get()));
        SpaceEquipment.inventory(player).setStackInSlot(4,new ItemStack(ModItems.OXYGEN_TANK.get()));
        helper.assertTrue(menu.reason()==com.vic17usd.galaxycraft.navigation.NavigationRules.BlockReason.READY,"No confirmó requisitos completos");
        helper.assertTrue(!menu.launch(player,mars),"Aceptó confirmación de un destino desactualizado");
        player.setPos(root.getX()+30,root.getY(),root.getZ());helper.assertTrue(!menu.select(player,moon),"Aceptó selección a distancia");
        player.setPos(root.getX()+2.5,root.getY(),root.getZ()+.5);player.containerMenu=player.inventoryMenu;
        helper.assertTrue(!menu.launch(player,moon),"Aceptó un menú ya cerrado");player.containerMenu=menu;
        helper.assertTrue(menu.launch(player,moon),"No inició el viaje confirmado");
        RocketBlockEntity.tick(level,root,rocket.getBlockState(),rocket);
        helper.assertTrue(rocket.fuel.getFluidAmount()==4000&&rocket.launchStarted()<0,"Cancelar gastó combustible");
        helper.succeed();com.mojang.logging.LogUtils.getLogger().info("GALAXYCRAFT_INTEGRATION_OK navigation_authority");
    }
    @GameTest(template = "tests/empty", timeoutTicks = 80)
    public static void equipment_round_trip(GameTestHelper helper) {
        helper.assertTrue(com.vic17usd.galaxycraft.registry.ModSites.SCHEMATICS.size() == 145, "Faltan esquemas registrados");
        var tab = com.vic17usd.galaxycraft.registry.ModPresentation.GALAXY_TAB.get();
        tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(
                net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS,true,helper.getLevel().registryAccess()));
        for (var item : ModItems.ITEMS.getEntries()) helper.assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> stack.is(item.get())), "Falta un ítem en creativo: " + item.getId());
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "GCEquipmentTest"));
        var inventory = SpaceEquipment.inventory(player);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        var menu = new SpaceEquipmentMenu(1, player.getInventory());
        helper.assertTrue(!menu.slots.getFirst().mayPlace(new ItemStack(Items.DIAMOND)), "Aceptó un objeto ajeno al traje");
        helper.assertTrue(!menu.slots.get(4).mayPlace(new ItemStack(ModItems.SPACE_HELMET.get())), "Aceptó casco en ranura de tanque");
        player.getInventory().setItem(9, new ItemStack(ModItems.SPACE_HELMET.get()));
        menu.quickMoveStack(player, 8);
        helper.assertTrue(inventory.getStackInSlot(0).is(ModItems.SPACE_HELMET.get()), "Shift-clic no equipó el casco");
        helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "Shift-clic duplicó el casco");
        inventory.setStackInSlot(1, new ItemStack(ModItems.SPACE_CHESTPLATE.get()));
        inventory.setStackInSlot(2, new ItemStack(ModItems.SPACE_LEGGINGS.get()));
        inventory.setStackInSlot(3, new ItemStack(ModItems.SPACE_BOOTS.get()));
        var tank = new ItemStack(ModItems.OXYGEN_TANK.get()); tank.set(ModDataComponents.OXYGEN_LEVEL.get(),new OxygenData(7,20));
        inventory.setStackInSlot(4,tank); inventory.setStackInSlot(5,new ItemStack(ModItems.OXYGEN_TANK.get()));
        helper.assertTrue(SpaceEquipment.hasHelmet(player) && SpaceEquipment.fullSuit(player), "No reconoció el traje completo");
        helper.assertTrue(SpaceEquipment.consume(player), "No consumió el tanque equipado");
        helper.assertTrue(tank.get(ModDataComponents.OXYGEN_LEVEL.get()).current() == 6, "Consumo incorrecto");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), "Reemplazó la armadura tradicional");
        var restored = new SpaceEquipmentInventory(); restored.deserializeNBT(player.registryAccess(),inventory.serializeNBT(player.registryAccess()));
        helper.assertTrue(restored.getStackInSlot(4).get(ModDataComponents.OXYGEN_LEVEL.get()).equals(new OxygenData(6,20)), "Se perdió el componente al serializar");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),player.registryAccess());
        try {
            var snapshot = EquipmentNetworking.snapshot(player); EquipmentNetworking.Snapshot.CODEC.encode(buffer,snapshot);
            helper.assertTrue(EquipmentNetworking.Snapshot.CODEC.decode(buffer).equals(snapshot), "Snapshot de red incoherente");
        } finally { buffer.release(); }
        inventory.setStackInSlot(6,new ItemStack(com.vic17usd.galaxycraft.expansion.ModExpansion.THERMAL.get()));
        inventory.setStackInSlot(7,new ItemStack(com.vic17usd.galaxycraft.expansion.ModExpansion.SHIELD.get()));
        boolean previous = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        try {
            var drops = new ArrayList<ItemEntity>();
            helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true,helper.getLevel().getServer());
            EquipmentEvents.drops(new LivingDropsEvent(player,player.damageSources().generic(),drops,false));
            helper.assertTrue(drops.isEmpty() && !inventory.getStackInSlot(0).isEmpty(), "Ignoró keepInventory");
            helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false,helper.getLevel().getServer());
            EquipmentEvents.drops(new LivingDropsEvent(player,player.damageSources().generic(),drops,false));
            helper.assertTrue(drops.size() == 8, "No devolvió las ocho piezas al morir");
            for (int i=0;i<8;i++) helper.assertTrue(inventory.getStackInSlot(i).isEmpty(), "El equipo se duplicaría al reaparecer");
        } finally { helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(previous,helper.getLevel().getServer()); }
        helper.succeed(); com.mojang.logging.LogUtils.getLogger().info("GALAXYCRAFT_INTEGRATION_OK equipment_round_trip");
    }
    @GameTest(template = "tests/empty", timeoutTicks = 80)
    public static void rocket_multiblock_cleanup(GameTestHelper helper) {
        var level = helper.getLevel(); var root = helper.absolutePos(new BlockPos(6,2,6));
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) level.setBlockAndUpdate(root.offset(x,-1,z),ModBlocks.LAUNCH_PAD.get().defaultBlockState());
        level.setBlockAndUpdate(root,ModBlocks.TIER1_ROCKET.get().defaultBlockState());
        int count = 0;
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) for (int y=0;y<7;y++) if (level.getBlockState(root.offset(x,y,z)).is(ModBlocks.ROCKET_PART.get())) count++;
        helper.assertTrue(count == 62 && RocketParts.clear(level,root), "Reserva física incompleta");
        var rocket = (RocketBlockEntity)level.getBlockEntity(root);
        rocket.inventory.setStackInSlot(0,new ItemStack(Items.LAVA_BUCKET));
        rocket.inventory.setStackInSlot(1,new ItemStack(Items.BUCKET));
        rocket.fuel.fill(new FluidStack(Fluids.LAVA,4000),IFluidHandler.FluidAction.EXECUTE);
        var saved = rocket.saveWithoutMetadata(level.registryAccess()); rocket.loadWithComponents(saved,level.registryAccess());
        helper.assertTrue(rocket.fuel.getFluidAmount() == 4000 && rocket.inventory.getStackInSlot(0).is(Items.LAVA_BUCKET), "Persistencia de máquina rota");
        var pilot = FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"GCRocketTest"));
        pilot.setPos(root.getX() + 2.5,root.getY(),root.getZ() + .5);
        helper.assertTrue(!rocket.beginLaunch(pilot),"Permitió ir al vacío sin equipo");
        SpaceEquipment.inventory(pilot).setStackInSlot(0,new ItemStack(ModItems.SPACE_HELMET.get()));
        SpaceEquipment.inventory(pilot).setStackInSlot(4,new ItemStack(ModItems.OXYGEN_TANK.get()));
        helper.assertTrue(rocket.beginLaunch(pilot),"No inició el encendido con equipo y reserva completos");
        // El piloto simulado no pertenece a la lista de conexiones: equivale a desconectarse durante el encendido.
        RocketBlockEntity.tick(level,root,rocket.getBlockState(),rocket);
        helper.assertTrue(rocket.launchStarted() < 0 && rocket.fuel.getFluidAmount() == 4000,"Cancelar gastó combustible o dejó el encendido activo");
        level.destroyBlock(root.above(3),true);
        helper.assertTrue(level.getBlockState(root).isAir(), "Romper una pieza no desmontó la raíz");
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) for (int y=0;y<7;y++) helper.assertTrue(!level.getBlockState(root.offset(x,y,z)).is(ModBlocks.ROCKET_PART.get()),"Quedó una pieza huérfana");
        var drops = level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(root).inflate(5));
        helper.assertTrue(drops.stream().filter(item -> item.getItem().is(ModItems.TIER1_ROCKET.get())).mapToInt(item -> item.getItem().getCount()).sum() == 1,"El cohete se duplicó o no se devolvió");
        helper.assertTrue(drops.stream().filter(item -> item.getItem().is(Items.LAVA_BUCKET)).mapToInt(item -> item.getItem().getCount()).sum() == 1,"Entrada duplicada o perdida");
        helper.succeed(); com.mojang.logging.LogUtils.getLogger().info("GALAXYCRAFT_INTEGRATION_OK rocket_multiblock_cleanup");
    }
}
