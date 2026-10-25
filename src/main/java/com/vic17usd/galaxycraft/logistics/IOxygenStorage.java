package com.vic17usd.galaxycraft.logistics;

/** Unidades respirables (una por ocupante y segundo), no milibuckets de combustible. */
public interface IOxygenStorage {
  int stored();

  int capacity();

  int receive(int amount, boolean simulate);

  int extract(int amount, boolean simulate);
}
