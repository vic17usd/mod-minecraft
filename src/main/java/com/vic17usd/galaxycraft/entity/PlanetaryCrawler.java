package com.vic17usd.galaxycraft.entity;

import com.vic17usd.galaxycraft.morphology.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Gusano de regolito: embestida rasante, sin infestar ni destruir bloques del mundo. */
public final class PlanetaryCrawler extends Monster {
  public PlanetaryCrawler(EntityType<? extends Monster> type, Level level) {
    super(type, level);
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
    goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, .7));
    targetSelector.addGoal(1, new HurtByTargetGoal(this));
    targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
  }

  @Override
  public void aiStep() {
    super.aiStep();
    if (!level().isClientSide()
        && onGround()
        && getTarget() != null
        && hasLineOfSight(getTarget())
        && distanceToSqr(getTarget()) < 64
        && tickCount % 80 == 0) {
      var direction = getTarget().position().subtract(position()).normalize();
      setDeltaMovement(direction.x * .4, .1, direction.z * .4);
      hasImpulse = true;
      MorphologyNetworking.animate(this, AlienPose.BURROW, 24);
    }
  }

  @Override
  public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
    MorphologyNetworking.animate(this, AlienPose.ATTACK, 14);
    return super.doHurtTarget(entity);
  }
}
