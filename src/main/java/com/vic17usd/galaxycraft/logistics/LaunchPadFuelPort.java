package com.vic17usd.galaxycraft.logistics;

import com.vic17usd.galaxycraft.block.LaunchPadBlock;
import com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Puerto dinámico de entrada: una capacidad cacheada sigue válida al poner o quitar el cohete. */
public final class LaunchPadFuelPort implements IFluidHandler {
  private final Level level;
  private final BlockPos pad;

  public LaunchPadFuelPort(Level level, BlockPos pad) {
    this.level = level;
    this.pad = pad.immutable();
  }

  private RocketBlockEntity rocket() {
    for (int x = -1; x <= 1; x++)
      for (int z = -1; z <= 1; z++) {
        var pos = pad.offset(x, 1, z);
        if (level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
            && level.getBlockEntity(pos) instanceof RocketBlockEntity rocket
            && !rocket.isRemoved()
            && LaunchPadBlock.isComplete(level, pos.below())) return rocket;
      }
    return null;
  }

  public int getTanks() {
    return 1;
  }

  public FluidStack getFluidInTank(int tank) {
    var rocket = rocket();
    return tank == 0 && rocket != null ? rocket.fuel.getFluid().copy() : FluidStack.EMPTY;
  }

  public int getTankCapacity(int tank) {
    var rocket = rocket();
    return tank == 0 && rocket != null ? rocket.fuel.getCapacity() : 0;
  }

  public boolean isFluidValid(int tank, FluidStack stack) {
    var rocket = rocket();
    return tank == 0 && rocket != null && rocket.fuel.isFluidValid(stack);
  }

  public int fill(FluidStack stack, FluidAction action) {
    var rocket = rocket();
    return rocket == null ? 0 : rocket.fuel.fill(stack, action);
  }

  public FluidStack drain(FluidStack stack, FluidAction action) {
    return FluidStack.EMPTY;
  }

  public FluidStack drain(int max, FluidAction action) {
    return FluidStack.EMPTY;
  }
}
