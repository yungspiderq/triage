package com.claudemods.triage.mixin;

import com.claudemods.triage.event.DamageEvents;
import com.claudemods.triage.event.DiseaseEvents;
import com.claudemods.triage.event.LivingEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two moments this mod needs that Fabric has no callback for: being healed, and finishing
 * whatever was being eaten or drunk.
 *
 * <p>Being hit is not here, and the reason is worth writing down. Player overrides
 * actuallyHurt with its own copy and never calls super, so a mixin on this class is dead
 * code for the only entity this mod cares about. It lives on Player instead - see
 * PlayerMixin - which is also the narrower place for it. Both of these two are declared
 * only on LivingEntity and Player leaves them alone.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    protected ItemStack useItem;

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void triage$onHeal(float amount, CallbackInfo callback) {
        LivingEvents.LivingHealEvent event =
                new LivingEvents.LivingHealEvent((LivingEntity) (Object) this, amount);
        DamageEvents.onHeal(event);
        if (event.isCanceled()) {
            callback.cancel();
        }
    }

    /**
     * The last swallow of whatever was being eaten or drunk, while it is still in hand.
     *
     * <p>Head of the method rather than the end of it, because the stack is about to be
     * spent and what was eaten is the whole question being asked.
     */
    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void triage$onFinishUsing(CallbackInfo callback) {
        DiseaseEvents.onFinishEating(new LivingEvents.LivingEntityUseItemEvent.Finish(
                (LivingEntity) (Object) this, useItem));
    }
}
