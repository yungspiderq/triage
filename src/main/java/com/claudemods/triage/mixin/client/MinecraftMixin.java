package com.claudemods.triage.mixin.client;

import com.claudemods.triage.client.ClientEvents;
import com.claudemods.triage.client.ClientHooks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A swing at somebody who is not there.
 *
 * <p>Caught before the packet goes out, because the server has never heard of that entity
 * and a client telling it otherwise is a client sending nonsense. The arm still moves - the
 * player really did swing - and the figure is simply gone, which is the whole of what
 * happens when you take a swing at a hallucination. Returning true here is what keeps the
 * swing while dropping the blow.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void triage$startAttack(CallbackInfoReturnable<Boolean> callback) {
        ClientHooks.InteractionKeyMappingTriggered event =
                new ClientHooks.InteractionKeyMappingTriggered(true);
        ClientEvents.onAttack(event);
        if (event.isCanceled()) {
            callback.setReturnValue(event.shouldSwingHand());
        }
    }
}
