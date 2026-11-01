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
 * Pressure: on the one place it is coming from, hard, and without looking.
 *
 * <p>What was here was a force band - push in the middle of a gauge and hold. Force is
 * the least of it. The two things that decide whether pressure works are both things
 * people get wrong on purpose, believing they are being careful.
 *
 * <p><b>It has to be on the point.</b> Blood pools where gravity takes it, so the middle
 * of the mess is almost never the middle of the hole. Pressing on the pool is pressing on
 * nothing; the bleeding has one source, and finding it is the first half of the job.
 *
 * <p><b>And it has to stay there.</b> Lifting the hand to see whether it has worked is
 * the single commonest way pressure fails: whatever was starting to form comes away with
 * your fingers and the clock starts again. So looking, here, costs exactly what it costs.
 *
 * <p>The pool is drawn, the point is not. What gives it away is the same thing that gives
 * it away in life: the pool is fed from somewhere, and if you watch it you can see which
 * way it is running.
 */
public final class PressMinigame implements Minigame {
    /** How long the hand has to stay on it. */
    private static final int HOLD_TICKS = 70;
    /** How close to the source counts as on it. */
    private static final float ON_POINT = 9.0F;
    /** How hard, as a share of the gauge, and how much either way is close enough. */
    private static final float FORCE_TARGET = 0.66F;
    private static final float FORCE_BAND = 0.16F;
    /** How far the hand travels for full force. */
    private static final float FORCE_TRAVEL = 46.0F;
    /** How much of the hold is lost by taking the hand off to look. */
    private static final float PEEK_COST = 0.45F;

    private static final int COLOR_SKIN = 0xFFB07A5A;
    private static final int COLOR_SKIN_EDGE = 0xFF6E4632;
    private static final int COLOR_POOL = 0xFF8E1616;
    private static final int COLOR_POOL_DEEP = 0xFF5E0E0E;
    private static final int COLOR_SPURT = 0xFFE04A42;
    private static final int COLOR_HAND = 0xFFF2ECE0;
    private static final int COLOR_HAND_EDGE = 0xFFB9B2A0;
    private static final int COLOR_BAND = 0x44FFE070;
    private static final int COLOR_BAND_EDGE = 0x99FFE070;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;
    private static final int COLOR_EMPTY = 0x40FFFFFF;
    /** Brighter, because it has not been anywhere yet. */
    private static final int COLOR_ARTERY = 0xFFF03A22;
    private static final int COLOR_ARTERY_DEEP = 0xFFB01A10;
    /** How much harder an artery has to be held, and for how much longer. */
    private static final float ARTERY_FORCE = 0.86F;
    private static final float ARTERY_BAND = 0.12F;
    private static final int ARTERY_HOLD = 110;
    /** Ticks between beats, which is what this game is played against rather than a clock. */
    private static final int BEAT = 19;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int centreY;
    /** Where it is actually coming from, which is not the middle of the mess. */
    private final int sourceX;
    private final int sourceY;
    /**
     * Whether what is under the hand is a vessel.
     *
     * <p>The same game and not the same job. A cut is held until it clots; an artery is
     * held because letting go means it starts again from nothing, and the clock under it
     * is the only thing between this patient and the floor. So it wants more force, it
     * wants it for longer, and taking the hand off is not a setback - it is starting
     * over.
     */
    private final boolean artery;

    private boolean held;
    private float handX;
    private float handY;
    private float grabY;
    private float force;
    private int holding;
    private int peeks;
    private int age;
    private int flash;

    public PressMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        this(areaLeft, areaTop, areaWidth, areaHeight, false);
    }

    public PressMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight, boolean artery) {
        this.artery = artery;
        this.left = areaLeft;
        this.top = areaTop;
        this.width = areaWidth;
        this.height = areaHeight;
        this.centreX = areaLeft + areaWidth / 2;
        this.centreY = areaTop + areaHeight / 2;

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        float angle = random.nextFloat() * Mth.TWO_PI;
        float away = 14.0F + random.nextFloat() * 12.0F;
        this.sourceX = centreX + Math.round(Mth.cos(angle) * away);
        this.sourceY = centreY + Math.round(Mth.sin(angle) * away * 0.8F);
        this.handX = centreX;
        this.handY = centreY;
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public void tick() {
        age++;
        if (flash > 0) {
            flash--;
        }
        if (complete()) {
            return;
        }
        if (held && onPoint() && goodForce()) {
            holding++;
        } else if (held && !onPoint()) {
            // On the pool rather than the hole: the hand is doing nothing, and the game
            // says so by doing nothing.
            flash = Math.max(flash, 1);
        }
    }

    private boolean onPoint() {
        return Math.abs(handX - sourceX) <= ON_POINT && Math.abs(handY - sourceY) <= ON_POINT;
    }

    private boolean goodForce() {
        return Math.abs(force - target()) <= band();
    }

    private float target() {
        return artery ? ARTERY_FORCE : FORCE_TARGET;
    }

    private float band() {
        return artery ? ARTERY_BAND : FORCE_BAND;
    }

    private int holdTicks() {
        return artery ? ARTERY_HOLD : HOLD_TICKS;
    }

    /** Where the beat is, 0 to 1, for everything that moves in time with it. */
    private float pulse() {
        return (age % BEAT) / (float) BEAT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        handX = (float) mouseX;
        handY = (float) mouseY;
        grabY = (float) mouseY;
        force = 0.0F;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || !held || complete()) {
            return false;
        }
        // Where the hand is stays where it was put; pushing down is what leaning on it
        // means, and it is a second thing rather than a different thing.
        force = Mth.clamp(((float) mouseY - grabY) / FORCE_TRAVEL, 0.0F, 1.0F);
        if (force <= 0.05F) {
            handX = (float) mouseX;
            handY = (float) mouseY;
            grabY = (float) mouseY;
        }
        return true;
    }

    /**
     * Taking the hand off.
     *
     * <p>Which is the whole of the second rule. Nothing is refused and nothing fails -
     * what was forming simply comes away, and the clock goes back most of the way to
     * where it started.
     */
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        held = false;
        force = 0.0F;
        if (holding > 0 && !complete()) {
            peeks++;
            // On a cut, what was forming comes away with the fingers. On an artery there
            // was never anything forming: the only thing holding it shut was the hand.
            holding = artery ? 0 : Math.round(holding * (1.0F - PEEK_COST));
            flash = 16;
            play(SoundEvents.SLIME_SQUISH, 0.7F);
        }
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        return Mth.clamp(holding / (float) holdTicks(), 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return holding >= holdTicks();
    }

    /** A hand is not a thing that runs out. It is only ever in the wrong place. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - peeks * 0.18F, 0.0F, 1.0F);
    }

    @Override
    public Component instruction() {
        if (!held) {
            return Component.translatable(holding > 0
                    ? "triage.minigame.press_lifted" : "triage.minigame.press_find");
        }
        if (!onPoint()) {
            return Component.translatable("triage.minigame.press_off");
        }
        if (force < target() - band()) {
            return Component.translatable(artery
                    ? "triage.minigame.press_artery_harder" : "triage.minigame.press_harder");
        }
        if (force > target() + band()) {
            return Component.translatable("triage.minigame.press_softer");
        }
        return Component.translatable(artery
                ? "triage.minigame.press_artery_hold" : "triage.minigame.press_hold");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        skin(graphics);
        blood(graphics);
        hand(graphics);
        gauge(graphics);
    }

    private void skin(GuiGraphics graphics) {
        int half = Math.min(76, width / 2 - 16);
        graphics.fill(centreX - half - 1, top + 7, centreX + half + 1, top + height - 15, COLOR_SKIN_EDGE);
        Draw.body(graphics, centreX - half, top + 8, half * 2, height - 24, COLOR_SKIN);
    }

    /**
     * The pool, and the one thing that gives the hole away.
     *
     * <p>The source is never marked. What is drawn is what would be visible: a pool, and
     * a run of blood leaving the point it is fed from. Watch it and you know where to
     * press, which is the same way anybody knows.
     */
    private void blood(GuiGraphics graphics) {
        if (complete()) {
            return;
        }
        boolean stopped = held && onPoint() && goodForce();
        if (artery) {
            jet(graphics, stopped);
            return;
        }
        for (int i = 0; i < 26; i++) {
            float angle = i * 0.62F;
            float away = 6.0F + (i % 7) * 3.4F;
            int x = centreX + Math.round(Mth.cos(angle) * away * 1.5F);
            int y = centreY + Math.round(Mth.sin(angle) * away);
            Draw.disc(graphics, x, y, 3, i % 3 == 0 ? COLOR_POOL_DEEP : COLOR_POOL);
        }
        if (!stopped) {
            // The run: from the source, in whichever direction it is going, moving.
            for (int i = 0; i < 5; i++) {
                int step = (age * 2 + i * 7) % 30;
                int x = sourceX + Math.round((centreX - sourceX) * step / 30.0F);
                int y = sourceY + Math.round((centreY - sourceY) * step / 30.0F);
                Draw.disc(graphics, x, y, 2, COLOR_SPURT);
            }
            Draw.disc(graphics, sourceX, sourceY, 2 + (age / 6) % 2, COLOR_SPURT);
        }
    }

    /**
     * What an artery looks like, which is the whole reason this is a separate picture.
     *
     * <p>It does not pool and spread. It throws, in a line, away from the body, and it
     * does it again every time the heart does - so the thing on screen is not a stain
     * getting slowly bigger, it is a rhythm. Held properly the jet stops and only the
     * mess that is already there is left, which is exactly how anybody knows the hand is
     * in the right place.
     */
    private void jet(GuiGraphics graphics, boolean stopped) {
        // What has already come out, and is not going anywhere.
        for (int i = 0; i < 22; i++) {
            float angle = i * 0.71F;
            float away = 8.0F + (i % 6) * 4.0F;
            int x = centreX + Math.round(Mth.cos(angle) * away * 1.6F);
            int y = centreY + Math.round(Mth.sin(angle) * away);
            Draw.disc(graphics, x, y, 3, i % 3 == 0 ? COLOR_ARTERY_DEEP : COLOR_POOL);
        }
        if (stopped) {
            Draw.disc(graphics, sourceX, sourceY, 3, COLOR_ARTERY_DEEP);
            return;
        }
        // And the jet, which arrives all at once and is gone before the next one.
        float beat = pulse();
        float reach = beat < 0.45F ? Mth.sin(beat / 0.45F * Mth.PI) : 0.0F;
        int length = Math.round(reach * Math.min(58, width / 2.6F));
        for (int i = 0; i < length; i += 2) {
            int x = sourceX + Math.round((sourceX - centreX) * 0.07F * i);
            int y = sourceY - i;
            Draw.disc(graphics, x, y, i < length / 2 ? 3 : 2,
                    i < 8 ? COLOR_ARTERY : COLOR_ARTERY_DEEP);
        }
        Draw.disc(graphics, sourceX, sourceY, 3 + Math.round(reach * 2.0F), COLOR_ARTERY);
    }

    private void hand(GuiGraphics graphics) {
        if (!held) {
            return;
        }
        int x = Math.round(Mth.clamp(handX, left + 6, left + width - 6));
        int y = Math.round(Mth.clamp(handY, top + 6, top + height - 6));
        int squash = Math.round(force * 4.0F);
        int colour = flash > 0 ? COLOR_BAD : onPoint() && goodForce() ? COLOR_GOOD : COLOR_HAND;
        graphics.fill(x - 12, y - 9 + squash, x + 12, y + 9, COLOR_HAND_EDGE);
        graphics.fill(x - 11, y - 8 + squash, x + 11, y + 8, colour);
        for (int i = -6; i <= 6; i += 6) {
            graphics.fill(x + i, y - 13 + squash, x + i + 3, y - 8 + squash, Draw.shade(colour, 0.9F));
        }
    }

    /** How hard, and how long it has been held. */
    private void gauge(GuiGraphics graphics) {
        int barLeft = left + 8;
        int barTop = top + 10;
        int barHeight = height - 30;
        graphics.fill(barLeft, barTop, barLeft + 8, barTop + barHeight, 0x40000000);
        int from = barTop + Math.round(barHeight * (target() - band()));
        int to = barTop + Math.round(barHeight * (target() + band()));
        graphics.fill(barLeft, from, barLeft + 8, to, COLOR_BAND);
        graphics.fill(barLeft, from, barLeft + 8, from + 1, COLOR_BAND_EDGE);
        graphics.fill(barLeft, to - 1, barLeft + 8, to, COLOR_BAND_EDGE);
        int at = barTop + Math.round(barHeight * force);
        graphics.fill(barLeft - 3, at - 1, barLeft + 11, at + 1, goodForce() ? COLOR_GOOD : COLOR_HAND);

        int holdLeft = left + 22;
        int holdWidth = width - 32;
        int holdTop = top + height - 8;
        graphics.fill(holdLeft, holdTop, holdLeft + holdWidth, holdTop + 4, COLOR_EMPTY);
        graphics.fill(holdLeft, holdTop, holdLeft + Math.round(holdWidth * progress()), holdTop + 4,
                flash > 0 ? COLOR_BAD : COLOR_GOOD);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
