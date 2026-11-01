package com.claudemods.triage.client.screen;

import com.claudemods.triage.body.Pulse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Two fingers on a wrist, and wait.
 *
 * <p>Every other game in this mod is a thing done to a patient. This one is a thing
 * <em>read off</em> one, and it is the only game here whose reward is information rather
 * than a repair - so it is built the other way round from all of them. Nothing improves
 * because it went well. What goes well is that you come away knowing the number, and what
 * goes badly is that you come away knowing only "fast".
 *
 * <p>There is no target to hit and no bar to stop. There is a place on the wrist, and
 * beats arriving at the rate the patient's heart is actually running at - the same
 * number the server will read - and the whole job is to hold still on the spot while
 * enough of them go past. Fingers resting off the artery find nothing, and fingers resting
 * too lightly on it find nothing either: a thin pulse has to be pressed for.
 *
 * <p>Which means a player who does this properly hears the tachycardia before they are
 * told about it. That is the entire point: the sound arrives at the rate, and a racing
 * heart sounds like one.
 */
public final class PulseMinigame implements Minigame {
    /** How many beats have to go past under the fingers for a countable reading. */
    private static final int BEATS_WANTED = 6;
    /** How far off the spot the fingers can be and still find anything. */
    private static final float REACH = 9.0F;
    /** How long a lost pulse takes to give up on. */
    private static final int PATIENCE = 70;
    /** How fast the fingers bear down, and how fast they come off. */
    private static final float PRESS_ON = 0.022F;
    private static final float PRESS_OFF = 0.035F;

    private final int left;
    private final int top;
    private final int width;
    private final int height;

    /** The heart being read, which is the patient's real one. */
    private final Pulse pulse;
    /** Ticks between beats, from that rate. */
    private final float period;
    /**
     * Whether the helper's own ears are gone.
     *
     * <p>Counting a pulse by ear is most of what this game is. With the sound taken away
     * it can still be done - the spike on the trace is a beat too - but it has to be
     * watched instead of heard, and that is harder, which is the correct amount harder.
     */
    private final boolean deaf;

    private static final int SKIN = 0xFFC89571;
    private static final int SKIN_LIT = 0xFFDCAA85;
    private static final int SKIN_DARK = 0xFF9A6A4C;
    private static final int SKIN_LINE = 0xFF83573D;
    private static final int PALM = 0xFFD2A07E;
    private static final int CREASE = 0xFFA87759;
    private static final int FINGER = 0xFFD9A886;
    private static final int FINGER_DARK = 0xFF9C6E51;
    private static final int NAIL = 0xFFEBC9B4;
    private static final int PRESS = 0xFF8E6046;
    private static final int BEAT = 0xFFCC423A;

    private final int spotX;
    private final int spotY;

    /**
     * How hard the fingers are bearing down, 0 to 1.
     *
     * <p>The game used to ask one question - are the fingers in the right place - and a
     * wrist asks two. Resting them on somebody does not find an artery; you have to mean
     * it. So this builds while the button is down and falls off when it is not, and the
     * reading only runs while it is deep enough.
     *
     * <p>How deep is enough depends on the pulse. A thready one has to be pressed for,
     * which is the reason a weak pulse is the hard one to count in life and the reason it
     * is the hard one here. There is no ceiling: see {@link #feeling()}.
     */
    private float press;
    private float held;
    private float sinceBeat;
    private int caught;
    private int missed;
    private int idle;
    private int flash;
    private boolean down;
    private boolean onSpot;
    private boolean finished;
    private final float[] trace = new float[64];
    private int traceHead;

    public PulseMinigame(int left, int top, int width, int height, Pulse pulse, boolean deaf) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.pulse = pulse;
        this.deaf = deaf;
        this.period = pulse.rate() <= 0 ? 0.0F : Math.max(2.0F, 1200.0F / pulse.rate());
        // The wrist sits left of centre, where the drawing puts it.
        this.spotX = left + width / 2 - 34;
        this.spotY = top + height / 2 + 4;
    }

    /** 1 for a pulse you can barely feel, 0 for one you cannot miss. */
    private float weak() {
        return pulse.character() <= Pulse.THREADY ? 1.0F : 0.0F;
    }

    private float bandLow() {
        return 0.22F + 0.18F * weak();
    }

    /**
     * Whether anything can be felt at this moment.
     *
     * <p>There was an upper bound here as well - press too hard and the vessel shuts, which
     * is true of a real wrist. It came out because it was not worth what it cost: the number
     * you are after is the same either way, and all the ceiling added was a second invisible
     * thing to get wrong while counting. A weak pulse still wants a firmer touch to find at
     * all, and that is the half of it worth keeping.
     */
    private boolean feeling() {
        return onSpot && press >= bandLow();
    }

    // ---- playing -----------------------------------------------------------

    @Override
    public void tick() {
        if (finished) {
            return;
        }
        if (flash > 0) {
            flash--;
        }
        // A stopped heart is found by finding nothing, and the only way to be sure of
        // nothing is to have waited. So the game still runs; it simply never beats.
        press = Mth.clamp(press + (down ? PRESS_ON : -PRESS_OFF), 0.0F, 1.0F);

        if (pulse.rate() <= 0) {
            // A stopped heart is found by finding nothing, and being sure of nothing means
            // having pressed properly for long enough to be sure.
            if (feeling()) {
                held++;
                if (held >= PATIENCE) {
                    finished = true;
                }
            }
            push(0.0F);
            return;
        }
        if (!feeling()) {
            idle++;
            push(0.0F);
            // Off the spot long enough and the beats going past are beats missed, which
            // is what makes holding still the skill rather than clicking in time.
            if (idle % Math.max(4, (int) period) == 0) {
                missed++;
            }
            if (idle > PATIENCE * 2) {
                finished = true;
            }
            return;
        }
        idle = 0;
        held++;
        sinceBeat++;
        if (sinceBeat >= period) {
            sinceBeat -= period;
            caught++;
            flash = 4;
            beat();
            if (caught >= BEATS_WANTED) {
                finished = true;
            }
        }
        push(flash > 0 ? 1.0F - flash / 5.0F : 0.0F);
    }

    /** The beat, at the rate. Hearing it is most of what this is for. */
    private void beat() {
        Minecraft client = Minecraft.getInstance();
        if (client.getSoundManager() == null) {
            return;
        }
        if (deaf) {
            return;
        }
        // Pitched up when it is thin, so a thready one sounds thin.
        float pitch = pulse.character() <= Pulse.THREADY ? 1.6F : 1.25F;
        client.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.NOTE_BLOCK_BASEDRUM.value(), pitch, 0.55F));
    }

    private void push(float value) {
        trace[traceHead] = value;
        traceHead = (traceHead + 1) % trace.length;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        down = true;
        aim(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        aim(mouseX, mouseY);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        down = false;
        onSpot = false;
    }

    private void aim(double mouseX, double mouseY) {
        float dx = (float) mouseX - spotX;
        float dy = (float) mouseY - spotY;
        onSpot = Math.sqrt(dx * dx + dy * dy) <= REACH;
    }

    // ---- what it was worth -------------------------------------------------

    @Override
    public float progress() {
        if (pulse.rate() <= 0) {
            return Mth.clamp(held / PATIENCE, 0.0F, 1.0F);
        }
        return Mth.clamp(caught / (float) BEATS_WANTED, 0.0F, 1.0F);
    }

    /**
     * How much of a reading this was.
     *
     * <p>Which the server turns into precision rather than into healing: a good hold
     * gives the number, a poor one gives the word. Nobody is ever made worse by a
     * clumsy hand on a wrist, and pretending otherwise to make the game feel like the
     * others would have been a lie about what taking a pulse is.
     */
    @Override
    public float quality() {
        if (pulse.rate() <= 0) {
            return Mth.clamp(held / PATIENCE, 0.0F, 1.0F);
        }
        float got = caught / (float) BEATS_WANTED;
        float lost = missed / (float) (BEATS_WANTED * 2);
        return Mth.clamp(got - lost * 0.5F, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return finished;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        if (finished) {
            return Component.translatable("triage.minigame.pulse_done");
        }
        if (feeling()) {
            return Component.translatable("triage.minigame.pulse_holding", caught, BEATS_WANTED);
        }
        // Which way they are wrong, because a rule nobody can see is a rule nobody learns.
        // It says nothing about where the artery is - finding that is the other half - only
        // what the fingers themselves are doing.
        if (onSpot && down) {
            return Component.translatable("triage.minigame.pulse_too_light");
        }
        return Component.translatable(deaf
                ? "triage.minigame.pulse_find_deaf" : "triage.minigame.pulse_find");
    }

    // ---- drawing -----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int midY = top + height / 2;
        limb(graphics, midY);

        // Where the fingers are: wherever the hand is, not a target to click. Nothing
        // marks the spot, because nothing marks it on a wrist either - the creases and
        // the edge of the bone are the landmarks, and they are drawn.
        fingers(graphics, Mth.clamp(mouseX, left + 12, left + width - 12),
                Mth.clamp(mouseY, top + 14, top + height - 24));

        // The fingers, wherever they are, so a player can see that they have come off.
        if (down) {
            graphics.fill(mouseX - 3, mouseY - 6, mouseX - 1, mouseY + 6,
                    onSpot ? Paper.INK : Paper.INK_FAINT);
            graphics.fill(mouseX + 1, mouseY - 6, mouseX + 3, mouseY + 6,
                    onSpot ? Paper.INK : Paper.INK_FAINT);
        }

        trace(graphics, midY);

        // The beats caught, as marks on the paper. Counting is the job, so the count is
        // the readout - and nothing here says the rate, because the rate is what the
        // player is supposed to be working out.
        int tickX = left + width - 12;
        for (int i = 0; i < BEATS_WANTED; i++) {
            int y = top + 10 + i * 7;
            graphics.fill(tickX, y, tickX + 8, y + 2, i < caught ? Paper.RED : Paper.INK_FAINT);
        }
    }

    /** An arm laid out with the wrist towards the reader, because that is where it goes. */
    /**
     * A forearm, a wrist and a hand, palm up.
     *
     * <p>Two rectangles is what this was, and two rectangles is not a wrist. It is drawn
     * in slices a few pixels wide rather than a pixel at a time, because a quad per pixel
     * is how the other games in here ended up running at thirteen frames.
     */
    private void limb(GuiGraphics graphics, int midY) {
        int wristX = spotX + 22;
        for (int x = left + 4; x < wristX; x += 3) {
            float t = (x - left - 4) / (float) Math.max(1, wristX - left - 4);
            int half = Math.round(26 - 6 * t);
            int x1 = Math.min(wristX, x + 3);
            graphics.fill(x, midY - half, x1, midY + half, SKIN);
            graphics.fill(x, midY - half, x1, midY - half + 2, SKIN_LINE);
            graphics.fill(x, midY + half - 2, x1, midY + half, SKIN_DARK);
            graphics.fill(x, midY - half + 2, x1, midY - half + 8, SKIN_LIT);
        }
        // The palm, wider than the wrist, with the pad at the thumb side.
        for (int x = wristX; x < wristX + 46 && x < left + width - 30; x += 3) {
            float t = (x - wristX) / 46.0F;
            int half = Math.round(20 + 8 * Mth.sin(t * 2.2F));
            int x1 = x + 3;
            graphics.fill(x, midY - half, x1, midY + half, PALM);
            graphics.fill(x, midY + half - 2, x1, midY + half, SKIN_DARK);
            if (t < 0.62F) {
                int pad = Math.round(11 * Mth.sin(t * 3.1F));
                graphics.fill(x, midY - 20 - pad, x1, midY - 16, PALM);
                graphics.fill(x, midY - 20 - pad, x1, midY - 19 - pad, SKIN_LINE);
            }
        }
        // Wrist creases: the landmark anybody actually uses to find the thing.
        for (int off : new int[]{-2, 4}) {
            for (int x = wristX - 12; x < wristX + 4; x += 2) {
                int y = midY + off + Math.round(Mth.sin((x - wristX) * 0.3F) * 1.5F);
                graphics.fill(x, y, x + 2, y + 1, CREASE);
            }
        }
        // Fingers off the end of the palm.
        for (int f = 0; f < 4; f++) {
            int y0 = midY - 15 + f * 10;
            int length = 30 - Math.abs(f - 1) * 4;
            int from = wristX + 44;
            for (int x = from; x < from + length && x < left + width - 2; x += 3) {
                float t = (x - from) / (float) length;
                int h = Math.round(4.6F - 1.2F * t);
                graphics.fill(x, y0 - h, x + 3, y0 + h, FINGER);
                graphics.fill(x, y0 + h - 1, x + 3, y0 + h, FINGER_DARK);
            }
            int nail = Math.min(left + width - 2, from + length - 5);
            graphics.fill(nail, y0 - 3, nail + 4, y0 + 3, NAIL);
        }
    }

    /** The two fingers doing the feeling, and how hard they are bearing down. */
    private void fingers(GuiGraphics graphics, int x, int y) {
        int patch = Math.round(5 + 5 * press);
        for (int side = -1; side <= 1; side += 2) {
            int cx = x + side * 7;
            // The dimple, which is the whole of how pressure is shown: press harder and
            // the skin gives more.
            for (int r = patch; r > 0; r--) {
                int alpha = Math.round(255 * (0.10F + 0.45F * press * (1.0F - r / (float) patch)));
                int colour = (alpha << 24) | (PRESS & 0xFFFFFF);
                int h = Math.max(1, Math.round(r * 0.8F));
                graphics.fill(cx - r, y - h, cx + r, y + h, colour);
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            int cx = x + side * 7;
            for (int i = 0; i < 40; i += 2) {
                float t = i / 39.0F;
                int h = Math.round(6.5F - 1.6F * (1.0F - t));
                int fx = cx + Math.round(side * i * 0.12F);
                int fy = y - i;
                if (fy < top) {
                    break;
                }
                graphics.fill(fx - h, fy, fx + h, fy + 2, i > 3 ? FINGER : FINGER_DARK);
                graphics.fill(fx - h, fy, fx - h + 1, fy + 2, FINGER_DARK);
                graphics.fill(fx + h - 1, fy, fx + h, fy + 2, FINGER_DARK);
            }
            graphics.fill(cx - 3, y - 12, cx + 4, y - 5, NAIL);
        }
        if (flash > 0) {
            // A beat arriving under the fingers.
            int r = patch + 3;
            int alpha = Math.round(255 * (flash / 5.0F));
            graphics.fill(x - r * 2, y - r, x + r * 2, y - r + 1, (alpha << 24) | (BEAT & 0xFFFFFF));
            graphics.fill(x - r * 2, y + r, x + r * 2, y + r + 1, (alpha << 24) | (BEAT & 0xFFFFFF));
        }
    }

    /**
     * The trace under it all.
     *
     * <p>Not a heart monitor. Nobody in this mod owns one, and drawing one would hand
     * the player the rate for free and make the fingers decoration. It only shows what
     * has actually been felt: a spike where a beat was caught, flat everywhere else.
     */
    private void trace(GuiGraphics graphics, int midY) {
        int baseY = midY + 34;
        int traceLeft = left + 8;
        int traceWidth = width - 24;
        Paper.rule(graphics, traceLeft, baseY, traceWidth);
        int step = Math.max(1, traceWidth / trace.length);
        for (int i = 0; i < trace.length; i++) {
            float value = trace[(traceHead + i) % trace.length];
            if (value <= 0.0F) {
                continue;
            }
            int x = traceLeft + i * step;
            int spike = Math.round(14 * value);
            graphics.fill(x, baseY - spike, x + step, baseY, Paper.RED);
        }
    }
}
