package com.vic17usd.galaxycraft.civilization;

import com.vic17usd.galaxycraft.biosphere.*;
import com.vic17usd.galaxycraft.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/**
 * Colono persistente con stock compartido; todas las transacciones y recompensas son del servidor.
 */
public final class ColonyCitizen extends PathfinderMob implements MenuProvider {
  private int stock = 8;
  private long restocked;
  private BlockPos home;

  public ColonyCitizen(EntityType<? extends PathfinderMob> type, Level level) {
    super(type, level);
    setPersistenceRequired();
    if (getNavigation()
        instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground)
      ground.setCanOpenDoors(true);
  }

  public String body() {
    return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath().split("_")[0];
  }

  public void home(BlockPos pos) {
    home = pos.immutable();
    restrictTo(home, 24);
  }

  public int stock() {
    return stock;
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 8, .8, 1.2));
    goalSelector.addGoal(2, new OpenDoorGoal(this, true));
    goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, .8));
    goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, .6));
    goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
    goalSelector.addGoal(6, new RandomLookAroundGoal(this));
  }

  @Override
  public void aiStep() {
    super.aiStep();
    if (!level().isClientSide() && level().getGameTime() - restocked >= 6000) {
      stock = 8;
      restocked = level().getGameTime();
    }
  }

  public static int count(ServerPlayer player, Item item) {
    int count = 0;
    for (var stack : player.getInventory().items) if (stack.is(item)) count += stack.getCount();
    if (player.getOffhandItem().is(item)) count += player.getOffhandItem().getCount();
    return count;
  }

  private static void take(ServerPlayer player, Item item, int amount) {
    for (var stack : player.getInventory().items)
      if (stack.is(item) && amount > 0) {
        int used = Math.min(amount, stack.getCount());
        stack.shrink(used);
        amount -= used;
      }
    if (amount > 0) player.getOffhandItem().shrink(amount);
  }

  private static void give(ServerPlayer player, Item item, int count) {
    var stack = new ItemStack(item, count);
    if (!player.getInventory().add(stack)) player.drop(stack, false);
  }

  public boolean act(ServerPlayer player, int action) {
    if (!isAlive()
        || !player.isAlive()
        || player.isSpectator()
        || player.level() != level()
        || player.distanceToSqr(this) > 36
        || !player.level().dimension().equals(LivingProfiles.get(body()).celestial().dimension()))
      return false;
    var contracts = player.getData(ModBiosphere.CONTRACTS.get());
    var harvest = ModBiosphere.HARVEST.get(body()).get();
    var resource =
        BuiltInRegistries.ITEM.get(ModPresentation.id(LivingProfiles.get(body()).resource()));
    switch (action) {
      case 0 -> {
        if (stock <= 0 || count(player, resource) < 4) return false;
        take(player, resource, 4);
        stock--;
        give(player, ModItems.OXYGEN_TANK.get(), 1);
      }
      case 1 -> {
        if (stock <= 0 || count(player, harvest) < 8) return false;
        take(player, harvest, 8);
        stock--;
        give(player, ModBiosphere.TOKEN.get(), 1);
      }
      case 2 -> {
        if (contracts.completed.contains(body()) || !contracts.accepted.add(body())) return false;
      }
      case 3 -> {
        if (!contracts.accepted.contains(body())
            || contracts.completed.contains(body())
            || count(player, harvest) < 16) return false;
        contracts.completed.add(body());
        take(player, harvest, 16);
        give(player, ModBiosphere.TOKEN.get(), 2);
        give(player, resource, 4);
        player.giveExperiencePoints(150);
      }
      case 4 -> {
        if (stock <= 0 || count(player, ModBiosphere.TOKEN.get()) < 1) return false;
        take(player, ModBiosphere.TOKEN.get(), 1);
        stock--;
        give(player, resource, 4);
      }
      default -> {
        return false;
      }
    }
    player.inventoryMenu.broadcastChanges();
    if (player.containerMenu instanceof ColonyMenu menu) menu.broadcastChanges();
    return true;
  }

  @Override
  public InteractionResult mobInteract(Player player, InteractionHand hand) {
    if (player instanceof ServerPlayer server
        && server.containerMenu == server.inventoryMenu
        && server.isAlive()
        && !server.isSpectator()
        && server.level().dimension().equals(LivingProfiles.get(body()).celestial().dimension()))
      server.openMenu(this, buffer -> buffer.writeInt(getId()));
    return InteractionResult.sidedSuccess(level().isClientSide());
  }

  @Override
  public Component getDisplayName() {
    return getName();
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new ColonyMenu(id, inventory, this);
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putInt("Stock", stock);
    tag.putLong("Restocked", restocked);
    if (home != null) tag.putLong("ColonyHome", home.asLong());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    stock = tag.contains("Stock") ? Math.clamp(tag.getInt("Stock"), 0, 8) : 8;
    restocked = Math.max(0, tag.getLong("Restocked"));
    if (tag.contains("ColonyHome")) home(BlockPos.of(tag.getLong("ColonyHome")));
  }
}
