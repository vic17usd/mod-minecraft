package com.vic17usd.galaxycraft.client;

import com.mojang.blaze3d.vertex.*;
import com.vic17usd.galaxycraft.combat.PlanetBoss;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dos geometrías originales: bípedo industrial y reina acorazada de ocho patas. */
public final class FrontierBossRenderer
    extends MobRenderer<PlanetBoss, FrontierBossRenderer.BossModel> {
  public FrontierBossRenderer(EntityRendererProvider.Context context, boolean queen) {
    super(context, new BossModel(queen), 1);
  }

  @Override
  public ResourceLocation getTextureLocation(PlanetBoss boss) {
    return ModPresentation.id(
        "textures/entity/" + (boss.queen() ? "martian_queen" : "lunar_guardian") + ".png");
  }

  @Override
  protected void scale(PlanetBoss boss, PoseStack pose, float partial) {
    pose.scale(1.45F, 1.45F, 1.45F);
  }

  public static final class BossModel extends EntityModel<PlanetBoss> {
    private final ModelPart root, head;
    private final boolean queen;
    private final int limbs;

    BossModel(boolean queen) {
      this.queen = queen;
      var mesh = new MeshDefinition();
      var parts = mesh.getRoot();
      limbs = queen ? 8 : 4;
      parts.addOrReplaceChild(
          "body",
          CubeListBuilder.create()
              .texOffs(0, 0)
              .addBox(
                  queen ? -8 : -10,
                  queen ? 5 : 3,
                  queen ? -10 : -6,
                  queen ? 16 : 20,
                  queen ? 14 : 11,
                  queen ? 20 : 12),
          PartPose.ZERO);
      parts.addOrReplaceChild(
          "head",
          CubeListBuilder.create()
              .texOffs(0, 50)
              .addBox(-6, -7, -6, 12, 8, 12)
              .texOffs(52, 50)
              .addBox(-4, -3, -8, 8, 3, 3),
          PartPose.offset(0, queen ? 12 : 4, queen ? -11 : 0));
      for (int i = 0; i < limbs; i++) {
        float x = i % 2 == 0 ? -1 : 1;
        boolean arm = !queen && i >= 2;
        parts.addOrReplaceChild(
            "limb" + i,
            CubeListBuilder.create()
                .texOffs(75, 0)
                .addBox(queen ? -2 : -3, 0, -3, queen ? 4 : 6, queen ? 13 : arm ? 15 : 12, 6),
            PartPose.offsetAndRotation(
                x * (queen ? 9 : arm ? 13 : 6),
                queen ? 13 : arm ? 5 : 12,
                queen ? (i / 2 - 1.5F) * 5 : 0,
                0,
                0,
                queen ? -x * .9F : 0));
      }
      if (queen)
        parts.addOrReplaceChild(
            "crest",
            CubeListBuilder.create().texOffs(0, 80).addBox(-2, -8, -12, 4, 10, 24),
            PartPose.offset(0, 6, 0));
      root = LayerDefinition.create(mesh, 128, 128).bakeRoot();
      head = root.getChild("head");
    }

    @Override
    public void setupAnim(
        PlanetBoss boss, float swing, float amount, float age, float yaw, float pitch) {
      head.yRot = yaw * Mth.DEG_TO_RAD;
      head.xRot = pitch * Mth.DEG_TO_RAD;
      for (int i = 0; i < limbs; i++)
        root.getChild("limb" + i).xRot = Mth.cos(swing * .65F + i % 2 * Mth.PI) * amount * .7F;
    }

    @Override
    public void renderToBuffer(
        PoseStack pose, VertexConsumer vertices, int light, int overlay, int color) {
      root.render(pose, vertices, light, overlay, color);
    }
  }
}
