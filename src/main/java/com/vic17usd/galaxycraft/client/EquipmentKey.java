package com.vic17usd.galaxycraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Tecla reasignable en Opciones → Controles; abrir no necesita tener la terminal en la mano. */
public final class EquipmentKey {
    public static final KeyMapping OPEN = new KeyMapping("key.galaxycraft.equipment", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, "key.categories.galaxycraft");
    public static final KeyMapping NAVIGATE = new KeyMapping("key.galaxycraft.navigation",InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,"key.categories.galaxycraft");
    private EquipmentKey() {}
}
