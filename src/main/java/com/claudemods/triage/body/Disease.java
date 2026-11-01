package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * Illness, as opposed to injury: it belongs to the whole body rather than to one part,
 * and no bandage will touch it.
 *
 * <p>Two of these are dead ends you sit out and two of them are a ladder - a wound left
 * dirty long enough becomes a fever, and a fever left alone becomes blood poisoning,
 * which does not stop by itself. That ladder is the reason the mod has diseases at all:
 * it puts a deadline on the wounds you were planning to ignore.
 */
public enum Disease implements StringRepresentable {
    /** Cold and wet for too long. Mild, passes, but leaves you easier to infect. */
    COLD("cold", 0, ChatFormatting.AQUA),
    /**
     * A cold that went down into the chest and stayed there.
     *
     * <p>The middle of an illness that used to have none: a cold sat at "mild, passes"
     * however long somebody spent wet and freezing with it. This is what happens instead
     * when they do. It bruises both lungs rather than tearing anything, which is the
     * honest difference between an illness and a wound - there is no hole to find, so
     * opening the chest achieves nothing and only a course answers it.
     */
    PNEUMONIA("pneumonia", 1, ChatFormatting.DARK_AQUA),
    /**
     * A belly full of what was meant to stay in the gut.
     *
     * <p>The other missing middle. A torn gut used to roll straight for sepsis, from the
     * wound to the worst thing in the mod in one step with nothing in between to notice.
     * This is the step: it runs hot, it eats the belly, and it gives whoever is working
     * a few minutes to close the hole.
     *
     * <p>It is also the one illness antibiotics cannot finish, because while the gut is
     * still open the cause is still arriving. The bottle buys time; the surgeon ends it.
     */
    PERITONITIS("peritonitis", 2, ChatFormatting.DARK_RED),
    /**
     * Influenza. The first thing in here that a course of antibiotics cannot touch.
     *
     * <p>Everything else the bottle answers is bacterial, and the mod never had to say so
     * because there was nothing it did not answer. A virus is the sentence that makes the
     * bottle mean something: it is not a stronger illness, it is a different kind of thing,
     * and pouring a course into it achieves exactly nothing except that the course is gone.
     *
     * <p>It passes on its own, given time and somewhere warm, and most of the time that
     * is all it does. What it can do on the way is leave the chest open to something that
     * is bacterial - which is how influenza has always killed people, and which is the one
     * part of it a bottle does answer. Whether that happens is about the body it is in: a
     * warm, dry, rested one usually just gets better.
     */
    FLU("flu", 1, ChatFormatting.GOLD),
    /**
     * The other one, and it empties people rather than burning them.
     *
     * <p>Caught from what was eaten or what it was washed in. Antibiotics do nothing here
     * either, and the answer is the one nobody thinks of as medicine: fluid, faster than
     * it is going out. The mod already has a bag of saline and a way to lose volume, so
     * this needed no new machinery - only a reason to reach for the bag that is not a
     * wound.
     */
    STOMACH("stomach", 1, ChatFormatting.DARK_GREEN),
    /**
     * An airway that has breathed fire.
     *
     * <p>The half of burning that kills quickly. Everything on the outside of somebody is
     * measured in hours; an airway is measured in minutes, and it happens to people who
     * walked out of it looking fine. The tell is not the skin, it is the voice - hoarse,
     * and a cough with soot in it - which is why this mod says it out loud rather than
     * drawing it.
     *
     * <p>No bottle touches it: this is swelling, not infection. What it wants is time and
     * air, and what makes it lethal is the one drug in the bag that presses on the drive
     * to breathe. Morphine into a chest that breathed fire is the mistake, and the mod
     * already knew how to punish morphine on a failing chest - this simply is one.
     */
    SMOKE("smoke", 2, ChatFormatting.DARK_GRAY),
    /**
     * Worms, from what was eaten.
     *
     * <p>The commonest illness in the world and the one nobody puts in a game, because it
     * has no scene in it. It does not burn, it does not open anything and it will not kill
     * you this afternoon. It <b>takes</b>: it eats before you do, and it drinks off the
     * blood while it is there, and a body carrying a load of it is a body that is hungry
     * and short and tired for weeks without one dramatic moment in the whole business.
     *
     * <p>Which makes it the right illness for this mod. Nothing else here is slow. And it
     * is the second thing a bottle of antibiotics does nothing for - but unlike a virus it
     * has an answer of its own, and that answer is a different card of tablets, because
     * that is exactly what a worming tablet is for.
     *
     * <p>It does not pass on its own. Nothing about a parasite is self-limiting: that is
     * the definition of one.
     */
    PARASITES("parasites", 1, ChatFormatting.DARK_GREEN),
    /** Something eaten that should not have been. */
    POISONING("poisoning", 0, ChatFormatting.DARK_GREEN),
    /** A dirty wound that was not cleaned. Turns into sepsis if left. */
    FEVER("fever", 1, ChatFormatting.GOLD),
    /** The end of that road. Does not pass on its own. */
    SEPSIS("sepsis", 2, ChatFormatting.DARK_RED),
    /**
     * Something that set in a leg, and is now in the lungs.
     *
     * <p>The fastest thing in this mod after an artery, and the only one that arrives with
     * no warning at all: the warning was four minutes ago, in a limb, and it was a limb
     * that ached. What happens here is a chest that cannot get air past something solid,
     * so the breathing goes first and the heart follows it up.
     *
     * <p>Nothing that answers an illness answers this. No bandage, no drip, no course.
     * There is one drug, it breaks the thing up rather than treating the damage, and it
     * has about ninety seconds in which to be found.
     */
    EMBOLISM("embolism", 2, ChatFormatting.DARK_RED),
    /**
     * A hard knock to the head. No medicine touches it and none needs to - it wears off.
     * What it costs while it lasts is knowing your own condition: a concussed player can
     * see which parts are red and not how red, which is the one thing this mod otherwise
     * always tells you.
     */
    CONCUSSION("concussion", 0, ChatFormatting.LIGHT_PURPLE),
    /**
     * Ears blown out, which is the injury everybody who has been near a blast has and
     * nobody counts.
     *
     * <p>No treatment and none needed - it wears off. What it costs while it lasts is the
     * one sense this mod uses to warn you about yourself: a heartbeat you cannot hear is
     * a heartbeat that stops telling you how much blood is left, and a pulse you cannot
     * count by ear has to be counted by eye instead.
     *
     * <p>And it costs the medic too, which is the half worth having. A deafened patient
     * cannot hear the question - so the one channel in this mod that comes out of a body
     * rather than off one stops working on exactly the casualty who was closest to the
     * explosion.
     */
    DEAFNESS("deafness", 0, ChatFormatting.GRAY),
    /**
     * Bitten by something, and the bite was the last easy moment.
     *
     * <p>Every other illness in this mod is a race you can join late. A fever gives you
     * minutes, sepsis gives you a fight, and both of them can be answered after they have
     * announced themselves. This one cannot, and that is the whole reason it is here: it
     * is the only condition in medicine where the treatment works perfectly right up
     * until the first symptom and then does not work at all, ever, for anybody.
     *
     * <p>So it is silent. It sits there for minutes doing nothing, showing nothing, and
     * a course of antibiotics during that time takes it away completely. The moment it
     * starts to show, nothing does - and what it becomes is {@link #FRENZY}.
     */
    RABIES("rabies", 1, ChatFormatting.DARK_RED),
    /**
     * And the end of it, which is not a treatment problem any more.
     *
     * <p>Nothing touches this. There is no version of the mod where a player finds the
     * right item and walks out of it, because there is no version of the world where
     * anybody does. What is left is what to do with the time - and the honest answer,
     * the one people actually took, is to tell somebody where you are going.
     */
    FRENZY("frenzy", 2, ChatFormatting.RED),
    /**
     * The wrong blood. The body attacks what was put into it: it costs health, it costs
     * more volume than the transfusion put in, and no course of anything shortens it.
     *
     * <p>It has to be worse than doing nothing, or checking the label before you hang
     * the bag would be optional - and a rule nobody has to follow is not a rule.
     */
    REACTION("reaction", 2, ChatFormatting.DARK_PURPLE),
    /**
     * The body giving up on keeping itself running. Enough volume gone, or enough of it
     * still leaving, and circulation stops reaching the far end of anything: the patient
     * goes grey and cold and slides, and none of it is a wound anybody can point at.
     *
     * <p>This is the half of bleeding the mod never had. Losing blood used to be a slow
     * penalty you could walk off; shock is what actually kills people who have been shot,
     * and it kills them after the bleeding has been stopped.
     *
     * <p>No pill touches it and none should. It lifts when the reason for it is gone -
     * nothing still open, volume back up - and only while the patient stays still, which
     * is the argument this exists to have. They will want to get up and keep moving.
     */
    SHOCK("shock", 2, ChatFormatting.RED),
    /**
     * Cold that has stopped being a nuisance and started being a clotting problem.
     *
     * <p>It does little by itself. What it does is stop blood setting: every light cut
     * that would have closed on its own keeps running instead, so a patient who is merely
     * cold quietly turns into a patient who is bleeding out. Real trauma medicine calls
     * that half of the lethal triad, and it is the reason the first thing done to
     * somebody on a cold floor is to get them off the cold floor.
     *
     * <p>Blood loss makes it far easier to catch, which is not a balance lever: a body
     * short of blood cannot keep itself warm. The two feed each other on purpose.
     */
    HYPOTHERMIA("hypothermia", 2, ChatFormatting.DARK_AQUA),
    /**
     * The one thing on this list nobody catches and nothing caused.
     *
     * <p>Every other entry here is something that happened to a body: a wound went bad,
     * a temperature dropped, blood went on the floor. This one is rolled with the blood
     * type, at the same moment and for the same reason - it is a fact about a person
     * rather than an event in their day, and the mod has exactly one slot for that.
     *
     * <p>What it does is deliberately not what films do with it. Real voices comment and
     * criticise; they do not hand out orders, and the version that does is the single
     * most repeated untruth about this illness. The heaviest part of it is not the
     * hallucinations at all - it is the half that has no drama in it, the wanting to do
     * nothing, which is why that is in here too and why it is the part that actually
     * costs the player something.
     *
     * <p>It is treatable and not curable, which is the last true thing about it. The
     * course holds it down for as long as it is taken, and taking it costs enough that
     * stopping is a real temptation - in the mod, exactly as in life.
     */
    SCHIZOPHRENIA("schizophrenia", 1, ChatFormatting.BLUE),
    /**
     * The illness at its worst: a body that has stopped.
     *
     * <p>Not a coma and not sleep - the person is there and taking it in, and cannot
     * start anything. It is a real state, it is a medical emergency, and it is the one
     * thing on this list that does nothing to a body at all and is still the most
     * frightening entry on the chart. Nothing in the box touches it. It passes.
     *
     * <p>Which makes it the second condition in the mod after being on the floor where
     * the only thing that helps is somebody else being there - not to treat it, because
     * there is no treatment, but to keep whatever else is happening from finishing the
     * job while its owner cannot move.
     */
    CATATONIA("catatonia", 2, ChatFormatting.DARK_GRAY),
    /**
     * The treatment at its worst.
     *
     * <p>A rare reaction to the course that holds the illness down: the temperature
     * climbs, the muscles lock, and it kills. It is the mod's oldest argument in its
     * sharpest form - every treatment is a trade, and this is the one where the trade
     * can go badly enough to be the thing that ends you.
     *
     * <p>There is no antidote. Stop the course and get the body cold, which is the exact
     * inverse of what hypothermia asks for and the only place in the mod where being
     * soaked in cold water is the right answer.
     */
    NEUROLEPTIC("neuroleptic", 2, ChatFormatting.DARK_RED),
    /**
     * A mind that has stopped keeping up, because the body stopped supplying it.
     *
     * <p>The last quarter of ABCDE the mod had no answer for. A concussion is a knock to
     * the head and behaves like one; this is what a head does when it is not being fed -
     * short of blood, short of air, cooking with a fever, or cold enough that nothing is
     * working properly. It is the commonest thing in the world in a real casualty and
     * the one nobody expects, because it looks like the patient being difficult.
     *
     * <p>It has no clock and no cure of its own, which is the whole point of it: it is a
     * <em>sign</em>. Fix what is causing it and it goes. It exists to be read, on
     * somebody else, as "whatever is wrong with them is now bad enough to reach their
     * head" - and on yourself as the moment your own chart stops being worth trusting.
     */
    DELIRIUM("delirium", 1, ChatFormatting.YELLOW),
    /**
     * Lockjaw, out of a puncture somebody closed over dirt.
     *
     * <p>The mod already punishes a wound left dirty - it goes septic - so a second illness
     * out of the same neglect would be the same illness twice. This one is not about dirt in
     * general. It is about <b>the shape of the wound</b>: a deep narrow hole, from an arrow
     * or a fragment, closed at the top with the bottom still sealed in. Nothing else in this
     * mod cares what shape a wound is, and this is the one thing in medicine that does more
     * than anything else.
     *
     * <p>It is silent for a long time, and then it is not treatable. Which makes it the
     * second illness here built on a deadline nobody is shown - the first being the one in a
     * dog's mouth - and the tell is the same kind of thing: not a reading, a sentence. The
     * patient says their jaw is stiff. Ask them, or find out when the spasms start.
     *
     * <p>A spasm is the only thing in this mod that interrupts a treatment from the inside.
     * Everything else that stops a pair of hands is somebody hitting them.
     */
    TETANUS("tetanus", 2, ChatFormatting.DARK_RED),
    /**
     * What comes out of a limb when the strap comes off it.
     *
     * <p>The most purely medic-made injury in the mod, and the only one that is caused by
     * <em>doing the right thing too late</em>. A limb with no blood in it for long enough
     * fills up with everything a living limb is supposed to be flushing away, and the moment
     * the tourniquet is released all of that goes back into the person at once and stops
     * their heart.
     *
     * <p>Which is why the answer is not "leave the strap on" - a strap left on kills the limb
     * - and it is not "take it off" either. It is <b>fill them up first</b>: fluid in before
     * the strap comes off, so that what comes back out of the leg arrives diluted. That is a
     * real decision made with two items the mod already has, and it is the only place here
     * where a bag of saline is a thing you hang <em>before</em> the bleeding rather than
     * after it.
     */
    CRUSH("crush", 2, ChatFormatting.RED),
    /**
     * The one that came back, because the course did not finish.
     *
     * <p>Every other illness here is cured or it is not. This one is what happens when it
     * was nearly cured: the fever goes, the patient gets up, and four minutes later it is
     * back - and now it comes in waves rather than staying.
     *
     * <p>Which breaks the instrument. A thermometer is a figure taken at a moment, and a
     * fever that cycles is normal at the bottom of its cycle: measure then and the reading
     * is honest, correct, and says there is nothing wrong. Nothing else in this mod can be
     * <b>right and wrong at the same time</b>, and learning to measure twice ten seconds
     * apart is the whole of what this illness teaches.
     */
    RELAPSE("relapse", 1, ChatFormatting.GOLD);

    public static final Disease[] VALUES = values();
    public static final Codec<Disease> CODEC = StringRepresentable.fromEnum(Disease::values);

    private final String id;
    private final int severity;
    private final ChatFormatting colour;

    Disease(String id, int severity, ChatFormatting colour) {
        this.id = id;
        this.severity = severity;
        this.colour = colour;
    }

    public String id() {
        return id;
    }

    /** 0 is a nuisance, 2 is lethal. Also how hard the antibiotics are to get right. */
    public int severity() {
        return severity;
    }

    public ChatFormatting colour() {
        return colour;
    }

    public Component title() {
        return Component.translatable("triage.disease." + id);
    }

    /**
     * How long this runs before {@link #then()} happens, in ticks. For the ones that
     * pass by themselves this is how long they last; for the ladder it is the deadline.
     */
    public int duration() {
        Config.Values values = Config.values();
        return 20 * switch (this) {
            case COLD -> values.coldSeconds;
            case POISONING -> values.poisoningSeconds;
            case FEVER -> values.feverSeconds;
            case CONCUSSION -> values.concussionSeconds;
            case DEAFNESS -> values.deafnessSeconds;
            // The silent part. What it runs out into is the part nothing answers.
            case RABIES -> values.rabiesSeconds;
            case FRENZY -> values.frenzySeconds;
            case REACTION -> values.reactionSeconds;
            // These three do not run on a clock at all. Sepsis has to be cured; shock
            // and hypothermia lift when the thing causing them is dealt with, which is
            // a condition rather than a duration - see Ailments.
            // Worms sit on this list rather than the timed one for the same reason: a
            // parasite is not self-limiting, and waiting it out is not a plan.
            // And the one in the lungs, which is on this list for the bleakest reason of
            // the lot: it does not pass, it does not get worse, and it does not need to.
            // It is already taking the chest apart at a rate nothing else here matches,
            // and the only clock that matters is how long the patient has.
            case SEPSIS, SHOCK, HYPOTHERMIA, SCHIZOPHRENIA, NEUROLEPTIC, DELIRIUM, PARASITES,
                 EMBOLISM -> Integer.MAX_VALUE / 20;
            // What comes out of a limb does not wait and does not pass: either the fluid
            // arrives or the heart stops, and both of those happen inside a minute.
            case CRUSH -> values.crushSeconds;
            // The silence before the jaw. This is the clock the patient is not shown, and
            // when it runs out the course stops being an answer - see curable().
            case TETANUS -> values.tetanusSeconds;
            // And the one that does not end on its own, because ending on its own is
            // exactly what it pretended to do once already.
            case RELAPSE -> Integer.MAX_VALUE / 20;
            // Neither of these two passes on its own either. What their clock measures is
            // how long there is before they stop being what they are - see then().
            case FLU -> values.fluSeconds;
            case SMOKE -> values.smokeSeconds;
            case STOMACH -> values.stomachSeconds;
            case PNEUMONIA -> values.pneumoniaSepsisSeconds;
            case PERITONITIS -> values.peritonitisSepsisSeconds;
            case CATATONIA -> values.catatoniaSeconds;
        };
    }

    /**
     * Whether a course of antibiotics is the answer to this. A concussion is the one that
     * is not: it is an injury the body files under illness because it belongs to the
     * whole head rather than to a wound, and pills would be a cure for nothing.
     *
     * <p>A transfusion reaction is the other one. It is not an infection either; it is
     * the body destroying what was given to it, and it stops when it has finished.
     */
    public boolean treatable() {
        return switch (this) {
            // None of these four is an infection, so none of them is what antibiotics
            // are for. A concussion is a knock, a reaction is the body destroying what
            // was given to it, shock is circulation failing and hypothermia is heat
            // gone. A course aimed at any of them would be a cure for nothing.
            // Antibiotics are for infections. This one has its own course and it is not
            // a cure either: see MedicineKind.ANTIPSYCHOTIC.
            // Rabies is deliberately absent from this list: a course does answer it,
            // and only while it is still silent. FRENZY is on it, because by then no
            // course answers anything.
            // And the two that are not bacteria at all. This is the whole of what a
            // virus is for in here: a course is not weak against it, it is beside the
            // point, and the bottle is better kept for something it can reach.
            // Swelling is not an infection. There is nothing in a bottle for an
            // airway, which is exactly why this one is frightening.
            // And worms, which are not bacteria either and have their own card.
            case FLU, STOMACH, SMOKE, PARASITES,
                 FRENZY, DEAFNESS, CONCUSSION, REACTION, SHOCK, HYPOTHERMIA, SCHIZOPHRENIA,
                 CATATONIA, NEUROLEPTIC, DELIRIUM -> false;
            default -> true;
        };
    }

    /** What this turns into when its time runs out, or null when it simply ends. */
    public @Nullable Disease then() {
        return switch (this) {
            case FEVER -> SEPSIS;
            // Influenza is deliberately not here. It does become pneumonia - that is
            // the whole of what it is for - but not by running out of clock, because
            // then every flu would end in pneumonia and the bottle would be mandatory
            // rather than a decision. Whether the chest opens is decided by the body,
            // in Sick.complicates, the same way a cold settling is.
            // Both of the illnesses that happen to an organ end the same way if nobody
            // stops them, and it is the same end everything else here has.
            case PNEUMONIA, PERITONITIS -> SEPSIS;
            // The silence running out. This is the moment the course stops being an
            // answer, and nothing in the mod tells anybody it is coming.
            case RABIES -> FRENZY;
            default -> null;
        };
    }

    /**
     * Whether waiting is enough. The rest run down a clock and end; these three do not.
     * Sepsis has to be cured, and shock and hypothermia end when their cause does.
     */
    /**
     * Whether this is a virus.
     *
     * <p>Which matters for exactly one reason and it is worth the enum knowing it: a
     * course of antibiotics does nothing to a virus. Not less, not slower - nothing.
     */
    public boolean viral() {
        return this == FLU || this == STOMACH;
    }

    public boolean permanent() {
        // Nothing about a parasite is self-limiting. That is the definition of one, and it
        // is why waiting is not a plan here the way it is for most of this list.
        return this == SEPSIS || this == SHOCK || this == HYPOTHERMIA || this == SCHIZOPHRENIA
                || this == NEUROLEPTIC || this == DELIRIUM || this == PARASITES;
    }

    public static @Nullable Disease byId(String id) {
        for (Disease disease : VALUES) {
            if (disease.id.equals(id.toLowerCase(Locale.ROOT))) {
                return disease;
            }
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
