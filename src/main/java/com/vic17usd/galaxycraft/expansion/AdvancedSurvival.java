package com.vic17usd.galaxycraft.expansion;

import com.vic17usd.galaxycraft.equipment.SpaceEquipment;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import com.vic17usd.galaxycraft.station.OrbitStations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Exposición, módulos y flotación sin cambiar las defensas ni la física de otros mods. */
public final class AdvancedSurvival {
  private static final ResourceLocation FLIGHT =
      ResourceLocation.fromNamespaceAndPath("galaxycraft", "orbital_flight");

  public static boolean zeroG(Entity entity) {
    return OrbitStations.isOrbit(entity.level())
        && !ExpansionNetworks.gravity(entity.level(), entity.blockPosition());
  }

  public static void physics(Entity entity) {
    if (entity instanceof net.minecraft.world.entity.player.Player player) {
      var attribute =
          player.getAttribute(net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT);
      if (attribute != null) {
        boolean zero = zeroG(player);
        if (zero && !attribute.hasModifier(FLIGHT))
          attribute.addTransientModifier(
              new AttributeModifier(FLIGHT, 1, AttributeModifier.Operation.ADD_VALUE));
        else if (!zero && attribute.hasModifier(FLIGHT)) {
          attribute.removeModifier(FLIGHT);
          if (!player.mayFly()) {
            player.getAbilities().flying = false;
            if (player instanceof ServerPlayer server) server.onUpdateAbilities();
          }
        }
      }
      if (player instanceof ServerPlayer server
          && zeroG(player)
          && !player.isPassenger()
          && !player.isSpectator()) {
        var exposure = player.getData(ModExpansion.EXPOSURE.get());
        int keys = player.level().getGameTime() - exposure.inputAt <= 8 ? exposure.input : 0;
        Vec3
            forward =
                new Vec3(
                    -Math.sin(Math.toRadians(player.getYRot())),
                    0,
                    Math.cos(Math.toRadians(player.getYRot()))),
            right = new Vec3(forward.z, 0, -forward.x);
        Vec3 acceleration =
            forward
                .scale(((keys & 1) != 0 ? 1 : 0) - ((keys & 2) != 0 ? 1 : 0))
                .add(right.scale(((keys & 8) != 0 ? 1 : 0) - ((keys & 4) != 0 ? 1 : 0)))
                .add(0, ((keys & 16) != 0 ? 1 : 0) - ((keys & 32) != 0 ? 1 : 0), 0);
        if (acceleration.lengthSqr() > 1) acceleration = acceleration.normalize();
        var velocity = player.getDeltaMovement().scale(.98).add(acceleration.scale(.025));
        if (velocity.length() > .4) velocity = velocity.normalize().scale(.4);
        player.setDeltaMovement(velocity);
        player.fallDistance = 0;
        if (player.tickCount % 3 == 0)
          server.connection.send(
              new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
      }
    } else if (entity instanceof ItemEntity) {
      // La etiqueta solo marca nuestra intervención, no sustituye Data Components de la pila.
      var data = entity.getPersistentData();
      if (zeroG(entity)) {
        if (!data.contains("GalaxyCraftOriginalGravity"))
          data.putBoolean("GalaxyCraftOriginalGravity", entity.isNoGravity());
        entity.setNoGravity(true);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(.995));
      } else if (data.contains("GalaxyCraftOriginalGravity")) {
        entity.setNoGravity(data.getBoolean("GalaxyCraftOriginalGravity"));
        data.remove("GalaxyCraftOriginalGravity");
      }
    }
  }

  public static int temperature(net.minecraft.world.level.Level level) {
    if (OrbitStations.isOrbit(level)) return -180;
    var body = SolarSystem.body(level);
    if (body == null) return 15;
    return switch (body.id()) {
      case "earth" -> 15;
      case "venus" -> 465;
      case "mercury" -> 430;
      case "mars" -> -65;
      case "moon" -> -80;
      case "io" -> 120;
      default -> -170;
    };
  }

  private static boolean module(ServerPlayer player, int slot) {
    var inventory = SpaceEquipment.inventory(player);
    return inventory
            .getStackInSlot(slot)
            .is(slot == 6 ? ModExpansion.THERMAL.get() : ModExpansion.SHIELD.get())
        && SpaceEquipment.fullSuit(player)
        && SpaceEquipment.hasOxygen(player);
  }

  private static void wear(ServerPlayer player, int slot) {
    var inventory = SpaceEquipment.inventory(player);
    var stack = inventory.getStackInSlot(slot);
    stack.setDamageValue(stack.getDamageValue() + 1);
    if (stack.getDamageValue() >= stack.getMaxDamage()) stack.shrink(1);
    inventory.markDirty();
  }

  public static void tick(ServerPlayer player) {
    if (!player.isAlive() || player.isSpectator() || player.tickCount % 20 != 0) return;
    var value = player.getData(ModExpansion.EXPOSURE.get());
    var room = ExpansionNetworks.room(player);
    value.sheltered = room != null;
    var reserve =
        room == null
            ? new com.vic17usd.galaxycraft.component.OxygenData(0, 1200)
            : com.vic17usd.galaxycraft.item.OxygenTankItem.oxygen(room.inventory.getStackInSlot(0));
    value.roomOxygen = reserve.current();
    value.roomCapacity = reserve.maximum();
    value.temperature = value.sheltered ? 22 : temperature(player.level());
    var body = OrbitStations.parent(player.level());
    int radiation =
        value.sheltered
            ? 0
            : OrbitStations.isOrbit(player.level()) ? 3 : body == null ? 0 : body.radiation();
    int flare=value.sheltered?0:com.vic17usd.galaxycraft.weather.SpaceWeather.radiation(player.level());radiation+=flare;
    value.weather=com.vic17usd.galaxycraft.weather.SpaceWeather.active(player.level());
    value.weatherSeconds=value.weather==0?0:(int)Math.max(0,(com.vic17usd.galaxycraft.weather.SpaceWeatherState.get(player.serverLevel()).ends-player.level().getGameTime())/20);
    boolean extreme = value.temperature > 60 || value.temperature < -35,
        thermal = module(player, 6),
        shield = module(player, 7);
    value.thermal = Math.clamp(value.thermal + (extreme && !thermal ? 4 : -5), 0, 100);
    value.radiation =
        Math.clamp(value.radiation + (shield ? Math.max(-4,radiation-4) : radiation > 0 ? radiation : -2), 0, 100);
    value.pressure =
        value.sheltered
            ? 101
            : com.vic17usd.galaxycraft.survival.SpaceSurvival.isAirless(player.level()) ? 0 : 101;
    if (!player.isCreative() && player.tickCount % 100 == 0) {
      if (extreme && thermal) wear(player, 6);
      if (radiation > 0 && shield) wear(player, 7);
      float factor = SpaceEquipment.fullSuit(player) && SpaceEquipment.hasOxygen(player) ? .25F : 1;
      if (value.thermal >= 40 && !thermal)
        player.hurt(
            value.temperature < 0
                ? player.damageSources().freeze()
                : player.damageSources().hotFloor(),
            2 * factor);
      if (value.radiation >= 30 && (!shield||flare>0))
        player.hurt(player.damageSources().magic(), Math.max(1, radiation) * factor*(shield?.5F:1));
    }
    var tag = new net.minecraft.nbt.CompoundTag();
    tag.putInt("Temperature", value.temperature);
    tag.putInt("Thermal", value.thermal);
    tag.putInt("Radiation", value.radiation);
    tag.putInt("Pressure", value.pressure);
    tag.putBoolean("Sheltered", value.sheltered);
    tag.putInt("RoomOxygen", value.roomOxygen);
    tag.putInt("RoomCapacity", value.roomCapacity);
    tag.putInt("Weather",value.weather);tag.putInt("WeatherSeconds",value.weatherSeconds);
    PacketDistributor.sendToPlayer(player, new ExpansionNetworking.EnvironmentSnapshot(tag));
  }

  private AdvancedSurvival() {}
}
