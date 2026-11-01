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
 * Putting somebody under, by watching them rather than by watching a number.
 *
 * <p>Every other game in this mod has a lit band somewhere: get the thing into it and
 * hold it there. This one does not, and the absence is the point. An open-drop mask is
 * given the way it was actually given - a drop at a time onto gauze over the face, with
 * your eyes on the patient - and what tells you where they are is the patient. Breathing
 * quick and eyelids moving means not enough. Slow and even and still means there. Slower,
 * and slower, and then a gap where a breath should have been, means you have gone past it
 * and the next thing that happens is nothing.
 *
 * <p>So the readout is a chest that rises and a pair of eyelids, and the only control is
 * a bottle you tip one drop at a time. The depth falls on its own the whole while, which
 * is why it cannot be set and left: ether goes out through the same lungs it went in
 * through, and keeping somebody under is a thing done continuously for as long as the
 * operation takes.
 *
 * <p>Nothing here fails on the screen. Giving too much is not a fail state, it is a
 * <em>result</em> - it is handed to the server as a bad number and what happens next is
 * the patient not breathing, which this mod already knows what to do about.
 */
public final class MaskMinigame implements Minigame {
    /** How deep they have to be, and how much either way still counts. */
    private static final float TARGET = 0.62F;
    private static final float BAND = 0.14F;
    /** Past this the breathing is going, and past the top of the scale it has gone. */
    private static final float DANGER = 0.86F;

    /** What one drop is worth, and how fast it wears off. */
    private static final float DROP = 0.085F;
    private static final float FADE = 0.0042F;
    /** How long they have to be held there. */
    private static final int NEEDED = 90;

    private static final int COLOR_FACE = 0xFFB07A5A;
    private static final int COLOR_FACE_EDGE = 0xFF6E4632;
    private static final int COLOR_BROW = 0xFF6E4632;
    private static final int COLOR_EYE = 0xFF2A2118;
    private static final int COLOR_LID = 0xFFA06E52;
    private static final int COLOR_GAUZE = 0xFFF2ECE0;
    private static final int COLOR_GAUZE_WET = 0xFFD8DCE2;
    private static final int COLOR_CHEST = 0xFFC08A6A;
    private static final int COLOR_BOTTLE = 0xFFB8CBD2;
    private static final int COLOR_BOTTLE_EDGE = 0xFF7A8288;
    private static final int COLOR_DROP = 0xC0DDEFF2;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_LIGHT = 0xFFFFE070;
    private static final int COLOR_BAD = 0xFFCC3333;
    private static final int COLOR_EMPTY = 0x40FFFFFF;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int faceX;
    private final int faceY;
    private final int chestX;

    /** How far under they are. Never shown as a number, because it never is one. */
    private float depth;
    private int steady;
    private int age;
    /** Where in the breath they are, which is the readout. */
    private float breath;
    private int drops;
    private int overdone;
    private boolean bottleHeld;
    private int dripFor;

    public MaskMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        this.left = areaLeft;
        this.top = areaTop;
        this.width = areaWidth;
        this.height = areaHeight;
        this.faceX = areaLeft + Math.round(areaWidth * 0.34F);
        this.faceY = areaTop + Math.round(areaHeight * 0.44F);
        this.chestX = areaLeft + Math.round(areaWidth * 0.74F);
    }

    // ---- the patient ------------------------------------------------------

    @Override
    public void tick() {
        age++;
        if (dripFor > 0) {
            dripFor--;
        }
        if (complete()) {
            return;
        }
        // It goes out through the same lungs it went in through, the whole time.
        depth = Mth.clamp(depth - FADE, 0.0F, 1.2F);
        // Breathing: quick and shallow while they are light, slow and long once they are
        // under, and stalling once they are past it.
        float rate = 0.30F - 0.20F * Mth.clamp(depth / DANGER, 0.0F, 1.0F);
        breath += Math.max(0.02F, rate);
        if (breath > Mth.TWO_PI) {
            breath -= Mth.TWO_PI;
            if (depth < TARGET - BAND) {
                play(SoundEvents.PLAYER_BREATH, 1.2F);
            }
        }
        if (right()) {
            steady++;
        } else if (steady > 0 && depth > DANGER) {
            // Too deep is not merely not-progress. It is the thing that undoes it.
            steady = Math.max(0, steady - 2);
        }
        if (depth > DANGER) {
            overdone++;
        }
    }

    private boolean right() {
        return Math.abs(depth - TARGET) <= BAND;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        bottleHeld = true;
        drip();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return button == 0 && bottleHeld;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            bottleHeld = false;
        }
    }

    /**
     * One drop.
     *
     * <p>One per click, on purpose. A held-down button would make this a slider with the
     * numbers hidden, which is the worst of both - the whole reason it is drops is that
     * each one is a decision taken while looking at a face.
     */
    private void drip() {
        depth = Mth.clamp(depth + DROP, 0.0F, 1.2F);
        drops++;
        dripFor = 8;
        play(SoundEvents.BOTTLE_EMPTY, 1.6F);
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        return Mth.clamp(steady / (float) NEEDED, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return steady >= NEEDED;
    }

    /**
     * Never on the screen.
     *
     * <p>Giving too much is not a fail state, it is a result: it goes out as a bad number
     * and what happens to the patient happens on the server, where the rest of this mod
     * keeps its consequences.
     */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - overdone / 45.0F, 0.0F, 1.0F);
    }

    @Override
    public Component instruction() {
        if (depth > DANGER) {
            return Component.translatable("triage.minigame.mask_deep");
        }
        if (depth < TARGET - BAND) {
            return Component.translatable("triage.minigame.mask_light");
        }
        return Component.translatable("triage.minigame.mask_there");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        face(graphics);
        chest(graphics);
        bottle(graphics, mouseX, mouseY);
        held(graphics);
    }

    /**
     * The face, and the eyelids that are the first sign of anything.
     *
     * <p>Light: they move. Under: they are still. Too far: they are still and so is
     * everything else, which is what the chest is for.
     */
    private void face(GuiGraphics graphics) {
        graphics.fill(faceX - 23, faceY - 27, faceX + 23, faceY + 27, COLOR_FACE_EDGE);
        Draw.body(graphics, faceX - 22, faceY - 26, 44, 52, COLOR_FACE);

        boolean light = depth < TARGET - BAND;
        boolean flutter = light && (age / 4) % 3 == 0;
        for (int side = -1; side <= 1; side += 2) {
            int x = faceX + side * 10;
            graphics.fill(x - 6, faceY - 14, x + 6, faceY - 12, COLOR_BROW);
            if (flutter) {
                // Half open, which is the thing you are watching for.
                graphics.fill(x - 5, faceY - 8, x + 5, faceY - 5, COLOR_EYE);
                graphics.fill(x - 5, faceY - 5, x + 5, faceY - 3, COLOR_LID);
            } else {
                graphics.fill(x - 5, faceY - 8, x + 5, faceY - 3, COLOR_LID);
                graphics.fill(x - 5, faceY - 4, x + 5, faceY - 3, COLOR_BROW);
            }
        }

        // The gauze over the nose and mouth, wet where the drops have gone.
        int wet = Math.round(Mth.clamp(depth, 0.0F, 1.0F) * 9.0F);
        graphics.fill(faceX - 16, faceY + 2, faceX + 16, faceY + 22, COLOR_GAUZE);
        if (wet > 0) {
            graphics.fill(faceX - wet, faceY + 4, faceX + wet, faceY + 20, COLOR_GAUZE_WET);
        }
        for (int i = -14; i < 15; i += 5) {
            graphics.fill(faceX + i, faceY + 2, faceX + i + 1, faceY + 22,
                    Draw.alpha(COLOR_BOTTLE_EDGE, 0.25F));
        }
        if (dripFor > 0) {
            Draw.disc(graphics, faceX, faceY + 6 - dripFor, 2, COLOR_DROP);
        }
    }

    /**
     * The chest, which is the whole readout.
     *
     * <p>Fast and shallow while they are light; slow and full once they are under; and
     * once they are past it, a rise that does not come. There is no bar anywhere on this
     * screen because there is no bar in the room.
     */
    private void chest(GuiGraphics graphics) {
        float wave = Mth.sin(breath);
        float size = 1.0F - 0.45F * Mth.clamp(depth / DANGER, 0.0F, 1.0F);
        int rise = Math.round((wave * 0.5F + 0.5F) * 9.0F * size);
        if (depth > DANGER) {
            // A gap where a breath should have been.
            rise = (age / 20) % 3 == 0 ? rise : 0;
        }
        graphics.fill(chestX - 29, faceY - 20 - rise, chestX + 29, faceY + 30, COLOR_FACE_EDGE);
        Draw.body(graphics, chestX - 28, faceY - 19 - rise, 56, 48 + rise, COLOR_CHEST);

        // How long they have been held there, which is the only number kept.
        int barLeft = left + 12;
        int barTop = top + height - 8;
        int barWidth = width - 24;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 4, COLOR_EMPTY);
        graphics.fill(barLeft, barTop, barLeft + Math.round(barWidth * progress()), barTop + 4,
                depth > DANGER ? COLOR_BAD : COLOR_GOOD);
    }

    /** The bottle, and a count of what has gone on the mask. */
    private void bottle(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = left + 16;
        int y = top + 16;
        graphics.fill(x - 8, y - 2, x + 8, y + 26, COLOR_BOTTLE_EDGE);
        graphics.fill(x - 7, y - 1, x + 7, y + 25, COLOR_BOTTLE);
        graphics.fill(x - 4, y - 8, x + 4, y - 1, COLOR_BOTTLE_EDGE);
        int inside = Math.max(0, 22 - drops);
        graphics.fill(x - 6, y + 24 - inside, x + 6, y + 24, Draw.alpha(COLOR_DROP, 0.8F));
        if (bottleHeld) {
            graphics.renderOutline(x - 9, y - 9, 18, 36, COLOR_LIGHT);
        }
    }

    /** A ring round the face while they are where they should be, and nothing else. */
    private void held(GuiGraphics graphics) {
        if (complete()) {
            return;
        }
        int colour = depth > DANGER ? COLOR_BAD : right() ? COLOR_GOOD : COLOR_LIGHT;
        graphics.renderOutline(faceX - 25, faceY - 29, 50, 56, Draw.alpha(colour, 0.55F));
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
