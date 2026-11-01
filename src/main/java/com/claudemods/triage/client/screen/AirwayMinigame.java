package com.claudemods.triage.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * An airway: look in it, then open it, then check it is open.
 *
 * <p>What was here was the middle third - tilt the head to an angle and hold it against a
 * sag. That part was right and it is still here. What it was missing is the two halves
 * that make it a procedure rather than a knob.
 *
 * <p><b>Look first.</b> Tilting the head back on a mouth with something in it drives the
 * something further in. If there is anything visible, it comes out first, and only what
 * is visible - a finger going in blind after something it cannot see is how an obstruction
 * that was in the mouth ends up in the throat.
 *
 * <p><b>Then the tilt</b>, held, because a jaw is heavy and an unconscious tongue is what
 * is actually in the way. The sag is not a difficulty invented for the game: it is the
 * head coming forward again the moment nobody is holding it.
 *
 * <p><b>Then check.</b> Look, listen, feel - and the check here is the same as the check
 * there: watch for the chest to move. Which is the only thing in this whole screen that
 * tells you it worked.
 */
public final class AirwayMinigame implements Minigame {
    /** Where the head has to be held, as a share of the swing, and how much is close enough. */
    private static final float TILT_TARGET = 0.72F;
    private static final float TILT_BAND = 0.13F;
    /** How far the hand travels for the full swing. */
    private static final float TILT_TRAVEL = 58.0F;
    /** How fast the head comes forward again with nobody holding it. */
    private static final float SAG = 0.022F;

    /** How long it has to be held open before a breath comes. */
    private static final int HOLD_TICKS = 40;
    /** How far a sweep has to travel to take what is in there out. */
    private static final float SWEEP_NEEDED = 34.0F;

    private static final int COLOR_HEAD = 0xFFB07A5A;
    private static final int COLOR_HEAD_EDGE = 0xFF6E4632;
    private static final int COLOR_NECK = 0xFF9A6A4E;
    private static final int COLOR_AIRWAY = 0xFF3A1614;
    private static final int COLOR_OPEN = 0x90BFE4E8;
    private static final int COLOR_TONGUE = 0xFF8E4A4A;
    private static final int COLOR_BLOCK = 0xFF4E6B36;
    private static final int COLOR_BLOCK_EDGE = 0xFF33471F;
    private static final int COLOR_CHEST = 0xFFC08A6A;
    private static final int COLOR_BAND = 0x44FFE070;
    private static final int COLOR_BAND_EDGE = 0x99FFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;
    private static final int COLOR_EMPTY = 0x40FFFFFF;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int headX;
    private final int headY;
    private final int chestX;

    /** Something in the mouth, or nothing. Half of them have something. */
    private boolean blocked;
    private float blockX;
    private float blockY;
    private float swept;
    private float lastSweepX;
    private boolean sweeping;

    /** 0 chin on chest, 1 head right back. */
    private float tilt;
    private boolean held;
    private float grabY;
    private float grabTilt;
    private int holding;
    private int age;
    private int flash;

    public AirwayMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        this.left = areaLeft;
        this.top = areaTop;
        this.width = areaWidth;
        this.height = areaHeight;
        this.headX = areaLeft + Math.round(areaWidth * 0.33F);
        this.headY = areaTop + Math.round(areaHeight * 0.44F);
        this.chestX = areaLeft + Math.round(areaWidth * 0.76F);

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        this.blocked = random.nextInt(10) < 5;
        this.blockX = headX + 10 + random.nextInt(9);
        this.blockY = headY + 4 + random.nextInt(7);
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public void tick() {
        age++;
        if (flash > 0) {
            flash--;
        }
        if (complete() || blocked) {
            return;
        }
        if (!held) {
            // A head that nobody is holding comes forward, and the tongue with it.
            tilt = Math.max(0.0F, tilt - SAG);
        }
        if (open()) {
            holding++;
        } else if (holding > 0 && tilt < TILT_TARGET - TILT_BAND) {
            holding = Math.max(0, holding - 2);
        }
    }

    private boolean open() {
        return !blocked && Math.abs(tilt - TILT_TARGET) <= TILT_BAND;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        if (blocked) {
            sweeping = true;
            lastSweepX = (float) mouseX;
            return true;
        }
        held = true;
        grabY = (float) mouseY;
        grabTilt = tilt;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        if (blocked) {
            sweep(mouseX, mouseY);
            return true;
        }
        if (!held) {
            return false;
        }
        // Up tips the head back, which is the direction a head goes.
        tilt = Mth.clamp(grabTilt + (grabY - (float) mouseY) / TILT_TRAVEL, 0.0F, 1.0F);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            held = false;
            sweeping = false;
            swept = 0.0F;
        }
    }

    /**
     * A finger sweep, and only across what can be seen.
     *
     * <p>Sideways, hooking it out, rather than in and down: the whole warning about this
     * is that a finger going straight in pushes whatever is there past the point anybody
     * can reach it.
     */
    private void sweep(double mouseX, double mouseY) {
        if (!sweeping) {
            sweeping = true;
            lastSweepX = (float) mouseX;
            return;
        }
        float step = (float) mouseX - lastSweepX;
        lastSweepX = (float) mouseX;
        if (Math.abs(mouseX - blockX) > 16.0 || Math.abs(mouseY - blockY) > 13.0) {
            return;
        }
        if (step < 0.0F) {
            // Hooking it outwards, which is towards the front of the mouth.
            swept += -step;
            blockX = Math.max(headX - 8.0F, blockX + step * 0.6F);
            if (swept >= SWEEP_NEEDED) {
                blocked = false;
                play(SoundEvents.ITEM_PICKUP, 0.9F);
            }
            return;
        }
        // Pushing it further in, which is the one thing everybody is told not to do.
        if (flash == 0) {
            flash = 16;
            swept = 0.0F;
            blockX = Math.min(headX + 26.0F, blockX + step * 0.8F);
            play(SoundEvents.SLIME_SQUISH, 0.7F);
        }
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        if (blocked) {
            return Mth.clamp(0.25F * (swept / SWEEP_NEEDED), 0.0F, 0.25F);
        }
        return Mth.clamp(0.25F + 0.75F * holding / HOLD_TICKS, 0.25F, 1.0F);
    }

    @Override
    public boolean complete() {
        return !blocked && holding >= HOLD_TICKS;
    }

    /** Hands do not run out. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        if (blocked) {
            return Component.translatable(flash > 0
                    ? "triage.minigame.airway_deeper" : "triage.minigame.airway_sweep");
        }
        if (tilt < TILT_TARGET - TILT_BAND) {
            return Component.translatable("triage.minigame.airway_more");
        }
        if (tilt > TILT_TARGET + TILT_BAND) {
            return Component.translatable("triage.minigame.airway_far");
        }
        return Component.translatable("triage.minigame.airway_hold");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        chest(graphics);
        head(graphics);
        gauge(graphics);
    }

    /**
     * The chest, which is the check.
     *
     * <p>It only moves while the airway is open, and it is the one thing on this screen
     * that says the work is working. Look, listen, feel - this is the looking.
     */
    private void chest(GuiGraphics graphics) {
        float breath = open() ? (Mth.sin(age * 0.18F) * 0.5F + 0.5F) : 0.0F;
        int rise = Math.round(breath * 5.0F);
        graphics.fill(chestX - 26, headY + 6 - rise, chestX + 26, headY + 46, COLOR_HEAD_EDGE);
        Draw.body(graphics, chestX - 25, headY + 7 - rise, 50, 38, COLOR_CHEST);
        if (open()) {
            for (int i = 0; i < 3; i++) {
                int x = headX - 14 - ((age * 2 + i * 11) % 30);
                Draw.disc(graphics, x, headY + 2 - i, 2, COLOR_OPEN);
            }
        }
    }

    /** The head in profile, tipped as far as it is being held. */
    private void head(GuiGraphics graphics) {
        int lift = Math.round(tilt * 16.0F);
        int chin = Math.round(tilt * 10.0F);

        // The neck, and the airway down it: open when the head is back, folded when not.
        graphics.fill(headX + 14, headY - 2, chestX - 24, headY + 16, COLOR_NECK);
        int channel = Math.round(3.0F + tilt * 6.0F);
        graphics.fill(headX + 14, headY + 7 - channel / 2, chestX - 24, headY + 7 + channel / 2,
                open() ? COLOR_OPEN : COLOR_AIRWAY);

        // The head itself.
        graphics.fill(headX - 19, headY - 21 - lift, headX + 17, headY + 11 - lift / 2, COLOR_HEAD_EDGE);
        Draw.body(graphics, headX - 18, headY - 20 - lift, 34, 30, COLOR_HEAD);
        // The jaw, lifted with it.
        graphics.fill(headX - 4, headY + 2 - lift / 2 + chin, headX + 18, headY + 11 - lift / 2 + chin,
                COLOR_HEAD);
        // The tongue, which is what is in the way when the head is forward.
        int tongue = Math.round((1.0F - tilt) * 9.0F);
        graphics.fill(headX + 8, headY + 3 - lift / 2, headX + 16 + tongue, headY + 8 - lift / 2,
                COLOR_TONGUE);

        if (blocked) {
            int x = Math.round(blockX);
            int y = Math.round(blockY) - lift / 2;
            Draw.disc(graphics, x, y, 5, COLOR_BLOCK_EDGE);
            Draw.disc(graphics, x, y, 4, COLOR_BLOCK);
            graphics.renderOutline(x - 7, y - 7, 14, 14, flash > 0 ? COLOR_BAD : COLOR_BAND_EDGE);
            // Which way it has to come.
            Draw.line(graphics, x - 8, y + 10, x - 22, y + 10, 1, COLOR_BAND_EDGE);
            Draw.line(graphics, x - 22, y + 10, x - 18, y + 7, 1, COLOR_BAND_EDGE);
            Draw.line(graphics, x - 22, y + 10, x - 18, y + 13, 1, COLOR_BAND_EDGE);
        }
    }

    /** How far back the head is, and how long it has stayed there. */
    private void gauge(GuiGraphics graphics) {
        if (blocked) {
            int barLeft = left + 10;
            int barTop = top + height - 9;
            int barWidth = width - 20;
            graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 4, COLOR_EMPTY);
            graphics.fill(barLeft, barTop, barLeft + Math.round(barWidth * (swept / SWEEP_NEEDED)),
                    barTop + 4, flash > 0 ? COLOR_BAD : COLOR_GOOD);
            return;
        }
        int barLeft = left + 8;
        int barTop = top + 12;
        int barHeight = height - 34;
        graphics.fill(barLeft, barTop, barLeft + 8, barTop + barHeight, 0x40000000);
        // Drawn the way the head moves: further back is higher up.
        int from = barTop + Math.round(barHeight * (1.0F - TILT_TARGET - TILT_BAND));
        int to = barTop + Math.round(barHeight * (1.0F - TILT_TARGET + TILT_BAND));
        graphics.fill(barLeft, from, barLeft + 8, to, COLOR_BAND);
        graphics.fill(barLeft, from, barLeft + 8, from + 1, COLOR_BAND_EDGE);
        graphics.fill(barLeft, to - 1, barLeft + 8, to, COLOR_BAND_EDGE);
        int at = barTop + Math.round(barHeight * (1.0F - tilt));
        graphics.fill(barLeft - 3, at - 1, barLeft + 11, at + 1, open() ? COLOR_GOOD : COLOR_BAD);

        int holdLeft = left + 22;
        int holdWidth = width - 32;
        int holdTop = top + height - 9;
        graphics.fill(holdLeft, holdTop, holdLeft + holdWidth, holdTop + 4, COLOR_EMPTY);
        graphics.fill(holdLeft, holdTop, holdLeft + Math.round(holdWidth * (holding / (float) HOLD_TICKS)),
                holdTop + 4, COLOR_GOOD);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
