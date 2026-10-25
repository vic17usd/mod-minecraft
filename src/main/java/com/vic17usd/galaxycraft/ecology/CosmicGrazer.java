package com.vic17usd.galaxycraft.ecology;

import com.vic17usd.galaxycraft.biosphere.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;

/** Tres especies por planeta, reproducción restringida a su tipo y alimento nativo. */
public final class CosmicGrazer extends Animal {
  private int woolCooldown;

  public CosmicGrazer(EntityType<? extends Animal> type, Level level) {
    super(type, level);
  }

  public String body() {
    return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath().split("_")[0];
  }

  public String species() {
    return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath().substring(body().length() + 1);
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(1, new PanicGoal(this, 1.4));
    goalSelector.addGoal(2, new BreedGoal(this, 1));
    goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Monster.class, 8, 1, 1.4));
    goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
    goalSelector.addGoal(5, new TemptGoal(this, 1.1, this::isFood, false));
    goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, .8));
    goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6));
    goalSelector.addGoal(8, new RandomLookAroundGoal(this));
  }

  public static boolean canSpawn(
      EntityType<CosmicGrazer> type,
      ServerLevelAccessor level,
      MobSpawnType reason,
      BlockPos pos,
      RandomSource random) {
    String body = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath().split("_")[0];
    return level.getLevel().dimension().equals(LivingProfiles.get(body).celestial().dimension())
        && AlienPlantBlock.soil(body, level.getBlockState(pos.below()));
  }

  @Override
  public boolean isFood(ItemStack stack) {
    return stack.is(ModBiosphere.HARVEST.get(body()).get());
  }

  @Override
  public boolean canMate(Animal other) {
    return other.getType() == getType() && super.canMate(other);
  }

  @Override
  public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
    return other.getType() == getType() ? (AgeableMob) getType().create(level) : null;
  }

  @Override
  public void aiStep() {
    super.aiStep();
    if (!level().isClientSide() && woolCooldown > 0) woolCooldown--;
  }

  @Override
  public InteractionResult mobInteract(Player player, InteractionHand hand) {
    if (species().equals("lumen_sheep")
        && player.getItemInHand(hand).is(Items.SHEARS)
        && !isBaby()
        && woolCooldown == 0) {
      if (!level().isClientSide()) {
        woolCooldown = 600;
        spawnAtLocation(new ItemStack(ModBiosphere.FIBER.get(), 2));
        player.getItemInHand(hand).hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
      }
      return InteractionResult.sidedSuccess(level().isClientSide());
    }
    return super.mobInteract(player, hand);
  }

  @Override
  public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putInt("WoolCooldown", woolCooldown);
  }

  @Override
  public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    woolCooldown = Math.clamp(tag.getInt("WoolCooldown"), 0, 600);
  }
}
