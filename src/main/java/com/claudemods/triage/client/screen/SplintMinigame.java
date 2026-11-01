package com.claudemods.triage.client.screen;

import com.claudemods.triage.body.BodyPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Splinting: something rigid long enough to hold the break still, tied on either side of
 * it.
 *
 * <p>The game this replaces had the player pulling the broken bone back into line and
 * holding it there while it slipped. It was a good exercise and it taught the wrong
 * thing. Straightening a broken limb is not what splinting is - the instruction, in every
 * manual there is, is to splint it <em>as you find it</em>, because hauling on a broken
 * bone in a field is how a closed fracture becomes an open one. What actually decides
 * whether a splint works is much less dramatic and much easier to get wrong:
 *
 * <ul>
 *   <li><b>It has to be long enough.</b> A support that stops near the break holds
 *       nothing; the whole idea is to take the movement out of the bone by holding what
 *       is on both sides of it. Short splints are the commonest mistake made with them.
 *   <li><b>The ties go above and below, never over.</b> A strap across the break presses
 *       on the one place that must not be pressed on, and it is the second commonest.
 * </ul>
 *
 * <p>Both of those are places on a limb, so both are drawn on the limb and neither is
 * hidden: the board follows the hand up and down with a lit band showing where it has to
 * end up, and once it is on, the two bands for the ties appear on either side of the
 * break with the break itself conspicuously not lit.
 *
 * <p>The lesson of the bandage game is baked in here: every number the player has to
 * control is one they are holding still and can see, and there is one way for an attempt
 * to be refused rather than three.
 */
public final class SplintMinigame implements Minigame {
    /** Half the length of the board, and how much of the bone it has to cover each way. */
    private static final int BOARD_HALF = 32;
    private static final int MUST_COVER = 24;
    /** So the board's middle has to land within this of the break. */
    private static final int BOARD_WINDOW = BOARD_HALF - MUST_COVER;

    /** How far either side of the break a tie has to sit: clear of it, and on the board. */
    private static final int TIE_NEAR = 8;
    private static final int TIE_FAR = 26;
    private static final int TIE_HALF = 4;
    /** How far out of line a displaced break is drawn. Enough to read at a glance. */
    private static final int OUT_OF_LINE = 9;

    private static final int COLOR_LIMB = 0xFFB07A5A;
    private static final int COLOR_LIMB_EDGE = 0xFF6E4632;
    private static final int COLOR_BONE = 0xFFEFE7D2;
    private static final int COLOR_BONE_EDGE = 0xFFB6AC92;
    private static final int COLOR_BREAK = 0xFF8E1616;
    private static final int COLOR_CRACK = 0xFFA08A3C;
    private static final int COLOR_BOARD = 0xFF7A5230;
    private static final int COLOR_BOARD_LIT = 0xFF95663D;
    private static final int COLOR_TIE = 0xFFE4DCC4;
    private static final int COLOR_WINDOW = 0x44FFE070;
    private static final int COLOR_WINDOW_EDGE = 0x99FFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;
    private static final int COLOR_GUIDE = 0x50FFFFFF;

    private enum Slip {
        NONE, SHORT_HIGH, SHORT_LOW, ON_BREAK, OFF_BOARD
    }

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int centreY;
    private final int limbTop;
    private final int limbBottom;
    private final int halfWidth;
    /** True for a chest, which is not a limb and is drawn as what it is. */
    private final boolean chest;
    /** Whether the bone is actually in two pieces, or only has a line in it. */
    private final boolean broken;
    /**
     * And which of the three a break is, because they do not look alike and should not.
     *
     * <p>The picture used to be the same picture whatever was under it: two ends, always
     * three pixels out of line, whether the chart said cracked, broken or anything else.
     * That taught the wrong thing twice over - it drew a displacement onto bones that had
     * none, and it drew the same small one onto bones that were badly out.
     *
     * <p>Now the screen shows what is actually there. Ends that meet, ends that do not, or
     * an end that is outside the skin - and the last of those is not a splint job at all,
     * which is why it is drawn and then refused.
     */
    private final boolean displaced;
    private final boolean open;
    /** Where the bone has gone, which everything else is measured from. */
    private final int breakY;

    private boolean boardOn;
    private float boardY;
    private boolean holding;

    /** The two ties, by height, or Float.NaN while they are still in the pile. */
    private float tieAbove = Float.NaN;
    private float tieBelow = Float.NaN;
    private float carryX;
    private float carryY;
    private boolean carrying;

    private Slip slip = Slip.NONE;
    private int slipFlash;
    private int fumbles;

    public SplintMinigame(int left, int top, int width, int height, @Nullable BodyPart part,
                          boolean broken) {
        this(left, top, width, height, part, broken, false, false);
    }

    public SplintMinigame(int left, int top, int width, int height, @Nullable BodyPart part,
                          boolean broken, boolean displaced, boolean open) {
        this.displaced = broken && displaced;
        this.open = broken && open;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.centreX = left + width / 2;
        this.centreY = top + height / 2;
        this.chest = part == BodyPart.THORAX;
        this.broken = broken;
        this.halfWidth = chest ? 30 : 17;
        this.limbTop = centreY - 52;
        this.limbBottom = centreY + 52;
        // Not always in the middle. A splint that is the same picture every time is a
        // splint the player stops reading, and where the break is is the one thing every
        // rule here is measured from.
        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        this.breakY = centreY + random.nextInt(21) - 10;
        this.boardY = breakY;
        this.carryX = centreX + halfWidth + 30.0F;
        this.carryY = centreY + 34.0F;
    }

    @Override
    public void tick() {
        if (slipFlash > 0 && --slipFlash == 0) {
            slip = Slip.NONE;
        }
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        if (!boardOn) {
            holding = true;
            boardY = clampBoard(mouseY);
            return true;
        }
        carrying = true;
        carryX = (float) mouseX;
        carryY = (float) mouseY;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        if (!boardOn) {
            holding = true;
            boardY = clampBoard(mouseY);
            return true;
        }
        carrying = true;
        carryX = (float) mouseX;
        carryY = (float) mouseY;
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        if (!boardOn) {
            holding = false;
            layBoard();
            return;
        }
        if (carrying) {
            carrying = false;
            layTie(carryY);
            carryX = centreX + halfWidth + 30.0F;
            carryY = centreY + 34.0F;
        }
    }

    private float clampBoard(double mouseY) {
        return Mth.clamp((float) mouseY, limbTop + BOARD_HALF - 8, limbBottom - BOARD_HALF + 8);
    }

    /**
     * The board goes on, if it is long enough where it is.
     *
     * <p>Only one thing is checked and it is the thing splints are got wrong by: whether
     * what is rigid reaches far enough past the break on both sides to take the movement
     * out of it.
     */
    private void layBoard() {
        if (boardY < breakY - BOARD_WINDOW) {
            fail(Slip.SHORT_LOW);
            return;
        }
        if (boardY > breakY + BOARD_WINDOW) {
            fail(Slip.SHORT_HIGH);
            return;
        }
        boardOn = true;
        slip = Slip.NONE;
        slipFlash = 0;
        play(SoundEvents.BONE_BLOCK_PLACE, 0.9F);
    }

    /** A tie goes above or below the break, and never across it. */
    private void layTie(float y) {
        float from = y - breakY;
        if (Math.abs(from) < TIE_NEAR) {
            fail(Slip.ON_BREAK);
            return;
        }
        if (Math.abs(from) > TIE_FAR) {
            fail(Slip.OFF_BOARD);
            return;
        }
        if (from < 0.0F) {
            if (!Float.isNaN(tieAbove)) {
                fail(Slip.OFF_BOARD);
                return;
            }
            tieAbove = y;
        } else {
            if (!Float.isNaN(tieBelow)) {
                fail(Slip.OFF_BOARD);
                return;
            }
            tieBelow = y;
        }
        slip = Slip.NONE;
        slipFlash = 0;
        play(SoundEvents.LEASH_KNOT_PLACE, 1.0F);
    }

    private void fail(Slip why) {
        slip = why;
        slipFlash = 16;
        fumbles++;
        play(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 0.7F);
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        int done = (boardOn ? 1 : 0) + (Float.isNaN(tieAbove) ? 0 : 1) + (Float.isNaN(tieBelow) ? 0 : 1);
        return done / 3.0F;
    }

    @Override
    public boolean complete() {
        return boardOn && !Float.isNaN(tieAbove) && !Float.isNaN(tieBelow);
    }

    /** Nothing here is unrecoverable: a board in the wrong place is picked up again. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - fumbles * 0.12F, 0.0F, 1.0F);
    }

    @Override
    public Component instruction() {
        if (slipFlash > 0) {
            return Component.translatable(switch (slip) {
                case SHORT_HIGH -> "triage.minigame.splint_high";
                case SHORT_LOW -> "triage.minigame.splint_low";
                case ON_BREAK -> "triage.minigame.splint_on_break";
                case OFF_BOARD -> "triage.minigame.splint_off";
                case NONE -> "triage.minigame.splint_ties";
            });
        }
        return Component.translatable(boardOn ? "triage.minigame.splint_ties" : "triage.minigame.splint_board");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        board(graphics);
        limb(graphics);
        windows(graphics);
        ties(graphics);
        carried(graphics);
    }

    /** The board, under the limb, so it reads as something the limb is lying against. */
    private void board(GuiGraphics graphics) {
        int y = Math.round(boardY) - BOARD_HALF;
        int x0 = centreX - halfWidth - 8;
        int x1 = centreX + halfWidth + 8;
        int colour = boardOn ? COLOR_BOARD : COLOR_BOARD_LIT;
        Draw.vGradient(graphics, x0, y, x1 - x0, BOARD_HALF * 2,
                Draw.shade(colour, 1.06F), Draw.shade(colour, 0.84F));
        graphics.fill(x0, y, x1, y + 1, Draw.shade(colour, 1.24F));
        graphics.fill(x0, y + BOARD_HALF * 2 - 1, x1, y + BOARD_HALF * 2, Draw.shade(colour, 0.6F));
        // The grain, so a plank looks like a plank rather than a brown box.
        for (int i = 8; i < BOARD_HALF * 2; i += 11) {
            graphics.fill(x0 + 2, y + i, x1 - 2, y + i + 1, Draw.shade(colour, 0.78F));
        }
        if (!boardOn) {
            boolean good = Math.abs(boardY - breakY) <= BOARD_WINDOW;
            graphics.renderOutline(x0 - 1, y - 1, x1 - x0 + 2, BOARD_HALF * 2 + 2,
                    good ? COLOR_GOOD : COLOR_BAD);
        }
    }

    private void limb(GuiGraphics graphics) {
        // A limb with a bone out of line in it is not a rectangle, and that is the whole
        // way anybody tells the two apart across a room: the shape is wrong.
        int shift = displaced ? OUT_OF_LINE : 0;
        if (shift == 0) {
            graphics.fill(centreX - halfWidth - 1, limbTop - 1, centreX + halfWidth + 1, limbBottom + 1,
                    COLOR_LIMB_EDGE);
            Draw.body(graphics, centreX - halfWidth, limbTop, halfWidth * 2, limbBottom - limbTop, COLOR_LIMB);
        } else {
            graphics.fill(centreX - halfWidth - 1, limbTop - 1, centreX + halfWidth + 1, breakY + 1,
                    COLOR_LIMB_EDGE);
            Draw.body(graphics, centreX - halfWidth, limbTop, halfWidth * 2, breakY - limbTop, COLOR_LIMB);
            graphics.fill(centreX + shift - halfWidth - 1, breakY - 1,
                    centreX + shift + halfWidth + 1, limbBottom + 1, COLOR_LIMB_EDGE);
            Draw.body(graphics, centreX + shift - halfWidth, breakY, halfWidth * 2,
                    limbBottom - breakY, COLOR_LIMB);
        }

        // What is under the skin, which is different in each place and is the whole
        // reason this mod says a body is seven places rather than one number.
        if (chest) {
            for (int y = limbTop + 10; y < limbBottom - 8; y += 12) {
                graphics.fill(centreX - halfWidth + 4, y, centreX + halfWidth - 4, y + 3, COLOR_BONE);
                graphics.fill(centreX - halfWidth + 4, y + 3, centreX + halfWidth - 4, y + 4, COLOR_BONE_EDGE);
            }
        } else if (displaced) {
            // Two bones, because that is what is in there: one where it belongs and one
            // that has gone with the rest of the limb.
            graphics.fill(centreX - 6, limbTop + 8, centreX + 6, breakY, COLOR_BONE_EDGE);
            graphics.fill(centreX - 5, limbTop + 9, centreX + 5, breakY, COLOR_BONE);
            graphics.fill(centreX + shift - 6, breakY, centreX + shift + 6, limbBottom - 8, COLOR_BONE_EDGE);
            graphics.fill(centreX + shift - 5, breakY, centreX + shift + 5, limbBottom - 9, COLOR_BONE);
            graphics.fill(centreX - 9, limbTop + 4, centreX + 9, limbTop + 12, COLOR_BONE);
            graphics.fill(centreX + shift - 9, limbBottom - 12, centreX + shift + 9, limbBottom - 4, COLOR_BONE);
        } else {
            graphics.fill(centreX - 6, limbTop + 8, centreX + 6, limbBottom - 8, COLOR_BONE_EDGE);
            graphics.fill(centreX - 5, limbTop + 9, centreX + 5, limbBottom - 9, COLOR_BONE);
            // A knob at each end, so it reads as a bone between two joints rather than a
            // white stripe - and the joints are what the board has to reach past.
            graphics.fill(centreX - 9, limbTop + 4, centreX + 9, limbTop + 12, COLOR_BONE);
            graphics.fill(centreX - 9, limbBottom - 12, centreX + 9, limbBottom - 4, COLOR_BONE);
        }

        if (broken) {
            fracture(graphics);
        } else {
            crack(graphics);
        }
    }

    /**
     * A bone in two pieces: right across it, jagged, and with the halves out of line.
     *
     * <p>The offset is the whole difference between this drawing and the next one. A
     * break is a bone that has stopped being one thing; a crack is a bone that is still
     * one thing with a line in it, and until now the screen drew the same red gash for
     * both. Which of the two you are looking at changes nothing about the splint - the
     * board and the ties go on the same way - and it is the first thing anybody would
     * want to know, because it is the difference between a week and a month.
     */
    private void fracture(GuiGraphics graphics) {
        // Ends that meet, or ends that do not. Zero here is the honest picture of a break
        // that is where it fell: two pieces, still in line, and a splint is the whole of
        // what it wants.
        int shift = displaced ? OUT_OF_LINE : 0;
        if (open) {
            through(graphics);
            return;
        }
        graphics.fill(centreX - halfWidth + 2, breakY - 2, centreX + halfWidth - 2, breakY + 3, COLOR_BREAK);
        for (int i = -halfWidth + 3; i < halfWidth - 3; i += 3) {
            int wobble = (i / 3) % 2 == 0 ? -1 : 2;
            graphics.fill(centreX + i, breakY - 2 + wobble, centreX + i + 3, breakY + 3 + wobble, COLOR_BREAK);
        }
        // The two halves, pushed out of line with each other.
        if (!chest) {
            graphics.fill(centreX - 5 + shift, breakY - 14, centreX + 5 + shift, breakY - 2, COLOR_BONE);
            graphics.fill(centreX - 5 - shift, breakY + 3, centreX + 5 - shift, breakY + 15, COLOR_BONE);
            graphics.fill(centreX - 6 + shift, breakY - 14, centreX - 5 + shift, breakY - 2, COLOR_BONE_EDGE);
            graphics.fill(centreX + 5 - shift, breakY + 3, centreX + 6 - shift, breakY + 15, COLOR_BONE_EDGE);
        }
    }

    /**
     * The end of it, outside.
     *
     * <p>Drawn so that the refusal makes sense before anybody reads the refusal. What is
     * on the screen is a wound with bone in it, and the answer to that is not a stick.
     */
    private void through(GuiGraphics graphics) {
        int shift = OUT_OF_LINE;
        graphics.fill(centreX - halfWidth + 2, breakY - 3, centreX + halfWidth - 2, breakY + 4, COLOR_BREAK);
        // The spike: the far end, coming out of the side of the limb rather than lying
        // under it.
        for (int i = 0; i < 16; i++) {
            int x = centreX + shift + 4 + i;
            int y = breakY + 2 - i / 2;
            graphics.fill(x, y - 3, x + 2, y + 3, i % 4 == 0 ? COLOR_BONE_EDGE : COLOR_BONE);
        }
        for (int i = 0; i < 7; i++) {
            Draw.disc(graphics, centreX + shift + 6 + i * 2, breakY + 6 + (i % 3) * 3, 2, COLOR_BREAK);
        }
    }

    /**
     * A line in a bone that is still in one piece: thin, along it rather than across it,
     * and the colour the chart already uses for the same thing.
     */
    private void crack(GuiGraphics graphics) {
        int from = breakY - 13;
        int to = breakY + 13;
        int x = centreX - 2;
        for (int y = from; y < to; y += 2) {
            int wobble = ((y - from) / 2) % 3 == 0 ? 1 : 0;
            graphics.fill(x + wobble, y, x + wobble + 1, y + 2, COLOR_CRACK);
        }
        if (chest) {
            graphics.fill(centreX - halfWidth + 6, breakY, centreX + halfWidth - 6, breakY + 1, COLOR_CRACK);
        }
    }

    /**
     * The two rules, drawn where they apply.
     *
     * <p>Before the board is on, one band: where the middle of it has to end up for the
     * ends to reach past the break. After it is on, two bands with a conspicuous gap
     * between them - the gap is the break, and the gap is the rule.
     */
    private void windows(GuiGraphics graphics) {
        if (complete()) {
            return;
        }
        int x0 = centreX - halfWidth;
        int x1 = centreX + halfWidth;
        if (!boardOn) {
            band(graphics, x0, x1, breakY - BOARD_WINDOW, breakY + BOARD_WINDOW);
            // How far the ends have to reach, marked outside the limb.
            tick(graphics, breakY - MUST_COVER);
            tick(graphics, breakY + MUST_COVER);
            return;
        }
        if (Float.isNaN(tieAbove)) {
            band(graphics, x0, x1, breakY - TIE_FAR, breakY - TIE_NEAR);
        }
        if (Float.isNaN(tieBelow)) {
            band(graphics, x0, x1, breakY + TIE_NEAR, breakY + TIE_FAR);
        }
    }

    private void band(GuiGraphics graphics, int x0, int x1, int from, int to) {
        graphics.fill(x0, from, x1, to + 1, COLOR_WINDOW);
        graphics.fill(x0, from, x1, from + 1, COLOR_WINDOW_EDGE);
        graphics.fill(x0, to, x1, to + 1, COLOR_WINDOW_EDGE);
    }

    private void tick(GuiGraphics graphics, int y) {
        graphics.fill(centreX - halfWidth - 13, y, centreX - halfWidth - 9, y + 1, COLOR_GUIDE);
        graphics.fill(centreX + halfWidth + 9, y, centreX + halfWidth + 13, y + 1, COLOR_GUIDE);
    }

    private void ties(GuiGraphics graphics) {
        if (!Float.isNaN(tieAbove)) {
            tie(graphics, Math.round(tieAbove), COLOR_TIE);
        }
        if (!Float.isNaN(tieBelow)) {
            tie(graphics, Math.round(tieBelow), COLOR_TIE);
        }
    }

    private void tie(GuiGraphics graphics, int y, int colour) {
        int x0 = centreX - halfWidth - 10;
        int x1 = centreX + halfWidth + 10;
        Draw.vGradient(graphics, x0, y - TIE_HALF, x1 - x0, TIE_HALF * 2,
                Draw.shade(colour, 1.1F), Draw.shade(colour, 0.82F));
        graphics.fill(x0, y - TIE_HALF, x1, y - TIE_HALF + 1, Draw.shade(colour, 1.25F));
        graphics.fill(x0, y + TIE_HALF - 1, x1, y + TIE_HALF, Draw.shade(colour, 0.6F));
        // The knot, off to one side, because a strap tied over the bone is a strap tied
        // on the sore bit.
        graphics.fill(x1 - 6, y - TIE_HALF - 2, x1 - 1, y + TIE_HALF + 2, Draw.shade(colour, 0.72F));
    }

    /** The next tie, either in the hand or waiting in the pile beside the limb. */
    private void carried(GuiGraphics graphics) {
        if (!boardOn || complete()) {
            return;
        }
        int x = Math.round(carrying ? carryX : centreX + halfWidth + 30.0F);
        int y = Math.round(carrying ? carryY : centreY + 34.0F);
        Draw.vGradient(graphics, x - 12, y - TIE_HALF, 24, TIE_HALF * 2,
                Draw.shade(COLOR_TIE, 1.1F), Draw.shade(COLOR_TIE, 0.82F));
        graphics.fill(x - 12, y - TIE_HALF, x + 12, y - TIE_HALF + 1, Draw.shade(COLOR_TIE, 1.25F));
        if (carrying) {
            float from = Math.abs(carryY - breakY);
            boolean good = from >= TIE_NEAR && from <= TIE_FAR
                    && Float.isNaN(carryY < breakY ? tieAbove : tieBelow);
            graphics.renderOutline(x - 13, y - TIE_HALF - 1, 26, TIE_HALF * 2 + 2,
                    good ? COLOR_GOOD : COLOR_BAD);
        }
        if (slipFlash > 0) {
            graphics.renderOutline(x - 14, y - TIE_HALF - 2, 28, TIE_HALF * 2 + 4, COLOR_BAD);
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
