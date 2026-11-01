package com.claudemods.triage.compat;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Ragdollified, reached without naming it.
 *
 * <p>Ragdollified is Forge/NeoForge only and is All Rights Reserved, so Triage does not
 * compile against it and does not ship it. Its API is small and has kept the same shape
 * across 1.20.1 and 1.21.1, which makes it a good candidate for reflection: the two calls
 * that matter are "start a player ragdoll" and "stop one", and both are static.
 *
 * <p>Everything here is only touched after {@link Ragdolls} has confirmed the mod is
 * loaded. A failed lookup is remembered, so a version that moved a method degrades to the
 * pose rather than throwing once a second.
 */
final class RagdollifiedHooks {
    private static final String SERVER_API = "com.raiiiden.ragdollified.api.RagdollifiedServerApi";
    private static final String API = "com.raiiiden.ragdollified.api.RagdollifiedApi";

    /**
     * Long enough that Triage's own downed clock always ends first. If it does expire,
     * the next second's {@link #drop} sees no ragdoll and starts another.
     */
    private static final int TICKS = 20 * 600;

    private static boolean resolved;
    private static Method start;
    private static Method stop;
    private static Method has;

    private RagdollifiedHooks() {
    }

    static boolean drop(ServerPlayer player) {
        if (!resolve()) {
            return false;
        }
        try {
            if ((boolean) has.invoke(null, player)) {
                return true;
            }
            // Riding anything means somebody or something else owns where this body is.
            if (player.isPassenger()) {
                return false;
            }
            // The fourth and fifth arguments are the initial facing. The body falls from
            // there under its own physics, which is the point of handing it over.
            start.invoke(null, player, player.getDeltaMovement(), player.getYRot(), player.getXRot(), TICKS);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean holding(ServerPlayer player) {
        if (!resolve()) {
            return false;
        }
        try {
            return (boolean) has.invoke(null, player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void getUp(ServerPlayer player) {
        if (!resolve()) {
            return;
        }
        try {
            if ((boolean) has.invoke(null, player)) {
                stop.invoke(null, player.getId());
            }
        } catch (Throwable ignored) {
            // Nothing to do: the body is already free, or the API is not the one we know.
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return start != null && stop != null && has != null;
        }
        resolved = true;
        try {
            Class<?> server = Class.forName(SERVER_API);
            start = server.getMethod("startPlayerRagdoll", ServerPlayer.class, Vec3.class,
                    float.class, float.class, int.class);
            stop = server.getMethod("stopRagdoll", int.class);
            Class<?> api = Class.forName(API);
            has = api.getMethod("hasRagdoll", Entity.class);
            return true;
        } catch (Throwable ignored) {
            start = null;
            stop = null;
            has = null;
            return false;
        }
    }
}
