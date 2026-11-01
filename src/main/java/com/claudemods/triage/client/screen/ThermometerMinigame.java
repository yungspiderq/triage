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
 * Taking a temperature with a mercury thermometer, which is three things and not one.
 *
 * <p>The first version of this was a bar that filled while the button was held. The note
 * at the top of it argued that waiting <em>was</em> the content, because a temperature is
 * the one reading that cannot be hurried. That is true of the reading and it is not true
 * of the instrument, and it made for a game with nothing in it: press, wait, let go.
 *
 * <p>What a mercury thermometer actually asks of a pair of hands is three separate things,
 * and every one of them is something anybody who has used one remembers doing.
 *
 * <p><b>Shake it down.</b> Mercury does not go back on its own - that is the whole trick of
 * the instrument, the reason it can be taken out and read at leisure, and the reason it
 * opens still showing <em>the last person's fever</em>. A thermometer that is not shaken
 * down does not read the patient in front of you. It is the same idea as the grime on a saw:
 * the instrument carries the last patient into this one, and nobody is told, and the fix is
 * free and takes a second.
 *
 * <p><b>Hold it in place.</b> The column climbs fast, then slower, then hardly at all - the
 * shape mercury has, and the reason the last tenth of a degree takes longer than the first
 * two. A body is not a table: it breathes and shifts, and the bulb comes out of the fold if
 * nobody is paying attention. Out of the fold nothing climbs. Nothing falls either, because
 * mercury does not fall - the cost of not paying attention is time, which is exactly what
 * this reading is made of.
 *
 * <p><b>Read the thread.</b> The column is a hair of metal behind a lens of glass, and it is
 * invisible from almost every angle. You turn it in the light until the thread flashes into
 * view against the scale, and then you can say the number. This is the part everybody who
 * has held one remembers, and it was the part the old game left out entirely.
 *
 * <p>None of it can be failed. Pulled early the figure is low, unshaken the figure is
 * somebody else's, and both come back as a band rather than a number - which is what an
 * unreliable reading is worth, and rather better than a lie.
 */
public final class ThermometerMinigame implements Minigame {
    private static final int SHAKE = 0;
    private static final int HOLD = 1;
    private static final int READ = 2;
    private static final int DONE = 3;

    /** Ticks in the fold for the column to be as settled as it is going to get. */
    private static final float SETTLE = 110.0F;
    /** Past this share of the climb, the reading is worth the exact number. */
    private static final float GOOD = 0.93F;
    /** How much of the leftover has to be driven down for this to be the patient's reading. */
    private static final float SHAKEN = 0.85F;
    /** Mouse travel, in pixels, that counts as one flick of the wrist. */
    private static final float FLICK = 46.0F;
    /** What one flick knocks off the column. */
    private static final float PER_FLICK = 0.17F;
    /** How far off the angle can be and still catch the thread. */
    private static final float TOLERANCE = 0.22F;
    /** How wide a slice of arm one rectangle covers. One pixel a time was the old bug. */
    private static final int COLUMN = 3;
    /** Ticks of holding the thread in view before the number can be said. */
    private static final int FOCUS = 14;

    private static final int CASE = 0xFFE9EDF1;
    private static final int CASE_EDGE = 0xFF8E97A0;
    private static final int GLASS = 0xFFCFDAE3;
    private static final int GLASS_LIT = 0xFFF2F7FA;
    private static final int BORE = 0xFF9FAEBA;
    private static final int MERCURY = 0xFFC63B33;
    private static final int MERCURY_DIM = 0x60A0342E;
    private static final int BULB = 0xFFA82B25;
    private static final int SCALE = 0xFF4A525A;
    private static final int SKIN = 0xFFC89571;
    private static final int SKIN_LIT = 0xFFDCAA85;
    private static final int SKIN_DARK = 0xFF9A6A4C;
    private static final int FOLD = 0xFF7C5238;
    private static final int SETTLED = 0xFF7FD46B;
    private static final int MOVING = 0xFFFFD37F;
    private static final int WRONG = 0xFFCC3333;
    private static final int QUIET = 0xFFBFC6CC;

    private final int left;
    private final int top;
    private final int width;
    private final int height;

    /** Where the last reading left the column. Nobody knows whose it was. */
    private final float leftover;
    /** The angle at which the light catches the thread. Different every time it is picked up. */
    private final float sweet;

    private int phase = SHAKE;

    /**
     * Whether the column was driven down before this went anywhere near anybody.
     *
     * <p>Remembered at the moment it happens, and that is the whole of this fix. It used
     * to be worked out at the end, from the height of the column - but the column is
     * reused during the hold to show the reading climbing, so by the time anybody asked,
     * the number being divided was the new reading rather than the old leftover.
     *
     * <p>Which made the test read <b>backwards</b>: the longer the thermometer was held -
     * the better the reading, the more careful the player - the higher the column stood
     * and the more certain the game became that it had never been shaken. Every reading
     * anybody has ever taken came back capped at three tenths, and the line under it
     * blamed the one thing the player had done right.
     */
    private boolean shaken;

    private float column;
    private float lastColumn;
    private float swing;
    private int flash;

    private float held;
    private float lastHeld;
    private boolean inFold;

    private float angle;
    private float lastAngle;
    private int focus;

    private boolean down;
    private float mouseX;
    private float mouseY;
    private float quality;

    public ThermometerMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = Math.max(80, width);
        this.height = Math.max(60, height);
        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.random : RandomSource.create();
        // High enough to be somebody's fever, which is the point: it is a reading, it looks
        // like a reading, and it is not this patient's.
        this.leftover = 0.52F + random.nextFloat() * 0.3F;
        this.column = leftover;
        this.lastColumn = leftover;
        this.sweet = -0.4F + random.nextFloat() * 0.8F;
    }

    // ---- where things are on the screen -------------------------------------

    private int midY() {
        return top + height / 2 + 2;
    }

    /**
     * The underside of the arm lying across them, which is the fold the bulb goes into.
     *
     * <p>A shallow curve rather than a line, because that is what the edge of an arm is,
     * and because the same function decides both where it is drawn and where the hand has
     * to be - so what the player is asked to hit is exactly what the player can see.
     */
    private float foldAt(float x, float partial) {
        float t = Mth.clamp((x - left) / (float) width, 0.0F, 1.0F);
        float curve = Mth.sin(t * Mth.PI * 0.85F + 0.25F);
        return top + height * 0.34F + curve * (height * 0.1F) + breath(partial);
    }

    /** A chest that is not a table: it rises and falls, and the fold moves with it. */
    private float breath(float partial) {
        return Mth.sin((held + partial) * 0.055F) * 4.0F;
    }

    // ---- the hand ----------------------------------------------------------

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0 || phase == DONE) {
            return false;
        }
        down = true;
        mouseX = (float) x;
        mouseY = (float) y;
        if (phase == READ) {
            turn((float) x);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button) {
        if (button != 0 || phase == DONE) {
            return false;
        }
        float dx = (float) x - mouseX;
        float dy = (float) y - mouseY;
        mouseX = (float) x;
        mouseY = (float) y;
        switch (phase) {
            case SHAKE -> shake(Mth.sqrt(dx * dx + dy * dy));
            case READ -> turn((float) x);
            default -> {
            }
        }
        return true;
    }

    @Override
    public void mouseReleased(double x, double y, int button) {
        if (button != 0) {
            return;
        }
        down = false;
        // Letting go in the fold is taking it out, and taking it out is the answer. Every
        // other phase simply stops.
        if (phase == HOLD && held > 6.0F) {
            phase = READ;
            sound(SoundEvents.ITEM_PICKUP, 1.4F);
        }
    }

    /**
     * A flick of the wrist, and what it knocks off the column.
     *
     * <p>Measured in mouse travel rather than in clicks on purpose: shaking a thermometer
     * down is a movement of the arm and not a button, and asking for the movement is what
     * makes it the thing it is rather than another press.
     */
    private void shake(float travel) {
        swing += travel;
        if (swing < FLICK) {
            return;
        }
        swing = 0.0F;
        column = Math.max(0.0F, column - PER_FLICK);
        flash = 4;
        sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.9F);
        if (column <= 0.02F) {
            phase = HOLD;
            column = 0.0F;
            shaken = true;
            sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F);
        }
    }

    /** Turning it in the light. The angle is simply where along the well the hand is. */
    private void turn(float x) {
        angle = Mth.clamp((x - (left + width / 2.0F)) / (width / 2.0F), -1.0F, 1.0F);
    }

    /** How much of the thread is catching the light, 0 to 1. */
    private float clarity() {
        return 1.0F - Math.min(1.0F, Math.abs(angle - sweet) / TOLERANCE);
    }

    @Override
    public void tick() {
        lastColumn = column;
        lastHeld = held;
        lastAngle = angle;
        if (flash > 0) {
            flash--;
        }
        switch (phase) {
            case SHAKE -> swing *= 0.85F;
            case HOLD -> {
                if (!down) {
                    return;
                }
                // In the fold or not: the whole of the attention this reading asks for.
                inFold = Math.abs(mouseY - foldAt(mouseX, 0.0F)) < 10.0F;
                if (!inFold) {
                    return;
                }
                held++;
                column = climb(held);
            }
            case READ -> {
                float clear = clarity();
                if (clear > 0.7F && down) {
                    focus++;
                    if (focus >= FOCUS) {
                        read();
                    }
                } else {
                    focus = Math.max(0, focus - 1);
                }
            }
            default -> {
            }
        }
    }

    /**
     * How far up the column has got, 0 to 1.
     *
     * <p>Fast, then slower, then hardly at all - which is the shape a thermometer actually
     * has and the reason the last tenth of a degree takes longer than the first two.
     */
    private float climb(float ticks) {
        return 1.0F - (float) Math.exp(-3.0F * ticks / SETTLE);
    }

    /** How much of the leftover was driven out of it before it went anywhere near anybody. */
    private float shakenShare() {
        return leftover <= 0.0F ? 1.0F : Mth.clamp(1.0F - column / leftover, 0.0F, 1.0F);
    }

    private void read() {
        phase = DONE;
        float settled = Mth.clamp(climb(held) / GOOD, 0.0F, 1.0F);
        // Two bands rather than one number, so that the line under the reading can say
        // which of the two things went wrong. Under three tenths is a thermometer that
        // still had somebody else's fever in it; above four is this patient's column,
        // however far up it got.
        quality = shaken ? 0.4F + settled * 0.6F : Math.min(0.3F, settled);
        boolean figure = quality >= 0.995F;
        sound(figure ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.ITEM_PICKUP,
                figure ? 1.3F : 0.8F);
    }

    private void sound(SoundEvent event, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(event, pitch, 0.35F));
    }

    // ---- what it looks like -------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        float shown = Mth.lerp(partialTick, lastColumn, column);

        switch (phase) {
            case SHAKE -> shakeScene(graphics, shown);
            case HOLD -> holdScene(graphics, shown, partialTick);
            default -> readScene(graphics, font, shown, partialTick);
        }
        note(graphics, font);
    }

    /** Held up by the far end, bulb down, being knocked back to nothing. */
    private void shakeScene(GuiGraphics graphics, float shown) {
        int cx = left + width / 2;
        int topY = top + 6;
        int stemBottom = top + height - 30;
        int stemTop = topY + 6;
        // The glass, upright, with the bulb at the foot of it.
        graphics.fill(cx - 7, topY, cx + 7, stemBottom + 2, CASE_EDGE);
        graphics.fill(cx - 6, topY + 1, cx + 6, stemBottom + 1, CASE);
        graphics.fill(cx - 3, stemTop, cx + 3, stemBottom, GLASS);
        // A scale down the side of it, so this reads as a thermometer and not a tube.
        int step = Math.max(3, (stemBottom - stemTop - 6) / 8);
        for (int i = 0; i < 9; i++) {
            int y = stemTop + 3 + i * step;
            graphics.fill(cx + 3, y, cx + 3 + (i % 3 == 0 ? 4 : 2), y + 1, SCALE);
        }
        int run = Math.round((stemBottom - stemTop) * Mth.clamp(shown, 0.0F, 1.0F));
        if (run > 0) {
            graphics.fill(cx - 2, stemBottom - run, cx + 2, stemBottom, MERCURY);
        }
        Draw.disc(graphics, cx, stemBottom + 5, 6, BULB);

        // Streaks off the glass while it is moving, which is all the motion there is.
        if (flash > 0) {
            for (int i = 1; i <= 3; i++) {
                int y = topY + 12 + i * 13;
                int reach = 4 + flash * 3 - i;
                graphics.fill(cx + 9, y, cx + 9 + reach, y + 1, QUIET);
                graphics.fill(cx - 7 - reach, y + 6, cx - 9, y + 7, QUIET);
            }
        }
        // And how far down it has been driven, because that is the whole of this phase.
        int barLeft = left + 10;
        int barRight = left + width - 10;
        int barY = top + height - 20;
        graphics.fill(barLeft, barY, barRight, barY + 3, CASE_EDGE);
        int gone = Math.round((barRight - barLeft) * shakenShare());
        graphics.fill(barLeft, barY, barLeft + gone, barY + 3, SETTLED);
    }

    /** Laid into a fold of skin, being kept there. */
    private void holdScene(GuiGraphics graphics, float shown, float partialTick) {
        // The chest they are lying against, underneath everything, shaded downwards.
        for (int y = top; y < top + height - 10; y++) {
            float t = (y - top) / (float) Math.max(1, height - 10);
            graphics.fill(left, y, left + width, y + 1, t < 0.5F ? SKIN : SKIN_DARK);
        }

        // The thermometer, going in under the arm from below and to the right.
        float ax = 0.93F;
        float ay = 0.36F;
        float bulbX = left + width * 0.46F;
        float bulbY = foldAt(bulbX, partialTick) - 1.0F;
        float reach = Math.min(88.0F, width * 0.42F);
        Shapes.bar(graphics, bulbX, bulbY, ax, ay, reach, 5.0F, CASE_EDGE);
        Shapes.bar(graphics, bulbX, bulbY, ax, ay, reach, 4.0F, CASE);
        Shapes.bar(graphics, bulbX, bulbY, ax, ay, reach - 4.0F, 1.8F, GLASS);
        Shapes.bar(graphics, bulbX, bulbY, ax, ay,
                Math.max(2.0F, (reach - 10.0F) * shown), 1.4F, MERCURY);
        Draw.disc(graphics, Math.round(bulbX), Math.round(bulbY), 5, BULB);

        // And the arm laid over the top of it, drawn last on purpose: what it covers is
        // the bulb, and a covered bulb is the whole of what "in the fold" looks like.
        // Three pixels at a time rather than one. The curve is shallow enough that the
        // step does not show, and a quad per pixel across an arm is how the syringe game
        // ended up running at thirteen frames.
        for (int x = left; x < left + width; x += COLUMN) {
            int e = Math.round(foldAt(x, partialTick));
            int x1 = Math.min(left + width, x + COLUMN);
            graphics.fill(x, top, x1, e - 6, SKIN_LIT);
            graphics.fill(x, e - 6, x1, e, SKIN);
            graphics.fill(x, e, x1, e + 2, FOLD);
            graphics.fill(x, e + 2, x1, e + 4, SKIN_DARK);
        }

        // Where the hand is, and whether that is in the fold.
        int hx = Math.round(this.mouseX);
        int hy = Math.round(this.mouseY);
        int mark = inFold && down ? SETTLED : down ? WRONG : QUIET;
        graphics.fill(hx - 6, hy, hx + 7, hy + 1, mark);
        graphics.fill(hx, hy - 6, hx + 1, hy + 7, mark);
    }

    /** Out, and turned in the light until the thread shows. */
    private void readScene(GuiGraphics graphics, Font font, float shown, float partialTick) {
        float shownAngle = Mth.lerp(partialTick, lastAngle, angle);
        float clear = phase == DONE ? 1.0F : clarity();
        int midY = midY();
        int stemLeft = left + 20;
        int stemRight = left + width - 14;

        // The glass, big and flat, because this is the phase where it is being looked at.
        graphics.fill(stemLeft - 3, midY - 13, stemRight + 3, midY + 13, CASE_EDGE);
        graphics.fill(stemLeft - 2, midY - 12, stemRight + 2, midY + 12, CASE);
        graphics.fill(stemLeft, midY - 7, stemRight, midY + 7, GLASS);
        Draw.disc(graphics, left + 12, midY, 8, BULB);
        graphics.fill(left + 18, midY - 2, stemLeft, midY + 3, BULB);

        // The highlight running along the glass as it turns. Where it sits is the angle,
        // and finding the angle is the game. It is drawn above and below the bore rather
        // than across it, because a highlight that cuts the thread reads as a gap in it.
        int glint = Math.round(Mth.lerp((shownAngle + 1.0F) * 0.5F, stemLeft, stemRight - 16));
        for (int dy = -7; dy <= 7; dy++) {
            if (Math.abs(dy) <= 2) {
                continue;
            }
            int skew = Math.round(dy * 0.7F);
            graphics.fill(glint + skew, midY + dy, glint + skew + 14, midY + dy + 1, GLASS_LIT);
        }

        // The bore, and the thread in it - which is only there when the light is right.
        graphics.fill(stemLeft, midY, stemRight, midY + 1, BORE);
        int run = Math.round((stemRight - stemLeft - 4) * shown);
        if (clear > 0.25F) {
            int alpha = Math.round(255 * Mth.clamp(clear, 0.0F, 1.0F));
            int colour = (alpha << 24) | (MERCURY & 0xFFFFFF);
            graphics.fill(stemLeft, midY - 1, stemLeft + run, midY + 2, colour);
        } else {
            graphics.fill(stemLeft, midY, stemLeft + run, midY + 1, MERCURY_DIM);
        }

        // The scale, which is only worth anything once the thread is visible against it.
        for (int i = 0; i < 12; i++) {
            int x = stemLeft + 4 + i * ((stemRight - stemLeft - 8) / 11);
            boolean tall = i % 3 == 0;
            graphics.fill(x, midY - 12, x + 1, midY - 12 + (tall ? 7 : 4), SCALE);
            graphics.fill(x, midY + 12 - (tall ? 7 : 4), x + 1, midY + 12, SCALE);
        }
        if (phase == DONE) {
            graphics.fill(stemLeft + run - 1, midY - 12, stemLeft + run + 1, midY + 12, SETTLED);
        } else if (focus > 0) {
            // Holding it still: the last thing before the number can be said.
            int fill = Math.round((stemRight - stemLeft) * (focus / (float) FOCUS));
            graphics.fill(stemLeft, midY + 15, stemLeft + fill, midY + 17, MOVING);
        }
    }

    private void note(GuiGraphics graphics, Font font) {
        Component text;
        int colour;
        if (phase == DONE) {
            boolean clean = shakenShare() >= SHAKEN;
            boolean full = quality >= 1.0F;
            text = Component.translatable(!clean ? "triage.minigame.temp_stale"
                    : full ? "triage.minigame.temp_read" : "triage.minigame.temp_early");
            colour = !clean ? WRONG : full ? SETTLED : MOVING;
        } else if (phase == SHAKE) {
            text = Component.translatable(flash > 0
                    ? "triage.minigame.temp_shaking" : "triage.minigame.temp_stale_start");
            colour = MOVING;
        } else if (phase == HOLD) {
            if (!down) {
                text = Component.translatable("triage.minigame.temp_place");
                colour = QUIET;
            } else if (!inFold) {
                text = Component.translatable("triage.minigame.temp_slipped");
                colour = WRONG;
            } else if (climb(held) >= GOOD) {
                text = Component.translatable("triage.minigame.temp_settled");
                colour = SETTLED;
            } else {
                text = Component.translatable("triage.minigame.temp_climbing");
                colour = MOVING;
            }
        } else if (clarity() > 0.7F) {
            text = Component.translatable("triage.minigame.temp_thread");
            colour = SETTLED;
        } else {
            text = Component.translatable("triage.minigame.temp_turning");
            colour = MOVING;
        }
        graphics.drawString(font, text, left + 2, top + height - 9, colour, false);
    }

    @Override
    public float progress() {
        return switch (phase) {
            case SHAKE -> shakenShare() * 0.3F;
            case HOLD -> 0.3F + climb(held) * 0.5F;
            case READ -> 0.8F + 0.2F * (focus / (float) FOCUS);
            default -> 1.0F;
        };
    }

    @Override
    public float quality() {
        return quality;
    }

    @Override
    public boolean complete() {
        return phase == DONE;
    }

    @Override
    public boolean failed() {
        // Nothing here can go wrong. The worst outcome is a number that is too low or one
        // that belongs to somebody else, and both of those are worse readings rather than
        // failed ones. That is what a thermometer is.
        return false;
    }

    @Override
    public Component instruction() {
        return Component.translatable(switch (phase) {
            case SHAKE -> "triage.minigame.temp_hint_shake";
            case HOLD -> down && inFold ? "triage.minigame.temp_hint_hold"
                    : "triage.minigame.temp_hint_place";
            case READ -> "triage.minigame.temp_hint_turn";
            default -> "triage.minigame.temp_done";
        });
    }
}
