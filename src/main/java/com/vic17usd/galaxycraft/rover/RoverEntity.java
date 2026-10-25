package com.vic17usd.galaxycraft.rover;

import com.vic17usd.galaxycraft.expansion.*;
import com.vic17usd.galaxycraft.quest.QuestService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Vehículo de un piloto, nueve muestras y una batería. Movimiento y gasto se calculan solo en
 * servidor.
 */
public final class RoverEntity extends Entity implements MenuProvider {
  private static final EntityDataAccessor<Integer> ENERGY =
      SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Float> HEALTH =
      SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.FLOAT);
  private float throttle, steer;
  private long inputAt;
  private double targetX, targetY, targetZ;
  private float targetYaw;
  private int lerpSteps;
  public float wheelAngle;
  public final ItemStackHandler inventory =
      new ItemStackHandler(10) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
          return slot < 9 || stack.is(ModExpansion.BATTERY.get());
        }

        @Override
        public int getSlotLimit(int slot) {
          return slot == 9 ? 1 : 64;
        }
      };
  public final IEnergyStorage battery =
      new IEnergyStorage() {
        public int receiveEnergy(int amount, boolean simulate) {
          int received = Math.clamp(amount, 0, Math.min(200, 6000 - energy()));
          if (!simulate) entityData.set(ENERGY, energy() + received);
          return received;
        }

        public int extractEnergy(int amount, boolean simulate) {
          return 0;
        }

        public int getEnergyStored() {
          return energy();
        }

        public int getMaxEnergyStored() {
          return 6000;
        }

        public boolean canExtract() {
          return false;
        }

        public boolean canReceive() {
          return true;
        }
      };

  public RoverEntity(EntityType<? extends RoverEntity> type, Level level) {
    super(type, level);
  }

  public int energy() {
    return entityData.get(ENERGY);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(ENERGY, 0);
    builder.define(HEALTH, 30F);
  }

  @Override
  public boolean isPickable() {
    return !isRemoved();
  }

  @Override
  public boolean isPushable() {
    return true;
  }

  @Override
  public boolean canBeCollidedWith() {
    return true;
  }

  @Override
  public float maxUpStep() {
    return 1;
  }

  @Override
  public boolean isControlledByLocalInstance() {
    return !level().isClientSide();
  }

  @Override
  public LivingEntity getControllingPassenger() {
    return getFirstPassenger() instanceof Player player ? player : null;
  }

  @Override
  protected boolean canAddPassenger(Entity entity) {
    return getPassengers().isEmpty() && entity instanceof Player;
  }

  @Override
  protected Vec3 getPassengerAttachmentPoint(
      Entity entity, EntityDimensions dimensions, float scale) {
    return new Vec3(0, .85, 0);
  }

  public boolean input(ServerPlayer player, float throttle, float steer) {
    if (player.getVehicle() != this
        || getControllingPassenger() != player
        || !Float.isFinite(throttle)
        || !Float.isFinite(steer)
        || Math.abs(throttle) > 1
        || Math.abs(steer) > 1) return false;
    this.throttle = throttle;
    this.steer = steer;
    inputAt = level().getGameTime();
    return true;
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide()) {
      var previous = position();
      if (lerpSteps > 0) {
        setPos(
            getX() + (targetX - getX()) / lerpSteps,
            getY() + (targetY - getY()) / lerpSteps,
            getZ() + (targetZ - getZ()) / lerpSteps);
        setYRot(getYRot() + net.minecraft.util.Mth.wrapDegrees(targetYaw - getYRot()) / lerpSteps);
        lerpSteps--;
      }
      wheelAngle += (float) position().subtract(previous).horizontalDistance() * 4;
      return;
    }
    var cell = inventory.getStackInSlot(9);
    if (cell.is(ModExpansion.BATTERY.get())) {
      int moved = battery.receiveEnergy(BatteryItem.charge(cell), false);
      BatteryItem.charge(cell, BatteryItem.charge(cell) - moved);
    }
    if (level().getGameTime() - inputAt > 8 || getControllingPassenger() == null) {
      throttle = 0;
      steer = 0;
    }
    boolean powered = energy() > 0 && Math.abs(throttle) > .01 && onGround();
    setYRot(getYRot() + (powered ? steer * 3 : 0));
    double speed = powered ? throttle * .42 : 0;
    var old = position();
    double radians = Math.toRadians(getYRot());
    var body = com.vic17usd.galaxycraft.solar.SolarSystem.body(level());
    double gravity =
        AdvancedSurvival.zeroG(this) ? 0 : .06 * (body == null ? 1 : Math.max(.1, body.gravity()));
    var velocity = getDeltaMovement();
    double vx = velocity.x * .65 - Math.sin(radians) * speed * .35,
        vz = velocity.z * .65 + Math.cos(radians) * speed * .35;
    setDeltaMovement(vx, Math.max(-.6, velocity.y - gravity), vz);
    move(MoverType.SELF, getDeltaMovement());
    if (horizontalCollision) setDeltaMovement(0, getDeltaMovement().y, 0);
    double distance = position().subtract(old).horizontalDistance();
    wheelAngle += (float) distance * 4;
    if (powered && distance > .005) {
      entityData.set(ENERGY, energy() - 1);
      if (getControllingPassenger() instanceof ServerPlayer player) {
        var state = QuestService.state(player);
        state.drivenMillimeters =
            Math.min(1000000, state.drivenMillimeters + (int) (Math.min(distance, 1) * 1000));
      }
    }
  }

  @Override
  public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
    targetX = x;
    targetY = y;
    targetZ = z;
    targetYaw = yaw;
    lerpSteps = 3;
  }

  @Override
  public InteractionResult interact(Player player, InteractionHand hand) {
    if (player instanceof ServerPlayer server) {
      if (player.isShiftKeyDown()) server.openMenu(this, buffer -> buffer.writeInt(getId()));
      else if (getPassengers().isEmpty()) player.startRiding(this);
    }
    return InteractionResult.sidedSuccess(level().isClientSide());
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    if (level().isClientSide() || isRemoved() || isInvulnerableTo(source)) return false;
    entityData.set(HEALTH, entityData.get(HEALTH) - amount);
    markHurt();
    if (entityData.get(HEALTH) <= 0) {
      for (int i = 0; i < 10; i++) spawnAtLocation(inventory.getStackInSlot(i));
      spawnAtLocation(ModExpansion.ROVER_ITEM.get());
      discard();
    }
    return true;
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    tag.putInt("Energy", energy());
    tag.putFloat("Health", entityData.get(HEALTH));
    tag.put("Inventory", inventory.serializeNBT(registryAccess()));
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    entityData.set(ENERGY, Math.clamp(tag.getInt("Energy"), 0, 6000));
    entityData.set(HEALTH, tag.contains("Health") ? Math.clamp(tag.getFloat("Health"), 1, 30) : 30);
    var items = tag.getCompound("Inventory").copy();
    items.putInt("Size", 10);
    inventory.deserializeNBT(registryAccess(), items);
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("entity.galaxycraft.planetary_rover");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new RoverMenu(id, inventory, this);
  }
}
