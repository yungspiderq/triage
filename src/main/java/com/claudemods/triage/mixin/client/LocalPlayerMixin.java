package com.claudemods.triage.mixin.client;

import com.claudemods.triage.client.ClientEvents;
import com.claudemods.triage.client.ClientHooks;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * What a broken leg does to the keys.
 *
 * <p>Caught after the input has been read and before the player moves on it, which is the
 * one window where a limp can be a limp rather than a change to how fast a body walks.
 * Nothing is taken away: what you asked for is still what happens, less than you asked for.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Shadow
    public Input input;

    @Inject(method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/Input;tick(ZF)V",
                    shift = At.Shift.AFTER))
    private void triage$movementInput(CallbackInfo callback) {
        ClientEvents.onMovementInput(new ClientHooks.MovementInputUpdateEvent(input));
    }
}
