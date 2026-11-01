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
 * Putting a bone back where it came from.
 *
 * <p>The mod's splint game deliberately refused to be this. Splinting is done <em>as you
 * find it</em>, and the game for it is about a board and two ties. Straightening the bone
 * is a different job with different hands, and this is it - which is why the two look
 * nothing alike on screen.
 *
 * <p>Three things happen in order, and they are the three that happen in a corridor:
 *
 * <ol>
 *   <li><b>Pull.</b> Along the bone, away from the body, until the ends are far enough
 *       apart to pass each other. Not hard enough and they grind; too hard and something
 *       between them tears.</li>
 *   <li><b>Line it up.</b> While the pull is holding, bring the far half back under the
 *       near one. Moving it sideways <em>without</em> the pull is dragging one broken end
 *       across the other, which is exactly the thing everybody is afraid of.</li>
 *   <li><b>Let it down.</b> Slowly. The ends seat as the pull comes off, and letting go
 *       all at once drops them together however they happened to be lying.</li>
 * </ol>
 *
 * <p>None of it can be done well quickly, and all of it is done to somebody who can feel
 * it - which is the whole reason the mask in the bag is worth carrying, and the reason
 * this is the one treatment in the mod that the room can hear.
 */
public final class SetMinigame implements Minigame {
    /** How far the hand travels for a full pull. */
    private static final float PULL_TRAVEL = 90.0F;
    /** The band the pull has to be in: enough to clear the ends, not enough to tear. */
    private static final float PULL_LOW = 0.45F;
    private static final float PULL_HIGH = 0.82F;
    /** And where something gives. */
    private static final float PULL_TEAR = 0.97F;
    /** How far out of line it starts, and how close counts as back in line. */
    private static final float OFFSET_MAX = 26.0F;
    private static final float IN_LINE = 3.0F;
    /** How fast the pull falls away when nobody is holding it. */
    private static final float SLACK = 0.055F;
    /** Seating slower than this is a controlled let-down rather than a drop. */
    private static final float GENTLE = 0.045F;
    /** Nobody who got the bone back in line comes away with nothing. */
    private static final float FLOOR = 0.2F;

    private static final int COLOR_LIMB = 0xFFB07A5A;
    private static final int COLOR_LIMB_EDGE = 0xFF6E4632;
    private static final int COLOR_BONE = 0xFFEFE7D2;
    private static final int COLOR_BONE_EDGE = 0xFFB6AC92;
    private static final int COLOR_GRIND = 0xFFCC3333;
    private static final int COLOR_BAND = 0x44FFE070;
    private static final int COLOR_BAND_EDGE = 0x99FFE070;
    private static final int COLOR_TEAR = 0x66CC3333;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_EMPTY = 0x40FFFFFF;
    private static final int COLOR_AXIS = 0x40FFFFFF;

    private enum Phase { PULL, LINE, SEAT, DONE, TORN }

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int breakY;

    /** How hard it is being pulled, 0 to 1, and how far the far half is out of line. */
    private float pull;
    private float offset;
    private float lastPull;

    private boolean held;
    private float grabX;
    private float grabY;
    private float grabPull;
    private float grabOffset;

    private Phase phase = Phase.PULL;
    /** What the alignment was at the moment the ends seated, which is the whole score. */
    private float seatedOffset = OFFSET_MAX;
    private boolean gentle = true;
    private int age;
    private int grind;

    public SetMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.centreX = left + width / 2;
        this.breakY = top + height / 2;
        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        // Which way it has gone is never the same twice, and it is the first thing the
        // player has to read off the picture rather than off a number.
        this.offset = (random.nextBoolean() ? 1 : -1) * (14.0F + random.nextFloat() * 12.0F);
    }

    @Override
    public void tick() {
        age++;
        if (grind > 0) {
            grind--;
        }
        if (phase == Phase.DONE || phase == Phase.TORN) {
            return;
        }
        if (!held) {
            lastPull = pull;
            pull = Math.max(0.0F, pull - SLACK);
            // The ends come together as the pull comes off. Where they come together is
            // where they were, which is why letting go early is how a bone ends up set
            // crooked by somebody who did the hard part correctly.
            if (lastPull >= PULL_LOW && pull < PULL_LOW) {
                seat(lastPull - pull);
            }
        }
    }

    /** The ends have met. Everything this game was about is decided here. */
    private void seat(float rate) {
        seatedOffset = Math.abs(offset);
        gentle = rate <= GENTLE;
        phase = Phase.DONE;
        play(seatedOffset <= IN_LINE ? SoundEvents.BONE_BLOCK_PLACE : SoundEvents.BONE_BLOCK_HIT,
                seatedOffset <= IN_LINE ? 1.2F : 0.7F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || phase == Phase.DONE || phase == Phase.TORN) {
            return false;
        }
        held = true;
        grabX = (float) mouseX;
        grabY = (float) mouseY;
        grabPull = pull;
        grabOffset = offset;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!held || phase == Phase.DONE || phase == Phase.TORN) {
            return false;
        }
        // Down the screen is along the bone and away from the body: that is the pull.
        pull = Mth.clamp(grabPull + ((float) mouseY - grabY) / PULL_TRAVEL, 0.0F, 1.0F);
        if (pull >= PULL_TEAR) {
            phase = Phase.TORN;
            play(SoundEvents.PLAYER_HURT, 0.6F);
            return true;
        }
        // And across it is the alignment - but only while there is room for the ends to
        // pass each other. Without the pull, this is one broken end being dragged across
        // the other, and the game says so rather than quietly ignoring it.
        float wanted = grabOffset + ((float) mouseX - grabX);
        if (Math.abs(wanted - offset) > 0.2F && pull < PULL_LOW) {
            grind = 6;
            grabX = (float) mouseX;
            grabOffset = offset;
            return true;
        }
        offset = Mth.clamp(wanted, -OFFSET_MAX - 8.0F, OFFSET_MAX + 8.0F);
        phase = pull >= PULL_LOW && pull <= PULL_HIGH ? Phase.LINE : Phase.PULL;
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        held = false;
        // A hand taken off all at once is the ends dropping together rather than being
        // let down onto each other, and the difference is worth something.
        if (pull >= PULL_LOW) {
            gentle = false;
            phase = Phase.SEAT;
        }
    }

    @Override
    public float progress() {
        if (phase == Phase.DONE) {
            return 1.0F;
        }
        // Two halves: getting the pull into the band, and getting the line right.
        float pulled = Mth.clamp(pull / PULL_LOW, 0.0F, 1.0F) * 0.4F;
        float lined = (1.0F - Mth.clamp(Math.abs(offset) / OFFSET_MAX, 0.0F, 1.0F)) * 0.6F;
        return Mth.clamp(pulled + lined, 0.0F, 0.99F);
    }

    @Override
    public float quality() {
        if (phase == Phase.TORN) {
            return 0.0F;
        }
        if (seatedOffset > IN_LINE * 3.0F) {
            // Seated well out of line. The bone is together and it is together wrong,
            // which the server turns into exactly what that is worth: not a failure, and
            // not a reduction either.
            return FLOOR;
        }
        float accuracy = 1.0F - Mth.clamp(seatedOffset / (IN_LINE * 3.0F), 0.0F, 1.0F);
        return Math.max(FLOOR, accuracy * (gentle ? 1.0F : 0.75F));
    }

    @Override
    public boolean complete() {
        return phase == Phase.DONE;
    }

    @Override
    public boolean failed() {
        return phase == Phase.TORN;
    }

    @Override
    public Component instruction() {
        return switch (phase) {
            case TORN -> Component.translatable("triage.minigame.set_torn");
            case DONE -> Component.translatable(seatedOffset <= IN_LINE
                    ? "triage.minigame.set_done" : "triage.minigame.set_crooked");
            case SEAT -> Component.translatable("triage.minigame.set_seating");
            case LINE -> Math.abs(offset) <= IN_LINE
                    ? Component.translatable("triage.minigame.set_hold")
                    : Component.translatable("triage.minigame.set_line");
            default -> pull > PULL_HIGH
                    ? Component.translatable("triage.minigame.set_easier")
                    : Component.translatable("triage.minigame.set_pull");
        };
    }

    // ---- drawing ------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        limb(graphics);
        bone(graphics);
        gauge(graphics);
    }

    /** The limb, which follows the far half rather than staying square. */
    private void limb(GuiGraphics graphics) {
        int half = 18;
        int gap = Math.round(pull * 10.0F);
        int shift = Math.round(offset);
        graphics.fill(centreX - half - 1, top + 10, centreX + half + 1, breakY - gap, COLOR_LIMB_EDGE);
        Draw.body(graphics, centreX - half, top + 11, half * 2, breakY - gap - top - 11, COLOR_LIMB);
        graphics.fill(centreX + shift - half - 1, breakY + gap, centreX + shift + half + 1,
                top + height - 10, COLOR_LIMB_EDGE);
        Draw.body(graphics, centreX + shift - half, breakY + gap, half * 2,
                height - 10 - (breakY + gap - top), COLOR_LIMB);
        // Where the bone ought to be pointing, so "in line" is a thing you can see.
        for (int y = top + 12; y < top + height - 12; y += 6) {
            graphics.fill(centreX - 1, y, centreX, y + 3, COLOR_AXIS);
        }
    }

    private void bone(GuiGraphics graphics) {
        int gap = Math.round(pull * 10.0F);
        int shift = Math.round(offset);
        // The near half, which never moves. It is the thing the other one is brought to.
        graphics.fill(centreX - 6, top + 18, centreX + 6, breakY - gap, COLOR_BONE_EDGE);
        graphics.fill(centreX - 5, top + 19, centreX + 5, breakY - gap - 1, COLOR_BONE);
        // And the far half, which is where the hands are.
        graphics.fill(centreX + shift - 6, breakY + gap, centreX + shift + 6, top + height - 18,
                COLOR_BONE_EDGE);
        graphics.fill(centreX + shift - 5, breakY + gap + 1, centreX + shift + 5, top + height - 19,
                COLOR_BONE);

        boolean lined = Math.abs(offset) <= IN_LINE;
        if (grind > 0) {
            // Two broken ends, meeting. Drawn rather than explained.
            for (int i = -6; i <= 6; i += 3) {
                Draw.disc(graphics, centreX + shift + i, breakY, 2, COLOR_GRIND);
            }
        }
        if (lined && phase != Phase.DONE) {
            graphics.fill(centreX - 8, breakY - gap - 1, centreX + 8, breakY - gap, COLOR_GOOD);
            graphics.fill(centreX - 8, breakY + gap, centreX + 8, breakY + gap + 1, COLOR_GOOD);
        }
        if (phase == Phase.DONE) {
            int colour = seatedOffset <= IN_LINE ? COLOR_GOOD : COLOR_GRIND;
            graphics.fill(centreX + shift - 7, breakY - 1, centreX + shift + 7, breakY + 1, colour);
        }
    }

    /** How hard it is being pulled, against the band it has to be in. */
    private void gauge(GuiGraphics graphics) {
        int barLeft = left + 8;
        int barTop = top + 12;
        int barHeight = height - 30;
        graphics.fill(barLeft, barTop, barLeft + 8, barTop + barHeight, 0x40000000);
        int from = barTop + Math.round(barHeight * PULL_LOW);
        int to = barTop + Math.round(barHeight * PULL_HIGH);
        graphics.fill(barLeft, from, barLeft + 8, to, COLOR_BAND);
        graphics.fill(barLeft, from, barLeft + 8, from + 1, COLOR_BAND_EDGE);
        graphics.fill(barLeft, to - 1, barLeft + 8, to, COLOR_BAND_EDGE);
        // And the part of it nobody should reach, which is marked because a rule that
        // can end the job should never be a surprise.
        graphics.fill(barLeft, barTop + Math.round(barHeight * PULL_TEAR), barLeft + 8,
                barTop + barHeight, COLOR_TEAR);
        int at = barTop + Math.round(barHeight * pull);
        graphics.fill(barLeft - 3, at - 1, barLeft + 11, at + 1,
                pull >= PULL_LOW && pull <= PULL_HIGH ? COLOR_GOOD : 0xFFF2ECE0);

        int holdLeft = left + 24;
        int holdWidth = width - 34;
        int holdTop = top + height - 8;
        graphics.fill(holdLeft, holdTop, holdLeft + holdWidth, holdTop + 4, COLOR_EMPTY);
        graphics.fill(holdLeft, holdTop, holdLeft + Math.round(holdWidth * progress()), holdTop + 4,
                phase == Phase.TORN ? COLOR_GRIND : COLOR_GOOD);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
