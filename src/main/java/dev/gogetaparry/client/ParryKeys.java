package dev.gogetaparry.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Tastenbelegung (aenderbar unter Optionen -> Steuerung). */
public final class ParryKeys {
    public static final KeyMapping SWITCH_HUD = new KeyMapping(
            "key.gogetaparry.switch_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.gogetaparry");

    private ParryKeys() {
    }
}
