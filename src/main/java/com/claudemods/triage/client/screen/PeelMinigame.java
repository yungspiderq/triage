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
 * Taking a dressing off with your fingers.
 *
 * <p>A dressing that has been on a while is not lying on a wound. It is stuck to one, and
 * what is holding it there is the thing that was trying to close underneath. Everybody
 * knows what happens when you take a plaster off quickly and everybody does it anyway,
 * which is exactly why this is worth playing rather than being a button.
 *
 * <p>So it peels. Drag the edge back along the limb and the cloth lifts behind your hand;
 * go slowly and it lifts clean, hurry and it takes what it is stuck to. The stuck places
 * are drawn, because they are visible in life too - that is where the cloth has gone dark.
 *
 * <p>Nothing here fails on the screen. How carefully it was done goes out as a number, and
 * whether the wound opens again is decided on the server against how long the dressing had
 * been on. A fresh one forgives almost anything. An old one forgives almost nothing.
 */
public final class PeelMinigame implements Minigame {
    /** How far the edge has to travel to be off. */
    private static final float LENGTH = 88.0F;
    /** Faster than this in a tick and it is a pull rather than a peel. */
    private static final float GENTLE = 1.6F;
    /** How close the hand has to be to the edge to have hold of it. */
    private static final float GRAB = 18.0F;
    /** Where the cloth has dried on, as shares of the way along. */
    private static final float[] STUCK = {0.24F, 0.52F, 0.79F};
    private static final float STUCK_WIDTH = 0.06F;

    private static final int COLOR_LIMB = 0xFFB07A5A;
    private static final int COLOR_LIMB_EDGE = 0xFF6E4632;
    private static final int COLOR_WOUND = 0xFF8E1616;
    private static final int COLOR_CLOTH = 0xFFF2ECE0;
    private static final int COLOR_CLOTH_EDGE = 0xFFC9C0AF;
    private static final int COLOR_STUCK = 0xFF9A7A5A;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;
    private static final int COLOR_MARK = 0xFFFFE070;
    private static final int COLOR_EMPTY = 0x40FFFFFF;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreY;
    private final int startX;

    /** How far the edge has been lifted, 0 to 1. */
    private float lifted;
    private boolean held;
    private float lastX;
    private boolean hasLast;
    private int snags;
    private int flash;

    public PeelMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        this.left = areaLeft;
        this.top = areaTop;
        this.width = areaWidth;
        this.height = areaHeight;
        this.centreY = areaTop + areaHeight / 2;
        this.startX = areaLeft + Math.round((areaWidth - LENGTH) / 2.0F);
    }

    @Override
    public void tick() {
        if (flash > 0) {
            flash--;
        }
    }

    // ---- the hand ---------------------------------------------------------

    private float edgeX() {
        return startX + LENGTH * lifted;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        if (Math.abs(mouseX - edgeX()) > GRAB || Math.abs(mouseY - centreY) > GRAB + 6) {
            return false;
        }
        held = true;
        lastX = (float) mouseX;
        hasLast = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || !held || complete()) {
            return false;
        }
        if (!hasLast) {
            lastX = (float) mouseX;
            hasLast = true;
            return true;
        }
        float step = (float) mouseX - lastX;
        lastX = (float) mouseX;
        if (step <= 0.0F) {
            return true;
        }
        // Over a stuck patch the cloth wants holding back; anywhere else it lifts.
        float limit = onStuck() ? GENTLE * 0.55F : GENTLE;
        if (step > limit) {
            snag();
            step = limit;
        }
        lifted = Mth.clamp(lifted + step / LENGTH, 0.0F, 1.0F);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            held = false;
            hasLast = false;
        }
    }

    private boolean onStuck() {
        for (float at : STUCK) {
            if (Math.abs(lifted - at) <= STUCK_WIDTH) {
                return true;
            }
        }
        return false;
    }

    private void snag() {
        // One per moment rather than one per frame, the same as everywhere else here.
        if (flash > 0) {
            return;
        }
        snags++;
        flash = 12;
        play(SoundEvents.WOOL_BREAK, 0.75F);
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        return lifted;
    }

    @Override
    public boolean complete() {
        return lifted >= 1.0F;
    }

    /** A dressing half off is worse than either end of it. There is no stopping. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - snags * 0.2F, 0.0F, 1.0F);
    }

    @Override
    public Component instruction() {
        if (flash > 0) {
            return Component.translatable("triage.minigame.peel_snag");
        }
        if (!held) {
            return Component.translatable("triage.minigame.peel_take");
        }
        return Component.translatable(onStuck()
                ? "triage.minigame.peel_stuck" : "triage.minigame.peel_slow");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int limbTop = centreY - 20;
        graphics.fill(startX - 13, limbTop - 1, startX + Math.round(LENGTH) + 13, centreY + 21,
                COLOR_LIMB_EDGE);
        Draw.body(graphics, startX - 12, limbTop, Math.round(LENGTH) + 24, 40, COLOR_LIMB);

        // What is under it, uncovered as the cloth comes back.
        int uncovered = Math.round(LENGTH * lifted);
        for (int i = 0; i < uncovered; i += 3) {
            int x = startX + i;
            int half = 5 - Math.abs(((i / 3) % 5) - 2);
            graphics.fill(x, centreY - half, x + 3, centreY + half, COLOR_WOUND);
        }

        // The cloth still down, and the stuck patches in it.
        int edge = Math.round(edgeX());
        int end = startX + Math.round(LENGTH);
        if (edge < end) {
            Draw.vGradient(graphics, edge, centreY - 12, end - edge, 24,
                    COLOR_CLOTH, Draw.shade(COLOR_CLOTH, 0.86F));
            graphics.fill(edge, centreY - 12, end, centreY - 11, Draw.shade(COLOR_CLOTH, 1.15F));
            graphics.fill(edge, centreY + 11, end, centreY + 12, COLOR_CLOTH_EDGE);
            for (float at : STUCK) {
                int from = startX + Math.round(LENGTH * (at - STUCK_WIDTH));
                int to = startX + Math.round(LENGTH * (at + STUCK_WIDTH));
                if (to <= edge) {
                    continue;
                }
                graphics.fill(Math.max(from, edge), centreY - 10, to, centreY + 10, COLOR_STUCK);
            }
        }

        // The lifted flap, folded back on itself.
        if (lifted > 0.0F) {
            int foldFrom = Math.max(startX - 10, edge - Math.round(LENGTH * lifted * 0.45F));
            Draw.vGradient(graphics, foldFrom, centreY - 17, edge - foldFrom, 10,
                    Draw.shade(COLOR_CLOTH, 0.9F), COLOR_CLOTH_EDGE);
        }

        // The edge itself: what the hand takes hold of.
        int colour = flash > 0 ? COLOR_BAD : held ? (onStuck() ? COLOR_MARK : COLOR_GOOD) : COLOR_MARK;
        graphics.fill(edge - 2, centreY - 14, edge + 2, centreY + 14, colour);
        if (!held && !complete()) {
            graphics.renderOutline(edge - 8, centreY - 18, 16, 36, Draw.alpha(COLOR_MARK, 0.6F));
        }

        int barLeft = left + 12;
        int barTop = top + height - 8;
        int barWidth = width - 24;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 4, COLOR_EMPTY);
        graphics.fill(barLeft, barTop, barLeft + Math.round(barWidth * lifted), barTop + 4,
                snags > 0 ? COLOR_MARK : COLOR_GOOD);
        for (int i = 0; i < 3; i++) {
            int x = left + width - 10 - i * 7;
            graphics.fill(x, top + 4, x + 4, top + 8, i < snags ? COLOR_BAD : COLOR_EMPTY);
        }
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
