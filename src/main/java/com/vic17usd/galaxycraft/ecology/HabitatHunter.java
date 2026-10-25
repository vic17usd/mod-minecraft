package com.vic17usd.galaxycraft.ecology;

import com.vic17usd.galaxycraft.biosphere.*;
import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Cazador por hábitat: saltos cristalinos, escupidor de carbono o pinzas ígneas. */
public final class HabitatHunter extends Monster implements RangedAttackMob {
  private int shot;

  public HabitatHunter(EntityType<? extends Monster> type, Level level) {
    super(type, level);
    xpReward = 12;
  }

  public String body() {
    return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath().split("_")[0];
  }

  public String style() {
    return LivingProfiles.get(body()).hunter();
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(
        1,
        new MeleeAttackGoal(this, 1.15, false) {
          @Override
          public boolean canUse() {
            return getTarget() != null
                && (!style().equals("spitter") || distanceToSqr(getTarget()) < 36)
                && super.canUse();
          }

          @Override
          public boolean canContinueToUse() {
            return getTarget() != null
                && (!style().equals("spitter") || distanceToSqr(getTarget()) < 49)
                && super.canContinueToUse();
          }
        });
    goalSelector.addGoal(
        2,
        new RangedAttackGoal(this, 1, 70, 18) {
          @Override
          public boolean canUse() {
            return style().equals("spitter") && super.canUse();
          }

          @Override
          public boolean canContinueToUse() {
            return style().equals("spitter") && super.canContinueToUse();
          }
        });
    goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, .8));
    goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12));
    targetSelector.addGoal(1, new HurtByTargetGoal(this));
    targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
  }

  @Override
  public void aiStep() {
    super.aiStep();
    if (shot > 0) shot--;
    if (!level().isClientSide()
        && style().equals("pouncer")
        && getTarget() != null
        && onGround()
        && tickCount % 50 == 0
        && distanceToSqr(getTarget()) < 100) {
      var towards = getTarget().position().subtract(position()).normalize();
      setDeltaMovement(towards.x * .65, .45, towards.z * .65);
      hasImpulse = true;
      com.vic17usd.galaxycraft.morphology.MorphologyNetworking.animate(
          this, com.vic17usd.galaxycraft.morphology.AlienPose.ATTACK, 18);
    }
  }

  @Override
  public void performRangedAttack(LivingEntity target, float strength) {
    if (level().isClientSide() || !style().equals("spitter") || shot > 0 || !hasLineOfSight(target))
      return;
    shot = 60;
    com.vic17usd.galaxycraft.morphology.MorphologyNetworking.animate(
        this, com.vic17usd.galaxycraft.morphology.AlienPose.SPIT, 22);
    var bolt = ModFrontiers.PLASMA_BOLT.get().create(level());
    if (bolt == null) return;
    bolt.setOwner(this);
    bolt.setPos(getX(), getY() + .7, getZ());
    var aim = target.getEyePosition().subtract(bolt.position());
    bolt.shoot(aim.x, aim.y, aim.z, 1.1F, 3);
    level().addFreshEntity(bolt);
  }

  @Override
  public boolean doHurtTarget(Entity target) {
    com.vic17usd.galaxycraft.morphology.MorphologyNetworking.animate(
        this, com.vic17usd.galaxycraft.morphology.AlienPose.ATTACK, 18);
    boolean hit = super.doHurtTarget(target);
    if (hit && target instanceof LivingEntity living) {
      if (style().equals("magma")) living.igniteForSeconds(3);
      else if (style().equals("pouncer"))
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60), this);
    }
    return hit;
  }
}
