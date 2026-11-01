package com.claudemods.triage.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Filling shapes that are not axis-aligned, without paying a quad per pixel.
 *
 * <p>Both of the games that draw an instrument at an angle were first written by walking
 * the shape and setting one pixel at a time, because that is how the pictures were worked
 * out. In a still image that costs nothing. In a render loop it is a separate draw call
 * per pixel, and an arm alone is fifteen thousand of them every frame - which is how a
 * minigame ends up running at thirteen.
 *
 * <p>So anything with a slope is filled a row at a time instead: work out where the shape
 * starts and stops on each scanline and lay down one rectangle for it. The picture is
 * identical and the cost drops by two orders of magnitude.
 */
final class Shapes {
    private Shapes() {
    }

    /**
     * A rectangle lying along an axis, at any angle.
     *
     * @param x     where the near end sits
     * @param half  half its width, across the axis
     */
    static void bar(GuiGraphics graphics, float x, float y, float ax, float ay,
                    float length, float half, int colour) {
        float px = -ay;
        float py = ax;
        float ex = x + ax * length;
        float ey = y + ay * length;
        polygon(graphics, new float[]{
                x + px * half, x - px * half, ex - px * half, ex + px * half,
        }, new float[]{
                y + py * half, y - py * half, ey - py * half, ey + py * half,
        }, colour);
    }

    /**
     * Any convex shape, a row at a time.
     *
     * <p>One rectangle per scanline, which for everything drawn here is a few dozen
     * rather than a few thousand.
     */
    static void polygon(GuiGraphics graphics, float[] xs, float[] ys, int colour) {
        int n = xs.length;
        float lowest = ys[0];
        float highest = ys[0];
        for (int i = 1; i < n; i++) {
            lowest = Math.min(lowest, ys[i]);
            highest = Math.max(highest, ys[i]);
        }
        int top = Math.round(lowest);
        int bottom = Math.round(highest);
        for (int y = top; y <= bottom; y++) {
            float mid = y + 0.5F;
            float from = Float.MAX_VALUE;
            float to = -Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                float x0 = xs[i];
                float y0 = ys[i];
                float x1 = xs[(i + 1) % n];
                float y1 = ys[(i + 1) % n];
                if ((y0 <= mid && y1 > mid) || (y1 <= mid && y0 > mid)) {
                    float at = x0 + (x1 - x0) * (mid - y0) / (y1 - y0);
                    from = Math.min(from, at);
                    to = Math.max(to, at);
                }
            }
            if (from > to) {
                continue;
            }
            int x0 = Math.round(from);
            int x1 = Math.max(x0 + 1, Math.round(to));
            graphics.fill(x0, y, x1, y + 1, colour);
        }
    }
}
