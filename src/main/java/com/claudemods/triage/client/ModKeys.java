package com.claudemods.triage.client;

import com.claudemods.triage.TriageMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TriageMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ModKeys {
    /** H for health. Unbound in vanilla, and next to the hand you keep on the keys. */
    public static final KeyMapping OPEN_BODY = new KeyMapping(
            "key.triage.body", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            bodyKey(), "key.categories.triage");

    /**
     * H, unless something else already owns it.
     *
     * <p>Sable: Ragdolls puts "go limp" on H too, and two mods sharing one default is a
     * conflict a player has to go and untangle in the options before either key does
     * anything. Ours is the one that moves: this key opens a screen that also has a
     * button next to the inventory, and theirs is the whole mod.
     *
     * <p>Only the default moves. Anybody who has already bound this to something keeps
     * what they bound - the setting is saved on their end, not here - so installing the
     * physics mod later still needs one trip to the options.
     */
    private static int bodyKey() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded("sable_player_ragdoll")
                ? GLFW.GLFW_KEY_J : GLFW.GLFW_KEY_H;
    }

    /** Held, not tapped: giving up should never happen by brushing a key. */
    public static final KeyMapping GIVE_UP = new KeyMapping(
            "key.triage.give_up", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, "key.categories.triage");

    private ModKeys() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BODY);
        event.register(GIVE_UP);
    }
}
