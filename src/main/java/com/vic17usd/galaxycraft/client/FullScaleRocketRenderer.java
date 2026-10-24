package com.vic17usd.galaxycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vic17usd.galaxycraft.blockentity.RocketBlockEntity;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Modelo de más de seis metros, con anillos, paneles, aletas, motores, toberas y encendido animado. */
public final class FullScaleRocketRenderer implements BlockEntityRenderer<RocketBlockEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ModPresentation.id("full_scale_rocket"), "main");
    private final ModelPart model;
    public FullScaleRocketRenderer(BlockEntityRendererProvider.Context context) { model = context.bakeLayer(LAYER); }
    public static LayerDefinition layer() {
        var mesh = new MeshDefinition(); var root = mesh.getRoot();
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-8, 12, -8, 16, 59, 16)
                .texOffs(0, 80).addBox(-9, 17, -9, 18, 3, 18).addBox(-9, 45, -9, 18, 2, 18)
                .addBox(-9, 68, -9, 18, 3, 18), PartPose.ZERO);
        for (int i = 0; i < 8; i++) root.addOrReplaceChild("panel" + i,
                CubeListBuilder.create().texOffs(70, 0).addBox(-4, 21, 7, 8, 45, 1), PartPose.rotation(0, i * (float)Math.PI / 4, 0));
        root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 100)
                .addBox(-7, 71, -7, 14, 9, 14).addBox(-5, 80, -5, 10, 8, 10)
                .addBox(-3, 88, -3, 6, 7, 6).addBox(-1, 95, -1, 2, 6, 2), PartPose.ZERO);
        root.addOrReplaceChild("window", CubeListBuilder.create().texOffs(106, 0).addBox(-4, 57, -8.6F, 8, 8, 1), PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            float angle = i * (float)Math.PI / 2;
            root.addOrReplaceChild("fin" + i, CubeListBuilder.create().texOffs(80, 52)
                    .addBox(7, 3, -1, 9, 12, 2).addBox(7, 15, -1, 5, 10, 2), PartPose.rotation(0, angle, 0));
            root.addOrReplaceChild("booster" + i, CubeListBuilder.create().texOffs(82, 82)
                    .addBox(-3, 5, 8, 6, 24, 6).addBox(-4, 1, 8, 8, 4, 7), PartPose.rotation(0, angle, 0));
        }
        root.addOrReplaceChild("engine", CubeListBuilder.create().texOffs(0, 80)
                .addBox(-6, 7, -6, 12, 5, 12).addBox(-7, 3, -7, 14, 4, 14).addBox(-8, 0, -8, 16, 3, 16), PartPose.ZERO);
        root.addOrReplaceChild("flame", CubeListBuilder.create().texOffs(100, 110).addBox(-3, -10, -3, 6, 10, 6), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }
    @Override public void render(RocketBlockEntity rocket, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        long start = rocket.launchStarted();
        float progress = start < 0 || rocket.getLevel() == null ? 0 : Math.clamp((rocket.getLevel().getGameTime() - start + partialTick) / RocketBlockEntity.LAUNCH_TICKS, 0, 1);
        pose.pushPose(); pose.translate(.5, .6 * progress * progress, .5);
        var flame = model.getChild("flame"); flame.visible = false;
        var texture = ModPresentation.id("textures/entity/rocket_tier" + rocket.tier() + ".png");
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        if (start >= 0) {
            flame.visible = true;
            flame.yScale = .4F + progress * 1.6F + (float)Math.sin((rocket.getLevel().getGameTime() + partialTick) * 2) * .12F;
            flame.render(pose, buffers.getBuffer(RenderType.entityTranslucent(texture)), 0xF000F0, overlay);
        }
        pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(RocketBlockEntity rocket) { return true; }
    @Override public int getViewDistance() { return 192; }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(RocketBlockEntity rocket) {
        var pos = rocket.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 8, pos.getZ() + 2);
    }
}
