package com.claudemods.triage.client;

import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.PartState;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The figure of a body, built out of rectangles.
 *
 * <p>Each part is several boxes rather than one, which is the whole difference between
 * a diagram and a person: a neck and a pair of shoulders join the head to the chest, and
 * the limbs bend at the elbow and the knee. It still costs nothing but fills, so it
 * scales to any GUI size and needs no texture.
 *
 * <p>Shared by the corner HUD and the treatment screen, so the shape a player learns to
 * read in one place is the same shape in the other - only the scale differs.
 */
public final class BodyFigure {
    public static final int WIDTH = 32;
    public static final int HEIGHT = 52;

    /**
     * The boxes each part is made of, in the order of {@link BodyPart#VALUES}: skull and
     * crown, neck and shoulders and chest, belly, upper arms and forearms, thighs and
     * shins.
     */
    private static final int[][][] LAYOUT = {
            {{12, 0, 8, 2}, {11, 2, 10, 7}},                 // head
            {{14, 9, 4, 2}, {6, 11, 20, 5}, {8, 16, 16, 9}}, // thorax
            {{9, 25, 14, 7}},                                // stomach
            {{2, 12, 5, 10}, {1, 22, 5, 10}},                // left arm
            {{25, 12, 5, 10}, {26, 22, 5, 10}},              // right arm
            {{9, 33, 6, 9}, {10, 42, 5, 10}},                // left leg
            {{17, 33, 6, 9}, {17, 42, 5, 10}},               // right leg
    };

    // Printed rather than lit: the same three steps, mixed for a cream sheet.
    private static final int COLOR_WHOLE = 0xFF5F9E52;
    private static final int COLOR_HALF = 0xFFC08F28;
    private static final int COLOR_GONE = 0xFFA8402A;
    private static final int COLOR_BLACKED = 0xFF3B0F0F;
    // Not red. A limb that has died is not an injury getting worse, it is a different
    // thing on the chart, and it should not read as "very badly hurt".
    private static final int COLOR_DEAD = 0xFF3A1C42;
    private static final int COLOR_WOOD = 0xFF6B4A28;
    private static final int COLOR_SHADE = 0x40000000;

    private static final int COLOR_BLEED = 0xFFFF2020;
    /** Brighter, and it alternates with the other rather than with nothing. */
    private static final int COLOR_ARTERY = 0xFFFF7A5A;
    private static final int COLOR_FRACTURE = 0xFFFFE070;
    private static final int COLOR_CRACK = 0xFFB9A052;
    private static final int COLOR_TOURNIQUET = 0xFF7FE7FF;
    private static final int COLOR_INFECTION = 0xFF6FCF4A;
    private static final int COLOR_BURN = 0xFFFF8A33;
    /** A burn that is only red, and one that has gone through. Three depths, three marks. */
    private static final int COLOR_BURN_LIGHT = 0xFFFFC266;
    private static final int COLOR_BURN_DEEP = 0xFF8C4A3C;
    private static final int COLOR_HIGHLIGHT = 0xFF7FE7FF;
    private static final int COLOR_DIM = 0x66000000;

    private BodyFigure() {
    }

    /**
     * @param highlight parts worth pointing at, or null to draw them all the same. The
     *                  ones it rejects are dimmed rather than hidden - a player picking
     *                  a limb to treat still needs to see the state of the others.
     */
    public static void draw(GuiGraphics graphics, BodyHealth body, int originX, int originY, int scale,
                            int alpha, int tick, @Nullable Predicate<BodyPart> highlight) {
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            int fill = withAlpha(fillColour(state), alpha);
            int outline = outlineFor(state, tick);
            boolean wanted = highlight == null || highlight.test(part);

            for (int[] box : LAYOUT[part.ordinal()]) {
                int x = originX + box[0] * scale;
                int y = originY + box[1] * scale;
                int w = box[2] * scale;
                int h = box[3] * scale;

                // A part that is not there is drawn as not there: an outline round the
                // space it used to be. Filling it in any colour at all would put a limb
                // on the figure, and the whole thing this state has to say is that
                // there is not one.
                if (state.gone() && !state.prosthetic) {
                    graphics.renderOutline(x, y, w, h, withAlpha(COLOR_SHADE, alpha * 2));
                    continue;
                }
                graphics.fill(x, y, x + w, y + h, fill);
                // A darker band along the bottom of every box gives the flat colour
                // just enough form to read as a limb rather than as a swatch.
                graphics.fill(x, y + h - scale, x + w, y + h, withAlpha(COLOR_SHADE, alpha));

                if (outline != 0) {
                    graphics.renderOutline(x, y, w, h, outline);
                }
                if (highlight != null && !wanted) {
                    graphics.fill(x, y, x + w, y + h, COLOR_DIM);
                }
            }

            if (highlight != null && wanted) {
                outlineWhole(graphics, part, originX, originY, scale,
                        withAlpha(COLOR_HIGHLIGHT, 140 + (int) (100 * Mth.sin(tick * 0.2F))));
            }

            // A second thing wrong with the same part gets a corner pip, so an arm that
            // is bleeding and broken does not look like a merely bleeding one.
            int second = secondaryFor(state);
            if (second != 0) {
                int[] box = LAYOUT[part.ordinal()][0];
                int x = originX + (box[0] + box[2]) * scale - 2 * scale;
                int y = originY + box[1] * scale;
                graphics.fill(x, y, x + 2 * scale, y + 2 * scale, second);
            }
        }
    }

    /** A frame around the whole part, drawn one box out, for the selection glow. */
    private static void outlineWhole(GuiGraphics graphics, BodyPart part, int originX, int originY,
                                     int scale, int colour) {
        for (int[] box : LAYOUT[part.ordinal()]) {
            graphics.renderOutline(originX + box[0] * scale - 1, originY + box[1] * scale - 1,
                    box[2] * scale + 2, box[3] * scale + 2, colour);
        }
    }

    /** The part under the cursor, or null. */
    public static @Nullable BodyPart at(int originX, int originY, int scale, double mouseX, double mouseY) {
        for (BodyPart part : BodyPart.VALUES) {
            for (int[] box : LAYOUT[part.ordinal()]) {
                int x = originX + box[0] * scale;
                int y = originY + box[1] * scale;
                if (mouseX >= x && mouseX < x + box[2] * scale && mouseY >= y && mouseY < y + box[3] * scale) {
                    return part;
                }
            }
        }
        return null;
    }

    /**
     * Green while whole, amber at half, red at the end of it, near-black once the part
     * is blacked out. Two straight ramps rather than one, so that half health looks
     * like a warning instead of like a muddy in-between.
     *
     * <p>The ratio comes through {@link ClientBody#shownRatio} rather than straight off
     * the part, so a concussed player gets the same three coarse bands here as they get
     * on the bars. Leaving the colours exact would have handed the whole readout back.
     */
    public static int fillColour(PartState state) {
        if (state.prosthetic) {
            return COLOR_WOOD;
        }
        if (state.necrotic()) {
            return COLOR_DEAD;
        }
        if (state.blacked()) {
            return COLOR_BLACKED;
        }
        float ratio = ClientBody.shownRatio(state);
        return ratio >= 0.5F
                ? lerp(COLOR_HALF, COLOR_WHOLE, (ratio - 0.5F) * 2.0F)
                : lerp(COLOR_GONE, COLOR_HALF, ratio * 2.0F);
    }

    /**
     * The one thing most worth knowing about this part, as an outline colour. Bleeding
     * flashes because it is running out now; the rest are steady, because they are
     * standing problems.
     */
    private static int outlineFor(PartState state, int tick) {
        // A strap outranks what is under it: it is the thing on a clock.
        if (state.tourniquet) {
            return COLOR_TOURNIQUET;
        }
        // Faster than the heavy flash and a brighter red, because the difference
        // between these two is the difference between minutes and seconds.
        if (state.arterial()) {
            return (tick / 2) % 2 == 0 ? COLOR_ARTERY : COLOR_BLEED;
        }
        if (state.bleed == PartState.BLEED_HEAVY) {
            return (tick / 5) % 2 == 0 ? COLOR_BLEED : 0;
        }
        if (state.bleed == PartState.BLEED_LIGHT) {
            return withAlpha(COLOR_BLEED, 170);
        }
        if (state.infected()) {
            return withAlpha(COLOR_INFECTION, state.infection >= PartState.INFECTION_SEVERE ? 255 : 180);
        }
        if (state.burned) {
            return burnColour(state);
        }
        if (state.fractured()) {
            return COLOR_FRACTURE;
        }
        if (state.cracked) {
            return COLOR_CRACK;
        }
        return 0;
    }

    /**
     * Which of the three a burn is, on the figure.
     *
     * <p>Worth three colours rather than one because they are three different problems,
     * and because the deep one reads as the deep one at a glance: it is the darkest thing
     * on the chart, which is what a burn through the skin looks like.
     */
    private static int burnColour(PartState state) {
        return switch (state.burn) {
            case PartState.BURN_SUPERFICIAL -> COLOR_BURN_LIGHT;
            case PartState.BURN_FULL -> COLOR_BURN_DEEP;
            default -> COLOR_BURN;
        };
    }

    /** The next thing wrong after the one the outline is already showing. */
    private static int secondaryFor(PartState state) {
        int[] byPriority = {
                state.tourniquet ? COLOR_TOURNIQUET : 0,
                state.bleeding() ? COLOR_BLEED : 0,
                state.infected() ? COLOR_INFECTION : 0,
                state.burned ? burnColour(state) : 0,
                state.fractured() ? COLOR_FRACTURE : 0,
                state.cracked ? COLOR_CRACK : 0,
        };
        boolean seenFirst = false;
        for (int colour : byPriority) {
            if (colour == 0) {
                continue;
            }
            if (seenFirst) {
                return colour;
            }
            seenFirst = true;
        }
        return 0;
    }

    private static int lerp(int from, int to, float t) {
        float amount = Mth.clamp(t, 0.0F, 1.0F);
        int r = Math.round(Mth.lerp(amount, (from >> 16) & 0xFF, (to >> 16) & 0xFF));
        int g = Math.round(Mth.lerp(amount, (from >> 8) & 0xFF, (to >> 8) & 0xFF));
        int b = Math.round(Mth.lerp(amount, from & 0xFF, to & 0xFF));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int argb, int alpha) {
        int scaled = (argb >>> 24) * Math.min(255, Math.max(0, alpha)) / 255;
        return (argb & 0x00FFFFFF) | (scaled << 24);
    }
}
