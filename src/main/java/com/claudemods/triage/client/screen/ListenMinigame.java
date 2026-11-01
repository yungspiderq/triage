package com.claudemods.triage.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Listening to a chest in the four places a chest is listened to.
 *
 * <p>Upper left, upper right, lower left, lower right - and the reason it is four rather
 * than one is the whole finding. A lung that has come away from the wall is quiet on its
 * own side and normal on the other, so one bell in one place answers nothing: what tells
 * you is the <em>difference</em> between two sides of the same person.
 *
 * <p>Like {@link FeelMinigame}, this is told nothing about the patient, and here it could
 * not be even if that were wanted. In realistic difficulty the chest this screen was handed
 * has already had its answer taken out of it on the server, so a game that drew what it
 * found would draw silence on a collapsed lung and then be contradicted by the message that
 * arrives a second later. So it draws the only thing it honestly has: the bell, and how
 * steady the hand on it is. Hold still and the trace is clean. Drag the bell about while
 * you listen and you are listening to your own hand.
 */
public final class ListenMinigame implements Minigame {
    /** The four places, as fractions of the chest box. */
    private static final float[][] SPOTS = {
            {0.30F, 0.30F}, {0.70F, 0.30F}, {0.30F, 0.66F}, {0.70F, 0.66F},
    };

    /** Ticks the bell has to stay down in one place for that place to count. */
    private static final int HOLD = 22;

    /** How far the bell may wander while listening before the trace is worthless. */
    private static final double WANDER = 3.0;

    private static final int SKIN = 0xFFC89571;
    private static final int SKIN_DARK = 0xFF9A6A4C;
    private static final int RIBS = 0xFFB3835F;
    private static final int SPOT = 0xFF6E6A61;
    private static final int SPOT_DONE = 0xFF7FD46B;
    private static final int SPOT_POOR = 0xFFD8C36A;
    private static final int BELL = 0xFFB9C0C6;
    private static final int BELL_DARK = 0xFF6E767C;
    private static final int TUBE = 0xFF3A362F;
    private static final int TRACE = 0xFFEFE7D6;
    private static final int TRACE_BACK = 0xFF2B2822;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final boolean self;

    /** How well each place was listened to, -1 until it has been. */
    private final float[] heard = new float[SPOTS.length];

    /** The trace, as a ring of samples. Drawn from the hand, not from the patient. */
    private final float[] wave = new float[64];
    private int waveAt;

    private int listening = -1;
    private int held;
    private double downX;
    private double downY;
    private double wander;
    private float breath;

    public ListenMinigame(int left, int top, int width, int height, boolean self) {
        this.left = left;
        this.top = top;
        this.width = Math.max(110, width);
        this.height = Math.max(70, height);
        this.self = self;
        for (int i = 0; i < heard.length; i++) {
            heard[i] = -1.0F;
        }
    }

    private int chestLeft() {
        return left + 8;
    }

    private int chestWidth() {
        return width - 70;
    }

    private int spotX(int index) {
        return chestLeft() + (int) (chestWidth() * SPOTS[index][0]);
    }

    private int spotY(int index) {
        return top + 8 + (int) ((height - 16) * SPOTS[index][1]);
    }

    private int nearest(double mouseX, double mouseY) {
        for (int i = 0; i < SPOTS.length; i++) {
            if (heard[i] < 0.0F && Math.abs(mouseX - spotX(i)) <= 7 && Math.abs(mouseY - spotY(i)) <= 7) {
                return i;
            }
        }
        return -1;
    }

    // ---- listening ---------------------------------------------------------

    @Override
    public void tick() {
        breath += 0.28F;
        if (listening >= 0) {
            held++;
            // The trace. A steady hand writes the room; a wandering one writes itself,
            // which is what a stethoscope actually does when it slides on skin.
            float noise = (float) Math.min(1.0, wander / WANDER);
            float clean = Mth.sin(breath) * 0.55F;
            float scratch = (float) ((Math.random() - 0.5) * 2.0) * noise;
            wave[waveAt] = clean * (1.0F - noise) + scratch;
            waveAt = (waveAt + 1) % wave.length;
            if (held >= HOLD) {
                finish();
            }
        }
    }

    private void finish() {
        if (listening < 0) {
            return;
        }
        heard[listening] = (float) Math.max(0.0, 1.0 - wander / WANDER);
        listening = -1;
        held = 0;
        wander = 0.0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || listening >= 0) {
            return false;
        }
        int at = nearest(mouseX, mouseY);
        if (at < 0) {
            return false;
        }
        listening = at;
        held = 0;
        wander = 0.0;
        downX = mouseX;
        downY = mouseY;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (listening < 0) {
            return false;
        }
        wander = Math.max(wander, Math.hypot(mouseX - downX, mouseY - downY));
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (listening >= 0 && held < HOLD) {
            // Lifted early. That place counts for what it was worth, which is not much.
            heard[listening] = held / (float) HOLD * 0.4F;
            listening = -1;
            held = 0;
            wander = 0.0;
        }
    }

    // ---- what the screen asks for ------------------------------------------

    private int done() {
        int count = 0;
        for (float one : heard) {
            if (one >= 0.0F) {
                count++;
            }
        }
        return count;
    }

    @Override
    public float progress() {
        return done() / (float) SPOTS.length;
    }

    @Override
    public float quality() {
        float sum = 0.0F;
        for (float one : heard) {
            sum += Math.max(0.0F, one);
        }
        return Mth.clamp(sum / SPOTS.length, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return done() >= SPOTS.length;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        return Component.translatable(self ? "triage.game.listen_self" : "triage.game.listen");
    }

    // ---- drawing -----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int chestLeft = chestLeft();
        int chestWidth = chestWidth();
        int chestTop = top + 8;
        int chestHeight = height - 16;

        // A chest, front on, with the ribs suggested rather than drawn. The whole box
        // rises and falls, because the one thing every chest on this screen is doing is
        // breathing, and a still drawing of one would be a drawing of a corpse.
        int rise = (int) (Mth.sin(breath) * 1.6F);
        graphics.fill(chestLeft, chestTop - rise, chestLeft + chestWidth, chestTop + chestHeight, SKIN);
        graphics.fill(chestLeft, chestTop + chestHeight - 3, chestLeft + chestWidth,
                chestTop + chestHeight, SKIN_DARK);
        for (int i = 1; i <= 4; i++) {
            int y = chestTop + chestHeight * i / 5 - rise;
            graphics.fill(chestLeft + 5, y, chestLeft + chestWidth - 5, y + 1, RIBS);
        }

        // The four places and how each went.
        for (int i = 0; i < SPOTS.length; i++) {
            int colour = heard[i] < 0.0F ? SPOT : heard[i] >= 0.7F ? SPOT_DONE : SPOT_POOR;
            int x = spotX(i);
            int y = spotY(i) - rise;
            graphics.fill(x - 3, y - 3, x + 3, y + 3, colour);
        }

        // The bell, on the place being listened to.
        if (listening >= 0) {
            int x = spotX(listening);
            int y = spotY(listening) - rise;
            graphics.fill(x - 5, y - 5, x + 5, y + 5, BELL);
            graphics.fill(x - 5, y + 3, x + 5, y + 5, BELL_DARK);
            graphics.fill(x - 1, y - 14, x + 1, y - 5, TUBE);
        }

        // And what is coming down the tube.
        int traceLeft = left + width - 58;
        int traceTop = top + height / 2 - 16;
        graphics.fill(traceLeft, traceTop, traceLeft + 52, traceTop + 32, TRACE_BACK);
        for (int i = 0; i < wave.length; i++) {
            int sample = (waveAt + i) % wave.length;
            int x = traceLeft + 2 + i * 48 / wave.length;
            int y = traceTop + 16 - (int) (wave[sample] * 13.0F);
            graphics.fill(x, y, x + 1, y + 1, TRACE);
        }
    }
}
