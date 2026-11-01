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
 * Breaking a bone again, on purpose, because it healed in the wrong shape.
 *
 * <p>The only operation in this mod that <b>is</b> the injury, and the only one that is
 * never an emergency: the bone knitted, the patient walked out, and what they walked out
 * with was a leg the wrong length. There is one way to undo that and it is the one a
 * surgeon actually uses - cut it through where the angle is, and put it back together
 * without the angle.
 *
 * <p>Two hands and two jobs. First the cut, along a line that is marked, because this is
 * the one time in the whole mod where somebody has stood and thought about where the bone
 * should be divided rather than finding out where it divided itself. Wander off the line
 * and the cut is ragged, which is a worse join and a longer week.
 *
 * <p>Then the wedge closes. The two ends come together and the angle that was in the bone
 * goes out of it - and that has to be done <b>squarely</b>, because the whole point of the
 * exercise was that the last person to do this left it crooked.
 */
public final class OsteotomyMinigame implements Minigame {
    /** How far along the line the saw has to travel. */
    private static final float CUT = 1.0F;
    /** Mouse travel, in pixels, for a full cut. */
    private static final float TRAVEL = 150.0F;
    /** How far off the line is still on it. */
    private static final float ON_LINE = 7.0F;
    /** How square the join has to be, in degrees of the old angle left in it. */
    private static final float SQUARE = 2.5F;
    /** And how far out of true it starts, which is the crooked bone itself. */
    private static final float BENT = 15.0F;

    private static final int LIMB = 0xFFB07A5A;
    private static final int LIMB_EDGE = 0xFF6E4632;
    private static final int BONE = 0xFFEFE7D2;
    private static final int BONE_EDGE = 0xFFB6AC92;
    private static final int LINE = 0x99FFE070;
    private static final int LINE_LIT = 0xFFFFE070;
    private static final int SAW = 0xFFD8DEE4;
    private static final int RAGGED = 0xFFCC5533;
    private static final int GOOD = 0xFF7FD46B;
    private static final int BAD = 0xFFCC3333;
    private static final int EMPTY = 0x40FFFFFF;

    private enum Phase { CUT, CLOSE, DONE }

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int centreY;
    /** The angle the bone healed with, and the angle the cut is marked at. */
    private final float bend;
    private final float cutLean;

    private Phase phase = Phase.CUT;
    private float along;
    private float angle;
    private boolean held;
    private float grabX;
    private float grabY;
    private float grabValue;
    private int ragged;
    private int age;
    private int flash;

    public OsteotomyMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = Math.max(80, width);
        this.height = Math.max(60, height);
        this.centreX = left + this.width / 2;
        this.centreY = top + this.height / 2;
        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        this.bend = (random.nextBoolean() ? 1.0F : -1.0F) * BENT;
        this.cutLean = (random.nextFloat() - 0.5F) * 0.45F;
        this.angle = bend;
    }

    // ---- the hands ----------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || phase == Phase.DONE) {
            return false;
        }
        held = true;
        grabX = (float) mouseX;
        grabY = (float) mouseY;
        grabValue = phase == Phase.CUT ? along : angle;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!held || phase == Phase.DONE) {
            return false;
        }
        if (phase == Phase.CUT) {
            along = Mth.clamp(grabValue + ((float) mouseX - grabX) / TRAVEL, 0.0F, CUT);
            // Off the marked line while the blade is in the bone: a ragged edge, and a
            // ragged edge is a worse join.
            float wanted = lineY(along);
            if (Math.abs((float) mouseY - wanted) > ON_LINE) {
                ragged++;
                flash = 4;
                if (age % 5 == 0) {
                    play(SoundEvents.BONE_BLOCK_BREAK, 0.7F);
                }
            } else if (age % 6 == 0) {
                play(SoundEvents.BONE_BLOCK_HIT, 1.1F);
            }
            if (along >= CUT) {
                phase = Phase.CLOSE;
                play(SoundEvents.BONE_BLOCK_BREAK, 1.2F);
            }
            return true;
        }
        // Closing the wedge: the far half turns about the cut until the bone is straight.
        angle = Mth.clamp(grabValue + ((float) mouseX - grabX) * 0.25F, -BENT * 1.5F, BENT * 1.5F);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        held = false;
        if (phase == Phase.CLOSE) {
            phase = Phase.DONE;
            play(Math.abs(angle) <= SQUARE ? SoundEvents.BONE_BLOCK_PLACE : SoundEvents.BONE_BLOCK_HIT,
                    Math.abs(angle) <= SQUARE ? 1.3F : 0.8F);
        }
    }

    /** Where the marked line is at a given point along the cut. */
    private float lineY(float at) {
        return centreY + (at - 0.5F) * TRAVEL * cutLean;
    }

    @Override
    public void tick() {
        age++;
        if (flash > 0) {
            flash--;
        }
    }

    // ---- where it stands ----------------------------------------------------

    @Override
    public float progress() {
        return switch (phase) {
            case CUT -> along * 0.6F;
            case CLOSE -> 0.6F + 0.4F * (1.0F - Mth.clamp(Math.abs(angle) / BENT, 0.0F, 1.0F));
            default -> 1.0F;
        };
    }

    @Override
    public float quality() {
        float straight = 1.0F - Mth.clamp((Math.abs(angle) - SQUARE) / BENT, 0.0F, 1.0F);
        float clean = 1.0F - Mth.clamp(ragged / 90.0F, 0.0F, 0.5F);
        return Mth.clamp(straight * clean, 0.15F, 1.0F);
    }

    @Override
    public boolean complete() {
        return phase == Phase.DONE;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        return switch (phase) {
            case DONE -> Component.translatable(Math.abs(angle) <= SQUARE
                    ? "triage.minigame.osteotomy_done" : "triage.minigame.osteotomy_still_bent");
            case CLOSE -> Component.translatable(Math.abs(angle) <= SQUARE
                    ? "triage.minigame.osteotomy_square" : "triage.minigame.osteotomy_close");
            default -> Component.translatable(flash > 0
                    ? "triage.minigame.osteotomy_off" : "triage.minigame.osteotomy_cut");
        };
    }

    // ---- drawing ------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        limb(graphics);
        mark(graphics);
        if (phase == Phase.CUT) {
            saw(graphics, mouseX, mouseY);
        }
        bar(graphics);
    }

    /**
     * The bone, with the angle in it - and after the cut, in two pieces that turn.
     *
     * <p>Drawn as two straight runs meeting at the cut rather than as a curve, because
     * that is what a malunion is: two straight bones that met at the wrong angle and were
     * left there.
     */
    private void limb(GuiGraphics graphics) {
        int half = Math.min(width / 2 - 14, 76);
        float shown = phase == Phase.CUT ? bend : angle;
        int drop = Math.round(half * (float) Math.tan(Math.toRadians(shown)));

        graphics.fill(centreX - half, centreY - 12, centreX, centreY + 12, LIMB_EDGE);
        graphics.fill(centreX - half, centreY - 11, centreX, centreY + 11, LIMB);
        graphics.fill(centreX - half + 2, centreY - 5, centreX + 2, centreY + 5, BONE_EDGE);
        graphics.fill(centreX - half + 2, centreY - 4, centreX + 2, centreY + 4, BONE);

        // The far half, hinged at the cut.
        for (int i = 0; i < half; i++) {
            int y = centreY + Math.round(drop * (i / (float) half));
            graphics.fill(centreX + i, y - 12, centreX + i + 1, y + 12, LIMB_EDGE);
            graphics.fill(centreX + i, y - 11, centreX + i + 1, y + 11, LIMB);
            graphics.fill(centreX + i, y - 5, centreX + i + 1, y + 5, BONE_EDGE);
            graphics.fill(centreX + i, y - 4, centreX + i + 1, y + 4, BONE);
        }
        if (ragged > 0 && phase != Phase.CUT) {
            for (int i = -4; i <= 4; i += 2) {
                graphics.fill(centreX - 1, centreY + i, centreX + 2, centreY + i + 1, RAGGED);
            }
        }
    }

    /** The line somebody stood and thought about, and how far the saw has got along it. */
    private void mark(GuiGraphics graphics) {
        if (phase != Phase.CUT) {
            return;
        }
        for (int i = 0; i <= 40; i++) {
            float at = i / 40.0F;
            int x = Math.round(centreX - TRAVEL / 2.0F + at * TRAVEL);
            int y = Math.round(lineY(at));
            graphics.fill(x, y - 1, x + 2, y + 1, at <= along ? LINE_LIT : LINE);
        }
    }

    private void saw(GuiGraphics graphics, int mouseX, int mouseY) {
        int colour = flash > 0 ? BAD : SAW;
        graphics.fill(mouseX - 14, mouseY - 2, mouseX + 14, mouseY + 1, colour);
        for (int i = -12; i <= 12; i += 3) {
            graphics.fill(mouseX + i, mouseY + 1, mouseX + i + 2, mouseY + 3, colour);
        }
    }

    private void bar(GuiGraphics graphics) {
        int barLeft = left + 8;
        int barWidth = width - 16;
        int barTop = top + height - 8;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 4, EMPTY);
        graphics.fill(barLeft, barTop, barLeft + Math.round(barWidth * progress()), barTop + 4,
                phase == Phase.CLOSE && Math.abs(angle) <= SQUARE ? GOOD : 0xFFF2ECE0);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F), 0.35F));
    }
}
