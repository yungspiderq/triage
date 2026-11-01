package com.claudemods.triage.compat;

import com.claudemods.triage.body.Carry;
import dev.leo.sableplayerragdoll.api.DespawnCondition;
import dev.leo.sableplayerragdoll.api.RagdollAPI;
import dev.leo.sableplayerragdoll.api.RagdollLaunchOptions;
import dev.leo.sableplayerragdoll.api.RagdollLimbOptions;
import dev.leo.sableplayerragdoll.api.RagdollPoseSnapshot;
import dev.leo.sableplayerragdoll.api.RagdollSession;
import dev.leo.sableplayerragdoll.api.RagdollStartEvent;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The only class in Triage that names Sable: Ragdolls, and the only one that must never
 * be loaded without it.
 *
 * <p>Java loads a class the first time something actually touches it, so keeping every
 * reference to another mod behind one door means the door is only ever opened by
 * {@link Ragdolls}, which has already checked. There are no annotations on this class for
 * the same reason: an {@code @EventBusSubscriber} would be found by the scanner and
 * loaded on every install, including the ones with no physics engine in them.
 */
final class RagdollHooks {
    /** The API takes metres per second; a player's known movement is per tick. */
    private static final double PER_TICK_TO_PER_SECOND = 20.0;

    private RagdollHooks() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(RagdollHooks::onRagdollStart);
    }

    /**
     * A body over your shoulder rides you. Going limp underneath one is a question the
     * physics has no good answer to, and the mod's own key is the easiest way to ask it,
     * so the answer is no. Set them down first.
     *
     * <p>Only carriers. A player going down is dropped through this same event, and
     * somebody on the floor is carrying nobody, so their own fall is never caught here.
     */
    private static void onRagdollStart(RagdollStartEvent event) {
        if (Carry.carrying(event.player()) != null) {
            event.setCanceled(true);
        }
    }

    /** @return true when the player is on a ragdoll afterwards */
    static boolean drop(ServerPlayer player) {
        if (RagdollAPI.isRagdolled(player)) {
            return true;
        }
        // Riding anything means somebody or something else owns where this body is.
        if (player.isPassenger()) {
            return false;
        }
        return RagdollAPI.launch(player,
                player.getKnownMovement().scale(PER_TICK_TO_PER_SECOND),
                RagdollLaunchOptions.builder()
                        // The whole reason this is worth wiring up. A downed player has
                        // never been able to pick themselves up in this mod; now they
                        // cannot shrug the doll off either, and the two rules are the
                        // same rule rather than two that have to be kept in agreement.
                        .lockDismount(true)
                        // Triage owns how long somebody lies there, down to the second,
                        // and it is drawn on their screen. A doll that quietly expired
                        // on its own clock would stand them up mid-count.
                        .despawnConditions(List.of(DespawnCondition.never()))
                        .build(),
                // Handing over a pose makes the launch immediate. Without one the mod
                // asks the client what shape the player is in and lands a tick or two
                // later, and the one moment that cannot wait is the moment of falling.
                new RagdollPoseSnapshot(RagdollLimbOptions.defaults(), player.yBodyRot)) != null;
    }

    static boolean holding(ServerPlayer player) {
        return RagdollAPI.isRagdolled(player);
    }

    static void getUp(ServerPlayer player) {
        RagdollSession session = RagdollAPI.activeSession(player);
        if (session == null) {
            return;
        }
        // Unlocked first. The lock is what stops the player letting go; releasing is us
        // letting go of them, and a session that refuses its own release would leave a
        // revived player lying in a heap.
        session.setDismountLocked(false);
        session.release();
    }
}
