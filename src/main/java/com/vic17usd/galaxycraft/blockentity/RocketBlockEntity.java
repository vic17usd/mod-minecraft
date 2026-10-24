package com.vic17usd.galaxycraft.blockentity;

import com.vic17usd.galaxycraft.block.LaunchPadBlock;
import com.vic17usd.galaxycraft.block.RocketBlock;
import com.vic17usd.galaxycraft.menu.RocketMenu;
import com.vic17usd.galaxycraft.registry.ModRocketRegistries;
import com.vic17usd.galaxycraft.rocket.RocketFueling;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Máquina persistente: dos ranuras y depósito de lava accesible mediante capabilities. */
public final class RocketBlockEntity extends BlockEntity implements MenuProvider {
    private int destination = 1;
    public int destination() { return destination; }
    public int tier() { return ((RocketBlock) getBlockState().getBlock()).tier(); }
    public void nextDestination() {
        for (int i = 0; i < SolarSystem.BODIES.size(); i++) {
            destination = (destination + 1) % SolarSystem.BODIES.size();
            var body = SolarSystem.destination(destination);
            if (SolarSystem.accessible(tier(), body) && !body.dimension().equals(level.dimension())) break;
        }
        setChanged();
    }
    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && stack.is(Items.LAVA_BUCKET); }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public final FluidTank fuel = new FluidTank(RocketFueling.CAPACITY, stack -> stack.is(Fluids.LAVA)) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    // Valores menores de 32768: el protocolo de propiedades de menú usa enteros cortos.
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return switch (index) {
            case 0 -> fuel.getFluidAmount(); case 1 -> readiness(); case 2 -> tier(); default -> destination; }; }
        @Override public void set(int index, int value) { /* El servidor es la autoridad. */ }
        @Override public int getCount() { return 4; }
    };

    public RocketBlockEntity(BlockPos pos, BlockState state) { super(ModRocketRegistries.ROCKET.get(), pos, state); }

    /** 0: base incompleta, 1: obstrucción, 2: falta combustible, 3: preparado. */
    public int readiness() {
        if (level == null || !LaunchPadBlock.isComplete(level, worldPosition.below())) return 0;
        if (!RocketBlock.hasClearance(level, worldPosition)) return 1;
        return fuel.getFluidAmount() < RocketFueling.CAPACITY ? 2 : 3;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RocketBlockEntity rocket) {
        if (level.getGameTime() % 20 == 0) RocketFueling.refuel(rocket.inventory, rocket.fuel);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // El NBT con HolderLookup es el almacenamiento correcto para una BlockEntity de 1.21.
        // Los componentes de ItemStack de la fase 2 no sustituyen este ciclo de persistencia.
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Fuel", fuel.writeToNBT(registries, new CompoundTag()));
        // El identificador estable evita cambiar destinos al reordenar el catálogo.
        tag.putString("DestinationId", SolarSystem.destination(destination).id());
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        fuel.readFromNBT(registries, tag.getCompound("Fuel"));
        destination = tag.contains("DestinationId") ? SolarSystem.indexOf(tag.getString("DestinationId"))
                : tag.contains("Destination") ? Math.floorMod(tag.getInt("Destination"), SolarSystem.BODIES.size()) : 1;
        // Descarta fluidos incompatibles y recorta cantidades de datos externos editados.
        if (!fuel.isEmpty() && !fuel.isFluidValid(fuel.getFluid())) fuel.setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
        else if (fuel.getFluidAmount() > fuel.getCapacity()) fuel.setFluid(fuel.getFluid().copyWithAmount(fuel.getCapacity()));
    }

    @Override public Component getDisplayName() { return Component.translatable("container.galaxycraft.rocket"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new RocketMenu(id, playerInventory, inventory, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
