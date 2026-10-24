package com.vic17usd.galaxycraft.registry;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
import com.vic17usd.galaxycraft.menu.RocketMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registros comunes de máquinas, menús y capacidades; no importa clases cliente. */
public final class ModRocketRegistries {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GalaxyCraft.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, GalaxyCraft.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RocketBlockEntity>> ROCKET =
            BLOCK_ENTITIES.register("tier1_rocket", () -> BlockEntityType.Builder.of(
                    RocketBlockEntity::new, ModBlocks.TIER1_ROCKET.get(), ModBlocks.TIER2_ROCKET.get(), ModBlocks.TIER3_ROCKET.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<RocketMenu>> ROCKET_MENU =
            MENUS.register("rocket", () -> IMenuTypeExtension.create(RocketMenu::new));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Los mismos handlers permanecen estables durante la vida de la máquina.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ROCKET.get(), (rocket, side) -> rocket.fuel);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ROCKET.get(), (rocket, side) -> rocket.inventory);
    }

    private ModRocketRegistries() {}
}
