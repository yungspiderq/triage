package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * What a body keeps.
 *
 * <p>Every injury in this mod healed to the same body it started as. A player who had
 * survived four amputations, a septic belly and a skull fracture was, once mended,
 * indistinguishable from one who had never been touched - and that is a mod about
 * medicine quietly saying that none of it mattered once it was over.
 *
 * <p>So parts remember now, and they remember three things, each of which is a real
 * consequence rather than a tax:
 *
 * <ul>
 *   <li><b>A bone that has knitted</b> is not weaker - the mod will not teach that it is -
 *       but it is the place the next fall finds, because whatever broke that femur is the
 *       thing this body keeps doing. And it knows about weather, which is the single most
 *       reported thing about an old fracture by the people who have one.</li>
 *   <li><b>Skin that has been opened and closed</b> is scar tissue. There is less blood
 *       going through it than through what it replaced, which is exactly why an infection
 *       settles there and not somewhere else.</li>
 *   <li><b>A burn that healed through the skin</b> pulls its own edges together as it
 *       closes. That is a contracture, and it is the reason a burn survivor's arm does not
 *       straighten.</li>
 * </ul>
 *
 * <p><b>The counts never go down and only dying clears them.</b> The stiffness does, and
 * that split is the whole design. A history that ratchets one way with no way back would
 * make a long-lived character into a character who is quietly getting worse, and the
 * fastest cure for that would be to go and die - which is the opposite of everything this
 * mod is for. So the scar is permanent and the disability is worked back down by hand,
 * over days, by somebody willing to take the joint past where it is comfortable.
 *
 * @see PartState#knits
 * @see PartState#stiff
 */
public final class Scars {
    /** Long enough that a twinge is a twinge rather than a status effect. */
    private static final int ACHE_LINES = 4;

    /** The last day each body was told its old break is talking, so it says it once. */
    private static final Map<UUID, Integer> SPOKEN = new HashMap<>();

    private Scars() {
    }

    // ---- what leaves a mark -------------------------------------------------

    /**
     * A bone in this part has finished knitting.
     *
     * <p>Called where a break actually ends rather than where a splint goes on, because a
     * splint is not the end of a fracture - resting one is.
     */
    public static void knitted(ServerPlayer player, BodyHealth body, BodyPart part) {
        if (!Config.values().scars) {
            return;
        }
        PartState state = body.part(part);
        state.knitted();
        body.setDirty(true);
        // Said once, quietly, and never again. It is not an injury and there is nothing
        // to do about it; it is the part becoming a place this body has a history with.
        Body.notify(player, Component.translatable("triage.msg.knit_scar", part.title(), state.knits),
                ChatFormatting.GRAY, null);
    }

    /**
     * The skin here has been opened and has closed again.
     *
     * <p>One place for all of it, because as far as the skin is concerned a blade, a
     * blast and a surgeon are the same event: something went through, and something
     * that is not skin has grown across the gap.
     */
    public static void closed(ServerPlayer player, BodyHealth body, BodyPart part) {
        Config.Values config = Config.values();
        if (!config.scars) {
            return;
        }
        PartState state = body.part(part);
        state.scarred(config.scarStiffness);
        body.setDirty(true);
    }

    /** A burn that healed. How tight it goes is decided by how deep it went. */
    public static void burnHealed(BodyHealth body, BodyPart part, int depth) {
        Config.Values config = Config.values();
        if (!config.scars || depth < PartState.BURN_PARTIAL) {
            return;
        }
        PartState state = body.part(part);
        state.burnt = Math.max(state.burnt, depth);
        // Blistered leaves a mark. Through the skin leaves a mark that pulls.
        state.stiff = Mth.clamp(state.stiff + (depth >= PartState.BURN_FULL
                ? config.burnStiffness : config.burnStiffness * 0.4F), 0.0F, 1.0F);
        body.setDirty(true);
    }

    /** A limb sewn back on. It works; it does not work the way it did. */
    public static void sewn(BodyHealth body, BodyPart part) {
        Config.Values config = Config.values();
        if (!config.scars) {
            return;
        }
        PartState state = body.part(part);
        state.cuts++;
        state.stiff = Mth.clamp(state.stiff + config.graftStiffness, 0.0F, 1.0F);
        body.setDirty(true);
    }

    /** Something has grown in here once. It will find the place again. */
    public static void wasInfected(BodyHealth body, BodyPart part) {
        if (!Config.values().scars) {
            return;
        }
        body.part(part).septic = true;
        body.setDirty(true);
    }

    // ---- what a mark costs --------------------------------------------------

    /**
     * The multiplier on the blow needed to break a bone here.
     *
     * <p>Under one after the first knit, and it does not keep stacking forever: a leg
     * that has been broken six times is not made of glass, it is simply the leg this
     * body breaks.
     */
    public static float fragility(PartState state) {
        Config.Values config = Config.values();
        if (!config.scars || state.knits <= 0) {
            return 1.0F;
        }
        return Math.max(0.3F, (float) Math.pow(config.knitFragility, Math.min(3, state.knits)));
    }

    /** And the multiplier on the chance a wound here goes bad. */
    public static float infectionFactor(PartState state) {
        Config.Values config = Config.values();
        if (!config.scars) {
            return 1.0F;
        }
        float factor = 1.0F;
        if (state.cuts > 0) {
            factor *= config.scarInfection;
        }
        if (state.septic) {
            factor *= config.scarInfection;
        }
        return factor;
    }

    /**
     * How much of the hands is gone, 0 to 1, counting both arms.
     *
     * <p>Stiffness in the arms and whatever is strapped to them. A stiff arm is not a
     * weak arm - it is one that does not go where it is put - so what it costs is the
     * quality of work done with it, and nothing else.
     */
    public static float handsLost(BodyHealth body) {
        Config.Values config = Config.values();
        float worst = 0.0F;
        for (BodyPart part : new BodyPart[]{BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM}) {
            PartState state = body.part(part);
            float cost = 0.0F;
            if (config.scars) {
                cost += state.stiff * config.stiffQuality;
            }
            if (config.wornSplints && state.splinted()) {
                cost += config.splintQuality;
            }
            // A bone that knitted out of line is the third thing that makes an arm not
            // go where it is put, and it is the only one of the three that is for good.
            cost += Bones.penalty(state);
            worst = Math.max(worst, cost);
        }
        return Mth.clamp(worst, 0.0F, 0.9F);
    }

    /** And what the legs cost off walking, for the same two reasons. */
    public static double legsLost(BodyHealth body) {
        Config.Values config = Config.values();
        double worst = 0.0;
        for (BodyPart part : new BodyPart[]{BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG}) {
            PartState state = body.part(part);
            double cost = 0.0;
            if (config.scars) {
                cost += state.stiff * config.stiffSlow;
            }
            if (config.wornSplints && state.splinted()) {
                cost += config.splintSlow;
            }
            cost += Bones.penalty(state);
            worst = Math.max(worst, cost);
        }
        return Mth.clamp(worst, 0.0, 0.6);
    }

    // ---- the weather --------------------------------------------------------

    /**
     * Whether the sky is doing the thing old breaks answer to.
     *
     * <p>Rain and storms wherever you are, because what an old fracture reacts to is the
     * pressure rather than getting wet - being indoors is no help at all, and everybody
     * who has one will tell you so. Cold does it too, and cold is easier to find.
     *
     * <p>Worked out on both sides from the same two facts, so the chart in the corner can
     * say it without anything being sent: the client knows what the weather is.
     */
    public static boolean weather(Level level, Player player) {
        if (!Config.values().ache) {
            return false;
        }
        return level.isRaining() || level.isThundering()
                || level.getBiome(player.blockPosition()).value().coldEnoughToSnow(player.blockPosition());
    }

    /** Whether this particular part is one of the ones that would be talking. */
    public static boolean aching(PartState state) {
        return Config.values().scars && Config.values().ache && state.knits > 0 && !state.gone();
    }

    /** Which old break is loudest: the crooked one, every time. */
    public static int acheWeight(PartState state) {
        return state.knits + state.crooked * 3;
    }

    /**
     * One second of weather over a body with a history.
     *
     * <p>Nothing here touches the body. No damage, no penalty, no clock - it is a person
     * saying that their leg knows it is going to rain, which is worth exactly what it is
     * worth in life: nothing, and everything, depending on who is listening.
     */
    public static void ache(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        if (!config.scars || !config.ache || !weather(player.level(), player)) {
            SPOKEN.remove(player.getUUID());
            return;
        }
        BodyPart worst = null;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!aching(state)) {
                continue;
            }
            if (worst == null || acheWeight(state) > acheWeight(body.part(worst))) {
                worst = part;
            }
        }
        if (worst == null) {
            return;
        }
        // Once per weather front rather than on a timer, so that a long storm is one
        // remark from an old injury instead of a drip of them.
        int day = (int) (player.level().getDayTime() / 24000L);
        Integer last = SPOKEN.get(player.getUUID());
        if (last != null && last == day) {
            return;
        }
        float chance = Math.min(1.0F, interval / (float) Math.max(1, config.acheSeconds * 20));
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }
        SPOKEN.put(player.getUUID(), day);
        Body.notify(player, Component.translatable("triage.msg.ache", worst.title()),
                ChatFormatting.GRAY, null);
        Reactions.say(player, "ache");
    }

    public static void forget(UUID playerId) {
        SPOKEN.remove(playerId);
    }

    // ---- working it loose ---------------------------------------------------

    /** Whether this part is worth anybody's hands at all. */
    public static boolean worthStretching(PartState state) {
        return Config.values().stretch && Config.values().scars
                && state.stiff > 0.01F && !state.gone() && !state.splinted();
    }

    /**
     * Whether this part has already had its session today.
     *
     * <p>Takes a level rather than a player because both sides ask it - the chart decides
     * whether to offer the hands, the server decides whether to accept them - and a
     * client that has not finished loading a world has no day to compare against, which
     * is a "no" rather than a crash.
     */
    public static boolean workedToday(@Nullable Level level, PartState state) {
        return level != null && state.worked != 0 && state.worked == day(level);
    }

    private static int day(Level level) {
        return (int) (level.getDayTime() / 24000L) + 1;
    }

    /**
     * A session, over.
     *
     * <p>What it is worth is how close the hands got to the edge of the joint without
     * going past it, and the edge is not marked: the only thing telling anybody where it
     * is, is the person it belongs to. Which is why doing it to yourself is worth less -
     * you stop at your own pain, every time, and that is not weakness, it is the reason
     * this is a job for somebody else's hands.
     *
     * @param quality 0 to 1, or below zero for a session that went past the joint
     */
    public static void stretch(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                               BodyPart part, float quality) {
        Config.Values config = Config.values();
        PartState state = body.part(part);
        state.worked = day(patient.level());
        boolean self = helper == patient;

        if (quality <= 0.0F) {
            // Past the joint rather than to it. Something in there tears, the session is
            // spent, and the part is tighter tomorrow for it - which is the honest
            // outcome of the commonest mistake made with a stiff limb.
            Injuries.apply(patient, body, part, config.stretchTear, false);
            state.stiff = Mth.clamp(state.stiff + config.scarStiffness * 0.5F, 0.0F, 1.0F);
            body.setDirty(true);
            Body.notify(helper, Component.translatable("triage.msg.stretch_tore", part.title()),
                    ChatFormatting.RED, SoundEvents.PLAYER_HURT);
            if (!self) {
                Body.notify(patient, Component.translatable("triage.msg.stretch_tore_you",
                        helper.getName(), part.title()), ChatFormatting.RED, null);
            }
            return;
        }

        float gain = config.stretchGain * Mth.clamp(quality, 0.0F, 1.0F)
                * (self ? config.stretchSelfCap : 1.0F);
        float before = state.stiff;
        state.stiff = Math.max(0.0F, state.stiff - gain);
        body.setDirty(true);

        boolean freed = state.stiff <= 0.0F && before > 0.0F;
        Body.notify(helper, Component.translatable(freed
                        ? "triage.msg.stretch_free" : "triage.msg.stretch_worked",
                        part.title(), Math.round(state.stiff * 100.0F)),
                freed ? ChatFormatting.GREEN : ChatFormatting.AQUA, SoundEvents.ARMOR_EQUIP_LEATHER.value());
        if (!self) {
            Body.notify(patient, Component.translatable("triage.msg.stretch_worked_you",
                    helper.getName(), part.title()), ChatFormatting.AQUA, null);
        }
    }

    /** How many parts of this body carry something permanent, for the chart. */
    public static int marks(BodyHealth body) {
        int count = 0;
        for (BodyPart part : BodyPart.VALUES) {
            if (body.part(part).marked()) {
                count++;
            }
        }
        return count;
    }

    /** How many variations the line about an aching old break has. */
    public static int acheLines() {
        return ACHE_LINES;
    }
}
