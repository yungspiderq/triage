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
 * A course, out of the card it comes in.
 *
 * <p>The mod already said the right thing about this in a comment and then drew the wrong
 * thing: "a card of tablets rather than a bottle". It was the needle game with the
 * difficulty nudged, the same as three other treatments.
 *
 * <p>A blister pack is not a bottle and does not behave like one. Nothing is counted out
 * of it - the counting was done at the factory - and there is no dose to get wrong. What
 * there is, is a strip with the days printed under it, and the only thing anybody does
 * with one is press today's out through the foil. So that is the game: press, feel it
 * give, and move to the next.
 *
 * <p>Which makes it the shortest thing in the mod, and it should be. This is not a
 * treatment somebody performs, it is a treatment somebody is <em>on</em>: the weight of it
 * is in the six hundred seconds afterwards and in what happens when they run out, and a
 * long minigame in front of that would be putting the effort in the one place this
 * particular medicine does not ask for any.
 */
public final class CourseMinigame implements Minigame {
    /** Pockets on the card, and how many of them are this dose. */
    private static final int POCKETS = 8;
    private static final int COLUMNS = 4;
    private static final int TAKE = 2;
    /** How long the thumb has to press before the foil gives. */
    private static final int PUSH_TICKS = 9;

    private static final int COLOR_CARD = 0xFFE8E4DA;
    private static final int COLOR_CARD_EDGE = 0xFFB0AA9C;
    private static final int COLOR_FOIL = 0xFFC9CED4;
    private static final int COLOR_FOIL_EDGE = 0xFF8E959C;
    private static final int COLOR_DOME = 0xFFF6F4EE;
    private static final int COLOR_EMPTY = 0xFF9A958A;
    private static final int COLOR_PILL = 0xFFF4F1E6;
    private static final int COLOR_PALM = 0xFFB07A5A;
    private static final int COLOR_PALM_EDGE = 0xFF6E4632;
    private static final int COLOR_MARK = 0xFFFFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_TEXT = 0xFF5A5347;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int cardLeft;
    private final int cardTop;
    private final int cell;

    private final boolean[] popped = new boolean[POCKETS];
    private int taken;
    /** Which pocket the thumb is on, and how long it has been on it. */
    private int pressing = -1;
    private int pushed;

    public CourseMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.cell = 22;
        this.cardLeft = left + (width - COLUMNS * cell) / 2;
        this.cardTop = top + 16;
    }

    @Override
    public void tick() {
        if (complete() || pressing < 0) {
            return;
        }
        if (++pushed >= PUSH_TICKS) {
            popped[pressing] = true;
            taken++;
            pressing = -1;
            pushed = 0;
            play(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1.4F);
        }
    }

    /** Which pocket is under the cursor, or -1. */
    private int at(double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - cardLeft) / cell);
        int row = (int) Math.floor((mouseY - cardTop) / cell);
        if (column < 0 || row < 0 || column >= COLUMNS || row >= POCKETS / COLUMNS) {
            return -1;
        }
        return row * COLUMNS + column;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        int pocket = at(mouseX, mouseY);
        if (pocket < 0 || popped[pocket]) {
            return false;
        }
        pressing = pocket;
        pushed = 0;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || pressing < 0) {
            return false;
        }
        // A thumb that slides off the pocket stops pushing it, which is what a thumb does.
        if (at(mouseX, mouseY) != pressing) {
            pressing = -1;
            pushed = 0;
        }
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            pressing = -1;
            pushed = 0;
        }
    }

    @Override
    public float progress() {
        return Mth.clamp(taken / (float) TAKE, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return taken >= TAKE;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        return Component.translatable("triage.minigame.course_press", TAKE - taken);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int cardWidth = COLUMNS * cell;
        int cardHeight = (POCKETS / COLUMNS) * cell;
        graphics.fill(cardLeft - 6, cardTop - 6, cardLeft + cardWidth + 6, cardTop + cardHeight + 6,
                COLOR_CARD_EDGE);
        graphics.fill(cardLeft - 5, cardTop - 5, cardLeft + cardWidth + 5, cardTop + cardHeight + 5,
                COLOR_CARD);

        for (int i = 0; i < POCKETS; i++) {
            int x = cardLeft + (i % COLUMNS) * cell + cell / 2;
            int y = cardTop + (i / COLUMNS) * cell + cell / 2;
            if (popped[i]) {
                // A pocket with the foil torn out of it.
                Draw.disc(graphics, x, y, 7, COLOR_EMPTY);
                Draw.disc(graphics, x, y, 5, Draw.shade(COLOR_EMPTY, 0.7F));
                continue;
            }
            boolean under = pressing == i;
            int give = under ? Math.round(3.0F * pushed / PUSH_TICKS) : 0;
            Draw.disc(graphics, x, y + give, 8, COLOR_FOIL_EDGE);
            Draw.disc(graphics, x, y + give, 7, COLOR_FOIL);
            Draw.disc(graphics, x - 2, y - 2 + give, 3, COLOR_DOME);
            if (under) {
                graphics.renderOutline(x - 9, y - 9 + give, 18, 18, COLOR_MARK);
                // How far the foil has gone, under the thumb.
                int bar = Math.round(16.0F * pushed / PUSH_TICKS);
                graphics.fill(x - 8, y + 11, x - 8 + bar, y + 13, COLOR_GOOD);
            }
        }

        // The strip is printed with the days, which is the only reason a card looks like
        // a card and not a tray.
        for (int i = 0; i < COLUMNS; i++) {
            int x = cardLeft + i * cell + cell / 2;
            graphics.fill(x - 5, cardTop - 3, x + 5, cardTop - 2, COLOR_TEXT);
        }

        // The palm underneath, and what has landed in it.
        int palmY = cardTop + cardHeight + 22;
        int half = 40;
        graphics.fill(centreX() - half - 1, palmY - 1, centreX() + half + 1, palmY + 15, COLOR_PALM_EDGE);
        Draw.body(graphics, centreX() - half, palmY, half * 2, 14, COLOR_PALM);
        for (int i = 0; i < taken; i++) {
            int x = centreX() - (TAKE - 1) * 12 / 2 + i * 12;
            Draw.disc(graphics, x, palmY + 7, 4, Draw.shade(COLOR_PILL, 0.8F));
            Draw.disc(graphics, x, palmY + 6, 3, COLOR_PILL);
        }
        for (int i = 0; i < TAKE; i++) {
            int x = centreX() - (TAKE - 1) * 12 / 2 + i * 12;
            if (i >= taken) {
                graphics.renderOutline(x - 4, palmY + 3, 8, 8, COLOR_MARK);
            }
        }
    }

    private int centreX() {
        return left + width / 2;
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
