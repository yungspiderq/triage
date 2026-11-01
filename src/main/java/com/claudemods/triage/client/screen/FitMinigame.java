package com.claudemods.triage.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Fitting a leg or a hook to a stump: how deep it sits, and which way it points.
 *
 * <p>What was here was the splint's bone-holding game followed by the bandage's winding
 * game, on the theory that fitting one is lining something up and then strapping it. Both
 * halves were somebody else's, and neither was about the thing that actually decides
 * whether a prosthesis is worth having.
 *
 * <p>Two things decide it, and the first is the one nobody outside the trade knows:
 * <b>a socket must not bear on the end of the bone.</b> The load goes on the sides of the
 * stump. Push a socket on until it is sitting on the tip and it is unwearable - not sore,
 * unwearable - and pull it back so it is barely on and it rocks and rubs instead. There is
 * a depth between those two and it is the entire craft.
 *
 * <p>The second is alignment. A leg set half an inch out of line is a limp for as long as
 * it is worn, and the limp wrecks the other side in a year. So it is lined up against the
 * one that is left, which is drawn beside it - not against a number, against the person.
 *
 * <p>The hook is the same fitting with the second question turned round: not how straight
 * it hangs but which way it opens, because a hook facing the wrong way is a hook that
 * cannot pick anything up.
 */
public final class FitMinigame implements Minigame {
    /** How far the socket can travel onto the stump. */
    private static final float SEAT_TRAVEL = 52.0F;
    /** The band it has to end in: off the bone end, and not rocking. */
    private static final float SEAT_TARGET = 0.62F;
    private static final float SEAT_BAND = 0.11F;

    /** How far it can be swung either way, and how straight is straight enough. */
    private static final float MAX_ANGLE = 34.0F;
    private static final float ANGLE_BAND = 5.5F;
    private static final float SWING_PER_PIXEL = 0.55F;

    private static final int COLOR_SKIN = 0xFFB07A5A;
    private static final int COLOR_SKIN_EDGE = 0xFF6E4632;
    private static final int COLOR_STUMP = 0xFFC08A6A;
    private static final int COLOR_SCAR = 0xFF8A4A40;
    private static final int COLOR_WOOD = 0xFF7A5230;
    private static final int COLOR_WOOD_LIT = 0xFF95663D;
    private static final int COLOR_IRON = 0xFFB8BCC4;
    private static final int COLOR_IRON_DARK = 0xFF80868F;
    private static final int COLOR_CUFF = 0xFF6B4A28;
    private static final int COLOR_BAND = 0x44FFE070;
    private static final int COLOR_BAND_EDGE = 0x99FFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;
    private static final int COLOR_GUIDE = 0x60FFFFFF;

    private final boolean leg;
    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int stumpX;
    private final int stumpEnd;
    private final int soundX;

    /** 0 barely on, 1 hard against the bone end. */
    private float seat;
    private boolean seated;
    /** Degrees off straight, positive to the right. */
    private float angle = -21.0F;
    private boolean held;
    private float grabX;
    private float grabAngle;
    private int flash;
    private int fumbles;

    public FitMinigame(int left, int top, int width, int height, boolean leg) {
        this.leg = leg;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.stumpX = left + width / 2 - 22;
        this.stumpEnd = top + 46;
        this.soundX = left + width / 2 + 34;
    }

    @Override
    public void tick() {
        if (flash > 0) {
            flash--;
        }
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        if (!seated) {
            seat = clampSeat(mouseY);
        } else {
            grabX = (float) mouseX;
            grabAngle = angle;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        if (!seated) {
            // Pushing it on: one axis, and the axis the socket actually moves along.
            seat = clampSeat(mouseY);
        } else {
            // Swinging it: also one axis, and the sound leg is the thing to match.
            angle = Mth.clamp(grabAngle + ((float) mouseX - grabX) * SWING_PER_PIXEL,
                    -MAX_ANGLE, MAX_ANGLE);
        }
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        held = false;
        if (!seated) {
            if (Math.abs(seat - SEAT_TARGET) <= SEAT_BAND) {
                seated = true;
                play(SoundEvents.WOOD_PLACE, 0.9F);
            } else {
                flash = 16;
                fumbles++;
                play(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 0.7F);
            }
            return;
        }
        if (Math.abs(angle) <= ANGLE_BAND) {
            angle = 0.0F;
            play(SoundEvents.ANVIL_USE, 1.0F);
            return;
        }
        flash = 16;
        fumbles++;
        play(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 0.7F);
    }

    private float clampSeat(double mouseY) {
        return Mth.clamp(((float) mouseY - stumpEnd) / SEAT_TRAVEL, 0.0F, 1.0F);
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        if (!seated) {
            return Mth.clamp(0.5F * (1.0F - Math.abs(seat - SEAT_TARGET) / 0.5F), 0.0F, 0.5F);
        }
        return 0.5F + 0.5F * Mth.clamp(1.0F - Math.abs(angle) / MAX_ANGLE, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return seated && angle == 0.0F;
    }

    /** Nothing here is unrecoverable: a socket in the wrong place is pulled off again. */
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
        if (!seated) {
            if (flash > 0) {
                return Component.translatable(seat > SEAT_TARGET
                        ? "triage.minigame.fit_deep" : "triage.minigame.fit_shallow");
            }
            return Component.translatable("triage.minigame.fit_seat");
        }
        if (flash > 0) {
            return Component.translatable(leg ? "triage.minigame.fit_crooked" : "triage.minigame.fit_turned");
        }
        return Component.translatable(leg ? "triage.minigame.fit_line" : "triage.minigame.fit_face");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        sound(graphics);
        stump(graphics);
        gauge(graphics);
        piece(graphics);
    }

    /**
     * The limb that is left, drawn beside the one being made.
     *
     * <p>The reference for the whole second half. A number would have done the job and
     * would have been the wrong job: what a leg has to match is the other leg.
     */
    private void sound(GuiGraphics graphics) {
        if (!leg) {
            return;
        }
        graphics.fill(soundX - 10, top + 12, soundX + 10, top + height - 18, COLOR_SKIN_EDGE);
        Draw.body(graphics, soundX - 9, top + 13, 18, height - 32, COLOR_SKIN);
        graphics.fill(soundX - 12, top + height - 20, soundX + 14, top + height - 14, COLOR_SKIN_EDGE);
        // The line the new one has to stand on, carried across from the sound foot.
        Draw.line(graphics, stumpX - 34, top + height - 17, soundX + 14, top + height - 17,
                1, COLOR_GUIDE);
    }

    /** What is left of the limb, and the scar across the end of it. */
    private void stump(GuiGraphics graphics) {
        graphics.fill(stumpX - 12, top + 10, stumpX + 12, stumpEnd + 2, COLOR_SKIN_EDGE);
        Draw.body(graphics, stumpX - 11, top + 11, 22, stumpEnd - top - 10, COLOR_STUMP);
        // Rounded off, the way one heals.
        graphics.fill(stumpX - 9, stumpEnd - 4, stumpX + 9, stumpEnd, COLOR_STUMP);
        graphics.fill(stumpX - 7, stumpEnd, stumpX + 7, stumpEnd + 2, COLOR_STUMP);
        for (int i = -6; i <= 6; i += 3) {
            graphics.fill(stumpX + i, stumpEnd - 7, stumpX + i + 2, stumpEnd - 6, COLOR_SCAR);
        }
    }

    /**
     * How the socket is bearing: on the sides, or on the end of the bone.
     *
     * <p>The one thing about fitting a socket that has to be said out loud, so it is a
     * gauge with a band on it rather than a feeling.
     */
    private void gauge(GuiGraphics graphics) {
        if (seated) {
            return;
        }
        int x = left + 10;
        int barTop = top + 16;
        int barHeight = Math.round(SEAT_TRAVEL);
        graphics.fill(x, barTop, x + 8, barTop + barHeight, 0x40000000);
        int from = barTop + Math.round(barHeight * (SEAT_TARGET - SEAT_BAND));
        int to = barTop + Math.round(barHeight * (SEAT_TARGET + SEAT_BAND));
        graphics.fill(x, from, x + 8, to, COLOR_BAND);
        graphics.fill(x, from, x + 8, from + 1, COLOR_BAND_EDGE);
        graphics.fill(x, to - 1, x + 8, to, COLOR_BAND_EDGE);
        int at = barTop + Math.round(barHeight * seat);
        boolean good = Math.abs(seat - SEAT_TARGET) <= SEAT_BAND;
        graphics.fill(x - 3, at - 1, x + 11, at + 1, flash > 0 ? COLOR_BAD : good ? COLOR_GOOD : COLOR_IRON);
    }

    /** The peg or the hook, at the depth and angle the hand has it. */
    private void piece(GuiGraphics graphics) {
        float shown = seated ? SEAT_TARGET : seat;
        int socketTop = stumpEnd - Math.round(28.0F * shown);
        boolean good = seated ? Math.abs(angle) <= ANGLE_BAND
                : Math.abs(seat - SEAT_TARGET) <= SEAT_BAND;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(stumpX, socketTop, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-stumpX, -socketTop, 0.0F);

        // The socket, which is the same on both and is the part that matters.
        graphics.fill(stumpX - 13, socketTop, stumpX + 13, socketTop + 16, COLOR_CUFF);
        graphics.fill(stumpX - 13, socketTop, stumpX + 13, socketTop + 2, Draw.shade(COLOR_CUFF, 1.25F));
        graphics.fill(stumpX - 13, socketTop + 14, stumpX + 13, socketTop + 16, Draw.shade(COLOR_CUFF, 0.7F));

        if (leg) {
            int base = seated ? COLOR_WOOD : COLOR_WOOD_LIT;
            int shaftTop = socketTop + 16;
            int shaftBottom = top + height - 22;
            graphics.fill(stumpX - 5, shaftTop, stumpX + 5, shaftBottom, base);
            graphics.fill(stumpX - 5, shaftTop, stumpX - 3, shaftBottom, Draw.shade(base, 1.18F));
            graphics.fill(stumpX + 3, shaftTop, stumpX + 5, shaftBottom, Draw.shade(base, 0.76F));
            graphics.fill(stumpX - 7, shaftBottom, stumpX + 7, shaftBottom + 5, Draw.shade(base, 0.86F));
        } else {
            int shaftTop = socketTop + 16;
            graphics.fill(stumpX - 3, shaftTop, stumpX + 3, shaftTop + 18, COLOR_IRON_DARK);
            graphics.fill(stumpX - 2, shaftTop, stumpX + 1, shaftTop + 18, COLOR_IRON);
            // The hook, opening towards the left of the picture when it is straight.
            graphics.fill(stumpX - 16, shaftTop + 18, stumpX + 3, shaftTop + 23, COLOR_IRON);
            graphics.fill(stumpX - 16, shaftTop + 8, stumpX - 11, shaftTop + 20, COLOR_IRON);
            graphics.fill(stumpX - 16, shaftTop + 8, stumpX - 13, shaftTop + 14, COLOR_IRON_DARK);
        }
        pose.popPose();

        // Which way is straight, once the socket is on and the swing is the question.
        if (seated && !complete()) {
            int colour = flash > 0 ? COLOR_BAD : good ? COLOR_GOOD : COLOR_GUIDE;
            Draw.line(graphics, stumpX, socketTop, stumpX, top + height - 16, 1, colour);
        }
        if (held) {
            int colour = flash > 0 ? COLOR_BAD : good ? COLOR_GOOD : COLOR_BAND_EDGE;
            graphics.renderOutline(stumpX - 15, socketTop - 2, 30, 20, colour);
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
