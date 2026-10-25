package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.biosphere.LivingProfiles;
import com.vic17usd.galaxycraft.expansion.ModExpansion;
import com.vic17usd.galaxycraft.weather.SpaceWeather;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Niebla por perfil sin sobrescribir inmersión en agua/lava ni la tormenta marciana existente. */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID, value = Dist.CLIENT)
public final class PlanetAtmospheres {
  private static LivingProfiles.Profile profile(ViewportEvent event) {
    var player = Minecraft.getInstance().player;
    if (player == null || event.getCamera().getFluidInCamera() != FogType.NONE) return null;
    if (player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
        || player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)) return null;
    var state = player.getData(ModExpansion.EXPOSURE.get());
    if (state.weather == SpaceWeather.DUST && !state.sheltered) return null;
    var profile = LivingProfiles.of(player.level());
    return profile == null || profile.body().equals("earth") ? null : profile;
  }

  @SubscribeEvent
  public static void fog(ViewportEvent.RenderFog event) {
    var profile = profile(event);
    if (profile != null) {
      boolean vacuum = profile.celestial().atmosphere().equals("none");
      float far =
          vacuum
              ? event.getFarPlaneDistance()
              : Math.min(event.getFarPlaneDistance(), profile.fogDistance());
      event.setNearPlaneDistance(
          vacuum
              ? far * .9F
              : profile.fogDistance() <= 64
                  ? 0
                  : Math.min(event.getNearPlaneDistance(), far * .7F));
      event.setFarPlaneDistance(far);
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void color(ViewportEvent.ComputeFogColor event) {
    var profile = profile(event);
    if (profile != null) {
      if (profile.celestial().atmosphere().equals("none")) {
        event.setRed(.003F);
        event.setGreen(.004F);
        event.setBlue(.008F);
        return;
      }
      float brightness =
          (float)
              (.18
                  + .82
                      * Math.max(
                          0,
                          Math.cos(Minecraft.getInstance().level.getTimeOfDay(0) * Math.PI * 2)));
      event.setRed((profile.fog() >> 16 & 255) / 255F * brightness);
      event.setGreen((profile.fog() >> 8 & 255) / 255F * brightness);
      event.setBlue((profile.fog() & 255) / 255F * brightness);
    }
  }

  private PlanetAtmospheres() {}
}
