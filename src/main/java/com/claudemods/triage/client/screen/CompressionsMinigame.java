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
 * Compressions: deep enough, and all the way back up.
 *
 * <p>What was here was a rhythm game - press on the beat, six times. Rate is one of the
 * three things that matter and it is the one everybody already knows. The other two are
 * where compressions are actually lost, and neither was in it.
 *
 * <p><b>Depth.</b> Two inches, near enough, and shallow compressions do nothing at all:
 * not less, nothing. The chest has to be squeezed hard enough to squeeze the heart inside
 * it, and the number of people doing this too gently is most of them.
 *
 * <p><b>Recoil.</b> The hands have to come all the way off the pressure between each one,
 * because the heart only fills while the chest is back out. Leaning on the chest between
 * compressions is the commonest fault there is, it is invisible to the person doing it,
 * and it halves everything.
 *
 * <p>So a compression here is a press and a release, both measured, on a beat. Three
 * things, and the screen shows all three: how deep this one went, whether the chest came
 * back, and where the beat is. Ribs still go - they go at the bottom of a compression
 * that is too deep, which is where they go.
 */
public final class CompressionsMinigame implements Minigame {
    private static final int ROUNDS = 6;
    /** Ticks between beats: a hundred and ten a minute, near enough. */
    private static final int BEAT_TICKS = 11;
    /** How close to the beat still counts as on it. */
    private static final int ON_BEAT = 4;

    /** How far down the chest goes at full press, and the band a compression must reach. */
    private static final float PRESS_TRAVEL = 46.0F;
    private static final float DEEP_ENOUGH = 0.60F;
    private static final float TOO_DEEP = 0.90F;
    /** Under this the chest counts as back out. */
    private static final float RECOILED = 0.12F;

    private static final int COLOR_BODY = 0xFFB07A5A;
    private static final int COLOR_BODY_EDGE = 0xFF6E4632;
    private static final int COLOR_CHEST = 0xFFC08A6A;
    private static final int COLOR_RIB = 0xFFE4DCC4;
    private static final int COLOR_HANDS = 0xFFF2ECE0;
    private static final int COLOR_HANDS_EDGE = 0xFFB9B2A0;
    private static final int COLOR_BAND = 0x44FFE070;
    private static final int COLOR_BAND_EDGE = 0x99FFE070;
    private static final int COLOR_BEAT = 0xFFFFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;
    private static final int COLOR_EMPTY = 0x40FFFFFF;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int chestY;

    /** 0 chest out, 1 pressed to the floor. */
    private float press;
    private boolean held;
    /** How deep this press has been so far, so a compression is judged by its lowest point. */
    private float lowest;
    private boolean pressing;
    private boolean recoiled = true;

    private int done;
    private int shallow;
    private int leaned;
    private int cracked;
    private int beat;
    private int sinceBeat;
    private int flash;
    private int flashColour;

    public CompressionsMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        this.left = areaLeft;
        this.top = areaTop;
        this.width = areaWidth;
        this.height = areaHeight;
        this.centreX = areaLeft + areaWidth / 2;
        this.chestY = areaTop + Math.round(areaHeight * 0.34F);
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public void tick() {
        if (flash > 0) {
            flash--;
        }
        if (complete()) {
            return;
        }
        if (++sinceBeat >= BEAT_TICKS) {
            sinceBeat = 0;
            beat++;
        }
        if (!held) {
            // The chest comes back out on its own the moment the hands let it.
            press = Math.max(0.0F, press - 0.14F);
            if (press <= RECOILED) {
                recoiled = true;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        pressing = true;
        lowest = 0.0F;
        aim(mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || !held || complete()) {
            return false;
        }
        aim(mouseY);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        held = false;
        if (pressing) {
            pressing = false;
            land();
        }
    }

    private void aim(double mouseY) {
        press = Mth.clamp(((float) mouseY - chestY) / PRESS_TRAVEL, 0.0F, 1.0F);
        lowest = Math.max(lowest, press);
        if (press > RECOILED) {
            recoiled = false;
        }
        // Ribs go at the bottom of a compression, not on the way up.
        if (press >= TOO_DEEP && flash == 0) {
            cracked++;
            mark(COLOR_BAD);
            play(SoundEvents.BONE_BLOCK_BREAK, 0.7F);
        }
    }

    /**
     * One compression, judged when the hands come off it.
     *
     * <p>By its lowest point rather than where the hands happened to be at the end, which
     * is the honest measure: a compression is how far the chest went.
     */
    private void land() {
        if (lowest < DEEP_ENOUGH) {
            shallow++;
            mark(COLOR_BAD);
            play(SoundEvents.WOOL_HIT, 0.7F);
            return;
        }
        if (!recoiledSinceLast()) {
            leaned++;
            mark(COLOR_BAD);
            play(SoundEvents.WOOL_HIT, 0.8F);
            return;
        }
        done++;
        mark(onBeat() ? COLOR_GOOD : COLOR_BEAT);
        play(SoundEvents.PLAYER_ATTACK_STRONG, onBeat() ? 1.1F : 0.9F);
    }

    /**
     * Whether the chest actually came back out before this one.
     *
     * <p>The invisible fault, made visible: leaning on the chest between compressions is
     * what most people do and none of them know they are doing it, so the game has to be
     * the thing that knows.
     */
    private boolean recoiledSinceLast() {
        boolean was = recoiled;
        recoiled = false;
        return was || done == 0;
    }

    private boolean onBeat() {
        return sinceBeat <= ON_BEAT || sinceBeat >= BEAT_TICKS - ON_BEAT;
    }

    private void mark(int colour) {
        flash = 10;
        flashColour = colour;
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        return Mth.clamp(done / (float) ROUNDS, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return done >= ROUNDS;
    }

    /** Compressions are not abandoned. They are done badly or they are done. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - (shallow * 0.10F + leaned * 0.10F + cracked * 0.15F), 0.0F, 1.0F);
    }

    @Override
    public Component instruction() {
        if (flash > 0 && flashColour == COLOR_BAD) {
            if (cracked > 0 && press >= TOO_DEEP) {
                return Component.translatable("triage.minigame.cpr_deep");
            }
            if (leaned > shallow) {
                return Component.translatable("triage.minigame.cpr_leaning");
            }
            return Component.translatable("triage.minigame.cpr_shallow");
        }
        return Component.translatable(held ? "triage.minigame.cpr_down" : "triage.minigame.cpr_up");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        body(graphics);
        depth(graphics);
        metronome(graphics);
        pips(graphics);
    }

    /** The chest, pressed as far as it is pressed. */
    private void body(GuiGraphics graphics) {
        int sink = Math.round(PRESS_TRAVEL * press * 0.35F);
        int halfWidth = 44;
        graphics.fill(centreX - halfWidth - 1, chestY - 21, centreX + halfWidth + 1,
                top + height - 12, COLOR_BODY_EDGE);
        Draw.body(graphics, centreX - halfWidth, chestY - 20, halfWidth * 2,
                top + height - 12 - (chestY - 20), COLOR_BODY);
        graphics.fill(centreX - 30, chestY - 14 + sink, centreX + 30, chestY + 14, COLOR_CHEST);
        for (int i = -10; i <= 10; i += 10) {
            graphics.fill(centreX - 26, chestY + i + sink / 2, centreX + 26, chestY + i + 2 + sink / 2,
                    Draw.alpha(COLOR_RIB, 0.5F));
        }

        // The hands, where they are and how flat.
        int handY = chestY - 16 + sink;
        int colour = flash > 0 ? flashColour : COLOR_HANDS;
        graphics.fill(centreX - 13, handY - 7, centreX + 13, handY + 5, COLOR_HANDS_EDGE);
        graphics.fill(centreX - 12, handY - 6, centreX + 12, handY + 4, colour);
        graphics.fill(centreX - 8, handY - 12, centreX + 8, handY - 6, Draw.shade(colour, 0.88F));
    }

    /**
     * How far this compression has gone, with the band it has to reach.
     *
     * <p>Two marks rather than one: the bottom of the band is the depth below which a
     * compression is not a compression, and the top of it is where the ribs are.
     */
    private void depth(GuiGraphics graphics) {
        int barLeft = left + 10;
        int barTop = chestY - 22;
        int barHeight = Math.round(PRESS_TRAVEL);
        graphics.fill(barLeft, barTop, barLeft + 9, barTop + barHeight, 0x40000000);
        int from = barTop + Math.round(barHeight * DEEP_ENOUGH);
        int to = barTop + Math.round(barHeight * TOO_DEEP);
        graphics.fill(barLeft, from, barLeft + 9, to, COLOR_BAND);
        graphics.fill(barLeft, from, barLeft + 9, from + 1, COLOR_BAND_EDGE);
        graphics.fill(barLeft, to - 1, barLeft + 9, to, COLOR_BAD);

        int at = barTop + Math.round(barHeight * press);
        boolean good = press >= DEEP_ENOUGH && press < TOO_DEEP;
        graphics.fill(barLeft - 3, at - 1, barLeft + 12, at + 1,
                good ? COLOR_GOOD : press >= TOO_DEEP ? COLOR_BAD : COLOR_HANDS);

        // Whether the chest is back out, which is the other half and has no other home.
        int lampY = barTop + barHeight + 8;
        graphics.fill(barLeft, lampY, barLeft + 9, lampY + 5, recoiled ? COLOR_GOOD : COLOR_BAD);
    }

    /** The beat, as a ring that closes. */
    private void metronome(GuiGraphics graphics) {
        int x = left + width - 24;
        int y = chestY - 4;
        float phase = sinceBeat / (float) BEAT_TICKS;
        Draw.ring(graphics, x, y, 12.0F, 12, 1, COLOR_EMPTY);
        Draw.onRing(graphics, x, y, 12.0F, (phase * Mth.TWO_PI) - Mth.HALF_PI, 3,
                onBeat() ? COLOR_GOOD : COLOR_BEAT);
        Draw.disc(graphics, x, y, onBeat() ? 5 : 3, onBeat() ? COLOR_BEAT : COLOR_EMPTY);
    }

    private void pips(GuiGraphics graphics) {
        for (int i = 0; i < ROUNDS; i++) {
            int x = left + width - 10 - i * 7;
            graphics.fill(x, top + 4, x + 4, top + 8, i < done ? COLOR_GOOD : COLOR_EMPTY);
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
