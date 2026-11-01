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
 * A forearm, a syringe held over it, and the one thing that says where the tip is.
 *
 * <p>Pushing a plunger is not a skill and a game about pushing one would be a lie. What
 * takes a pair of hands is picking a vein you can barely see, going in far enough to be
 * inside it and no further, and waiting for the only confirmation anybody ever gets.
 *
 * <p>That confirmation is the flashback: dark blood welling back up into the barrel the
 * instant the tip is in the lumen. Inject with it and the dose is in the circulation.
 * Inject without it and it is in the muscle of somebody's arm, doing a fraction of what
 * was wanted and doing it late.
 *
 * <p>So it is three things anybody would recognise. Put the needle where you think the
 * vein is. Hold, and it goes in - at an angle, because nobody goes into a vein
 * perpendicular, and the shaft greys out as it passes under the skin. Stop when the blood
 * comes up, because a needle that keeps going comes out the far side and there is nothing
 * in the barrel any more.
 *
 * <p>The difficulty is the patient rather than a number. A vein is held open by what is
 * inside it, so somebody empty, cold or in shock has veins that are flat and barely
 * visible - which is exactly the person who needs the needle. They are drawn thinner and
 * fainter, so the player is shown the difficulty instead of being told it.
 */
public final class VeinMinigame implements Minigame {
    /** How wide a vein is on somebody full, and on somebody with nothing left. */
    private static final float FULL_VEIN = 5.0F;
    private static final float FLAT_VEIN = 2.0F;

    /** The axis the syringe is held on, and the perpendicular of it. */
    private static final float AXIS_X = 0.88F;
    private static final float AXIS_Y = -0.47F;
    private static final float PERP_X = 0.47F;
    private static final float PERP_Y = 0.88F;

    /** How far it goes in per tick while the button is down. */
    private static final float STEP = 0.9F;
    /** How wide a slice of arm one rectangle covers. One pixel a time was the bug. */
    private static final int COLUMN = 3;
    /** And how much of the barrel empties per tick once it is the plunger being held. */
    private static final float PLUNGE = 0.035F;

    private static final int NEEDLE = 26;
    private static final int HUB = 7;
    private static final int BARREL = 54;

    private static final int SKIN = 0xFFC89571;
    private static final int SKIN_LIT = 0xFFDCAA85;
    private static final int SKIN_DARK = 0xFF9A6A4C;
    private static final int SKIN_LINE = 0xFF83573D;
    private static final int UNDER_SKIN = 0xC0865A40;
    private static final int VEIN = 0xFF546E88;
    private static final int GLASS = 0xFFE4EBF0;
    private static final int GLASS_EDGE = 0xFF141A20;
    private static final int GLASS_SHADE = 0xFFA9B6C0;
    private static final int STEEL = 0xFFC6CBD2;
    private static final int BLOOD = 0xFF8E1616;
    private static final int GOOD = 0xFF7FD46B;
    private static final int BAD = 0xFFCC3333;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    /** The top edge of the skin, per column, so the needle can go under it. */
    private final int[] skinTop;
    private final int armMid;
    private final float thickness;
    private final float collapse;
    private final int fluid;
    private final float wobble;

    private float aimX;
    private float aimY;
    private float depth;
    /** Last tick's values, so the drawing can run between ticks instead of on them. */
    private float lastDepth;
    private float lastPlunger;
    private boolean pushing;
    /** True once they have stopped pushing the needle. From here the button is the plunger. */
    private boolean settled;
    private boolean flash;
    private boolean everFlashed;
    private boolean through;
    private boolean injected;
    private float plunger;
    /** How much of the dose has gone, and how much of that went where it was meant to. */
    private float given;
    private float givenWell;
    private float quality;
    /** What is drawn: the tick values carried between ticks, so it does not step. */
    private float shownDepth;
    private float shownPlunger;
    /** The hand, which is never quite still. */
    private float drift;

    public VeinMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight,
                        float collapse, int fluid) {
        this.left = areaLeft;
        this.top = areaTop;
        this.width = Math.max(40, areaWidth);
        this.height = Math.max(40, areaHeight);
        this.collapse = Mth.clamp(collapse, 0.0F, 1.0F);
        this.thickness = Mth.lerp(this.collapse, FULL_VEIN, FLAT_VEIN);
        this.fluid = fluid;
        this.armMid = areaTop + Math.round(this.height * 0.62F);

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        // Not the same arm twice: the veins sit somewhere else every time.
        this.wobble = random.nextFloat() * 6.0F;

        this.skinTop = new int[this.width];
        for (int i = 0; i < this.width; i++) {
            skinTop[i] = armMid - Math.round(armHalf(i));
        }
        this.aimX = areaLeft + this.width * 0.52F;
        this.aimY = armMid - 40;
    }

    /** Half the thickness of the arm at this column: fuller at the elbow, thinner at the wrist. */
    private float armHalf(int column) {
        float t = column / (float) Math.max(1, width - 1);
        return height * 0.19F - height * 0.05F * t + 2.0F * Mth.sin(t * 3.4F);
    }

    /** Where each vein runs at this column. */
    private float veinY(int which, int column) {
        float base = which == 0 ? -height * 0.07F : height * 0.05F;
        float amp = which == 0 ? 5.0F : 4.0F;
        float rate = which == 0 ? 0.030F : 0.024F;
        return armMid + base + amp * Mth.sin(column * rate + which * 2.0F + wobble);
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public void tick() {
        lastDepth = depth;
        lastPlunger = plunger;
        if (injected || !pushing) {
            return;
        }
        if (!settled) {
            // First hold: the needle goes in.
            if (depth < NEEDLE - 2) {
                depth += STEP;
                look();
            }
            return;
        }
        // Second hold: the fluid goes in, and it only counts while there is blood in the
        // barrel. Let the tip slip out of the vein half way and half the dose is in the
        // meat of somebody's arm - which is exactly what happens.
        // A hand is never quite still, and a needle in a vein the width of a pencil lead
        // notices. The drift is what makes "keep it steady" mean anything: the tip has to
        // be held on the vein with the mouse while the dose goes down, and every tick it
        // is off the vein is a tick of the dose going into the arm instead.
        drift += 0.35F;
        aimY += Mth.sin(drift) * 0.22F + Mth.cos(drift * 0.61F) * 0.16F;
        look();

        float push = Math.min(PLUNGE, 1.0F - plunger);
        plunger += push;
        given += push;
        if (flash) {
            givenWell += push;
        }
        if (plunger >= 1.0F) {
            finish();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || injected) {
            return false;
        }
        // Before it is in, the click also places it. After, the button is the plunger
        // and nothing moves the syringe any more.
        if (!settled) {
            aimX = (float) mouseX;
            aimY = (float) mouseY;
        }
        pushing = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || injected) {
            return false;
        }
        // The hand still steers while the dose is going in. That is the answer to the
        // drift, and it is the whole of the second half of this.
        if (settled) {
            aimY += ((float) mouseY - aimY) * 0.35F;
            aimX += ((float) mouseX - aimX) * 0.15F;
            look();
        }
        return pushing;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        pushing = false;
        // Letting go with the tip under the skin is the decision to stop going in. From
        // here the same button pushes the dose instead.
        if (!injected && under()) {
            settled = true;
        }
    }

    private float tipX() {
        return aimX - depth * AXIS_X;
    }

    private float tipY() {
        return aimY - depth * AXIS_Y;
    }

    /** Whether the tip has passed under the skin at all. */
    private boolean under() {
        int column = Math.round(tipX()) - left;
        return column >= 0 && column < width && tipY() >= skinTop[column];
    }

    /**
     * What the barrel is showing.
     *
     * <p>Geometric rather than a depth window: the tip walks toward the line of the vein,
     * touches it, and if nobody stops pushing it comes out the other side. Which is what
     * the player is watching happen.
     */
    private void look() {
        boolean was = flash;
        flash = !through && under() && onVein();
        if (flash && !was) {
            everFlashed = true;
            hiss(SoundEvents.BUCKET_FILL, 1.7F);
        }
        // Past it. Not final once the dose is going in: a hand that steers back onto the
        // vein saves the rest of the dose, which is what would happen.
        if (everFlashed && !flash && !through) {
            through = true;
            hiss(SoundEvents.PLAYER_HURT, 1.3F);
        } else if (flash) {
            through = false;
        }
    }

    private boolean onVein() {
        int column = Math.round(tipX()) - left;
        if (column < 0 || column >= width) {
            return false;
        }
        float y = tipY();
        for (int which = 0; which < 2; which++) {
            if (Math.abs(y - veinY(which, column)) <= thickness / 2.0F + 0.5F) {
                return true;
            }
        }
        return false;
    }

    /**
     * The barrel is empty, and what it was worth is the share of it that went where it
     * was meant to.
     *
     * <p>Not pass or fail: a dose is a liquid and it goes where the tip was pointing at
     * the time. Hold steady with blood in the barrel and all of it is in the circulation.
     * Slip out half way and half of it is in the arm.
     */
    private void finish() {
        injected = true;
        plunger = 1.0F;
        float share = given <= 0.0F ? 0.0F : Mth.clamp(givenWell / given, 0.0F, 1.0F);
        quality = Mth.lerp(share, 0.3F, 1.0F);
        hiss(share >= 0.85F ? SoundEvents.PLAYER_LEVELUP : SoundEvents.SLIME_SQUISH,
                share >= 0.85F ? 1.4F : 0.9F);
    }

    private void hiss(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, pitch, 0.45F));
    }

    // ---- what it looks like -----------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        if (!injected && !settled && depth <= 0.0F && !pushing) {
            aimX = Mth.clamp(mouseX, left + 8, left + width - 8);
            aimY = Mth.clamp(mouseY, top + 6, armMid - 14);
        }
        // Drawn between ticks rather than on them. The logic runs twenty times a second,
        // which is plenty for deciding anything and nowhere near enough to look like
        // movement - so what gets drawn is where it is now, not where it was at the last
        // tick, and the needle stops travelling in visible steps.
        shownDepth = Mth.lerp(partialTick, lastDepth, depth);
        shownPlunger = Mth.lerp(partialTick, lastPlunger, plunger);
        arm(graphics);
        syringe(graphics);
        say(graphics, font);
    }

    /**
     * The arm, in columns a few pixels wide rather than one.
     *
     * <p>Each column is five rectangles - the two edges, the two shaded halves and the lit
     * middle - instead of fifty separate pixels. Same picture, a fiftieth of the calls.
     */
    private void arm(GuiGraphics graphics) {
        for (int i = 0; i < width; i += COLUMN) {
            int x = left + i;
            int x1 = Math.min(left + width, x + COLUMN);
            float half = armHalf(i);
            int edge = Math.max(1, Math.round(half * 0.07F));
            int dark = Math.round(half * 0.72F);
            int lit = Math.round(half * 0.28F);
            int y0 = armMid - Math.round(half);
            int y1 = armMid + Math.round(half);
            graphics.fill(x, y0, x1, y1, SKIN_DARK);
            graphics.fill(x, armMid - dark, x1, armMid + dark, SKIN);
            graphics.fill(x, armMid - lit, x1, armMid + lit, SKIN_LIT);
            graphics.fill(x, y0, x1, y0 + edge, SKIN_LINE);
            graphics.fill(x, y1 - edge, x1, y1, SKIN_LINE);
        }

        // The veins, fainter and thinner the flatter they are.
        int fade = Math.round(255 * Mth.lerp(collapse, 0.85F, 0.30F));
        int colour = (fade << 24) | (VEIN & 0xFFFFFF);
        int half = Math.max(1, Math.round(thickness / 2.0F));
        for (int which = 0; which < 2; which++) {
            for (int i = 4; i < width - 4; i += COLUMN) {
                int y = Math.round(veinY(which, i));
                graphics.fill(left + i, y - half, left + Math.min(width - 4, i + COLUMN), y + half, colour);
            }
        }
    }

    private void syringe(GuiGraphics graphics) {
        float tx = aimX - shownDepth * AXIS_X;
        float ty = aimY - shownDepth * AXIS_Y;

        // The needle: steel in the air, a shadow under the skin. Split at the surface
        // rather than drawn a pixel at a time, which is the same picture for two calls.
        float buried = 0.0F;
        for (int i = 0; i <= NEEDLE; i++) {
            int column = Math.round(tx + AXIS_X * i) - left;
            if (column >= 0 && column < width && Math.round(ty + AXIS_Y * i) >= skinTop[column]) {
                buried = i;
            }
        }
        if (buried > 0.0F) {
            Shapes.bar(graphics, tx, ty, AXIS_X, AXIS_Y, buried, 1.0F, UNDER_SKIN);
        }
        Shapes.bar(graphics, tx + AXIS_X * buried, ty + AXIS_Y * buried,
                AXIS_X, AXIS_Y, NEEDLE - buried, 1.0F, STEEL);

        float hx = tx + AXIS_X * NEEDLE;
        float hy = ty + AXIS_Y * NEEDLE;
        Shapes.bar(graphics, hx, hy, AXIS_X, AXIS_Y, HUB, 2.5F, GLASS_SHADE);

        float bx = hx + AXIS_X * HUB;
        float by = hy + AXIS_Y * HUB;
        Shapes.bar(graphics, bx, by, AXIS_X, AXIS_Y, BARREL, 6.0F, GLASS_EDGE);
        Shapes.bar(graphics, bx, by, AXIS_X, AXIS_Y, BARREL, 5.0F, GLASS);

        float filled = BARREL * (1.0F - shownPlunger);
        if (filled > 1.0F) {
            Shapes.bar(graphics, bx + AXIS_X * (BARREL - filled), by + AXIS_Y * (BARREL - filled),
                    AXIS_X, AXIS_Y, filled - 1.0F, 4.0F, fluid);
        }
        // Last, and at the needle end: it is the thing being watched for, and under the
        // fluid it was invisible.
        if (flash) {
            Shapes.bar(graphics, bx, by, AXIS_X, AXIS_Y, 16, 5.0F, BLOOD);
            Shapes.bar(graphics, hx, hy, AXIS_X, AXIS_Y, HUB, 2.5F, BLOOD);
        }
        for (int i = 1; i < 6; i++) {
            float gx = bx + AXIS_X * (i * 9) + PERP_X * 2.0F;
            float gy = by + AXIS_Y * (i * 9) + PERP_Y * 2.0F;
            Shapes.bar(graphics, gx, gy, PERP_X, PERP_Y, 3.0F, 0.5F, GLASS_SHADE);
        }
        // The flange, and the plunger sticking out behind it.
        float fx = bx + AXIS_X * BARREL;
        float fy = by + AXIS_Y * BARREL;
        flange(graphics, fx, fy, 11.0F);
        float rod = 6.0F + 24.0F * shownPlunger;
        Shapes.bar(graphics, fx + AXIS_X * 3, fy + AXIS_Y * 3, AXIS_X, AXIS_Y,
                32.0F - rod, 1.6F, GLASS_SHADE);
        flange(graphics, fx + AXIS_X * (35.0F - rod), fy + AXIS_Y * (35.0F - rod), 7.0F);
    }

    /** A short bar across the axis: a finger flange, a thumb rest. */
    private void flange(GuiGraphics graphics, float x, float y, float half) {
        Shapes.bar(graphics, x - PERP_X * half, y - PERP_Y * half,
                PERP_X, PERP_Y, half * 2.0F, 1.5F, GLASS_EDGE);
    }

    private void say(GuiGraphics graphics, Font font) {
        Component note;
        int colour;
        if (injected) {
            note = Component.translatable(quality >= 1.0F
                    ? "triage.minigame.vein_in" : "triage.minigame.vein_tissue");
            colour = quality >= 1.0F ? GOOD : BAD;
        } else if (through) {
            note = Component.translatable("triage.minigame.vein_through");
            colour = BAD;
        } else if (flash) {
            note = Component.translatable("triage.minigame.vein_flash");
            colour = GOOD;
        } else {
            note = Component.translatable("triage.minigame.vein_searching");
            colour = 0xFFBFC6CC;
        }
        graphics.drawString(font, note, left + 2, top + height - 9, colour, false);
    }

    @Override
    public float progress() {
        if (injected) {
            return 1.0F;
        }
        // Half the bar is getting in, half is the dose going down.
        return settled ? 0.5F + plunger * 0.5F : Mth.clamp(depth / (float) NEEDLE, 0.0F, 0.5F);
    }

    @Override
    public float quality() {
        return quality;
    }

    @Override
    public boolean complete() {
        return injected;
    }

    @Override
    public boolean failed() {
        // Never. A syringe that has been pushed is an empty one, and handing the ampoule
        // back for carelessness would be forgiving something nobody forgives.
        return false;
    }

    @Override
    public Component instruction() {
        if (injected) {
            return Component.translatable("triage.minigame.vein_hint_done");
        }
        if (settled) {
            return Component.translatable(flash
                    ? "triage.minigame.vein_hint_press" : "triage.minigame.vein_hint_press_bad");
        }
        if (through) {
            return Component.translatable("triage.minigame.vein_hint_through");
        }
        if (flash) {
            return Component.translatable("triage.minigame.vein_hint_flash");
        }
        if (under()) {
            return Component.translatable("triage.minigame.vein_hint_in");
        }
        return Component.translatable("triage.minigame.vein_hint");
    }
}
