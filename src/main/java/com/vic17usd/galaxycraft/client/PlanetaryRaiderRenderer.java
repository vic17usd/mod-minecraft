package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/** Cada registro planetario tiene su propia piel; no usa texturas de mobs vanilla. */
public final class PlanetaryRaiderRenderer extends HuskRenderer {
  public PlanetaryRaiderRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public ResourceLocation getTextureLocation(Zombie entity) {
    return ModPresentation.id(
        "textures/entity/"
            + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath()
            + ".png");
  }
}
