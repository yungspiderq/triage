package com.claudemods.triage.event;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Carry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The gesture that picks somebody up, and the things a player cannot do while they are
 * holding one.
 *
 * <p>Shift and an empty hand, which is the mod's usual grammar for "I mean this" and
 * also the plain truth of it: you cannot lift somebody with a sword in your fist.
 */
@EventBusSubscriber(modid = TriageMod.MODID)
public final class CarryEvents {
    private CarryEvents() {
    }

    @SubscribeEvent
    public static void onInteractWithPlayer(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof ServerPlayer patient)
                || !(event.getEntity() instanceof ServerPlayer carrier)) {
            return;
        }
        if (!event.getItemStack().isEmpty()) {
            return;
        }

        // Hands already full: this gesture can only mean "put them down". The person you
        // are carrying is the entity your cursor is most likely to be on - they are on
        // your shoulder - so reaching for them used to land in the pick-up path and get
        // told you already had somebody, which is a dead end at the exact moment you are
        // visibly trying to empty your hands. Shift or no shift, one click sets them
        // down; if you were reaching for a different body, the next click picks that one
        // up. One pair of hands, one job.
        if (Carry.carrying(carrier) != null) {
            if (Carry.putDown(carrier)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
            return;
        }

        if (!carrier.isShiftKeyDown()) {
            return;
        }
        if (Carry.pickUp(carrier, patient)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    /**
     * Right-clicking anything with a free hand while carrying sets them down.
     *
     * <p>The key does it too, but a key is a thing you have to have been told about.
     * This is the gesture people try first, and it is honest about the state: your
     * hands are full, and the only thing they can still do is put this person down.
     * Which is also why it does not need Shift and does not open the chest.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer carrier)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().isEmpty()) {
            return;
        }
        if (Carry.putDown(carrier)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    /**
     * No swinging with somebody over your shoulder. The carry is meant to be a decision
     * with a cost rather than a free extra pair of hands, and the cost is that you are
     * carrying: put them down and fight, or keep walking and do not.
     */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Carry.carrying(player) != null) {
            event.setCanceled(true);
        }
    }

    /**
     * Either end dying or leaving ends the carry. The once-a-second sweep cannot do
     * this one: it runs off the body tick, which skips a player who is already dying.
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Carry.release(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Carry.release(player);
        }
    }
}
