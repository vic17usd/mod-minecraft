package com.vic17usd.galaxycraft.combat;

import com.vic17usd.galaxycraft.frontier.ModFrontiers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Dos encuentros con barra de jefe, fase de furia, ataques de plasma y combate cercano. */
public final class PlanetBoss extends Monster implements RangedAttackMob {
  private final ServerBossEvent bossBar =
      new ServerBossEvent(
          getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
  private BlockPos anchor;
  private int shotCooldown;
  private String encounterBody = "";

  /** El encuentro utiliza un arquetipo existente, con nombre, dificultad y botín planetarios. */
  public void expedition(String body) {
    var profile = com.vic17usd.galaxycraft.biosphere.LivingProfiles.get(body);
    encounterBody = body;
    setCustomName(
        net.minecraft.network.chat.Component.translatable("boss.galaxycraft.expedition." + body));
    getAttribute(Attributes.MAX_HEALTH).setBaseValue(260 + profile.celestial().tier() * 80);
    getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7 + profile.celestial().tier() * 2);
    setHealth(getMaxHealth());
  }

  @Override
  protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>
      getDefaultLootTable() {
    return encounterBody.isEmpty()
        ? super.getDefaultLootTable()
        : net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.LOOT_TABLE,
            com.vic17usd.galaxycraft.registry.ModPresentation.id(
                "entities/expedition_wardens/" + encounterBody));
  }

  public PlanetBoss(EntityType<? extends Monster> type, Level level) {
    super(type, level);
    xpReward = 150;
    setPersistenceRequired();
  }

  public boolean queen() {
    return getType() == ModFrontiers.QUEEN.get();
  }

  public static AttributeSupplier.Builder attributes(boolean queen) {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, queen ? 360 : 300)
        .add(Attributes.ATTACK_DAMAGE, queen ? 10 : 8)
        .add(Attributes.ARMOR, queen ? 6 : 12)
        .add(Attributes.MOVEMENT_SPEED, queen ? .28 : .23)
        .add(Attributes.FOLLOW_RANGE, 32)
        .add(Attributes.KNOCKBACK_RESISTANCE, .85);
  }

  public void anchor(BlockPos pos) {
    anchor = pos.immutable();
    restrictTo(anchor, 20);
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(
        1,
        new MeleeAttackGoal(this, 1.1, false) {
          @Override
          public boolean canUse() {
            return getTarget() != null && distanceToSqr(getTarget()) < 36 && super.canUse();
          }

          @Override
          public boolean canContinueToUse() {
            return getTarget() != null
                && distanceToSqr(getTarget()) < 49
                && super.canContinueToUse();
          }
        });
    goalSelector.addGoal(2, new RangedAttackGoal(this, 1, 60, 22));
    goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1));
    goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16));
    goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    targetSelector.addGoal(1, new HurtByTargetGoal(this));
    targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
  }

  @Override
  public void aiStep() {
    super.aiStep();
    if (!level().isClientSide()) {
      bossBar.setName(getDisplayName());
      bossBar.setProgress(getHealth() / getMaxHealth());
      bossBar.setColor(
          queen()
              ? BossEvent.BossBarColor.PURPLE
              : getHealth() < getMaxHealth() / 2
                  ? BossEvent.BossBarColor.RED
                  : BossEvent.BossBarColor.BLUE);
      if (shotCooldown > 0) shotCooldown--;
      var target = getTarget();
      if (getHealth() < getMaxHealth() / 2
          && target != null
          && hasLineOfSight(target)
          && distanceToSqr(target) > 36
          && tickCount % 40 == 0) performRangedAttack(target, 1);
      if (target != null && distanceToSqr(target) < 25 && tickCount % 100 == 0) {
        com.vic17usd.galaxycraft.morphology.MorphologyNetworking.animate(
            this, com.vic17usd.galaxycraft.morphology.AlienPose.ROAR, 30);
        for (var living :
            level()
                .getEntitiesOfClass(
                    Player.class,
                    getBoundingBox().inflate(4),
                    player -> !player.isCreative() && !player.isSpectator())) {
          living.hurt(damageSources().mobAttack(this), queen() ? 6 : 4);
          living.knockback(.8, getX() - living.getX(), getZ() - living.getZ());
        }
      }
    }
  }

  @Override
  public void performRangedAttack(LivingEntity target, float distance) {
    if (level().isClientSide() || shotCooldown > 0) return;
    shotCooldown = getHealth() < getMaxHealth() / 2 ? 30 : 55;
    com.vic17usd.galaxycraft.morphology.MorphologyNetworking.animate(
        this, com.vic17usd.galaxycraft.morphology.AlienPose.SPIT, 24);
    var bolt = ModFrontiers.PLASMA_BOLT.get().create(level());
    if (bolt == null) return;
    bolt.setOwner(this);
    bolt.setPos(getX(), getY() + 1.7, getZ());
    var aim = target.getEyePosition().subtract(bolt.position());
    bolt.shoot(aim.x, aim.y, aim.z, queen() ? 1.15F : 1.4F, 2);
    level().addFreshEntity(bolt);
    level()
        .playSound(
            null,
            blockPosition(),
            net.minecraft.sounds.SoundEvents.BLAZE_SHOOT,
            net.minecraft.sounds.SoundSource.HOSTILE,
            .9F,
            queen() ? .7F : 1.2F);
  }

  @Override
  public boolean doHurtTarget(Entity entity) {
    com.vic17usd.galaxycraft.morphology.MorphologyNetworking.animate(
        this, com.vic17usd.galaxycraft.morphology.AlienPose.ATTACK, 22);
    return super.doHurtTarget(entity);
  }

  @Override
  public void startSeenByPlayer(ServerPlayer player) {
    super.startSeenByPlayer(player);
    bossBar.addPlayer(player);
  }

  @Override
  public void stopSeenByPlayer(ServerPlayer player) {
    super.stopSeenByPlayer(player);
    bossBar.removePlayer(player);
  }

  @Override
  public void die(net.minecraft.world.damagesource.DamageSource source) {
    super.die(source);
    if (!level().isClientSide()
        && anchor != null
        && level().getBlockEntity(anchor) instanceof BossAnchor home) home.defeat(getUUID());
    bossBar.removeAllPlayers();
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    if (anchor != null) tag.putLong("BossAnchor", anchor.asLong());
    if (!encounterBody.isEmpty()) tag.putString("EncounterBody", encounterBody);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    if (tag.contains("BossAnchor")) anchor(BlockPos.of(tag.getLong("BossAnchor")));
    encounterBody = tag.getString("EncounterBody");
    if (!encounterBody.isEmpty())
      com.vic17usd.galaxycraft.biosphere.LivingProfiles.get(encounterBody);
  }
}
