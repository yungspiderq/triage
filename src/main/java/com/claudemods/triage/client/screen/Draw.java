package com.claudemods.triage.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Circles and lines out of axis-aligned rectangles.
 *
 * <p>{@code GuiGraphics} can only fill boxes, and everything the treatment screen draws
 * is round. Approximating it in whole pixels also happens to be the right look: it
 * comes out as pixel art rather than as smooth vector shapes pasted into Minecraft.
 */
public final class Draw {
    private Draw() {
    }

    /** A filled disc, drawn as one horizontal bar per row. */
    public static void disc(GuiGraphics graphics, int cx, int cy, int radius, int colour) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.sqrt(Math.max(0, radius * radius - dy * dy));
            if (half <= 0) {
                continue;
            }
            graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 1, colour);
        }
    }

    /** A ring of square pips, evenly spaced around a circle. */
    public static void ring(GuiGraphics graphics, int cx, int cy, float radius, int pips, int size, int colour) {
        for (int i = 0; i < pips; i++) {
            double angle = i * Mth.TWO_PI / pips;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x - size, y - size, x + size, y + size, colour);
        }
    }

    /** One pip of that ring, at an arbitrary angle - used to show where the hand is. */
    public static void onRing(GuiGraphics graphics, int cx, int cy, float radius, float angle, int size, int colour) {
        int x = cx + (int) Math.round(Math.cos(angle) * radius);
        int y = cy + (int) Math.round(Math.sin(angle) * radius);
        graphics.fill(x - size, y - size, x + size, y + size, colour);
    }

    /**
     * A straight line of the given thickness, stepped along its longer axis.
     *
     * <p>The stroke is centred on the line. It used to hang off the bottom right of it,
     * which is invisible at a thickness of two and catastrophic at twenty.
     */
    public static void line(GuiGraphics graphics, int x1, int y1, int x2, int y2, int thickness, int colour) {
        int offset = thickness / 2;
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            graphics.fill(x1 - offset, y1 - offset, x1 - offset + thickness, y1 - offset + thickness, colour);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(Mth.lerp(t, x1, x2)) - offset;
            int y = Math.round(Mth.lerp(t, y1, y2)) - offset;
            graphics.fill(x, y, x + thickness, y + thickness, colour);
        }
    }

    /** Fades a colour towards transparent. */
    public static int alpha(int argb, float factor) {
        int a = (int) (((argb >>> 24) & 0xFF) * Mth.clamp(factor, 0.0F, 1.0F));
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    /** The same colour, lighter or darker. Above 1 lightens, below 1 darkens. */
    public static int shade(int argb, float factor) {
        int r = Mth.clamp(Math.round(((argb >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp(Math.round(((argb >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp(Math.round((argb & 0xFF) * factor), 0, 255);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** A vertical ramp, one fill per row. */
    public static void vGradient(GuiGraphics graphics, int x, int y, int width, int height, int top, int bottom) {
        for (int row = 0; row < height; row++) {
            float t = height <= 1 ? 0.0F : row / (float) (height - 1);
            int r = Math.round(Mth.lerp(t, (top >> 16) & 0xFF, (bottom >> 16) & 0xFF));
            int g = Math.round(Mth.lerp(t, (top >> 8) & 0xFF, (bottom >> 8) & 0xFF));
            int b = Math.round(Mth.lerp(t, top & 0xFF, bottom & 0xFF));
            graphics.fill(x, y + row, x + width, y + row + 1, 0xFF000000 | (r << 16) | (g << 8) | b);
        }
    }

    /**
     * A solid shape with light on it: lit along the top and left, shaded along the
     * bottom and right, corners knocked off.
     *
     * <p>Flat fills read as placeholder art. This is the least work that stops them,
     * and it costs nothing but a few more rectangles.
     */
    public static void body(GuiGraphics graphics, int x, int y, int width, int height, int base) {
        vGradient(graphics, x, y, width, height, shade(base, 1.10F), shade(base, 0.76F));

        graphics.fill(x, y, x + width, y + 1, shade(base, 1.30F));
        graphics.fill(x, y, x + 1, y + height, shade(base, 1.16F));
        graphics.fill(x, y + height - 1, x + width, y + height, shade(base, 0.58F));
        graphics.fill(x + width - 1, y, x + width, y + height, shade(base, 0.66F));

        int corner = shade(base, 0.5F);
        graphics.fill(x, y, x + 1, y + 1, corner);
        graphics.fill(x + width - 1, y, x + width, y + 1, corner);
        graphics.fill(x, y + height - 1, x + 1, y + height, corner);
        graphics.fill(x + width - 1, y + height - 1, x + width, y + height, corner);
    }

    /**
     * A raised panel: dark edge, then a light line down the top and left and a dark one
     * down the bottom and right. Two pixels of bevel is all it takes to stop a flat
     * rectangle reading as a debug overlay.
     */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height,
                             int fill, int edge, int light, int shadow) {
        graphics.fill(x, y, x + width, y + height, edge);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, light);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, light);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, shadow);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, shadow);
    }

    /** The same bevel inverted: a well for something to sit inside. */
    public static void well(GuiGraphics graphics, int x, int y, int width, int height,
                            int fill, int light, int shadow) {
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.fill(x, y, x + width, y + 1, shadow);
        graphics.fill(x, y, x + 1, y + height, shadow);
        graphics.fill(x, y + height - 1, x + width, y + height, light);
        graphics.fill(x + width - 1, y, x + width, y + height, light);
    }

    /** A progress bar in a well, with the fill inset so the frame stays visible. */
    public static void bar(GuiGraphics graphics, int x, int y, int width, int height,
                           float progress, int fill, int track, int light, int shadow) {
        well(graphics, x, y, width, height, track, light, shadow);
        int inner = Math.round((width - 2) * Mth.clamp(progress, 0.0F, 1.0F));
        if (inner > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + inner, y + height - 1, fill);
            graphics.fill(x + 1, y + 1, x + 1 + inner, y + 2, alpha(0xFFFFFFFF, 0.25F));
        }
    }
}
