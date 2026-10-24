package com.vic17usd.galaxycraft.registry;

import com.vic17usd.galaxycraft.GalaxyCraft;
import com.vic17usd.galaxycraft.component.OxygenData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registro de componentes persistentes y sincronizados de los ítems. */
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, GalaxyCraft.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OxygenData>> OXYGEN_LEVEL =
            COMPONENTS.register("oxygen_level", () -> DataComponentType.<OxygenData>builder()
                    .persistent(OxygenData.CODEC)
                    .networkSynchronized(OxygenData.STREAM_CODEC)
                    .build());

    private ModDataComponents() {}
}
