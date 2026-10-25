package com.vic17usd.galaxycraft.client;

/** Las estaciones comparten catálogo orbital, pero no heredan la dispersión de la superficie. */
public final class OrbitalEffects extends PlanetSkyEffects {
  public OrbitalEffects() {
    super(null, true);
  }
}
