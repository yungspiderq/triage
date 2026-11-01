package com.claudemods.triage;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

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
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, TriageMod.MODID);

    /**
     * Somebody talking, close, and not making out the words.
     *
     * <p>Twenty of them, and they are real speech with the voice taken out of it: the
     * mouth shapes are still in the spectrum, nothing is periodic any more. That is
     * physically what a whisper is, and it is why they are not quite intelligible - which
     * is the point. The words are on the screen. This is the thing underneath them.
     */
    public static final RegistryObject<SoundEvent> WHISPER =
            SOUNDS.register("whisper", () -> SoundEvent.createVariableRangeEvent(TriageMod.id("whisper")));

    private ModSounds() {
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
