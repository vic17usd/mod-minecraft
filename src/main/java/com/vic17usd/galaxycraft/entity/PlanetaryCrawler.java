package com.vic17usd.galaxycraft.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;

/** Reptador alienígena: IA terrestre y geometría vanilla, sin necesidad de respirar. */
public final class PlanetaryCrawler extends Silverfish {
    public PlanetaryCrawler(EntityType<? extends Silverfish> type, Level level) { super(type, level); }
}
