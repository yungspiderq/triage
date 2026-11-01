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
 * The operation: an open field that keeps filling with blood, and the bleeding stopped
 * one vessel at a time.
 *
 * <p>Surgery used to be three other games in a row - the antiseptic scrub, a fragment
 * being lifted out, and a run of stitches. It read as a procedure and it was a playlist,
 * and the first of the three was literally another treatment's game with the numbers
 * turned up. The most expensive item in the mod, and the answer to the worst thing that
 * can happen to a limb, had nothing of its own.
 *
 * <p>What an operation actually is, once you are inside, is a fight for the view. The
 * field fills, so you cannot see; you swab it clear, and what you find are the vessels
 * that are doing the filling; you clamp those, and the field fills more slowly, so you
 * see more, so you find the rest. Two hands doing two different jobs in a loop, and a
 * patient paying for every second of it in volume. Nothing else in this mod works like
 * that, and nothing else should.
 *
 * <p>Then it closes, and closing is not a run of clicks either: each stitch is the needle
 * going across the wound and out the other side, which is a movement, so it is a movement
 * here.
 *
 * <p>It cannot be failed. Everything that goes wrong goes wrong as blood, and the patient
 * is the one who paid it - which is what the number handed back at the end is.
 */
public final class SurgeryMinigame implements Minigame {
    private static final int BLEEDERS = 4;
    private static final int STITCHES = 6;

    /** How fast the field fills with nobody bleeding, and how much each bleeder adds. */
    private static final float RISE = 0.0016F;
    private static final float RISE_EACH = 0.0024F;
    /** How fast a swab takes it away. */
    private static final float SWAB = 0.030F;

    /** How close a click has to be to catch a vessel. */
    private static final float CLAMP_REACH = 13.0F;
    /** And how close the two ends of a stitch have to be to their marks. */
    private static final float STITCH_REACH = 12.0F;

    private static final int COLOR_SKIN = 0xFFB07A5A;
    private static final int COLOR_SKIN_EDGE = 0xFF6E4632;
    private static final int COLOR_CAVITY = 0xFF5A2422;
    private static final int COLOR_CAVITY_DEEP = 0xFF3A1414;
    private static final int COLOR_BLOOD = 0xFF8E1616;
    private static final int COLOR_BLOOD_TOP = 0xFFB52A22;
    private static final int COLOR_VESSEL = 0xFFE04A42;
    private static final int COLOR_CLAMP = 0xFFB8BCC4;
    private static final int COLOR_RETRACTOR = 0xFF9AA0A8;
    private static final int COLOR_GAUZE = 0xFFF2ECE0;
    private static final int COLOR_MARK = 0xFFFFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_THREAD = 0xFF2A2118;
    private static final int COLOR_COST = 0xFFCC3333;
    private static final int COLOR_COST_BG = 0x50FFFFFF;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int fieldLeft;
    private final int fieldRight;
    private final int fieldTop;
    private final int fieldBottom;

    private final int[] vesselX = new int[BLEEDERS];
    private final int[] vesselY = new int[BLEEDERS];
    private final boolean[] clamped = new boolean[BLEEDERS];
    private int held;

    /** How much of the field is under blood, from the bottom up. */
    private float blood = 0.55F;
    /** What the patient has paid for the view, which is the only score here. */
    private float cost;

    private boolean swabbing;
    private float handX;
    private float handY;
    private int age;

    /** Closing: which stitch is being made, and where the needle went in. */
    private int stitches;
    private boolean needleIn;
    private float needleX;
    private float needleY;
    private int missFlash;

    public SurgeryMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.fieldLeft = left + Math.max(8, (width - 150) / 2);
        this.fieldRight = left + width - Math.max(8, (width - 150) / 2);
        this.fieldTop = top + 12;
        this.fieldBottom = top + height - 22;

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        // Spread down the field rather than scattered, so clearing it uncovers them in
        // an order instead of all at once - which is what makes this a loop rather than
        // one long swab followed by four clicks.
        int span = (fieldBottom - fieldTop) - 24;
        for (int i = 0; i < BLEEDERS; i++) {
            vesselY[i] = fieldTop + 12 + Math.round(span * (i + 0.5F) / BLEEDERS)
                    + random.nextInt(9) - 4;
            vesselX[i] = fieldLeft + 16 + random.nextInt(Math.max(1, fieldRight - fieldLeft - 32));
        }
        this.handX = (fieldLeft + fieldRight) / 2.0F;
        this.handY = (fieldTop + fieldBottom) / 2.0F;
    }

    // ---- the operation ----------------------------------------------------

    @Override
    public void tick() {
        age++;
        if (missFlash > 0) {
            missFlash--;
        }
        if (closing() || complete()) {
            return;
        }
        int open = BLEEDERS - held;
        blood = Mth.clamp(blood + RISE + RISE_EACH * open
                - (swabbing && inField(handX, handY) ? SWAB : 0.0F), 0.0F, 1.0F);
        // Every second with a vessel still going costs the patient, whether or not the
        // player can see it. That is the clock: not a timer somebody set, the bleeding.
        cost += open * 0.011F;
    }

    /** The surface of the pool, in screen rows. */
    private float surface() {
        return fieldBottom - blood * (fieldBottom - fieldTop);
    }

    private boolean visible(int i) {
        return !clamped[i] && vesselY[i] <= surface();
    }

    private boolean inField(float x, float y) {
        return x >= fieldLeft && x <= fieldRight && y >= fieldTop && y <= fieldBottom;
    }

    private boolean closing() {
        return held >= BLEEDERS;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        handX = (float) mouseX;
        handY = (float) mouseY;

        if (closing()) {
            needleIn = true;
            needleX = handX;
            needleY = handY;
            return true;
        }
        // A click on a vessel is a clamp. A click on anything else is a swab, which is
        // the whole of the mode switching in this game: there is not one.
        for (int i = 0; i < BLEEDERS; i++) {
            if (visible(i) && Math.abs(handX - vesselX[i]) <= CLAMP_REACH
                    && Math.abs(handY - vesselY[i]) <= CLAMP_REACH) {
                clamped[i] = true;
                held++;
                play(SoundEvents.LEASH_KNOT_PLACE, 1.2F);
                return true;
            }
        }
        swabbing = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        handX = (float) mouseX;
        handY = (float) mouseY;
        if (!closing() && !swabbing) {
            swabbing = true;
        }
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        swabbing = false;
        if (closing() && needleIn && !complete()) {
            needleIn = false;
            stitch((float) mouseX, (float) mouseY);
        }
    }

    /**
     * One stitch: in one side, out the other, at the height the wound wants it.
     *
     * <p>Both ends are checked, which is the difference between this and clicking a dot.
     * A needle that goes in at the right place and comes out anywhere is not a stitch.
     */
    private void stitch(float outX, float outY) {
        float y = stitchY(stitches);
        boolean entered = Math.abs(needleX - entryX()) <= STITCH_REACH
                && Math.abs(needleY - y) <= STITCH_REACH;
        boolean exited = Math.abs(outX - exitX()) <= STITCH_REACH
                && Math.abs(outY - y) <= STITCH_REACH;
        if (entered && exited) {
            stitches++;
            play(SoundEvents.WOOL_HIT, 1.0F + 0.04F * stitches);
            return;
        }
        missFlash = 14;
        play(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 0.8F);
    }

    private float stitchY(int index) {
        float span = (fieldBottom - fieldTop) - 20;
        return fieldTop + 10 + span * (index + 0.5F) / STITCHES;
    }

    private int entryX() {
        return (fieldLeft + fieldRight) / 2 - 16;
    }

    private int exitX() {
        return (fieldLeft + fieldRight) / 2 + 16;
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        float first = held / (float) BLEEDERS * 0.6F;
        float second = stitches / (float) STITCHES * 0.4F;
        return Mth.clamp(first + second, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return held >= BLEEDERS && stitches >= STITCHES;
    }

    /** An operation is not abandoned halfway. Everything that goes wrong goes wrong as blood. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - cost * 0.6F, 0.0F, 1.0F);
    }

    @Override
    public Component instruction() {
        if (closing()) {
            return Component.translatable(missFlash > 0
                    ? "triage.minigame.surgery_missed" : "triage.minigame.surgery_close");
        }
        boolean anyVisible = false;
        for (int i = 0; i < BLEEDERS; i++) {
            anyVisible |= visible(i);
        }
        return Component.translatable(anyVisible
                ? "triage.minigame.surgery_clamp" : "triage.minigame.surgery_swab");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        skin(graphics);
        if (closing()) {
            closed(graphics);
        } else {
            cavity(graphics);
        }
        retractors(graphics);
        costBar(graphics);
        hand(graphics);
    }

    private void skin(GuiGraphics graphics) {
        graphics.fill(fieldLeft - 15, fieldTop - 9, fieldRight + 15, fieldBottom + 9, COLOR_SKIN_EDGE);
        Draw.body(graphics, fieldLeft - 14, fieldTop - 8, fieldRight - fieldLeft + 28,
                fieldBottom - fieldTop + 16, COLOR_SKIN);
    }

    /** The open field: the hole, what is welling up in it, and what is doing the welling. */
    private void cavity(GuiGraphics graphics) {
        graphics.fill(fieldLeft, fieldTop, fieldRight, fieldBottom, COLOR_CAVITY);
        graphics.fill(fieldLeft + 4, fieldTop + 4, fieldRight - 4, fieldBottom - 4, COLOR_CAVITY_DEEP);

        int line = Math.round(surface());
        if (line < fieldBottom) {
            graphics.fill(fieldLeft, line, fieldRight, fieldBottom, COLOR_BLOOD);
            graphics.fill(fieldLeft, line, fieldRight, line + 2, COLOR_BLOOD_TOP);
        }

        for (int i = 0; i < BLEEDERS; i++) {
            int x = vesselX[i];
            int y = vesselY[i];
            if (clamped[i]) {
                // A clamp: two jaws across the vessel, and it has stopped.
                graphics.fill(x - 7, y - 2, x + 7, y, COLOR_CLAMP);
                graphics.fill(x - 7, y + 1, x + 7, y + 3, COLOR_CLAMP);
                graphics.fill(x + 5, y - 4, x + 8, y + 5, Draw.shade(COLOR_CLAMP, 0.75F));
                continue;
            }
            if (!visible(i)) {
                continue;
            }
            // Pulsing, because that is how one is found: it is the thing that moves.
            float pulse = 0.6F + 0.4F * Mth.sin(age * 0.35F + i);
            int r = 3 + Math.round(2.0F * pulse);
            Draw.disc(graphics, x, y, r, COLOR_VESSEL);
            Draw.disc(graphics, x, y, Math.max(1, r - 2), COLOR_BLOOD_TOP);
            graphics.renderOutline(x - CLAMP_REACH_I, y - CLAMP_REACH_I,
                    CLAMP_REACH_I * 2, CLAMP_REACH_I * 2, Draw.alpha(COLOR_MARK, pulse));
        }
    }

    private static final int CLAMP_REACH_I = (int) CLAMP_REACH;

    /** Closed and being stitched: the wound as a line, with the next pass marked. */
    private void closed(GuiGraphics graphics) {
        int middle = (fieldLeft + fieldRight) / 2;
        graphics.fill(middle - 3, fieldTop, middle + 3, fieldBottom, COLOR_CAVITY);
        graphics.fill(middle - 1, fieldTop, middle + 1, fieldBottom, COLOR_CAVITY_DEEP);

        for (int i = 0; i < stitches; i++) {
            int y = Math.round(stitchY(i));
            graphics.fill(entryX(), y, exitX(), y + 1, COLOR_THREAD);
            graphics.fill(entryX(), y - 2, entryX() + 2, y + 3, COLOR_THREAD);
            graphics.fill(exitX() - 2, y - 2, exitX(), y + 3, COLOR_THREAD);
        }
        if (complete()) {
            return;
        }
        int y = Math.round(stitchY(stitches));
        int colour = missFlash > 0 ? COLOR_COST : COLOR_MARK;
        Draw.disc(graphics, entryX(), y, 3, needleIn ? COLOR_GOOD : colour);
        Draw.disc(graphics, exitX(), y, 3, colour);
        Draw.line(graphics, entryX(), y, exitX(), y, 1, Draw.alpha(colour, 0.4F));
        if (needleIn) {
            Draw.line(graphics, entryX(), y, Math.round(handX), Math.round(handY), 1, COLOR_THREAD);
        }
    }

    /** What is holding the field open, which is the one thing here nobody has to do. */
    private void retractors(GuiGraphics graphics) {
        for (int s = -1; s <= 1; s += 2) {
            int x = s < 0 ? fieldLeft - 4 : fieldRight;
            graphics.fill(x, fieldTop + 10, x + 4, fieldBottom - 10, COLOR_RETRACTOR);
            graphics.fill(x, fieldTop + 10, x + 4, fieldTop + 12, Draw.shade(COLOR_RETRACTOR, 1.2F));
        }
    }

    /** What it has cost so far, which is the only number this game keeps. */
    private void costBar(GuiGraphics graphics) {
        int barTop = top + height - 8;
        int barLeft = fieldLeft;
        int barWidth = fieldRight - fieldLeft;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 4, COLOR_COST_BG);
        int filled = Math.round(barWidth * Mth.clamp(cost * 0.6F, 0.0F, 1.0F));
        graphics.fill(barLeft, barTop, barLeft + filled, barTop + 4, COLOR_COST);
    }

    /** Gauze in the hand while swabbing, forceps while closing. */
    private void hand(GuiGraphics graphics) {
        int x = Math.round(Mth.clamp(handX, left + 5, left + width - 5));
        int y = Math.round(Mth.clamp(handY, top + 5, top + height - 5));
        if (closing()) {
            if (!needleIn) {
                return;
            }
            graphics.fill(x - 4, y - 1, x + 4, y + 1, COLOR_CLAMP);
            return;
        }
        if (!swabbing) {
            return;
        }
        graphics.fill(x - 8, y - 8, x + 8, y + 8, Draw.shade(COLOR_GAUZE, 0.86F));
        graphics.fill(x - 7, y - 7, x + 7, y + 7, COLOR_GAUZE);
        graphics.fill(x - 5, y - 3, x + 5, y + 3, Draw.alpha(COLOR_BLOOD, 0.55F));
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
