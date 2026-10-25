package com.vic17usd.galaxycraft.combat;

import com.vic17usd.galaxycraft.morphology.*;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Hostil sin combustión solar ni conversión acuática; las lunas heladas ralentizan con sus golpes.
 */
public final class PlanetaryRaider extends Monster {
  public PlanetaryRaider(EntityType<? extends Monster> type, Level level) {
    super(type, level);
  }

  public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder
      createAttributes() {
    return Monster.createMonsterAttributes();
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, false));
    goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, .8));
    goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12));
    targetSelector.addGoal(1, new HurtByTargetGoal(this));
    targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
  }

  /** Planeo con colisión normal, no vuelo de navegación ni teletransporte a través de paredes. */
  @Override
  public void aiStep() {
    super.aiStep();
    if (!level().isClientSide()) {
      if (onGround()
          && getTarget() != null
          && hasLineOfSight(getTarget())
          && distanceToSqr(getTarget()) < 144
          && tickCount % 65 == 0) {
        var towards = getTarget().position().subtract(position()).normalize();
        setDeltaMovement(towards.x * .6, .6, towards.z * .6);
        hasImpulse = true;
        MorphologyNetworking.animate(this, AlienPose.GLIDE, 40);
      }
      if (!onGround() && getDeltaMovement().y < -.12)
        setDeltaMovement(getDeltaMovement().x, -.12, getDeltaMovement().z);
    }
  }

  @Override
  public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
    return false;
  }

  @Override
  public boolean doHurtTarget(Entity target) {
    MorphologyNetworking.animate(this, AlienPose.ATTACK, 18);
    boolean result = super.doHurtTarget(target);
    var body = SolarSystem.body(level());
    if (result && target instanceof LivingEntity living && body != null && body.tier() == 3)
      living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0), this);
    return result;
  }
}
