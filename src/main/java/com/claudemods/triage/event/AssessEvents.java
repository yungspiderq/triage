package com.claudemods.triage.event;

import com.claudemods.triage.Config;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.Carry;
import com.claudemods.triage.item.ModItems;
import com.claudemods.triage.net.ShowBodyPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Looking at somebody without touching them.
 *
 * <p>The mod is named after sorting the wounded, and until now there was no way to sort
 * anybody: the only way to find out how badly a teammate was hurt was to start a
 * treatment on them and read the screen it opened, which costs a click, a choice and
 * sometimes an item. Now an empty hand and a right click is a look.
 *
 * <p>It is a snapshot. Nothing updates, nothing is spent, nothing is started. If you
 * want to know whether they have got worse since, look again.
 *
 * <p>A handful of blank tags counts as an empty hand, because the chart it opens is
 * where they get written on. See {@link com.claudemods.triage.body.Tags}.
 */
@EventBusSubscriber(modid = TriageMod.MODID)
public final class AssessEvents {
    private AssessEvents() {
    }

    @SubscribeEvent
    public static void onInteractWithPlayer(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof ServerPlayer patient)
                || !(event.getEntity() instanceof ServerPlayer looker)) {
            return;
        }
        // Shift and an empty hand is how you pick somebody up. Without shift it is how
        // you look at them, which is the gentler half of the same gesture.
        //
        // A handful of blank tags counts as an empty hand here, and nothing else does.
        // Reading a body and writing on it are one job; making the player put the tags
        // away to look and dig them out again to write would be asking them to fumble
        // through their hotbar in the one situation the mod is about.
        ItemStack held = event.getItemStack();
        if (looker.isShiftKeyDown() || !(held.isEmpty() || held.is(ModItems.TRIAGE_TAG.get()))) {
            return;
        }
        if (!Config.values().assess || looker == patient) {
            return;
        }
        // Hands full. While you are carrying somebody a right click means "set them
        // down" and nothing else: two meanings on one gesture, in the one situation
        // where it has to be obvious, is exactly the wrong place for it.
        if (Carry.carrying(looker) != null) {
            return;
        }
        if (!Body.tracks(patient)) {
            // Nothing to read: a creative or spectator body is not being tracked, and
            // saying so beats a click that does nothing.
            Body.notify(looker, Component.translatable("triage.msg.untracked_them"), ChatFormatting.GRAY, null);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        PacketDistributor.sendToPlayer(looker,
                new ShowBodyPayload(patient.getId(), Body.seen(Body.of(patient), looker == patient)));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }
}
