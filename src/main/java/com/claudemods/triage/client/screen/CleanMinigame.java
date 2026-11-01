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
 * Cleaning a wound out: flushing it, and then wiping away from it and never towards it.
 *
 * <p>The game this replaces had the player scrubbing a wound side to side until the dirt
 * went. It was clear and it was two things wrong at once. Scrubbing a wound is the thing
 * you are specifically told not to do - it drives dirt in and tears up what is trying to
 * heal - and side to side is the direction that carries whatever is on the skin straight
 * back across the opening.
 *
 * <p>What actually cleans a wound is volume: flush it, and the dirt comes out with the
 * fluid. That is the first half. The second half is the oldest rule in wound care and the
 * one everybody breaks: <b>every stroke goes away from the wound</b>. A wipe towards it is
 * not a wipe, it is putting the dirt back, and here it does exactly that - the specks
 * move with your hand, so a hand coming inwards brings them with it.
 *
 * <p>Nothing can be failed. Wiping the wrong way costs what wiping the wrong way costs,
 * which is doing it again.
 */
public final class CleanMinigame implements Minigame {
    /** In the channel to start with, and already on the skin round it. */
    private static final int INSIDE = 5;
    private static final int OUTSIDE = 6;
    private static final int SPECKS = INSIDE + OUTSIDE;

    /** How long the flush has to run to wash the channel out. */
    private static final int FLUSH_TICKS = 46;
    /** How close the bottle has to be held to the wound to be flushing it. */
    private static final float OVER = 16.0F;

    /** How close the cloth has to pass to take a speck with it. */
    private static final float BRUSH = 13.0F;
    /** How far out a speck has to get before it is off the skin altogether. */
    private static final float OFF = 62.0F;

    private static final int COLOR_SKIN = 0xFFB07A5A;
    private static final int COLOR_SKIN_EDGE = 0xFF6E4632;
    private static final int COLOR_WOUND = 0xFF8E1616;
    private static final int COLOR_WOUND_DEEP = 0xFF4E0F0F;
    private static final int COLOR_CLEAN = 0xFFC98A6A;
    private static final int COLOR_DIRT = 0xFF4E6B36;
    private static final int COLOR_DIRT_EDGE = 0xFF33471F;
    private static final int COLOR_FLUID = 0xA0BFE4E8;
    private static final int COLOR_BOTTLE = 0xFFD8E4E0;
    private static final int COLOR_CLOTH = 0xFFF2ECE0;
    private static final int COLOR_MARK = 0xFFFFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int centreY;

    private final float[] speckX = new float[SPECKS];
    private final float[] speckY = new float[SPECKS];
    private final boolean[] gone = new boolean[SPECKS];
    private final int[] size = new int[SPECKS];
    private int inChannel = INSIDE;

    private int flushed;
    private boolean held;
    private float handX;
    private float handY;
    private float lastX;
    private float lastY;
    private boolean hasLast;
    private int wrongWay;
    private int age;

    public CleanMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.centreX = left + width / 2;
        this.centreY = top + height / 2;

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        for (int i = 0; i < SPECKS; i++) {
            size[i] = 2 + random.nextInt(2);
            if (i < INSIDE) {
                // Down in the channel: nothing on the skin will shift these.
                speckX[i] = centreX + random.nextInt(9) - 4;
                speckY[i] = centreY + random.nextInt(37) - 18;
            } else {
                float angle = random.nextFloat() * Mth.TWO_PI;
                float distance = 20.0F + random.nextFloat() * 16.0F;
                speckX[i] = centreX + Mth.cos(angle) * distance;
                speckY[i] = centreY + Mth.sin(angle) * distance * 0.9F;
            }
        }
        this.handX = centreX;
        this.handY = centreY;
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        held = true;
        handX = (float) mouseX;
        handY = (float) mouseY;
        hasLast = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        handX = (float) mouseX;
        handY = (float) mouseY;
        if (!flushing()) {
            wipe();
        }
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            held = false;
            hasLast = false;
        }
    }

    @Override
    public void tick() {
        age++;
        if (wrongWay > 0) {
            wrongWay--;
        }
        if (!flushing()) {
            return;
        }
        if (held && Math.abs(handX - centreX) <= OVER && Math.abs(handY - centreY) <= 30.0F) {
            flushed++;
            if (flushed % 9 == 0) {
                play(SoundEvents.BOTTLE_FILL, 1.3F);
            }
            if (flushed >= FLUSH_TICKS) {
                wash();
            }
        }
    }

    private boolean flushing() {
        return inChannel > 0;
    }

    /**
     * The channel runs clear, and what was in it is now on the skin.
     *
     * <p>Not gone - moved. That is what irrigation does, and it is why there is a second
     * half to this at all: the dirt is out of the wound and it is still on the person.
     */
    private void wash() {
        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        for (int i = 0; i < INSIDE; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            float distance = 22.0F + random.nextFloat() * 12.0F;
            speckX[i] = centreX + Mth.cos(angle) * distance;
            speckY[i] = centreY + Mth.sin(angle) * distance * 0.9F;
        }
        inChannel = 0;
        play(SoundEvents.BOTTLE_EMPTY, 0.9F);
    }

    /**
     * One wipe.
     *
     * <p>Every speck the cloth passes over moves with it, which is all the rule there is
     * and all it needs to be: go outwards and the dirt goes outwards with you, come back
     * inwards and it comes back with you. Nobody has to be told the second half; they do
     * it once and watch it happen.
     */
    private void wipe() {
        if (!hasLast) {
            lastX = handX;
            lastY = handY;
            hasLast = true;
            return;
        }
        float dx = handX - lastX;
        float dy = handY - lastY;
        lastX = handX;
        lastY = handY;
        if (dx * dx + dy * dy < 0.5F) {
            return;
        }

        boolean tookAny = false;
        boolean broughtAny = false;
        for (int i = 0; i < SPECKS; i++) {
            if (gone[i]) {
                continue;
            }
            if (Math.abs(handX - speckX[i]) > BRUSH || Math.abs(handY - speckY[i]) > BRUSH) {
                continue;
            }
            speckX[i] += dx;
            speckY[i] += dy;
            float from = distance(speckX[i], speckY[i]);
            if (from >= OFF) {
                gone[i] = true;
                tookAny = true;
                continue;
            }
            // Dragged back over the opening: it is in the wound again, and it goes back
            // to the bottom of the channel where only a flush will reach it.
            if (from <= 10.0F) {
                broughtAny = true;
            }
        }
        if (tookAny) {
            play(SoundEvents.WOOL_HIT, 1.2F);
        }
        if (broughtAny) {
            wrongWay = 16;
        }
    }

    private float distance(float x, float y) {
        float dx = x - centreX;
        float dy = (y - centreY) * 1.1F;
        return Mth.sqrt(dx * dx + dy * dy);
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        float flush = Mth.clamp(flushed / (float) FLUSH_TICKS, 0.0F, 1.0F) * 0.35F;
        int off = 0;
        for (boolean g : gone) {
            if (g) {
                off++;
            }
        }
        return Mth.clamp(flush + 0.65F * off / SPECKS, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        if (flushing()) {
            return false;
        }
        for (boolean g : gone) {
            if (!g) {
                return false;
            }
        }
        return true;
    }

    /** Nothing here is unrecoverable. Dirt put back is dirt to take off again. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        if (flushing()) {
            return Component.translatable("triage.minigame.clean_flush");
        }
        if (wrongWay > 0) {
            return Component.translatable("triage.minigame.clean_inward");
        }
        return Component.translatable("triage.minigame.clean_outward");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        skin(graphics);
        wound(graphics);
        specks(graphics);
        hand(graphics);
    }

    private void skin(GuiGraphics graphics) {
        int half = Math.min(84, width / 2 - 4);
        graphics.fill(centreX - half - 1, top + 5, centreX + half + 1, top + height - 5, COLOR_SKIN_EDGE);
        Draw.body(graphics, centreX - half, top + 6, half * 2, height - 12, COLOR_SKIN);
        // The ring the dirt has to be taken past, so "outwards" has an edge to it.
        if (!flushing() && !complete()) {
            Draw.ring(graphics, centreX, centreY, OFF, 40, 1, Draw.alpha(COLOR_MARK, 0.35F));
        }
    }

    private void wound(GuiGraphics graphics) {
        for (int row = -19; row <= 19; row++) {
            int half = Math.max(0, 7 - Math.abs(row) / 3);
            if (half <= 0) {
                continue;
            }
            graphics.fill(centreX - half, centreY + row, centreX + half, centreY + row + 1,
                    flushing() ? COLOR_WOUND : COLOR_CLEAN);
            if (half > 2) {
                graphics.fill(centreX - half + 2, centreY + row, centreX + half - 2, centreY + row + 1,
                        flushing() ? COLOR_WOUND_DEEP : COLOR_WOUND);
            }
        }
        if (flushing()) {
            // Where to hold it, and how far through the flush is.
            int colour = wrongWay > 0 ? COLOR_BAD : COLOR_MARK;
            graphics.renderOutline(centreX - Math.round(OVER), centreY - 30,
                    Math.round(OVER) * 2, 60, colour);
            int barLeft = centreX - 30;
            int filled = Math.round(60 * flushed / (float) FLUSH_TICKS);
            graphics.fill(barLeft, top + height - 14, barLeft + 60, top + height - 11, 0x50FFFFFF);
            graphics.fill(barLeft, top + height - 14, barLeft + filled, top + height - 11, COLOR_GOOD);
        }
    }

    private void specks(GuiGraphics graphics) {
        for (int i = 0; i < SPECKS; i++) {
            if (gone[i]) {
                continue;
            }
            int x = Math.round(speckX[i]);
            int y = Math.round(speckY[i]);
            int r = size[i];
            Draw.disc(graphics, x, y, r + 1, COLOR_DIRT_EDGE);
            Draw.disc(graphics, x, y, r, COLOR_DIRT);
        }
    }

    /** A bottle while it is being flushed, a swab once it is not. */
    private void hand(GuiGraphics graphics) {
        if (!held) {
            return;
        }
        int x = Math.round(Mth.clamp(handX, left + 6, left + width - 6));
        int y = Math.round(Mth.clamp(handY, top + 6, top + height - 6));
        if (flushing()) {
            graphics.fill(x - 5, y - 16, x + 5, y - 2, COLOR_BOTTLE);
            graphics.fill(x - 2, y - 2, x + 2, y + 2, Draw.shade(COLOR_BOTTLE, 0.8F));
            for (int i = 0; i < 5; i++) {
                int sy = y + 3 + i * 6 + (age % 6);
                Draw.disc(graphics, x + ((i + age / 3) % 3) - 1, sy, 2, COLOR_FLUID);
            }
            return;
        }
        int colour = wrongWay > 0 ? COLOR_BAD : COLOR_CLOTH;
        graphics.fill(x - 9, y - 9, x + 9, y + 9, Draw.shade(colour, 0.85F));
        graphics.fill(x - 8, y - 8, x + 8, y + 8, colour);
        // Which way is out, drawn on the cloth itself.
        float dx = handX - centreX;
        float dy = handY - centreY;
        float length = Math.max(1.0F, Mth.sqrt(dx * dx + dy * dy));
        Draw.line(graphics, x, y, x + Math.round(dx / length * 13.0F), y + Math.round(dy / length * 13.0F),
                1, wrongWay > 0 ? COLOR_BAD : COLOR_GOOD);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
