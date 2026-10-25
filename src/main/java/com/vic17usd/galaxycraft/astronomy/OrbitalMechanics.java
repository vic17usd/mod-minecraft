package com.vic17usd.galaxycraft.astronomy;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Órbitas keplerianas medias, km y días terrestres desde J2000; no es una efeméride JPL. */
public final class OrbitalMechanics {
  public record Orbit(
      String id,
      String parent,
      double radius,
      double axis,
      double eccentricity,
      double period,
      double inclination,
      double node,
      double periapsis,
      double anomaly,
      double rotation,
      double tilt,
      boolean equatorial) {}

  public static final List<Orbit> ALL = load();
  private static final Map<String, Orbit> BY_ID = new HashMap<>();

  static {
    for (var orbit : ALL)
      if (BY_ID.put(orbit.id(), orbit) != null) throw new IllegalStateException("Órbita duplicada");
  }

  private static List<Orbit> load() {
    try (var stream = OrbitalMechanics.class.getResourceAsStream("/celestial_orbits.json")) {
      if (stream == null) throw new IllegalStateException("Falta celestial_orbits.json");
      return List.of(
          new Gson()
              .fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Orbit[].class));
    } catch (java.io.IOException error) {
      throw new IllegalStateException(error);
    }
  }

  public static Orbit get(String id) {
    var result = BY_ID.get(id);
    if (result == null) throw new IllegalArgumentException(id);
    return result;
  }

  public static double eccentricAnomaly(double mean, double e) {
    if (!Double.isFinite(mean) || !Double.isFinite(e) || e < 0 || e >= 1)
      throw new IllegalArgumentException("Órbita no elíptica");
    double m = Math.IEEEremainder(mean, 2 * Math.PI), value = m;
    for (int i = 0; i < 12; i++) {
      double delta = (value - e * Math.sin(value) - m) / (1 - e * Math.cos(value));
      value -= delta;
      if (Math.abs(delta) < 1e-12) break;
    }
    return value;
  }

  public static Vec3 position(String id, double days) {
    var orbit = get(id);
    if (orbit.parent().isEmpty()) return Vec3.ZERO;
    double e =
        eccentricAnomaly(
            Math.toRadians(orbit.anomaly())
                + 2 * Math.PI * (days % orbit.period()) / orbit.period(),
            orbit.eccentricity());
    Vec3 local =
        new Vec3(
            orbit.axis() * (Math.cos(e) - orbit.eccentricity()),
            0,
            orbit.axis()
                * Math.sqrt(1 - orbit.eccentricity() * orbit.eccentricity())
                * Math.sin(e));
    local = rotateY(local, Math.toRadians(orbit.periapsis()));
    local = rotateX(local, Math.toRadians(orbit.inclination()));
    local = rotateY(local, Math.toRadians(orbit.node()));
    if (orbit.equatorial()) local = rotateX(local, Math.toRadians(get(orbit.parent()).tilt()));
    return position(orbit.parent(), days).add(local);
  }

  public static Vec3 rotateX(Vec3 v, double angle) {
    return new Vec3(
        v.x,
        v.y * Math.cos(angle) - v.z * Math.sin(angle),
        v.y * Math.sin(angle) + v.z * Math.cos(angle));
  }

  public static Vec3 rotateY(Vec3 v, double angle) {
    return new Vec3(
        v.x * Math.cos(angle) - v.z * Math.sin(angle),
        v.y,
        v.x * Math.sin(angle) + v.z * Math.cos(angle));
  }

  /** Rotación de referencia para superficies; las lunas se aproximan con rotación síncrona. */
  public static double spin(Orbit orbit, double days) {
    if (!orbit.parent().isEmpty() && !orbit.parent().equals("sun")) {
      var facing =
          rotateX(
              position(orbit.parent(), days).subtract(position(orbit.id(), days)),
              -Math.toRadians(orbit.tilt()));
      return Math.atan2(facing.z, facing.x);
    }
    return 2 * Math.PI * (days % Math.abs(orbit.rotation())) / orbit.rotation()
        + Math.toRadians(orbit.anomaly());
  }

  public static Vec3 localDirection(String body, Vec3 world, double days, double x, double z) {
    var orbit = get(body);
    double longitude = x / (orbit.radius() * 1000),
        latitude = Math.clamp(z / (orbit.radius() * 1000), -Math.PI / 2, Math.PI / 2);
    Vec3 v = rotateX(world, -Math.toRadians(orbit.tilt()));
    v = rotateY(v, -spin(orbit, days) - longitude);
    // El origen se sitúa sobre el ecuador; Y local es la vertical, X el este.
    return new Vec3(
        v.z,
        v.x * Math.cos(latitude) + v.y * Math.sin(latitude),
        -v.x * Math.sin(latitude) + v.y * Math.cos(latitude));
  }

  public static Vec3 surfaceOffset(String body, double days, double x, double z, double height) {
    var orbit = get(body);
    double latitude = Math.clamp(z / (orbit.radius() * 1000), -Math.PI / 2, Math.PI / 2),
        longitude = x / (orbit.radius() * 1000);
    var v =
        new Vec3(Math.cos(latitude), Math.sin(latitude), 0)
            .scale(orbit.radius() + Math.max(0, height) / 1000);
    return rotateX(rotateY(v, spin(orbit, days) + longitude), Math.toRadians(orbit.tilt()));
  }

  public static double angularRadius(double radius, double distance) {
    return Math.asin(Math.clamp(radius / Math.max(radius, distance), 0, 1));
  }

  public static double illumination(Vec3 toSun, Vec3 toObserver) {
    return Math.clamp((1 + toSun.normalize().dot(toObserver.normalize())) * .5, 0, 1);
  }

  private OrbitalMechanics() {}
}
