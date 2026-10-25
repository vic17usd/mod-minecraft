package com.vic17usd.galaxycraft.combat;

import com.vic17usd.galaxycraft.solar.SolarSystem;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.Level;

/**
 * Hostil sin combustión solar ni conversión acuática; las lunas heladas ralentizan con sus golpes.
 */
public final class PlanetaryRaider extends Husk {
  public PlanetaryRaider(EntityType<? extends Husk> type, Level level) {
    super(type, level);
  }

  @Override
  protected boolean convertsInWater() {
    return false;
  }

  @Override
  public boolean doHurtTarget(Entity target) {
    boolean result = super.doHurtTarget(target);
    var body = SolarSystem.body(level());
    if (result && target instanceof LivingEntity living && body != null && body.tier() == 3)
      living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0), this);
    return result;
  }
}
