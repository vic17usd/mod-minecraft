package com.vic17usd.galaxycraft.rover;

import com.vic17usd.galaxycraft.expansion.ModExpansion;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

/** Coloca únicamente en espacio libre; no consume el ítem si falla el registro de la entidad. */
public final class RoverItem extends Item {
  public RoverItem() {
    super(new Properties().stacksTo(1));
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    var pos = context.getClickedPos().relative(context.getClickedFace());
    var level = context.getLevel();
    var rover = ModExpansion.ROVER.get().create(level);
    if (rover == null) return InteractionResult.FAIL;
    rover.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
    if (context.getPlayer() != null) rover.setYRot(context.getPlayer().getYRot());
    if (!level.getWorldBorder().isWithinBounds(rover.getBoundingBox()) || !level.noCollision(rover))
      return InteractionResult.FAIL;
    if (!level.isClientSide()) {
      if (!level.addFreshEntity(rover)) return InteractionResult.FAIL;
      if (context.getPlayer() == null || !context.getPlayer().isCreative())
        context.getItemInHand().shrink(1);
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
  }
}
