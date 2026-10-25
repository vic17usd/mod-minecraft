package com.vic17usd.galaxycraft.client;

import com.mojang.blaze3d.vertex.*;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Modelos propios para ganadería, colonos y tres morfologías de depredador; texturas por registro.
 */
public final class BiosphereMobRenderer<T extends Mob>
    extends MobRenderer<T, BiosphereMobRenderer.LifeModel<T>> {
  private final String shape;

  public BiosphereMobRenderer(EntityRendererProvider.Context context, String shape, int seed) {
    super(context, new LifeModel<>(shape, seed), .65F);
    this.shape = shape;
  }

  @Override
  public ResourceLocation getTextureLocation(T entity) {
    return ModPresentation.id(
        "textures/entity/"
            + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath()
            + ".png");
  }

  @Override
  protected int getBlockLightLevel(T entity, BlockPos pos) {
    return shape.equals("lumen_sheep") ? 15 : super.getBlockLightLevel(entity, pos);
  }

  public static final class LifeModel<T extends Mob> extends EntityModel<T> {
    private final ModelPart root, head;
    private final int limbs;
    private final boolean citizen;

    LifeModel(String shape, int seed) {
      var mesh = new MeshDefinition();
      var parts = mesh.getRoot();
      citizen = shape.equals("colonist");
      boolean hunter = shape.equals("pouncer") || shape.equals("magma") || shape.equals("spitter");
      limbs = citizen ? 4 : hunter ? 8 : 4;
      if (citizen) {
        parts.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4, 0, -3, 8, 12, 6),
            PartPose.offset(0, 0, 0));
        parts.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-5, -10, -4, 10, 10, 8)
                .texOffs(50, 32)
                .addBox(-4, -15, -1, 1, 5, 1)
                .addBox(3, -15, -1, 1, 5, 1),
            PartPose.ZERO);
      } else {
        float w = shape.equals("lumen_sheep") ? 15 : 12, depth = shape.equals("spitter") ? 23 : 18;
        parts.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-w / 2, -4, -depth / 2, w, 9, depth),
            PartPose.offset(0, 14, 0));
        parts.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 45).addBox(-4, -4, -5, 8, 7, 7),
            PartPose.offset(0, 12, -11));
        for (int i = 0; i < 4; i++)
          parts.addOrReplaceChild(
              "shell" + i,
              CubeListBuilder.create()
                  .texOffs(66, 0)
                  .addBox(-2, -5 - (seed + i) % 3, -2, 4, 5 + (seed + i) % 3, 4),
              PartPose.offset((i % 2 == 0 ? -1 : 1) * 4, 10, (i / 2) * 8 - 4));
        if (shape.equals("crystal_bovine") || shape.equals("plated_boar"))
          parts
              .getChild("head")
              .addOrReplaceChild(
                  "horns",
                  CubeListBuilder.create()
                      .texOffs(80, 24)
                      .addBox(-6, -8, -2, 2, 7, 2)
                      .addBox(4, -8, -2, 2, 7, 2),
                  PartPose.ZERO);
        if (shape.equals("magma"))
          parts.addOrReplaceChild(
              "tail",
              CubeListBuilder.create()
                  .texOffs(0, 80)
                  .addBox(-2, -8, 8, 4, 12, 4)
                  .addBox(-2, -11, 3, 4, 4, 9),
              PartPose.offset(0, 12, 0));
        if (shape.equals("spitter"))
          parts.addOrReplaceChild(
              "sacs",
              CubeListBuilder.create().texOffs(0, 80).addBox(-5, -5, -7, 10, 6, 16),
              PartPose.offset(0, 10, 0));
      }
      for (int i = 0; i < limbs; i++) {
        boolean arm = citizen && i >= 2;
        float side = i % 2 == 0 ? -1 : 1;
        parts.addOrReplaceChild(
            "limb" + i,
            CubeListBuilder.create()
                .texOffs(70, 50)
                .addBox(-2, 0, -2, 4, citizen ? 12 : hunter ? 8 : 6, 4),
            PartPose.offsetAndRotation(
                side * (citizen ? arm ? 6 : 2 : hunter ? 8 : 4),
                citizen ? arm ? 0 : 12 : 18,
                citizen ? 0 : hunter ? (i / 2 - 1.5F) * 5 : i < 2 ? -6 : 6,
                0,
                0,
                hunter ? -side * .65F : 0));
      }
      root = LayerDefinition.create(mesh, 128, 128).bakeRoot();
      head = root.getChild("head");
    }

    @Override
    public void setupAnim(T mob, float swing, float amount, float age, float yaw, float pitch) {
      head.yRot = yaw * Mth.DEG_TO_RAD;
      head.xRot = pitch * Mth.DEG_TO_RAD;
      for (int i = 0; i < limbs; i++)
        root.getChild("limb" + i).xRot = Mth.cos(swing * .666F + i % 2 * Mth.PI) * amount * .8F;
    }

    @Override
    public void renderToBuffer(
        PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
      pose.pushPose();
      if (young && !citizen) {
        pose.scale(.5F, .5F, .5F);
        pose.translate(0, 1.5, 0);
      }
      root.render(pose, buffer, light, overlay, color);
      pose.popPose();
    }
  }
}
