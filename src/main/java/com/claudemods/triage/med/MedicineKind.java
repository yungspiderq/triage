package com.claudemods.triage.med;

import com.claudemods.triage.Config;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.PartState;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The ten things a player can do about being hurt. Which items count as which is a
 * config list, so a pack can point these at real medical items from another mod.
 */
public enum MedicineKind {
    /** Stops bleeding on one part and patches it up a little. */
    BANDAGE,
    /**
     * Stops bleeding at once, at the price of the limb: nothing below the strap is fed
     * and nothing below it heals. The choice it offers is the point - keep the arm, or
     * keep the blood.
     */
    TOURNIQUET,
    /** Sets a broken bone, or a cracked one before it becomes a broken one. */
    SPLINT,
    /** Heals the part in the worst shape. Cannot touch a blacked-out one. */
    MEDKIT,
    /** Brings a blacked-out part back, at a fraction of its maximum. */
    SURGERY,
    /** Hides the penalties for a while and heals nothing at all. */
    PAINKILLER,
    /** Cleans an infected wound out, before it turns into an illness. */
    ANTISEPTIC,
    /** Takes a burn off a part so it can start healing again. */
    SALVE,
    /** The only thing that touches an illness. Aimed at the body, not at a part. */
    ANTIBIOTICS,
    /**
     * Volume back into a body that has lost it. Aimed at the body rather than at a part,
     * and worth nothing while a wound is still open: filling a leaking bucket is not
     * treatment. It is the first answer blood loss has ever had that is not "sit down".
     */
    TRANSFUSION,
    /**
     * Taking blood out rather than putting it in, into an empty bag. The donor pays for
     * it in the only currency the mod has for this - their own volume - which is what
     * stops a blood bank being free.
     */
    DRAW,
    /**
     * A needle between the ribs, to let out the air that is pressing a lung flat.
     *
     * <p>The only treatment here that is not repair. Nothing is closed, cleaned, set or
     * put back; something is let out, and the chest goes back to being an ordinary
     * damaged chest. It is also the only one aimed at a part that will look untouched on
     * the figure, which is the point of it.
     */
    DECOMPRESS,
    /**
     * A course that holds one illness down without ever curing it.
     *
     * <p>Every other thing in this box repairs something. This one repairs nothing at
     * all and is still the most useful item a player with that illness will ever carry -
     * which is exactly the shape of the real thing, and the reason it is worth having in
     * a mod that keeps insisting treatments are trades. The trade here is that taking it
     * costs you something the whole time you are taking it, and stopping costs more.
     */
    ANTIPSYCHOTIC,
    /**
     * Enough of something to take the person out of the body for a while.
     *
     * <p>Everything else in this box is done <em>to</em> a patient. Two things in it are
     * done to a patient who is awake and can feel all of it, and until now the mod simply
     * did not mention that: a surgical kit opened somebody up and a saw took their leg
     * off while they lay there watching.
     *
     * <p>What this buys is not healing. It buys the difference between an operation and
     * a fight, and it is the first item here whose whole value is what it lets somebody
     * <em>else</em> do properly.
     *
     * <p>And it is the most dangerous thing in the box. The dose that stops somebody
     * feeling it is a short way from the dose that stops them breathing, which is a state
     * this mod already has and already has an answer to. Nothing else in here can kill
     * the patient by working too well.
     */
    ANAESTHETIC,
    /**
     * Adrenaline, in a needle.
     *
     * <p>The body already does this by itself after a bad injury, and the two halves of
     * that are worth keeping in mind: for a short while nothing hurts, and then the heart
     * is going too fast and the hands shake. A needle is that on purpose, and everything
     * true of the natural one is true of this.
     *
     * <p>It has two correct uses and both are real. In a stopped heart it does not
     * restart anything - nothing restarts a heart in a needle - it makes the <em>next</em>
     * round of compressions worth what the first one was, which is exactly what it is
     * given between cycles for. And it is the answer to a body destroying the blood that
     * was hung on it, which is the one emergency in this mod that a bottle cannot touch.
     *
     * <p>And it is the most dangerous thing in the bag, because the wrong use looks like
     * the right one. Somebody emptying out looks like somebody fading, and the instinct
     * is to push something into them. Adrenaline raises the pressure: into an open vessel
     * that means the bleed pours harder, and the patient goes faster for having been
     * helped. On top of a body that is already surging it is the other way a heart stops.
     */
    EPINEPHRINE,
    /**
     * Morphine, in a needle.
     *
     * <p>The pills are a course: slow, safe, and worth less each time. This is the other
     * thing entirely - it works at once, it works on somebody on the floor, and it does
     * not care how many courses they have had, because it is not the same drug.
     *
     * <p>What it costs is breathing. That is not a balance decision, it is what an opiate
     * does: the drive to breathe is the thing it puts to sleep first. On a chest that is
     * already failing - crushed, broken, full of pneumonia - the dose that takes the pain
     * away is next to the dose that takes the breathing with it. The mod already has an
     * airway and a clock for a body that has stopped breathing, so nothing new had to be
     * invented for the mistake. It was already waiting.
     */
    /**
     * The thinner: the one drug in the bag that answers something the body did to itself.
     *
     * <p>Two jobs and they are not the same size. In a limb it is a course - the thing
     * that set in there breaks up over the next while and the danger is over, and nobody
     * will ever know how close it was. In a chest it is the only item in this mod that
     * undoes an emergency rather than treating the damage one left, and it has about a
     * minute and a half in which to be found in somebody's bag.
     */
    ANTICOAGULANT,
    MORPHINE,
    /**
     * The undo, and the only item here that is one.
     *
     * <p>It answers morphine and nothing else - not pills, not anaesthetic, not pain
     * itself. Given to somebody whose breathing is going it is the difference between a
     * patient and a body, and it works in seconds.
     *
     * <p>Its own wrong use is the reason it is a decision rather than a reflex. Pushing
     * it into somebody who is on morphine for a good reason does exactly what it says on
     * the ampoule: it takes the morphine away. All of it, at once, in a person who was
     * being held together by it - and what arrives instead is every bit of the pain that
     * was being kept off them.
     */
    NALOXONE,
    /**
     * A thermometer, which is the only way anybody knows a number.
     *
     * <p>Every other reading in this mod is taken with the hands and is there the moment
     * you look. A temperature is not. A hot forehead is something you can feel - the chart
     * still says warm, and it always will - but thirty-eight point six is not a thing any
     * hand has ever reported, and the mod said it anyway.
     *
     * <p>So the word stays free and the figure costs an instrument and the half minute it
     * takes to settle. Which is the right price, because the figure is what separates a
     * fever that is starting from one that is finished, and that is the difference between
     * a course now and a course too late.
     */
    THERMOMETER,
    /**
     * A stethoscope, and the one question nothing else in this mod can ask.
     *
     * <p>Every other finding has a way in without an instrument. A limb is a shape you can
     * see; a burn is on the skin; a bone grates under a hand; blood that is going somewhere
     * drives a pulse you can feel with two fingers. A lung that has come off the chest wall
     * gives you none of that. The chest looks like a chest, and what the patient says about
     * it - <em>I cannot breathe</em> - is the same sentence they say with a mouthful of
     * blood and with a rib through the muscle.
     *
     * <p>Which is what makes this the first instrument here that is not a convenience. The
     * thermometer puts a number on something a hand already knew; this is the only way in
     * to the thing itself, and the finding it brings back is the same shape as the one the
     * burn press brings: <b>an absence</b>. Air moving on both sides, or air moving on one.
     *
     * <p>It wants quiet, and that is not flavour either. Four places on a chest, a second
     * at each, and the whole thing is spoiled by the same conditions that spoil it in life:
     * a patient who will not lie still and a room with something happening in it.
     */
    STETHOSCOPE,
    /**
     * Bandage scissors: a dressing off without touching what is under it.
     *
     * <p>The whole of the item is the blunt lower blade. It goes flat against the skin and
     * slides along under the cloth, so what is being cut is above it and what matters is
     * below - which means it does not care how long the dressing has been on or what it
     * has dried onto. Fingers care enormously.
     *
     * <p>So this is the second tool in the box after the saw, and the same bargain: it is
     * not spent, it is carried, and what it buys is that the free way of doing this stops
     * being the only way.
     */
    SCISSORS,
    /**
     * Taking fragments out of somebody, with the thing they are taken out with.
     *
     * <p>This was a passive item and that was the mistake. Every other tool in this bag
     * is picked up and used - a saw, a pair of scissors - and a player who picks up a
     * pair of forceps and right-clicks with them expects the same. What they got was
     * nothing at all, and an item that does nothing when you use it is an item that does
     * not work, whatever it quietly changes elsewhere.
     *
     * <p>Fingers still do the job. They are still on the chart, they still tear anything
     * they have to dig for, and they are still what somebody without a pair uses. The
     * forceps are the same job done with the right thing in your hand, and now they are
     * reached the same way as everything else.
     */
    EXTRACT,
    /**
     * The limb comes off.
     *
     * <p>The only entry on this list that removes something instead of repairing it, and
     * the only one whose result is worse than the limb was a week ago and better than
     * the limb is now. It exists because the mod finally has a state nothing else
     * answers: a limb that has died on a living body and is taking the body with it.
     *
     * <p>It is deliberately not a rescue. It costs volume, it puts the patient into
     * shock, and done without a strap above it, it is the fastest way to kill somebody
     * in the whole mod. What it buys is that the chest stops paying for a limb that is
     * never coming back.
     */
    AMPUTATE,
    /**
     * A leg made of wood, screwed to what is left.
     *
     * <p>It is not a leg. It gives back about half of one, it will never sprint, and it
     * is still the difference between walking and dragging. The mod needed something at
     * the end of the worst road it has, or amputation would only ever have been the
     * least bad way to lose.
     */
    PEG,
    /**
     * The same thing for an arm, and honest about being a hook rather than a hand.
     */
    HOOK,
    /**
     * A leg with a knee in it.
     *
     * <p>The one part that makes the difference, and the reason the second tier exists at
     * all: a peg is a stick that meets the ground and everything above it has to swing
     * round it, while a jointed one bends where a leg bends. It gives back most of the
     * difference between walking and getting about, and it costs what a joint costs.
     *
     * <p>It goes onto a bare stump or over a peg, because upgrading is what anybody would
     * actually do with one.
     */
    JOINT,
    /** The same for an arm: a hand that opens, rather than a hook that does not. */
    HAND,
    /**
     * Their own arm, put back on.
     *
     * <p>The only treatment in the mod that restores a part rather than repairing one,
     * and the only one that can be too late rather than too difficult. What decides it is
     * how long the limb has been off and how cold it was kept, both of which happened
     * before anybody opened a screen - so the surgery here is the easy half.
     *
     * <p>It is worse than the arm was and much better than a hook. That is the trade and
     * it is the honest one: a replanted limb is stiff, weak and slow to come back, and
     * everybody who has the choice takes it anyway.
     */
    GRAFT,
    /**
     * A card of worming tablets, and the one thing it is for.
     *
     * <p>The mod has spent two versions teaching that the bottle is for bacteria. This is
     * the first illness on the other side of that line with an answer of its own - the
     * viruses have none and never will - and the answer is a different card, because a
     * worming tablet does one job and does not do anybody else's.
     */
    WORMING;

    public static final MedicineKind[] VALUES = values();

    private final String id = name().toLowerCase(Locale.ROOT);

    public String id() {
        return id;
    }

    public Component title() {
        return Component.translatable("triage.medicine." + id);
    }

    /** What the treatment screen tells the player to do. */
    public Component hint() {
        return Component.translatable("triage.hint." + id);
    }

    /**
     * Whether this is done to a particular part. Pills are swallowed and a drip goes
     * into the body as a whole, so none of the three asks which limb.
     */
    public boolean needsPart() {
        return this != PAINKILLER && this != ANTIBIOTICS && this != TRANSFUSION && this != DRAW
                && this != ANTIPSYCHOTIC && this != ANAESTHETIC
                // A needle goes into whoever is in front of you. None of the three asks
                // which arm, because none of the three is being given to an arm.
                && this != EPINEPHRINE && this != MORPHINE && this != NALOXONE
                // Nor does the thinner: what it is being given to is a vein, and which
                // limb the clot is in is not a thing the needle cares about.
                && this != ANTICOAGULANT
                // A temperature is taken off a person, not off an arm.
                && this != THERMOMETER
                // And a chest is a chest. There is only one of them, and this is it.
                && this != STETHOSCOPE;
    }

    /**
     * Whether this is simply swallowed. Nothing can knock a player off doing that: being
     * ill is exactly the state you cannot fight your way clear of first, so making the
     * cure need a quiet moment would be a trap with no way out. A needle in the arm is
     * not in that class - a hard enough knock pulls it straight back out.
     */
    public boolean swallowed() {
        return this == PAINKILLER || this == ANTIBIOTICS || this == ANTIPSYCHOTIC;
    }

    /**
     * Whether the item survives being used.
     *
     * <p>Everything else here is a dressing, a dose or a bag: it is spent and it is
     * gone. A saw is a tool. Spending one on every limb would make the operation a
     * question of supply, and the question this operation is meant to ask is whether to
     * do it at all.
     */
    /**
     * Whether this one opens somebody, or goes in through something already open.
     *
     * <p>Which is the list of treatments that can leave something behind in a wound. A
     * splint is a stick tied to the outside of a leg and has never given anybody
     * anything; a saw and a needle and a pair of forceps are a different matter, and so
     * is a hand going into a wound that is already there.
     */
    public boolean opensSkin() {
        return switch (this) {
            case SURGERY, AMPUTATE, EXTRACT, DECOMPRESS, MEDKIT, TRANSFUSION, DRAW -> true;
            default -> false;
        };
    }

    public boolean reusable() {
        return this == AMPUTATE || this == SCISSORS || this == EXTRACT || this == THERMOMETER
                || this == STETHOSCOPE;
    }

    /**
     * Whether this can be used to hold somebody on the floor rather than to treat them.
     *
     * <p>Dressings, and only dressings: packing a wound hard is a thing you can do to a
     * body that is going, and it is the thing the mod is named after. Surgery still does
     * the standing up. Everything else - pills, straps, a drip - either needs a patient
     * who can help or does nothing for the reason they are down.
     */
    public boolean canStabilise() {
        return this == BANDAGE || this == MEDKIT;
    }

    /**
     * Whether this part is in a state this kind of medicine can do anything about.
     * Lives here rather than in the server-side code because the treatment screen has
     * to grey out the parts that are not worth treating.
     */
    public boolean canTreat(BodyPart part, PartState state) {
        return switch (this) {
            // A bandage is also how a strap comes off: the wound gets closed properly
            // and the limb gets its blood back.
            // Not over an arrow. Closing a wound with something still in it is how a
            // clean injury becomes an infected one, and the mod would rather say so than
            // let a dressing quietly do nothing.
            // And a dressing that has been on too long, which is the only reason a
            // bandage is ever worth putting on something that is not bleeding: what
            // goes on is a clean one, and what comes off is the reason it went bad.
            case BANDAGE -> (state.bleeding() && !state.stuck()) || state.tourniquet
                    || state.soiled(Config.values().dressingCleanSeconds * 20);
            // Also onto a limb that has died, which is the only reason this line is not
            // just about bleeding. A dead limb has nothing running through it to stop,
            // so on the old rule it could never be strapped - which quietly made "strap
            // above the cut" a rule nobody could follow, since the strap that killed the
            // limb comes off the moment it dies. This is the one that goes on before
            // the saw, and putting one on is exactly what it is for.
            case TOURNIQUET -> (state.bleeding() || state.necrotic()) && !state.tourniquet;
            // A splint is worth putting on a crack too - that is how you stop it
            // becoming the thing a splint is actually for.
            // Only where a splint is the answer. A skull and a spine break like anything
            // else and a stick strapped to either does nothing, so those go to the
            // surgical kit instead - which is expensive, and is the right price.
            case SPLINT -> (state.fractured() && part.bone().settable()) || state.cracked;
            case MEDKIT -> !state.blacked() && state.health < state.max;
            // And anything torn inside, which is the only thing that ends that at all.
            // And a burn that went through the skin, which is the one wound in here
            // that is answered by taking something away rather than putting something on.
            // What is dead has to come off before the edges can close over it.
            // And the two things a bone can want from one: cleaning out and closing
            // when the end of it is outside, or breaking again when it healed where it
            // was left. The second is the only treatment in the mod that is the injury.
            case SURGERY -> state.blacked() || state.organHurt() || state.burnFull()
                    || (state.fractured() && !part.bone().settable())
                    || state.openFracture() || state.bent();
            case ANTISEPTIC -> state.infected();
            // Exactly the middle rung. A salve on dead skin does nothing, and a salve
            // on skin that is only red is a jar spent on a burn that was going to close
            // by itself - so the mod refuses both, which is the mod telling the truth
            // about what a salve is for.
            case SALVE -> state.burn == PartState.BURN_PARTIAL;
            // Only ever the chest, and only when the air is actually in there. A needle
            // into a chest that does not need one is not a treatment, it is a wound.
            case DECOMPRESS -> state.pneumo;
            // Anything with a dressing on it, and nothing else. Taking one off is not a
            // treatment of the wound; it is a treatment of the dressing.
            case SCISSORS -> state.dressed;
            // Anything with something in it. Which is exactly what the bare hands offer
            // too - the difference is not what can be done, it is what it costs.
            // Either thing in the wound, under either switch: forceps do not care which
            // of the two they are reaching for.
            case EXTRACT -> (Config.values().arrows || Config.values().bulletsStick
                    || Config.values().weaponsStick) && state.stuck();
            // Only a limb that has already died. A saw is not an answer to a bad arm,
            // and the mod will not offer it as one: until the limb is gone there is
            // always something else on this list that is still worth doing.
            case AMPUTATE -> Config.values().amputation && state.necrotic() && part.limb();
            // On a stump, once, and only the right shape of thing for the right end of
            // the body. A wooden leg screwed to a shoulder is not a treatment.
            // And not onto something that is still running. A stump nobody has closed
            // is not a stump anything gets fitted to, and saying so by greying the limb
            // out is better than saying it after the work is done.
            case PEG -> Config.values().prosthetics && fittable(state) && part.isLeg();
            case HOOK -> Config.values().prosthetics && fittable(state) && part.isArm();
            // Onto a stump, or over a plain one: fitting a better leg to somebody who
            // already has a peg is the thing anybody would do with one.
            case JOINT -> Config.values().prosthetics && upgradable(state) && part.isLeg();
            case HAND -> Config.values().prosthetics && upgradable(state) && part.isArm();
            // Only a stump, and only one nobody has screwed anything onto yet. Whether
            // this particular arm belongs on this particular shoulder is a question about
            // the stack rather than the part, so it is asked where the stack is.
            case GRAFT -> Config.values().grafts && state.gone() && !state.prosthetic;
            // None of these is given to a limb, so no limb is ever the right answer.
            case ANTICOAGULANT -> false;
            case PAINKILLER, ANTIBIOTICS, TRANSFUSION, DRAW, ANTIPSYCHOTIC, ANAESTHETIC,
                 EPINEPHRINE, MORPHINE, NALOXONE, THERMOMETER, STETHOSCOPE, WORMING -> false;
        };
    }

    private static boolean fittable(PartState state) {
        return state.gone() && !state.bleeding() && (!state.prosthetic || worn(state));
    }

    /**
     * Whether the one already on there has gone loose enough to be worth replacing.
     *
     * <p>Which is what makes fitting a second one anything other than a waste: a fresh
     * peg over a fresh peg is the same peg, and the screen should say so rather than
     * quietly spending the item.
     */
    private static boolean worn(PartState state) {
        Config.Values config = Config.values();
        return state.wornShare(config.prostheticWearSeconds * 20) >= 0.5F;
    }

    private static boolean upgradable(PartState state) {
        return state.gone() && !state.bleeding()
                && (!state.fineProsthetic || worn(state));
    }

    public static @Nullable MedicineKind byId(String id) {
        for (MedicineKind kind : VALUES) {
            if (kind.id.equals(id.toLowerCase(Locale.ROOT))) {
                return kind;
            }
        }
        return null;
    }
}
