package com.vic17usd.galaxycraft.expansion;

/**
 * Exposición transitoria, no se conserva al morir; el cliente recibe una instantánea para el HUD.
 */
public final class Exposure {
  public int thermal, radiation, temperature, pressure, input, roomOxygen, roomCapacity;
  public long inputAt;
  public boolean sheltered;
}
