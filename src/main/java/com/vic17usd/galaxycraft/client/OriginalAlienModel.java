package com.vic17usd.galaxycraft.client;

import com.mojang.blaze3d.vertex.*;
import com.vic17usd.galaxycraft.morphology.*;
import java.util.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Geometría escrita desde cero: once esqueletos, articulaciones jerárquicas y UV de 256 píxeles. No
 * importa capas, mallas, animaciones ni modelos de criaturas vanilla.
 */
public final class OriginalAlienModel<T extends Mob> extends EntityModel<T> {
  private final ModelPart root;
  private final CreatureDesign design;
  private final List<ModelPart> legs = new ArrayList<>(),
      knees = new ArrayList<>(),
      ankles = new ArrayList<>(),
      segments = new ArrayList<>(),
      jaws = new ArrayList<>(),
      wings = new ArrayList<>();
  private final ModelPart body, head;

  public OriginalAlienModel(CreatureDesign design, int variant) {
    this.design = design;
    root = layer(design, variant).bakeRoot();
    body = root.getChild("body");
    head = body.getChild("head");
    for (var part : root.getAllParts().toList()) {
      // Los nombres se consultan en cada padre, sin reflexión ni búsqueda durante el renderizado.
      for (int i = 0; i < 16; i++) {
        if (part.hasChild("leg" + i)) {
          var leg = part.getChild("leg" + i);
          legs.add(leg);
          knees.add(leg.getChild("knee"));
          ankles.add(leg.getChild("knee").getChild("ankle"));
        }
        if (part.hasChild("segment" + i)) segments.add(part.getChild("segment" + i));
      }
    }
    for (int i = 0; i < 4; i++) if (head.hasChild("jaw" + i)) jaws.add(head.getChild("jaw" + i));
    for (int i = 0; i < 2; i++) if (body.hasChild("wing" + i)) wings.add(body.getChild("wing" + i));
  }

  private static PartDefinition box(
      PartDefinition parent,
      String name,
      int material,
      float x,
      float y,
      float z,
      float w,
      float h,
      float d,
      float px,
      float py,
      float pz,
      float rx,
      float ry,
      float rz) {
    return parent.addOrReplaceChild(
        name,
        CubeListBuilder.create()
            .texOffs(material % 2 * 128, material / 2 * 64)
            .addBox(x, y, z, w, h, d),
        PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
  }

  private static PartDefinition node(
      PartDefinition parent, String name, float x, float y, float z) {
    return parent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, y, z));
  }

  /** Cada extremidad tiene cadera, rodilla inversa, tobillo y dedos; no son patas rígidas. */
  private static void leg(
      PartDefinition parent,
      int i,
      float x,
      float y,
      float z,
      float length,
      float thickness,
      float spread) {
    var hip =
        box(
            parent,
            "leg" + i,
            0,
            -thickness / 2,
            0,
            -thickness / 2,
            thickness,
            length,
            thickness,
            x,
            y,
            z,
            0,
            0,
            spread);
    var knee =
        box(
            hip,
            "knee",
            2,
            -thickness * .4F,
            0,
            -thickness * .4F,
            thickness * .8F,
            length * .8F,
            thickness * .8F,
            0,
            length,
            0,
            -.45F,
            0,
            -spread * .7F);
    var ankle =
        box(
            knee,
            "ankle",
            3,
            -thickness * .3F,
            0,
            -thickness * .3F,
            thickness * .6F,
            length * .35F,
            thickness * .6F,
            0,
            length * .8F,
            0,
            .4F,
            0,
            0);
    for (int j = 0; j < 2; j++)
      box(
          ankle,
          "toe" + j,
          2,
          -.4F,
          0,
          -2,
          .8F,
          .7F,
          3,
          j == 0 ? -thickness * .35F : thickness * .35F,
          length * .3F,
          0,
          0,
          j == 0 ? -.2F : .2F,
          0);
  }

  public static LayerDefinition layer(CreatureDesign design, int variant) {
    var mesh = new MeshDefinition();
    var root = mesh.getRoot();
    int v = Math.floorMod(variant, 4);
    var body = node(root, "body", 0, 12, 0);
    PartDefinition head;
    switch (design) {
      case WORM -> {
        head = box(body, "head", 2, -3, -3, -4, 6, 6, 4, 0, 6, -7, 0, 0, 0);
        PartDefinition chain = body;
        for (int i = 0; i < 12; i++) {
          float width = 5.6F - i * .25F;
          chain =
              box(
                  chain,
                  "segment" + i,
                  0,
                  -width / 2,
                  -2,
                  -1.2F,
                  width,
                  4,
                  2.4F,
                  0,
                  i == 0 ? 6 : 0,
                  i == 0 ? -4 : 1.9F,
                  0,
                  0,
                  0);
          for (int side = -1; side <= 1; side += 2)
            box(
                chain,
                "cilia" + side,
                2,
                0,
                0,
                -.4F,
                3,
                .5F,
                .8F,
                side * width / 2,
                1,
                0,
                0,
                0,
                side * .5F);
        }
      }
      case LUMEN -> {
        head = node(body, "head", 0, 1, -2);
        for (int i = 0; i < 5; i++)
          box(
              body,
              "segment" + i,
              i == 2 ? 1 : 4,
              -(7 - i * .7F),
              -1,
              -(7 - i * .7F),
              14 - i * 1.4F,
              2.2F,
              14 - i * 1.4F,
              0,
              -i * 1.5F,
              0,
              0,
              i * .32F,
              0);
        box(body, "nucleus", 1, -3, -4, -3, 6, 7, 6, 0, 0, 0, 0, .7F, .2F);
        for (int i = 0; i < 8; i++) {
          double angle = i * Math.PI / 4;
          leg(
              body,
              i,
              (float) Math.cos(angle) * 5,
              2,
              (float) Math.sin(angle) * 5,
              4,
              .9F,
              (float) Math.cos(angle) * .65F);
        }
      }
      case SAGE -> {
        box(body, "mantle", 2, -5, -10, -4, 10, 12, 8, 0, 0, 0, 0, 0, 0);
        box(body, "collar", 3, -7, -2, -6, 14, 3, 12, 0, -8, 0, 0, 0, 0);
        head = box(body, "head", 0, -6, -4, -4, 12, 6, 8, 0, -13, 0, 0, 0, 0);
        for (int i = 0; i < 3; i++) {
          double angle = i * Math.PI * 2 / 3;
          leg(
              body,
              i,
              (float) Math.cos(angle) * 4,
              1,
              (float) Math.sin(angle) * 4,
              6,
              1.8F,
              (float) Math.cos(angle) * .4F);
        }
        for (int i = 0; i < 4; i++) {
          float side = i % 2 == 0 ? -1 : 1;
          var arm =
              box(
                  body,
                  "segment" + i,
                  0,
                  -1,
                  0,
                  -1,
                  2,
                  5,
                  2,
                  side * 6,
                  -7 + i / 2 * 4,
                  0,
                  0,
                  0,
                  -side * .65F);
          var hand = box(arm, "forearm", 2, -.8F, 0, -.8F, 1.6F, 5, 1.6F, 0, 5, 0, -.8F, 0, 0);
          for (int j = 0; j < 3; j++)
            box(
                hand,
                "finger" + j,
                0,
                -.3F,
                0,
                -.3F,
                .6F,
                2,
                .6F,
                j - 1,
                5,
                0,
                0,
                0,
                (j - 1) * .25F);
        }
      }
      case RAPTOR -> {
        box(body, "sternum", 0, -3, -6, -5, 6, 11, 10, 0, -2, 0, -.35F, 0, 0);
        head = box(body, "head", 2, -2, -2, -5, 4, 4, 8, 0, -8, -5, .2F, 0, 0);
        for (int i = 0; i < 2; i++) {
          float side = i == 0 ? -1 : 1;
          leg(body, i, side * 3, 2, 3, 5, 1.8F, -side * .2F);
          var wing = node(body, "wing" + i, side * 3, -5, 0);
          for (int j = 0; j < 4; j++) {
            var rib =
                box(
                    wing,
                    "rib" + j,
                    2,
                    0,
                    -.4F,
                    -.4F,
                    12 + j * 2,
                    .8F,
                    .8F,
                    0,
                    0,
                    j * 1.2F,
                    0,
                    side * (j * .25F - .15F),
                    i == 0 ? 3.05F : .09F);
            box(rib, "membrane", 4, 1, 0, 0, 10 + j * 2, .15F, 4, 0, 0, 0, 0, 0, 0);
          }
          box(wing, "tip", 0, 0, -1, -1, 19, 2, 2, 0, 0, 0, 0, 0, i == 0 ? 3.05F : .09F);
        }
        tail(body, 7, 8, 2);
      }
      case GUARDIAN -> {
        box(body, "reactor", 2, -8, -12, -5, 16, 17, 10, 0, 0, 0, 0, 0, 0);
        for (int i = 0; i < 4; i++)
          box(body, "rib" + i, 3, -9, -1, -6, 18, 2, 12, 0, -10 + i * 4, 0, 0, 0, 0);
        head = box(body, "head", 3, -5, -3, -4, 10, 6, 8, 0, -15, 0, 0, 0, 0);
        for (int i = 0; i < 4; i++)
          leg(body, i, i % 2 == 0 ? -6 : 6, 4, i < 2 ? -3 : 4, 4, 3, i % 2 == 0 ? .35F : -.35F);
        for (int i = 0; i < 2; i++) {
          float side = i == 0 ? -1 : 1;
          var arm =
              box(
                  body,
                  "segment" + i,
                  2,
                  -2,
                  -1,
                  -2,
                  4,
                  9,
                  4,
                  side * 11,
                  -10,
                  0,
                  0,
                  0,
                  side * .15F);
          var cannon = box(arm, "barrel", 3, -3, -3, -10, 6, 6, 12, 0, 9, 0, 0, 0, 0);
          for (int j = 0; j < 3; j++)
            box(cannon, "coil" + j, 1, -3.5F, -3.5F, -1, 7, 7, 1, 0, 0, -3 - j * 2, 0, 0, 0);
        }
      }
      case QUEEN -> {
        box(body, "thorax", 0, -8, -6, -9, 16, 12, 18, 0, 0, 0, 0, 0, 0);
        head = box(body, "head", 2, -5, -4, -6, 10, 8, 9, 0, -4, -12, 0, 0, 0);
        for (int i = 0; i < 12; i++)
          leg(
              body,
              i,
              (i % 2 == 0 ? -1 : 1) * 8,
              1,
              (i / 2 - 2.5F) * 3.1F,
              6,
              2,
              i % 2 == 0 ? .85F : -.85F);
        for (int i = 0; i < 7; i++)
          box(
              head,
              "crown" + i,
              2,
              -.7F,
              -8,
              -.7F,
              1.4F,
              8 + i % 3,
              1.4F,
              (i - 3) * 2,
              -3,
              0,
              0,
              0,
              (i - 3) * .17F);
        tail(body, 8, 10, 4);
      }
      default -> {
        boolean cargo = design == CreatureDesign.CARGO,
            magma = design == CreatureDesign.MAGMA,
            spitter = design == CreatureDesign.SPITTER,
            lancer = design == CreatureDesign.LANCER;
        float length = cargo ? 22 : spitter ? 20 : 16, width = magma ? 14 : cargo ? 12 : 10;
        box(body, "thorax", 0, -width / 2, -4, -length / 2, width, 8, length, 0, 0, 0, 0, 0, 0);
        head = box(body, "head", 2, -3, -2, -4, 6, 5, 7, 0, -1, -length / 2 - 2, 0, 0, 0);
        int count = magma ? 4 : lancer ? 8 : 6;
        for (int i = 0; i < count; i++)
          leg(
              body,
              i,
              (i % 2 == 0 ? -1 : 1) * width / 2,
              3,
              (i / 2 - (count / 2 - 1) * .5F) * 5,
              4,
              magma ? 3 : 1.5F,
              i % 2 == 0 ? .65F : -.65F);
        for (int i = 0; i < 5 + v; i++) {
          var plate =
              box(
                  body,
                  "segment" + i,
                  magma ? 3 : 2,
                  -width * .45F,
                  -2,
                  -1.6F,
                  width * .9F,
                  3,
                  3.2F,
                  0,
                  -4,
                  -length / 2 + 2 + i * (length - 4) / (4 + v),
                  -.15F,
                  0,
                  0);
          if (lancer || magma)
            box(
                plate, "spine", magma ? 1 : 2, -.8F, -5, -.8F, 1.6F, 5 + (i + v) % 3, 1.6F, 0, 0, 0,
                -.35F, 0, .25F);
        }
        if (spitter)
          for (int i = 0; i < 4; i++)
            box(
                body,
                "gland" + i,
                1,
                -2.5F,
                -3,
                -3,
                5,
                6,
                6,
                i % 2 == 0 ? -5 : 5,
                -4,
                (i / 2) * 8 - 4,
                0,
                0,
                0);
        if (cargo)
          for (int i = 0; i < 2; i++)
            box(
                body,
                "cargo_sail" + i,
                3,
                -1,
                -8,
                -7,
                2,
                8,
                14,
                i == 0 ? -4 : 4,
                -4,
                2,
                0,
                0,
                i == 0 ? -.2F : .2F);
        if (magma || lancer) tail(body, 6, 8, magma ? 2.5F : 1.2F);
      }
    }
    // Órganos sensoriales individuales y mandíbulas articuladas, no una cara de animal vanilla.
    int eyes = design == CreatureDesign.SAGE ? 5 : design == CreatureDesign.QUEEN ? 6 : 3;
    for (int i = 0; i < eyes; i++) {
      var stalk =
          box(
              head,
              "sensor" + i,
              0,
              -.3F,
              -3,
              -.3F,
              .6F,
              3,
              .6F,
              (i - (eyes - 1) * .5F) * 1.5F,
              -2,
              -3,
              0,
              0,
              (i - (eyes - 1) * .5F) * .1F);
      box(stalk, "eye", 1, -.7F, -1, -.8F, 1.4F, 1.4F, 1.4F, 0, -3, 0, 0, 0, 0);
    }
    for (int i = 0; i < 4; i++)
      box(
          head,
          "jaw" + i,
          2,
          -.5F,
          -.5F,
          -3,
          1,
          1,
          4,
          (i % 2 == 0 ? -1 : 1) * 2,
          i / 2,
          -3,
          0,
          (i % 2 == 0 ? -1 : 1) * .25F,
          0);
    return LayerDefinition.create(mesh, 256, 256);
  }

  private static void tail(PartDefinition body, int count, float z, float thickness) {
    var current = body;
    for (int i = 0; i < count; i++) {
      current =
          box(
              current,
              "segment" + (8 + i),
              i % 2 == 0 ? 0 : 2,
              -thickness / 2,
              -thickness / 2,
              0,
              thickness,
              thickness,
              3.5F,
              0,
              i == 0 ? -1 : 0,
              i == 0 ? z : 3,
              -.12F,
              0,
              0);
      thickness *= .9F;
    }
  }

  public int partCount() {
    return (int) root.getAllParts().count();
  }

  @Override
  public void setupAnim(T mob, float swing, float amount, float age, float yaw, float pitch) {
    root.getAllParts().forEach(ModelPart::resetPose);
    var pose = mob.getData(ModMorphology.POSE);
    double time = mob.level().getGameTime() + age - mob.tickCount;
    float pulse = pose.envelope(time), phase = pose.progress(time) * Mth.PI;
    head.yRot += yaw * Mth.DEG_TO_RAD * .65F;
    head.xRot += pitch * Mth.DEG_TO_RAD * .55F;
    body.y +=
        (float) Math.sin(age * .08 + mob.getId()) * (design == CreatureDesign.LUMEN ? 1.2F : .18F);
    for (int i = 0; i < legs.size(); i++) {
      float step = swing * .7F + i / 2 * 1.8F + i % 2 * Mth.PI,
          walk = amount * (float) Math.sin(step);
      legs.get(i).xRot += walk * .65F;
      knees.get(i).xRot += Math.max(0, walk) * .9F;
      ankles.get(i).xRot -= Math.max(0, walk) * .7F;
      if (design == CreatureDesign.LUMEN) {
        legs.get(i).zRot += Mth.sin(age * .09F + i) * .2F;
        knees.get(i).xRot += Mth.cos(age * .1F + i) * .3F;
      }
      if (pose.action() == AlienPose.ATTACK) {
        legs.get(i).xRot -= pulse * (i < 2 ? 1 : .2F);
        knees.get(i).zRot += pulse * (i % 2 == 0 ? .2F : -.2F);
      }
    }
    for (int i = 0; i < segments.size(); i++) {
      var segment = segments.get(i);
      segment.yRot += Mth.sin(age * .09F - i * .5F) * .075F;
      if (design == CreatureDesign.WORM) {
        segment.y += Mth.sin(age * .22F - i * .7F) * .15F;
        segment.xRot += Mth.sin(age * .22F - i * .7F) * .1F;
      }
      if (design == CreatureDesign.LUMEN) {
        float s = 1 + Mth.sin(age * .09F - i * .6F) * .05F;
        segment.xScale = s;
        segment.zScale = s;
      }
      if (design == CreatureDesign.SAGE && pose.action() == AlienPose.TRADE)
        segment.xRot -= pulse * .9F;
      if (design == CreatureDesign.GUARDIAN && pose.action() == AlienPose.SPIT)
        segment.xRot -= pulse * .75F;
    }
    for (int i = 0; i < jaws.size(); i++)
      jaws.get(i).yRot += (i % 2 == 0 ? -1 : 1) * (pulse * .7F + Mth.sin(age * .12F + i) * .035F);
    for (int i = 0; i < wings.size(); i++) {
      float side = i == 0 ? -1 : 1;
      wings.get(i).zRot =
          side * (mob.onGround() ? .5F + Mth.sin(age * .07F) * .04F : Mth.sin(age * .6F) * .4F);
      if (pose.action() == AlienPose.GLIDE) wings.get(i).zRot *= 1 - pulse * .8F;
    }
    if (pose.action() == AlienPose.BURROW) {
      body.y += pulse * 3;
      head.xRot += pulse * .7F;
    }
    if (pose.action() == AlienPose.ATTACK || pose.action() == AlienPose.SPIT) {
      head.xRot -= Mth.sin(phase) * .5F;
      body.z -= pulse * .7F;
    }
    if (pose.action() == AlienPose.ROAR) {
      head.xRot -= pulse * .7F;
      body.xRot -= pulse * .1F;
    }
    if (pose.action() == AlienPose.FEED) head.xRot += pulse * .8F;
  }

  @Override
  public void renderToBuffer(
      PoseStack stack, VertexConsumer vertices, int light, int overlay, int color) {
    stack.pushPose();
    if (young) {
      stack.scale(.5F, .5F, .5F);
      stack.translate(0, 1.5, 0);
    }
    root.render(stack, vertices, light, overlay, color);
    stack.popPose();
  }
}
