package com.vic17usd.galaxycraft.logistics;

/** Equilibra porcentaje de llenado entre depósitos, sin hacer circular recursos entre iguales. */
public final class TransferPolicy {
  public static int balance(
      int source, int sourceCapacity, int target, int targetCapacity, int maximum) {
    if (sourceCapacity <= 0 || targetCapacity <= 0 || maximum <= 0) return 0;
    long difference = (long) source * targetCapacity - (long) target * sourceCapacity;
    return (int)
        Math.max(0, Math.min(maximum, difference / ((long) sourceCapacity + targetCapacity)));
  }

  private TransferPolicy() {}
}
