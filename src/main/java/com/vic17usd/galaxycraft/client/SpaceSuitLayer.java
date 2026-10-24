package com.vic17usd.galaxycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vic17usd.galaxycraft.equipment.SpaceEquipment;
import com.vic17usd.galaxycraft.registry.ModPresentation;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Capa de traje superpuesta: no sustituye objetos ni modifica la armadura del jugador al renderizar. */
public final class SpaceSuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final HumanoidModel<AbstractClientPlayer> suit = new HumanoidModel<>(LayerDefinition.create(
            HumanoidModel.createMesh(new CubeDeformation(1.25F), 0), 64, 32).bakeRoot());
    private final HumanoidModel<AbstractClientPlayer> inner = new HumanoidModel<>(LayerDefinition.create(
            HumanoidModel.createMesh(new CubeDeformation(1.15F), 0), 64, 32).bakeRoot());
    private final ModelPart tanks;
    public SpaceSuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent); var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("left", CubeListBuilder.create().texOffs(16,16).addBox(-5, 0, 4, 4, 12, 4), PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("right", CubeListBuilder.create().texOffs(16,16).addBox(1, 0, 4, 4, 12, 4), PartPose.ZERO);
        tanks = LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float limbSwing, float limbAmount, float partialTick, float age, float yaw, float pitch) {
        if (player.isInvisible() || player.isSpectator()) return;
        var equipment = SpaceEquipment.inventory(player);
        getParentModel().copyPropertiesTo(suit); getParentModel().copyPropertiesTo(inner);
        for (int i = 0; i < 4; i++) {
            if (equipment.getStackInSlot(i).isEmpty()) continue;
            var model = i == 2 ? inner : suit;
            model.setAllVisible(false);
            if (i == 0) { model.head.visible = true; model.hat.visible = true; }
            if (i == 1) { model.body.visible = true; model.leftArm.visible = true; model.rightArm.visible = true; }
            if (i == 2) { model.body.visible = true; model.leftLeg.visible = true; model.rightLeg.visible = true; }
            if (i == 3) { model.leftLeg.visible = true; model.rightLeg.visible = true; }
            var texture = ModPresentation.id("textures/models/armor/space_suit_layer_" + (i == 2 ? 2 : 1) + ".png");
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
        }
        if (!equipment.getStackInSlot(4).isEmpty() || !equipment.getStackInSlot(5).isEmpty()) {
            pose.pushPose(); getParentModel().body.translateAndRotate(pose);
            tanks.getChild("left").visible = !equipment.getStackInSlot(4).isEmpty();
            tanks.getChild("right").visible = !equipment.getStackInSlot(5).isEmpty();
            tanks.render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(ModPresentation.id("textures/models/armor/space_suit_layer_1.png"))), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
