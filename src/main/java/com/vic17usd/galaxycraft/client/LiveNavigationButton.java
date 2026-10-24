package com.vic17usd.galaxycraft.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Reevalúa la telemetría: el menú empieza con N0 hasta recibir los datos del servidor. */
public abstract class LiveNavigationButton extends Button {
    private final BooleanSupplier enabled;
    private final BooleanSupplier locked;
    private boolean lockedNow;
    protected LiveNavigationButton(int x,int y,int size,Component label,OnPress action,
            BooleanSupplier enabled,BooleanSupplier locked) {
        super(x,y,size,size,label,action,DEFAULT_NARRATION);
        this.enabled = enabled; this.locked = locked; refreshState();
    }
    public final void refreshState() { active = enabled.getAsBoolean(); lockedNow = locked.getAsBoolean(); }
    public final boolean isLocked() { return lockedNow; }
    @Override public boolean mouseClicked(double x,double y,int button) {
        // También se valida al pulsar, aunque haya llegado un paquete desde el último fotograma.
        refreshState(); return super.mouseClicked(x,y,button);
    }
    @Override public boolean keyPressed(int key,int scanCode,int modifiers) {
        refreshState(); return super.keyPressed(key,scanCode,modifiers);
    }
}
