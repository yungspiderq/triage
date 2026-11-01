package com.claudemods.triage.med;

import com.claudemods.triage.Config;
import com.claudemods.triage.block.ModBlocks;
import com.claudemods.triage.item.ModComponents;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.Adrenaline;
import com.claudemods.triage.body.Fatigue;
import com.claudemods.triage.body.Vigil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.Nullable;

/**
 * Where somebody is treated, and what they are treated with.
 *
 * <p>Every treatment in this mod has been worth exactly the same wherever it happened and
 * whatever it was done with. A surgical kit opened in a lit room over a table did the same
 * job as one opened face-down in a cave, with the same saw that took somebody else's leg
 * off an hour ago and has not been touched since.
 *
 * <p>Both halves of that are wrong, and the second is the famous one. For most of the
 * history of surgery the instrument was the thing that killed people - not the cutting,
 * the carrying: a blade went from one patient into the next and nobody thought anything of
 * it. Putting that in costs the two tools that go into a wound and are never spent - the
 * saw and the forceps - the one thing they never had, which is a running cost.
 *
 * <p>Neither half punishes anybody for playing the way they already do. A clean instrument
 * in an ordinary room is worth what a treatment has always been worth; what opens up is a
 * direction in each sense. Somewhere good is better than the old baseline, and a filthy
 * saw in the dark is worse.
 */
public final class Theatre {
    /** How far from the patient a table still counts. Arm's reach and a bit. */
    private static final double TABLE_REACH = 4.0;
    /** And how close somebody has to be to have hold of them. */
    private static final double HANDS_REACH = 4.0;
    /** Uses before an instrument is as dirty as it gets. */
    private static final int FILTHY = 6;

    private Theatre() {
    }

    // ---- what it was done with --------------------------------------------

    /** How many patients this instrument has been through since it was cleaned. */
    public static int grime(ItemStack stack) {
        Integer used = stack.get(ModComponents.GRIME.get());
        return used == null ? 0 : Math.max(0, used);
    }

    /** 0 for something clean, 1 for something nobody should be holding. */
    public static float filth(ItemStack stack) {
        return Mth.clamp(grime(stack) / (float) FILTHY, 0.0F, 1.0F);
    }

    /**
     * One more patient on it.
     *
     * <p>Only the tools that are carried rather than spent - a bandage that has been used
     * is gone, so it cannot be used again dirty, and the one thing this must not turn into
     * is a durability bar on a consumable.
     *
     * <p>And only the ones that go into somebody. Bandage scissors are carried and are
     * reusable, but nothing in here has them opening a wound, so dirt on them could never
     * be read by anything: keeping it would be a chore with no consequence at the end of
     * it, which is worse than not having the rule. That leaves the saw and the forceps,
     * and both of them earn it.
     */
    public static void used(ItemStack stack, MedicineKind kind) {
        if (!kind.reusable() || !kind.opensSkin()) {
            return;
        }
        stack.set(ModComponents.GRIME.get(), Math.min(FILTHY + 2, grime(stack) + 1));
    }

    public static void clean(ItemStack stack) {
        stack.remove(ModComponents.GRIME.get());
    }

    /**
     * Left lying in water.
     *
     * <p>Water rinses; it does not sterilise, and it never has. What comes off a blade in
     * a stream is the blood, which is the part you can see and the part that matters
     * least - and a stream brings its own contribution. So this takes an instrument most
     * of the way down and stops short, and the last of it still wants a table or a bottle.
     *
     * <p>Which is the right shape for a field expedient: better than carrying a filthy saw
     * and worse than doing the job properly, so it is worth doing and it does not replace
     * anything.
     *
     * <p>Rolled once a tick rather than fired on a schedule. The first version keyed off
     * how long the item had existed, which is not the same as how long it has been in
     * water: something thrown in at the wrong moment waited twice as long for the first
     * of it to come off, and something bobbing in and out of a stream could miss every
     * window it had and never clean at all. A roll per tick in the water cannot be
     * dodged by arriving on the wrong tick, and comes to the same rate.
     *
     * @return true when this tick actually took something off
     */
    public static boolean rinse(ItemStack stack, MedicineKind kind, RandomSource random) {
        Config.Values config = Config.values();
        if (!config.rinse || !kind.reusable() || !kind.opensSkin()) {
            return false;
        }
        int floor = Math.max(0, config.rinseFloor);
        int grime = grime(stack);
        if (grime <= floor) {
            return false;
        }
        int every = Math.max(1, config.rinseSeconds * 20);
        if (random.nextInt(every) != 0) {
            return false;
        }
        stack.set(ModComponents.GRIME.get(), grime - 1);
        return true;
    }

    // ---- where it was done ------------------------------------------------

    /**
     * How good a place this is to be operated on, 0 to 1.
     *
     * <p>Three things, and they are the three anybody would name: is there somewhere to
     * put the patient, can the person working see, and is the place shut off from the
     * weather and everything living in it.
     *
     * <p>A table is the largest of the three because it is the one a player has to decide
     * to build. The other two are mostly free indoors, which is the point - a room is
     * already most of what an operating theatre is.
     */
    public static float place(ServerPlayer patient) {
        Level level = patient.level();
        BlockPos at = patient.blockPosition();
        float score = 0.0F;

        if (nearTable(level, at)) {
            score += 0.5F;
        }
        // Enough to work by, and from a lamp rather than from the sky: a surgeon who can
        // only operate at noon is not a surgeon, they are a sundial.
        int light = level.getBrightness(LightLayer.BLOCK, at);
        score += 0.3F * Mth.clamp((light - 4) / 8.0F, 0.0F, 1.0F);
        // A roof. Anything over their head counts; the sky does not.
        if (!level.canSeeSky(at)) {
            score += 0.2F;
        }
        return Mth.clamp(score, 0.0F, 1.0F);
    }

    private static boolean nearTable(Level level, BlockPos at) {
        int reach = (int) Math.ceil(TABLE_REACH);
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-reach, -2, -reach),
                at.offset(reach, 2, reach))) {
            if (level.getBlockState(pos).is(ModBlocks.MEDICAL_TABLE.get())) {
                return true;
            }
        }
        return false;
    }

    // ---- and who else is there --------------------------------------------

    /**
     * Somebody holding the patient still.
     *
     * <p>The two halves above are where and what. This is who, and it is the one the mod
     * has been pointing at since the first version: everything here is easier with a
     * second person, and until now a second person could do everything except the thing
     * an assistant actually does, which is hold on.
     *
     * <p>It asks for two deliberate things and nothing accidental. They have to be
     * crouched, and their hand has to be empty - you cannot hold a patient down with a
     * sword in your fist, and nobody is going to do either by walking past. Which also
     * means the mod never has to guess at intent: if both are true, they meant it.
     *
     * <p>One person, not a crowd. A second assistant is not better surgery, it is a
     * room full of people, so the first one found is the one who counts.
     */
    @Nullable
    public static ServerPlayer assistant(ServerPlayer patient, ServerPlayer healer) {
        Config.Values config = Config.values();
        if (!config.assist || !(patient.level() instanceof ServerLevel level)) {
            return null;
        }
        double reach = HANDS_REACH * HANDS_REACH;
        for (ServerPlayer other : level.players()) {
            if (other == patient || other == healer || !other.isAlive() || other.isSpectator()) {
                continue;
            }
            if (!other.isCrouching() || !other.getMainHandItem().isEmpty()) {
                continue;
            }
            if (other.distanceToSqr(patient) > reach || Body.of(other).downed()) {
                continue;
            }
            return other;
        }
        return null;
    }

    // ---- and what the three of them come to -------------------------------

    /**
     * The chance this treatment leaves something behind in the wound.
     *
     * <p>The instrument is the bigger half and deliberately so: a bad room is bad luck,
     * and a filthy blade is a decision somebody made and kept making. Nothing here fires
     * at all for a clean tool in a fair place, which is the shape the whole thing is built
     * to have - the old way of playing costs nothing new.
     */
    public static float infectionChance(ServerPlayer patient, ItemStack tool) {
        Config.Values config = Config.values();
        if (!config.theatre || !config.infection) {
            return 0.0F;
        }
        float dirty = filth(tool);
        // Centred on the same middle as steadiness, and for the same reason: an ordinary
        // room has to be worth what a room has always been worth. A place below that adds
        // risk; a place at or above it adds none, so a clean tool indoors rolls nothing at
        // all and the way people already play costs them nothing new.
        float room = Mth.clamp((0.5F - place(patient)) * 2.0F, 0.0F, 1.0F);
        return Mth.clamp(dirty * config.grimeInfection + room * config.roomInfection, 0.0F, 1.0F);
    }

    /**
     * What a place is worth to a pair of hands, as a multiplier on how well it went.
     *
     * <p>Centred on the middle: an ordinary room leaves a treatment exactly as good as it
     * has always been, a proper theatre makes it better, and the floor of a cave in the
     * dark makes it worse. A rule that only ever took something away would be a tax on
     * playing, and this is meant to be a reason to build something.
     */
    public static float steadiness(ServerPlayer patient, ServerPlayer healer,
                                   @Nullable ServerPlayer assistant) {
        Config.Values config = Config.values();
        float room = config.theatre
                ? 1.0F + (place(patient) - 0.5F) * 2.0F * config.placeQuality
                : 1.0F;
        // The assistant is added to the room rather than multiplied by it, because what
        // they are worth does not depend on where you are standing. A pair of hands on a
        // cave floor is worth what a pair of hands is worth.
        if (assistant != null) {
            room += config.assistQuality;
        }
        // And the fourth of them: whose hands these are and what they have just been
        // doing. The room, the instrument and the assistant are all things somebody
        // arranged; this is the only one that is about the surgeon.
        BodyHealth hands = Body.of(healer);
        // Two ways a pair of hands is worse than it was an hour ago: it is tired, or it
        // is full of adrenaline. The second is the reason the minute after somebody gets
        // hurt is the worst minute in which to start cutting into them.
        return room - Fatigue.of(hands) * config.fatigueQuality - Adrenaline.shake(hands)
                // And the fifth: how long it has been since this pair slept. The only
                // one of the five with no answer available in the next half minute.
                - Vigil.hands(healer);
    }
}
