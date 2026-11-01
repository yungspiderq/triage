package com.claudemods.triage.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Washing out a wound that has had the floor in it.
 *
 * <p>The operation for a bone that came through the skin, and it is not a repair at all -
 * the bone is still broken when this is finished and still out of line. What this is, is
 * the end of a race: an open fracture is the inside of a leg introduced to whatever it
 * landed on, and every second of that is the fastest road to infection in the mod.
 *
 * <p>So the job is the thing surgeons actually say about it - <b>the solution to pollution
 * is dilution</b>. Not picking the grit out one piece at a time with a pair of tweezers:
 * washing it out, in quantity, until what comes back is clean.
 *
 * <p>Which makes the pressure of the stream the whole of the skill and the bottle the whole
 * of the limit. Grit moves away from where the water lands, so it is driven to the edge
 * and over it; what is deep in the wound needs more of the bottle than what is lying on
 * top; and there is exactly one bottle. Waving it about empties it having moved nothing.
 */
public final class IrrigateMinigame implements Minigame {
    /** How many pieces of the world came in with the bone. */
    private static final int GRIT = 7;
    /** Ticks of pouring in the bottle. */
    private static final float WATER = 150.0F;
    /** How far from the stream a piece still feels it. */
    private static final float SPREAD = 22.0F;
    /** How hard the stream pushes, in pixels a tick at point blank. */
    private static final float PUSH = 1.5F;

    private static final int SKIN = 0xFFC89571;
    private static final int SKIN_EDGE = 0xFF8E5F41;
    private static final int MEAT = 0xFF8A322C;
    private static final int MEAT_DEEP = 0xFF5E1F1B;
    private static final int BONE = 0xFFEFE7D2;
    private static final int BONE_EDGE = 0xFFB6AC92;
    private static final int DIRT = 0xFF4A3A28;
    private static final int DIRT_LIT = 0xFF6B5539;
    private static final int WATER_COL = 0x99BFE4F2;
    private static final int WATER_LIT = 0xFFDDF2FA;
    private static final int GOOD = 0xFF7FD46B;
    private static final int BAD = 0xFFCC3333;
    private static final int EMPTY = 0x40FFFFFF;

    private static final class Speck {
        float x;
        float y;
        final int size;
        boolean out;

        Speck(float x, float y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int centreX;
    private final int centreY;
    private final float radius;
    private final List<Speck> specks = new ArrayList<>();

    private float water = WATER;
    private boolean pouring;
    private float handX;
    private float handY;
    private int cleared;
    private int age;

    public IrrigateMinigame(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = Math.max(80, width);
        this.height = Math.max(60, height);
        this.centreX = left + this.width / 2;
        this.centreY = top + this.height / 2;
        this.radius = Math.min(this.width * 0.36F, this.height * 0.40F);

        RandomSource random = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getRandom() : RandomSource.create();
        for (int i = 0; i < GRIT; i++) {
            // Scattered through the wound rather than round its edge: what has to be
            // washed out is what went in, and what went in went in deep.
            float angle = random.nextFloat() * Mth.TWO_PI;
            float away = radius * (0.15F + random.nextFloat() * 0.62F);
            specks.add(new Speck(centreX + Mth.cos(angle) * away,
                    centreY + Mth.sin(angle) * away * 0.8F, 2 + random.nextInt(3)));
        }
        this.handX = centreX;
        this.handY = centreY - radius;
    }

    // ---- the bottle ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        handX = (float) mouseX;
        handY = (float) mouseY;
        pouring = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!pouring) {
            return false;
        }
        handX = (float) mouseX;
        handY = (float) mouseY;
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        pouring = false;
    }

    @Override
    public void tick() {
        age++;
        if (complete() || !pouring || water <= 0.0F) {
            return;
        }
        water -= 1.0F;
        if (age % 7 == 0) {
            play(1.0F);
        }
        for (Speck speck : specks) {
            if (speck.out) {
                continue;
            }
            float dx = speck.x - handX;
            float dy = speck.y - handY;
            float away = Mth.sqrt(dx * dx + dy * dy);
            if (away > SPREAD) {
                continue;
            }
            // Straight out from where the water is landing, and harder the closer it is.
            float force = PUSH * (1.0F - away / SPREAD);
            if (away < 0.5F) {
                dx = 0.0F;
                dy = 1.0F;
                away = 1.0F;
            }
            speck.x += dx / away * force;
            speck.y += dy / away * force;
            if (offEdge(speck)) {
                speck.out = true;
                cleared++;
                play(1.6F);
            }
        }
    }

    /** Whether a piece has been driven past the edge of the wound. */
    private boolean offEdge(Speck speck) {
        float dx = (speck.x - centreX) / radius;
        float dy = (speck.y - centreY) / (radius * 0.8F);
        return dx * dx + dy * dy >= 1.0F;
    }

    // ---- where it stands ----------------------------------------------------

    @Override
    public float progress() {
        return cleared / (float) GRIT;
    }

    @Override
    public float quality() {
        return Mth.clamp(cleared / (float) GRIT, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return cleared >= GRIT || water <= 0.0F;
    }

    /** Nothing here fails. A wound washed badly is a wound washed badly. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        if (complete()) {
            return Component.translatable(cleared >= GRIT
                    ? "triage.minigame.wash_done" : "triage.minigame.wash_dry");
        }
        return Component.translatable(pouring
                ? "triage.minigame.wash_pouring" : "triage.minigame.wash_hint");
    }

    // ---- drawing ------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        if (!pouring && !complete()) {
            handX = mouseX;
            handY = mouseY;
        }
        wound(graphics);
        for (Speck speck : specks) {
            if (!speck.out) {
                grit(graphics, speck);
            }
        }
        if (pouring && water > 0.0F) {
            stream(graphics);
        }
        gauge(graphics);
    }

    private void wound(GuiGraphics graphics) {
        graphics.fill(left, top, left + width, top + height, SKIN);
        graphics.fill(left, top + height - 3, left + width, top + height, SKIN_EDGE);
        // The wound itself: an ellipse of opened meat with the end of the bone in it,
        // which is the whole reason this operation exists.
        for (int y = -Math.round(radius * 0.8F); y <= radius * 0.8F; y++) {
            float t = y / (radius * 0.8F);
            int half = Math.round(radius * Mth.sqrt(Math.max(0.0F, 1.0F - t * t)));
            int yy = centreY + y;
            graphics.fill(centreX - half, yy, centreX + half, yy + 1, MEAT);
            if (Math.abs(t) < 0.55F) {
                int inner = Math.round(half * 0.6F);
                graphics.fill(centreX - inner, yy, centreX + inner, yy + 1, MEAT_DEEP);
            }
        }
        graphics.fill(centreX - 5, centreY - 14, centreX + 5, centreY + 14, BONE_EDGE);
        graphics.fill(centreX - 4, centreY - 13, centreX + 4, centreY + 13, BONE);
    }

    private void grit(GuiGraphics graphics, Speck speck) {
        int x = Math.round(speck.x);
        int y = Math.round(speck.y);
        graphics.fill(x - speck.size, y - speck.size, x + speck.size, y + speck.size, DIRT);
        graphics.fill(x - speck.size, y - speck.size, x, y, DIRT_LIT);
    }

    /** The stream, and the splash where it lands. */
    private void stream(GuiGraphics graphics) {
        int x = Math.round(handX);
        int y = Math.round(handY);
        for (int i = 0; i < 16; i++) {
            int sy = top - 6 + i * ((y - top + 6) / 16 + 1);
            if (sy >= y) {
                break;
            }
            graphics.fill(x - 1, sy, x + 2, sy + 3, WATER_COL);
        }
        for (int i = 0; i < 9; i++) {
            float angle = (age * 0.5F + i * 0.7F) % Mth.TWO_PI;
            float away = 4.0F + (i % 4) * 4.0F;
            Draw.disc(graphics, x + Math.round(Mth.cos(angle) * away),
                    y + Math.round(Mth.sin(angle) * away * 0.6F), 2, WATER_LIT);
        }
        Draw.ring(graphics, x, y, SPREAD, 22, 1, 0x33BFE4F2);
    }

    /** What is left in the bottle, and how much of the wound is clean. */
    private void gauge(GuiGraphics graphics) {
        int barLeft = left + 8;
        int barTop = top + height - 8;
        int barWidth = width - 16;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 4, EMPTY);
        graphics.fill(barLeft, barTop, barLeft + Math.round(barWidth * (water / WATER)), barTop + 4,
                water > WATER * 0.25F ? WATER_LIT : BAD);
        for (int i = 0; i < GRIT; i++) {
            int x = barLeft + i * 8;
            graphics.fill(x, top + 4, x + 6, top + 9, i < cleared ? GOOD : DIRT);
        }
    }

    private static void play(float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.BUCKET_EMPTY, Mth.clamp(pitch, 0.5F, 2.0F), 0.4F));
    }
}
