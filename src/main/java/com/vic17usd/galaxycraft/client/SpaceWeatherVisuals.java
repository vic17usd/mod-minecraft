package com.vic17usd.galaxycraft.client;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.expansion.ModExpansion;
import com.vic17usd.galaxycraft.weather.SpaceWeather;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Polvo visible sin alterar clima vanilla ni cargar código de cliente en el servidor. */
@EventBusSubscriber(modid = GalaxyCraft.MOD_ID, value = Dist.CLIENT)
public final class SpaceWeatherVisuals {
  private static boolean dust() {
    var player = Minecraft.getInstance().player;
    if (player == null) return false;
    var exposure = player.getData(ModExpansion.EXPOSURE.get());
    return exposure.weather == SpaceWeather.DUST && !exposure.sheltered;
  }

  @SubscribeEvent
  public static void fog(ViewportEvent.RenderFog event) {
    if (dust()) {
      event.setNearPlaneDistance(0);
      event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), 36));
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void color(ViewportEvent.ComputeFogColor event) {
    if (dust()) {
      event.setRed(.43F);
      event.setGreen(.22F);
      event.setBlue(.13F);
    }
  }

  private SpaceWeatherVisuals() {}
}
