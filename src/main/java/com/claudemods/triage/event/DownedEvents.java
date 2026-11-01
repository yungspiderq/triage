package com.claudemods.triage.event;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.Config;
import com.claudemods.triage.body.Body;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import com.claudemods.triage.event.LivingEvents.LivingChangeTargetEvent;

/**
 * Mobs lose interest in a player who is already on the floor.
 *
 * <p>Without this, going down is not a last chance at all: whatever put you there
 * simply finishes the job while you lie still and watch, and the medic the whole
 * feature exists for never gets to arrive. You look dead, so they treat you as dead.
 */
public final class DownedEvents {
    private DownedEvents() {
    }

    /** Stops anything picking a downed player as its next target. */
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof ServerPlayer player && ignored(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * Drops whatever is already chasing them. Called once a second from the body tick,
     * because the event above only catches new decisions - a zombie that had you before
     * you fell would keep swinging.
     */
    public static void shakeOffPursuers(ServerPlayer player) {
        if (!ignored(player)) {
            return;
        }
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24.0))) {
            LivingEntity target = mob.getTarget();
            if (target == player) {
                mob.setTarget(null);
            }
        }
    }

    private static boolean ignored(ServerPlayer player) {
        Config.Values config = Config.values();
        return config.downed && config.downedIgnoredByMobs && Body.of(player).downed();
    }
}
