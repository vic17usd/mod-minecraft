package com.vic17usd.galaxycraft.rocket;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Prueba las transacciones reales de inventario/fluido, incluidas las rutas sin espacio. */
class RocketFuelingTest {
    @BeforeAll static void initializeMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private FluidTank tank() { return new FluidTank(4000, stack -> stack.is(Fluids.LAVA)); }
    private ItemStackHandler inventory() {
        var inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, new ItemStack(Items.LAVA_BUCKET));
        return inventory;
    }

    @Test void cargaConservaElEnvase() {
        var inventory = inventory();
        var tank = tank();
        assertTrue(RocketFueling.refuel(inventory, tank));
        assertEquals(1000, tank.getFluidAmount());
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertTrue(inventory.getStackInSlot(1).is(Items.BUCKET));
        assertFalse(RocketFueling.refuel(inventory, tank));
        assertEquals(1000, tank.getFluidAmount());
    }

    @Test void faltaDeEspacioNoConsumeUnaFraccionDelCubo() {
        var inventory = inventory();
        var tank = tank();
        tank.fill(new FluidStack(Fluids.LAVA, 3500), FluidAction.EXECUTE);
        assertFalse(RocketFueling.refuel(inventory, tank));
        assertEquals(3500, tank.getFluidAmount());
        assertTrue(inventory.getStackInSlot(0).is(Items.LAVA_BUCKET));
        assertTrue(inventory.getStackInSlot(1).isEmpty());
    }

    @Test void salidaLlenaOIncompatibleNoConsumeCombustible() {
        var inventory = inventory();
        var tank = tank();
        inventory.setStackInSlot(1, new ItemStack(Items.BUCKET, 16));
        assertFalse(RocketFueling.refuel(inventory, tank));
        inventory.setStackInSlot(1, new ItemStack(Items.STONE));
        assertFalse(RocketFueling.refuel(inventory, tank));
        assertEquals(0, tank.getFluidAmount());
        assertTrue(inventory.getStackInSlot(0).is(Items.LAVA_BUCKET));
    }

    @Test void cuatroCubosLlenanElDepositoSinDuplicarEnvases() {
        var inventory = inventory();
        var tank = tank();
        for (int i = 0; i < 4; i++) {
            inventory.setStackInSlot(0, new ItemStack(Items.LAVA_BUCKET));
            assertTrue(RocketFueling.refuel(inventory, tank));
        }
        inventory.setStackInSlot(0, new ItemStack(Items.LAVA_BUCKET));
        assertFalse(RocketFueling.refuel(inventory, tank));
        assertEquals(4000, tank.getFluidAmount());
        assertEquals(4, inventory.getStackInSlot(1).getCount());
        assertTrue(inventory.getStackInSlot(0).is(Items.LAVA_BUCKET));
    }

    @Test void capacidadNeoForgeRechazaAguaYSimularNoModifica() {
        var tank = tank();
        assertEquals(0, tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
        assertEquals(1000, tank.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.SIMULATE));
        assertTrue(tank.isEmpty());
    }
}
