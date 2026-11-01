package com.claudemods.triage.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Testing a burn with a fingertip, where the answer is the place that does not answer.
 *
 * <p>Every other reading in this mod is something the body offers up: a pulse arrives, a
 * chest moves, a column of mercury climbs. This one is the opposite - <b>the finding is an
 * absence</b>. You press across a burn, the patient flinches where the burn is survivable,
 * and somewhere in the middle of it there is a patch where pressing does nothing at all.
 * That silence is the diagnosis, and it is the worst news on the screen.
 *
 * <p>It is the oldest test in burns and it works because of what a full thickness burn is:
 * the nerve endings that would have done the hurting are part of what burnt. Which makes a
 * burn the one injury that lies in the direction nobody expects. The patch somebody is
 * screaming about will close on its own. The quiet one beside it will not close at all,
 * and a jar of salve on it does nothing whatever.
 *
 * <p>So this game exists to stop a player treating the loud half and walking away. The
 * reward is not a repair - it is knowing which of the two answers this burn wants, and it
 * is worth exactly what the mapping was worth: probe the edges only and you come away with
 * "burnt", which is what you already knew.
 *
 * <p>Drawn in scanline rows rather than per pixel, for the reason written on
 * {@link Shapes}.
 */
public final class TouchMinigame implements Minigame {
    /** How many places have to be pressed before this is a map rather than a poke. */
    private static final int PROBES_WANTED = 7;
    /** How close two presses can be before the second one is the same press. */
    private static final float APART = 9.0F;
    /** Ticks a flinch, or the lack of one, stays on the screen. */
    private static final int SHOW = 9;

    private static final int SKIN = 0xFFC89571;
    private static final int SKIN_LIT = 0xFFDCAA85;
    private static final int SKIN_DARK = 0xFF9A6A4C;
    private static final int RED = 0xFFC4564A;
    private static final int BLISTER = 0xFFE8CDA8;
    private static final int BLISTER_EDGE = 0xFFB99A74;
    private static final int DEAD = 0xFFB7AFA2;
    private static final int DEAD_EDGE = 0xFF6E6A61;
    private static final int CHAR = 0xFF43403B;
    private static final int FINGER = 0xFFD9A886;
    private static final int FINGER_DARK = 0xFF9C6E51;
    private static final int NAIL = 0xFFEBC9B4;
    private static final int FELT = 0xFF7FD46B;
    private static final int NOTHING = 0xFFCC3333;

    private final int left;
    private final int top;
    private final int width;
    private final int height;

    /** The depth this burn really is, which the chart already knows and the player does not. */
    private final int depth;

    private final float[] probeX = new float[24];
    private final float[] probeY = new float[24];
    private final boolean[] probeFelt = new boolean[24];
    private int probes;

    /** Which probes landed in the quiet patch. One is the whole finding. */
    private int silent;
    private int show;
    private boolean lastFelt;
    private boolean down;
    private boolean finished;
    private float jerk;
    private float lastJerk;

    public TouchMinigame(int left, int top, int width, int height, int depth) {
        this.left = left;
        this.top = top;
        this.width = Math.max(80, width);
        this.height = Math.max(60, height);
        this.depth = depth;
    }

    // ---- where the burn is -------------------------------------------------

    private int centreX() {
        return left + width / 2;
    }

    private int centreY() {
        return top + height / 2 + 2;
    }

    /**
     * How wide the burn is at a given height, as a share of its widest.
     *
     * <p>Two sine terms rather than a circle, because nothing about the edge of a burn is
     * round - it is the shape of whatever was against the skin.
     */
    private float lobe(float t) {
        float edge = Mth.sin(t * Mth.PI);
        return edge * (0.82F + 0.18F * Mth.cos(t * 7.3F));
    }

    private float burnHalfWidth() {
        return width * 0.34F;
    }

    private float burnHalfHeight() {
        return height * 0.32F;
    }

    /**
     * Whether this point is in the part of the burn that has nothing left to feel with.
     *
     * <p>Only a full thickness burn has one, and it sits in the middle of it, because that
     * is where the heat was longest. A burn with no dead centre is a burn that is going to
     * hurt everywhere - which, once a player has seen one of each, is the good news.
     */
    private boolean dead(float x, float y) {
        if (depth < 3) {
            return false;
        }
        float dx = (x - centreX()) / (burnHalfWidth() * 0.52F);
        float dy = (y - centreY()) / (burnHalfHeight() * 0.52F);
        return dx * dx + dy * dy <= 1.0F;
    }

    private boolean onBurn(float x, float y) {
        float dy = (y - centreY()) / burnHalfHeight();
        if (dy < -1.0F || dy > 1.0F) {
            return false;
        }
        float half = burnHalfWidth() * lobe((dy + 1.0F) * 0.5F);
        return Math.abs(x - centreX()) <= half;
    }

    // ---- the hand ----------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || finished) {
            return false;
        }
        down = true;
        press((float) mouseX, (float) mouseY);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        down = false;
    }

    private void press(float x, float y) {
        if (!onBurn(x, y) || probes >= probeX.length) {
            return;
        }
        for (int i = 0; i < probes; i++) {
            float dx = probeX[i] - x;
            float dy = probeY[i] - y;
            if (dx * dx + dy * dy < APART * APART) {
                return;
            }
        }
        boolean felt = !dead(x, y);
        probeX[probes] = x;
        probeY[probes] = y;
        probeFelt[probes] = felt;
        probes++;
        if (!felt) {
            silent++;
        }
        lastFelt = felt;
        show = SHOW;
        if (felt) {
            // The whole of the feedback is this: one of them makes the arm move and the
            // other one does not.
            jerk = 4.5F;
            sound(SoundEvents.PLAYER_HURT, 1.4F, 0.35F);
        } else {
            sound(SoundEvents.WOOL_HIT, 0.8F, 0.4F);
        }
        if (probes >= PROBES_WANTED) {
            finished = true;
        }
    }

    private void sound(net.minecraft.sounds.SoundEvent event, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(event, pitch, volume));
    }

    @Override
    public void tick() {
        lastJerk = jerk;
        jerk *= 0.72F;
        if (show > 0) {
            show--;
        }
    }

    // ---- what it looks like ------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int shake = Math.round(Mth.lerp(partialTick, lastJerk, jerk));

        // The skin the burn is sitting on, in rows, with the light along the top.
        for (int y = top; y < top + height - 10; y++) {
            float t = (y - top) / (float) Math.max(1, height - 10);
            int shade = t < 0.22F ? SKIN_LIT : t > 0.76F ? SKIN_DARK : SKIN;
            graphics.fill(left, y, left + width, y + 1, shade);
        }

        burn(graphics, shake);
        marks(graphics, shake);
        finger(graphics, mouseX, mouseY);

        Component note;
        int colour;
        if (finished) {
            note = Component.translatable(silent > 0
                    ? "triage.minigame.touch_dead" : "triage.minigame.touch_live");
            colour = silent > 0 ? NOTHING : FELT;
        } else if (show > 0) {
            note = Component.translatable(lastFelt
                    ? "triage.minigame.touch_felt" : "triage.minigame.touch_silent");
            colour = lastFelt ? FELT : NOTHING;
        } else {
            note = Component.translatable("triage.minigame.touch_probe", probes, PROBES_WANTED);
            colour = 0xFFBFC6CC;
        }
        graphics.drawString(font, note, left + 2, top + height - 9, colour, false);
    }

    /** The burn itself: red at the rim, blistered inside it, and grey where it is through. */
    private void burn(GuiGraphics graphics, int shake) {
        int cx = centreX() + shake;
        int cy = centreY();
        float halfW = burnHalfWidth();
        float halfH = burnHalfHeight();
        for (int y = Math.round(cy - halfH); y <= cy + halfH; y++) {
            float dy = (y - cy) / halfH;
            float half = halfW * lobe((dy + 1.0F) * 0.5F);
            if (half <= 0.5F) {
                continue;
            }
            int x0 = Math.round(cx - half);
            int x1 = Math.round(cx + half);
            graphics.fill(x0, y, x1, y + 1, RED);
            // Inside the rim, whatever this burn is made of.
            float inner = half * 0.72F;
            if (inner > 1.0F) {
                int i0 = Math.round(cx - inner);
                int i1 = Math.round(cx + inner);
                graphics.fill(i0, y, i1, y + 1, depth >= 2 ? BLISTER : RED);
                if (depth >= 2) {
                    graphics.fill(i0, y, i0 + 1, y + 1, BLISTER_EDGE);
                    graphics.fill(i1 - 1, y, i1, y + 1, BLISTER_EDGE);
                }
            }
        }
        if (depth < 3) {
            return;
        }
        // And the middle of it, which is the thing being looked for. Drawn plainly: it is
        // paler and drier than everything round it, and that is how it looks.
        float deadW = halfW * 0.52F;
        float deadH = halfH * 0.52F;
        for (int y = Math.round(cy - deadH); y <= cy + deadH; y++) {
            float dy = (y - cy) / deadH;
            float half = deadW * Mth.sqrt(Math.max(0.0F, 1.0F - dy * dy));
            if (half <= 0.5F) {
                continue;
            }
            int x0 = Math.round(cx - half);
            int x1 = Math.round(cx + half);
            graphics.fill(x0, y, x1, y + 1, DEAD);
            graphics.fill(x0, y, x0 + 1, y + 1, DEAD_EDGE);
            graphics.fill(x1 - 1, y, x1, y + 1, DEAD_EDGE);
            if (Math.abs(dy) < 0.45F) {
                graphics.fill(Math.round(cx - half * 0.4F), y,
                        Math.round(cx + half * 0.4F), y + 1, CHAR);
            }
        }
    }

    /** Where the fingers have already been, and what each place said. */
    private void marks(GuiGraphics graphics, int shake) {
        for (int i = 0; i < probes; i++) {
            int x = Math.round(probeX[i]) + shake;
            int y = Math.round(probeY[i]);
            int colour = probeFelt[i] ? FELT : NOTHING;
            graphics.fill(x - 2, y, x + 3, y + 1, colour);
            graphics.fill(x, y - 2, x + 1, y + 3, colour);
        }
    }

    /** A fingertip, following the mouse, pressed flat while the button is down. */
    private void finger(GuiGraphics graphics, int mouseX, int mouseY) {
        int half = down ? 7 : 5;
        for (int i = 0; i < 34; i += 2) {
            float t = i / 33.0F;
            int h = Math.round(half - 1.6F * (1.0F - t));
            int y = mouseY - i;
            if (y < top) {
                break;
            }
            graphics.fill(mouseX - h, y, mouseX + h, y + 2, i > 3 ? FINGER : FINGER_DARK);
            graphics.fill(mouseX - h, y, mouseX - h + 1, y + 2, FINGER_DARK);
            graphics.fill(mouseX + h - 1, y, mouseX + h, y + 2, FINGER_DARK);
        }
        graphics.fill(mouseX - 3, mouseY - 13, mouseX + 4, mouseY - 6, NAIL);
        if (down) {
            graphics.fill(mouseX - half - 1, mouseY, mouseX + half + 1, mouseY + 2, FINGER_DARK);
        }
    }

    @Override
    public float progress() {
        return Mth.clamp(probes / (float) PROBES_WANTED, 0.0F, 1.0F);
    }

    /**
     * What the mapping was worth.
     *
     * <p>Full marks for having pressed enough places, and nothing else asked. There is no
     * skill here beyond being thorough, which is exactly the skill the test is: a burn
     * probed in one place is a burn nobody has tested.
     */
    @Override
    public float quality() {
        return Mth.clamp(probes / (float) PROBES_WANTED, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return finished;
    }

    @Override
    public boolean failed() {
        // Nothing here can go wrong. Pressing on a burn hurts and that is the reading.
        return false;
    }

    @Override
    public Component instruction() {
        if (finished) {
            return Component.translatable("triage.minigame.touch_done");
        }
        return Component.translatable("triage.minigame.touch_hint");
    }
}
