package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.claudemods.triage.ModParticles;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The visible half of bleeding: it runs down you and it ends up on the floor.
 *
 * <p>Stains are kept as a list of places and re-drawn as particles each second rather
 * than left as anything in the world. Nothing is placed, nothing is saved, and they dry
 * up on a timer - a mod about wounds should not be a mod that redecorates your base
 * permanently every time you take a hit.
 */
public final class BloodEffects {
    /**
     * Beyond this many stains in one world the oldest are dropped early.
     *
     * <p>Raised with the drying time: a mark a second for five minutes is three hundred
     * of them from one person, and a cap that a single casualty can reach on their own is
     * a cap that quietly shortens everybody else's trail.
     */
    private static final int MAX_STAINS = 2000;
    /** How far a stain is worth drawing for. */
    private static final double VISIBLE = 24.0;
    /** Two marks closer than this are one mark getting bigger. */
    private static final double POOLING = 0.55;

    /**
     * One mark on the floor, and how much of it there is.
     *
     * <p>Size is the difference between a trail and a pool, and it is the difference that
     * says which one somebody is looking at. Walking wounded leave a line of drops.
     * Somebody who stopped leaves one mark that kept growing - and where that mark is, is
     * where they stopped being able to walk.
     */
    private record Stain(Vec3 pos, long bornAt, long expiresAt, float size) {
    }

    private static final Map<ResourceKey<Level>, List<Stain>> STAINS = new HashMap<>();

    private BloodEffects() {
    }

    /** Once a second, for a player who is bleeding somewhere. */
    public static void bleed(ServerPlayer player, BodyHealth body) {
        Config.Values config = Config.values();
        if (!config.bloodParticles) {
            return;
        }
        ServerLevel level = player.serverLevel();

        int worst = 0;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            // A strapped limb has a wound but nothing coming out of it.
            if (!state.losingBlood()) {
                continue;
            }
            // Neither does a bandaged one, until the bandage has gone through. The model
            // has always hidden a dressed wound; the floor was still being dripped on,
            // which had a bandaged man leaving a trail with nothing showing on him.
            if (state.dressed && !state.soiled(config.dressingCleanSeconds * 20)) {
                continue;
            }
            worst = Math.max(worst, state.bleed);
            drip(level, player, part, state.bleed);
        }
        if (worst == 0) {
            return;
        }

        // A drip only becomes a stain when it has somewhere to land.
        if (player.onGround() && player.getRandom().nextFloat() < (worst >= PartState.BLEED_HEAVY ? 0.9F : 0.35F)) {
            stain(level, player.position(), config.bloodStainSeconds);
        }
    }

    /** Blood leaving the wound itself, at roughly the height of the part it came from. */
    private static void drip(ServerLevel level, ServerPlayer player, BodyPart part, int level0) {
        double height = switch (part) {
            case HEAD -> 0.92;
            case THORAX -> 0.72;
            case STOMACH -> 0.55;
            case LEFT_ARM, RIGHT_ARM -> 0.66;
            case LEFT_LEG, RIGHT_LEG -> 0.28;
        };
        // An artery does not drip. It throws, sideways, in time with something - and
        // that is the whole tell from across a clearing, so it is given real speed and
        // a beat rather than more drops.
        if (level0 >= PartState.BLEED_ARTERIAL && Config.values().arterialSpurt) {
            boolean beat = level.getGameTime() % 20 < 4;
            level.sendParticles(ModParticles.BLOOD.get(),
                    player.getX(), player.getY() + player.getBbHeight() * height, player.getZ(),
                    beat ? 9 : 2, 0.18, 0.08, 0.18, beat ? 0.22 : 0.02);
            return;
        }
        int count = level0 >= PartState.BLEED_HEAVY ? 3 : 1;
        // No speed: they are given none and gravity takes them, which is what a drop
        // does. Where they land, they leave a mark of their own.
        level.sendParticles(ModParticles.BLOOD.get(),
                player.getX(), player.getY() + player.getBbHeight() * height, player.getZ(),
                count, 0.14, 0.05, 0.14, 0.0);
    }

    /**
     * Remembers a place on the floor as bloodied for a while.
     *
     * <p>A mark landing on top of one that is already there does not become a second
     * mark - it makes the first one bigger. Which is why standing still while bleeding
     * leaves a pool and walking leaves a line, without either being written as a rule.
     */
    public static void stain(ServerLevel level, Vec3 where, int seconds) {
        List<Stain> stains = STAINS.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
        long now = level.getGameTime();
        for (int i = stains.size() - 1; i >= 0 && i >= stains.size() - 4; i--) {
            Stain near = stains.get(i);
            if (near.pos().distanceToSqr(where) <= POOLING * POOLING) {
                stains.set(i, new Stain(near.pos(), near.bornAt(), now + seconds * 20L,
                        Math.min(1.0F, near.size() + 0.16F)));
                return;
            }
        }
        stains.add(new Stain(where, now, now + seconds * 20L, 0.0F));
        while (stains.size() > MAX_STAINS) {
            stains.remove(0);
        }
    }

    /**
     * Draws every stain that somebody is close enough to see, and forgets the ones that
     * have dried. Called once a second for each world with players in it.
     */
    public static void tickStains(ServerLevel level) {
        List<Stain> stains = STAINS.get(level.dimension());
        if (stains == null || stains.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Stain> iterator = stains.iterator();
        while (iterator.hasNext()) {
            Stain stain = iterator.next();
            if (now >= stain.expiresAt()) {
                iterator.remove();
                continue;
            }
            if (level.getNearestPlayer(stain.pos().x, stain.pos().y, stain.pos().z, VISIBLE, false) == null) {
                continue;
            }
            // Fresh blood is bright and wet; an old stain is dark and thin. How old and
            // how big ride in the first two speeds - a mark that does not move has no use
            // for them, and with a count of nothing the client hands them straight to the
            // particle. A speed of one, because that count multiplies them by it.
            float age = (now - stain.bornAt()) / (float) Math.max(1L, stain.expiresAt() - stain.bornAt());
            level.sendParticles(ModParticles.STAIN.get(),
                    stain.pos().x, stain.pos().y + 0.02, stain.pos().z,
                    0, age, stain.size(), 0.0, 1.0);
        }
    }

    /** Dropped when a world unloads, so nothing is remembered about a place that is gone. */
    public static void forget(ServerLevel level) {
        STAINS.remove(level.dimension());
    }
}
