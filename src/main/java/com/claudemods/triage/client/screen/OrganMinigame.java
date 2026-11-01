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
 * Sewing a tear shut on something that will not hold still.
 *
 * <p>The operation for what is torn inside, and the one thing that makes it different from
 * every other job in this mod is not the stitching. It is that <b>the patient is
 * breathing</b>, and everything under the ribs moves when they do.
 *
 * <p>So the skill is not aim. It is waiting. A chest rises, holds for a moment at the top
 * of the breath and at the bottom of it, and then moves again - and a needle put in while
 * the surface is travelling does not make a stitch, it makes the tear longer. Which is
 * exactly what it does here: a stitch placed on the move tears, and the tear it makes has
 * to be sewn as well.
 *
 * <p>Nothing on screen says when the pause is. The organ is drawn moving, and a player
 * learns the rhythm the way anybody does - by watching it for a second before touching
 * anything, which is the entire lesson.
 */
public final class OrganMinigame implements Minigame {
    /** How many stitches a tear this size takes. */
    private static final int STITCHES = 6;
    /** Ticks in a full breath, in and out. */
    private static final float BREATH = 62.0F;
    /** How slowly the surface has to be moving for a needle to take. */
    private static final float STILL = 0.22F;
    /** How close to the next stitch mark the needle has to be. */
    private static final float ON_MARK = 9.0F;
    /** How far the organ travels with the breathing. */
    private static final float TRAVEL = 13.0F;

    private static final int ORGAN = 0xFF8E2F3A;
    private static final int ORGAN_LIT = 0xFFB04452;
    private static final int ORGAN_EDGE = 0xFF5E1E26;
    private static final int TEAR = 0xFF3A0E12;
    private static final int BLOOD = 0xFF8E1616;
    private static final int THREAD = 0xFFE8E2D2;
    private static final int NEEDLE = 0xFFD8DEE4;
    private static final int GOOD = 0xFF7FD46B;
    private static final int BAD = 0xFFCC3333;
    private static final int EMPTY = 0x40FFFFFF;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int centreY;
    /** Where each stitch has to go, in the organ's own frame. */
    private final float[] markX = new float[STITCHES];
    private final float[] markY = new float[STITCHES];

    private int placed;
    private int torn;
    private int age;
    private int flash;
    private float lastRise;
    private float rise;

    public OrganMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = Math.max(80, width);
        this.height = Math.max(60, height);
        this.centreX = left + this.width / 2;
        this.centreY = top + this.height / 2;

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        // The tear: a line across the organ that wanders, so the marks are never a row
        // of dots anybody can click along without looking.
        float startX = centreX - this.width * 0.22F;
        float endX = centreX + this.width * 0.22F;
        float lean = (random.nextFloat() - 0.5F) * 26.0F;
        for (int i = 0; i < STITCHES; i++) {
            float t = i / (float) (STITCHES - 1);
            markX[i] = Mth.lerp(t, startX, endX);
            markY[i] = centreY + lean * (t - 0.5F) + Mth.sin(t * 5.0F) * 4.0F;
        }
    }

    // ---- the needle ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        float dx = (float) mouseX - markX[placed];
        float dy = (float) mouseY - (markY[placed] + rise);
        if (Mth.sqrt(dx * dx + dy * dy) > ON_MARK) {
            // Not on the mark at all. Nothing happens, which is the right price for a
            // needle that went nowhere near the tear.
            flash = 4;
            play(SoundEvents.ITEM_PICKUP, 0.6F);
            return true;
        }
        if (Math.abs(rise - lastRise) > STILL) {
            // On the mark, on the move. The needle goes in and the tear gets longer.
            torn++;
            flash = 8;
            play(SoundEvents.PLAYER_HURT, 1.4F);
            return true;
        }
        placed++;
        play(SoundEvents.ITEM_PICKUP, 1.5F);
        return true;
    }

    @Override
    public void tick() {
        age++;
        if (flash > 0) {
            flash--;
        }
        lastRise = rise;
        // In, hold, out, hold. A sine would move fastest in the middle and never pause;
        // what a chest does is pause at both ends, and the pause is the whole game.
        float phase = (age % BREATH) / BREATH;
        rise = TRAVEL * (float) Math.sin(phase * Mth.TWO_PI);
        rise += TRAVEL * 0.35F * (float) Math.sin(phase * Mth.TWO_PI * 2.0F);
    }

    // ---- where it stands ----------------------------------------------------

    @Override
    public float progress() {
        return placed / (float) STITCHES;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - torn * 0.18F, 0.15F, 1.0F);
    }

    @Override
    public boolean complete() {
        return placed >= STITCHES;
    }

    /** It cannot be failed. What a bad one costs is what the patient paid for it. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        if (complete()) {
            return Component.translatable("triage.minigame.organ_done");
        }
        boolean still = Math.abs(rise - lastRise) <= STILL;
        return Component.translatable(still
                ? "triage.minigame.organ_now" : "triage.minigame.organ_wait");
    }

    // ---- drawing ------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        organ(graphics);
        tear(graphics);
        needle(graphics, mouseX, mouseY);
        bar(graphics);
    }

    private void organ(GuiGraphics graphics) {
        int shift = Math.round(rise);
        int halfW = Math.min(width / 2 - 10, 78);
        int halfH = Math.min(height / 2 - 10, 42);
        for (int y = -halfH; y <= halfH; y++) {
            float t = y / (float) halfH;
            int span = Math.round(halfW * Mth.sqrt(Math.max(0.0F, 1.0F - t * t)));
            int yy = centreY + y + shift;
            graphics.fill(centreX - span, yy, centreX + span, yy + 1,
                    y < -halfH / 3 ? ORGAN_LIT : ORGAN);
            graphics.fill(centreX - span, yy, centreX - span + 2, yy + 1, ORGAN_EDGE);
            graphics.fill(centreX + span - 2, yy, centreX + span, yy + 1, ORGAN_EDGE);
        }
    }

    /** The tear, the stitches that are in, and the blood from the ones that were not. */
    private void tear(GuiGraphics graphics) {
        int shift = Math.round(rise);
        for (int i = 0; i < STITCHES - 1; i++) {
            Draw.line(graphics, Math.round(markX[i]), Math.round(markY[i]) + shift,
                    Math.round(markX[i + 1]), Math.round(markY[i + 1]) + shift,
                    i < placed ? 2 : 4, i < placed ? ORGAN : TEAR);
        }
        for (int i = 0; i < STITCHES; i++) {
            int x = Math.round(markX[i]);
            int y = Math.round(markY[i]) + shift;
            if (i < placed) {
                // A stitch: two short strokes across the line, which is what one looks
                // like and is not the same picture as a dot.
                graphics.fill(x - 4, y - 3, x - 2, y + 4, THREAD);
                graphics.fill(x + 2, y - 4, x + 4, y + 3, THREAD);
            } else if (i == placed) {
                Draw.ring(graphics, x, y, ON_MARK, 14, 1,
                        Math.abs(rise - lastRise) <= STILL ? 0x997FD46B : 0x55FFFFFF);
            }
        }
        for (int i = 0; i < torn; i++) {
            Draw.disc(graphics, Math.round(markX[Math.min(placed, STITCHES - 1)]) + 6 + i * 5,
                    Math.round(markY[Math.min(placed, STITCHES - 1)]) + shift + 8, 3, BLOOD);
        }
    }

    private void needle(GuiGraphics graphics, int mouseX, int mouseY) {
        if (complete()) {
            return;
        }
        int colour = flash > 0 ? BAD : NEEDLE;
        graphics.fill(mouseX - 1, mouseY - 9, mouseX + 1, mouseY + 1, colour);
        graphics.fill(mouseX - 3, mouseY - 11, mouseX + 3, mouseY - 9, colour);
    }

    /** How many are in, and how many tears were added getting them there. */
    private void bar(GuiGraphics graphics) {
        int barLeft = left + 8;
        int barWidth = width - 16;
        int barTop = top + height - 8;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 4, EMPTY);
        graphics.fill(barLeft, barTop, barLeft + Math.round(barWidth * progress()), barTop + 4, GOOD);
        for (int i = 0; i < torn; i++) {
            graphics.fill(barLeft + i * 6, top + 4, barLeft + i * 6 + 4, top + 8, BAD);
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F), 0.4F));
    }
}
