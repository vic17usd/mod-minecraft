package com.vic17usd.galaxycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import com.vic17usd.galaxycraft.rover.RoverEntity;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Chasis, asiento, depósito y cuatro ruedas independientes con textura original. */
public final class RoverRenderer extends EntityRenderer<RoverEntity> {
  private final ModelPart model;

  public RoverRenderer(EntityRendererProvider.Context context) {
    super(context);
    shadowRadius = 1;
    var mesh = new MeshDefinition();
    var root = mesh.getRoot();
    root.addOrReplaceChild(
        "body",
        CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-11, 5, -15, 22, 5, 30)
            .texOffs(0, 40)
            .addBox(-7, 10, -6, 14, 3, 12)
            .addBox(-7, 13, -7, 14, 10, 3)
            .addBox(-10, 10, -14, 20, 7, 5),
        PartPose.ZERO);
    for (int i = 0; i < 4; i++)
      root.addOrReplaceChild(
          "wheel" + i,
          CubeListBuilder.create().texOffs(80, 0).addBox(-2, -4, -4, 4, 8, 8),
          PartPose.offset(i % 2 == 0 ? -12 : 12, 4, i < 2 ? -10 : 10));
    model = LayerDefinition.create(mesh, 128, 128).bakeRoot();
  }

  @Override
  public ResourceLocation getTextureLocation(RoverEntity entity) {
    return ModPresentation.id("textures/entity/planetary_rover.png");
  }

  @Override
  public void render(
      RoverEntity entity,
      float yaw,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light) {
    pose.pushPose();
    pose.mulPose(Axis.YP.rotationDegrees(-yaw));
    float rotation = entity.wheelAngle;
    for (int i = 0; i < 4; i++) model.getChild("wheel" + i).xRot = rotation;
    model.render(
        pose,
        buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))),
        light,
        OverlayTexture.NO_OVERLAY);
    pose.popPose();
    super.render(entity, yaw, partial, pose, buffers, light);
  }
}
