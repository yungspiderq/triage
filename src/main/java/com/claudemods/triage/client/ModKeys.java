package com.claudemods.triage.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Two keys.
 *
 * <p>H on this loader without the argument the NeoForge tree has to have: the physics mod
 * that also wants H is a NeoForge mod, so nothing here is fighting over it.
 */
public final class ModKeys {
    /** H for health. Unbound in vanilla, and next to the hand you keep on the keys. */
    public static final KeyMapping OPEN_BODY = new KeyMapping(
            "key.triage.body", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.triage");

    /** Held, not tapped: giving up should never happen by brushing a key. */
    public static final KeyMapping GIVE_UP = new KeyMapping(
            "key.triage.give_up", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.triage");

    private ModKeys() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_BODY);
        KeyBindingHelper.registerKeyBinding(GIVE_UP);
    }
}
