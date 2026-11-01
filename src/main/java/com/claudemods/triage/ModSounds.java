package com.claudemods.triage;

/*
 * Translated from the NeoForge tree by tools/registries.py. What each thing is, and why,
 * is the same file there; only the four lines that hand it to a registry are different.
 */

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The mod's own sounds.
 *
 * <p>One so far, and it is the one the mod could not borrow: somebody whispering.
 * Everything else it needs already exists in the game - a door, gravel, a heartbeat - and
 * using those is honest, because a hallucinated door should sound exactly like a door.
 * A voice cannot be borrowed from anything, and a symptom that is entirely text is a
 * symptom you read rather than one you have.
 */
public final class ModSounds {

    /**
     * Somebody talking, close, and not making out the words.
     *
     * <p>Twenty of them, and they are real speech with the voice taken out of it: the
     * mouth shapes are still in the spectrum, nothing is periodic any more. That is
     * physically what a whisper is, and it is why they are not quite intelligible - which
     * is the point. The words are on the screen. This is the thing underneath them.
     */
    public static final Supplier<SoundEvent> WHISPER =
            sound("whisper", SoundEvent.createVariableRangeEvent(TriageMod.id("whisper")));

    private ModSounds() {
    }

    /** Touching the class is what registers it; called once from the initialiser. */
    public static void register() {
    }

    private static Supplier<SoundEvent> sound(String name, SoundEvent event) {
        Registry.register(BuiltInRegistries.SOUND_EVENT, TriageMod.id(name), event);
        return () -> event;
    }
}
