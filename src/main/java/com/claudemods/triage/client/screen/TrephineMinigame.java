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
 * Taking the pressure off something that has no room to swell.
 *
 * <p>The operation for the two bones no splint reaches. A skull and a spine are not broken
 * limbs: what makes them dangerous is not the bone at all, it is that the bone is a box,
 * and the thing inside the box is now being squeezed by its own swelling with nowhere to
 * go. Everything else in this mod can wait an hour. This cannot.
 *
 * <p>So the job is a hole, and the whole of the job is <b>where</b> and <b>how deep</b>.
 *
 * <p>Where: not on the fracture line, which is where the bone is already loose and where
 * a drill goes through into what is underneath; and not on a vessel, which is drawn
 * because the alternative is a game about guessing. Both of those are visible and both of
 * them are refused with the bit still spinning, rather than punished afterwards.
 *
 * <p>How deep: through the bone and not a millimetre past it. The gauge does not say where
 * to stop - what says it is the bone itself, which changes under the bit when it is nearly
 * through, and that is the whole of the sign a surgeon has.
 */
public final class TrephineMinigame implements Minigame {
    /** How far from the crack or a vessel the bit has to sit. */
    private static final float CLEAR = 16.0F;
    /** Where the bone gives way, as a share of the gauge, and how wide the window is. */
    private static final float THROUGH = 0.72F;
    private static final float WINDOW = 0.16F;
    /** And past this the bit is in what the bone was protecting. */
    private static final float TOO_FAR = 0.94F;
    /** How fast the bit goes down while it is being leaned on. */
    private static final float BITE = 0.011F;

    private static final int BONE = 0xFFEFE7D2;
    private static final int BONE_EDGE = 0xFFB6AC92;
    private static final int BONE_DEEP = 0xFFCFC3A6;
    private static final int UNDER = 0xFFB98C8C;
    private static final int CRACK = 0xFF8A6A3A;
    private static final int VESSEL = 0xFFA8281E;
    private static final int DRILL = 0xFF9AA4AE;
    private static final int DRILL_DARK = 0xFF5A646E;
    private static final int BAND = 0x44FFE070;
    private static final int BAND_EDGE = 0x99FFE070;
    private static final int GOOD = 0xFF7FD46B;
    private static final int BAD = 0xFFCC3333;
    private static final int EMPTY = 0x40FFFFFF;

    private enum Phase { PLACE, DRILL, DONE, RUINED }

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int centreY;
    /** A skull is a dome and a spine is a column. Same job, and they are not the same picture. */
    private final boolean skull;
    /** Where the bone is already broken, as a line across the field. */
    private final float crackX;
    private final float crackLean;
    /** And the two things under the bone that must not be gone through. */
    private final float[] vesselX = new float[2];

    private Phase phase = Phase.PLACE;
    private float bitX;
    private float bitY;
    private boolean placed;
    private boolean leaning;
    private float depth;
    private int refused;
    private int shake;
    private int age;

    public TrephineMinigame(int left, int top, int width, int height, boolean skull) {
        this.left = left;
        this.top = top;
        this.width = Math.max(80, width);
        this.height = Math.max(60, height);
        this.centreX = left + this.width / 2;
        this.centreY = top + this.height / 2;
        this.skull = skull;

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        this.crackX = centreX + (random.nextFloat() - 0.5F) * this.width * 0.35F;
        this.crackLean = (random.nextFloat() - 0.5F) * 0.5F;
        this.vesselX[0] = left + this.width * (0.2F + random.nextFloat() * 0.15F);
        this.vesselX[1] = left + this.width * (0.65F + random.nextFloat() * 0.15F);
        this.bitX = centreX;
        this.bitY = centreY;
    }

    // ---- the hands ----------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || phase == Phase.DONE || phase == Phase.RUINED) {
            return false;
        }
        if (phase == Phase.PLACE) {
            bitX = (float) mouseX;
            bitY = (float) mouseY;
            if (!clear()) {
                refused = 10;
                play(SoundEvents.ITEM_BREAK, 0.6F);
                return true;
            }
            placed = true;
            phase = Phase.DRILL;
            play(SoundEvents.STONE_HIT, 1.2F);
            return true;
        }
        leaning = true;
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0 || phase != Phase.DRILL) {
            return;
        }
        leaning = false;
        // Off the bit. If the bone has given way, that is the operation; if it has not,
        // nothing has happened yet and the bit can go back on.
        if (depth >= THROUGH - WINDOW) {
            phase = Phase.DONE;
            play(SoundEvents.BONE_BLOCK_BREAK, 1.1F);
        }
    }

    /** Whether the bit is somewhere a hole may go. */
    private boolean clear() {
        if (Math.abs(bitX - crackAt(bitY)) < CLEAR) {
            return false;
        }
        for (float vessel : vesselX) {
            if (Math.abs(bitX - vessel) < CLEAR * 0.75F) {
                return false;
            }
        }
        return bitY > top + 14 && bitY < top + height - 18;
    }

    /** Where the fracture line is at a given height. */
    private float crackAt(float y) {
        return crackX + (y - centreY) * crackLean;
    }

    @Override
    public void tick() {
        age++;
        if (refused > 0) {
            refused--;
        }
        if (shake > 0) {
            shake--;
        }
        if (phase != Phase.DRILL || !leaning) {
            return;
        }
        depth += BITE;
        if (age % 4 == 0) {
            play(SoundEvents.STONE_HIT, 0.8F + depth * 0.5F);
        }
        // The sign, and the only one: the bone changes under the bit when it is nearly
        // through. Drawn as a shake rather than said in words.
        if (depth >= THROUGH - WINDOW && shake <= 0) {
            shake = 3;
        }
        if (depth >= TOO_FAR) {
            phase = Phase.RUINED;
            play(SoundEvents.PLAYER_HURT, 0.5F);
        }
    }

    // ---- where it stands ----------------------------------------------------

    @Override
    public float progress() {
        return switch (phase) {
            case PLACE -> 0.0F;
            case DRILL -> 0.2F + Mth.clamp(depth / THROUGH, 0.0F, 1.0F) * 0.8F;
            default -> 1.0F;
        };
    }

    @Override
    public float quality() {
        if (phase == Phase.RUINED) {
            return 0.0F;
        }
        // How close to the far side of the window it was stopped, less what was spent
        // trying to put the bit somewhere it could not go.
        float over = Mth.clamp((depth - (THROUGH - WINDOW)) / (WINDOW * 2.0F), 0.0F, 1.0F);
        return Mth.clamp(1.0F - Math.abs(over - 0.5F) * 0.6F - refusedCost(), 0.2F, 1.0F);
    }

    private float refusedCost() {
        return 0.0F;
    }

    @Override
    public boolean complete() {
        return phase == Phase.DONE;
    }

    @Override
    public boolean failed() {
        return phase == Phase.RUINED;
    }

    @Override
    public Component instruction() {
        return switch (phase) {
            case RUINED -> Component.translatable("triage.minigame.trephine_through");
            case DONE -> Component.translatable("triage.minigame.trephine_done");
            case DRILL -> Component.translatable(depth >= THROUGH - WINDOW
                    ? "triage.minigame.trephine_giving" : "triage.minigame.trephine_lean");
            default -> Component.translatable(refused > 0
                    ? "triage.minigame.trephine_not_there" : "triage.minigame.trephine_place");
        };
    }

    // ---- drawing ------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        if (phase == Phase.PLACE) {
            bitX = mouseX;
            bitY = mouseY;
        }
        plate(graphics);
        hazards(graphics);
        bit(graphics);
        gauge(graphics);
    }

    /** The bone itself: a dome for a skull, a stack of blocks for a spine. */
    private void plate(GuiGraphics graphics) {
        graphics.fill(left, top, left + width, top + height, UNDER);
        if (skull) {
            int half = Math.min(width / 2 - 6, height - 26);
            for (int y = 0; y < height - 18; y++) {
                float t = y / (float) (height - 18);
                int span = Math.round(half * Mth.sqrt(Math.max(0.0F, 1.0F - (1.0F - t) * (1.0F - t))));
                int yy = top + 8 + y;
                graphics.fill(centreX - span, yy, centreX + span, yy + 1, BONE);
                graphics.fill(centreX - span, yy, centreX - span + 2, yy + 1, BONE_EDGE);
                graphics.fill(centreX + span - 2, yy, centreX + span, yy + 1, BONE_EDGE);
            }
        } else {
            int half = Math.min(width / 3, 46);
            for (int i = 0; i < 4; i++) {
                int yy = top + 10 + i * ((height - 28) / 4);
                graphics.fill(centreX - half, yy, centreX + half, yy + (height - 28) / 4 - 3, BONE);
                graphics.fill(centreX - half, yy + (height - 28) / 4 - 5, centreX + half,
                        yy + (height - 28) / 4 - 3, BONE_EDGE);
            }
        }
    }

    /** The crack, and the two things underneath that are worth avoiding. */
    private void hazards(GuiGraphics graphics) {
        for (int y = top + 8; y < top + height - 10; y += 3) {
            int x = Math.round(crackAt(y)) + ((y / 3) % 2 == 0 ? -1 : 1);
            graphics.fill(x, y, x + 2, y + 3, CRACK);
        }
        for (float vessel : vesselX) {
            int x = Math.round(vessel);
            for (int y = top + 10; y < top + height - 12; y += 4) {
                int wobble = (y / 4) % 2 == 0 ? 0 : 1;
                graphics.fill(x + wobble, y, x + wobble + 2, y + 4, VESSEL);
            }
        }
    }

    /** The bit, where it is, and whether it is anywhere it may be. */
    private void bit(GuiGraphics graphics) {
        int x = Math.round(bitX) + (shake > 0 ? (age % 2 == 0 ? 1 : -1) : 0);
        int y = Math.round(bitY);
        boolean ok = phase != Phase.PLACE || clear();
        int colour = refused > 0 || !ok ? BAD : phase == Phase.DRILL ? GOOD : DRILL;

        if (phase == Phase.PLACE) {
            Draw.ring(graphics, x, y, CLEAR * 0.55F, 18, 1, ok ? 0x66FFFFFF : 0x99CC3333);
        }
        // A hole that deepens as the bit goes down: the ring stays and the middle darkens.
        if (placed) {
            Draw.disc(graphics, x, y, 7, BONE_DEEP);
            int sunk = Math.round(Mth.clamp(depth / THROUGH, 0.0F, 1.0F) * 6.0F);
            Draw.disc(graphics, x, y, Math.max(1, sunk), depth >= THROUGH ? UNDER : BONE_EDGE);
        }
        graphics.fill(x - 2, top + 2, x + 3, y - 6, DRILL);
        graphics.fill(x - 1, top + 2, x + 1, y - 6, DRILL_DARK);
        graphics.fill(x - 4, y - 8, x + 5, y - 5, DRILL);
        Draw.disc(graphics, x, y, 3, colour);
    }

    /** How far down the bit has gone, with the window it has to be stopped in. */
    private void gauge(GuiGraphics graphics) {
        int barLeft = left + 6;
        int barTop = top + 10;
        int barHeight = height - 26;
        graphics.fill(barLeft, barTop, barLeft + 7, barTop + barHeight, 0x40000000);
        int from = barTop + Math.round(barHeight * (THROUGH - WINDOW));
        int to = barTop + Math.round(barHeight * (THROUGH + WINDOW));
        graphics.fill(barLeft, from, barLeft + 7, to, BAND);
        graphics.fill(barLeft, from, barLeft + 7, from + 1, BAND_EDGE);
        graphics.fill(barLeft, to - 1, barLeft + 7, to, BAND_EDGE);
        graphics.fill(barLeft, barTop + Math.round(barHeight * TOO_FAR), barLeft + 7,
                barTop + barHeight, 0x66CC3333);
        int at = barTop + Math.round(barHeight * Mth.clamp(depth, 0.0F, 1.0F));
        graphics.fill(barLeft - 2, at - 1, barLeft + 9, at + 1,
                depth >= THROUGH - WINDOW && depth < TOO_FAR ? GOOD : 0xFFF2ECE0);

        int holdLeft = left + 20;
        int holdWidth = width - 28;
        int holdTop = top + height - 8;
        graphics.fill(holdLeft, holdTop, holdLeft + holdWidth, holdTop + 4, EMPTY);
        graphics.fill(holdLeft, holdTop, holdLeft + Math.round(holdWidth * progress()), holdTop + 4,
                phase == Phase.RUINED ? BAD : GOOD);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F), 0.4F));
    }
}
