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
 * Cutting a dressing off, which is what the scissors are for.
 *
 * <p>The point of a pair of bandage scissors is the blunt lower blade: it goes flat
 * against the skin and slides along under the cloth, so the cutting happens above it and
 * nothing that matters is underneath. Cut this way and it does not matter how long the
 * dressing has been on or what it is stuck to - the cloth comes off in one piece and the
 * wound never knows.
 *
 * <p>Which is why this game cannot tear anything, and why it is short. It is not meant to
 * be hard. It is meant to be the thing you would rather be doing than pulling.
 *
 * <p>What it can do is nick the skin, and only if the blade is lifted off it - because a
 * blunt tip stops being blunt the moment it is not lying flat. So the work is keeping the
 * scissors down against the limb while they travel, which is one axis, visible, and the
 * only rule here.
 */
public final class CutMinigame implements Minigame {
    /** How far the cut has to travel. */
    private static final float LENGTH = 92.0F;
    /** How far off the skin the lower blade may drift before it is a point again. */
    private static final float FLAT = 6.0F;

    private static final int COLOR_LIMB = 0xFFB07A5A;
    private static final int COLOR_LIMB_EDGE = 0xFF6E4632;
    private static final int COLOR_WOUND = 0xFF8E1616;
    private static final int COLOR_CLOTH = 0xFFF2ECE0;
    private static final int COLOR_CLOTH_EDGE = 0xFFC9C0AF;
    private static final int COLOR_CUT = 0xFF9A9488;
    private static final int COLOR_STEEL = 0xFFB8BCC4;
    private static final int COLOR_STEEL_DARK = 0xFF80868F;
    private static final int COLOR_GRIP = 0xFF2C5878;
    private static final int COLOR_GOOD = 0xFF7FD46B;
    private static final int COLOR_BAD = 0xFFCC3333;
    private static final int COLOR_MARK = 0xFFFFE070;
    private static final int COLOR_EMPTY = 0x40FFFFFF;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int skinY;
    private final int startX;

    /** How far along the cut has gone. */
    private float along;
    private boolean held;
    private float bladeY;
    private float lastX;
    private boolean hasLast;
    private int nicks;
    private int flash;
    private int age;

    public CutMinigame(int areaLeft, int areaTop, int areaWidth, int areaHeight) {
        this.left = areaLeft;
        this.top = areaTop;
        this.width = areaWidth;
        this.height = areaHeight;
        this.skinY = areaTop + areaHeight / 2 + 6;
        this.startX = areaLeft + Math.round((areaWidth - LENGTH) / 2.0F);
        this.bladeY = skinY;
    }

    @Override
    public void tick() {
        age++;
        if (flash > 0) {
            flash--;
        }
    }

    // ---- the hand ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || complete()) {
            return false;
        }
        held = true;
        bladeY = (float) mouseY;
        lastX = (float) mouseX;
        hasLast = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || !held || complete()) {
            return false;
        }
        bladeY = (float) mouseY;
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
        if (Math.abs(bladeY - skinY) > FLAT) {
            // Lifted off the skin. A blunt tip is only blunt while it is lying flat.
            nick();
            return true;
        }
        along = Mth.clamp(along + step / LENGTH, 0.0F, 1.0F);
        if (((int) (along * 12.0F)) != ((int) ((along - step / LENGTH) * 12.0F))) {
            play(SoundEvents.SHEEP_SHEAR, 1.5F);
        }
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            held = false;
            hasLast = false;
        }
    }

    private void nick() {
        if (flash > 0) {
            return;
        }
        nicks++;
        flash = 14;
        play(SoundEvents.PLAYER_HURT, 0.9F);
    }

    // ---- where it stands --------------------------------------------------

    @Override
    public float progress() {
        return along;
    }

    @Override
    public boolean complete() {
        return along >= 1.0F;
    }

    /** Scissors do not fail. That is the entire argument for owning a pair. */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public float quality() {
        return Mth.clamp(1.0F - nicks * 0.15F, 0.0F, 1.0F);
    }

    @Override
    public Component instruction() {
        if (flash > 0) {
            return Component.translatable("triage.minigame.cut_nick");
        }
        if (!held) {
            return Component.translatable("triage.minigame.cut_take");
        }
        return Component.translatable("triage.minigame.cut_along");
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int end = startX + Math.round(LENGTH);
        graphics.fill(startX - 13, skinY - 27, end + 13, skinY + 13, COLOR_LIMB_EDGE);
        Draw.body(graphics, startX - 12, skinY - 26, Math.round(LENGTH) + 24, 38, COLOR_LIMB);

        // The wound under it, showing through where the cloth has been opened.
        int cut = startX + Math.round(LENGTH * along);
        for (int i = 0; i < Math.round(LENGTH * along); i += 3) {
            int x = startX + i;
            int half = 4 - Math.abs(((i / 3) % 5) - 2);
            graphics.fill(x, skinY - 12 - half, x + 3, skinY - 12 + half, COLOR_WOUND);
        }

        // The cloth, in one piece ahead of the blade and open behind it.
        Draw.vGradient(graphics, startX, skinY - 22, Math.round(LENGTH), 20,
                COLOR_CLOTH, Draw.shade(COLOR_CLOTH, 0.86F));
        graphics.fill(startX, skinY - 22, end, skinY - 21, Draw.shade(COLOR_CLOTH, 1.15F));
        graphics.fill(startX, skinY - 3, end, skinY - 2, COLOR_CLOTH_EDGE);
        if (cut > startX) {
            // The kerf: a line of cut cloth opening as the scissors go.
            graphics.fill(startX, skinY - 14, cut, skinY - 10, COLOR_CUT);
            graphics.fill(startX, skinY - 22, cut, skinY - 14, Draw.shade(COLOR_CLOTH, 0.94F));
            graphics.fill(startX, skinY - 10, cut, skinY - 2, Draw.shade(COLOR_CLOTH, 0.94F));
        }

        // The line the lower blade has to stay on.
        Draw.line(graphics, startX - 6, skinY, end + 6, skinY, 1, Draw.alpha(COLOR_MARK, 0.35F));

        // The scissors: a blunt lower blade flat on the skin and the cutting one above.
        int x = held ? cut : startX;
        int y = held ? Math.round(Mth.clamp(bladeY, top + 8, top + height - 8)) : skinY;
        boolean flat = Math.abs(y - skinY) <= FLAT;
        int steel = flash > 0 ? COLOR_BAD : flat ? COLOR_STEEL : COLOR_BAD;
        // Lower blade, blunt tip.
        graphics.fill(x - 22, y - 2, x + 6, y + 1, steel);
        Draw.disc(graphics, x + 6, y - 1, 2, COLOR_STEEL_DARK);
        // Upper blade, opening and closing as it travels.
        int gap = 3 + (int) (2.5F * (1.0F + Mth.sin(age * 0.5F)));
        graphics.fill(x - 22, y - 2 - gap, x + 4, y - gap, steel);
        // Handles.
        graphics.fill(x - 30, y - 5, x - 22, y + 3, COLOR_GRIP);
        graphics.fill(x - 30, y - 6 - gap, x - 22, y - gap + 1, COLOR_GRIP);

        int barLeft = left + 12;
        int barTop = top + height - 8;
        int barWidth = width - 24;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 4, COLOR_EMPTY);
        graphics.fill(barLeft, barTop, barLeft + Math.round(barWidth * along), barTop + 4,
                nicks > 0 ? COLOR_MARK : COLOR_GOOD);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, Mth.clamp(pitch, 0.5F, 2.0F)));
    }
}
