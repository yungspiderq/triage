package com.claudemods.triage.med;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.item.MedicineItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.claudemods.triage.event.PlayerEvent;
import com.claudemods.triage.event.PlayerInteractEvent;

/**
 * The mod's own medical items handle their own right click, in {@link MedicineItem}.
 * This is only for the other kind: items from elsewhere that the config has declared
 * to be medicine. Those are ordinary items with ordinary uses, so they need Shift to
 * tell "bandage my arm" apart from "place this block".
 */
public final class MedicineEvents {
    private MedicineEvents() {
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (treat(event.getEntity(), event.getItemStack(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (treat(event.getEntity(), event.getItemStack(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Treatments.forget(event.getEntity().getUUID());
        com.claudemods.triage.body.Aid.forget(event.getEntity().getUUID());
    }

    /** The same Shift + right click, aimed at somebody else. */
    public static void onInteractWithPlayer(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ServerPlayer patient)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer healer) || !healer.isShiftKeyDown()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof MedicineItem || !Body.tracks(healer) || !Body.tracks(patient)) {
            return; // the mod's own items handle this themselves
        }
        MedicineKind kind = Medicine.kindOf(stack);
        if (kind == null) {
            return;
        }
        Treatments.begin(healer, patient, kind, event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    /**
     * @return true when this click was a treatment and should not also do whatever the
     *         item normally does
     */
    private static boolean treat(Player clicker, ItemStack stack, InteractionHand hand) {
        if (!(clicker instanceof ServerPlayer player) || !player.isShiftKeyDown() || !Body.tracks(player)) {
            return false;
        }
        if (stack.getItem() instanceof MedicineItem) {
            return false; // handled by the item itself, on any click
        }
        MedicineKind kind = Medicine.kindOf(stack);
        if (kind == null) {
            return false;
        }
        // Even a refused treatment counts as handled: the player meant to treat
        // something, and placing the block instead is not what they asked for.
        Treatments.begin(player, player, kind, hand);
        return true;
    }
}
