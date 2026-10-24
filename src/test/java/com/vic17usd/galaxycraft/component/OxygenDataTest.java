package com.vic17usd.galaxycraft.component;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica transiciones e integridad de los formatos persistente y de red. */
class OxygenDataTest {
    @Test
    void consumoSeSaturaSinModificarElOriginal() {
        OxygenData initial = new OxygenData(2, 10);
        assertEquals(new OxygenData(1, 10), initial.consume(1));
        assertEquals(new OxygenData(0, 10), initial.consume(30));
        assertEquals(2, initial.current());
        assertThrows(IllegalArgumentException.class, () -> initial.consume(-1));
    }

    @Test
    void recargaRespetaLaCapacidadIndividual() {
        assertEquals(new OxygenData(42, 42), new OxygenData(0, 42).refill());
    }

    @Test
    void persistenciaConservaAmbosValores() {
        OxygenData initial = new OxygenData(13, 90);
        var json = OxygenData.CODEC.encodeStart(JsonOps.INSTANCE, initial).getOrThrow();
        assertEquals(initial, OxygenData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void persistenciaRechazaDatosInvalidos() {
        for (String json : new String[]{"{\"current\":-1,\"maximum\":10}",
                "{\"current\":11,\"maximum\":10}", "{\"current\":0,\"maximum\":0}"}) {
            assertTrue(OxygenData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent());
        }
    }

    @Test
    void sincronizacionDeRedConservaAmbosValores() {
        var buffer = Unpooled.buffer();
        try {
            OxygenData initial = new OxygenData(7, 1200);
            OxygenData.STREAM_CODEC.encode(buffer, initial);
            assertEquals(initial, OxygenData.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
