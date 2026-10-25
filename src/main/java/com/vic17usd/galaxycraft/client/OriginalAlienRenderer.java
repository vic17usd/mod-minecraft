package com.vic17usd.galaxycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vic17usd.galaxycraft.morphology.CreatureDesign;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Renderizador propio, máscara emisiva y material translúcido para membranas y biomasa. */
public final class OriginalAlienRenderer<T extends Mob>
    extends MobRenderer<T, OriginalAlienModel<T>> {
  private final ResourceLocation texture;
  private final CreatureDesign design;

  public OriginalAlienRenderer(EntityRendererProvider.Context context, String id) {
    super(context, new OriginalAlienModel<>(CreatureDesign.of(id), id.hashCode()), .55F);
    design = CreatureDesign.of(id);
    texture = ModPresentation.id("textures/entity/" + id + ".png");
    var glow = RenderType.eyes(ModPresentation.id("textures/entity/" + id + "_glow.png"));
    addLayer(
        new EyesLayer<T, OriginalAlienModel<T>>(this) {
          @Override
          public RenderType renderType() {
            return glow;
          }
        });
  }

  @Override
  public ResourceLocation getTextureLocation(T entity) {
    return texture;
  }

  @Override
  protected RenderType getRenderType(
      T entity, boolean visible, boolean translucent, boolean outline) {
    if (visible && (design == CreatureDesign.LUMEN || design == CreatureDesign.RAPTOR))
      return RenderType.entityTranslucent(texture);
    return super.getRenderType(entity, visible, translucent, outline);
  }

  @Override
  protected void scale(T mob, PoseStack stack, float partial) {
    float scale =
        switch (design) {
          case WORM -> .47F;
          case GUARDIAN, QUEEN -> 1.25F;
          default -> 1;
        };
    stack.scale(scale, scale, scale);
  }
}
