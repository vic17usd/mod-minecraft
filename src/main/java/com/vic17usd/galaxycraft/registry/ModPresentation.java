package com.vic17usd.galaxycraft.registry;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.solar.SolarSystem;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registros comunes: nunca importan clases de renderizado del cliente. */
public final class ModPresentation {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GalaxyCraft.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR = DeferredRegister.create(Registries.ARMOR_MATERIAL, GalaxyCraft.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GalaxyCraft.MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, GalaxyCraft.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> LAUNCH = sound("rocket_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEPRESSURIZE = sound("suit_depressurize");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT = sound("space_ambient");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EXHAUST = PARTICLES.register("rocket_exhaust", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMOKE = PARTICLES.register("rocket_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST = PARTICLES.register("lunar_dust", () -> new SimpleParticleType(false));

    // Material propio con las mismas defensas iniciales del casco de hierro y reparación con titanio.
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SPACE_ARMOR = ARMOR.register("space_suit", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 2, ArmorItem.Type.CHESTPLATE, 6, ArmorItem.Type.LEGGINGS, 5, ArmorItem.Type.BOOTS, 2), 9, SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ModItems.TITANIUM_ORE.get()),
            List.of(new ArmorMaterial.Layer(id("space_suit"))), 0, 0));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GALAXY_TAB = TABS.register("galaxycraft", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.galaxycraft"))
            .icon(() -> ModItems.GALAXY_EMBLEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Orden estable: construcción, minerales, supervivencia, vehículos y tecnología.
                section(output, "building");
                for (var body : SolarSystem.BODIES) if (!body.id().equals("earth")) {
                    output.accept(ModPlanetContent.ITEMS.get(body.id() + "_stone").get());
                    output.accept(ModPlanetContent.ITEMS.get(body.id() + "_regolith").get());
                }
                section(output, "resources"); output.accept(ModItems.TITANIUM_ORE.get());
                for (var body : SolarSystem.BODIES) if (!body.id().equals("earth")) {
                    output.accept(ModPlanetContent.ITEMS.get(body.id() + "_ore").get());
                    output.accept(ModPlanetContent.ITEMS.get(body.resource()).get());
                }
                section(output, "tools"); output.accept(ModItems.TITANIUM_PICKAXE.get()); output.accept(ModItems.EQUIPMENT_CONSOLE.get());
                section(output, "suit"); output.accept(ModItems.SPACE_HELMET.get());
                output.accept(ModItems.SPACE_CHESTPLATE.get()); output.accept(ModItems.SPACE_LEGGINGS.get()); output.accept(ModItems.SPACE_BOOTS.get());
                output.accept(ModItems.OXYGEN_TANK.get());
                section(output, "technology");
                output.accept(ModItems.THRUSTER.get()); output.accept(ModItems.NAVIGATION_CORE.get());
                section(output, "rockets"); output.accept(ModItems.LAUNCH_PAD.get());
                output.accept(ModItems.TIER1_ROCKET.get()); output.accept(ModItems.TIER2_ROCKET.get()); output.accept(ModItems.TIER3_ROCKET.get());
                section(output, "schematics");
                output.accept(ModItems.LUNAR_SCHEMATIC.get()); output.accept(ModItems.MARTIAN_SCHEMATIC.get());
                ModSites.SCHEMATICS.values().forEach(item -> output.accept(item.get()));
                output.accept(ModItems.GALAXY_EMBLEM.get());
            }).build());

    private static void section(CreativeModeTab.Output output, String section) {
        var marker = ModItems.GALAXY_EMBLEM.get().getDefaultInstance();
        marker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.translatable("section.galaxycraft." + section)
                .withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD));
        output.accept(marker);
    }

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(GalaxyCraft.MOD_ID, path); }
    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name)));
    }
    private ModPresentation() {}
}
