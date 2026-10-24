package com.vic17usd.galaxycraft.navigation;

import com.vic17usd.galaxycraft.client.LiveNavigationButton;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regresión cliente sin OpenGL: simula los paquetes que llegan después de crear la pantalla. */
class LiveNavigationButtonTest {
    private LiveNavigationButton system(AtomicInteger tier) {
        return new LiveNavigationButton(0,0,48,Component.literal("Sol"),button -> {},
                () -> tier.get() >= 1,() -> tier.get() < 1) {
            @Override protected void renderWidget(GuiGraphics gui,int x,int y,float partialTick) { refreshState(); }
        };
    }
    @Test void unBotonCreadoAntesDeLaSincronizacionSeHabilitaAlLlegarN3() {
        var tier = new AtomicInteger(0); var button = system(tier);
        assertFalse(button.active); assertTrue(button.isLocked());
        tier.set(3); button.refreshState();
        assertTrue(button.active); assertFalse(button.isLocked());
    }
    @Test void laEntradaTambienActualizaElEstadoSinEsperarOtroFotograma() {
        var tier = new AtomicInteger(0); var button = system(tier);
        tier.set(3);
        // Fuera del botón evita reproducir audio en una prueba sin cliente gráfico.
        assertFalse(button.mouseClicked(-10,-10,0)); assertTrue(button.active);
        tier.set(0); button.mouseClicked(-10,-10,0);
        assertFalse(button.active); assertTrue(button.isLocked());
    }
    @Test void losPlanetasConservanLaPrevisualizacionYActualizanSuCandado() {
        var tier = new AtomicInteger(0);
        var button = new LiveNavigationButton(0,0,24,Component.literal("Neptuno"),pressed -> {},
                () -> true,() -> tier.get() < 3) {
            @Override protected void renderWidget(GuiGraphics gui,int x,int y,float partialTick) { refreshState(); }
        };
        assertTrue(button.active); assertTrue(button.isLocked());
        tier.set(3); button.refreshState(); assertTrue(button.active); assertFalse(button.isLocked());
    }
}
