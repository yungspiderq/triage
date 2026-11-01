package com.claudemods.triage.mixin.client;

import com.claudemods.triage.client.ClientHooks;
import com.claudemods.triage.client.Deafness;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A blast beside your head, and the hour afterwards.
 *
 * <p>Every sound on its way out is swapped for a quieter, duller copy of itself while the
 * ears are still ringing. Here rather than on the volume slider because deafness is not
 * quietness: what goes is the top of the sound, and what is left is the part you feel.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @ModifyVariable(method = "play", at = @At("HEAD"), argsOnly = true)
    private SoundInstance triage$muffle(SoundInstance sound) {
        ClientHooks.PlaySoundEvent event = new ClientHooks.PlaySoundEvent(sound);
        Deafness.onSound(event);
        return event.getSound();
    }
}
