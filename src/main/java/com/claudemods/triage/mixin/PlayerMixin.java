package com.claudemods.triage.mixin;

import com.claudemods.triage.compat.Poses;
import com.claudemods.triage.event.DamageEvents;
import com.claudemods.triage.event.LivingDamageEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Where a blow lands, and a pose the player does not get a say in.
 *
 * <p>Not written out with the player on purpose, the same as on NeoForge: the pose is a
 * consequence of what the body is doing, and whatever put somebody on the floor is in the
 * save and will put them back there within a second of loading. Keeping the pose as well
 * would only add a second place for the two to disagree.
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements Poses.Forced {
    @Unique
    @Nullable
    private Pose triage$forced;

    @Override
    public void triage$setForcedPose(@Nullable Pose pose) {
        triage$forced = pose;
    }

    @Override
    @Nullable
    public Pose triage$forcedPose() {
        return triage$forced;
    }

    /**
     * Vanilla works out a pose from what the player is doing every tick, so holding one
     * means answering that question instead of it, not once but every time it is asked.
     */
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void triage$holdPose(CallbackInfo callback) {
        if (triage$forced != null) {
            ((Entity) (Object) this).setPose(triage$forced);
            callback.cancel();
        }
    }

    /**
     * A blow, after armour and enchantments and before absorption is spent.
     *
     * <p>On Player rather than on LivingEntity, which is not a detail: Player carries its own
     * copy of actuallyHurt and never calls the one above it, so the same injection on
     * LivingEntity is dead code for the only entity this mod is about. That is what it was,
     * and body parts took nothing at all.
     *
     * <p>The second store into the damage argument is the point NeoForge fires its event at:
     * armour and resistance have had their say, absorption has not, and the mod spends that
     * itself because it is about to set the damage to nothing. Returning 0 is how it says so -
     * vanilla checks for exactly that and does nothing further.
     */
    @ModifyVariable(method = "actuallyHurt", at = @At(value = "STORE", ordinal = 1), argsOnly = true)
    private float triage$onDamage(float amount, DamageSource source) {
        LivingDamageEvent.Pre event = new LivingDamageEvent.Pre((Player) (Object) this, source, amount);
        DamageEvents.onDamage(event);
        return event.getNewDamage();
    }
}
