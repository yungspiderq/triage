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
 * Taking the limb off: the flap first, then the bone.
 *
 * <p>The first version of this was eleven strokes of a saw and nothing else. It was long
 * on purpose and it was still only half the operation - the half anybody would guess. What
 * it left out is the half that decides whether the patient has a stump or an open wound
 * with a bone in the middle of it.
 *
 * <p>You do not cut straight through. You cut a <b>flap</b> of skin and muscle below where
 * the bone is going to be divided, draw the soft tissue back out of the way, and saw the
 * bone <em>shorter than the flesh</em>. Then the flap folds over the end and there is
 * something to close the stump with. Cut everything off at one level - a guillotine, which
 * is exactly what the old game was - and there is nothing to cover the bone with, and the
 * stump never closes. It is the oldest distinction in the trade and it is the whole
 * difference between an amputation and a chop.
 *
 * <p>So: trace the flap, then saw. The sawing keeps the rules it had, because they were
 * the right ones - full strokes end to end, because a saw worked in the middle of its
 * length binds, and level, because it is going through in a line.
 *
 * <p>And it still cannot be failed. There is no state of the world in which putting the
 * saw down halfway is better than finishing, so the game does not offer it: it can be
 * done badly, slowly and miserably, and it will be done.
 */
public final class SawMinigame implements Minigame {
    /** How far round the limb the flap has to be traced. */
    private static final int FLAP_STEPS = 16;
    /** How close the hand has to pass to take the next step of it. */
    private static final float TRACE_REACH = 11.0F;

    /** Full passes to get through the bone. Long on purpose. */
    private static final int STROKES = 9;
    /** How far from an end counts as having reached it. */
    private static final float END = 0.14F;
    /** How far off the line the blade may wander before it starts binding. */
    private static final float OFF_LINE = 7.0F;

    private static final int COLOR_FLESH = 0xFF6E3A34;
    private static final int COLOR_SKIN = 0xFFB07A5A;
    private static final int COLOR_SKIN_EDGE = 0xFF6E4632;
    private static final int COLOR_BONE = 0xFFE4DCC4;
    private static final int COLOR_BONE_EDGE = 0xFFB6AC92;
    private static final int COLOR_CUT = 0xFF3A1614;
    private static final int COLOR_BLADE = 0xFFB8BCC4;
    private static final int COLOR_TEETH = 0xFF8A8F98;
    private static final int COLOR_HANDLE = 0xFF6B4A28;
    private static final int COLOR_KNIFE = 0xFFD8DCE2;
    private static final int COLOR_MARK = 0xFFFFE070;
    private static final int COLOR_DONE = 0xFF7FD46B;
    private static final int COLOR_BIND = 0xFFCC3333;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    /** Where the flesh is cut, and where the bone is cut: never the same line. */
    private final int flapY;
    private final int boneY;
    private final int limbTop;
    private final int limbBottom;
    private final int halfWidth;

    private int traced;
    private boolean cutting;

    /** Where the blade is along its travel, 0 to 1. */
    private float at = 0.5F;
    private boolean towardsRight = true;
    private boolean held;
    private int strokes;
    private int binds;
    private int bindFlash;
    private float off;

    public SawMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        this.left = areaLeft;
        this.top = areaTop;
        this.width = areaWidth;
        this.height = areaHeight;
        this.centreX = areaLeft + areaWidth / 2;
        this.halfWidth = 22;
        this.limbTop = areaTop + 10;
        this.limbBottom = areaTop + areaHeight - 10;
        this.boneY = areaTop + areaHeight / 2 - 12;
        this.flapY = boneY + 26;
    }

    @Override
    public void tick() {
        if (bindFlash > 0) {
            bindFlash--;
        }
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        work(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || !held) {
            return false;
        }
        work(mouseX, mouseY);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            held = false;
        }
    }

    private void work(double mouseX, double mouseY) {
        if (!cutting) {
            trace(mouseX, mouseY);
        } else {
            saw(mouseX, mouseY);
        }
    }

    /**
     * The flap: a curve traced round the limb, well below where the bone will go.
     *
     * <p>Traced in order and in one direction, because that is what a knife does and
     * because it means there is always exactly one place to be - which the screen can
     * then point at. Nothing here is a guess either.
     */
    private void trace(double mouseX, double mouseY) {
        float tx = flapX(traced);
        float ty = flapPointY(traced);
        if (Math.abs((float) mouseX - tx) > TRACE_REACH || Math.abs((float) mouseY - ty) > TRACE_REACH) {
            return;
        }
        traced++;
        play(SoundEvents.PLAYER_ATTACK_SWEEP, 1.3F + 0.02F * traced);
        if (traced >= FLAP_STEPS) {
            cutting = true;
            play(SoundEvents.WOOL_BREAK, 0.8F);
        }
    }

    /** One position of the hand on the saw. */
    private void saw(double mouseX, double mouseY) {
        if (complete()) {
            return;
        }
        at = Mth.clamp((float) (mouseX - trackLeft()) / trackWidth(), 0.0F, 1.0F);
        off = Math.abs((float) mouseY - boneY);
        if (off > OFF_LINE) {
            bind();
        }
        if (towardsRight && at >= 1.0F - END) {
            towardsRight = false;
            stroke();
        } else if (!towardsRight && at <= END) {
            towardsRight = true;
            stroke();
        }
    }

    private void stroke() {
        strokes++;
        play(strokes >= STROKES ? SoundEvents.BONE_BLOCK_BREAK : SoundEvents.BONE_BLOCK_HIT,
                0.6F + 0.03F * strokes);
    }

    private void bind() {
        // One per moment rather than one per frame: a hand that is crooked for half a
        // second has gone crooked once.
        if (bindFlash > 0) {
            return;
        }
        binds++;
        bindFlash = 10;
        play(SoundEvents.SLIME_SQUISH, 0.8F);
    }

    private int trackLeft() {
        return centreX - halfWidth - 6;
    }

    private int trackWidth() {
        return (halfWidth + 6) * 2;
    }

    /** Where each step of the flap sits: a curve round the front of the limb. */
    private float flapX(int step) {
        float along = Mth.clamp(step / (float) (FLAP_STEPS - 1), 0.0F, 1.0F);
        return centreX - halfWidth - 4 + along * (halfWidth + 4) * 2.0F;
    }

    private float flapPointY(int step) {
        float along = Mth.clamp(step / (float) (FLAP_STEPS - 1), 0.0F, 1.0F);
        return flapY + Mth.sin(along * Mth.PI) * 16.0F;
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        float flap = 0.3F * Mth.clamp(traced / (float) FLAP_STEPS, 0.0F, 1.0F);
        float bone = 0.7F * Mth.clamp(strokes / (float) STROKES, 0.0F, 1.0F);
        return Mth.clamp(flap + bone, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return cutting && strokes >= STROKES;
    }

    /**
     * Never.
     *
     * <p>The saw is in the leg. There is no version of putting it down that leaves the
     * patient better off than finishing, so the game has no way of putting it down - and
     * the crooked strokes are charged for in {@link #quality()} instead, which is where
     * the mod puts everything a player can do badly and still have done.
     */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - binds * 0.12F, 0.0F, 1.0F);
    }

    @Override
    public Component instruction() {
        if (!cutting) {
            return Component.translatable("triage.minigame.saw_flap");
        }
        return Component.translatable(bindFlash > 0 ? "triage.minigame.saw_binding"
                : !held ? "triage.minigame.saw_take"
                : towardsRight ? "triage.minigame.saw_right"
                : "triage.minigame.saw_left");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        limb(graphics);
        if (cutting) {
            bone(graphics);
            blade(graphics);
        } else {
            flap(graphics, mouseX, mouseY);
        }
        pips(graphics);
    }

    /** The limb, with the soft tissue drawn back once the flap is cut. */
    private void limb(GuiGraphics graphics) {
        int fleshTop = cutting ? flapY - 6 : limbTop;
        graphics.fill(centreX - halfWidth - 1, limbTop - 1, centreX + halfWidth + 1, limbBottom + 1,
                COLOR_SKIN_EDGE);
        Draw.body(graphics, centreX - halfWidth, limbTop, halfWidth * 2, limbBottom - limbTop, COLOR_SKIN);

        // Above the flap the flesh has been retracted, so what is up there is cut
        // surface rather than skin - which is the picture the whole first half was for.
        if (cutting) {
            graphics.fill(centreX - halfWidth, limbTop, centreX + halfWidth, fleshTop, COLOR_FLESH);
            for (float along = 0.0F; along <= 1.0F; along += 0.02F) {
                int x = Math.round(centreX - halfWidth - 4 + along * (halfWidth + 4) * 2.0F);
                int y = Math.round(flapY + Mth.sin(along * Mth.PI) * 16.0F);
                graphics.fill(x, y - 6, x + 2, y, COLOR_CUT);
            }
        }
    }

    /** The flap to be traced, and how much of it is done. */
    private void flap(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < FLAP_STEPS; i++) {
            int x = Math.round(flapX(i));
            int y = Math.round(flapPointY(i));
            if (i < traced) {
                Draw.disc(graphics, x, y, 2, COLOR_CUT);
                continue;
            }
            if (i == traced) {
                Draw.disc(graphics, x, y, 3, COLOR_MARK);
                graphics.renderOutline(x - 5, y - 5, 10, 10, COLOR_MARK);
                continue;
            }
            Draw.disc(graphics, x, y, 1, Draw.alpha(COLOR_MARK, 0.35F));
        }
        // Where the bone is going to be divided, marked from the start, so that "below
        // the bone line" is a place rather than a sentence.
        Draw.line(graphics, centreX - halfWidth - 8, boneY, centreX + halfWidth + 8, boneY, 1,
                Draw.alpha(COLOR_BONE, 0.5F));
        if (held) {
            graphics.fill(mouseX - 1, mouseY - 5, mouseX + 1, mouseY + 5, COLOR_KNIFE);
        }
    }

    /** The bone, higher than the flesh, with the kerf opening in it. */
    private void bone(GuiGraphics graphics) {
        graphics.fill(centreX - 9, limbTop + 2, centreX + 9, flapY, COLOR_BONE_EDGE);
        graphics.fill(centreX - 8, limbTop + 3, centreX + 8, flapY, COLOR_BONE);

        int cut = Math.round(9.0F * Mth.clamp(strokes / (float) STROKES, 0.0F, 1.0F));
        if (cut > 0) {
            graphics.fill(centreX - cut, boneY - 3, centreX + cut, boneY + 3, COLOR_CUT);
        }
        Draw.line(graphics, centreX - halfWidth - 6, boneY, centreX + halfWidth + 6, boneY, 1,
                Draw.alpha(COLOR_MARK, 0.4F));
    }

    /** The saw, wherever the hand has it. */
    private void blade(GuiGraphics graphics) {
        int bladeX = trackLeft() + Math.round(trackWidth() * at);
        int bladeY = held ? Math.round(Mth.clamp(boneY + (off > OFF_LINE ? OFF_LINE : off),
                top + 6, top + height - 6)) : boneY;
        int colour = bindFlash > 0 ? COLOR_BIND : COLOR_BLADE;
        Draw.line(graphics, bladeX - 26, bladeY - 7, bladeX + 26, bladeY - 7, 3, colour);
        for (int i = -24; i <= 24; i += 4) {
            graphics.fill(bladeX + i, bladeY - 5, bladeX + i + 2, bladeY - 3, COLOR_TEETH);
        }
        graphics.fill(bladeX - 30, bladeY - 11, bladeX - 26, bladeY - 2, COLOR_HANDLE);
    }

    /** One pip per stroke, because the only question in the second half is how many more. */
    private void pips(GuiGraphics graphics) {
        int total = cutting ? STROKES : FLAP_STEPS;
        int done = cutting ? strokes : traced;
        for (int i = 0; i < total; i++) {
            int x = left + width - 8 - i * 5;
            graphics.fill(x, top + 4, x + 3, top + 8,
                    i < done ? (cutting ? COLOR_BONE : COLOR_DONE) : Draw.alpha(COLOR_BONE, 0.25F));
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
