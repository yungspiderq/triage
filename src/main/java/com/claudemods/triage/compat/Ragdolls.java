package com.claudemods.triage.compat;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Physics for a body that has stopped holding itself up - when there is any.
 *
 * <p>On NeoForge this talks to Sable: Ragdolls, and a player who goes down goes down like
 * a dropped coat instead of standing there asleep. There is no such mod on Fabric, so
 * every answer here is no and the mod behaves the way it does on NeoForge without it
 * installed: the pose changes, nothing falls over.
 *
 * <p>The class stays rather than the calls being deleted from the shared half, and that is
 * the whole point of it. Seven places in the mod ask whether somebody is on a doll; each
 * of them would otherwise need a version for each loader, and they would drift. When
 * something like Sable turns up on Fabric this is the only file that has to change.
 */
public final class Ragdolls {
    private Ragdolls() {
    }

    public static boolean present() {
        return false;
    }

    public static void init() {
    }

    /** @return whether anything actually caught the body. Nothing does here. */
    public static boolean drop(ServerPlayer player) {
        return false;
    }

    public static boolean holding(ServerPlayer player) {
        return false;
    }

    /** How far the body fell before it stopped, in blocks. Nothing falls, so nothing fell. */
    public static double landing(ServerPlayer player) {
        return 0.0;
    }

    public static void forget(UUID playerId) {
    }

    public static void getUp(ServerPlayer player) {
    }
}
