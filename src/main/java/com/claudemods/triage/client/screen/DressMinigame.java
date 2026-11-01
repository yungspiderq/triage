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
 * Closing a wound with a kit: drawing the edges together along its length, from one end
 * to the other.
 *
 * <p>What this replaced was the surgery game with the numbers turned down - a row of
 * points to click before a timer ran out. That is a test of aim, and aim is not what a
 * medkit is: it made dressing a cut and operating on a chest the same activity, only one
 * of them was easier.
 *
 * <p>A wound is not a set of points. It is a line, and closing one is a single unbroken
 * movement down that line with the edges held together behind you. So this is a line, and
 * the work is going down it slowly enough to bring each stretch together - a stretch
 * closes after the hand has been on it for a moment, which means dragging quickly down
 * the wound closes nothing at all. There is no timer, because there is nothing about
 * this that is faster if you hurry.
 *
 * <p>One thing can go wrong and it is the honest one: pull away from the line while you
 * are working and the last stretch comes apart again. You have to go back over it.
 */
public final class DressMinigame implements Minigame {
    /** How many stretches the wound is closed in. */
    private static final int SEGMENTS = 14;
    /** How long the hand has to stay on one before it holds. */
    private static final int HOLD_TICKS = 3;
    /** How far off the line the hand may be and still be working on it. */
    private static final float REACH = 16.0F;
    /** And how far off before the last stretch is pulled open again. */
    private static final float STRAY = 34.0F;

    private static final int COLOR_SKIN = 0xFFB07A5A;
    private static final int COLOR_SKIN_EDGE = 0xFF6E4632;
    private static final int COLOR_OPEN = 0xFF8E1616;
    private static final int COLOR_DEEP = 0xFF4E0F0F;
    private static final int COLOR_CLOSED = 0xFF7A4438;
    private static final int COLOR_SEAM = 0xFF5A2A24;
    private static final int COLOR_NEXT = 0xFFFFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;
    private static final int COLOR_TOOL = 0xFFD8D2C4;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;

    private final int woundTop;
    private final int segmentHeight;
    /** How far each stretch of the wound wanders off the centre line, and how wide it is. */
    private final int[] offset = new int[SEGMENTS];
    private final int[] gape = new int[SEGMENTS];

    private int closed;
    private int held;
    private boolean working;
    private float handX;
    private float handY;
    private int strayFlash;

    public DressMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.centreX = left + width / 2;

        int usable = Math.min(height - 24, SEGMENTS * 7);
        this.segmentHeight = Math.max(4, usable / SEGMENTS);
        this.woundTop = top + (height - segmentHeight * SEGMENTS) / 2;

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        int drift = 0;
        for (int i = 0; i < SEGMENTS; i++) {
            drift = Mth.clamp(drift + random.nextInt(5) - 2, -9, 9);
            offset[i] = drift;
            // Widest in the middle and pinched at both ends, which is the shape of a
            // wound rather than the shape of a rectangle.
            float along = i / (float) (SEGMENTS - 1);
            gape[i] = 2 + Math.round(5.0F * Mth.sin(along * Mth.PI)) + random.nextInt(2);
        }
        this.handX = centreX;
        this.handY = woundTop;
    }

    /**
     * The wound closes under the hand, one stretch at a time and in order.
     *
     * <p>In order because that is how it is done - you do not close the middle of a cut
     * and come back for the ends - and because it means there is always exactly one place
     * to be, which the screen can then point at. Nothing in this game is a guess.
     */
    @Override
    public void tick() {
        if (strayFlash > 0) {
            strayFlash--;
        }
        if (!working || complete()) {
            return;
        }
        float line = lineAt(closed);
        float away = Math.abs(handX - line);
        float near = nearness(closed);

        if (away > STRAY || near > segmentHeight * 2.5F) {
            // Pulled away from it. What was last drawn together comes apart, which is
            // the one thing that can go backwards here and the one thing that should.
            if (closed > 0 && strayFlash == 0) {
                closed--;
                held = 0;
                strayFlash = 14;
                play(SoundEvents.SLIME_SQUISH, 0.7F);
            }
            return;
        }
        if (away > REACH || near > segmentHeight) {
            held = 0;
            return;
        }
        if (++held >= HOLD_TICKS) {
            closed++;
            held = 0;
            play(SoundEvents.WOOL_HIT, 0.9F + 0.03F * closed);
        }
    }

    /** The middle of the wound at one stretch. */
    private float lineAt(int segment) {
        int i = Mth.clamp(segment, 0, SEGMENTS - 1);
        return centreX + offset[i];
    }

    /** How far the hand is from that stretch, up or down the wound. */
    private float nearness(int segment) {
        float middle = woundTop + segment * segmentHeight + segmentHeight / 2.0F;
        return Math.abs(handY - middle);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        working = true;
        handX = (float) mouseX;
        handY = (float) mouseY;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        working = true;
        handX = (float) mouseX;
        handY = (float) mouseY;
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            working = false;
            held = 0;
        }
    }

    @Override
    public float progress() {
        return Mth.clamp(closed / (float) SEGMENTS, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return closed >= SEGMENTS;
    }

    /** Nothing here is unrecoverable. What comes apart is closed again. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        if (strayFlash > 0) {
            return Component.translatable("triage.minigame.dress_open");
        }
        if (closed == 0) {
            return Component.translatable("triage.minigame.dress_start");
        }
        return Component.translatable("triage.minigame.dress_along");
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int skinLeft = centreX - 40;
        int skinRight = centreX + 40;
        graphics.fill(skinLeft - 1, woundTop - 9, skinRight + 1, woundTop + segmentHeight * SEGMENTS + 9,
                COLOR_SKIN_EDGE);
        Draw.body(graphics, skinLeft, woundTop - 8, skinRight - skinLeft,
                segmentHeight * SEGMENTS + 16, COLOR_SKIN);

        for (int i = 0; i < SEGMENTS; i++) {
            int y = woundTop + i * segmentHeight;
            int x = Math.round(lineAt(i));
            if (i < closed) {
                // Drawn together: a seam rather than a channel.
                graphics.fill(x - 1, y, x + 2, y + segmentHeight, COLOR_CLOSED);
                graphics.fill(x, y, x + 1, y + segmentHeight, COLOR_SEAM);
            } else {
                int half = gape[i];
                graphics.fill(x - half, y, x + half, y + segmentHeight, COLOR_OPEN);
                graphics.fill(x - half / 2, y, x + half / 2 + 1, y + segmentHeight, COLOR_DEEP);
            }
        }

        // Where to be next, which is always exactly one place.
        if (!complete()) {
            int y = woundTop + closed * segmentHeight;
            int x = Math.round(lineAt(closed));
            int colour = strayFlash > 0 ? COLOR_BAD : COLOR_NEXT;
            graphics.fill(x - gape[closed] - 6, y, x - gape[closed] - 2, y + segmentHeight, colour);
            graphics.fill(x + gape[closed] + 2, y, x + gape[closed] + 6, y + segmentHeight, colour);
            // How long the hand has been on it, so that "hold it" has a length.
            if (held > 0) {
                int bar = Math.round((segmentHeight - 1) * held / (float) HOLD_TICKS);
                graphics.fill(x - gape[closed] - 6, y, x - gape[closed] - 2, y + bar, COLOR_GOOD);
            }
        }

        // The hand: a pair of forceps holding the edges, near enough.
        if (working) {
            int x = Math.round(Mth.clamp(handX, left + 4, left + width - 4));
            int y = Math.round(Mth.clamp(handY, top + 4, top + height - 4));
            boolean good = !complete() && Math.abs(handX - lineAt(closed)) <= REACH
                    && nearness(closed) <= segmentHeight;
            int colour = strayFlash > 0 ? COLOR_BAD : good ? COLOR_GOOD : COLOR_TOOL;
            graphics.fill(x - 5, y - 1, x - 1, y + 1, colour);
            graphics.fill(x + 1, y - 1, x + 5, y + 1, colour);
            graphics.fill(x - 1, y - 4, x + 1, y + 4, Draw.shade(colour, 0.8F));
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
