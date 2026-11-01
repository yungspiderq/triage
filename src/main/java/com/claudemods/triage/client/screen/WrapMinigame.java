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
 * Dressing a wound: the roll goes across the limb, and each pass is a turn of cloth.
 *
 * <p>This went through two wrong shapes before it got here, and both of them were wrong
 * in a way worth writing down.
 *
 * <p>The first asked for three circles round the limb and counted nothing else. The
 * gesture was right and everything that makes bandaging <em>bandaging</em> was missing:
 * it did not care whether anything was on the wound, where the turns landed, or how hard
 * they were pulled, so the only thing anybody could learn from it was how to rotate a
 * mouse.
 *
 * <p>The second kept the circling and added the rules - and could not show them. Where a
 * turn lands is a height, and a hand going round in circles has no steady height: it
 * swings through forty pixels twice a second. Every attempt to draw the turn that was
 * about to be laid drew something flying up and down the arm, because that is genuinely
 * what the hand was doing. The arithmetic was not the problem. The gesture was: it put
 * the one number the player had to control on the axis they had least control of.
 *
 * <p>So the roll crosses instead. Take it out past one side, sweep it across, and that is
 * one turn - laid at exactly the height the roll is at, which is a height the hand is
 * holding still and can see. Then out the other side and back for the next, a little
 * higher. The cloth going round the back is the half that is not drawn, which is fine:
 * that half never needed playing.
 *
 * <p>What is left is what a dressing actually is. Something on the wound first, because
 * gauze over a bare wound is a bandage with a wound under it. Turns that travel, each
 * over half the last, because far apart leaves it open and staying still covers nothing.
 * And a pace, because gauze snatched off the roll goes on crooked.
 *
 * <p>A tourniquet comes through the same class and inverts nearly all of it, which is the
 * neatest thing about the pair: it goes <em>above</em> the wound, between it and the
 * heart; it must not travel at all, because a strap that walks up an arm is a bandage and
 * occludes nothing; and it cannot be pulled too hard, because too tight is the whole idea.
 * Same hands, opposite rules.
 */
public final class WrapMinigame implements Minigame {
    private static final int LIMB_HALF_WIDTH = 17;
    private static final int LIMB_HALF_HEIGHT = 52;
    /** One turn of cloth, as drawn and as counted for overlap. */
    private static final float BAND = 9.0F;

    /** How far out to the side the roll has to be carried before a pass counts. */
    private static final float PASS_LINE = 34.0F;

    /** Closer than this to the last turn and the roll has not gone anywhere. */
    private static final float MIN_STEP = 2.5F;
    /** Further than this and there is a stripe of open wound between the two. */
    private static final float MAX_STEP = 8.0F;
    /** How far a strap may wander and still be a strap rather than a spiral. */
    private static final float STRAP_DRIFT = 4.0F;
    private static final int STRAP_TURNS = 3;

    /** A pass finished faster than this was snatched, and gauze goes on crooked. */
    private static final int MIN_PASS_TICKS = 5;

    /** How close the pad has to land to count as on the wound. */
    private static final float PAD_REACH = 15.0F;
    private static final int PAD_HALF = 9;

    private static final int COLOR_LIMB = 0xFFB07A5A;
    private static final int COLOR_LIMB_EDGE = 0xFF6E4632;
    private static final int COLOR_LIMB_BLUE = 0xFF6E5A8A;
    private static final int COLOR_WOUND = 0xFF8E1616;
    private static final int COLOR_BAND = 0xFFF2ECE0;
    private static final int COLOR_STRAP = 0xFF6B4A28;
    private static final int COLOR_PAD = 0xFFFBF7EE;
    private static final int COLOR_PAD_EDGE = 0xFFC9C0AF;
    private static final int COLOR_GUIDE = 0x50FFFFFF;
    private static final int COLOR_WINDOW = 0x44FFE070;
    private static final int COLOR_WINDOW_EDGE = 0x99FFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;

    /** What went wrong with the last turn, for one moment. */
    private enum Slip {
        NONE, GAP, SAME, TORN, DRIFT, SHORT
    }

    private final boolean strap;
    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int centreY;
    private final int limbTop;
    private final int limbBottom;
    /** Where the first turn goes, and where the last one has to reach. */
    private final float anchorY;
    private final float finishY;

    /** The pad, until it is on the wound. */
    private boolean padPlaced;
    private float padX;
    private float padY;
    private boolean padHeld;

    /** Turns already laid, as the height of each, and which went on badly. */
    private final float[] turns = new float[24];
    private final boolean[] rough = new boolean[24];
    private int turnCount;

    /** Where the roll is, and whether a hand is on it. */
    private boolean held;
    private float rollX;
    private float rollY;
    /** Which side of the limb the roll was last carried out past, or 0. */
    private float readySide;
    /** Which side it was on last frame, so a crossing can be spotted. */
    private float lastSide;
    private int passTicks;

    private Slip slip = Slip.NONE;
    private int slipFlash;
    /** How many turns went on snatched. Nothing here fails; this is what it costs. */
    private int torn;

    public WrapMinigame(int left, int top, int width, int height) {
        this(left, top, width, height, false);
    }

    /** A tourniquet is not a dressing and does almost nothing a dressing does. */
    public static WrapMinigame forTourniquet(int left, int top, int width, int height) {
        return new WrapMinigame(left, top, width, height, true);
    }

    private WrapMinigame(int left, int top, int width, int height, boolean strap) {
        this.strap = strap;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.centreX = left + width / 2;
        this.centreY = top + height / 2;
        this.limbTop = centreY - LIMB_HALF_HEIGHT;
        this.limbBottom = centreY + LIMB_HALF_HEIGHT;
        // A dressing is laid from below the wound to above it. A strap has one line and
        // it is above the wound, because the point of it is what lies between the strap
        // and the heart.
        this.anchorY = strap ? centreY - 22.0F : centreY + 11.0F;
        this.finishY = strap ? centreY - 22.0F : centreY - 15.0F;
        this.padX = centreX + 52.0F;
        this.padY = centreY + 36.0F;
        this.rollX = centreX + PASS_LINE + 6.0F;
        this.rollY = anchorY;
        this.readySide = 1.0F;
        this.lastSide = 1.0F;
    }

    @Override
    public void tick() {
        if (slipFlash > 0 && --slipFlash == 0) {
            slip = Slip.NONE;
        }
        if (held) {
            passTicks++;
        }
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (needsPad()) {
            padHeld = true;
            padX = (float) mouseX;
            padY = (float) mouseY;
            return true;
        }
        held = true;
        take(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        if (padHeld) {
            padX = (float) mouseX;
            padY = (float) mouseY;
            return true;
        }
        if (needsPad()) {
            return false;
        }
        held = true;
        take(mouseX, mouseY);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        if (padHeld) {
            padHeld = false;
            if (Math.abs(padX - centreX) <= PAD_REACH && Math.abs(padY - centreY) <= PAD_REACH) {
                padPlaced = true;
                padX = centreX;
                padY = centreY;
                play(SoundEvents.WOOL_PLACE, 1.1F);
            } else {
                padX = centreX + 52.0F;
                padY = centreY + 36.0F;
            }
            return;
        }
        held = false;
    }

    /**
     * The roll follows the hand, and a pass across the limb lays a turn.
     *
     * <p>A pass is not "the cursor went past the middle". It is out past one side, across,
     * and that is deliberate: waggling over the centre of the arm is not winding anything
     * round it, and the two marks on the sides say how far out is far enough.
     *
     * <p>The height a turn lands at is simply the height the roll is at. There is nothing
     * else to it, and after two versions that computed it out of a circling hand, that is
     * the entire point.
     */
    private void take(double mouseX, double mouseY) {
        rollX = Mth.clamp((float) mouseX, left + 7, left + width - 7);
        rollY = Mth.clamp((float) mouseY, limbTop + 4, limbBottom - 4);

        float from = rollX - centreX;
        float side = Math.signum(from);
        if (Math.abs(from) >= PASS_LINE) {
            if (readySide != side) {
                readySide = side;
                passTicks = 0;
            }
        } else if (side != 0.0F && side != lastSide && lastSide == readySide) {
            // Came across, from the side it had been carried out to.
            lay(rollY);
            readySide = 0.0F;
            passTicks = 0;
        }
        if (side != 0.0F) {
            lastSide = side;
        }
    }

    /**
     * The band of limb the next turn has to land on.
     *
     * <p>One method, asked by the rule and by the drawing both, so what is lit on the
     * screen and what is accepted can never be two different things.
     */
    private float windowTop() {
        if (turnCount == 0) {
            return strap ? centreY - 30.0F : centreY + 5.0F;
        }
        return strap ? turns[0] - STRAP_DRIFT : turns[turnCount - 1] - MAX_STEP;
    }

    private float windowBottom() {
        if (turnCount == 0) {
            return strap ? centreY - 14.0F : centreY + 18.0F;
        }
        return strap ? turns[0] + STRAP_DRIFT : turns[turnCount - 1] - MIN_STEP;
    }

    /** One turn of cloth, at the height the roll crossed at. */
    private void lay(float at) {
        // Snatched. It still goes on - because it does - and it goes on crooked. This is
        // the one thing here that costs rather than refuses: a game with two ways to lose
        // a turn had one way too many.
        boolean snatched = !strap && passTicks < MIN_PASS_TICKS;
        if (at > windowBottom()) {
            fail(strap ? Slip.DRIFT : Slip.SAME);
            return;
        }
        if (at < windowTop()) {
            fail(strap ? Slip.DRIFT : Slip.GAP);
            return;
        }
        if (turnCount < turns.length) {
            rough[turnCount] = snatched;
            turns[turnCount++] = at;
        }
        if (snatched) {
            torn++;
        }
        slip = snatched ? Slip.TORN : Slip.NONE;
        slipFlash = snatched ? 16 : 0;
        play(snatched ? SoundEvents.WOOL_BREAK : SoundEvents.WOOL_HIT,
                snatched ? 0.7F : 0.85F + 0.05F * turnCount);
    }

    /**
     * A pass that did not lay anything.
     *
     * <p>Nothing is lost except the pass, and the window stays where it was, so the next
     * one is measured from the same place rather than from the mistake.
     */
    private void fail(Slip why) {
        slip = why;
        slipFlash = 16;
        play(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 0.7F);
    }

    private boolean needsPad() {
        return !strap && !padPlaced;
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        if (strap) {
            return Mth.clamp(turnCount / (float) STRAP_TURNS, 0.0F, 1.0F);
        }
        if (!padPlaced) {
            return 0.0F;
        }
        if (turnCount == 0) {
            return 0.05F;
        }
        float travelled = anchorY - turns[turnCount - 1];
        return Mth.clamp(0.05F + 0.95F * (travelled / (anchorY - finishY)), 0.05F, 1.0F);
    }

    @Override
    public boolean complete() {
        if (strap) {
            return turnCount >= STRAP_TURNS;
        }
        return padPlaced && turnCount > 0 && turns[turnCount - 1] <= finishY;
    }

    /** Nothing about a dressing is unrecoverable. A wasted pass is a wasted pass. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - torn * 0.15F, 0.0F, 1.0F);
    }

    /**
     * The one line, and the only one.
     *
     * <p>An earlier version drew its own caption across the top of the box as well, which
     * put the same sentence on screen twice - once inside the frame, where it was wider
     * than the frame, and once under the panel where the screen prints it anyway. So the
     * mistakes come through here: for a moment after a pass goes wrong the line says which
     * way it went wrong, and then it goes back to the rule.
     */
    @Override
    public Component instruction() {
        if (slipFlash > 0) {
            return Component.translatable(switch (slip) {
                case GAP -> "triage.minigame.wrap_gap";
                case SAME -> "triage.minigame.wrap_same";
                case TORN -> "triage.minigame.wrap_torn";
                case DRIFT -> "triage.minigame.strap_drift";
                case SHORT, NONE -> "triage.minigame.wrap_turn";
            });
        }
        if (strap) {
            return Component.translatable("triage.minigame.strap_turn",
                    Math.max(0, STRAP_TURNS - turnCount));
        }
        if (!padPlaced) {
            return Component.translatable("triage.minigame.wrap_pad");
        }
        if (turnCount == 0) {
            return Component.translatable("triage.minigame.wrap_start");
        }
        return Component.translatable("triage.minigame.wrap_turn");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        limb(graphics);
        wound(graphics);
        aim(graphics);
        cloth(graphics);
        pad(graphics);
        roll(graphics);
    }

    private void limb(GuiGraphics graphics) {
        graphics.fill(centreX - LIMB_HALF_WIDTH - 1, limbTop - 1,
                centreX + LIMB_HALF_WIDTH + 1, limbBottom + 1, COLOR_LIMB_EDGE);
        Draw.body(graphics, centreX - LIMB_HALF_WIDTH, limbTop,
                LIMB_HALF_WIDTH * 2, LIMB_HALF_HEIGHT * 2, COLOR_LIMB);

        // Below a strap nothing is getting through, and it shows. Not a penalty - it is
        // the thing a tourniquet does, and it is why one has a clock on it.
        if (strap && turnCount > 0) {
            int from = Math.round(turns[0]) + 5;
            Draw.body(graphics, centreX - LIMB_HALF_WIDTH, from,
                    LIMB_HALF_WIDTH * 2, limbBottom - from, COLOR_LIMB_BLUE);
        }

        // How far out the roll has to be carried for the next pass to count. Two faint
        // posts, because "round the limb" is a distance and this is the distance.
        if (!complete() && !needsPad()) {
            for (int s = -1; s <= 1; s += 2) {
                int x = centreX + Math.round(PASS_LINE) * s;
                int colour = readySide == s ? COLOR_GOOD : COLOR_GUIDE;
                graphics.fill(x - 1, limbTop + 8, x + 1, limbBottom - 8, colour);
            }
        }
    }

    /** A torn opening rather than a red rectangle: dark at the edge, wet in the middle. */
    private void wound(GuiGraphics graphics) {
        if (padPlaced) {
            return;
        }
        for (int row = -6; row <= 4; row++) {
            int half = 11 - Math.abs(row + 1) * 2;
            if (half <= 0) {
                continue;
            }
            graphics.fill(centreX - half, centreY + row, centreX + half, centreY + row + 1, COLOR_WOUND);
        }
        graphics.fill(centreX - 6, centreY - 3, centreX + 6, centreY + 1, 0xFFC42A2A);
        graphics.fill(centreX - 4, centreY - 2, centreX + 4, centreY - 1, 0xFFE04A42);
        // A ring round it while there is nothing on it, so the first thing to do is the
        // first thing you see.
        graphics.renderOutline(centreX - 13, centreY - 9, 26, 17, COLOR_WINDOW_EDGE);
    }

    /**
     * Where the next turn has to go, lit on the limb, and where it would go right now.
     *
     * <p>The pending turn sits at the height of the roll and nowhere else. An earlier
     * version worked it out from a hand going round in circles, which meant it flew up
     * and down the arm forty pixels at a time - it was showing the truth, and the truth
     * was that the gesture had put the one number the player needed on the axis they
     * could not hold still.
     */
    private void aim(GuiGraphics graphics) {
        if (complete() || needsPad()) {
            return;
        }
        int from = Math.round(windowTop());
        int to = Math.round(windowBottom());
        int x0 = centreX - LIMB_HALF_WIDTH;
        int x1 = centreX + LIMB_HALF_WIDTH;
        graphics.fill(x0, from, x1, to + 1, COLOR_WINDOW);
        graphics.fill(x0, from, x1, from + 1, COLOR_WINDOW_EDGE);
        graphics.fill(x0, to, x1, to + 1, COLOR_WINDOW_EDGE);

        boolean good = rollY >= windowTop() && rollY <= windowBottom();
        int y = Math.round(rollY - BAND / 2.0F);
        graphics.renderOutline(x0 - 2, y, LIMB_HALF_WIDTH * 2 + 4, Math.round(BAND),
                good ? COLOR_GOOD : COLOR_BAD);

        if (!strap) {
            mark(graphics, finishY, LIMB_HALF_WIDTH + 7, COLOR_GUIDE);
        }
    }

    private void mark(GuiGraphics graphics, float y, int outer, int colour) {
        int row = Math.round(y);
        graphics.fill(centreX - outer - 4, row, centreX - outer, row + 1, colour);
        graphics.fill(centreX + outer, row, centreX + outer + 4, row + 1, colour);
    }

    /**
     * The turns, in the order they went on.
     *
     * <p>Each one lit along its top edge and shadowed along the bottom, so a spiral reads
     * as cloth laid over cloth rather than as a stack of bars - and so a gap, when
     * somebody leaves one, is visible as the limb showing through it.
     */
    private void cloth(GuiGraphics graphics) {
        int base = strap ? COLOR_STRAP : COLOR_BAND;
        int thickness = Math.round(BAND);
        for (int i = 0; i < turnCount; i++) {
            int y = Math.round(turns[i] - BAND / 2.0F);
            int edgeLeft = centreX - LIMB_HALF_WIDTH - 2 + (i % 2);
            int edgeRight = centreX + LIMB_HALF_WIDTH + 2 - (i % 2);
            // A turn that was snatched on goes on crumpled and grubby, so a sloppy
            // dressing looks like one afterwards instead of only having been a message.
            int shade = rough[i] ? Draw.shade(base, 0.74F) : base;
            Draw.vGradient(graphics, edgeLeft, y, edgeRight - edgeLeft, thickness,
                    Draw.shade(shade, 1.08F), Draw.shade(shade, 0.82F));
            graphics.fill(edgeLeft, y, edgeRight, y + 1, Draw.shade(shade, 1.22F));
            graphics.fill(edgeLeft, y + thickness - 1, edgeRight, y + thickness, Draw.shade(shade, 0.62F));
        }
    }

    /** The dressing itself, waiting to be dragged on. Once it is on, the turns cover it. */
    private void pad(GuiGraphics graphics) {
        if (strap || padPlaced) {
            return;
        }
        int x = Math.round(padX);
        int y = Math.round(padY);
        graphics.fill(x - PAD_HALF - 1, y - PAD_HALF - 1, x + PAD_HALF + 1, y + PAD_HALF + 1, COLOR_PAD_EDGE);
        Draw.vGradient(graphics, x - PAD_HALF, y - PAD_HALF, PAD_HALF * 2, PAD_HALF * 2,
                COLOR_PAD, Draw.shade(COLOR_PAD, 0.88F));
        for (int i = -PAD_HALF + 2; i < PAD_HALF - 1; i += 3) {
            graphics.fill(x - PAD_HALF + 2, y + i, x + PAD_HALF - 2, y + i + 1,
                    Draw.alpha(COLOR_PAD_EDGE, 0.5F));
        }
    }

    /**
     * The roll, and the gauze coming off it.
     *
     * <p>Drawn whether or not anybody has hold of it, and with the tail already running
     * to the arm, because that is the whole instruction: here is the cloth, one end of it
     * is on the limb, take the other end across.
     */
    private void roll(GuiGraphics graphics) {
        if (complete() || needsPad()) {
            return;
        }
        int x = Math.round(rollX);
        int y = Math.round(rollY);
        int base = strap ? COLOR_STRAP : COLOR_BAND;

        int anchorAt = Math.round(turnCount > 0 ? turns[turnCount - 1] : anchorY);
        int side = x < centreX ? centreX - LIMB_HALF_WIDTH : centreX + LIMB_HALF_WIDTH;
        Draw.line(graphics, x, y, side, anchorAt, 3, Draw.shade(base, 0.78F));
        Draw.line(graphics, x, y, side, anchorAt, 1, Draw.shade(base, 1.10F));

        Draw.vGradient(graphics, x - 6, y - 5, 12, 10, Draw.shade(base, 1.12F), Draw.shade(base, 0.80F));
        graphics.fill(x - 6, y - 5, x + 6, y - 4, Draw.shade(base, 1.28F));
        graphics.fill(x - 6, y + 4, x + 6, y + 5, Draw.shade(base, 0.58F));
        graphics.fill(x - 2, y - 3, x + 2, y + 3, Draw.shade(base, 0.66F));
        if (slipFlash > 0) {
            graphics.renderOutline(x - 7, y - 6, 14, 12, COLOR_BAD);
        }
        if (held) {
            graphics.renderOutline(x - 7, y - 6, 14, 12, COLOR_GOOD);
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
