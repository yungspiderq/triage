package com.claudemods.triage.mixin;

import com.claudemods.triage.event.DownedEvents;
import com.claudemods.triage.event.LivingEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Something deciding who to go after, before it has decided.
 *
 * <p>What this is for: a player on the floor looks dead, so things treat them as dead and
 * go and find somebody standing. Without it, going down is not a last chance at all -
 * whatever put you there finishes you while you watch, and the medic the whole feature
 * exists for never arrives.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void triage$onChangeTarget(LivingEntity target, CallbackInfo callback) {
        LivingEvents.LivingChangeTargetEvent event = new LivingEvents.LivingChangeTargetEvent(target);
        DownedEvents.onChangeTarget(event);
        if (event.isCanceled()) {
            callback.cancel();
        }
    }
}
