package com.vic17usd.galaxycraft.component;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Estado inmutable: una unidad representa un segundo de respiración. */
public record OxygenData(int current, int maximum) {
    public static final OxygenData FULL_TANK = new OxygenData(1200, 1200);

    // Valida también las relaciones entre campos al leer datos persistentes.
    public static final Codec<OxygenData> CODEC = Codec.mapPair(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("current"),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("maximum")
    ).codec().comapFlatMap(pair -> pair.getFirst() <= pair.getSecond()
            ? DataResult.success(new OxygenData(pair.getFirst(), pair.getSecond()))
            : DataResult.error(() -> "El oxígeno actual supera la capacidad"),
            oxygen -> Pair.of(oxygen.current(), oxygen.maximum()));

    // Sincroniza el componente del ItemStack sin paquetes personalizados ni NBT manual.
    public static final StreamCodec<ByteBuf, OxygenData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OxygenData::current,
            ByteBufCodecs.VAR_INT, OxygenData::maximum, OxygenData::new);

    public OxygenData {
        if (maximum < 1 || current < 0 || current > maximum) {
            throw new IllegalArgumentException("Cantidad de oxígeno fuera de rango");
        }
    }

    public OxygenData consume(int amount) {
        if (amount < 0) throw new IllegalArgumentException("El consumo no puede ser negativo");
        return new OxygenData(Math.max(0, current - amount), maximum);
    }

    public OxygenData refill() {
        return new OxygenData(maximum, maximum);
    }
}
