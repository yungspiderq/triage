package com.claudemods.triage.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Feeling along a bone for the break in it.
 *
 * <p>Three places, one press each, and the whole of the skill is how hard. Too light and
 * the hand is on skin rather than on bone and learns nothing; too hard and the patient
 * comes off the floor - and on a fracture that is still fresh, a press like that moves it.
 * Between the two there is a band, and finding it three times running is what an
 * examination is.
 *
 * <p><b>Nothing in here knows what is under the hand.</b> That is deliberate and it is the
 * one place this mod's minigames differ from each other: the burn one is handed the depth
 * because it has to draw a flinch that only a deep burn gives, and in realistic difficulty
 * that would be the answer arriving on the client before the player earned it. So this one
 * is told nothing at all. Everything it draws is a reaction to the player's own hand, the
 * score it sends back is a number about pressure, and the finding comes back from the
 * server afterwards like a pulse or a temperature does. A modified client learns the shape
 * of its own press.
 */
public final class FeelMinigame implements Minigame {
    /** Three presses: one up the limb, one across the middle, one down. */
    private static final int STATIONS = 3;

    /** How long a held press takes to run from nothing to the top of the gauge. */
    private static final float RISE_PER_TICK = 0.055F;

    /** The band that tells you something without costing more than it is worth. */
    private static final float BAND_LOW = 0.52F;
    private static final float BAND_HIGH = 0.78F;

    private static final int SKIN = 0xFFC89571;
    private static final int SKIN_DARK = 0xFF9A6A4C;
    private static final int BONE = 0xFFE8DCC0;
    private static final int BONE_EDGE = 0xFFB3A784;
    private static final int MARK = 0xFF6E6A61;
    private static final int MARK_DONE = 0xFF7FD46B;
    private static final int MARK_LIGHT = 0xFFD8C36A;
    private static final int MARK_HARD = 0xFFCC3333;
    private static final int GAUGE_BACK = 0xFF3A362F;
    private static final int GAUGE_BAND = 0x807FD46B;
    private static final int GAUGE_NEEDLE = 0xFFEFE7D6;
    private static final int FINGER = 0xFFD9A886;
    private static final int FINGER_DARK = 0xFF9C6E51;

    private final int left;
    private final int top;
    private final int width;
    private final int height;

    /** Whether these are your own hands on your own limb, which changes only the wording. */
    private final boolean self;

    /** How each station went, 0 to 1. Unpressed stations stay at -1. */
    private final float[] scored = new float[STATIONS];

    private int station;
    private boolean down;
    private float pressure;
    private float wince;
    private float lastWince;
    /** Ticks left of the mark the last press left behind: green, yellow or red. */
    private int flash;
    private int flashColour;

    public FeelMinigame(int left, int top, int width, int height, boolean self) {
        this.left = left;
        this.top = top;
        this.width = Math.max(100, width);
        this.height = Math.max(60, height);
        this.self = self;
        for (int i = 0; i < STATIONS; i++) {
            scored[i] = -1.0F;
        }
    }

    // ---- where things are --------------------------------------------------

    private int limbTop() {
        return top + height / 2 - 9;
    }

    private int limbLeft() {
        return left + 10;
    }

    private int limbWidth() {
        return width - 46;
    }

    private int stationX(int index) {
        return limbLeft() + limbWidth() * (index * 2 + 1) / (STATIONS * 2);
    }

    private int gaugeLeft() {
        return left + width - 26;
    }

    // ---- the press ---------------------------------------------------------

    @Override
    public void tick() {
        lastWince = wince;
        if (down && station < STATIONS) {
            // Pressure keeps climbing while the hand stays down. There is no ceiling on
            // purpose: a player who holds on runs off the top of the band and finds out
            // what that costs, which is the only way this game teaches anything.
            pressure = Math.min(1.35F, pressure + RISE_PER_TICK);
        } else {
            pressure = Math.max(0.0F, pressure - RISE_PER_TICK * 2.2F);
        }
        // What the patient does about it. Nothing here consults the body: a person being
        // pressed on tenses in proportion to the press, and that is all this is.
        float want = Math.max(0.0F, pressure - 0.25F) * 1.25F;
        wince += (want - wince) * 0.35F;
        if (flash > 0) {
            flash--;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || station >= STATIONS) {
            return false;
        }
        down = true;
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0 || !down || station >= STATIONS) {
            return;
        }
        down = false;
        scored[station] = score(pressure);
        flashColour = pressure > BAND_HIGH ? MARK_HARD
                : pressure < BAND_LOW ? MARK_LIGHT : MARK_DONE;
        flash = 10;
        station++;
    }

    /**
     * What one press was worth.
     *
     * <p>Inside the band, a press is worth most at the middle of it and tails off toward
     * either edge - there is no cliff, because there is no cliff in a pair of hands. Under
     * it, almost nothing: a hand on skin. Over it, nothing at all, and the cost of that
     * press is paid by the patient rather than by this number.
     */
    private static float score(float at) {
        if (at > BAND_HIGH) {
            return 0.0F;
        }
        if (at < BAND_LOW) {
            return Math.max(0.0F, at / BAND_LOW) * 0.2F;
        }
        float middle = (BAND_LOW + BAND_HIGH) * 0.5F;
        float half = (BAND_HIGH - BAND_LOW) * 0.5F;
        return 1.0F - Math.abs(at - middle) / half * 0.35F;
    }

    // ---- what the screen asks for and reports ------------------------------

    @Override
    public float progress() {
        return station / (float) STATIONS;
    }

    @Override
    public float quality() {
        float sum = 0.0F;
        for (float one : scored) {
            sum += Math.max(0.0F, one);
        }
        return Mth.clamp(sum / STATIONS, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return station >= STATIONS;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        return Component.translatable(self ? "triage.game.feel_self" : "triage.game.feel");
    }

    // ---- drawing -----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int limbLeft = limbLeft();
        int limbWidth = limbWidth();
        int limbTop = limbTop();
        float shown = Mth.lerp(partialTick, lastWince, wince);

        // The limb, and the bone down the middle of it. The whole limb shifts a pixel or
        // two under a hard press, which is the only thing on this screen that says the
        // examination is happening to somebody.
        int shift = (int) (shown * 2.2F);
        graphics.fill(limbLeft, limbTop + shift, limbLeft + limbWidth, limbTop + 18 + shift, SKIN);
        graphics.fill(limbLeft, limbTop + 16 + shift, limbLeft + limbWidth, limbTop + 18 + shift, SKIN_DARK);
        graphics.fill(limbLeft + 3, limbTop + 7 + shift, limbLeft + limbWidth - 3, limbTop + 11 + shift, BONE);
        graphics.fill(limbLeft + 3, limbTop + 10 + shift, limbLeft + limbWidth - 3, limbTop + 11 + shift, BONE_EDGE);

        // The three places, and what each press left behind.
        for (int i = 0; i < STATIONS; i++) {
            int x = stationX(i);
            int colour = scored[i] < 0.0F ? MARK
                    : scored[i] >= 0.8F ? MARK_DONE
                    : scored[i] > 0.0F ? MARK_LIGHT : MARK_HARD;
            if (i == station && flash > 0) {
                colour = flashColour;
            }
            graphics.fill(x - 3, limbTop - 4 + shift, x + 3, limbTop - 2 + shift, colour);
        }

        // The hand, at the station it is working on.
        if (station < STATIONS) {
            int x = stationX(station);
            int y = limbTop - 14 + (int) (shown * 3.0F) + (down ? (int) (pressure * 6.0F) : 0);
            graphics.fill(x - 4, y, x + 4, y + 10, FINGER);
            graphics.fill(x - 4, y + 8, x + 4, y + 10, FINGER_DARK);
        }

        // The gauge: how hard, and the band worth being in.
        int gaugeLeft = gaugeLeft();
        int gaugeTop = top + 6;
        int gaugeHeight = height - 12;
        graphics.fill(gaugeLeft, gaugeTop, gaugeLeft + 8, gaugeTop + gaugeHeight, GAUGE_BACK);
        int bandTop = gaugeTop + (int) ((1.0F - BAND_HIGH) * gaugeHeight);
        int bandBottom = gaugeTop + (int) ((1.0F - BAND_LOW) * gaugeHeight);
        graphics.fill(gaugeLeft, bandTop, gaugeLeft + 8, bandBottom, GAUGE_BAND);
        int needle = gaugeTop + (int) ((1.0F - Math.min(1.0F, pressure)) * gaugeHeight);
        graphics.fill(gaugeLeft - 2, needle - 1, gaugeLeft + 10, needle + 1, GAUGE_NEEDLE);
    }
}
