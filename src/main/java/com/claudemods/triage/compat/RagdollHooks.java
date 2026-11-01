package com.claudemods.triage.compat;

import net.minecraft.server.level.ServerPlayer;

/**
 * The ragdoll integration, which does nothing on 1.20.1.
 *
 * <p>Not a decision about whether it is a good feature. Sable: Ragdolls has no 1.20.1
 * build, so there is nothing here to compile against and nothing at runtime to talk to.
 * The other line keeps the real one; this keeps the shape of it so that everything above
 * carries on asking the same four questions and getting an honest no.
 *
 * <p>Which is exactly what the caller already does when the mod is absent on 1.21.1 -
 * {@link com.claudemods.triage.body.Ragdolls} has always had to work without it, because
 * most people do not have it installed. So the floor under this is already tested: a
 * player who goes down without a physics mod gets the pose, and that path is the one
 * every 1.20.1 player will take.
 *
 * <p>If a 1.20.1 build of it ever appears, this is the one file that has to change.
 */
public final class RagdollHooks {
    private RagdollHooks() {
    }

    static void register() {
        // Nothing to listen to.
    }

    static boolean drop(ServerPlayer player) {
        return false;
    }

    static boolean holding(ServerPlayer player) {
        return false;
    }

    static void getUp(ServerPlayer player) {
        // Nobody is on a ragdoll, so nobody has to be taken off one.
    }
}
