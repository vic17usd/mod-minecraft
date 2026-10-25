package com.vic17usd.galaxycraft.morphology;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.astronomy.CelestialClock;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.*;

/** Datos transitorios y partículas; ninguna clase común referencia clases del cliente. */
public final class ModMorphology {
  public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
      DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GalaxyCraft.MOD_ID);
  public static final java.util.function.Supplier<AttachmentType<AlienPose>> POSE =
      ATTACHMENTS.register(
          "alien_pose", () -> AttachmentType.builder(() -> AlienPose.REST).build());
  public static final java.util.function.Supplier<AttachmentType<CelestialClock>> CLOCK =
      ATTACHMENTS.register(
          "celestial_clock",
          () -> AttachmentType.builder(() -> new CelestialClock(0, 0, 1)).build());
  public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES =
      DeferredRegister.create(Registries.PARTICLE_TYPE, GalaxyCraft.MOD_ID);
  public static final java.util.function.Supplier<SimpleParticleType> SPORE =
      PARTICLES.register("bio_spore", () -> new SimpleParticleType(false));
  public static final ModConfigSpec SERVER_SPEC;
  public static final ModConfigSpec.DoubleValue ORBIT_RATE;

  static {
    var b = new ModConfigSpec.Builder();
    ORBIT_RATE =
        b.comment(
                "Días terrestres por día de Minecraft; referencia J2000, no fecha real del"
                    + " ordenador.")
            .defineInRange("orbitalDaysPerMinecraftDay", 1, .01, 365);
    SERVER_SPEC = b.build();
  }

  private ModMorphology() {}
}
