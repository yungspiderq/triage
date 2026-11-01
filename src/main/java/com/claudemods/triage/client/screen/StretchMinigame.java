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
 * Taking a joint to the edge of what it will do, and not past it.
 *
 * <p>Every other game in this mod is about a thing you can see: a wound, a bone, a gauge,
 * a needle. There is nothing to see here at all. What decides whether this works is a line
 * across a range of movement that is <b>not drawn and cannot be drawn</b>, because the only
 * thing in the world that knows where it is, is the person whose arm it is.
 *
 * <p>So they tell you. They tense before they say anything, they say something before it
 * hurts, and they shout before it tears - and that is the whole instrument. Watch the face,
 * listen to the line, and stop one step past comfortable.
 *
 * <p>Which is exactly the difference between this and the rest of the mod. Short of the
 * edge, nothing loosens: a stretch that does not reach the tissue is a stretch that did
 * nothing, and being gentle with a contracture is how people keep one for life. Past the
 * edge, something tears, and tomorrow it is tighter than it was today. The gain is in the
 * narrow band between those two, and it is found by hurting somebody carefully.
 *
 * <p>And it is why doing this to yourself is worth less. You stop at your own pain. Every
 * time.
 */
public final class StretchMinigame implements Minigame {
    /** Three sets, which is what anybody would be told to do. */
    private static final int REPS = 3;
    /** How long the joint has to be held out there for the rep to count. */
    private static final int HOLD_TICKS = 34;
    /** How far the hand travels, in pixels, for the joint to go through its whole range. */
    private static final float TRAVEL = 120.0F;
    /** How fast it comes back when nobody is holding it. */
    private static final float SPRING = 0.022F;
    /** Where the pain can start, and how much further there is before something goes. */
    private static final float EDGE_MIN = 0.42F;
    private static final float EDGE_MAX = 0.66F;
    private static final float TEAR_GAP = 0.15F;
    /** The warning before the warning: the joint tightening under the hands. */
    private static final float TENSION = 0.17F;
    /** Nobody who did three honest sets comes away with nothing. */
    private static final float FLOOR = 0.15F;

    private static final int COLOR_LIMB = 0xFFB07A5A;
    private static final int COLOR_LIMB_EDGE = 0xFF6E4632;
    private static final int COLOR_JOINT = 0xFFE8D9C0;
    private static final int COLOR_ARC = 0x33FFFFFF;
    private static final int COLOR_CALM = 0xFF7FD46B;
    private static final int COLOR_TENSE = 0xFFE0B040;
    private static final int COLOR_PAIN = 0xFFCC5533;
    private static final int COLOR_TORN = 0xFFCC3333;
    private static final int COLOR_EMPTY = 0x40FFFFFF;
    private static final int COLOR_BANKED = 0xFF7FD46B;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final RandomSource random = RandomSource.create();

    /** How far the joint is open, 0 to 1. */
    private float angle;
    /** Where this set starts hurting, and where it gives. Neither is ever drawn. */
    private float edge;
    private float tear;

    private boolean held;
    private float handX;
    private float grabX;
    private float grabAngle;

    private int holding;
    private int rep;
    private float banked;
    private boolean torn;
    private int age;
    /** How long the last shout has left on screen, so a wince reads as a wince. */
    private int flash;

    public StretchMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        nextSet();
    }

    /**
     * A fresh set, with the edge somewhere new.
     *
     * <p>Rolled again every set rather than once, because a joint that has just been taken
     * out to its limit does not have the same limit a minute later - and because finding
     * the line once and then driving straight back to it three times is not the skill this
     * is about.
     */
    private void nextSet() {
        edge = Mth.lerp(random.nextFloat(), EDGE_MIN, EDGE_MAX);
        tear = edge + TEAR_GAP;
        holding = 0;
    }

    @Override
    public void tick() {
        age++;
        if (flash > 0) {
            flash--;
        }
        if (complete() || torn) {
            return;
        }
        if (!held) {
            // Let go and it comes back on its own, which is most of the reason this
            // cannot be done by putting the mouse somewhere and walking away.
            angle = Math.max(0.0F, angle - SPRING);
            holding = Math.max(0, holding - 2);
            return;
        }
        if (angle >= tear) {
            torn = true;
            play(SoundEvents.PLAYER_HURT, 0.7F);
            return;
        }
        if (angle >= edge) {
            holding++;
            if (holding % 9 == 0) {
                play(SoundEvents.PLAYER_BREATH, 1.4F);
                flash = 6;
            }
            if (holding >= HOLD_TICKS) {
                // What the set was worth: how far into the band it was held. Right at
                // the edge is a set that happened; near the end of it is a set that did
                // something. Neither is worth anything if it goes past.
                banked += Mth.clamp((angle - edge) / TEAR_GAP, 0.0F, 1.0F);
                rep++;
                angle = 0.0F;
                held = false;
                play(SoundEvents.ARMOR_EQUIP_LEATHER, 1.2F);
                if (rep < REPS) {
                    nextSet();
                }
            }
            return;
        }
        // Short of it. Nothing loosens and the hold does not count - being gentle with a
        // contracture is how people keep one.
        holding = Math.max(0, holding - 1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete() || torn) {
            return false;
        }
        held = true;
        handX = (float) mouseX;
        grabX = (float) mouseX;
        grabAngle = angle;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!held) {
            return false;
        }
        handX = (float) mouseX;
        angle = Mth.clamp(grabAngle + (float) (mouseX - grabX) / TRAVEL, 0.0F, 1.0F);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        held = false;
    }

    @Override
    public float progress() {
        return complete() ? 1.0F : (rep + Math.min(1.0F, holding / (float) HOLD_TICKS)) / REPS;
    }

    @Override
    public float quality() {
        return torn ? 0.0F : Math.max(FLOOR, banked / REPS);
    }

    @Override
    public boolean complete() {
        return rep >= REPS;
    }

    @Override
    public boolean failed() {
        return torn;
    }

    @Override
    public Component instruction() {
        if (torn) {
            return Component.translatable("triage.minigame.stretch_torn");
        }
        if (complete()) {
            return Component.translatable("triage.minigame.stretch_done");
        }
        if (!held) {
            return Component.translatable("triage.minigame.stretch_take");
        }
        if (angle >= edge) {
            return Component.translatable("triage.minigame.stretch_hold");
        }
        if (angle >= edge - TENSION) {
            return Component.translatable("triage.minigame.stretch_nearly");
        }
        return Component.translatable("triage.minigame.stretch_further");
    }

    // ---- drawing ------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        limb(graphics);
        face(graphics);
        sets(graphics);
    }

    /**
     * The joint, opening.
     *
     * <p>The travelled range is drawn as a faint arc and the edge is not on it anywhere.
     * That is not an omission - a mark would turn this into the force gauge it deliberately
     * is not, and the whole point is that the number lives in somebody else's head.
     */
    private void limb(GuiGraphics graphics) {
        int jointX = left + width / 2 - 20;
        int jointY = top + height / 2 + 14;
        int upper = Math.min(62, width / 3);
        int lower = Math.min(58, width / 3);

        // The part that is held still, which is what a joint is measured against.
        Draw.line(graphics, jointX - upper, jointY + 10, jointX, jointY, 9, COLOR_LIMB_EDGE);
        Draw.line(graphics, jointX - upper, jointY + 10, jointX, jointY, 7, COLOR_LIMB);

        // Where the trailing end has been, so the range is visible even though the limit
        // is not.
        for (int i = 0; i <= 14; i++) {
            float along = i / 14.0F * Math.max(angle, 0.02F);
            int[] end = tip(jointX, jointY, lower, along);
            Draw.disc(graphics, end[0], end[1], 1, COLOR_ARC);
        }

        int[] end = tip(jointX, jointY, lower, angle);
        int colour = torn ? COLOR_TORN
                : angle >= edge ? COLOR_PAIN
                : angle >= edge - TENSION ? COLOR_TENSE : COLOR_LIMB;
        Draw.line(graphics, jointX, jointY, end[0], end[1], 9, COLOR_LIMB_EDGE);
        Draw.line(graphics, jointX, jointY, end[0], end[1], 7, colour);
        Draw.disc(graphics, jointX, jointY, 5, COLOR_JOINT);
        Draw.disc(graphics, end[0], end[1], 4, COLOR_LIMB_EDGE);

        // The hands, when there are hands on it.
        if (held && !complete()) {
            Draw.disc(graphics, end[0], end[1], 6, 0x66F2ECE0);
        }
    }

    /** Where the far end of the limb is, for a joint this far open. */
    private static int[] tip(int jointX, int jointY, int length, float open) {
        // From hanging straight down to swung out and up: a hundred and twenty degrees,
        // which is about what a stiff elbow or knee has to give.
        float radians = (float) Math.toRadians(80.0F - open * 120.0F);
        return new int[]{
                jointX + Math.round(Mth.cos(radians) * length),
                jointY + Math.round(Mth.sin(radians) * length)
        };
    }

    /**
     * The only instrument in the game.
     *
     * <p>It says three things and it says them in order: nothing, tightening, and that is
     * enough. The last one is where the work is, which is why the face is drawn as large
     * as it is - it is not decoration, it is the readout.
     */
    private void face(GuiGraphics graphics) {
        int cx = left + width - 34;
        int cy = top + 30;
        boolean pain = angle >= edge;
        boolean tense = !pain && angle >= edge - TENSION;
        int skin = torn ? COLOR_TORN : pain ? COLOR_PAIN : tense ? COLOR_TENSE : COLOR_CALM;

        Draw.disc(graphics, cx, cy, 15, 0x33000000);
        Draw.disc(graphics, cx, cy, 14, Draw.alpha(skin, 0.35F));
        Draw.ring(graphics, cx, cy, 14.0F, 24, 1, Draw.alpha(skin, 0.9F));

        // Eyes: open, narrowed, screwed shut.
        int eyeY = cy - 4;
        for (int side = -1; side <= 1; side += 2) {
            int ex = cx + side * 5;
            if (pain || torn) {
                graphics.fill(ex - 3, eyeY, ex + 3, eyeY + 1, skin);
            } else if (tense) {
                graphics.fill(ex - 2, eyeY - 1, ex + 2, eyeY + 1, skin);
            } else {
                graphics.fill(ex - 2, eyeY - 2, ex + 2, eyeY + 2, skin);
            }
        }
        // And the mouth, which is the bit anybody actually reads.
        int mouthY = cy + 6;
        if (torn) {
            graphics.fill(cx - 6, mouthY - 3, cx + 6, mouthY + 3, skin);
        } else if (pain) {
            int shake = (age / 2) % 2;
            graphics.fill(cx - 5, mouthY - 2 + shake, cx + 5, mouthY + 2 + shake, skin);
        } else if (tense) {
            graphics.fill(cx - 5, mouthY, cx + 5, mouthY + 1, skin);
        } else {
            graphics.fill(cx - 4, mouthY, cx + 4, mouthY + 1, skin);
            graphics.fill(cx - 5, mouthY - 1, cx - 4, mouthY, skin);
            graphics.fill(cx + 4, mouthY - 1, cx + 5, mouthY, skin);
        }
        if (flash > 0) {
            Draw.ring(graphics, cx, cy, 18.0F, 20, 1, Draw.alpha(COLOR_PAIN, flash / 6.0F));
        }
    }

    /** Which set this is, and how much of the hold is in the bank. */
    private void sets(GuiGraphics graphics) {
        int barLeft = left + 10;
        int barWidth = width - 20;
        int barTop = top + height - 10;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 4, COLOR_EMPTY);
        float share = complete() ? 1.0F : Math.min(1.0F, holding / (float) HOLD_TICKS);
        graphics.fill(barLeft, barTop, barLeft + Math.round(barWidth * share), barTop + 4,
                torn ? COLOR_TORN : COLOR_BANKED);
        for (int i = 0; i < REPS; i++) {
            int x = barLeft + i * 9;
            graphics.fill(x, barTop - 8, x + 6, barTop - 3, i < rep ? COLOR_BANKED : COLOR_EMPTY);
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
