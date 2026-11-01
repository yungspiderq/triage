package com.claudemods.triage.client;

import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.Config;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.body.PartState;
import com.claudemods.triage.client.screen.BodyScreen;
import com.claudemods.triage.client.screen.AidScreen;
import com.claudemods.triage.client.screen.TreatmentScreen;
import net.minecraft.network.chat.Component;
import com.claudemods.triage.net.GiveUpPayload;
import com.claudemods.triage.net.OpenTreatmentPayload;
import com.claudemods.triage.net.ShowBodyPayload;
import com.claudemods.triage.net.SyncBodyPayload;
import com.claudemods.triage.net.OpenAidPayload;
import com.claudemods.triage.net.UpdatePatientPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/** The client's copy of its own body. Read by the HUD, written by the sync packet. */
public final class ClientBody {
    /** Two seconds of holding. Long enough to be a decision, short enough to obey. */
    public static final int GIVE_UP_TICKS = 40;

    private static BodyHealth body = new BodyHealth();

    /** How long the give-up key has been held, for the bar on the HUD. */
    private static int giveUpHeld;
    private static boolean gaveUp;

    private ClientBody() {
    }

    public static BodyHealth get() {
        return body;
    }

    /**
     * Your own body, as the chart is allowed to print it.
     *
     * <p>Somebody else's arrives redacted from the server and needs nothing done to it.
     * Your own arrives whole, and it has to: everything that draws what being hurt feels
     * like - the edges closing in, the ringing, the leg that will not take weight - reads
     * this, and those are what a person has about themselves.
     *
     * <p>What a person does not have about themselves is a diagnosis. So the chart, and
     * only the chart, reads this instead: how bad each part is and how weak you feel stay,
     * and a cracked rib is a chest that hurts until somebody puts their hands on it.
     */
    public static BodyHealth charted() {
        BodyHealth own = get();
        return Config.values().realistic ? own.asSeen(true) : own;
    }

    /**
     * Whether the head doing the reading is too rattled to read.
     *
     * <p>Always the local player's own, even when the body on screen is somebody else's:
     * a concussed medic cannot read a chart any better than they can read themselves.
     */
    public static boolean concussed() {
        return body.has(Disease.CONCUSSION);
    }

    /**
     * A part's health as a pair of numbers, or a pair of question marks.
     *
     * <p>Deliberately question marks rather than wrong numbers. A HUD that quietly lies
     * reads as a broken mod; a HUD that says it does not know reads as a mechanic, and
     * the colours are still there to act on.
     */
    /**
     * What a limb is called on the chart just now.
     *
     * <p>Sometimes the wrong thing. Two of them swap names on a slow clock - slow on
     * purpose, so that reading it twice does not average the answer out - and nothing
     * else about the row moves: the bar, the pips and the numbers stay where they
     * belong. The information is all still there and it is attached to the wrong word,
     * which is a different problem from not being able to read it and a much worse one
     * to work with.
     *
     * <p>Concussion never does this. A coarse answer is still true; this one is not.
     */
    public static Component shownTitle(BodyPart part) {
        if (!disorganised()) {
            return part.title();
        }
        long window = System.currentTimeMillis() / 9000L;
        int count = BodyPart.VALUES.length;
        int first = (int) Math.floorMod(window * 31, count);
        int second = (int) Math.floorMod(window * 17 + 3, count);
        if (first == second) {
            return part.title();
        }
        if (part.ordinal() == first) {
            return BodyPart.VALUES[second].title();
        }
        if (part.ordinal() == second) {
            return BodyPart.VALUES[first].title();
        }
        return part.title();
    }

    private static boolean disorganised() {
        return Config.values().schizophrenia
                && body.has(Disease.SCHIZOPHRENIA) && !body.medicated();
    }

    public static Component readout(PartState state, String separator) {
        if (concussed()) {
            return Component.literal("?" + separator + "?");
        }
        return Component.literal("%.0f%s%.0f".formatted(state.health, separator, state.max));
    }

    /** The same for the one number that belongs to no part: how much blood is left. */
    public static Component bloodReadout(BodyHealth of) {
        return concussed()
                ? Component.literal("?%")
                : Component.literal(Math.round((1.0F - of.bloodLoss()) * 100.0F) + "%");
    }

    /**
     * How full to draw a part's bar, and how far along the colour ramp to put it.
     *
     * <p>Hiding the numbers alone was not worth much: a bar is a number you can read
     * with a ruler, so the whole readout survived intact underneath. Concussed, every
     * bar collapses to one of three lengths - bad, half, mostly fine - and the top band
     * stops short of full, because somebody who cannot assess themselves cannot tell
     * untouched from scratched either.
     *
     * <p>Nothing is fabricated: a coarse answer is still a true one, and a part that is
     * actually gone still reads as gone. You can act on this, you just cannot plan on it.
     */
    public static float shownRatio(PartState state) {
        return blur(state.ratio());
    }

    /** The same three bands for the blood bar. */
    public static float shownBlood(BodyHealth of) {
        return blur(1.0F - of.bloodLoss());
    }

    private static float blur(float ratio) {
        if (!concussed() || ratio <= 0.0F) {
            return ratio;
        }
        if (ratio < 0.34F) {
            return 0.2F;
        }
        return ratio < 0.67F ? 0.5F : 0.82F;
    }

    public static void accept(SyncBodyPayload payload) {
        body = payload.body();
    }

    public static void openTreatment(OpenTreatmentPayload payload) {
        Minecraft.getInstance().setScreen(
                new TreatmentScreen(payload.kind(), payload.patientId(), payload.body()));
    }

    public static void updatePatient(UpdatePatientPayload payload) {
        TreatmentScreen.updatePatient(payload.body());
        AidScreen.updatePatient(payload.body());
    }

    /** Somebody put their hands on somebody. Opens the screen where that is earned. */
    public static void openAid(OpenAidPayload payload) {
        Minecraft.getInstance().setScreen(new AidScreen(payload.kind(), payload.patientId(),
                payload.body(), payload.part().orElse(null)));
    }

    /** Somebody looked at somebody else. Opens their chart, read-only. */
    /** The picture, on its own screen, because a film is not a chart. */
    public static void showFilm(com.claudemods.triage.net.ShowFilmPayload payload) {
        Minecraft.getInstance().setScreen(new com.claudemods.triage.client.screen.FilmScreen(
                payload.patientId(), payload.body()));
    }

    public static void showBody(ShowBodyPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        // Already looking at them: this is the same chart being redrawn after a mark
        // went on it, and rebuilding the screen would throw away the scroll of the eye
        // for nothing.
        if (minecraft.screen instanceof BodyScreen open && open.showing(payload.patientId())) {
            open.update(payload.body());
            return;
        }
        Component name = minecraft.level != null
                && minecraft.level.getEntity(payload.patientId()) instanceof Player player
                ? player.getName()
                : Component.translatable("triage.screen.them");
        minecraft.setScreen(new BodyScreen(payload.patientId(), name, payload.body()));
    }

    public static void interruptTreatment() {
        TreatmentScreen.interruptCurrent();
        AidScreen.interruptCurrent();
    }

    public static int giveUpHeld() {
        return giveUpHeld;
    }

    /**
     * Counts the held key while the player is on the floor, and sends the packet
     * once. Reset the moment they are up again, so a rescue never leaves a primed
     * counter behind.
     */
    public static void tickGiveUp(boolean held) {
        if (!body.downed()) {
            giveUpHeld = 0;
            gaveUp = false;
            return;
        }
        giveUpHeld = held ? giveUpHeld + 1 : 0;
        if (giveUpHeld >= GIVE_UP_TICKS && !gaveUp) {
            gaveUp = true;
            PacketDistributor.sendToServer(new GiveUpPayload());
        }
    }

    /** Clears everything when leaving a world, so nothing leaks into the next one. */
    public static void reset() {
        body = new BodyHealth();
        giveUpHeld = 0;
        gaveUp = false;
    }
}
