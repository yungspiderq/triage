package com.claudemods.triage.body;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The two things worth doing for somebody that cost nothing to do.
 *
 * <p>Everything else the mod calls treatment is an item: a bandage, a splint, a kit
 * somebody crafted and carried and can run out of. That left a player with empty hands
 * standing over a dying one with nothing to offer but company, which is the exact
 * opposite of what first aid is. The two interventions that save the most lives outside
 * a hospital are both free, and both of them are here.
 *
 * <p>Neither of them repairs anything. An airway held open and a heart pushed round by
 * hand buy time and nothing else - the wound that put them on the floor is still there
 * and still needs the kit. What they change is who can help: until now the answer was
 * "whoever packed a surgical kit this morning", and now it is "whoever is here".
 */
public enum AidKind implements StringRepresentable {
    /**
     * Tilting the head back so that air can get past a slack tongue.
     *
     * <p>Not a fix and not a state that stays fixed: an unconscious airway closes again
     * the moment nobody is holding it, which is why this is worth doing over and over
     * rather than once.
     */
    AIRWAY("airway"),
    /**
     * Pushing the blood round for a heart that has stopped doing it.
     *
     * <p>Compressions do not restart hearts - that is the part everybody has wrong, and
     * the mod is not going to teach it wrong. They keep a body worth saving until
     * somebody arrives who can save it.
     */
    COMPRESSIONS("compressions"),
    /**
     * A hand pushed down on the place the blood is coming out of.
     *
     * <p>The first thing anybody is taught and the last thing this mod had: until now,
     * a bleeding teammate and no bandage was a bleeding teammate and nothing at all.
     * It closes nothing - it holds, and only while it is held.
     */
    PRESSURE("pressure"),
    /**
     * Getting the shaft out of somebody.
     *
     * <p>The only one of these you can do to yourself, and not as a concession: pulling
     * an arrow out of your own arm is a thing people do, badly, on their own, and the
     * mod refusing it would be a rule about tidiness rather than about bodies.
     */
    PULL("pull"),
    /**
     * Rolling somebody onto their side.
     *
     * <p>The thing that is done for an unconscious casualty who is breathing, in every
     * first-aid course there has ever been, and it is done for exactly the reason this
     * mod already models: nobody holds their own airway open. On their back, the tongue
     * and everything else in the mouth goes the one way gravity takes it. On their side,
     * gravity is on their side.
     *
     * <p>Which makes it the only thing in here that keeps working after the hands have
     * gone. Opening an airway buys twenty seconds and has to be done again; rolling
     * somebody over halves the rate for as long as they stay there, and a body left in
     * the recovery position by somebody who then walked off is a body that is still
     * better off. That is not a small thing to be able to say about a treatment.
     */
    ROLL("roll"),
    /**
     * Taking a dressing off with your fingers.
     *
     * <p>Free, available to anybody, and the only thing in this class that can leave the
     * patient worse than it found them - which puts it beside pulling an arrow out, and
     * for the same reason. A dressing that has been on a while is not lying on a wound,
     * it is stuck to one: what is holding it there is what was trying to close underneath.
     * Take it slowly and it comes away. Take it off the way anybody takes a plaster off
     * and it comes away with the clot.
     *
     * <p>Which is what the scissors are for, and the whole argument for carrying a pair.
     */
    UNWRAP("unwrap"),
    /**
     * Letting a strap off.
     *
     * <p>Nothing goes wrong here and nothing needs to: what happens is simply what the
     * strap was stopping. The wound under it was never closed - that is the entire bargain
     * a tourniquet makes - so the moment it comes off, the bleeding is exactly the bleeding
     * it was. This exists because the only way to take one off used to be to spend a
     * bandage, and "I have changed my mind" should not cost a dressing.
     */
    UNSTRAP("unstrap"),
    /**
     * Two fingers on a wrist.
     *
     * <p>The first thing anybody does to anybody, and the only one of these whose
     * reward is not a repair but a fact. It costs nothing, needs no light and no item,
     * works on somebody standing up, and takes about as long as it takes.
     *
     * <p>It is also the only sign this mod has for the injury it deliberately made
     * invisible. A torn spleen puts nothing on the skin, so until now the only way to
     * find one was to open the chart and be told. A body losing blood answers by
     * driving the heart faster - so a fast thin pulse on somebody with nothing open on
     * them is the tell, and it is the same tell it is in life.
     */
    PULSE("pulse"),
    /**
     * Asking them where it hurts.
     *
     * <p>Every other reading in this mod is taken off a body. This one comes out of one,
     * and it is the only channel here that does - which makes it the only thing that
     * stops working the moment the patient stops being able to talk, and the only one
     * that can be wrong.
     *
     * <p>It is also the only thing that says <em>where</em>. A pulse says the blood went
     * somewhere; a conscious person with a torn spleen says which side of their belly it
     * went into. That is how that injury is found in the world: by asking.
     *
     * <p>And painkillers work, which is the good part. A dosed patient reports less than
     * is true - small complaints vanish and big ones come down a grade - so the medic who
     * dosed them keeps the location and loses the urgency. Nothing else in the mod makes
     * a treatment cost information.
     */
    ASK("ask"),
    /**
     * Pressing on a burn to find out how deep it went.
     *
     * <p>The third of these whose reward is a fact rather than a repair, and the only one
     * in the mod whose finding is an <b>absence</b>. A burn that hurts when you press it
     * has something living left in it and will close. A patch that does not answer at all
     * is through the skin: the nerve endings that would have done the answering are part
     * of what burnt.
     *
     * <p>Which makes a burn the one injury here that lies in the direction nobody expects.
     * The loudest patch is the one that will be fine, and the quiet one beside it is the
     * one a jar of salve does nothing for. Without this, a player treats the half that is
     * complaining and walks away from the half that needed a knife.
     *
     * <p>Free, needs no item, and works on somebody standing up - because that is what
     * pressing on somebody is.
     */
    TOUCH("touch"),
    /**
     * Asking them to put a number on it.
     *
     * <p>The first question asked of anybody in pain anywhere in the world, and the one
     * this mod did not have. It had <em>where</em>, which is the question you ask second.
     *
     * <p>It is the third kind of reading here and the mod now has one of each: a pulse is
     * a fact about a body, a temperature is a figure from an instrument, and this is an
     * <b>opinion</b>. Nothing can check it, which is why it is wrong in more ways than
     * anything else here - and every one of those ways was already built.
     *
     * <p>Which is the point of asking. A two on a heart running at a hundred and thirty is
     * not a comfortable patient; it is one somebody has already given something to, and
     * finding that out costs four seconds instead of a wasted dose.
     */
    SCORE("score"),
    /**
     * Taking a stiff joint out to the edge of what it will do.
     *
     * <p>The only thing in this class that treats something <b>permanent</b>. Everything
     * else here buys a minute against something that is happening now; this is hands on a
     * contracture that has been there for days and will be there for months, and one
     * session moves it slightly.
     *
     * <p>It is also the only one where the patient is the instrument. There is nothing to
     * see - the edge of a range of movement is not drawn on anybody - so the whole of the
     * skill is watching a face and stopping one step past comfortable. Short of that,
     * nothing loosens. Past it, something tears and it is tighter tomorrow.
     *
     * <p>Which is why it can be done to yourself and is worth half as much when it is.
     * You stop at your own pain, every time, and that is not cowardice - it is the
     * reason this is a job for somebody else's hands.
     */
    STRETCH("stretch"),
    /**
     * Pulling a broken bone back into line.
     *
     * <p>The most painful thing in this mod by a distance, the only treatment the room
     * can hear, and the only one whose whole value is in what it prevents rather than in
     * what it repairs: a bone put back straight heals straight, and a bone left where it
     * fell heals where it fell, for good.
     *
     * <p>Free, like everything else in here - it is hands and nothing else - and it is
     * the one free thing that genuinely wants a second person. Doing it to yourself is
     * allowed and it is worse, for the same reason the stretching is: you stop.
     */
    SET("set"),
    /**
     * Running a hand along a bone to find out whether it is in one piece.
     *
     * <p>The answer to the question realistic difficulty is built around, and the only one
     * to it that does not need a machine. A bone that has moved out of line is visible from
     * across the room; a bone that broke and stayed where it was puts <b>nothing</b> on the
     * outside, and until somebody puts their hands on it the only other way to find it is a
     * film taken by a thing too heavy to carry.
     *
     * <p>It is also the first reading in this mod whose price is paid by the patient. A
     * pulse costs them nothing, a question costs them nothing; this is pressure on a part
     * that is already broken, and the harder the press the clearer the answer. Clumsy hands
     * hurt, and clumsy hands on a fresh break move it - which turns a bone that would have
     * knitted straight into one that needs pulling back into line first. The examination
     * can cost more than the finding is worth, and knowing when that is true is the skill.
     *
     * <p>On yourself it is worth less, for the reason stretching is: you stop early. Your
     * own hand never presses hard enough on your own fresh fracture, and it should not.
     */
    FEEL("feel"),
    /**
     * Hands down the whole body, once, to find out where to spend the next four minutes.
     *
     * <p>The first thing anybody does to anybody, and the thing this mod did not have while
     * being named after it. Without it, examining somebody in realistic difficulty means
     * feeling along seven parts in turn - which is not thoroughness, it is four minutes of
     * pressing on a person who is bleeding, and no medic has ever worked that way.
     *
     * <p>What it answers is <b>where</b>, and only where. How bad each part is, which is
     * exactly what looking at somebody and running hands over them tells you: this leg is a
     * problem, that arm is not. <em>Why</em> it is a problem - the bone, the rot, the metal
     * in it - is untouched, and that is the work the rest of the tools exist for.
     *
     * <p>Which makes it the cheapest reading here and the one that saves the most time: one
     * sweep turns seven questions into one or two.
     */
    SURVEY("survey");

    public static final AidKind[] VALUES = values();
    public static final Codec<AidKind> CODEC = StringRepresentable.fromEnum(AidKind::values);
    public static final StreamCodec<ByteBuf, AidKind> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private final String id;

    AidKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /**
     * Whether this only means anything to somebody already on the floor.
     *
     * <p>Pressure is the exception, and deliberately so: a wound is worth holding
     * shut long before its owner falls over, and refusing it until they do would be
     * teaching players to wait for the emergency.
     */
    public boolean needsDowned() {
        return this == AIRWAY || this == COMPRESSIONS || this == ROLL;
    }

    /**
     * Whether this is a thing you can do to yourself.
     *
     * <p>Only pulling. The other three are done to somebody who cannot help themselves,
     * and being able to do them to yourself would quietly undo the rule the whole downed
     * state rests on.
     */
    public boolean onSelf() {
        return this == PULL || this == UNWRAP || this == UNSTRAP || this == PULSE
                || this == ASK || this == TOUCH || this == SCORE || this == STRETCH
                || this == SET || this == FEEL || this == SURVEY;
    }

    public Component title() {
        return Component.translatable("triage.aid." + id);
    }

    /** What it is for, on the button that does it. */
    public Component hint() {
        return Component.translatable("triage.aid." + id + ".hint");
    }

    public static AidKind byId(String id) {
        for (AidKind kind : VALUES) {
            if (kind.id.equals(id.toLowerCase(Locale.ROOT))) {
                return kind;
            }
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
