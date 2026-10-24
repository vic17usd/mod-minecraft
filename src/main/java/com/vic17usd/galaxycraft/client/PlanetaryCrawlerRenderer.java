package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Silverfish;

/** Conserva la geometría del reptador y aplica una piel original para cada planeta. */
public final class PlanetaryCrawlerRenderer extends SilverfishRenderer {
    public PlanetaryCrawlerRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(Silverfish entity) {
        return ModPresentation.id("textures/entity/" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath() + ".png");
    }
}
