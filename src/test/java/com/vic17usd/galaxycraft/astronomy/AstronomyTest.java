package com.vic17usd.galaxycraft.astronomy;

import static org.junit.jupiter.api.Assertions.*;

import com.vic17usd.galaxycraft.solar.SolarSystem;
import java.util.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Pruebas numéricas independientes del cliente OpenGL y del tiempo real de la máquina. */
class AstronomyTest {
  @Test
  void catalogoTieneSolYTodosLosCuerposSinDuplicados() {
    assertEquals(30, OrbitalMechanics.ALL.size());
    var ids = new HashSet<String>();
    for (var orbit : OrbitalMechanics.ALL) {
      assertTrue(ids.add(orbit.id()));
      assertTrue(orbit.radius() > 0);
      assertTrue(orbit.period() > 0);
      assertTrue(orbit.eccentricity() >= 0 && orbit.eccentricity() < 1);
      if (!orbit.parent().isEmpty()) assertNotNull(OrbitalMechanics.get(orbit.parent()));
    }
    for (var body : SolarSystem.BODIES) assertTrue(ids.contains(body.id()));
  }

  @Test
  void solucionDeKeplerCumpleLaEcuacion() {
    for (double e : new double[] {0, .006, .0549, .20563, .8})
      for (double mean = -40; mean < 40; mean += .13) {
        double actual = OrbitalMechanics.eccentricAnomaly(mean, e);
        assertEquals(Math.IEEEremainder(mean, Math.PI * 2), actual - e * Math.sin(actual), 1e-9);
      }
    assertThrows(IllegalArgumentException.class, () -> OrbitalMechanics.eccentricAnomaly(1, 1));
    assertThrows(
        IllegalArgumentException.class, () -> OrbitalMechanics.eccentricAnomaly(1, Double.NaN));
  }

  @Test
  void posicionesSonFinitasInclusoEnDiasNegativosYLejanos() {
    for (double day : new double[] {-10000, 0, 1, 27.3, 365.25, 100000})
      for (var orbit : OrbitalMechanics.ALL) {
        var pos = OrbitalMechanics.position(orbit.id(), day);
        assertTrue(Double.isFinite(pos.lengthSqr()), orbit.id());
        if (!orbit.parent().isEmpty()) {
          double distance = pos.distanceTo(OrbitalMechanics.position(orbit.parent(), day));
          assertTrue(distance >= orbit.axis() * (1 - orbit.eccentricity()) - .001);
          assertTrue(distance <= orbit.axis() * (1 + orbit.eccentricity()) + .001);
        }
      }
  }

  @Test
  void periodoPlanetaYPeriodoLunaSeReproducen() {
    var mars = OrbitalMechanics.get("mars");
    assertTrue(
        OrbitalMechanics.position("mars", 0)
                .distanceTo(OrbitalMechanics.position("mars", mars.period()))
            < .001);
    var phobos = OrbitalMechanics.get("phobos");
    Vec3 a = OrbitalMechanics.position("phobos", 0).subtract(OrbitalMechanics.position("mars", 0)),
        b =
            OrbitalMechanics.position("phobos", phobos.period())
                .subtract(OrbitalMechanics.position("mars", phobos.period()));
    assertTrue(a.distanceTo(b) < .001);
    assertTrue(phobos.period() < OrbitalMechanics.get("deimos").period());
  }

  @Test
  void tierraDesdeLunaTieneDiametroDeDosGradosNoMedioCielo() {
    double degrees = Math.toDegrees(2 * OrbitalMechanics.angularRadius(6371, 384400));
    assertEquals(1.9, degrees, .04);
    assertEquals(.53, Math.toDegrees(2 * OrbitalMechanics.angularRadius(696340, 149598023)), .01);
  }

  @Test
  void fasesRespondenAlAnguloSolar() {
    var x = new Vec3(1, 0, 0);
    assertEquals(1, OrbitalMechanics.illumination(x, x), 1e-12);
    assertEquals(0, OrbitalMechanics.illumination(x, x.scale(-1)), 1e-12);
    assertEquals(.5, OrbitalMechanics.illumination(x, new Vec3(0, 1, 0)), 1e-12);
  }

  @Test
  void transformacionLocalConservaNormaYSuperficieTieneVertical() {
    for (var orbit : OrbitalMechanics.ALL) {
      var direction = new Vec3(.3, .7, .4).normalize();
      assertEquals(
          1, OrbitalMechanics.localDirection(orbit.id(), direction, 32, 100, -100).length(), 1e-9);
      var radial = OrbitalMechanics.surfaceOffset(orbit.id(), 32, 100, -100, 0).normalize();
      var local = OrbitalMechanics.localDirection(orbit.id(), radial, 32, 100, -100);
      assertEquals(1, local.y, 1e-9);
    }
  }

  @Test
  void lunaMuestraSuPlanetaDesdeElHemisferioCercano() {
    for (double day : new double[] {0, 4, 15, 31}) {
      var origin = OrbitalMechanics.position("moon", day);
      var earth = OrbitalMechanics.position("earth", day).subtract(origin).normalize();
      assertTrue(OrbitalMechanics.localDirection("moon", earth, day, 0, 0).y > .98);
    }
  }

  @Test
  void vacioNoDispersaYNieblaAtenuaSinValoresInvalidos() {
    assertEquals(new Vec3(1, 1, 1), AtmosphereOptics.transmittance(AtmosphereOptics.VACUUM, 10000));
    for (var body : SolarSystem.BODIES) {
      var medium = AtmosphereOptics.forBody(body.id());
      for (double path : new double[] {0, .01, 10, 1000}) {
        var t = AtmosphereOptics.transmittance(medium, path);
        assertTrue(t.x >= 0 && t.x <= 1 && t.y >= 0 && t.y <= 1 && t.z >= 0 && t.z <= 1);
      }
    }
  }

  @Test
  void fasesRayleighYMieTienenFisicaBasica() {
    assertEquals(AtmosphereOptics.rayleighPhase(-1), AtmosphereOptics.rayleighPhase(1));
    assertTrue(AtmosphereOptics.miePhase(1, .76) > AtmosphereOptics.miePhase(0, .76) * 30);
    var earth = AtmosphereOptics.forBody("earth");
    assertTrue(earth.rayleigh().z > earth.rayleigh().x * 5);
  }

  @Test
  void relojExtrapolaYSoloAceptaValoresFinitos() {
    var clock = new CelestialClock(100, 12, 2);
    assertTrue(clock.valid());
    assertEquals(14, clock.at(24100));
    assertFalse(new CelestialClock(0, Double.NaN, 1).valid());
    assertFalse(new CelestialClock(0, 0, -1).valid());
  }
}
