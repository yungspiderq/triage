package com.claudemods.triage.client;

import com.claudemods.triage.ClientConfig;
import com.claudemods.triage.Config;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.Breathing;
import com.claudemods.triage.body.PartState;
import com.claudemods.triage.body.Temperature;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.body.Pulse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraft.util.Mth;

/**
 * What being in a bad way looks like from inside it.
 *
 * <p>The mod used to say all of this with vanilla potion effects. Losing blood handed you
 * a bottle of blindness; a rattled head handed you nausea. Those are blunt instruments -
 * they are tuned for potions, they stack badly with whatever else a pack is doing, and
 * they put icons in the corner of the screen for things that are not potions. Worst of
 * all they are <b>the same four effects for nine different states</b>, so a body running
 * out of blood and a body with a fever looked identical from the inside.
 *
 * <p>This draws the things themselves, and each one is the thing that particular state
 * actually does to a pair of eyes:
 *
 * <ul>
 *   <li><b>Volume going</b> closes the edges in and takes the colour out of what is left.
 *       That is not an effect, it is the thing people describe: the world goes grey and
 *       narrow from the outside in, and then there is a tunnel, and then there is nothing.
 *       It is also the last warning anybody gets before {@link com.claudemods.triage.body.Faint}
 *       puts them on the floor.</li>
 *   <li><b>The heart</b> shows up in that narrowing. Everything dims and comes back on the
 *       beat, at the rate the body is actually running - so a screen that has started
 *       pulsing quickly is a pulse reading nobody had to take.</li>
 *   <li><b>A head that has been hit</b> will not hold still, and cannot quite focus. The
 *       picture doubles at the edges and the horizon drifts.</li>
 *   <li><b>Fever and sepsis</b> wash the whole thing warm and soft, which is what a
 *       temperature does to a room.</li>
 *   <li><b>Cold</b> is the opposite: everything goes hard, pale and blue at the corners,
 *       and the view shivers rather than drifts.</li>
 *   <li><b>Going out</b> is black, and it arrives faster than it leaves.</li>
 * </ul>
 *
 * <p>None of it changes anything about the body. It changes what the body can see, which
 * is the only honest place for this to live - and all of it can be turned off by the
 * person looking at it, because motion is the one thing here that can make somebody
 * genuinely unwell.
 */
public final class Symptoms implements IGuiOverlay {
    /** Where the greying starts. Below this a body is wounded, not failing. */
    private static final float BLOOD_FLOOR = 0.30F;
    /** How far in the edges can ever close, as a share of the screen. */
    private static final float MAX_CLOSE = 0.42F;
    /** How hard the beat shows, at worst. */
    private static final float MAX_THROB = 0.30F;

    private static final int GREY = 0x000C0C10;
    private static final int WARM = 0x00D8A060;
    private static final int COLD = 0x00A8C8E8;
    private static final int BLACK = 0xFF000000;
    /** Fresh blood over an eye, and what it goes once it has been there a while. */
    private static final int BLOOD_WET = 0x00A31F16;
    private static final int BLOOD_DRY = 0x005A1610;
    /** A speck of nothing, which is what the eye reports when it is short. */
    private static final int SPECK = 0x00F4F0E4;

    /** Smoothed so nothing on screen ever snaps between two frames. */
    private static float close;
    private static float wash;
    private static float black;
    /** How far the lids are down, and where the next blink is coming from. */
    private static float lids;
    private static int blinkIn = 200;
    private static int blinking;
    private static int blinkLength;
    /** Where the beat is in its cycle, advanced by the real rate. */
    private static float beat;

    /** How much blood has run into the eye, and how dry it has gone since. */
    private static float smear;

    /** The breath, on its own clock. Nothing else here runs at this speed. */
    private static float breathPhase;

    /**
     * Specks at the edge of vision: where each one is, and how long it has left.
     *
     * <p>Three numbers each rather than a class, because there are fourteen of them and
     * they are rebuilt constantly - the thing they are imitating is a retina that is not
     * getting enough, and that does not produce tidy objects either.
     */
    private static final int SPECKS = 14;
    private static final float[] speck = new float[SPECKS * 3];
    private static boolean specksReady;

    /**
     * Sweat, which is not drawn and is felt.
     *
     * <p>A fever does not put droplets on the inside of your eyes and no player has ever
     * seen their own face, so beads on the screen would be a film effect rather than a
     * symptom. What a hot person actually gets is the one thing sweat does to the person
     * producing it: it runs down, it arrives in an eye, and it stings - and for a second
     * they squint, and for that second they cannot see what they are doing.
     *
     * <p>Which lands where symptoms in this mod are supposed to land: it does not tell you
     * anything, it takes something away, and what it takes is exactly what somebody trying
     * to tie a knot in a bandage needs.
     */
    private static float sting;
    private static int stingIn = 240;

    /** How much of a stand-up is still being paid for, and whether one is owed. */
    private static float surge;
    private static boolean wasDown;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }
        BodyHealth body = ClientBody.get();
        float strength = ClientConfig.symptoms ? Mth.clamp(ClientConfig.symptomStrength, 0.0F, 1.0F) : 0.0F;
        float step = Math.min(0.25F, Minecraft.getInstance().getDeltaFrameTime() * 0.06F);

        close = Mth.lerp(step, close, Math.min(1.0F, narrowing(body) + surge * 0.55F) * strength);
        wash = Mth.lerp(step, wash, Math.min(1.0F, haze(body) + surge * 0.5F) * strength);
        // Black arrives quickly and lifts slowly, because that is the order it happens
        // in: going out is sudden and coming round is not.
        // Out is out, whatever the strength slider says: this one is not a symptom
        // being illustrated, it is the fact that the eyes are shut. Everything else here
        // can be turned down by whoever is looking; being unconscious cannot.
        float wantBlack = body.fainted() || body.under() ? 1.0F : dark(body) * strength;
        black = Mth.lerp(wantBlack > black ? 0.35F : 0.04F, black, wantBlack);
        if (wantBlack >= 1.0F && black > 0.9F) {
            black = 1.0F;
        }

        advanceBeat(body);
        advanceBreath(body);

        // Getting up. Blood that was level with the head a second ago is now a foot and a
        // half below it, and a body short of it does not catch up in time - which is the
        // one thing on this list everybody has felt, standing up too fast in a warm room.
        boolean down = body.downed() || minecraft.player.isCrouching();
        if (wasDown && !down) {
            surge = Math.max(surge, Mth.clamp(body.bloodLoss() * 1.6F, 0.0F, 1.0F));
        }
        wasDown = down;
        surge = Math.max(0.0F, surge - Minecraft.getInstance().getDeltaFrameTime() * 0.022F);

        // Blood over an eye arrives as fast as it runs and leaves at the speed of drying,
        // which is a great deal slower. Nothing clears it but a dressing.
        float wantSmear = eyeBlood(body) * strength;
        smear = Mth.lerp(wantSmear > smear ? 0.09F : 0.012F, smear, wantSmear);

        // Sweat arriving in an eye. Rare when somebody is merely warm and every few
        // seconds when they are burning, which is the whole of the effect: it is not the
        // sting that matters, it is how often it comes.
        heat(body, strength);

        float wantLids = tiredness(body) * strength;
        lids = Mth.lerp(step, lids, wantLids);
        blink(body);

        float specks = specking(body) * strength;
        float tight = tightness(body) * strength;
        if (close <= 0.004F && wash <= 0.004F && black <= 0.004F && lids <= 0.004F
                && smear <= 0.004F && specks <= 0.004F && tight <= 0.004F && sting <= 0.004F) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (lids > 0.004F || sting > 0.004F) {
            eyelids(graphics, width, height);
        }

        if (close > 0.004F) {
            edges(graphics, width, height, close);
        }
        // The chest, on its own clock. Top and bottom rather than all four sides, because
        // what this is imitating is a breath that will not finish rather than a view that
        // is closing - and because the tunnel already owns the corners.
        if (tight > 0.004F) {
            chest(graphics, width, height, tight);
        }
        if (specks > 0.004F) {
            specks(graphics, width, height, specks);
        }
        if (wash > 0.004F) {
            int tint = body.has(Disease.HYPOTHERMIA) ? COLD
                    : body.has(Disease.FEVER) || body.has(Disease.SEPSIS) ? WARM : GREY;
            // And the one that is both, in turn. A fever that comes in waves starts each
            // wave as a chill - which is why the person having it asks for a blanket
            // while their temperature is climbing - and ends it hot. It is the only
            // thing this file draws that changes colour on its own, and that is the tell:
            // the screen going cold and then warm and then cold again is the same
            // information the thermometer refuses to give unless you take it twice.
            if (body.has(Disease.RELAPSE)) {
                float wave = relapse(body);
                tint = wave < 0.5F ? COLD : WARM;
            }
            graphics.fill(0, 0, width, height, tint | (Math.round(wash * 70.0F) << 24));
        }
        // Over everything except being unconscious, because it is on the eye rather than
        // in it: whatever else is happening to the view, this is in front of it.
        if (smear > 0.004F) {
            eye(graphics, width, height);
        }
        if (black > 0.004F) {
            // Rounded up rather than eased in at the top end. A screen at 251 of 255 is
            // a screen somebody can still read shapes through, and the whole of what a
            // faint is, is not being there: while it lasts there is nothing to see, not
            // nearly nothing.
            int alpha = black > 0.985F ? 255 : Math.round(black * 255.0F);
            graphics.fill(0, 0, width, height, (alpha << 24) | (BLACK & 0xFFFFFF));
        }
    }

    /**
     * The edges coming in, and the beat showing in them.
     *
     * <p>Four gradients rather than a round mask, because a mask is a texture and a
     * texture is a file to keep in step with every screen size there is. What matters is
     * that the world is being seen through a narrowing hole, and four sides do that.
     */
    private static void edges(GuiGraphics graphics, int width, int height, float amount) {
        float throb = 1.0F + Mth.sin(beat * Mth.TWO_PI) * MAX_THROB * Math.min(1.0F, amount * 2.0F);
        float share = Mth.clamp(amount * MAX_CLOSE * throb, 0.0F, 0.9F);
        int vertical = Math.round(height * share);
        int horizontal = Math.round(width * share * 0.8F);
        int edge = 0xE6000000 | (GREY & 0xFFFFFF);
        int inner = GREY;

        graphics.fillGradient(0, 0, width, vertical, edge, inner);
        graphics.fillGradient(0, height - vertical, width, height, inner, edge);
        // Left and right have to be drawn as gradients along x, which fillGradient does
        // not do - so they are strips, each a little darker than the last.
        for (int i = 0; i < horizontal; i++) {
            int alpha = Math.round(230.0F * (1.0F - i / (float) horizontal));
            if (alpha <= 2) {
                continue;
            }
            int colour = (alpha << 24) | (GREY & 0xFFFFFF);
            graphics.fill(i, 0, i + 1, height, colour);
            graphics.fill(width - i - 1, 0, width - i, height, colour);
        }
    }

    /**
     * How far down the lids are, before the blinking is added.
     *
     * <p>Nothing at all for the first stretch of it - one late night is not an affliction
     * and a mod that letterboxed the screen for it would be a mod nobody keeps installed.
     */
    private static float tiredness(BodyHealth body) {
        float from = Config.values().sleepyFrom;
        return Mth.clamp((body.tired() - from) / Math.max(0.01F, 1.0F - from), 0.0F, 1.0F);
    }

    /**
     * And the blinking, which is the half that actually reads as tired.
     *
     * <p>Lids that sit at a height are a letterbox. What a tired person does is <b>blink
     * slowly, and then blink for longer</b>, and the tell is that the gaps between get
     * shorter while the blinks themselves get longer - until one of them is long enough
     * that the word for it is no longer blinking. That last one is not drawn here: it is
     * a real second and a half on the floor, and it arrives from the server. See Vigil.
     */
    private static void blink(BodyHealth body) {
        if (lids <= 0.01F) {
            blinking = 0;
            return;
        }
        int step = Math.max(1, Math.round(Minecraft.getInstance().getDeltaFrameTime()));
        if (blinking > 0) {
            blinking -= step;
            return;
        }
        blinkIn -= step;
        if (blinkIn > 0) {
            return;
        }
        // Between eight seconds and a second and a half, by how far gone they are.
        float gap = Mth.lerp(lids, 160.0F, 30.0F);
        blinkIn = Math.round(gap * (0.6F + (float) Math.random() * 0.8F));
        blinkLength = Math.round(Mth.lerp(lids, 4.0F, 14.0F));
        blinking = blinkLength;
    }

    /**
     * Two lids, and the edge of each one soft.
     *
     * <p>Drawn as a gradient rather than a bar because that is the difference between an
     * eyelid and a letterbox: the light does not stop at a line, it fades under the lash.
     */
    private static void eyelids(GuiGraphics graphics, int width, int height) {
        // A squint is the same two lids doing the same thing for a different reason, so
        // it is drawn by the same eight lines. Tiredness holds them down; salt shuts
        // them and lets go.
        float shut = Math.max(lids, sting * 0.8F);
        if (blinking > 0 && blinkLength > 0) {
            // Closed, and opening again - the second half of a blink is slower than the
            // first, which is most of why a slow blink looks like a tired one.
            float through = 1.0F - blinking / (float) blinkLength;
            shut = Math.max(shut, 1.0F - through * through);
        }
        int top = Math.round(height * (0.05F + 0.42F * shut));
        int bottom = Math.round(height * (0.03F + 0.30F * shut));
        int edge = 0xFF000000;
        int fade = 0x00000000;
        graphics.fill(0, 0, width, Math.max(0, top - 6), edge);
        graphics.fillGradient(0, Math.max(0, top - 6), width, top + 4, edge, fade);
        graphics.fill(0, Math.min(height, height - bottom + 6), width, height, edge);
        graphics.fillGradient(0, height - bottom - 4, width, Math.min(height, height - bottom + 6),
                fade, edge);
    }

    /** Moves the beat on at the rate this body is actually running. */
    private static void advanceBeat(BodyHealth body) {
        int rate = Math.max(30, Pulse.of(body).rate());
        // Beats per tick: a rate of sixty is one beat a second is one beat in twenty.
        beat += Minecraft.getInstance().getDeltaFrameTime() * (rate / 60.0F) / 20.0F;
        if (beat > 1.0F) {
            beat -= (int) beat;
        }
    }

    /** How far in the walls are: volume gone, then shock, then the head itself. */
    private static float narrowing(BodyHealth body) {
        float amount = Mth.clamp((body.bloodLoss() - BLOOD_FLOOR) / (1.0F - BLOOD_FLOOR), 0.0F, 1.0F);
        if (body.has(Disease.SHOCK)) {
            amount = Math.max(amount, 0.45F);
        }
        // A head with nothing left in it stops supplying its own eyes.
        float head = 1.0F - body.part(BodyPart.HEAD).ratio();
        amount = Math.max(amount, Math.max(0.0F, head - 0.4F));
        return Mth.clamp(amount, 0.0F, 1.0F);
    }

    /** And how much of a wash is over the top of it. */
    private static float haze(BodyHealth body) {
        float amount = 0.0F;
        if (body.has(Disease.FEVER)) {
            amount += 0.5F;
        }
        if (body.has(Disease.SEPSIS)) {
            amount += 0.7F;
        }
        if (body.has(Disease.CONCUSSION)) {
            amount += 0.35F;
        }
        if (body.has(Disease.HYPOTHERMIA)) {
            amount += 0.5F;
        }
        if (body.has(Disease.SMOKE)) {
            amount += 0.3F;
        }
        // Enough to be visible at the top of a wave and gone at the bottom, because the
        // bottom of the wave is the thing this illness is about.
        if (body.has(Disease.RELAPSE)) {
            float wave = relapse(body);
            amount += Math.abs(wave - 0.5F) * 2.0F * 0.45F;
        }
        return Mth.clamp(amount, 0.0F, 1.0F);
    }

    /**
     * Where a relapsing fever is in its cycle: 0 at the cold end, 1 at the hot one.
     *
     * <p>Taken from the same tick count the server runs the illness on, so what is on the
     * screen and what a thermometer would read are the same wave rather than two things
     * that happen to look alike.
     */
    private static float relapse(BodyHealth body) {
        float phase = (body.diseaseTicks(Disease.RELAPSE) % 400) / 400.0F;
        return 0.5F + Mth.sin(phase * Mth.TWO_PI) * 0.5F;
    }

    /**
     * How hard somebody is sweating, and therefore how often they are about to be blind
     * for half a second.
     *
     * <p>Taken from the temperature the mod already works out rather than from a list of
     * illnesses, so anything that ever makes anybody hot does this without being added
     * here - which is the same rule the breath and the beat on this screen follow.
     */
    private static float sweating(BodyHealth body) {
        float over = Temperature.of(body).degrees() - 37.6F;
        float heat = Mth.clamp(over / 2.4F, 0.0F, 1.0F);
        // And the cold sweat, which is not about heat at all: a body that has lost enough
        // blood does this while its temperature is going the other way.
        float shock = body.has(Disease.SHOCK) ? 0.5F : 0.0F;
        return Math.max(heat, Math.max(shock, Mth.clamp((body.bloodLoss() - 0.5F) * 2.0F,
                0.0F, 0.7F)));
    }

    /** Runs the clock between one sting and the next, and the sting itself. */
    private static void heat(BodyHealth body, float strength) {
        float amount = sweating(body) * strength;
        sting = Math.max(0.0F, sting - Minecraft.getInstance().getDeltaFrameTime() * 0.09F);
        if (amount <= 0.05F) {
            return;
        }
        stingIn -= Math.max(1, Math.round(Minecraft.getInstance().getDeltaFrameTime()));
        if (stingIn > 0) {
            return;
        }
        // Every twenty-five seconds when they are merely warm, every four when they are
        // not. Jittered, because a symptom on a metronome stops being a symptom.
        float gap = Mth.lerp(amount, 500.0F, 80.0F);
        stingIn = Math.round(gap * (0.6F + (float) Math.random() * 0.8F));
        sting = Math.min(1.0F, 0.55F + amount * 0.45F);
    }

    /** Out, or nearly. */
    private static float dark(BodyHealth body) {
        if (body.fainted() || body.under()) {
            return 1.0F;
        }
        // Nothing else in here ever reaches one, on purpose: the rest are degrees of
        // seeing badly, and only these two are not seeing.
        if (body.downed()) {
            return 0.45F;
        }
        // The last of it, so that the screen is nearly gone before the body is.
        return Mth.clamp((body.bloodLoss() - 0.82F) / 0.18F, 0.0F, 0.8F);
    }

    // ---- the half that moves ------------------------------------------------

    /**
     * How far the horizon is off, in degrees, this frame.
     *
     * <p>Three different movements for three different reasons, and they are told apart
     * by their speed rather than by their size. A rattled head drifts slowly and does not
     * come back to the same place. A body running out of blood sways with its own pulse.
     * A cold one shivers, which is fast, small and completely regular.
     */
    public static float roll(float ticks) {
        if (!ClientConfig.symptoms || !ClientConfig.sway) {
            return 0.0F;
        }
        BodyHealth body = ClientBody.get();
        float strength = Mth.clamp(ClientConfig.symptomStrength, 0.0F, 1.0F);
        float amount = 0.0F;
        if (body.has(Disease.CONCUSSION)) {
            amount += Mth.sin(ticks * 0.031F) * 2.6F + Mth.sin(ticks * 0.017F) * 1.4F;
        }
        if (body.has(Disease.DELIRIUM) || body.has(Disease.FRENZY)) {
            amount += Mth.sin(ticks * 0.044F) * 2.0F;
        }
        float bled = Mth.clamp((body.bloodLoss() - BLOOD_FLOOR) / (1.0F - BLOOD_FLOOR), 0.0F, 1.0F);
        if (bled > 0.0F) {
            amount += Mth.sin(beat * Mth.TWO_PI) * 2.2F * bled;
        }
        if (body.has(Disease.HYPOTHERMIA)) {
            // In bursts rather than constantly, which is what shivering is: a run of it,
            // a pause, another run. The shake inside a burst stays fast and regular -
            // that part was right - but a body that shivered without stopping would be a
            // body with nothing left to shiver with.
            float burst = Mth.sin(ticks * 0.021F);
            amount += Mth.sin(ticks * 0.9F) * 0.75F * (burst > 0.0F ? burst * burst : 0.0F);
        }
        // The only thing in here with a side to it. Everything else this file draws is a
        // state - how much blood, how hot, how tired - and a state has no direction; a body
        // that leans is a body naming the leg it will not stand on. Which in realistic is a
        // finding arriving through the floor instead of through a form, and it is the only
        // one that does.
        float leftLeg = body.part(BodyPart.LEFT_LEG).ratio();
        float rightLeg = body.part(BodyPart.RIGHT_LEG).ratio();
        float lean = (leftLeg - rightLeg) * 3.2F;
        if (Math.abs(lean) > 0.15F) {
            // Held rather than swung: weight goes onto the good leg and stays there.
            amount += Mth.clamp(lean, -3.2F, 3.2F);
        }
        // A jaw closing on its own, and then everything else with it. The quiet half is
        // deliberately almost nothing - a small fast tremor every half a minute, which a
        // player will feel and will not be able to name. That is the correct amount of
        // warning for an illness whose whole design is that it is silent while it is
        // treatable: enough to send somebody to find out, never enough to tell them.
        if (body.has(Disease.TETANUS)) {
            boolean loud = body.diseaseTicks(Disease.TETANUS)
                    >= Config.values().tetanusSeconds * 20;
            float window = Mth.sin(ticks * (loud ? 0.020F : 0.0053F));
            float on = window > 0.0F ? window * window * window : 0.0F;
            amount += Mth.sin(ticks * 1.3F) * (loud ? 3.4F : 0.85F) * on;
        }
        if (body.has(Disease.CONCUSSION)) {
            // And the thing a rattled head does that a drift does not: it goes, suddenly,
            // and comes back. Rare, short and sharp, so that a player learns to stop
            // moving for a second rather than to fight it - which is the correct lesson
            // and the one the slow drift alone never taught.
            float window = Mth.sin(ticks * 0.013F);
            float spike = window > 0.0F ? window * window * window * window : 0.0F;
            amount += Mth.sin(ticks * 0.71F) * 5.5F * spike;
        }
        return amount * strength;
    }

    /**
     * And how much narrower the world is, as a share of the field of view.
     *
     * <p>Tunnel vision is a real narrowing rather than a dark ring: what closes is what
     * the eye is getting, so the picture itself tightens. Kept small - a field of view
     * that moved a lot would be a camera effect rather than a symptom.
     */
    public static double fov() {
        if (!ClientConfig.symptoms) {
            return 1.0;
        }
        BodyHealth body = ClientBody.get();
        float strength = Mth.clamp(ClientConfig.symptomStrength, 0.0F, 1.0F);
        return 1.0 - narrowing(body) * 0.12F * strength;
    }


    /**
     * The breath, advanced on the rate the mod already works out for a chest.
     *
     * <p>Deliberately the same number the chart would print. An effect that ran at a speed
     * of its own would be an effect that disagrees with the readout beside it, and this mod
     * has spent nine versions making sure those two agree.
     */
    private static void advanceBreath(BodyHealth body) {
        int rate = Math.max(6, Breathing.of(body, Config.values().airwaySeconds).rate());
        breathPhase += Minecraft.getInstance().getDeltaFrameTime() * (rate / 60.0F) / 20.0F;
        if (breathPhase > 1.0F) {
            breathPhase -= (int) breathPhase;
        }
    }

    /**
     * How hard the chest is working, which is what the top and bottom of the screen do.
     *
     * <p>A lung that has come away, an airway closing, smoke, or a thorax with very little
     * left in it. Not blood loss - that already owns the tunnel, and two effects driven by
     * one cause is one effect drawn twice.
     */
    private static float tightness(BodyHealth body) {
        float amount = 0.0F;
        if (body.part(BodyPart.THORAX).pneumo) {
            amount += 0.7F;
        }
        if (body.has(Disease.SMOKE)) {
            amount += 0.35F;
        }
        float chest = 1.0F - body.part(BodyPart.THORAX).ratio();
        amount = Math.max(amount, Math.max(0.0F, chest - 0.5F) * 1.4F);
        return Mth.clamp(amount, 0.0F, 1.0F);
    }

    /**
     * The chest, drawn as the one thing a chest does.
     *
     * <p>Two bands that come in and go out together. The phase is the breath, so a patient
     * who can see this can count their own rate off it - which is the same number the medic
     * standing over them is counting, and that is worth more than the effect is.
     */
    private static void chest(GuiGraphics graphics, int width, int height, float amount) {
        // Shallow and fast reads as a band that never quite opens, so the curve is pushed
        // toward the closed end rather than sitting in the middle of its swing.
        float swing = (Mth.sin(breathPhase * Mth.TWO_PI) + 1.0F) * 0.5F;
        float squeeze = (0.45F + swing * 0.55F) * amount;
        int band = Math.max(1, Math.round(height * 0.16F * squeeze));
        for (int i = 0; i < band; i++) {
            int alpha = Math.round(140.0F * squeeze * (1.0F - i / (float) band));
            if (alpha <= 0) {
                continue;
            }
            int colour = (alpha << 24) | (BLACK & 0xFFFFFF);
            graphics.fill(0, i, width, i + 1, colour);
            graphics.fill(0, height - i - 1, width, height - i, colour);
        }
    }

    /** How short the eye is, which is when it starts reporting things that are not there. */
    private static float specking(BodyHealth body) {
        float bled = Mth.clamp((body.bloodLoss() - 0.45F) / 0.35F, 0.0F, 1.0F);
        float head = Mth.clamp((1.0F - body.part(BodyPart.HEAD).ratio() - 0.5F) * 2.0F, 0.0F, 1.0F);
        return Math.max(bled, head);
    }

    /**
     * Specks drifting at the edge of vision.
     *
     * <p>The sign that comes <em>before</em> the walls close in, which is why it is here at
     * all: by the time the tunnel is obvious the decision has been made for you, and this
     * arrives while there is still a decision. Everybody who has stood up too fast knows
     * what it means without being told, which is the best a symptom can do.
     */
    private static void specks(GuiGraphics graphics, int width, int height, float amount) {
        if (!specksReady) {
            for (int i = 0; i < SPECKS; i++) {
                seed(i);
            }
            specksReady = true;
        }
        int shown = Math.round(SPECKS * amount);
        for (int i = 0; i < shown; i++) {
            // Up and slightly across, and then round again. A floater drifts with the
            // fluid it is in, which is not a straight line and is never quite still.
            speck[i * 3 + 1] -= 0.0016F + speck[i * 3 + 2] * 0.0022F;
            speck[i * 3] += Mth.sin(speck[i * 3 + 1] * 22.0F) * 0.0008F;
            if (speck[i * 3 + 1] < -0.05F) {
                seed(i);
            }
            int x = Math.round(speck[i * 3] * width);
            int y = Math.round(speck[i * 3 + 1] * height);
            // Dimmer toward the middle: this is the edge of vision doing it, not the
            // middle, and a speck in the centre of the screen reads as a bug on the glass.
            float middle = 1.0F - Math.abs(speck[i * 3] - 0.5F) * 2.0F;
            int alpha = Math.round(120.0F * amount * (0.25F + middle * 0.75F));
            if (alpha <= 2) {
                continue;
            }
            int size = speck[i * 3 + 2] > 0.7F ? 2 : 1;
            graphics.fill(x, y, x + size, y + size, (alpha << 24) | (SPECK & 0xFFFFFF));
        }
    }

    private static void seed(int index) {
        speck[index * 3] = (float) Math.random();
        speck[index * 3 + 1] = 0.7F + (float) Math.random() * 0.45F;
        speck[index * 3 + 2] = (float) Math.random();
    }

    /**
     * How much of what is coming out of a head has got into the eye under it.
     *
     * <p>Only the head, and only while it is open. Every other bleed in this mod is
     * somebody else's problem to look at; this one is the only injury whose first cost is
     * that the person it happened to cannot see what they are doing about it.
     */
    private static float eyeBlood(BodyHealth body) {
        PartState head = body.part(BodyPart.HEAD);
        if (head.dressed || !head.bleeding()) {
            return 0.0F;
        }
        return Mth.clamp(head.bleed / (float) PartState.BLEED_ARTERIAL, 0.35F, 1.0F);
    }

    /**
     * Blood over the eye, which is not a tint and not a vignette.
     *
     * <p>It runs from above, it goes where the shape of a face sends it, and it dries
     * rather than fading. So it is drawn as a few overlapping runs from the upper corner,
     * thickest where they start, and its colour goes from wet to old as it sits - which is
     * the only thing on this screen that tells the player how long ago they were hit.
     */
    private static void eye(GuiGraphics graphics, int width, int height) {
        float wet = Math.min(1.0F, smear * 1.3F);
        int colour = smear > 0.75F ? BLOOD_WET : BLOOD_DRY;
        int reach = Math.round(height * 0.55F * wet);
        // Three runs, each narrower and longer than the last, from the top right.
        for (int run = 0; run < 3; run++) {
            int startX = width - Math.round(width * (0.06F + run * 0.09F));
            int wide = Math.round(width * (0.13F - run * 0.03F));
            int down = Math.round(reach * (1.0F - run * 0.22F));
            for (int y = 0; y < down; y++) {
                float along = y / (float) Math.max(1, down);
                int narrow = Math.round(wide * (1.0F - along * 0.65F));
                int alpha = Math.round(165.0F * wet * (1.0F - along * 0.55F));
                if (alpha <= 3 || narrow <= 0) {
                    continue;
                }
                graphics.fill(startX - narrow / 2, y, startX + narrow / 2, y + 1,
                        (alpha << 24) | (colour & 0xFFFFFF));
            }
        }
    }

    /** Wound right down when a body is put back together, so nothing lingers. */
    public static void reset() {
        close = 0.0F;
        wash = 0.0F;
        black = 0.0F;
        lids = 0.0F;
        blinking = 0;
        smear = 0.0F;
        surge = 0.0F;
        sting = 0.0F;
    }
}
