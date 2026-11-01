package com.claudemods.triage.compat;

import com.claudemods.triage.Config;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Lying on the floor, when somebody has installed a physics engine that can do it
 * properly.
 *
 * <p>Triage puts a downed player face down by forcing a swimming pose on them. That is a
 * pose, and it is the giveaway: it is the same shape whether you were shot off a roof or
 * quietly bled out standing still, it does not touch anything on the way down, and the
 * moment you look at it next to a body that actually fell it reads as an animation.
 * <a href="https://modrinth.com/project/I3mWDgfy">Sable: Ragdolls</a> and
 * <a href="https://modrinth.com/mod/ragdollified">Ragdollified</a> can both drop the real
 * thing, and both have the one option that matters here - a ragdoll the player is not
 * allowed to get up from, which is Triage's oldest rule written in somebody else's mod.
 *
 * <p>Nothing about this is required. Triage has never had a dependency and does not have
 * one now: without either mod every call here is a boolean check that returns false, and
 * the pose is still the pose. That is what the split between this class and the hook
 * classes is for - this one names no class from another mod, so it loads everywhere, and
 * the others are only ever reached through a branch that has already checked the mod is
 * there.
 */
public final class Ragdolls {
    private static final String SABLE = "sable_player_ragdoll";
    private static final String RAGDOLLIFIED = "ragdollified";

    /** Worked out once, on first use rather than on class load: nothing is loaded yet then. */
    private static Boolean sable;
    private static Boolean ragdollified;

    /** Shorter than this and it was a stumble, not a fall. Vanilla forgives three. */
    private static final double MIN_DROP = 4.0;

    /** Where each ragdolled body has been: the top of its arc, and whether it is falling. */
    private record Fall(double peak, double lastY) {
    }

    private static final java.util.Map<java.util.UUID, Fall> FALLS = new java.util.HashMap<>();

    private Ragdolls() {
    }

    public static boolean present() {
        return sable() || ragdollified();
    }

    private static boolean sable() {
        if (sable == null) {
            ModList mods = ModList.get();
            if (mods == null) {
                // Too early to know. Answering no is right for now and must not be
                // remembered: caching it would turn one unlucky call order into a mod
                // that is installed and never used, which is the worst of both.
                return false;
            }
            sable = mods.isLoaded(SABLE);
        }
        return sable;
    }

    private static boolean ragdollified() {
        if (ragdollified == null) {
            ModList mods = ModList.get();
            if (mods == null) {
                return false;
            }
            ragdollified = mods.isLoaded(RAGDOLLIFIED);
        }
        return ragdollified;
    }

    /** Called once from the mod constructor. Hooks nothing at all when the mods are absent. */
    public static void init() {
        if (sable()) {
            RagdollHooks.register();
        }
    }

    /**
     * Puts a downed player on a ragdoll, or leaves them where they are.
     *
     * <p>Idempotent on purpose, and called both the moment somebody goes down and once a
     * second afterwards. Anything that takes a player off their doll - the mod expiring
     * it, a carry ending, an addon - is undone within a second, so there is no state to
     * keep in step and nothing to go stale.
     *
     * @return true when the player is on a ragdoll now, whether or not this call put
     *         them there. False means the caller still owns the pose.
     */
    public static boolean drop(ServerPlayer player) {
        if (!Config.values().ragdoll) {
            return false;
        }
        if (sable()) {
            return RagdollHooks.drop(player);
        }
        return ragdollified() && RagdollifiedHooks.drop(player);
    }

    /**
     * Whether a physics engine has hold of this body rather than the pose.
     *
     * <p>Asked by anything that would otherwise assume a player is the right way up. A
     * standing body lands on its feet; one that has been tumbling through the air lands
     * on whatever happened to be underneath.
     */
    public static boolean holding(ServerPlayer player) {
        if (sable() && RagdollHooks.holding(player)) {
            return true;
        }
        return ragdollified() && RagdollifiedHooks.holding(player);
    }

    /**
     * How far a ragdolled body has just fallen, or zero if it has not landed.
     *
     * <p>Nothing else deals a landing. A player riding their own doll is a passenger, and
     * a passenger takes no fall damage - dropped a hundred and twenty blocks, a ragdolled
     * player arrives with a scratch. The mod that owns the physics does not hurt them
     * either; its only damage hook suppresses suffocation.
     *
     * <p>So the fall is measured here, off the one thing that needs no cooperation from
     * anybody: the player's own height, sampled once a second along with everything else.
     * Remember the highest point since the doll was dropped, notice the second the
     * descent stops, and the difference is the drop.
     *
     * <p>Once a second is coarse, and it is enough: nothing survivable falls far enough
     * to be missed between two samples, and a short hop does not clear the threshold.
     */
    public static double landing(ServerPlayer player) {
        if (!holding(player)) {
            FALLS.remove(player.getUUID());
            return 0.0;
        }
        double y = player.getY();
        Fall fall = FALLS.get(player.getUUID());
        if (fall == null) {
            FALLS.put(player.getUUID(), new Fall(y, y));
            return 0.0;
        }
        double peak = Math.max(fall.peak, y);
        double dropped = peak - y;
        // At rest, and lower than it has been. Asking to have *seen* the descent in the
        // previous sample missed half of all landings: a doll that bounces, or that
        // finishes its fall between two ticks, never presents that pattern. Coming to a
        // stop below the top of the arc is the whole of what a landing is.
        if (Math.abs(fall.lastY - y) <= 0.5 && dropped >= MIN_DROP) {
            FALLS.put(player.getUUID(), new Fall(y, y));
            return dropped;
        }
        FALLS.put(player.getUUID(), new Fall(peak, y));
        return 0.0;
    }

    public static void forget(java.util.UUID playerId) {
        FALLS.remove(playerId);
    }

    /**
     * Lets go of the ragdoll, if there is one.
     *
     * <p>Not gated on the config: a doll that exists has to be releasable even if the
     * setting was turned off underneath it, or a player who was down when the pack
     * changed its mind stays face down forever.
     */
    public static void getUp(ServerPlayer player) {
        if (sable()) {
            RagdollHooks.getUp(player);
        }
        if (ragdollified()) {
            RagdollifiedHooks.getUp(player);
        }
    }
}
