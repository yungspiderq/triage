package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The whole body of one player: seven parts plus however long the painkillers still
 * hold. Attached to the player, saved with them and sent to their client whenever it
 * changes - the HUD is the only way a player can see any of this.
 */
public final class BodyHealth {
    public static final MapCodec<BodyHealth> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(BodyPart.CODEC, PartState.CODEC)
                    .optionalFieldOf("parts", Map.of()).forGetter(BodyHealth::partView),
            Codec.INT.optionalFieldOf("painkiller_ticks", 0).forGetter(b -> b.painkillerTicks),
            Codec.unboundedMap(Disease.CODEC, Codec.INT)
                    .optionalFieldOf("diseases", Map.of()).forGetter(BodyHealth::diseaseView),
            Codec.FLOAT.optionalFieldOf("blood_loss", 0.0F).forGetter(b -> b.bloodLoss),
            Codec.INT.optionalFieldOf("downed_ticks", 0).forGetter(b -> b.downedTicks),
            BloodType.CODEC.optionalFieldOf("blood_type")
                    .forGetter(b -> Optional.ofNullable(b.bloodType)),
            Codec.INT.optionalFieldOf("stabilised_ticks", 0).forGetter(b -> b.stabilisedTicks),
            Codec.INT.optionalFieldOf("holds", 0).forGetter(b -> b.holds),
            Codec.INT.optionalFieldOf("painkiller_tolerance", 0).forGetter(b -> b.painkillerTolerance),
            TriageTag.CODEC.optionalFieldOf("tag").forGetter(b -> Optional.ofNullable(b.tag)),
            Codec.STRING.optionalFieldOf("tag_by", "").forGetter(b -> b.tagBy),
            Codec.INT.optionalFieldOf("tag_ticks", 0).forGetter(b -> b.tagTicks),
            Codec.INT.optionalFieldOf("airway_ticks", 0).forGetter(b -> b.airwayTicks),
            Codec.INT.optionalFieldOf("arrest_ticks", 0).forGetter(b -> b.arrestTicks),
            Codec.INT.optionalFieldOf("cpr_rounds", 0).forGetter(b -> b.cprRounds),
            Mind.CODEC.optionalFieldOf("mind", Mind.NONE).forGetter(Mind::of)
    ).apply(i, BodyHealth::new));

    /**
     * The sixteenth field, and everything after it.
     *
     * <p>A record codec group tops out at sixteen entries and the fifteen above are
     * spoken for, so anything newer than this comment lives in here as a nested object -
     * the same arrangement {@code PartState} came to for the same reason.
     *
     * <p>Both of these belong to the one condition in the mod that arrives with the
     * person rather than happening to them: how much of a course is still holding it
     * down, and how long it is going to be worse for now that the course has run out.
     */
    private record Mind(int courseTicks, int reboundTicks, int underTicks, Allergy allergy,
                       int faintTicks, int faintGrace, float tired, Notes notes) {
        private static final Mind NONE = new Mind(0, 0, 0, Allergy.NONE, 0, 0, 0.0F, Notes.NONE);

        private static final Codec<Mind> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("course_ticks", 0).forGetter(Mind::courseTicks),
                Codec.INT.optionalFieldOf("rebound_ticks", 0).forGetter(Mind::reboundTicks),
                Codec.INT.optionalFieldOf("under_ticks", 0).forGetter(Mind::underTicks),
                Allergy.CODEC.optionalFieldOf("allergy", Allergy.NONE).forGetter(Mind::allergy),
                Codec.INT.optionalFieldOf("faint_ticks", 0).forGetter(Mind::faintTicks),
                Codec.INT.optionalFieldOf("faint_grace", 0).forGetter(Mind::faintGrace),
                Codec.FLOAT.optionalFieldOf("tired", 0.0F).forGetter(Mind::tired),
                Notes.CODEC.optionalFieldOf("notes", Notes.NONE).forGetter(Mind::notes)
        ).apply(i, Mind::new));

        private static Mind of(BodyHealth body) {
            return new Mind(body.courseTicks, body.reboundTicks, body.underTicks, body.allergy,
                    body.faintTicks, body.faintGrace, body.tired, Notes.of(body));
        }
    }

    /**
     * What anybody has actually read off this body, and when.
     *
     * <p>The other half of the word the mod is named after. A chart used to show every
     * reading as a live fact - walk up to a stranger and the screen told you their pulse,
     * whether anybody had ever taken it. That is not a chart, it is a debug readout: it
     * made the taking of a reading worth nothing, and it made the second medic to arrive
     * repeat everything the first one had already done.
     *
     * <p>So readings are written down now. A pulse taken is a pulse on the notes, with a
     * name against it and a time, and it <b>goes stale</b> - because a pulse from four
     * minutes ago is a fact about four minutes ago, and the whole reason to write the time
     * down is so that everybody can see how old the news is.
     */
    private record Notes(int pulseRate, int pulseCharacter, long pulseAt, String pulseBy,
                         int tempTenths, long tempAt, String tempBy,
                         int said, long saidAt, long filmAt, String filmBy,
                         long found, List<Long> foundAt, List<Reading> log) {
        private static final Notes NONE = new Notes(0, -1, 0L, "", 0, 0L, "", -1, 0L, 0L, "",
                0L, List.of(), List.of());

        private static final Codec<Notes> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("pulse_rate", 0).forGetter(Notes::pulseRate),
                Codec.INT.optionalFieldOf("pulse_character", -1).forGetter(Notes::pulseCharacter),
                Codec.LONG.optionalFieldOf("pulse_at", 0L).forGetter(Notes::pulseAt),
                Codec.STRING.optionalFieldOf("pulse_by", "").forGetter(Notes::pulseBy),
                Codec.INT.optionalFieldOf("temp_tenths", 0).forGetter(Notes::tempTenths),
                Codec.LONG.optionalFieldOf("temp_at", 0L).forGetter(Notes::tempAt),
                Codec.STRING.optionalFieldOf("temp_by", "").forGetter(Notes::tempBy),
                Codec.INT.optionalFieldOf("said", -1).forGetter(Notes::said),
                Codec.LONG.optionalFieldOf("said_at", 0L).forGetter(Notes::saidAt),
                Codec.LONG.optionalFieldOf("film_at", 0L).forGetter(Notes::filmAt),
                Codec.STRING.optionalFieldOf("film_by", "").forGetter(Notes::filmBy),
                // What has been found out about this body, and when each part was last
                // looked at. Only realistic difficulty reads these; they are written down
                // in both, so a world can be switched over without losing the chart.
                Codec.LONG.optionalFieldOf("found", 0L).forGetter(Notes::found),
                Codec.LONG.listOf().optionalFieldOf("found_at", List.of()).forGetter(Notes::foundAt),
                // And the page itself. Fourteenth of the sixteen a record codec will take,
                // which is worth saying out loud: the next two people to add something to
                // the notes get to do it, and the one after that opens a new shelf.
                Reading.CODEC.listOf().optionalFieldOf("log", List.of()).forGetter(Notes::log)
        ).apply(i, Notes::new));

        private static Notes of(BodyHealth body) {
            List<Long> at = new ArrayList<>(body.foundAt.length);
            for (long when : body.foundAt) {
                at.add(when);
            }
            return new Notes(body.pulseRate, body.pulseCharacter, body.pulseAt, body.pulseBy,
                    body.tempTenths, body.tempAt, body.tempBy,
                    body.said, body.saidAt, body.filmAt, body.filmBy,
                    body.found, at, List.copyOf(body.log));
        }
    }

    public static final Codec<BodyHealth> CODEC = MAP_CODEC.codec();

    /**
     * How far gone the arms are, 0 to 1. See {@link Fatigue}.
     *
     * <p>Not in the codec above and not by accident. Everything else in this class is an
     * injury and has to outlive a disconnect; this is a minute of breath, and a minute of
     * breath is roughly what reconnecting costs anyway. It also keeps the group above at
     * sixteen entries, which is where the codec builder stops counting.
     */
    public float fatigue;

    /**
     * Ticks of not feeling it, and then ticks of feeling all of it. See {@link Adrenaline}.
     *
     * <p>Out of the codec for the same reason fatigue is: a minute of chemistry is not an
     * injury and has no business surviving a disconnect.
     */
    public int adrenalineTicks;
    public int crashTicks;

    /**
     * How much heat an open body has lost, 0 to 1. See {@link Chill}.
     *
     * <p>Out of the codec with the other two, and for the same reason: it is a state of
     * the last few minutes rather than an injury.
     */
    public float chill;

    /**
     * The one thing in the bag that will kill this particular person.
     *
     * <p>Kept beside the blood type because it is the same sort of fact - true about
     * somebody before anything happens to them - and wanted at the same moment, which is
     * the moment before a bag is opened over them.
     */
    private Allergy allergy = Allergy.NONE;

    /**
     * Seconds of not being there, and then seconds of not doing it again.
     *
     * <p>In the codec rather than beside fatigue, and not because a faint outlives a
     * disconnect - it does not, and nobody would notice either way. It is here because
     * <b>the screen has to know</b>: what a faint looks like is the world going out, and
     * the only thing the client is ever told about a body is what travels in this.
     */
    private int faintTicks;
    private int faintGrace;

    /**
     * How long since this one slept, 0 to 1, copied down from the vanilla counter.
     *
     * <p>Derived rather than owned - {@link Vigil} works it out from the stat the phantoms
     * already read - and it lives here for one reason: the eyes that are closing are on
     * the client, and this is the only thing that travels to it.
     *
     * <p>Quantised on the way in. It moves over days, so sending a packet every second to
     * report a number that has not visibly changed would be a packet a second for the
     * commonest state in the game, which is "awake".
     */
    private float tired;

    /**
     * The notes themselves, flat on the body so that everything which reads them reads
     * one field rather than a record it has to unpack. See {@link Notes}.
     */
    private int pulseRate;
    private int pulseCharacter = -1;
    private long pulseAt;
    private String pulseBy = "";
    private int tempTenths;
    private long tempAt;
    private String tempBy = "";
    private int said = -1;
    private long saidAt;
    private long filmAt;
    private String filmBy = "";

    /**
     * Everything anybody has measured on this body, oldest first.
     *
     * <p>Sixteen of them, and then the oldest falls off the top - which is what a chart
     * does when the page fills up. Sixteen is enough to hold four of each kind, and four
     * of anything is more of a line than anybody needs to read a direction off.
     *
     * <p>This is the one thing realistic difficulty never hides. Everything else about a
     * body is redacted until somebody finds it out; every line in here <em>is</em>
     * somebody finding something out, with their name against it. A chart is not a
     * secret from the person who wrote it.
     */
    private final List<Reading> log = new ArrayList<>();
    /** What fits on the page, and how many of any one thing it will hold. */
    private static final int LOG_LIMIT = 16;
    private static final int LOG_PER_KIND = 4;

    /**
     * What has been found out about this body: eight facts per part, packed. See
     * {@link Findings}.
     *
     * <p>Zero on a fresh body and zero is the honest answer - nobody has looked yet. In
     * arcade nothing reads this; in realistic it is the difference between the chart
     * saying "no break" and saying nothing at all.
     */
    private long found;

    /** When each part was last examined, by game time. Indexed by {@link BodyPart#ordinal}. */
    private final long[] foundAt = new long[BodyPart.VALUES.length];

    private final EnumMap<BodyPart, PartState> parts = new EnumMap<>(BodyPart.class);

    /** Every illness the player currently has, and how long they have had it. */
    private final EnumMap<Disease, Integer> diseases = new EnumMap<>(Disease.class);

    /** Ticks left of suppressed pain. Damage keeps piling up, it just stops hurting. */
    private int painkillerTicks;

    /**
     * How much blood has been lost, 0 to 1.
     *
     * <p>The one thing bleeding leaves behind after it is bandaged. Without it a wound
     * costs nothing the moment you tie cloth round it, which is the least believable
     * thing the mod used to do.
     */
    private float bloodLoss;

    /** Set when something moved but no packet went out yet; flushed once a second. */
    private transient boolean dirty;

    public BodyHealth() {
        for (BodyPart part : BodyPart.VALUES) {
            parts.put(part, PartState.full(part));
        }
    }

    private BodyHealth(Map<BodyPart, PartState> saved, int painkillerTicks, Map<Disease, Integer> diseases,
                       float bloodLoss, int downedTicks, Optional<BloodType> bloodType,
                       int stabilisedTicks, int holds, int painkillerTolerance,
                       Optional<TriageTag> tag, String tagBy, int tagTicks,
                       int airwayTicks, int arrestTicks, int cprRounds, Mind mind) {
        this();
        saved.forEach((part, state) -> parts.put(part, state.copy()));
        this.painkillerTicks = Math.max(0, painkillerTicks);
        diseases.forEach((disease, ticks) -> this.diseases.put(disease, Math.max(0, ticks)));
        this.bloodLoss = Mth.clamp(bloodLoss, 0.0F, 1.0F);
        this.downedTicks = Math.max(0, downedTicks);
        this.bloodType = bloodType.orElse(null);
        this.stabilisedTicks = Math.max(0, stabilisedTicks);
        this.holds = Math.max(0, holds);
        this.painkillerTolerance = Math.max(0, painkillerTolerance);
        this.tag = tag.orElse(null);
        this.tagBy = tagBy;
        this.tagTicks = Math.max(0, tagTicks);
        this.airwayTicks = Math.max(0, airwayTicks);
        this.arrestTicks = Math.max(0, arrestTicks);
        this.cprRounds = Math.max(0, cprRounds);
        this.courseTicks = Math.max(0, mind.courseTicks());
        this.reboundTicks = Math.max(0, mind.reboundTicks());
        this.underTicks = Math.max(0, mind.underTicks());
        this.allergy = mind.allergy();
        // Read back off the copy as well as written to it. Without this the field moved
        // on the server and arrived at the client as a zero, which is a faint nobody
        // could see from inside - the one place it has to be visible.
        this.faintTicks = Math.max(0, mind.faintTicks());
        this.faintGrace = Math.max(0, mind.faintGrace());
        this.tired = Mth.clamp(mind.tired(), 0.0F, 1.0F);
        Notes notes = mind.notes();
        this.pulseRate = Math.max(0, notes.pulseRate());
        this.pulseCharacter = notes.pulseCharacter();
        this.pulseAt = notes.pulseAt();
        this.pulseBy = notes.pulseBy();
        this.tempTenths = notes.tempTenths();
        this.tempAt = notes.tempAt();
        this.tempBy = notes.tempBy();
        this.said = notes.said();
        this.saidAt = notes.saidAt();
        // Read back like everything else here, and for the reason the faint clock taught:
        // a field written out and not read in is a field that exists only on the server.
        this.found = notes.found();
        List<Long> at = notes.foundAt();
        for (int i = 0; i < this.foundAt.length && i < at.size(); i++) {
            this.foundAt[i] = at.get(i);
        }
        this.filmAt = notes.filmAt();
        this.filmBy = notes.filmBy();
        // Read back, like the faint clock taught: a field written out and not read in is a
        // field that exists only on the server.
        this.log.addAll(notes.log());
        while (this.log.size() > LOG_LIMIT) {
            this.log.remove(0);
        }
    }

    /**
     * How many courses of painkillers this body has had lately.
     *
     * <p>Each one is worth less than the last. Without it the pills were a button with
     * no cost at all - swallow one whenever anything hurts - and the mod's whole argument
     * is that nothing is free. Now they are a thing you spend, and spending them badly is
     * how you find yourself with nothing left to spend when it matters.
     *
     * <p>The clock on it is not saved: losing at most one step of it across a reload is
     * not worth a field in every body on disk.
     */
    private int painkillerTolerance;
    private transient int toleranceTicks;

    public int painkillerTolerance() {
        return painkillerTolerance;
    }

    /**
     * What a dose would be worth right now, without taking it.
     *
     * <p>Separate from {@link #takePainkiller()} so that a refused dose does not still
     * cost the tolerance: asking is free, swallowing is not.
     */
    public int painkillerWorth(int fullTicks, float falloff) {
        return Math.round(fullTicks * (float) Math.pow(falloff, painkillerTolerance));
    }

    public void takePainkiller() {
        painkillerTolerance++;
        toleranceTicks = 0;
        dirty = true;
    }

    /** Wears the tolerance off, but only once the last dose is out of the body. */
    public void tickTolerance(int ticks, int perStep) {
        if (painkillerTolerance <= 0 || painkillerTicks > 0 || perStep <= 0) {
            return;
        }
        toleranceTicks += ticks;
        if (toleranceTicks >= perStep) {
            toleranceTicks = 0;
            painkillerTolerance--;
            dirty = true;
        }
    }

    /**
     * The mark somebody pinned on this body, who pinned it, and how long ago.
     *
     * <p>It is not a reading of anything. Nothing here is worked out from the parts
     * above: it is one player's judgement of another, written down where the rest of the
     * group can see it, and it is wrong exactly as often as they are.
     *
     * <p>The clock stops once the mark is stale. Past that point the number is no longer
     * the point - what matters is that somebody said this a while ago - and stopping it
     * also stops a body that is otherwise doing nothing from being sent to its owner
     * once a second forever.
     */
    private @Nullable TriageTag tag;
    private String tagBy = "";
    private int tagTicks;

    public @Nullable TriageTag tag() {
        return tag;
    }

    /** Whoever wrote it, by the name they had at the time. Empty if nobody has. */
    public String tagBy() {
        return tagBy;
    }

    public int tagTicks() {
        return tagTicks;
    }

    public void writeTag(TriageTag written, String by) {
        tag = written;
        tagBy = by;
        tagTicks = 0;
        dirty = true;
    }

    /** @return true when there was one to take off */
    public boolean tearOffTag() {
        if (tag == null) {
            return false;
        }
        tag = null;
        tagBy = "";
        tagTicks = 0;
        dirty = true;
        return true;
    }

    public void tickTag(int ticks, int staleTicks) {
        if (tag == null || tagTicks >= staleTicks) {
            return;
        }
        tagTicks = Math.min(staleTicks, tagTicks + ticks);
        dirty = true;
    }

    /** Old enough that the mod stops claiming to know how old. */
    public boolean tagStale(int staleTicks) {
        return tag != null && tagTicks >= staleTicks;
    }

    /**
     * Which blood this body has, or null before it has been given one.
     *
     * <p>Null rather than a default on purpose: a default would mean every body that has
     * not been rolled yet quietly shares a type, and the one that would get picked is
     * either the universal donor or the universal recipient. Both are wrong for free.
     */
    private @Nullable BloodType bloodType;

    public Allergy allergy() {
        return allergy;
    }

    public void setAllergy(Allergy allergy) {
        this.allergy = allergy == null ? Allergy.NONE : allergy;
        dirty = true;
    }

    public @Nullable BloodType bloodType() {
        return bloodType;
    }

    public void setBloodType(@Nullable BloodType type) {
        bloodType = type;
        dirty = true;
    }

    /**
     * Ticks left before a downed player stops being savable.
     *
     * <p>Emptying a vital part no longer kills outright: it puts the player on the
     * floor with a clock running. Somebody else can open them up and get them back;
     * nobody coming is what turns it into a death.
     */
    private int downedTicks;

    /**
     * Ticks of held clock, and how many times it has been held on this downing.
     *
     * <p>Packing somebody's wound does not stand them up - only surgery does that - but
     * it stops the count for a while. That is what the word triage means and what the
     * mod had no mechanic for: until this, a medic sixty seconds away was a medic who
     * arrived to a body.
     *
     * <p>The count is what stops it becoming immortality. Every hold on the same downing
     * is worth less than the last one, so bandages buy minutes and never the argument.
     */
    private int stabilisedTicks;
    private int holds;

    public boolean downed() {
        // A stopped heart is still being down, and every rule about a body on the floor
        // has to keep applying while it is stopped: mobs losing interest, the ragdoll
        // holding, a blow costing time instead of health, being carried, being packed.
        // Making arrest a second state beside this one would have meant finding every
        // one of those checks and adding an "or" to it, and the first one missed would
        // have been the bug.
        return downedTicks > 0 || arrestTicks > 0;
    }

    public boolean stabilised() {
        return stabilisedTicks > 0;
    }

    public int stabilisedTicks() {
        return stabilisedTicks;
    }

    /** @return the ticks actually granted, which shrinks with every previous hold */
    public int stabilise(int fullTicks, float falloff) {
        int granted = Math.round(fullTicks * (float) Math.pow(falloff, holds));
        if (granted <= 0) {
            return 0;
        }
        holds++;
        stabilisedTicks = Math.max(stabilisedTicks, granted);
        dirty = true;
        return granted;
    }

    /** A blow undoes the packing. Whatever was holding it closed no longer is. */
    public void unstabilise() {
        if (stabilisedTicks > 0) {
            stabilisedTicks = 0;
            dirty = true;
        }
    }

    /** Counts the hold down. @return true when it just ran out */
    public boolean tickStabilised(int ticks) {
        if (stabilisedTicks <= 0) {
            return false;
        }
        stabilisedTicks = Math.max(0, stabilisedTicks - ticks);
        dirty = true;
        return stabilisedTicks == 0;
    }

    /**
     * How long this body has been left to breathe by itself.
     *
     * <p>An unconscious airway is not a wound and never appears on the figure: there is
     * nothing to see, nothing to bandage and nothing to spend on it. It is a clock that
     * starts the moment somebody goes down and is put back to nothing by a pair of hands
     * - which is the whole reason it exists, because it is the one thing in the mod that
     * asks for a person rather than for an item.
     */
    private int airwayTicks;

    /**
     * What is left of the window after the clock ran out, and how many rounds of
     * compressions have already been spent on it.
     *
     * <p>Emptying a vital part stopped being instant death when the downed clock arrived.
     * This is the same argument one step further along: the clock running out is the
     * heart stopping rather than the person ending, and what happens next depends
     * entirely on whether anybody is standing there.
     *
     * <p>The count of rounds is what keeps it from being immortality, exactly as the
     * count of holds does for dressings: every round buys less than the one before, so a
     * pair of hands buys minutes and never the argument.
     */
    private int arrestTicks;
    private int cprRounds;

    /**
     * How much of a course is still holding the illness down, and how long it will be
     * worse for now that one has run out.
     *
     * <p>The course is not a cure and never becomes one. What it buys is quiet, for as
     * long as it is being taken - and the rebound is what makes stopping a decision
     * rather than a saving, which is the truest thing this mod can say about it.
     */
    private int courseTicks;
    private int reboundTicks;

    /**
     * How long this body is going to stay under.
     *
     * <p>Filed with the mind rather than with the wounds, which is where it belongs: the
     * body is exactly as damaged as it was a minute ago and the person is not in it.
     *
     * <p>It is the only state in the mod that is entirely good for the patient and
     * entirely dangerous at the same time. Nothing is dying on a clock while it runs -
     * there is no reviving anybody from this, they simply come round - and the dose that
     * puts somebody under is one step from the dose that stops their breathing.
     */
    private int underTicks;

    /** True while a course is holding it down. */
    public boolean medicated() {
        return courseTicks > 0;
    }

    public int courseTicks() {
        return courseTicks;
    }

    public boolean rebounding() {
        return reboundTicks > 0;
    }

    public int reboundTicks() {
        return reboundTicks;
    }

    public void takeCourse(int ticks) {
        courseTicks = Math.max(courseTicks, Math.max(0, ticks));
        reboundTicks = 0;
        dirty = true;
    }

    /** Counts the course down. @return true when it has just run out */
    public boolean tickCourse(int ticks) {
        if (courseTicks <= 0) {
            return false;
        }
        courseTicks = Math.max(0, courseTicks - ticks);
        dirty = true;
        return courseTicks == 0;
    }

    public void startRebound(int ticks) {
        reboundTicks = Math.max(0, ticks);
        dirty = true;
    }

    public void tickRebound(int ticks) {
        if (reboundTicks > 0) {
            reboundTicks = Math.max(0, reboundTicks - ticks);
            dirty = true;
        }
    }

    public int airwayTicks() {
        return airwayTicks;
    }

    public void tickAirway(int ticks) {
        airwayTicks += ticks;
        dirty = true;
    }

    /**
     * Whether air has stopped getting in on its own.
     *
     * <p>The rule lives here rather than beside the thing that acts on it because both
     * sides need it: the server to take time off the clock, and the chart to say why the
     * clock is going down twice as fast as the one the reader was looking at a minute
     * ago.
     */
    public boolean airwayBlocked(int afterTicks) {
        return downed() && airwayTicks >= afterTicks;
    }

    /** Somebody has hold of it. @return true when there was anything to clear */
    public boolean openAirway() {
        if (airwayTicks == 0) {
            return false;
        }
        airwayTicks = 0;
        dirty = true;
        return true;
    }

    /** True while the heart has stopped and somebody else is the only reason it matters. */
    public boolean arrested() {
        return arrestTicks > 0;
    }

    public int arrestTicks() {
        return arrestTicks;
    }

    public void setArrestTicks(int ticks) {
        arrestTicks = Math.max(0, ticks);
        dirty = true;
    }

    /**
     * What a round of compressions would buy right now, without spending one.
     *
     * <p>Split from the giving of it for the same reason asking about a painkiller is
     * split from swallowing one: a round that buys nothing must not cost anything. The
     * first version of this counted every press, so a player who did not know the rhythm
     * and simply held the button down burned the whole budget on a window that was
     * already longer than what the press was offering - and got nothing at all, twelve
     * times, and then a patient who could no longer be held.
     */
    public int compressionWorth(int fullTicks, float falloff) {
        return Math.round(fullTicks * (float) Math.pow(falloff, cprRounds));
    }

    /**
     * Give back some of what the falloff has taken.
     *
     * <p>What adrenaline does in a resuscitation. It does not restart anything - nothing
     * in a needle restarts a heart - it makes the next round of compressions worth what
     * the first one was, which is exactly why it is given between cycles.
     */
    public void freshRounds(int rounds) {
        cprRounds = Math.max(0, cprRounds - rounds);
        dirty = true;
    }

    /** One round of compressions actually given, at the worth already worked out. */
    public void compress(int granted) {
        cprRounds++;
        arrestTicks = Math.max(arrestTicks, granted);
        dirty = true;
    }

    /** Counts the window down. @return true when it just ran out */
    public boolean tickArrest(int ticks) {
        if (arrestTicks <= 0) {
            return false;
        }
        arrestTicks = Math.max(0, arrestTicks - ticks);
        dirty = true;
        return arrestTicks == 0;
    }

    /**
     * Off the floor, however it ended.
     *
     * <p>One door out rather than three. Everything that is only true of a body lying
     * there - the clock, the hold, the held airway, the stopped heart and the rounds
     * spent on it - is cleared in one place, because the last time this state had two
     * ways out one of them quietly skipped a step.
     */
    public void clearDowned() {
        downedTicks = 0;
        arrestTicks = 0;
        cprRounds = 0;
        stabilisedTicks = 0;
        holds = 0;
        airwayTicks = 0;
        dirty = true;
    }

    public int downedTicks() {
        return downedTicks;
    }

    public void setDownedTicks(int ticks) {
        downedTicks = Math.max(0, ticks);
        // Standing up, dying, or being put down fresh all end the current hold: the
        // count of them is per downing, or a player who has been rescued once would be
        // harder to hold the next time for no reason anybody could see.
        if (downedTicks == 0) {
            stabilisedTicks = 0;
            holds = 0;
        }
        dirty = true;
    }

    /** 0 while full of blood, 1 when there is none left to lose. */
    public float bloodLoss() {
        return bloodLoss;
    }

    public void addBloodLoss(float amount) {
        float next = Mth.clamp(bloodLoss + amount, 0.0F, 1.0F);
        if (next != bloodLoss) {
            bloodLoss = next;
            dirty = true;
        }
    }

    private Map<BodyPart, PartState> partView() {
        return parts;
    }

    private Map<Disease, Integer> diseaseView() {
        return diseases;
    }

    // ---- Illness ----------------------------------------------------------

    public boolean has(Disease disease) {
        return diseases.containsKey(disease);
    }

    public boolean ill() {
        return !diseases.isEmpty();
    }

    public Set<Disease> diseases() {
        return diseases.keySet();
    }

    /** How long this illness has been running, in ticks. */
    public int diseaseTicks(Disease disease) {
        return diseases.getOrDefault(disease, 0);
    }

    /**
     * Puts one illness's clock back to nothing.
     *
     * <p>For the two that end on a condition rather than a duration. Shock lifts after a
     * stretch of lying still with nothing still open, and hypothermia after a stretch of
     * being warm and dry - so every second that is not that stretch has to take the
     * count back to zero, or "thirty seconds of rest" would quietly mean "thirty seconds,
     * whenever, in any order".
     */
    public void resetDiseaseTicks(Disease disease) {
        if (diseases.containsKey(disease) && diseases.get(disease) != 0) {
            diseases.put(disease, 0);
            dirty = true;
        }
    }

    public void addDiseaseTicks(Disease disease, int ticks) {
        if (diseases.containsKey(disease)) {
            diseases.merge(disease, ticks, Integer::sum);
            dirty = true;
        }
    }

    /** @return true when this is news, rather than something already being suffered */
    public boolean catchDisease(Disease disease) {
        if (diseases.containsKey(disease)) {
            return false;
        }
        diseases.put(disease, 0);
        dirty = true;
        return true;
    }

    public boolean cure(Disease disease) {
        if (diseases.remove(disease) == null) {
            return false;
        }
        dirty = true;
        return true;
    }

    /**
     * The illness most worth spending a course of antibiotics on, ignoring the ones no
     * course would touch. Without that filter a concussed player could swallow a course
     * and watch it cure their concussion, which is not what any of this means.
     */
    public @Nullable Disease worstDisease() {
        Disease worst = null;
        for (Disease disease : diseases.keySet()) {
            if (!disease.treatable()) {
                continue;
            }
            if (worst == null || disease.severity() > worst.severity()) {
                worst = disease;
            }
        }
        return worst;
    }

    public PartState part(BodyPart part) {
        return parts.computeIfAbsent(part, PartState::full);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public int painkillerTicks() {
        return painkillerTicks;
    }

    public void setPainkillerTicks(int ticks) {
        painkillerTicks = Math.max(0, ticks);
    }

    /** True while painkillers are hiding the effects of whatever is wrong. */
    public boolean painkilled() {
        return painkillerTicks > 0;
    }

    /** Counts the painkillers down. Returns true when they just ran out. */
    public boolean tickPainkillers(int ticks) {
        if (painkillerTicks <= 0) {
            return false;
        }
        painkillerTicks = Math.max(0, painkillerTicks - ticks);
        return painkillerTicks == 0;
    }

    // ---- Reading the body -------------------------------------------------

    public float total() {
        float sum = 0.0F;
        for (BodyPart part : BodyPart.VALUES) {
            sum += part(part).health;
        }
        return sum;
    }

    public float totalMax() {
        float sum = 0.0F;
        for (BodyPart part : BodyPart.VALUES) {
            sum += part(part).max;
        }
        return sum;
    }

    /** How much of the body is left, all seven parts counted together. */
    public float overallRatio() {
        float max = totalMax();
        return max <= 0.0F ? 0.0F : Mth.clamp(total() / max, 0.0F, 1.0F);
    }

    /** How close the player is to dying: the worse of head and thorax. */
    public float vitalRatio() {
        float worst = 1.0F;
        for (BodyPart part : BodyPart.VALUES) {
            if (part.vital()) {
                worst = Math.min(worst, part(part).ratio());
            }
        }
        return worst;
    }

    /**
     * What the vanilla health bar should read. The worse of the two ratios, so the bar
     * never claims a player is fine when their chest is nearly gone, and never claims
     * they are dying when only their arms are.
     */
    public float mirrorRatio() {
        return Math.min(overallRatio(), vitalRatio());
    }

    public boolean isDead() {
        for (BodyPart part : BodyPart.VALUES) {
            if (part.vital() && part(part).blacked()) {
                return true;
            }
        }
        return false;
    }

    public boolean blacked(BodyPart part) {
        return part(part).blacked();
    }

    /**
     * Whether there is anything left to stand on.
     *
     * <p>False only when both legs are gone and neither has anything fitted to it. A
     * wrecked leg still holds a body up - badly, and at a price the attributes already
     * charge - because a blacked-out one comes back off an operating table in ten
     * seconds and this is a state nothing undoes. One is a bad afternoon; the other is
     * the rest of the game.
     *
     * <p>One wooden leg is enough. That is the whole argument for having built them.
     */
    /**
     * Whether they are on the floor because their body stopped asking permission.
     *
     * <p>Never confused with {@link #downed()} anywhere in the mod, and the separation is
     * the point: that one is a clock counting towards a grave and this one is seven
     * seconds. Nothing treats this, nothing has to, and nothing about it is remembered
     * once it is over.
     */
    public boolean fainted() {
        return faintTicks > 0;
    }

    public int faintTicks() {
        return faintTicks;
    }

    public void setFaint(int ticks) {
        faintTicks = Math.max(faintTicks, Math.max(0, ticks));
        dirty = true;
    }

    /** @return true when they have just come round */
    public boolean tickFaint(int ticks) {
        if (faintTicks <= 0) {
            return false;
        }
        faintTicks = Math.max(0, faintTicks - ticks);
        dirty = true;
        return faintTicks == 0;
    }

    public void clearFaint(int grace) {
        faintTicks = 0;
        faintGrace = Math.max(0, grace);
        dirty = true;
    }

    public int faintGrace() {
        return faintGrace;
    }

    public void tickFaintGrace(int ticks) {
        faintGrace = Math.max(0, faintGrace - ticks);
    }

    public float tired() {
        return tired;
    }

    /**
     * How long this body has been doing nothing, in ticks.
     *
     * <p>Not in the codec, and beside fatigue for the same reason: it is a fact about the
     * last minute rather than an injury, and a player who logs out has stopped being still
     * in the only sense that matters.
     */
    public transient int stillness;
    /**
     * How far this body had walked when it was last asked.
     *
     * <p>The distance counter vanilla keeps for hunger, sampled once a second and
     * subtracted from itself. A player's velocity is not a thing the server can read -
     * it is nearly always zero over there, whatever the player is doing - so asking it
     * "are you moving" would have answered "no" for everybody, for ever.
     */
    public transient float walked;

    public int stillness() {
        return stillness;
    }

    public void tickStillness(int by, int ceiling) {
        stillness = Mth.clamp(stillness + by, 0, ceiling);
    }

    // ---- the notes --------------------------------------------------------

    /**
     * One more line on the page.
     *
     * <p>Every reading in the mod comes through here as well as into its own field. The
     * field is what the chart prints at the top; this is what it is compared against, and
     * until this version there was nothing to compare anything against at all.
     */
    public void record(int kind, int value, String by, long at) {
        log.add(new Reading(kind, value, at, by));
        // Four of this kind, then the oldest of this kind goes - never the oldest line on
        // the page. A table beside a patient writes two lines a minute on its own, and a
        // flat cap would have that quietly throw away the pulse a medic took by hand,
        // which is the one line on the page somebody paid for.
        trim(kind);
        while (log.size() > LOG_LIMIT) {
            log.remove(0);
        }
        dirty = true;
    }

    private void trim(int kind) {
        int count = 0;
        for (Reading reading : log) {
            if (reading.kind() == kind) {
                count++;
            }
        }
        while (count > LOG_PER_KIND) {
            for (int i = 0; i < log.size(); i++) {
                if (log.get(i).kind() == kind) {
                    log.remove(i);
                    break;
                }
            }
            count--;
        }
    }

    /** The page, oldest first. */
    public List<Reading> readings() {
        return List.copyOf(log);
    }

    /** The newest of one kind, or null when nobody has taken one. */
    public @Nullable Reading latest(int kind) {
        for (int i = log.size() - 1; i >= 0; i--) {
            if (log.get(i).kind() == kind) {
                return log.get(i);
            }
        }
        return null;
    }

    /**
     * The one before that, which is the half that matters.
     *
     * <p>Null until somebody has measured the same thing twice - and that is the mechanic
     * rather than a limitation. A direction cannot be bought, borrowed or read off a
     * healthy-looking person: it costs a second visit.
     */
    public @Nullable Reading previous(int kind) {
        boolean seen = false;
        for (int i = log.size() - 1; i >= 0; i--) {
            if (log.get(i).kind() != kind) {
                continue;
            }
            if (seen) {
                return log.get(i);
            }
            seen = true;
        }
        return null;
    }

    /** A pulse somebody counted, with their name on it and the tick they counted it. */
    public void notePulse(Pulse pulse, String by, long at) {
        pulseRate = pulse.rate();
        pulseCharacter = pulse.character();
        pulseBy = by;
        pulseAt = at;
        record(Reading.PULSE, pulse.rate(), by, at);
        dirty = true;
    }

    /** What the notes say the heart was doing, or null when nobody has ever felt it. */
    public @Nullable Pulse notedPulse() {
        return pulseCharacter < 0 ? null : new Pulse(pulseRate, pulseCharacter);
    }

    public String pulseBy() {
        return pulseBy;
    }

    public long pulseAt() {
        return pulseAt;
    }

    public void noteTemp(int tenths, String by, long at) {
        tempTenths = tenths;
        tempBy = by;
        tempAt = at;
        record(Reading.TEMP, tenths, by, at);
        dirty = true;
    }

    /** Tenths of a degree, or zero when nobody has taken one. */
    public int notedTemp() {
        return tempTenths;
    }

    public String tempBy() {
        return tempBy;
    }

    public long tempAt() {
        return tempAt;
    }

    /**
     * What they said it was worth out of ten, written down at last.
     *
     * <p>This method existed for two versions and nothing ever called it: the score was
     * said out loud in chat and then thrown away. An opinion that is not written down
     * cannot be compared with the same opinion twenty minutes later, which is the only
     * thing a pain score has ever been for.
     */
    public void noteSaid(int outOfTen, String by, long at) {
        said = outOfTen;
        saidAt = at;
        record(Reading.PAIN, outOfTen, by, at);
        dirty = true;
    }

    /** Breaths a minute, off the one instrument that can count them. */
    public void noteBreath(int rate, String by, long at) {
        record(Reading.BREATH, rate, by, at);
        dirty = true;
    }

    public int notedSaid() {
        return said;
    }

    public long saidAt() {
        return saidAt;
    }

    public void noteFilm(String by, long at) {
        filmBy = by;
        filmAt = at;
        // A film answers about every bone in the picture at once, and about every piece of
        // metal in it - which is exactly what makes the machine worth its weight in the
        // mode where nothing else answers with certainty.
        for (BodyPart part : BodyPart.VALUES) {
            establish(part, Findings.BONE | Findings.METAL, at);
        }
        dirty = true;
    }

    public long filmAt() {
        return filmAt;
    }

    public String filmBy() {
        return filmBy;
    }

    /** @return true when the figure actually moved a step */
    public boolean setTired(float share) {
        float stepped = Math.round(Mth.clamp(share, 0.0F, 1.0F) * 64.0F) / 64.0F;
        if (Math.abs(stepped - tired) < 0.001F) {
            return false;
        }
        tired = stepped;
        dirty = true;
        return true;
    }

    /** Whether the person is not in the body just now. */
    public boolean under() {
        return underTicks > 0;
    }

    public int underTicks() {
        return underTicks;
    }

    public void putUnder(int ticks) {
        underTicks = Math.max(underTicks, Math.max(0, ticks));
    }

    /** @return true when they have just come round */
    public boolean tickUnder(int ticks) {
        if (underTicks <= 0) {
            return false;
        }
        underTicks = Math.max(0, underTicks - ticks);
        return underTicks == 0;
    }

    public void wake() {
        underTicks = 0;
    }

    public boolean canStand() {
        for (BodyPart part : BodyPart.VALUES) {
            if (!part.isLeg()) {
                continue;
            }
            PartState state = part(part);
            if (!state.gone() || state.prosthetic) {
                return true;
            }
        }
        return false;
    }

    /**
     * On the floor for any reason: dying on a clock, or simply having nothing to stand
     * on. Everything that asks "can somebody pick this person up" wants this rather than
     * {@link #downed()} - being carried is about not being able to walk, and it never
     * mattered why.
     */
    public boolean floored() {
        return downed() || !canStand() || under() || fainted();
    }

    /** True when anything at all is wrong: a damaged part, an illness, or blood lost. */
    public boolean anythingWrong() {
        if (ill() || bloodLoss > 0.05F) {
            return true;
        }
        for (BodyPart part : BodyPart.VALUES) {
            if (part(part).hurt()) {
                return true;
            }
        }
        return false;
    }

    /** The part in the worst shape among those the filter accepts, or null. */
    public @Nullable BodyPart worst(java.util.function.Predicate<BodyPart> filter) {
        BodyPart worst = null;
        float lowest = Float.MAX_VALUE;
        for (BodyPart part : BodyPart.VALUES) {
            if (!filter.test(part)) {
                continue;
            }
            float ratio = part(part).ratio();
            if (ratio < lowest) {
                lowest = ratio;
                worst = part;
            }
        }
        return worst;
    }

    // ---- Changing the body ------------------------------------------------

    /**
     * Damages one part. What a blacked-out limb cannot absorb runs into the thorax at
     * the rate the config sets - a wrecked leg keeps costing you, which is what makes
     * losing one matter after the moment it happens.
     */
    public void hurt(BodyPart part, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        float overflow = part(part).damage(amount);
        dirty = true;
        if (overflow <= 0.0F || part.vital()) {
            return;
        }
        float carried = overflow * Config.values().overflowToThorax;
        if (carried > 0.0F) {
            part(BodyPart.THORAX).damage(carried);
        }
    }

    /**
     * Spreads healing over everything that is damaged and not blacked out, in proportion
     * to how much each part is missing.
     *
     * @return how much of the healing was actually used
     */
    public float healAll(float amount) {
        if (amount <= 0.0F) {
            return 0.0F;
        }
        float missing = 0.0F;
        for (BodyPart part : BodyPart.VALUES) {
            if (mendable(part)) {
                PartState state = part(part);
                missing += state.max - state.health;
            }
        }
        if (missing <= 0.0F) {
            return 0.0F;
        }

        float budget = Math.min(amount, missing);
        float used = 0.0F;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = part(part);
            if (!mendable(part)) {
                continue;
            }
            float share = budget * ((state.max - state.health) / missing);
            used += share - state.heal(share);
        }
        if (used > 0.0F) {
            dirty = true;
        }
        return used;
    }

    /**
     * Whether the body can mend this part by itself. A blacked-out part needs surgery,
     * a dirty one needs cleaning first, and a burn does not knit under a scab - all
     * three are what makes the medicine worth carrying rather than just waiting.
     *
     * <p>Except the shallowest burn, which is the one that does knit. Red skin closes on
     * its own and always did; blocking it was the flag's fault rather than a decision, and
     * it turned every brush with a campfire into an errand for a jar.
     */
    private boolean mendable(BodyPart part) {
        PartState state = part(part);
        return !state.blacked() && !state.infected() && state.burn < PartState.BURN_PARTIAL
                && !state.tourniquet && !state.stuck() && !state.necrotic() && !state.gone();
    }

    /** Damage that has no single point of impact: drowning, poison, fire, starvation. */
    public void hurtAll(float amount) {
        if (amount <= 0.0F) {
            return;
        }
        float alive = 0.0F;
        for (BodyPart part : BodyPart.VALUES) {
            alive += part(part).health;
        }
        if (alive <= 0.0F) {
            // Everything is already blacked out; the thorax takes the rest.
            hurt(BodyPart.THORAX, amount);
            return;
        }
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = part(part);
            if (state.health > 0.0F) {
                state.damage(amount * (state.health / alive));
            }
        }
        dirty = true;
    }

    public void resetAll() {
        for (BodyPart part : BodyPart.VALUES) {
            parts.put(part, PartState.full(part));
        }
        diseases.clear();
        painkillerTicks = 0;
        bloodLoss = 0.0F;
        downedTicks = 0;
        stabilisedTicks = 0;
        holds = 0;
        arrestTicks = 0;
        cprRounds = 0;
        airwayTicks = 0;
        courseTicks = 0;
        reboundTicks = 0;
        painkillerTolerance = 0;
        toleranceTicks = 0;
        // The two that are not in the codec still have to go. Reset means reset, and a
        // body that comes back whole while its owner is still crashing off an injury it
        // no longer has is not a body anybody asked for.
        fatigue = 0.0F;
        adrenalineTicks = 0;
        crashTicks = 0;
        chill = 0.0F;
        faintTicks = 0;
        faintGrace = 0;
        tired = 0.0F;
        // A new body has never been read. Whatever anybody wrote about the last one was
        // about the last one.
        pulseCharacter = -1;
        pulseRate = 0;
        pulseAt = 0L;
        pulseBy = "";
        tempTenths = 0;
        tempAt = 0L;
        tempBy = "";
        said = -1;
        saidAt = 0L;
        filmAt = 0L;
        filmBy = "";
        log.clear();
        // A mark describes a wounded body, and this one no longer exists. Whatever
        // anybody wrote on it went with it.
        tag = null;
        tagBy = "";
        tagTicks = 0;
        // Cleared rather than kept: death is what rolls a new type, which is what stops
        // a stockpile of matching bags being a permanent solution to blood loss.
        bloodType = null;
        dirty = true;
    }

    /**
     * Brings saved data back in line with the config as it stands now. A pack that makes
     * legs tougher should not hand out the difference as free health, so the part keeps
     * the share of itself it had.
     */
    public void reconcile() {
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = part(part);
            // What the config says this part is worth, less whatever a bone in it lost
            // by healing out of line. The only permanent subtraction in the mod, and it
            // lives here so that every path which rebuilds a maximum agrees about it.
            float max = part.maxHealth() * Bones.maxShare(state);
            if (state.max != max) {
                float ratio = state.ratio();
                state.max = max;
                state.health = Mth.clamp(max * ratio, 0.0F, max);
                dirty = true;
            }
        }
    }

    public BodyHealth copy() {
        return new BodyHealth(parts, painkillerTicks, diseases, bloodLoss, downedTicks,
                Optional.ofNullable(bloodType), stabilisedTicks, holds, painkillerTolerance,
                Optional.ofNullable(tag), tagBy, tagTicks, airwayTicks, arrestTicks, cprRounds,
                Mind.of(this));
    }

    public void copyFrom(BodyHealth other) {
        parts.clear();
        for (BodyPart part : BodyPart.VALUES) {
            parts.put(part, other.part(part).copy());
        }
        diseases.clear();
        diseases.putAll(other.diseases);
        painkillerTicks = other.painkillerTicks;
        bloodLoss = other.bloodLoss;
        downedTicks = other.downedTicks;
        bloodType = other.bloodType;
        stabilisedTicks = other.stabilisedTicks;
        holds = other.holds;
        painkillerTolerance = other.painkillerTolerance;
        tag = other.tag;
        tagBy = other.tagBy;
        tagTicks = other.tagTicks;
        airwayTicks = other.airwayTicks;
        arrestTicks = other.arrestTicks;
        cprRounds = other.cprRounds;
        courseTicks = other.courseTicks;
        reboundTicks = other.reboundTicks;
        dirty = true;
    }

    // ---- what has been found out ------------------------------------------

    /** Whether this fact about this part has been established. See {@link Findings}. */
    public boolean found(BodyPart part, int fact) {
        return (found & Findings.bit(part, fact)) != 0L;
    }

    /**
     * Writes a finding down, with the moment it was made.
     *
     * <p>Several facts at once on purpose: hands on a limb learn about the bone, the flesh
     * and whatever is sitting under the skin in the same movement, and the time they were
     * learnt is the same time.
     */
    public void establish(BodyPart part, int facts, long now) {
        found |= Findings.bit(part, facts);
        foundAt[part.ordinal()] = now;
        dirty = true;
    }

    /**
     * Unlearns everything about a part.
     *
     * <p>Called when the part changes under the medic - a new break, a new wound, a limb
     * taken off. A finding is about a body at a moment, and the moment has passed.
     */
    public void forget(BodyPart part) {
        long mask = Findings.bit(part, Findings.ALL);
        if ((found & mask) == 0L) {
            return;
        }
        found &= ~mask;
        foundAt[part.ordinal()] = 0L;
        dirty = true;
    }

    /** When this part was last examined, or 0 if it never was. */
    public long examinedAt(BodyPart part) {
        return foundAt[part.ordinal()];
    }

    /** Whether a fact about the whole person has been established. See {@link Findings}. */
    public boolean foundWhole(int fact) {
        return (found & Findings.wholeBit(fact)) != 0L;
    }

    public void establishWhole(int fact) {
        found |= Findings.wholeBit(fact);
        dirty = true;
    }

    /**
     * A copy of this body with everything nobody has found out taken out of it.
     *
     * <p>This is the whole of realistic difficulty on the wire. The server keeps the truth
     * and sends this instead, so what a client cannot draw is also what a client was never
     * told - otherwise the mode is worth exactly what a modified client says it is worth.
     *
     * <p>Two rules, and the second one matters more than it looks. The first: a fact is
     * sent only if somebody established it, or if it is plain to look at - see
     * {@link Findings#plain}. The second: what is taken out is replaced by <b>the value
     * that means nothing is wrong</b>, never by a sentinel. A blanked bone is zero breaks,
     * a blanked wound is no infection. That way the failure mode of every piece of client
     * code that forgets to ask whether a fact is known is to draw a healthy body - which
     * is wrong, and safe. The chart knows the difference because the findings travel with
     * it: zero breaks and "nobody looked" are different answers to different questions.
     */
    public BodyHealth asSeen() {
        return asSeen(false);
    }

    /**
     * @param ownBody whether this is the body of whoever is looking, which changes three
     *                answers and nothing else. How bad a part is, how weak you feel and
     *                which blood you have are things a person has about themselves without
     *                being examined - the first two because they hurt and the third because
     *                somebody told you once. Everything else about your own inside is as
     *                closed to you as it is to anybody: you do not know your rib is cracked,
     *                you know your chest hurts.
     */
    public BodyHealth asSeen(boolean ownBody) {
        BodyHealth seen = copy();
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = seen.part(part);
            if (!ownBody && !knows(part, Findings.FLESH, state)) {
                state.health = state.max;
            }
            if (!knows(part, Findings.BONE, state)) {
                state.breaks = 0;
                state.cracked = false;
                state.crackTicks = 0;
                state.knits = 0;
                state.displaced = false;
                state.openBreak = false;
            }
            if (!knows(part, Findings.ROT, state)) {
                state.infection = PartState.INFECTION_NONE;
                state.infectionTicks = 0;
                state.septic = false;
            }
            // That there is a burn is on the skin and stays. How deep it went is a finding,
            // and until somebody presses on it every burn looks like the shallow kind.
            if (!knows(part, Findings.BURN, state) && state.burned) {
                state.burn = PartState.BURN_SUPERFICIAL;
                state.burnt = PartState.BURN_NONE;
            }
            // An arrow standing out of somebody is not a finding. What is under the skin is.
            if (!knows(part, Findings.METAL, state)) {
                state.shrapnel = false;
            }
            if (!knows(part, Findings.ORGAN, state)) {
                state.organs = 0;
                state.organTicks = 0;
            }
            if (!knows(part, Findings.CLOT, state)) {
                state.clot = false;
                state.clotTicks = 0;
            }
            if (!knows(part, Findings.BREATH, state)) {
                state.pneumo = false;
                state.pneumoTicks = 0;
            }
        }

        // How much blood is left is never a number anybody can read off a person. What is
        // readable is the colour of them, so what survives is the three bands that change
        // it: none to speak of, pale, grey. The figure still goes pale; the chart has no
        // percentage to print, and the pulse is what a medic reads instead.
        if (!ownBody) {
            seen.bloodLoss = seen.bloodLoss < 0.25F ? 0.0F : seen.bloodLoss < 0.5F ? 0.3F : 0.6F;
        }

        // An illness is known by its signs - a temperature, what they tell you, what they
        // are doing - and never by looking at somebody and being told the name of it.
        seen.diseases.clear();

        // And which blood they have, which is the one fact here that is not about the body
        // at all: it is something a person knows and says. Ask them, or read their band.
        // Which blood they have survives if they have said so or are wearing it. The mod
        // argued once that hiding this makes transfusion a coin toss, and that argument
        // still holds - so the fact is not taken away, only the telepathy.
        if (!ownBody && !foundWhole(Findings.GROUP)) {
            seen.bloodType = null;
        }
        return seen;
    }

    /** Whether this fact may be sent: somebody found it, or anybody could see it. */
    private boolean knows(BodyPart part, int fact, PartState state) {
        return found(part, fact) || Findings.plain(state, fact);
    }
}
