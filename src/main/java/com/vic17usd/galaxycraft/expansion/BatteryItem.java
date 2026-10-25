package com.vic17usd.galaxycraft.expansion;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Batería reutilizable: carga persistida y sincronizada mediante Data Components. */
public final class BatteryItem extends Item {
  public BatteryItem() {
    super(new Properties().stacksTo(1).component(ModExpansion.CHARGE.get(), 6000));
  }

  public static int charge(ItemStack stack) {
    return Math.clamp(stack.getOrDefault(ModExpansion.CHARGE.get(), 0), 0, 6000);
  }

  public static void charge(ItemStack stack, int value) {
    stack.set(ModExpansion.CHARGE.get(), Math.clamp(value, 0, 6000));
  }

  @Override
  public boolean isBarVisible(ItemStack stack) {
    return charge(stack) < 6000;
  }

  @Override
  public int getBarWidth(ItemStack stack) {
    return Math.round(13F * charge(stack) / 6000);
  }

  @Override
  public int getBarColor(ItemStack stack) {
    return 0x55DDEE;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    lines.add(Component.translatable("expansion.galaxycraft.battery", charge(stack), 6000));
  }
}
