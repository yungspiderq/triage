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
 * Rolling somebody onto their side, which is done in a particular order for a reason.
 *
 * <p>You do not push a body over. You take the arm and the knee on the <em>far</em> side,
 * and use the bent knee as the handle - so the weight comes towards you and lands on the
 * shoulder rather than the face. Doing it the other way is how somebody who was
 * unconscious with a straight neck stops having a straight neck.
 *
 * <p>Then the head goes back and the top hand goes under the cheek, which is not a
 * flourish: that is what keeps the head tilted once nobody is holding it, and it is why
 * this position keeps working after the person who did it has walked away.
 *
 * <p>Three steps, one movement each, and the middle one is the roll.
 */
public final class RollMinigame implements Minigame {
    /** How far the roll has to be brought across. */
    private static final float ROLL_TRAVEL = 66.0F;
    /** How close a hand has to be to take hold of something. */
    private static final float GRAB = 15.0F;

    private static final int COLOR_BODY = 0xFFB07A5A;
    private static final int COLOR_BODY_EDGE = 0xFF6E4632;
    private static final int COLOR_LIMB = 0xFF9A6A4E;
    private static final int COLOR_FACE = 0xFFC08A6A;
    private static final int COLOR_GROUND = 0x30FFFFFF;
    private static final int COLOR_MARK = 0xFFFFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;
    private static final int COLOR_HAND = 0xFFF2ECE0;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int centreY;

    /** Which of the three is being done. */
    private boolean armTaken;
    private boolean kneeTaken;
    private float rolled;
    private boolean headSet;

    private boolean held;
    private float handX;
    private float handY;
    private float grabX;
    private float grabRolled;
    private int flash;

    public RollMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        this.left = areaLeft;
        this.top = areaTop;
        this.width = areaWidth;
        this.height = areaHeight;
        this.centreX = areaLeft + areaWidth / 2;
        this.centreY = areaTop + areaHeight / 2;
        this.handX = centreX;
        this.handY = centreY;
    }

    @Override
    public void tick() {
        if (flash > 0) {
            flash--;
        }
    }

    // ---- the hand ---------------------------------------------------------

    private boolean holding() {
        return armTaken && kneeTaken;
    }

    private int farArmX() {
        return centreX - 34;
    }

    private int farArmY() {
        return centreY - 16;
    }

    private int farKneeX() {
        return centreX - 30;
    }

    private int farKneeY() {
        return centreY + 22;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        handX = (float) mouseX;
        handY = (float) mouseY;
        if (!holding()) {
            take();
            return true;
        }
        if (rolled >= 1.0F) {
            return true;
        }
        grabX = handX;
        grabRolled = rolled;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || !held || complete()) {
            return false;
        }
        handX = (float) mouseX;
        handY = (float) mouseY;
        if (!holding()) {
            take();
            return true;
        }
        if (rolled < 1.0F) {
            // Towards you, which on this screen is to the right: the far side comes over
            // and the body lands on its shoulder.
            float want = grabRolled + (handX - grabX) / ROLL_TRAVEL;
            if (want < rolled - 0.02F) {
                // Pushed back the other way, which is putting them on their face.
                if (flash == 0) {
                    flash = 16;
                    play(SoundEvents.SLIME_SQUISH, 0.7F);
                }
                return true;
            }
            rolled = Mth.clamp(want, 0.0F, 1.0F);
            if (rolled >= 1.0F) {
                play(SoundEvents.WOOL_PLACE, 0.9F);
            }
            return true;
        }
        // Last: the top hand under the cheek, which is what holds the head back.
        if (!headSet && Math.abs(handX - (centreX + 26)) <= GRAB
                && Math.abs(handY - (centreY - 24)) <= GRAB) {
            headSet = true;
            play(SoundEvents.WOOL_PLACE, 1.2F);
        }
        return true;
    }

    /** Taking hold of the far arm and the far knee, which are the handles. */
    private void take() {
        if (!armTaken && Math.abs(handX - farArmX()) <= GRAB && Math.abs(handY - farArmY()) <= GRAB) {
            armTaken = true;
            play(SoundEvents.WOOL_HIT, 1.0F);
        }
        if (!kneeTaken && Math.abs(handX - farKneeX()) <= GRAB && Math.abs(handY - farKneeY()) <= GRAB) {
            kneeTaken = true;
            play(SoundEvents.WOOL_HIT, 1.1F);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            held = false;
        }
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        float grip = ((armTaken ? 1 : 0) + (kneeTaken ? 1 : 0)) * 0.15F;
        return Mth.clamp(grip + rolled * 0.6F + (headSet ? 0.1F : 0.0F), 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return rolled >= 1.0F && headSet;
    }

    /** Rolling somebody over is not a thing that fails. It is a thing done badly or well. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        if (!holding()) {
            return Component.translatable("triage.minigame.roll_far");
        }
        if (rolled < 1.0F) {
            return Component.translatable("triage.minigame.roll_over");
        }
        return Component.translatable("triage.minigame.roll_head");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        Draw.line(graphics, left + 8, centreY + 42, left + width - 8, centreY + 42, 1, COLOR_GROUND);

        int shift = Math.round(rolled * 16.0F);
        int narrow = Math.round(rolled * 11.0F);

        // The trunk, turning onto its side: it gets narrower on the way over, which is
        // the whole of what turning looks like from above.
        graphics.fill(centreX - 24 + shift + narrow, centreY - 30, centreX + 24 + shift - narrow,
                centreY + 34, COLOR_BODY_EDGE);
        Draw.body(graphics, centreX - 23 + shift + narrow, centreY - 29,
                (24 - narrow) * 2 - 2, 62, COLOR_BODY);

        // The head, and the hand under the cheek once it is there.
        int headX = centreX + shift + Math.round(rolled * 6.0F);
        Draw.disc(graphics, headX, centreY - 38, 12, COLOR_BODY_EDGE);
        Draw.disc(graphics, headX, centreY - 38, 11, COLOR_FACE);
        if (headSet) {
            graphics.fill(headX + 6, centreY - 34, headX + 16, centreY - 26, COLOR_HAND);
        } else if (rolled >= 1.0F) {
            int colour = flash > 0 ? COLOR_BAD : COLOR_MARK;
            graphics.renderOutline(centreX + 26 - 8, centreY - 24 - 8, 16, 16, colour);
        }

        // The far arm and the far knee: the two handles, marked until they are held.
        limb(graphics, farArmX() + shift * 2, farArmY(), armTaken);
        limb(graphics, farKneeX() + shift * 2, farKneeY(), kneeTaken);

        if (holding() && rolled < 1.0F) {
            // Which way it goes, drawn once there is something to pull.
            int y = centreY;
            int from = centreX + 30;
            Draw.line(graphics, from, y, from + 34, y, 1, flash > 0 ? COLOR_BAD : COLOR_MARK);
            Draw.line(graphics, from + 34, y, from + 28, y - 5, 1, flash > 0 ? COLOR_BAD : COLOR_MARK);
            Draw.line(graphics, from + 34, y, from + 28, y + 5, 1, flash > 0 ? COLOR_BAD : COLOR_MARK);
        }

        if (held) {
            int x = Math.round(Mth.clamp(handX, left + 5, left + width - 5));
            int y = Math.round(Mth.clamp(handY, top + 5, top + height - 5));
            Draw.disc(graphics, x, y, 4, flash > 0 ? COLOR_BAD : COLOR_HAND);
        }
    }

    private void limb(GuiGraphics graphics, int x, int y, boolean taken) {
        graphics.fill(x - 9, y - 6, x + 9, y + 6, COLOR_BODY_EDGE);
        graphics.fill(x - 8, y - 5, x + 8, y + 5, COLOR_LIMB);
        if (!taken) {
            graphics.renderOutline(x - 12, y - 9, 24, 18, COLOR_MARK);
            return;
        }
        graphics.renderOutline(x - 12, y - 9, 24, 18, COLOR_GOOD);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
