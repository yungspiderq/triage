package com.claudemods.triage.body;

import java.util.ArrayList;
import java.util.List;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

/**
 * The condition of one body part: how much of it is left, and everything that is wrong
 * with it beyond the missing health.
 *
 * <p>The maximum is stored rather than read from the config every time, so that a
 * config change is noticed once, on login, instead of silently moving every player's
 * health mid-fight.
 */
public final class PartState {
    /** Nothing above this counts as bleeding; 1 is a light bleed, 2 a heavy one. */
    public static final int BLEED_NONE = 0;
    public static final int BLEED_LIGHT = 1;
    public static final int BLEED_HEAVY = 2;

    /**
     * A vessel with a pump behind it.
     *
     * <p>The mod had two kinds of bleeding and they were the same kind at two speeds.
     * This is the other thing entirely, and the difference is not the rate - it is that
     * a heavy bleed is a body losing blood and this is a body <b>throwing it away</b>.
     * It runs several times faster than anything else here, it is bright, it goes in
     * time with the heart, and nothing that works on the other two works on it: it does
     * not clot, a dressing over the top of it soaks through, and the count to a body on
     * the floor is measured in tens of seconds rather than minutes.
     *
     * <p>Which leaves exactly two answers, and both of them were already in the mod
     * waiting for something that needed them. A hand pressed hard into it holds while
     * the hand is there, and a strap above it stops it dead at the price the strap
     * always charges. That is the whole of it, and it is the whole of it in life too.
     *
     * <p>Deliberately rare, and rarer than a heavy bleed by a long way. A wound this
     * bad turning up every other fight would not be frightening, it would be weather.
     */
    public static final int BLEED_ARTERIAL = 3;

    /** Value of {@link #bleedTicks} for a bleed that will not stop on its own. */
    public static final int BLEED_FOREVER = -1;

    /** Nothing inside is hurt; something is bruised; something is open. */
    public static final int ORGAN_NONE = 0;
    public static final int ORGAN_BRUISED = 1;
    public static final int ORGAN_TORN = 2;

    public static final int INFECTION_NONE = 0;
    public static final int INFECTION_MILD = 1;
    /** Bad enough to make the whole body ill, not just the part. */
    public static final int INFECTION_SEVERE = 2;

    private static final MapCodec<PartState> BASE = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("health").forGetter(s -> s.health),
            Codec.FLOAT.fieldOf("max").forGetter(s -> s.max),
            Codec.INT.optionalFieldOf("bleed", BLEED_NONE).forGetter(s -> s.bleed),
            Codec.INT.optionalFieldOf("bleed_ticks", BLEED_FOREVER).forGetter(s -> s.bleedTicks),
            Codec.BOOL.optionalFieldOf("fractured", false).forGetter(s -> s.breaks > 0),
            Codec.INT.optionalFieldOf("breaks", 0).forGetter(s -> s.breaks),
            Codec.BOOL.optionalFieldOf("cracked", false).forGetter(s -> s.cracked),
            Codec.INT.optionalFieldOf("crack_ticks", 0).forGetter(s -> s.crackTicks),
            Codec.INT.optionalFieldOf("infection", INFECTION_NONE).forGetter(s -> s.infection),
            Codec.INT.optionalFieldOf("infection_ticks", 0).forGetter(s -> s.infectionTicks),
            Codec.BOOL.optionalFieldOf("burned", false).forGetter(s -> s.burned),
            Codec.INT.optionalFieldOf("dirty_ticks", 0).forGetter(s -> s.dirtyTicks),
            Codec.BOOL.optionalFieldOf("tourniquet", false).forGetter(s -> s.tourniquet),
            Codec.BOOL.optionalFieldOf("pneumo", false).forGetter(s -> s.pneumo),
            Codec.INT.optionalFieldOf("pneumo_ticks", 0).forGetter(s -> s.pneumoTicks),
            Extra.CODEC.optionalFieldOf("extra", Extra.NONE).forGetter(Extra::of)
    ).apply(i, PartState::new));

    /**
     * The seventeenth field, and everything after that.
     *
     * <p>{@link Extra} was the sixteenth and is now full itself, so the next shelf goes
     * up beside it rather than inside it: two map codecs written into the same object,
     * which is why nothing above had to be renamed and why every save from every earlier
     * version still loads exactly as it did.
     *
     * <p>What lives here is two things that are new in kind rather than in degree. The
     * first six are the only state in this class that <b>outlives what caused it</b> -
     * a body that has been put back together is not the body it was before, and until
     * now the mod said it was. The last two are the stick strapped to the outside.
     */
    public static final Codec<PartState> CODEC = Codec.mapPair(BASE,
                    Later.CODEC.optionalFieldOf("later", Later.NONE))
            .codec()
            .xmap(pair -> pair.getFirst().with(pair.getSecond()), state -> Pair.of(state, Later.of(state)));

    /**
     * What the part has been through, and what is tied to it now.
     *
     * <p>The counts never go down. That is the point of them: they are not a condition,
     * they are a history, and the only thing in this mod that clears one is dying.
     */
    private record Later(int knits, int cuts, int burnt, boolean septic, float stiff, int worked,
                         boolean splint, int splintTicks, boolean displaced, boolean open,
                         int crooked, boolean clot, int clotTicks, int bullets, CompoundTag blade,
                         List<String> bulletTypes) {
        private static final Later NONE =
                new Later(0, 0, 0, false, 0.0F, 0, false, 0, false, false, 0, false, 0, 0,
                        new CompoundTag(), List.of());

        private static final Codec<Later> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("knits", 0).forGetter(Later::knits),
                Codec.INT.optionalFieldOf("cuts", 0).forGetter(Later::cuts),
                Codec.INT.optionalFieldOf("burnt", 0).forGetter(Later::burnt),
                Codec.BOOL.optionalFieldOf("septic", false).forGetter(Later::septic),
                Codec.FLOAT.optionalFieldOf("stiff", 0.0F).forGetter(Later::stiff),
                Codec.INT.optionalFieldOf("worked", 0).forGetter(Later::worked),
                Codec.BOOL.optionalFieldOf("splint", false).forGetter(Later::splint),
                Codec.INT.optionalFieldOf("splint_ticks", 0).forGetter(Later::splintTicks),
                Codec.BOOL.optionalFieldOf("displaced", false).forGetter(Later::displaced),
                Codec.BOOL.optionalFieldOf("open_break", false).forGetter(Later::open),
                Codec.INT.optionalFieldOf("crooked", 0).forGetter(Later::crooked),
                Codec.BOOL.optionalFieldOf("clot", false).forGetter(Later::clot),
                Codec.INT.optionalFieldOf("clot_ticks", 0).forGetter(Later::clotTicks),
                Codec.INT.optionalFieldOf("bullets", 0).forGetter(Later::bullets),
                // Somebody else's weapon, saved whole. The item rather than its name,
                // because what goes back to whoever pulls it out has to be the thing that
                // went in - enchantments, damage and all - or the mechanic is a way of
                // destroying people's swords rather than a way of taking one off them.
                CompoundTag.CODEC.optionalFieldOf("blade", new CompoundTag()).forGetter(Later::blade),
                // One entry per round, in the order they went in. A list rather than a
                // count per calibre because a wound is a sequence of things that happened,
                // and pulling one out should leave the rest exactly as they were.
                Codec.STRING.listOf().optionalFieldOf("bullet_types", List.of())
                        .forGetter(Later::bulletTypes)
        ).apply(i, Later::new));

        private static Later of(PartState state) {
            return new Later(state.knits, state.cuts, state.burnt, state.septic, state.stiff,
                    state.worked, state.splint, state.splintTicks, state.displaced,
                    state.openBreak, state.crooked, state.clot, state.clotTicks,
                    state.bullets, state.blade, List.copyOf(state.bulletTypes));
        }
    }

    /** Reads the second shelf onto a part the first one has just built. */
    private PartState with(Later later) {
        knits = Math.max(0, later.knits());
        cuts = Math.max(0, later.cuts());
        burnt = Mth.clamp(later.burnt(), BURN_NONE, BURN_FULL);
        septic = later.septic();
        stiff = Mth.clamp(later.stiff(), 0.0F, 1.0F);
        worked = Math.max(0, later.worked());
        splint = later.splint();
        splintTicks = Math.max(0, later.splintTicks());
        // Neither of these can be true without a break under them. A save that somehow
        // claims one is read as the bone it describes rather than as a loose flag.
        displaced = later.displaced() && breaks > 0;
        openBreak = later.open() && breaks > 0;
        crooked = Math.max(0, later.crooked());
        clot = later.clot() && !missing;
        clotTicks = Math.max(0, later.clotTicks());
        // Nothing is left in a limb that is not there any more. A save that claims
        // otherwise is read as the limb, because the limb is the later fact.
        bullets = missing ? 0 : Math.max(0, later.bullets());
        // A round saved before the kinds were counted has a count and no names. It is
        // read as the generic round it was drawn as then, not as a missing injury: the
        // old save is the authority on how many are in there, and this list catches up.
        bulletTypes.clear();
        if (!missing) {
            List<String> stored = later.bulletTypes() == null ? List.of() : later.bulletTypes();
            for (String type : stored) {
                if (bulletTypes.size() >= bullets) {
                    break;
                }
                bulletTypes.add(BulletKinds.canonical(type));
            }
            while (bulletTypes.size() < bullets) {
                bulletTypes.add(BulletKinds.UNKNOWN);
            }
        }
        blade = missing ? new CompoundTag() : later.blade().copy();
        return this;
    }

    /**
     * The sixteenth field, and everything after it.
     *
     * <p>A record codec group tops out at sixteen entries, and the fifteen above are
     * spoken for. Rather than renaming what is already on disk to make room, anything
     * newer than this comment lives in here as a nested object - which is also where the
     * next one will go, and the one after that.
     *
     * <p>The first two are things somebody left in the wound rather than damage to it:
     * a hand held on it, and a shaft nobody has pulled out yet. The last four are the
     * other end of the same road - a limb that stopped being fed, then stopped being
     * there, then had something screwed on in its place.
     */
    private record Extra(int pressedTicks, int arrows, int deadTicks, boolean dead,
                         boolean missing, boolean prosthetic, boolean dressed, int dressedTicks,
                         boolean fine, int organ, int organTicks, int wear, boolean shrapnel,
                         int mending, int burn, int burnTicks) {
        private static final Extra NONE =
                new Extra(0, 0, 0, false, false, false, false, 0, false, 0, 0, 0, false, 0, 0, 0);

        private static final Codec<Extra> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("pressed_ticks", 0).forGetter(Extra::pressedTicks),
                Codec.INT.optionalFieldOf("arrows", 0).forGetter(Extra::arrows),
                Codec.INT.optionalFieldOf("dead_ticks", 0).forGetter(Extra::deadTicks),
                Codec.BOOL.optionalFieldOf("dead", false).forGetter(Extra::dead),
                Codec.BOOL.optionalFieldOf("missing", false).forGetter(Extra::missing),
                Codec.BOOL.optionalFieldOf("prosthetic", false).forGetter(Extra::prosthetic),
                Codec.BOOL.optionalFieldOf("dressed", false).forGetter(Extra::dressed),
                Codec.INT.optionalFieldOf("dressed_ticks", 0).forGetter(Extra::dressedTicks),
                Codec.BOOL.optionalFieldOf("fine", false).forGetter(Extra::fine),
                Codec.INT.optionalFieldOf("organ", 0).forGetter(Extra::organ),
                Codec.INT.optionalFieldOf("organ_ticks", 0).forGetter(Extra::organTicks),
                Codec.INT.optionalFieldOf("wear", 0).forGetter(Extra::wear),
                Codec.BOOL.optionalFieldOf("shrapnel", false).forGetter(Extra::shrapnel),
                Codec.INT.optionalFieldOf("mending", 0).forGetter(Extra::mending),
                Codec.INT.optionalFieldOf("burn", 0).forGetter(Extra::burn),
                Codec.INT.optionalFieldOf("burn_ticks", 0).forGetter(Extra::burnTicks)
        ).apply(i, Extra::new));

        private static Extra of(PartState state) {
            return new Extra(state.pressedTicks, state.arrows, state.deadTicks, state.dead,
                    state.missing, state.prosthetic, state.dressed, state.dressedTicks,
                    state.fineProsthetic, state.organs, state.organTicks, state.wear,
                    state.shrapnel, state.mending, state.burn, state.burnTicks);
        }
    }

    public static final int BURN_NONE = 0;
    /** Red, painful, and it will close on its own. Nothing has to be done about one. */
    public static final int BURN_SUPERFICIAL = 1;
    /** Blistered, and the most painful of the three. This is what a salve is for. */
    public static final int BURN_PARTIAL = 2;
    /** Through the skin. It does not hurt, it does not close, and a salve is no use. */
    public static final int BURN_FULL = 3;

    public float health;
    public float max;
    public int bleed;
    /** Ticks a light bleed has left, or {@link #BLEED_FOREVER}. Heavy bleeds never clot. */
    public int bleedTicks;

    /**
     * How many separate breaks this part is carrying.
     *
     * <p>A count rather than the flag it used to be, because a chest is ribs: it can be
     * broken in three places, and each of them is its own pass with a splint. Everywhere
     * else the ceiling is one, so the count behaves exactly as the flag did.
     *
     * <p>The old boolean is still read from disk and turns into a single break, so a body
     * saved before this exists loads with the fracture it had.
     */
    public int breaks;

    /** A bone that is not broken yet. Left alone it knits; leaned on, it breaks. */
    public boolean cracked;
    /** Ticks of not being leaned on. Reset by every hit and every sprint. */
    public int crackTicks;

    public int infection;
    /** How long this infection has been at its current level. */
    public int infectionTicks;

    /**
     * Whether there is a burn here at all. Kept because it is still the question most of
     * the mod is asking, and it is exactly {@code burn > 0}.
     */
    public boolean burned;

    /**
     * How deep it went: {@link #BURN_SUPERFICIAL}, {@link #BURN_PARTIAL}, {@link #BURN_FULL}.
     *
     * <p>This is the whole of what a burn is, and the mod had a boolean for it. Depth
     * decides everything about one: whether it hurts, whether it will close on its own,
     * how much fluid goes out through it, whether a salve is any use and whether a knife
     * is needed. A burn is not an amount of damage, it is a depth.
     *
     * <p>The one that matters is the deepest. A full-thickness burn is the only injury
     * in this mod that <b>does not hurt</b> - the nerve endings that would have done the
     * hurting are part of what burnt - and that is not a detail, it is how the bad ones
     * are told from the survivable ones at a glance. It is also why this mod now has a
     * way to test a burn rather than look at one.
     */
    public int burn;

    /**
     * How long this burn can still get worse.
     *
     * <p>The part everybody gets wrong: a burn is not over when the fire is out. Heat
     * sitting in tissue keeps cooking it for a good while afterwards, which is why the
     * first thing anybody is taught about burns is to put water on it and keep it there.
     * This is that window, and it is the only period in which anything anybody does
     * changes how deep it ends up.
     */
    public int burnTicks;

    /**
     * How long this part has been in a state that can go bad - open and bleeding, or
     * blacked out and left. Cleaning or closing the wound sets it back to nothing.
     */
    public int dirtyTicks;

    /**
     * A strap pulled tight above the wound. It stops the bleeding at once and starts
     * killing the limb: nothing below it is fed, nothing below it heals.
     */
    public boolean tourniquet;

    /**
     * Air where the lung should be expanding into, pressing it flat.
     *
     * <p>Only the thorax ever has this, and it is the one thing in the mod that gets
     * worse the longer it is left rather than at a steady rate: the pressure builds, so
     * the damage climbs. It is also the only injury with no outward sign - nothing
     * bleeds, nothing is bent - which is exactly why a chart is worth reading.
     *
     * <p>Nothing heals it and nothing waits it out. A needle between the ribs lets the
     * air out, and that is the whole treatment.
     */
    public boolean pneumo;
    /** How long the pressure has been building, which is what makes it accelerate. */
    public int pneumoTicks;

    /**
     * Ticks left of somebody's hands pressed onto this wound.
     *
     * <p>The only thing in the mod that stops bleeding without an item, and the only one
     * that stops being true by itself. Nothing is closed, nothing is cleaned and nothing
     * is healed: while the hands are there, nothing comes out, and when they go the wound
     * is exactly the wound it was. What it buys is the walk to the bandage.
     *
     * <p>A light bleed still counts down toward clotting underneath, so holding one long
     * enough does close it - which is what pressure is for and the only way a wound ever
     * closes here without a dressing.
     */
    public int pressedTicks;

    /**
     * How many shafts are still in this part.
     *
     * <p>An arrow that stays in is not extra damage - the damage already happened. What
     * it is, is a wound that cannot be dealt with: it will not clot, it will not mend,
     * and a dressing over the top of it is a dressing over an arrow. Getting it out is a
     * job for hands and it costs the patient a fresh bleed, which is the whole trade.
     */
    public int arrows;

    /**
     * How many bullets are still in this part.
     *
     * <p>The same problem as an arrow with a different way out. A shaft stands out of
     * the skin and can be drawn back along the hole it made; a round is under the skin,
     * so the wound has to be opened and the thing picked out with forceps. That is why
     * it has its own count and its own word on the chart: "two arrows" after a rifle
     * went off is the mod getting the one fact it exists to get right wrong.
     */
    public int bullets;

    /**
     * What those bullets were, one id per entry, in the order they went in.
     *
     * <p>{@link #bullets} remains the count because a count is what the chart, the aid
     * screen and the old saves all speak. This is the same fact with the one detail the
     * drawing needs, kept in step by {@link #addBullet(String)} and
     * {@link #pullBullet()}. A save from before this existed has a count and no list;
     * {@link #with(Later)} fills the gap with the generic round.
     */
    public final List<String> bulletTypes = new ArrayList<>();

    /**
     * Somebody's weapon, still in the wound.
     *
     * <p>Saved as the item itself rather than as a name, because it is going to be handed
     * to whoever gets it out. The only thing in this class that belongs to a different
     * person than the body does.
     */
    public CompoundTag blade = new CompoundTag();

    /**
     * Whether what is in there came out of a blast rather than off a bowstring.
     *
     * <p>It changes nothing: a piece of casing and an arrowhead are the same problem and
     * the same answer, and pulling either out is the same steady hand. What it changes is
     * the word on the chart - and getting the word right is most of what this mod does.
     * "Three arrows in a leg" after a creeper went off is a lie about what happened.
     */
    public boolean shrapnel;

    /**
     * Ticks this part still has to be left alone for.
     *
     * <p>Everything in this mod has ended at the moment the treatment did. A surgeon
     * closed somebody up and they stood straight back up and went to work, which is the
     * one thing about surgery that nobody who has had any believes.
     *
     * <p>What this is not is a debuff. Nothing is taken away while it lasts - a mended
     * arm swings exactly as well as it did an hour ago. What it is, is a wound that has
     * not finished being a wound: work the part hard and it comes open again, and then
     * everything that was true before the operation is true afresh.
     *
     * <p>Which makes it the first rule in the mod whose answer is to do nothing. There is
     * no item for this and there is not going to be one.
     */
    public int mending;

    /**
     * How long this limb has been going without a blood supply.
     *
     * <p>Counts up while there is a strap above it or something festering in it, and
     * back down again the moment that stops - so the clock a tourniquet starts is a
     * clock somebody can still beat. What it counts towards is the one state in the mod
     * that nothing undoes.
     */
    public int deadTicks;

    /**
     * The limb died and is still attached.
     *
     * <p>Every other thing on this list is a wound: it can be closed, cleaned, set,
     * healed or waited out. This one cannot. Nothing brings a limb back once it has gone
     * this far, which makes it the first state in the mod whose only answer is to lose
     * something on purpose - and it does not sit there quietly either, because a dead
     * limb attached to a live body poisons it.
     */
    public boolean dead;

    /**
     * It is not there any more.
     *
     * <p>The end of that road, and the only permanent thing a body carries. It is also,
     * bluntly, an improvement on the state before it: what a dead limb was doing to the
     * chest stops the moment it comes off. That is the whole trade the saw offers, and
     * it is exactly the trade the real operation is.
     */
    public boolean missing;

    /** Something wooden or iron in its place. Not a hand and not a leg, and better than none. */
    public boolean prosthetic;

    /**
     * And whether that something has a joint in it.
     *
     * <p>The whole difference between a peg and a leg somebody has spent money on, and it
     * is one part: a knee. A peg is a stick that meets the ground, so everything above it
     * has to swing round it; a jointed one bends where a leg bends, and what that gives
     * back is most of the difference between walking and getting about. The same for a
     * hook against a hand that opens.
     *
     * <p>Kept as a second flag rather than a number so that a body saved before this
     * existed loads with the plain one it had, which is what it has.
     */
    public boolean fineProsthetic;

    /**
     * What is torn inside this part, two bits per organ and four organs to a part.
     *
     * <p>Kept under the old field name because the old value decodes into it correctly:
     * what used to be "the torso is torn" lands on slot zero, which is the heart in a
     * chest and the liver in a belly, and those are the right answers.
     *
     * <p>The injury with nothing to see. A hard blow to a torso tears something inside it
     * and from that moment the blood is going somewhere - into the belly, into the chest,
     * anywhere but out - at a rate no dressing touches and no clot closes, because there
     * is nothing on the outside for a clot to close.
     *
     * <p>It is on the chart, because the chart has never lied. It is not on the figure,
     * because there is nothing on the figure to draw, and that is the entire character of
     * it: this is the injury found by reading rather than by looking.
     *
     * <p>One thing stops it, and it is a surgeon.
     */
    public int organs;
    /** How long a bruised one has had to settle. Means nothing to a torn one. */
    public int organTicks;

    /**
     * How long the fitted limb has been worn.
     *
     * <p>Nothing made of wood and leather and worn all day stays the shape it was cut to.
     * A socket loosens, a strap stretches, a joint develops play - and a prosthesis that
     * has gone loose is not a broken one, it is a worse one: it rubs, it wobbles, and it
     * gives back less of what it gave back on the day it was fitted.
     *
     * <p>Which puts a small standing job on the one permanent thing in the mod, and it is
     * the right job to put there. Losing a leg is not an event you get past; it is a thing
     * you then live with, and living with one is mostly maintenance.
     */
    public int wear;

    /**
     * Whether there is a dressing on this part, and how long it has been there.
     *
     * <p>The mod treated a bandage as an event: it went on, the bleeding stopped, and
     * that was the end of the thought. Nothing else in here works like that - a strap has
     * a clock, a course runs out, a splint leaves a crack to rest - and the one treatment
     * everybody uses had no second half at all.
     *
     * <p>It does now. A clean dressing is doing a job: it holds the wound shut and keeps
     * the dirt out, and while it is fresh nothing on this part can go bad. Left on, it
     * stops being clean and becomes the thing keeping the dirt <em>in</em>, which is the
     * single commonest way a treated wound turns into an infected one.
     *
     * <p>It comes off by itself when there is nothing left under it to dress. Nobody
     * should be changing a dressing on an arm that healed twenty minutes ago.
     */
    public boolean dressed;
    public int dressedTicks;

    /**
     * How many times a bone in here has been broken and has knitted.
     *
     * <p>The first thing in this mod that a body keeps. Everything else heals to the
     * same body it was before, which quietly said that nothing that happens to anybody
     * matters once it is over - and that is the one thing a mod about medicine should
     * never say.
     *
     * <p>What it costs is not health. A knitted bone is <b>not weaker</b>, and the mod
     * is not going to teach that it is: what it is, is a bone that knows about weather,
     * and a place that will be the first thing to go the next time this body takes a
     * fall. See {@link Scars}.
     */
    public int knits;

    /**
     * How many times the skin here has been opened and closed - by a wound, a blade or
     * a surgeon, which as far as the skin is concerned are the same event.
     *
     * <p>Scar tissue is not skin. It has less blood going through it than what it
     * replaced, which is why an old scar is the place an infection settles, and why the
     * part it is on never quite gets back to the maximum it had.
     */
    public int cuts;

    /** The deepest burn that ever healed here, which is what decides how tight it went. */
    public int burnt;

    /** Whether something has grown in here before. It will settle here again sooner. */
    public boolean septic;

    /**
     * How stiff the part is now, 0 to 1.
     *
     * <p>The one number in here that <b>can be worked back down</b>, and the reason the
     * rest of them are allowed to be permanent. A scar is forever and a contracture is
     * not: what tightens over a healed burn or a closed wound loosens again if somebody
     * takes the joint through its range, day after day, past the point where it stops
     * being comfortable.
     *
     * <p>So the history ratchets one way and the disability does not, which is the whole
     * design. Without this, a long-lived character is a character who is quietly getting
     * worse with no way back, and the fastest cure for that would be to go and die.
     */
    public float stiff;

    /** The day somebody last worked this part. One session a day counts; see {@link Scars}. */
    public int worked;

    /**
     * Whether there is a splint tied on here, and how long it still has to stay.
     *
     * <p>A splint used to be a full stop: the bone was set, the item was spent, and the
     * stick vanished. It does not vanish now - it stays on the limb, it is drawn on the
     * body, it is on the chart, and the limb is held still for as long as it is there.
     *
     * <p>And the clock is not a timer, it is a bargain. Walking runs it down. Running on
     * a set bone <b>puts time back on it</b>, which is the honest version of what happens
     * to a fracture somebody will not stay off: it does not send you back to the start,
     * it simply takes longer, and it takes longer every time you do it.
     */
    public boolean splint;
    public int splintTicks;

    /**
     * Whether the ends are still in line.
     *
     * <p>The mod had one kind of broken, and there are two. A bone can break and stay
     * where it was, which is a bad month; or the ends can move, which is a different
     * injury with a different job in front of it. The second one is what everybody
     * pictures when they hear the word: the limb is the wrong shape, and it is obvious
     * from across a room without anybody reading a chart.
     *
     * <p>What it changes is that <b>a splint is no longer the whole answer.</b> Strapping
     * a stick to a bone that is out of line holds it exactly where it is, and where it is
     * is wrong - so it knits like that, and that is permanent. Something has to pull it
     * back into line first, and that something is a pair of hands and a great deal of
     * screaming.
     */
    public boolean displaced;

    /**
     * Whether the end of it has come through the skin.
     *
     * <p>The top of the ladder, and the only bone injury here that is also a wound. It
     * bleeds, it cannot clot while there is bone in the way, and it is the fastest road
     * to infection in the mod - the inside of a leg has been introduced to the floor.
     *
     * <p>Nothing can be set while it is open and no splint goes on: it needs a surgeon
     * first, to clean it out and close it. That is the whole of the difference between
     * this and the others, and it is why an open fracture is a reason to stop whatever
     * else anybody was doing.
     */
    public boolean openBreak;

    /**
     * How many times a bone here has knitted out of line.
     *
     * <p>The permanent half of all this, and it is the one mark in the mod that costs
     * the part something it never gets back: a bone that healed crooked is shorter or
     * bent, and the limb it is in does less than it did.
     *
     * <p>It can be undone, and the way to undo it is exactly the way it is undone in
     * life: somebody breaks it again, properly, and sets it properly this time. Nothing
     * else touches it - which makes it the only injury in the mod whose treatment is to
     * inflict the injury again on purpose.
     */
    public int crooked;

    /**
     * Something has set in a vein in here, and how long it has been setting.
     *
     * <p>The one injury in this mod that <b>nothing does to you</b>. Every other thing in
     * this class arrived on the end of something - a blow, a blade, a fire, a mouthful of
     * the wrong dinner. This one is what a body does to itself when it is left alone: blood
     * that is not moving clots, and the things that stop it moving are a strap, a splint,
     * a broken leg, and lying on the floor while somebody decides whether to come.
     *
     * <p>Which makes it the mod's first injury <b>caused by treatment</b>, and it is not an
     * accident of design: the strap that saved the limb, the splint that set the bone and
     * the hour spent lying still are the three commonest ways to get one in life as well.
     *
     * <p>It is quiet. The limb is swollen and warm and the person says it aches, and that
     * is the whole of the warning - there is nothing to see and nothing on the outside to
     * put a hand on. What ends it is not the leg. See {@link Clots}.
     */
    public boolean clot;
    public int clotTicks;

    public PartState(float health, float max, int bleed, int bleedTicks, boolean fractured, int breaks,
                     boolean cracked, int crackTicks, int infection, int infectionTicks, boolean burned,
                     int dirtyTicks, boolean tourniquet, boolean pneumo, int pneumoTicks,
                     Extra extra) {
        this.max = Math.max(1.0F, max);
        this.health = Mth.clamp(health, 0.0F, this.max);
        this.bleed = Mth.clamp(bleed, BLEED_NONE, BLEED_ARTERIAL);
        this.bleedTicks = bleedTicks;
        // A save from before breaks were counted has only the flag, and one break is
        // what that flag always meant.
        this.breaks = Math.max(0, breaks > 0 ? breaks : (fractured ? 1 : 0));
        this.cracked = cracked && this.breaks == 0;
        this.crackTicks = Math.max(0, crackTicks);
        this.infection = Mth.clamp(infection, INFECTION_NONE, INFECTION_SEVERE);
        this.infectionTicks = Math.max(0, infectionTicks);
        // A body saved before burns had a depth has only the flag, and what that flag
        // always described - blistered, painful, treatable with a salve - is a partial
        // thickness burn. Reading it as anything else would either invent injuries or
        // erase them.
        this.burn = Mth.clamp(extra.burn() > 0 ? extra.burn() : (burned ? BURN_PARTIAL : BURN_NONE),
                BURN_NONE, BURN_FULL);
        this.burned = this.burn > BURN_NONE;
        // Negative is not corrupt data here: past zero it is the shallow burn closing
        // on its own, and clamping it would restart that every time somebody relogged.
        this.burnTicks = extra.burnTicks();
        this.dirtyTicks = Math.max(0, dirtyTicks);
        this.tourniquet = tourniquet;
        this.pneumo = pneumo;
        this.pneumoTicks = Math.max(0, pneumoTicks);
        this.pressedTicks = Math.max(0, extra.pressedTicks());
        this.arrows = Math.max(0, extra.arrows());
        this.deadTicks = Math.max(0, extra.deadTicks());
        this.missing = extra.missing();
        // A limb cannot be both gone and rotting: taking it off is what stops the
        // rotting. A save that somehow claims both is read as the later of the two.
        this.dead = extra.dead() && !this.missing;
        this.prosthetic = extra.prosthetic() && this.missing;
        this.fineProsthetic = extra.fine() && this.prosthetic;
        this.organs = Math.max(0, extra.organ());
        this.organTicks = Math.max(0, extra.organTicks());
        this.wear = Math.max(0, extra.wear());
        this.shrapnel = extra.shrapnel() && this.arrows > 0;
        this.mending = Math.max(0, extra.mending());
        this.dressed = extra.dressed();
        this.dressedTicks = Math.max(0, extra.dressedTicks());
    }

    public static PartState full(BodyPart part) {
        float max = part.maxHealth();
        return new PartState(max, max, BLEED_NONE, BLEED_FOREVER, false, 0, false, 0,
                INFECTION_NONE, 0, false, 0, false, false, 0, Extra.NONE);
    }

    /**
     * Whether blood is actually running. A strapped limb still has the wound - it just
     * is not losing anything through it, which is the whole bargain.
     */
    public boolean losingBlood() {
        return bleeding() && !tourniquet;
    }

    /** Starts a bleed, or upgrades one already running. Never downgrades a heavy bleed. */
    public void startBleeding(int level, int ticks) {
        if (level <= bleed) {
            return;
        }
        bleed = Mth.clamp(level, BLEED_NONE, BLEED_ARTERIAL);
        // Neither of the two bad ones ever closes on its own. A light cut clots; a
        // wound that needs a hand on it waits for the hand.
        bleedTicks = bleed >= BLEED_HEAVY ? BLEED_FOREVER : ticks;
    }

    public void stopBleeding() {
        bleed = BLEED_NONE;
        bleedTicks = BLEED_FOREVER;
        dirtyTicks = 0;
        // Nothing left to hold. Leaving the count running would put a pair of hands on
        // a closed wound on the chart, which is a thing the chart would be wrong about.
        pressedTicks = 0;
    }

    /** Whether what is open here is an artery. */
    public boolean arterial() {
        return bleed >= BLEED_ARTERIAL;
    }

    /**
     * A bone in here has finished knitting.
     *
     * <p>Called from the one place a break actually ends rather than from the splint,
     * because a splint is not the end of a fracture - resting it is.
     */
    public void knitted() {
        knits++;
    }

    /** The skin here has been opened and has closed again. */
    public void scarred(float tightness) {
        cuts++;
        stiff = Mth.clamp(stiff + tightness, 0.0F, 1.0F);
    }

    /** Whether there is something set in a vein in here. */
    public boolean clotted() {
        return clot && !missing;
    }

    /** It has gone, one way or the other: broken up by a drug, or broken off. */
    public void clearClot() {
        clot = false;
        clotTicks = 0;
    }

    /** Whether this part carries anything that outlives the wound that made it. */
    public boolean marked() {
        return knits > 0 || cuts > 0 || burnt > BURN_NONE || septic || stiff > 0.0F || crooked > 0;
    }

    /** The stick goes on, for as long as it is going to take. */
    public void setSplint(int ticks) {
        splint = true;
        splintTicks = Math.max(splintTicks, ticks);
    }

    /** And comes off - finished, cut off, or taken off by somebody impatient. */
    public void unsplint() {
        splint = false;
        splintTicks = 0;
    }

    public boolean splinted() {
        return splint;
    }

    /** Whether somebody currently has their hands on this wound. */
    public boolean pressed() {
        return pressedTicks > 0;
    }

    /** A fresh dressing goes on, or the one that was there is changed for a fresh one. */
    public void dress() {
        dressed = true;
        dressedTicks = 0;
        dirtyTicks = 0;
    }

    public void undress() {
        dressed = false;
        dressedTicks = 0;
    }

    /** Whether the dressing has been on long enough to have stopped helping. */
    public boolean soiled(int afterTicks) {
        return dressed && dressedTicks >= afterTicks;
    }

    /** How worn the fitted limb is, 0 to 1, or 0 when there is nothing fitted. */
    public float wornShare(int fullTicks) {
        if (!prosthetic || fullTicks <= 0) {
            return 0.0F;
        }
        return Mth.clamp(wear / (float) fullTicks, 0.0F, 1.0F);
    }

    /** Whether this part is still knitting after somebody worked on it. */
    public boolean mending() {
        return mending > 0;
    }

    /** Whether this limb has died on the body. Nothing treats it; it comes off. */
    public boolean necrotic() {
        return dead && !missing;
    }

    /** Whether anything is left of this part at all. */
    public boolean gone() {
        return missing;
    }

    /**
     * The limb stops being fed for good.
     *
     * <p>Everything open on it closes, because nothing is running through it any more,
     * and the strap comes off with it: there is nothing left below to save by keeping it
     * on. What is left is a limb-shaped problem attached to a living chest.
     *
     * <p>The wounds go too, and that is not mercy - it is that they have stopped being
     * wounds. A broken bone in a dead arm is not something a splint is for and the rot
     * in it is not something antiseptic reaches; leaving either flag set would have the
     * chart offering treatments that cannot do anything, on the one limb in the mod
     * where there is exactly one answer.
     */
    public void die() {
        undress();
        dead = true;
        health = 0.0F;
        stopBleeding();
        tourniquet = false;
        arrows = 0;
        bullets = 0;
        bulletTypes.clear();
        blade = new CompoundTag();
        breaks = 0;
        cracked = false;
        crackTicks = 0;
        infection = INFECTION_NONE;
        infectionTicks = 0;
        clearBurn();
        dirtyTicks = 0;
    }

    /**
     * It comes off.
     *
     * <p>Everything that was wrong with it goes with it - the rot, the break, the burn,
     * the shaft nobody pulled out. That is not generosity, it is what amputation is:
     * the problem is removed rather than treated. What is left behind is a stump, and
     * whether that stump is bleeding is decided by whoever held the saw.
     */
    /** Nothing in a limb that is not there. */
    public void amputate() {
        clearClot();
        undress();
        repairOrgans();
        missing = true;
        dead = false;
        deadTicks = 0;
        health = 0.0F;
        stopBleeding();
        tourniquet = false;
        pneumo = false;
        pneumoTicks = 0;
        arrows = 0;
        bullets = 0;
        bulletTypes.clear();
        blade = new CompoundTag();
        breaks = 0;
        cracked = false;
        crackTicks = 0;
        infection = INFECTION_NONE;
        infectionTicks = 0;
        clearBurn();
        dirtyTicks = 0;
        prosthetic = false;
        fineProsthetic = false;
    }

    /**
     * Puts a burn on, or deepens the one that is there.
     *
     * <p>A part carries one burn and it is the worst one: a fresh scorch across an arm
     * that is already burnt to the bone changes nothing about that arm.
     *
     * @return true when this actually made it worse
     */
    public boolean burn(int depth, int window) {
        int want = Mth.clamp(depth, BURN_NONE, BURN_FULL);
        if (want <= burn) {
            // Still fresh heat on an old burn: it cannot deepen it by itself, but it can
            // keep the window open, which is the honest half of standing back in a fire.
            burnTicks = Math.max(burnTicks, window);
            return false;
        }
        burn = want;
        burned = true;
        burnTicks = Math.max(burnTicks, window);
        return true;
    }

    /** Whether this one is past the point a salve can reach. */
    public boolean burnFull() {
        return burn >= BURN_FULL;
    }

    /** Whether it is still deciding how deep it will be. */
    public boolean burnFresh() {
        return burned && burnTicks > 0;
    }

    /** Water, in time. Takes it back a step and closes the window on it. */
    public void clearBurn() {
        burn = BURN_NONE;
        burned = false;
        burnTicks = 0;
    }

    /**
     * It goes back on.
     *
     * <p>The exact opposite of {@link #amputate()} and deliberately not a full undo: what
     * this restores is the limb being there. Whatever shape it is in when it gets back is
     * decided by whoever sewed it on, and everything else about the stump - what it was
     * bleeding, what was strapped above it - is over and stays over.
     */
    public void reattach() {
        missing = false;
        dead = false;
        deadTicks = 0;
        prosthetic = false;
        fineProsthetic = false;
        stopBleeding();
        tourniquet = false;
    }

    /** Whether there is a weapon in this part that nobody has taken out yet. */
    public boolean bladeIn() {
        return !blade.isEmpty();
    }

    /**
     * Whether there is something still in it that has to come out first.
     *
     * <p>One question with two answers behind it, and everything in the mod that cares -
     * a bandage that will not go on, a clot that will not form, a part that will not
     * mend, the pain of it, the icon on the chart - asks it here rather than asking what
     * the object is. Which is why a blade in a leg behaves correctly everywhere the day
     * it is added: none of those places ever knew it was an arrow.
     */
    public boolean stuck() {
        return arrows > 0 || bullets > 0 || bladeIn();
    }

    /** @return true when one actually came out */
    public boolean pull() {
        if (arrows <= 0) {
            return false;
        }
        arrows--;
        // The last one out takes the word with it, so a leg that later stops an arrow
        // does not go on calling it shrapnel.
        if (arrows <= 0) {
            shrapnel = false;
        }
        return true;
    }

    /**
     * Puts one round in, remembering what it was.
     *
     * <p>The count and the list are kept in step here rather than at every call site.
     * A part read from an old save can arrive with a count and no names; the first new
     * round fills the gap with the generic round, because those older rounds were drawn
     * as exactly that.
     */
    public void addBullet(String kind) {
        while (bulletTypes.size() < bullets) {
            bulletTypes.add(BulletKinds.UNKNOWN);
        }
        while (bulletTypes.size() > bullets) {
            bulletTypes.remove(bulletTypes.size() - 1);
        }
        bullets++;
        bulletTypes.add(BulletKinds.canonical(kind));
    }

    /**
     * Replaces what is in the part, for the command that stages a wound.
     *
     * <p>Zero clears it. A count with no kind is the generic round, which is what a
     * staged bullet has always been.
     */
    public void setBullets(int count, String kind) {
        bullets = Math.max(0, count);
        bulletTypes.clear();
        String canonical = BulletKinds.canonical(kind);
        for (int i = 0; i < bullets; i++) {
            bulletTypes.add(canonical);
        }
    }

    /** @return true when one actually came out */
    public boolean pullBullet() {
        if (bullets <= 0) {
            return false;
        }
        bullets--;
        while (bulletTypes.size() > bullets) {
            bulletTypes.remove(bulletTypes.size() - 1);
        }
        return true;
    }

    public boolean fractured() {
        return breaks > 0;
    }

    /** A break whose ends have moved. It cannot be splinted into anything good. */
    public boolean outOfLine() {
        return displaced && breaks > 0;
    }

    /** A break with bone through the skin. Everything else waits for a surgeon. */
    public boolean openFracture() {
        return openBreak && breaks > 0;
    }

    /** Whether a bone in here healed out of line, and is still that way. */
    public boolean bent() {
        return crooked > 0;
    }

    /**
     * Pulls the ends back into line.
     *
     * @return true when there was anything to pull
     */
    public boolean reduce() {
        if (!displaced) {
            return false;
        }
        displaced = false;
        return true;
    }

    /** The surgeon has cleaned it out and closed the skin over it. */
    public void closeBone() {
        openBreak = false;
    }

    /**
     * It knitted like that.
     *
     * <p>Called from the one place a break ends, the same as the ordinary knit, and it
     * is the other half of the same moment: a bone that was in line becomes a mark, and
     * a bone that was not becomes a mark that costs something.
     */
    public void knittedCrooked() {
        crooked++;
        knits++;
        displaced = false;
    }

    /**
     * Broken again, on purpose, so it can be set straight.
     *
     * <p>The only way out of a crooked bone. What comes back is an ordinary displaced
     * fracture - which is to say, the thing that should have been dealt with the first
     * time.
     */
    public void rebreak(int limit) {
        if (crooked <= 0) {
            return;
        }
        crooked--;
        fracture(limit);
        displaced = true;
    }

    /**
     * Breaks the bone properly, up to however many of it there are. A crack that gets
     * leaned on comes through here, which is why cracking is cleared at the same time:
     * it is not both at once.
     *
     * @return true when this actually added a break
     */
    public boolean fracture(int limit) {
        if (breaks >= Math.max(1, limit)) {
            return false;
        }
        breaks++;
        cracked = false;
        crackTicks = 0;
        return true;
    }

    /**
     * One break set. The last one leaves a crack behind - a bone that has been put back
     * together still has to be rested - and any others are still waiting for a splint.
     */
    public void setOne() {
        if (breaks > 0) {
            breaks--;
        }
    }

    public void crack() {
        if (!fractured()) {
            cracked = true;
            crackTicks = 0;
        }
    }

    public void mend() {
        breaks = 0;
        cracked = false;
        crackTicks = 0;
        // Nothing to be out of line with, and nothing left open.
        displaced = false;
        openBreak = false;
    }

    public void infect(int level) {
        if (level > infection) {
            infection = Mth.clamp(level, INFECTION_NONE, INFECTION_SEVERE);
            infectionTicks = 0;
        }
    }

    public void disinfect() {
        infection = INFECTION_NONE;
        infectionTicks = 0;
        dirtyTicks = 0;
    }

    /** 0 when the part is blacked out, 1 when it is untouched. */
    public float ratio() {
        return max <= 0.0F ? 0.0F : Mth.clamp(health / max, 0.0F, 1.0F);
    }

    /** A part at zero is "blacked out": it still exists, but nothing about it works. */
    public boolean blacked() {
        return health <= 0.0F;
    }

    public boolean bleeding() {
        return bleed > BLEED_NONE;
    }

    public boolean infected() {
        return infection > INFECTION_NONE;
    }

    /** What state one particular organ is in. */
    public int organ(Organ organ) {
        return (organs >> (organ.slot() * 2)) & 0b11;
    }

    /** Whether anything inside this part is hurt, at any level. */
    public boolean organHurt() {
        return organs != 0;
    }

    /** And whether any of it is the level that does not stop by itself. */
    /** Whether anything in here is bruised rather than torn - the kind that settles. */
    public boolean organBruised() {
        for (int slot = 0; slot < 4; slot++) {
            if (((organs >> (slot * 2)) & 0b11) == ORGAN_BRUISED) {
                return true;
            }
        }
        return false;
    }

    public boolean organTorn() {
        for (int slot = 0; slot < 4; slot++) {
            if (((organs >> (slot * 2)) & 0b11) >= ORGAN_TORN) {
                return true;
            }
        }
        return false;
    }

    /** Never downgrades: a torn organ does not become a bruised one by being hit again. */
    public void tearOrgan(Organ organ, int level) {
        int slot = organ.slot() * 2;
        int was = (organs >> slot) & 0b11;
        int now = Mth.clamp(level, ORGAN_NONE, ORGAN_TORN);
        if (now <= was) {
            return;
        }
        organs = (organs & ~(0b11 << slot)) | (now << slot);
        organTicks = 0;
    }

    /** A surgeon who has the belly open deals with everything that is open in it. */
    public void repairOrgans() {
        organs = 0;
        organTicks = 0;
    }

    /** Bruising settles; tears do not. Called when the settle clock runs out. */
    public void settleBruises() {
        int kept = 0;
        for (int slot = 0; slot < 4; slot++) {
            int level = (organs >> (slot * 2)) & 0b11;
            if (level >= ORGAN_TORN) {
                kept |= level << (slot * 2);
            }
        }
        organs = kept;
        organTicks = 0;
    }

    /** Whether anything at all is wrong with this part. */
    public boolean hurt() {
        return health < max || bleeding() || fractured() || cracked || infected() || burned
                || tourniquet || pneumo || stuck() || dead || missing || dressed || organHurt()
                || splint || bent() || clotted();
    }

    /**
     * Whether there is anything left under a dressing worth dressing.
     *
     * <p>What a dressing is for is a wound. Once there is not one, it comes off on its
     * own - otherwise the mod would be asking players to change the bandage on an arm
     * that healed twenty minutes ago, forever, which is not a mechanic, it is a chore.
     */
    public boolean worthDressing() {
        // A stump is worth dressing exactly as long as it is open. It has no health to
        // get back and never will, so the ordinary test would have an amputee changing
        // the bandage on an arm that is not there, forever.
        if (missing || dead) {
            return bleeding();
        }
        return health < max || bleeding() || infected() || burned || stuck();
    }

    /**
     * Takes damage off the part.
     *
     * @return how much damage the part could not absorb because it ran out of health
     */
    public float damage(float amount) {
        if (amount <= 0.0F) {
            return 0.0F;
        }
        float absorbed = Math.min(health, amount);
        health -= absorbed;
        return amount - absorbed;
    }

    /**
     * Heals the part and returns what it could not use. Blacked parts refuse healing.
     *
     * <p>So do the last two states, and for a different reason: a blacked limb is
     * refusing until somebody opens it up, while a dead one is refusing full stop. There
     * is nothing there to mend and nothing that will ever mend it, and a medkit that
     * quietly did nothing would be worse than one that says so.
     */
    public float heal(float amount) {
        if (amount <= 0.0F || blacked() || dead || missing) {
            return Math.max(0.0F, amount);
        }
        float used = Math.min(max - health, amount);
        health += used;
        return amount - used;
    }

    /** The needle goes in and the air comes out. Nothing else undoes this. */
    public void decompress() {
        pneumo = false;
        pneumoTicks = 0;
    }

    public PartState copy() {
        return new PartState(health, max, bleed, bleedTicks, breaks > 0, breaks,
                cracked, crackTicks, infection, infectionTicks, burned, dirtyTicks, tourniquet,
                pneumo, pneumoTicks, Extra.of(this)).with(Later.of(this));
    }
}
