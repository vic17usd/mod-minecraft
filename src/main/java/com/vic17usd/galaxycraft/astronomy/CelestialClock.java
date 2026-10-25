package com.vic17usd.galaxycraft.astronomy;

/** Snapshot S2C extrapolado con gameTime: no usa el reloj del ordenador ni permisos del cliente. */
public record CelestialClock(long tick, double days, double rate) {
  public double at(double gameTime) {
    return days + (gameTime - tick) / 24000 * rate;
  }

  public boolean valid() {
    return Double.isFinite(days) && Double.isFinite(rate) && rate >= .01 && rate <= 365;
  }
}
