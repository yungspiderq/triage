package com.claudemods.triage.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Running a line: the roller clamp, the drip chamber, and holding the rate where it
 * should be.
 *
 * <p>Everything else in this mod is a thing done with the hands and then finished. A line
 * is not. It is a thing set up and then <em>watched</em>, and it is the only treatment
 * here where the work is keeping a number where you put it rather than getting it there
 * once - which makes it the one game that has any business being about patience.
 *
 * <p>The rate does not stay where it is set. A bag empties, a line settles, a vein
 * behaves the way veins do, and the drops drift. So the clamp gets nudged, and nudged
 * again, and the whole of it is watching the chamber and not the clock.
 *
 * <p>Drawing blood is the same equipment run backwards, so it is the same game with the
 * arrow reversed: the bag fills instead of emptying, and the band is where it always was.
 * Anything else would be pretending a different piece of kit exists.
 */
public final class LineMinigame implements Minigame {
    /** How many ticks in the band it takes to finish. */
    private static final int NEEDED = 100;
    /** Where the rate should be, and how much either way is close enough. */
    private static final float TARGET = 0.55F;
    private static final float BAND = 0.13F;

    /** How far the clamp can be dragged, top to bottom. */
    private static final float TRAVEL = 74.0F;
    /** How hard the rate wanders off on its own. */
    private static final float DRIFT = 0.0032F;

    private static final int COLOR_BAG = 0xFFCBD8DE;
    private static final int COLOR_BAG_EDGE = 0xFF8FA6B2;
    private static final int COLOR_SALINE = 0xC0BFE4E8;
    private static final int COLOR_BLOOD = 0xE08E1616;
    private static final int COLOR_TUBE = 0x60FFFFFF;
    private static final int COLOR_TUBE_EDGE = 0xFF7A8288;
    private static final int COLOR_CHAMBER = 0x40FFFFFF;
    private static final int COLOR_CLAMP = 0xFFB8BCC4;
    private static final int COLOR_CLAMP_GRIP = 0xFF6E757C;
    private static final int COLOR_BAND = 0x44FFE070;
    private static final int COLOR_BAND_EDGE = 0x99FFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;
    private static final int COLOR_FILL_BG = 0x50FFFFFF;

    private final boolean draw;
    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int lineX;
    private final int bagTop;
    private final int chamberTop;
    private final int chamberBottom;
    private final int clampTop;

    /** Where the clamp is, 0 shut and 1 wide, and what the drops are actually doing. */
    private float clamp = 0.5F;
    private float rate = 0.5F;
    private float wander;
    private int inBand;
    private boolean held;
    private int age;
    private int dropAt;

    public LineMinigame(int left, int top, int width, int height) {
        this(left, top, width, height, false);
    }

    /** The same kit, run the other way: out of somebody and into a bag. */
    public static LineMinigame forDraw(int left, int top, int width, int height) {
        return new LineMinigame(left, top, width, height, true);
    }

    private LineMinigame(int left, int top, int width, int height, boolean draw) {
        this.draw = draw;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.lineX = left + width / 2 - 26;
        this.bagTop = top + 8;
        this.chamberTop = top + 46;
        this.chamberBottom = top + 74;
        this.clampTop = top + 30;
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public void tick() {
        age++;
        if (complete()) {
            return;
        }
        // The line settles, the bag empties, the vein does what veins do. Slow enough to
        // be a nudge rather than a fight, and never still.
        wander += (Mth.sin(age * 0.031F) + Mth.sin(age * 0.017F + 1.7F)) * DRIFT;
        wander = Mth.clamp(wander, -0.22F, 0.22F);
        rate = Mth.clamp(clamp + wander, 0.0F, 1.0F);

        if (Math.abs(rate - TARGET) <= BAND) {
            inBand++;
        }
        // Drops fall at the rate, which is the readout: nobody needs a number when the
        // chamber is right there.
        if (rate > 0.02F && --dropAt <= 0) {
            dropAt = Math.max(2, Math.round(22.0F - 18.0F * rate));
            play(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.4F + rate * 0.4F);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        roll(mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        roll(mouseY);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            held = false;
        }
    }

    /** Up shuts it, down opens it, and that is the whole control. */
    private void roll(double mouseY) {
        clamp = Mth.clamp(((float) mouseY - clampTop) / TRAVEL, 0.0F, 1.0F);
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        return Mth.clamp(inBand / (float) NEEDED, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return inBand >= NEEDED;
    }

    /** A line is not abandoned. It is set right, and that takes as long as it takes. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        if (rate > TARGET + BAND) {
            return Component.translatable(draw ? "triage.minigame.line_fast_draw" : "triage.minigame.line_fast");
        }
        if (rate < TARGET - BAND) {
            return Component.translatable("triage.minigame.line_slow");
        }
        return Component.translatable("triage.minigame.line_hold");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        bag(graphics);
        tube(graphics);
        chamber(graphics);
        clamp(graphics);
        gauge(graphics);
    }

    private int fluid() {
        return draw ? COLOR_BLOOD : COLOR_SALINE;
    }

    /** The bag: emptying into somebody, or filling out of them. */
    private void bag(GuiGraphics graphics) {
        int bagBottom = bagTop + 30;
        graphics.fill(lineX - 19, bagTop - 1, lineX + 19, bagBottom + 1, COLOR_BAG_EDGE);
        graphics.fill(lineX - 18, bagTop, lineX + 18, bagBottom, COLOR_BAG);
        float share = draw ? progress() : 1.0F - progress() * 0.7F;
        int surface = Math.round(bagBottom - (bagBottom - bagTop - 2) * Mth.clamp(share, 0.05F, 1.0F));
        graphics.fill(lineX - 17, surface, lineX + 17, bagBottom - 1, fluid());
        graphics.fill(lineX - 17, surface, lineX + 17, surface + 1, Draw.shade(fluid(), 1.2F));
        // The hanger, so it reads as something on a stand rather than a floating pouch.
        graphics.fill(lineX - 1, bagTop - 8, lineX + 1, bagTop, COLOR_TUBE_EDGE);
    }

    private void tube(GuiGraphics graphics) {
        graphics.fill(lineX - 3, bagTop + 30, lineX + 3, top + height - 10, COLOR_TUBE_EDGE);
        graphics.fill(lineX - 2, bagTop + 30, lineX + 2, top + height - 10, COLOR_TUBE);
    }

    /**
     * The drip chamber: where the rate is actually read.
     *
     * <p>Drops rather than a number, because that is how one is read in life and because
     * a number would make this a slider with a target on it. Watching the chamber is the
     * activity.
     */
    private void chamber(GuiGraphics graphics) {
        graphics.fill(lineX - 8, chamberTop - 1, lineX + 8, chamberBottom + 1, COLOR_TUBE_EDGE);
        graphics.fill(lineX - 7, chamberTop, lineX + 7, chamberBottom, COLOR_CHAMBER);
        graphics.fill(lineX - 7, chamberBottom - 6, lineX + 7, chamberBottom, fluid());

        if (rate > 0.02F) {
            int period = Math.max(2, Math.round(22.0F - 18.0F * rate));
            int fall = chamberBottom - 8 - chamberTop;
            int at = chamberTop + 3 + (age % period) * fall / period;
            Draw.disc(graphics, lineX, at, 2, Draw.shade(fluid(), 1.15F));
        }
    }

    /** The roller clamp, and the band it has to sit in. */
    private void clamp(GuiGraphics graphics) {
        int bandTop = Math.round(clampTop + TRAVEL * (TARGET - BAND));
        int bandBottom = Math.round(clampTop + TRAVEL * (TARGET + BAND));
        graphics.fill(lineX + 10, bandTop, lineX + 34, bandBottom, COLOR_BAND);
        graphics.fill(lineX + 10, bandTop, lineX + 34, bandTop + 1, COLOR_BAND_EDGE);
        graphics.fill(lineX + 10, bandBottom - 1, lineX + 34, bandBottom, COLOR_BAND_EDGE);

        // Where the drops actually are, which is not where the clamp is: that gap is the
        // drift, and closing it is the game.
        int rateY = Math.round(clampTop + TRAVEL * rate);
        boolean good = Math.abs(rate - TARGET) <= BAND;
        graphics.fill(lineX + 10, rateY - 1, lineX + 34, rateY + 1, good ? COLOR_GOOD : COLOR_BAD);

        int clampY = Math.round(clampTop + TRAVEL * clamp);
        graphics.fill(lineX - 7, clampY - 5, lineX + 7, clampY + 5, COLOR_CLAMP);
        graphics.fill(lineX - 7, clampY - 5, lineX + 7, clampY - 4, Draw.shade(COLOR_CLAMP, 1.2F));
        for (int i = -3; i <= 3; i += 3) {
            graphics.fill(lineX - 6, clampY + i, lineX + 6, clampY + i + 1, COLOR_CLAMP_GRIP);
        }
        if (held) {
            graphics.renderOutline(lineX - 8, clampY - 6, 16, 12, good ? COLOR_GOOD : COLOR_BAND_EDGE);
        }
    }

    /** How much of the run is done, which only moves while the rate is right. */
    private void gauge(GuiGraphics graphics) {
        int barTop = top + height - 7;
        int barLeft = left + 12;
        int barWidth = width - 24;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 4, COLOR_FILL_BG);
        graphics.fill(barLeft, barTop, barLeft + Math.round(barWidth * progress()), barTop + 4, COLOR_GOOD);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
