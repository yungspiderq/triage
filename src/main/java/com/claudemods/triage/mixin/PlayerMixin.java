package com.claudemods.triage.mixin;

import com.claudemods.triage.compat.Poses;
import com.claudemods.triage.event.DamageEvents;
import com.claudemods.triage.event.LivingEvents;
import com.claudemods.triage.data.BodyStore;
import com.claudemods.triage.event.BreakSpeed;
import com.claudemods.triage.event.PlayerEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where a blow lands, and three things a player carries here that a player carries for
 * free on the other two.
 *
 * <p>The body itself, because Fabric on 1.20.1 has no attachments and no capabilities - it
 * is a field on the player, written out with the player, and read back with them. A pose
 * they do not get a say in. And the answer to how fast they are digging, which Forge asks
 * as an event and nobody else asks at all.
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements Poses.Forced, BodyStore.Holder {
    @Unique
    private com.claudemods.triage.body.BodyHealth triage$body = new com.claudemods.triage.body.BodyHealth();

    @Unique
    @Nullable
    private Pose triage$forced;

    @Override
    public com.claudemods.triage.body.BodyHealth triage$body() {
        return triage$body;
    }

    @Override
    public void triage$setBody(com.claudemods.triage.body.BodyHealth body) {
        triage$body = body;
    }

    @Override
    public void triage$setForcedPose(@Nullable Pose pose) {
        triage$forced = pose;
    }

    @Override
    @Nullable
    public Pose triage$forcedPose() {
        return triage$forced;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void triage$save(CompoundTag tag, CallbackInfo callback) {
        BodyStore.save(this, tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void triage$load(CompoundTag tag, CallbackInfo callback) {
        BodyStore.load(this, tag);
    }

    /**
     * Vanilla works out a pose from what the player is doing every tick, so holding one
     * means answering that question instead of it - not once, but every time it is asked.
     */
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void triage$holdPose(CallbackInfo callback) {
        if (triage$forced != null) {
            ((Entity) (Object) this).setPose(triage$forced);
            callback.cancel();
        }
    }

    /**
     * How fast this player is getting through this block.
     *
     * <p>At the end rather than the start: what the rule wants is the speed everything else
     * arrived at - tool, haste, being underwater - and then its own share taken off that.
     */
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void triage$destroySpeed(BlockState state, CallbackInfoReturnable<Float> callback) {
        PlayerEvent.BreakSpeed event = new PlayerEvent.BreakSpeed(
                (Player) (Object) this, state, callback.getReturnValueF());
        BreakSpeed.onBreakSpeed(event);
        if (event.getNewSpeed() != callback.getReturnValueF()) {
            callback.setReturnValue(event.getNewSpeed());
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
        LivingEvents.LivingHurtEvent event =
                new LivingEvents.LivingHurtEvent((Player) (Object) this, source, amount);
        DamageEvents.onDamage(event);
        return event.getAmount();
    }
}
