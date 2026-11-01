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
 * Putting an arm back on, which is two jobs and only one of them is sewing.
 *
 * <p>Everything else in this mod repairs something that is still attached. This is the one
 * treatment that joins two pieces, and joining two pieces is a different kind of work: what
 * decides it is not how carefully you close the skin but whether the two ends are <b>lined
 * up</b> before you start. A limb sewn on crooked is sewn on crooked for good.
 *
 * <p>So it is in that order, and the order is the whole design. First the bone: the end in
 * your hands has to be turned until its marrow sits against the marrow in the stump, and
 * the closer that is the better everything after it goes. Then the rim, stitch by stitch,
 * all the way round - and a stitch is placed where the last one left off rather than
 * anywhere, because that is what going round something means.
 *
 * <p>Nothing here can be failed, and that is deliberate for once rather than habitual: the
 * thing that can go wrong with a graft already went wrong, out in the field, minutes ago,
 * while somebody decided whether to pick the arm up and whether to keep it cold. By the
 * time it reaches a screen the question is only how well.
 */
public final class GraftMinigame implements Minigame {
    private static final int ALIGN = 0;
    private static final int STITCH = 1;
    private static final int DONE = 2;

    /** How many stitches go round the rim. */
    private static final int STITCHES = 10;
    /** How close to the last one the next has to be, in degrees. */
    private static final float STEP = 360.0F / STITCHES;
    /** And how far off that spot a hand can be and still be putting one in. */
    private static final float SLOP = 26.0F;
    /** Turning this much off true is as bad as it gets. */
    private static final float CROOKED = 42.0F;

    private static final int SKIN = 0xFFC89571;
    private static final int SKIN_LIT = 0xFFDCAA85;
    private static final int SKIN_DARK = 0xFF9A6A4C;
    private static final int MUSCLE = 0xFF9B3A33;
    private static final int MUSCLE_DARK = 0xFF6E2420;
    private static final int BONE = 0xFFE8E2CF;
    private static final int MARROW = 0xFFB8524A;
    private static final int THREAD = 0xFF2A2A2E;
    private static final int GOOD = 0xFF7FD46B;
    private static final int POOR = 0xFFFFD37F;
    private static final int QUIET = 0xFFBFC6CC;

    private final int left;
    private final int top;
    private final int width;
    private final int height;

    /** How far the end in their hands is turned from where it has to be. */
    private float turn;
    private float lastTurn;
    /** And where it has to be, which is not zero, because a limb does not land face up. */
    private final float target;

    private int phase = ALIGN;
    private float alignment;

    private final boolean[] placed = new boolean[STITCHES];
    private int stitches;
    private int flash;
    private boolean down;

    public GraftMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = Math.max(80, width);
        this.height = Math.max(60, height);
        float roll = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.random.nextFloat() : 0.5F;
        this.target = -70.0F + roll * 140.0F;
        this.turn = 0.0F;
        this.lastTurn = 0.0F;
    }

    // ---- where things are ---------------------------------------------------

    private int centreX() {
        return left + width / 2;
    }

    private int centreY() {
        return top + height / 2 - 2;
    }

    private int radius() {
        return Math.min(width, height) / 4;
    }

    /** How far off true the end is, in degrees, worst case {@link #CROOKED}. */
    private float off() {
        return Math.abs(Mth.degreesDifference(turn, target));
    }

    /** Which stitch is next, and where round the rim it goes. */
    private float nextAngle() {
        return stitches * STEP - 90.0F;
    }

    // ---- the hand ----------------------------------------------------------

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0 || phase == DONE) {
            return false;
        }
        down = true;
        act(x, y);
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button) {
        if (button != 0 || phase == DONE) {
            return false;
        }
        act(x, y);
        return true;
    }

    @Override
    public void mouseReleased(double x, double y, int button) {
        down = false;
        if (phase == ALIGN && off() <= CROOKED) {
            // Letting go is saying that is as close as it is going to get. Nothing stops
            // anybody letting go early, and nothing stops them being wrong about it.
            alignment = 1.0F - Mth.clamp(off() / CROOKED, 0.0F, 1.0F);
            phase = STITCH;
            sound(SoundEvents.BONE_BLOCK_PLACE, 0.9F);
        }
    }

    private void act(double x, double y) {
        float dx = (float) x - centreX();
        float dy = (float) y - centreY();
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        if (phase == ALIGN) {
            // Dragging round the joint turns the piece, which is what turning a piece is.
            turn = angle;
            return;
        }
        if (stitches >= STITCHES) {
            return;
        }
        if (Math.abs(Mth.degreesDifference(angle, nextAngle())) > SLOP) {
            return;
        }
        placed[stitches] = true;
        stitches++;
        flash = 3;
        sound(SoundEvents.LEASH_KNOT_PLACE, 1.4F);
        if (stitches >= STITCHES) {
            phase = DONE;
            sound(SoundEvents.PLAYER_LEVELUP, 1.1F);
        }
    }

    private void sound(SoundEvent event, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(event, pitch, 0.4F));
    }

    @Override
    public void tick() {
        lastTurn = turn;
        if (flash > 0) {
            flash--;
        }
    }

    // ---- what it looks like -------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int cx = centreX();
        int cy = centreY();
        int r = radius();

        // The stump: skin round the outside, muscle inside it, bone in the middle. Drawn
        // as rows, which is the only way anything round gets drawn in here.
        ring(graphics, cx, cy, r, SKIN, SKIN_DARK);
        ring(graphics, cx, cy, Math.round(r * 0.74F), MUSCLE, MUSCLE_DARK);
        ring(graphics, cx, cy, Math.round(r * 0.3F), BONE, BONE);
        Draw.disc(graphics, cx, cy, Math.max(1, Math.round(r * 0.16F)), MARROW);

        // The end in their hands, off to one side, turned by however much they have
        // turned it. The mark on it is the thing being lined up.
        float shown = Mth.lerp(partialTick, lastTurn, turn);
        int px = cx + Math.round(Mth.cos((float) Math.toRadians(shown)) * (r + 12));
        int py = cy + Math.round(Mth.sin((float) Math.toRadians(shown)) * (r + 12));
        int tx = cx + Math.round(Mth.cos((float) Math.toRadians(target)) * (r + 12));
        int ty = cy + Math.round(Mth.sin((float) Math.toRadians(target)) * (r + 12));

        if (phase == ALIGN) {
            // Where it has to go, because lining something up against nothing is guessing.
            Draw.ring(graphics, tx, ty, 6.0F, 8, 1, QUIET);
            Draw.disc(graphics, px, py, 5, off() <= CROOKED * 0.35F ? GOOD : POOR);
            Draw.disc(graphics, px, py, 2, MARROW);
        }

        // The stitches, round the rim, where they have actually been put.
        for (int i = 0; i < STITCHES; i++) {
            float a = (float) Math.toRadians(i * STEP - 90.0F);
            int sx = cx + Math.round(Mth.cos(a) * r);
            int sy = cy + Math.round(Mth.sin(a) * r);
            if (placed[i]) {
                graphics.fill(sx - 3, sy - 1, sx + 3, sy + 1, THREAD);
                graphics.fill(sx - 1, sy - 3, sx + 1, sy + 3, THREAD);
            } else if (phase == STITCH && i == stitches) {
                // Only the next one is shown. Going round something means going round it.
                Draw.ring(graphics, sx, sy, 5.0F, 6, 1, flash > 0 ? GOOD : POOR);
            }
        }

        Component note;
        int colour;
        if (phase == DONE) {
            note = Component.translatable("triage.minigame.graft_done");
            colour = GOOD;
        } else if (phase == ALIGN) {
            boolean close = off() <= CROOKED * 0.35F;
            note = Component.translatable(close
                    ? "triage.minigame.graft_lined" : "triage.minigame.graft_turning");
            colour = close ? GOOD : POOR;
        } else {
            note = Component.translatable("triage.minigame.graft_stitch", stitches, STITCHES);
            colour = POOR;
        }
        graphics.drawString(font, note, left + 2, top + height - 9, colour, false);
    }

    /** One ring of flesh, as rows. */
    private static void ring(GuiGraphics graphics, int cx, int cy, int r, int fill, int edge) {
        for (int dy = -r; dy <= r; dy++) {
            int half = Math.round(Mth.sqrt(Math.max(0.0F, (float) (r * r - dy * dy))));
            if (half <= 0) {
                continue;
            }
            graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 1, fill);
            graphics.fill(cx - half, cy + dy, cx - half + 1, cy + dy + 1, edge);
            graphics.fill(cx + half - 1, cy + dy, cx + half, cy + dy + 1, edge);
        }
    }

    @Override
    public float progress() {
        return switch (phase) {
            case ALIGN -> 0.15F * (1.0F - Mth.clamp(off() / 180.0F, 0.0F, 1.0F));
            case STITCH -> 0.3F + 0.7F * (stitches / (float) STITCHES);
            default -> 1.0F;
        };
    }

    /**
     * What the two halves were worth.
     *
     * <p>Weighted towards the lining up, because that is the half that cannot be gone back
     * over. Every stitch gets put in eventually - there is no way to finish without them -
     * so the stitching is a floor rather than a score, and the bone is the grade.
     */
    @Override
    public float quality() {
        return Mth.clamp(0.35F + 0.65F * alignment, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return phase == DONE;
    }

    @Override
    public boolean failed() {
        // What could go wrong went wrong in a field, minutes ago, while somebody decided
        // whether to pick the arm up at all.
        return false;
    }

    @Override
    public Component instruction() {
        return Component.translatable(switch (phase) {
            case ALIGN -> "triage.minigame.graft_hint_align";
            case STITCH -> "triage.minigame.graft_hint_stitch";
            default -> "triage.minigame.graft_done";
        });
    }
}
