package com.vic17usd.galaxycraft.astronomy;

import com.vic17usd.galaxycraft.biosphere.LivingProfiles;
import net.minecraft.world.phys.Vec3;

/** Coeficientes por km y funciones de fase de dispersión simple; no simula dispersión múltiple. */
public final class AtmosphereOptics {
  public record Medium(
      Vec3 rayleigh,
      Vec3 mie,
      Vec3 absorption,
      double height,
      double rayleighScale,
      double mieScale,
      double asymmetry) {}

  public static final Medium VACUUM = new Medium(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, 0, 8, 1.2, .76);

  public static Medium forBody(String id) {
    var profile = LivingProfiles.get(id);
    if (profile.celestial().atmosphere().equals("none")) return VACUUM;
    if (id.equals("mars"))
      return new Medium(
          new Vec3(.0002, .00045, .0011),
          new Vec3(.003, .004, .005),
          new Vec3(.0004, .002, .005),
          50,
          11,
          9,
          .8);
    if (id.equals("venus"))
      return new Medium(
          new Vec3(.001, .002, .004),
          new Vec3(.06, .055, .04),
          new Vec3(.001, .006, .025),
          100,
          16,
          9,
          .85);
    if (id.equals("titan"))
      return new Medium(
          new Vec3(.0002, .0004, .0008),
          new Vec3(.012, .009, .005),
          new Vec3(.001, .007, .02),
          100,
          20,
          16,
          .82);
    return new Medium(
        new Vec3(.0058, .0135, .0331),
        new Vec3(.003, .003, .003),
        new Vec3(.0001, .0003, .0001),
        80,
        8,
        1.2,
        .76);
  }

  public static double rayleighPhase(double cosine) {
    return 3 / (16 * Math.PI) * (1 + cosine * cosine);
  }

  public static double miePhase(double cosine, double g) {
    return (1 - g * g)
        / (4
            * Math.PI
            * Math.pow(Math.max(.001, 1 + g * g - 2 * g * Math.clamp(cosine, -1, 1)), 1.5));
  }

  public static Vec3 transmittance(Medium medium, double path) {
    var beta = medium.rayleigh().add(medium.mie()).add(medium.absorption());
    return new Vec3(Math.exp(-beta.x * path), Math.exp(-beta.y * path), Math.exp(-beta.z * path));
  }

  private AtmosphereOptics() {}
}
