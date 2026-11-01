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
 * A worming tablet, and the reason it is not the same game as the other card.
 *
 * <p>The blister pack game already exists and its own note says the right thing: a card is
 * a card, and a second game about pressing foil would be a second game about pressing foil.
 * That is true of the two cards side by side and it stops being true at the moment the
 * tablet is out, because these two tablets are not taken the same way.
 *
 * <p>A worming tablet is <b>big</b>. It is chalky, it is bitter, and it is the one tablet
 * that regularly will not go down a patient who is ill enough to need it - which is why
 * anybody who has given one to a child, an animal, or somebody on the floor has done the
 * same two things: crushed it, and stirred it into something.
 *
 * <p>So that is the game. Grind it under the back of a spoon until there are no lumps, then
 * stir it into water until nothing is sitting on the bottom. Both halves are worth doing
 * badly: a half-crushed tablet leaves grit, and grit settles, and what settles is the dose
 * somebody did not get.
 */
public final class WormingMinigame implements Minigame {
    private static final int GRIND = 0;
    private static final int STIR = 1;
    private static final int DONE = 2;

    /** How much grinding it takes, in pixels of travel. */
    private static final float GROUND = 620.0F;
    /** How many turns of the spoon settle the powder in. */
    private static final float STIRRED = 5.5F;
    /** Lumps to break, drawn where they are and rubbed out as they go. */
    private static final int LUMPS = 7;

    private static final int DISH = 0xFFD8DDE2;
    private static final int DISH_EDGE = 0xFF9AA3AB;
    private static final int DISH_DEEP = 0xFFB6BEC6;
    private static final int TABLET = 0xFFF1EDE2;
    private static final int GRIT = 0xFFCFC7B4;
    private static final int POWDER = 0xFFE9E4D6;
    private static final int WATER = 0xC05E86A8;
    private static final int WATER_LIT = 0xC07FA6C4;
    private static final int SPOON = 0xFFC6CBD2;
    private static final int SPOON_DARK = 0xFF878E96;
    private static final int GOOD = 0xFF7FD46B;
    private static final int POOR = 0xFFFFD37F;
    private static final int QUIET = 0xFFBFC6CC;

    private final int left;
    private final int top;
    private final int width;
    private final int height;

    private final float[] lumpX = new float[LUMPS];
    private final float[] lumpY = new float[LUMPS];
    private final boolean[] broken = new boolean[LUMPS];

    private int phase = GRIND;
    private float travel;
    private float lastX;
    private float lastY;
    private boolean hasLast;
    private boolean down;

    /** Turns of the spoon, counted by the angle actually swept rather than by clicks. */
    private float turns;
    private float lastAngle;
    private boolean hasAngle;
    private float settled;
    private float lastSettled;

    public WormingMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = Math.max(80, width);
        this.height = Math.max(60, height);
        float seed = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.random.nextFloat() : 0.5F;
        for (int i = 0; i < LUMPS; i++) {
            float a = (float) (i * 2.39996 + seed * 6.28);
            float d = 0.24F + (i % 3) * 0.14F;
            lumpX[i] = centreX() + Mth.cos(a) * (radius() * d);
            lumpY[i] = centreY() + Mth.sin(a) * (radius() * d * 0.8F);
        }
    }

    private int centreX() {
        return left + width / 2;
    }

    private int centreY() {
        return top + height / 2;
    }

    private int radius() {
        return Math.min(width / 2, height / 2) - 6;
    }

    // ---- the hand ----------------------------------------------------------

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0 || phase == DONE) {
            return false;
        }
        down = true;
        lastX = (float) x;
        lastY = (float) y;
        hasLast = true;
        hasAngle = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button) {
        if (button != 0 || phase == DONE || !down) {
            return false;
        }
        if (phase == GRIND) {
            grind((float) x, (float) y);
        } else {
            stir((float) x, (float) y);
        }
        lastX = (float) x;
        lastY = (float) y;
        return true;
    }

    @Override
    public void mouseReleased(double x, double y, int button) {
        down = false;
        hasLast = false;
        hasAngle = false;
    }

    /**
     * Rubbing it out, and only where the spoon actually is.
     *
     * <p>Travel counts everywhere so the work is real, but a lump only goes when it has
     * been gone over - which is what stops the fastest way through being a scribble in one
     * corner.
     */
    private void grind(float x, float y) {
        if (!hasLast) {
            return;
        }
        float dx = x - lastX;
        float dy = y - lastY;
        travel += Mth.sqrt(dx * dx + dy * dy);
        for (int i = 0; i < LUMPS; i++) {
            if (broken[i]) {
                continue;
            }
            float lx = lumpX[i] - x;
            float ly = lumpY[i] - y;
            if (lx * lx + ly * ly < 36.0F) {
                broken[i] = true;
                sound(SoundEvents.GRAVEL_BREAK, 1.6F);
            }
        }
        if (travel >= GROUND && allBroken()) {
            phase = STIR;
            sound(SoundEvents.BUCKET_FILL, 1.2F);
        }
    }

    private boolean allBroken() {
        for (boolean b : broken) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    /** Turning the spoon, counted as the angle it actually sweeps. */
    private void stir(float x, float y) {
        float angle = (float) Math.atan2(y - centreY(), x - centreX());
        if (!hasAngle) {
            lastAngle = angle;
            hasAngle = true;
            return;
        }
        float swept = Mth.degreesDifference((float) Math.toDegrees(lastAngle),
                (float) Math.toDegrees(angle));
        lastAngle = angle;
        turns += Math.abs(swept) / 360.0F;
        if (turns >= STIRRED) {
            phase = DONE;
            sound(SoundEvents.GENERIC_DRINK, 1.0F);
        }
    }

    private void sound(SoundEvent event, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(event, pitch, 0.35F));
    }

    @Override
    public void tick() {
        lastSettled = settled;
        // What is in suspension, which is what the glass looks like: it clouds as it is
        // stirred and it is the cloud that gets swallowed.
        float want = phase == GRIND ? 0.0F : Mth.clamp(turns / STIRRED, 0.0F, 1.0F);
        settled += (want - settled) * 0.25F;
    }

    // ---- what it looks like -------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int cx = centreX();
        int cy = centreY();
        int r = radius();

        // The dish, from above.
        Draw.disc(graphics, cx, cy, r, DISH_EDGE);
        Draw.disc(graphics, cx, cy, r - 2, DISH);
        Draw.disc(graphics, cx, cy, r - 6, DISH_DEEP);

        if (phase == GRIND) {
            tablet(graphics, cx, cy, r);
        } else {
            water(graphics, cx, cy, r, Mth.lerp(partialTick, lastSettled, settled));
        }
        spoon(graphics, mouseX, mouseY);

        Component note;
        int colour;
        if (phase == DONE) {
            note = Component.translatable("triage.minigame.worm_done");
            colour = GOOD;
        } else if (phase == GRIND) {
            int left = 0;
            for (boolean b : broken) {
                if (!b) {
                    left++;
                }
            }
            note = left > 0
                    ? Component.translatable("triage.minigame.worm_lumps", left)
                    : Component.translatable("triage.minigame.worm_grinding");
            colour = left > 0 ? POOR : GOOD;
        } else {
            note = Component.translatable("triage.minigame.worm_stirring");
            colour = POOR;
        }
        graphics.drawString(font, note, this.left + 2, top + height - 9, colour, false);
    }

    /** The tablet, going from one lump to powder with lumps left in it. */
    private void tablet(GuiGraphics graphics, int cx, int cy, int r) {
        float done = Mth.clamp(travel / GROUND, 0.0F, 1.0F);
        // The powder spreads as it is worked.
        int spread = Math.round(r * (0.2F + 0.55F * done));
        Draw.disc(graphics, cx, cy, spread, POWDER);
        if (done < 0.9F) {
            Draw.disc(graphics, cx, cy, Math.max(2, Math.round(r * 0.22F * (1.0F - done))), TABLET);
        }
        for (int i = 0; i < LUMPS; i++) {
            if (broken[i]) {
                continue;
            }
            Draw.disc(graphics, Math.round(lumpX[i]), Math.round(lumpY[i]), 3, GRIT);
        }
    }

    /** And the glass, clouding as it is stirred. */
    private void water(GuiGraphics graphics, int cx, int cy, int r, float cloud) {
        Draw.disc(graphics, cx, cy, r - 6, WATER);
        int lit = Math.round((r - 8) * cloud);
        if (lit > 0) {
            Draw.disc(graphics, cx, cy, lit, WATER_LIT);
        }
        // What has not gone in yet sits on the bottom, and the bottom is the middle.
        int grit = Math.round((r - 10) * (1.0F - cloud) * 0.5F);
        if (grit > 0) {
            Draw.disc(graphics, cx, cy, grit, GRIT);
        }
    }

    /** The back of a spoon, following the hand. */
    private void spoon(GuiGraphics graphics, int x, int y) {
        Draw.disc(graphics, x, y, down ? 6 : 5, SPOON);
        Draw.disc(graphics, x, y, 3, SPOON_DARK);
        for (int i = 0; i < 18; i += 2) {
            int sy = y - 6 - i;
            if (sy < top) {
                break;
            }
            graphics.fill(x - 1, sy, x + 2, sy + 2, i > 2 ? SPOON : SPOON_DARK);
        }
    }

    @Override
    public float progress() {
        return switch (phase) {
            case GRIND -> 0.5F * Mth.clamp(travel / GROUND, 0.0F, 1.0F);
            case STIR -> 0.5F + 0.5F * Mth.clamp(turns / STIRRED, 0.0F, 1.0F);
            default -> 1.0F;
        };
    }

    @Override
    public boolean complete() {
        return phase == DONE;
    }

    @Override
    public boolean failed() {
        // There is nothing here to ruin. A tablet that is not crushed is a tablet that is
        // not crushed yet, and the dish is still in front of you.
        return false;
    }

    @Override
    public Component instruction() {
        return Component.translatable(switch (phase) {
            case GRIND -> "triage.minigame.worm_hint_grind";
            case STIR -> "triage.minigame.worm_hint_stir";
            default -> "triage.minigame.worm_done";
        });
    }
}
