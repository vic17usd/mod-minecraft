package com.vic17usd.galaxycraft.rocket;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Transfiere un cubo completo de forma atómica, sin perder combustible ni envases. */
public final class RocketFueling {
    public static final int CAPACITY = 4000;
    public static final int BUCKET_VOLUME = 1000;

    public static boolean refuel(ItemStackHandler inventory, FluidTank tank) {
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack output = inventory.getStackInSlot(1);
        if (!input.is(Items.LAVA_BUCKET) || (!output.isEmpty() && !output.is(Items.BUCKET))
                || output.getCount() >= Items.BUCKET.getDefaultMaxStackSize()) return false;
        FluidStack fuel = new FluidStack(Fluids.LAVA, BUCKET_VOLUME);
        // Primero simula: un depósito parcialmente lleno no consume un cubo completo.
        if (tank.fill(fuel, FluidAction.SIMULATE) != BUCKET_VOLUME) return false;
        tank.fill(fuel, FluidAction.EXECUTE);
        inventory.extractItem(0, 1, false);
        inventory.setStackInSlot(1, new ItemStack(Items.BUCKET, output.getCount() + 1));
        return true;
    }

    private RocketFueling() {}
}
