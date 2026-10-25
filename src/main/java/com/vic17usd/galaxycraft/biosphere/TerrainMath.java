package com.vic17usd.galaxycraft.biosphere;

/** Relieve puro, continuo entre chunks y dependiente de semilla; no consulta ni modifica mundos. */
public final class TerrainMath {
  private static long mix(long value) {
    value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
    value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
    return value ^ (value >>> 31);
  }

  public static double hash(int x, int z, long seed) {
    return (mix(seed + x * 341873128712L + z * 132897987541L) >>> 11) * 0x1.0p-53;
  }

  public static double noise(double x, double z, double scale, long seed) {
    double sx = x / scale, sz = z / scale;
    int ix = (int) Math.floor(sx), iz = (int) Math.floor(sz);
    double tx = sx - ix, tz = sz - iz;
    tx = tx * tx * (3 - 2 * tx);
    tz = tz * tz * (3 - 2 * tz);
    return (hash(ix, iz, seed) * (1 - tx) + hash(ix + 1, iz, seed) * tx) * (1 - tz)
        + (hash(ix, iz + 1, seed) * (1 - tx) + hash(ix + 1, iz + 1, seed) * tx) * tz;
  }

  public static int height(LivingProfiles.Profile profile, int x, int z, long worldSeed) {
    long seed = worldSeed ^ profile.seed();
    double broad = noise(x, z, 512, seed), detail = noise(x, z, 96, seed + 71), height;
    switch (profile.terrain()) {
      case "RIFT" -> {
        double fissure = (noise(x, z, 320, seed + 3) * 2 - 1) / .105;
        height = 118 + broad * 28 + detail * 22 - 112 * Math.exp(-fissure * fissure);
      }
      case "RIDGES" -> {
        double ridge = 1 - Math.abs(detail * 2 - 1);
        height = 57 + 110 * Math.pow(ridge, 3) + 42 * broad;
      }
      case "DUNES" -> {
        double wave = (Math.sin(x / 43.0 + z / 103.0 + noise(x, z, 256, seed + 1) * 3) + 1) / 2;
        height = 56 + 30 * broad + 26 * Math.pow(wave, 4) + detail * 9;
      }
      case "GEYSERS" -> height = 70 + broad * 12 + detail * 6;
      case "CRATERS" -> {
        height = 102 + broad * 13;
        int cx = Math.floorDiv(x, 256), cz = Math.floorDiv(z, 256);
        double depression = 0, rim = 0;
        for (int dx = -1; dx <= 1; dx++)
          for (int dz = -1; dz <= 1; dz++) {
            int gx = cx + dx, gz = cz + dz;
            double px = gx * 256 + 48 + hash(gx, gz, seed) * 160,
                pz = gz * 256 + 48 + hash(gx, gz, seed + 9) * 160,
                radius = 58 + hash(gx, gz, seed + 19) * 42,
                r = Math.hypot(x - px, z - pz),
                inside = Math.max(0, 1 - r * r / (radius * radius));
            depression = Math.max(depression, 68 * inside * inside);
            rim = Math.max(rim, 31 * Math.exp(-Math.pow((r - radius) / 9, 2)));
          }
        height += rim - depression;
      }
      default -> height = 72 + broad * 22 + detail * 12;
    }
    return (int) Math.clamp(Math.round(height), 18, 232);
  }

  public static boolean magma(LivingProfiles.Profile profile, int height) {
    return profile.terrain().equals("RIFT") && height < 44;
  }

  public static boolean cave(int x, int y, int z, long seed) {
    return noise(x + y * .7, z - y * .5, 23, seed + Math.floorDiv(y, 18) * 13) > .82;
  }

  private TerrainMath() {}
}
