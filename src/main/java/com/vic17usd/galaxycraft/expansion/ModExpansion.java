package com.vic17usd.galaxycraft.expansion;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.quest.QuestMenu;
import com.vic17usd.galaxycraft.quest.QuestState;
import com.vic17usd.galaxycraft.registry.ModBlocks;
import com.vic17usd.galaxycraft.registry.ModItems;
import com.vic17usd.galaxycraft.rover.RoverEntity;
import com.vic17usd.galaxycraft.rover.RoverItem;
import com.vic17usd.galaxycraft.rover.RoverMenu;
import com.vic17usd.galaxycraft.station.StationTerminalItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;

/** Registros de la expansión: todas las clases cliente quedan fuera del arranque dedicado. */
public final class ModExpansion {
  public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
      DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE =
      COMPONENTS.register(
          "battery_charge",
          () ->
              DataComponentType.<Integer>builder()
                  .persistent(com.mojang.serialization.Codec.intRange(0, 6000))
                  .networkSynchronized(ByteBufCodecs.VAR_INT)
                  .build());
  public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
      DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<AttachmentType<?>, AttachmentType<QuestState>> QUESTS =
      ATTACHMENTS.register(
          "quests", () -> AttachmentType.serializable(QuestState::new).copyOnDeath().build());
  public static final DeferredHolder<AttachmentType<?>, AttachmentType<Exposure>> EXPOSURE =
      ATTACHMENTS.register("exposure", () -> AttachmentType.builder(Exposure::new).build());
  public static final DeferredItem<Item> THERMAL =
      ModItems.ITEMS.registerSimpleItem("thermal_module", new Item.Properties().durability(600));
  public static final DeferredItem<Item> SHIELD =
      ModItems.ITEMS.registerSimpleItem(
          "lead_shield_module", new Item.Properties().durability(600));
  public static final DeferredItem<Item> RAW_LEAD = ModItems.ITEMS.registerSimpleItem("raw_lead");
  public static final DeferredItem<Item> LEAD = ModItems.ITEMS.registerSimpleItem("lead_ingot");
  public static final DeferredItem<BatteryItem> BATTERY =
      ModItems.ITEMS.register("rover_battery", BatteryItem::new);
  public static final DeferredItem<Item> NOTEBOOK =
      ModItems.ITEMS.register(
          "quest_notebook",
          () ->
              new Item(new Item.Properties().stacksTo(1)) {
                @Override
                public net.minecraft.world.InteractionResultHolder<
                        net.minecraft.world.item.ItemStack>
                    use(
                        net.minecraft.world.level.Level level,
                        net.minecraft.world.entity.player.Player player,
                        net.minecraft.world.InteractionHand hand) {
                  if (player instanceof net.minecraft.server.level.ServerPlayer server)
                    com.vic17usd.galaxycraft.quest.QuestService.open(server);
                  return net.minecraft.world.InteractionResultHolder.sidedSuccess(
                      player.getItemInHand(hand), level.isClientSide());
                }
              });
  public static final DeferredItem<Item> STATION_KIT =
      ModItems.ITEMS.registerSimpleItem("station_kit", new Item.Properties().stacksTo(1));
  public static final DeferredItem<StationTerminalItem> TERMINAL =
      ModItems.ITEMS.register("orbital_terminal", StationTerminalItem::new);
  public static final DeferredItem<RoverItem> ROVER_ITEM =
      ModItems.ITEMS.register("planetary_rover", RoverItem::new);
  public static final DeferredBlock<Block> LEAD_ORE =
      ModBlocks.BLOCKS.registerSimpleBlock(
          "lead_ore", BlockBehaviour.Properties.of().strength(3).requiresCorrectToolForDrops());
  public static final DeferredBlock<SpaceAirlockBlock> AIRLOCK =
      ModBlocks.BLOCKS.register(
          "airlock",
          () ->
              new SpaceAirlockBlock(
                  BlockBehaviour.Properties.of().strength(4).noOcclusion().sound(SoundType.METAL)));
  public static final DeferredBlock<ExpansionMachineBlock> DETECTOR =
      machine("pressure_detector", 0);
  public static final DeferredBlock<ExpansionMachineBlock> SOLAR = machine("solar_panel", 1);
  public static final DeferredBlock<ExpansionMachineBlock> DOCK = machine("docking_module", 2);
  public static final DeferredBlock<ExpansionMachineBlock> GRAVITY =
      machine("gravity_generator", 3);

  private static DeferredBlock<ExpansionMachineBlock> machine(String name, int kind) {
    return ModBlocks.BLOCKS.register(
        name,
        () ->
            new ExpansionMachineBlock(
                BlockBehaviour.Properties.of().strength(4).sound(SoundType.METAL), kind));
  }

  public static void registerBlockItems() {
    ModItems.ITEMS.registerSimpleBlockItem("lead_ore", LEAD_ORE);
    ModItems.ITEMS.register(
        "airlock", () -> new DoubleHighBlockItem(AIRLOCK.get(), new Item.Properties()));
    ModItems.ITEMS.registerSimpleBlockItem("pressure_detector", DETECTOR);
    ModItems.ITEMS.registerSimpleBlockItem("solar_panel", SOLAR);
    ModItems.ITEMS.registerSimpleBlockItem("docking_module", DOCK);
    ModItems.ITEMS.registerSimpleBlockItem("gravity_generator", GRAVITY);
  }

  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExpansionMachine>>
      MACHINE =
          BLOCK_ENTITIES.register(
              "expansion_machine",
              () ->
                  BlockEntityType.Builder.of(
                          ExpansionMachine::new,
                          DETECTOR.get(),
                          SOLAR.get(),
                          DOCK.get(),
                          GRAVITY.get())
                      .build(null));
  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<MenuType<?>, MenuType<QuestMenu>> QUEST_MENU =
      MENUS.register(
          "quests",
          () ->
              new MenuType<>(QuestMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
  public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE_MENU =
      MENUS.register("expansion_machine", () -> IMenuTypeExtension.create(MachineMenu::new));
  public static final DeferredHolder<MenuType<?>, MenuType<RoverMenu>> ROVER_MENU =
      MENUS.register("rover", () -> IMenuTypeExtension.create(RoverMenu::new));
  public static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.ENTITY_TYPE, GalaxyCraft.MOD_ID);
  public static final DeferredHolder<EntityType<?>, EntityType<RoverEntity>> ROVER =
      ENTITIES.register(
          "planetary_rover",
          () ->
              EntityType.Builder.<RoverEntity>of(RoverEntity::new, MobCategory.MISC)
                  .sized(1.8F, 1.15F)
                  .clientTrackingRange(10)
                  .updateInterval(2)
                  .build("planetary_rover"));

  public static void capabilities(RegisterCapabilitiesEvent event) {
    event.registerBlockEntity(
        Capabilities.EnergyStorage.BLOCK, MACHINE.get(), (machine, side) -> machine.energy);
    event.registerBlockEntity(
        Capabilities.ItemHandler.BLOCK, MACHINE.get(), (machine, side) -> machine.inventory);
    event.registerEntity(
        Capabilities.EnergyStorage.ENTITY, ROVER.get(), (rover, context) -> rover.battery);
    event.registerEntity(
        Capabilities.ItemHandler.ENTITY, ROVER.get(), (rover, context) -> rover.inventory);
  }

  private ModExpansion() {}
}
