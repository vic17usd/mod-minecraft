package com.vic17usd.galaxycraft.logistics;

import com.vic17usd.galaxycraft.component.OxygenData;
import com.vic17usd.galaxycraft.expansion.ModExpansion;
import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import com.vic17usd.galaxycraft.item.OxygenTankItem;
import com.vic17usd.galaxycraft.registry.ModDataComponents;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.neoforged.neoforge.capabilities.*;

/** FE y fluidos usan APIs NeoForge; el gas usa una capacidad propia y nunca duplica un tanque. */
public final class LogisticsCapabilities {
  public static final BlockCapability<IOxygenStorage, Void> OXYGEN =
      BlockCapability.createVoid(ModPresentation.id("oxygen_storage"), IOxygenStorage.class);

  public static void register(RegisterCapabilitiesEvent event) {
    event.registerBlock(
        Capabilities.FluidHandler.BLOCK,
        (level, pos, state, entity, side) -> new LaunchPadFuelPort(level, pos),
        com.vic17usd.galaxycraft.registry.ModBlocks.LAUNCH_PAD.get());
    event.registerBlockEntity(
        Capabilities.EnergyStorage.BLOCK,
        ModFrontiers.MACHINE.get(),
        (machine, side) -> machine.kind() == 4 || machine.kind() == 5 ? machine.energy : null);
    event.registerBlockEntity(
        Capabilities.FluidHandler.BLOCK,
        ModFrontiers.MACHINE.get(),
        (machine, side) -> machine.kind() == 3 || machine.kind() == 5 ? machine.fuel : null);
    event.registerBlockEntity(
        Capabilities.ItemHandler.BLOCK,
        ModFrontiers.MACHINE.get(),
        (machine, side) -> machine.kind() >= 3 ? machine.inventory : null);
    event.registerBlockEntity(
        OXYGEN,
        ModFrontiers.MACHINE.get(),
        (machine, context) -> machine.kind() == 6 ? machine.oxygen : null);
    event.registerBlockEntity(
        OXYGEN,
        ModExpansion.MACHINE.get(),
        (machine, context) ->
            machine.kind() != 0
                ? null
                : new IOxygenStorage() {
                  private net.minecraft.world.item.ItemStack tank() {
                    return machine.inventory.getStackInSlot(0);
                  }

                  public int stored() {
                    return tank().getItem() instanceof OxygenTankItem
                        ? OxygenTankItem.oxygen(tank()).current()
                        : 0;
                  }

                  public int capacity() {
                    return tank().getItem() instanceof OxygenTankItem
                        ? OxygenTankItem.oxygen(tank()).maximum()
                        : 0;
                  }

                  public int extract(int amount, boolean simulate) {
                    return 0;
                  }

                  public int receive(int amount, boolean simulate) {
                    int added = Math.clamp(amount, 0, capacity() - stored());
                    if (added > 0 && !simulate) {
                      tank()
                          .set(
                              ModDataComponents.OXYGEN_LEVEL.get(),
                              new OxygenData(stored() + added, capacity()));
                      machine.setChanged();
                    }
                    return added;
                  }
                });
  }

  private LogisticsCapabilities() {}
}
