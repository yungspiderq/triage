package com.claudemods.triage.client;

import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.Organ;
import com.claudemods.triage.body.PartState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The same figure, from the inside.
 *
 * <p>Drawn in {@link BodyFigure}'s own 32x52 grid and at its scale, so the two are the
 * same person seen two ways rather than two diagrams that happen to sit in one screen.
 * A heart is where a heart is on the body you were just looking at, and switching back
 * and forth does not move anything.
 *
 * <p>Each organ is a shape rather than a box, because a box is a swatch and the one
 * thing this page exists to teach is <em>where</em>. A liver is a wedge under the ribs
 * on one side, a spleen is a small oval on the other, and a player who has seen them in
 * their places once will know which side of a belly a boot went into.
 *
 * <p>Sides follow the figure: the body's left arm is drawn on the left of the screen, so
 * the body's left lung is too - and the liver, which lives on the body's right, comes
 * out on the right. Getting that backwards would teach the wrong lesson perfectly.
 */
public final class OrganFigure {
    /**
     * Where each organ sits and what shape it is, as a mask in figure cells.
     *
     * <p>'#' is the organ. Everything else is nothing. Small enough to read at a glance
     * in source, which is the point: these are anatomy, and anatomy that lives in a
     * table of magic numbers is anatomy nobody will ever correct.
     */
    private record Shape(Organ organ, int x, int y, String[] mask) {
    }

    private static final Shape[] SHAPES = {
            // Filling most of the skull, with the notch at the bottom where the stem
            // goes down. A hole through the middle would read as a doughnut.
            new Shape(Organ.BRAIN, 12, 3, new String[]{
                    " ###### ",
                    "########",
                    "########",
                    "########",
                    " ##..## ",
            }),
            // The lungs take the sides of the chest and are notched where the heart is,
            // which is the actual reason a heart fits between them.
            new Shape(Organ.LEFT_LUNG, 8, 12, new String[]{
                    " ####",
                    "#####",
                    "#####",
                    "#### ",
                    "#### ",
                    "#####",
                    "#####",
                    " ####",
                    "  ## ",
            }),
            new Shape(Organ.RIGHT_LUNG, 19, 12, new String[]{
                    "#### ",
                    "#####",
                    "#####",
                    " ####",
                    " ####",
                    "#####",
                    "#####",
                    "#### ",
                    " ##  ",
            }),
            // Between them and a little to the body's left, with the two bumps on top
            // and the point at the bottom that makes a heart read as a heart.
            new Shape(Organ.HEART, 12, 15, new String[]{
                    "##  ##",
                    "######",
                    "######",
                    " #####",
                    " #### ",
                    "  ##  ",
            }),
            // Under the ribs on the body's right: big, and tapering across the belly.
            // It reaches past the middle, which is why it is the one that gets hit.
            new Shape(Organ.LIVER, 15, 24, new String[]{
                    "########",
                    " #######",
                    "   #####",
            }),
            // And the small one under the ribs on the other side, which is the whole
            // argument for drawing this rather than listing it.
            new Shape(Organ.SPLEEN, 10, 24, new String[]{
                    "####",
                    "####",
            }),
            // A pair, low and towards the back, drawn as the pair they are.
            new Shape(Organ.KIDNEY, 9, 27, new String[]{
                    "##          ##",
                    "##          ##",
            }),
            // And the mass that fills what is left, which is most of a belly. No coil
            // drawn into it: at this size any detail inside a shape is a smudge, and
            // what has to read is the outline.
            new Shape(Organ.GUT, 11, 28, new String[]{
                    " ######## ",
                    "##########",
                    "##########",
                    " ######## ",
            }),
    };

    private static final int COLOR_INTACT = 0xFF4E7F6B;
    private static final int COLOR_BRUISED = 0xFFC08F28;
    private static final int COLOR_TORN = 0xFFB0301F;
    private static final int COLOR_SHADE = 0x50000000;
    private static final int COLOR_CAGE = 0x33000000;
    private static final int COLOR_HIGHLIGHT = 0xFF7FE7FF;

    private OrganFigure() {
    }

    /**
     * Draws the inside of somebody, in the space the outside of them was drawn in.
     *
     * @param hovered the one under the cursor, drawn brighter so hovering means
     *                something without having to move the shapes about.
     */
    public static void draw(GuiGraphics graphics, BodyHealth body, int originX, int originY, int scale,
                            int tick, @Nullable Organ hovered) {
        cage(graphics, originX, originY, scale);
        for (Shape shape : SHAPES) {
            PartState state = body.part(shape.organ().part());
            int level = state.organ(shape.organ());
            int fill = level >= PartState.ORGAN_TORN ? COLOR_TORN
                    : level == PartState.ORGAN_BRUISED ? COLOR_BRUISED : COLOR_INTACT;
            // A torn one beats, because it is the thing running out now. Everything else
            // is a standing fact and should sit still while it is read.
            if (level >= PartState.ORGAN_TORN) {
                fill = BodyFigure.withAlpha(fill, 170 + (int) (85 * Mth.sin(tick * 0.25F)));
            }
            paint(graphics, shape, originX, originY, scale, fill,
                    hovered == shape.organ() ? COLOR_HIGHLIGHT : 0);
        }
    }

    /** The one under the cursor, or null. */
    public static @Nullable Organ at(int originX, int originY, int scale, double mouseX, double mouseY) {
        // Backwards, so the small ones drawn last win the pixels they share with the
        // big ones behind them - a kidney is never unreachable because a gut is wide.
        for (int i = SHAPES.length - 1; i >= 0; i--) {
            Shape shape = SHAPES[i];
            for (int row = 0; row < shape.mask().length; row++) {
                String line = shape.mask()[row];
                for (int col = 0; col < line.length(); col++) {
                    if (line.charAt(col) != '#') {
                        continue;
                    }
                    int x = originX + (shape.x() + col) * scale;
                    int y = originY + (shape.y() + row) * scale;
                    if (mouseX >= x && mouseX < x + scale && mouseY >= y && mouseY < y + scale) {
                        return shape.organ();
                    }
                }
            }
        }
        return null;
    }

    /**
     * The outline everything sits in: a skull, a ribcage, and the line a belly starts at.
     *
     * <p>Faint, because it is not the subject. Without it the organs float in the middle
     * of nothing and the one thing the page is for - where they are in a person - stops
     * being legible at all.
     */
    private static void cage(GuiGraphics graphics, int originX, int originY, int scale) {
        graphics.renderOutline(originX + 11 * scale, originY + 2 * scale,
                10 * scale, 7 * scale, COLOR_CAGE);
        graphics.renderOutline(originX + 7 * scale, originY + 11 * scale,
                18 * scale, 14 * scale, COLOR_CAGE);
        // The ribs themselves, as bars down the sides of the chest, because a ribcage is
        // the thing between a boot and everything above.
        for (int i = 0; i < 4; i++) {
            int y = originY + (13 + i * 3) * scale;
            graphics.fill(originX + 7 * scale, y, originX + 9 * scale, y + scale, COLOR_CAGE);
            graphics.fill(originX + 23 * scale, y, originX + 25 * scale, y + scale, COLOR_CAGE);
        }
        // And the line the ribs stop at, which is where the rules about what a blow
        // finds change completely.
        graphics.fill(originX + 8 * scale, originY + 25 * scale,
                originX + 24 * scale, originY + 25 * scale + Math.max(1, scale / 2), COLOR_CAGE);
        graphics.renderOutline(originX + 9 * scale, originY + 25 * scale,
                14 * scale, 9 * scale, COLOR_CAGE);
    }

    /** One organ's mask, cell by cell, with a shade along its bottom to give it body. */
    private static void paint(GuiGraphics graphics, Shape shape, int originX, int originY, int scale,
                              int fill, int ring) {
        String[] mask = shape.mask();
        for (int row = 0; row < mask.length; row++) {
            String line = mask[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) != '#') {
                    continue;
                }
                int x = originX + (shape.x() + col) * scale;
                int y = originY + (shape.y() + row) * scale;
                graphics.fill(x, y, x + scale, y + scale, fill);
                // Shade the cells with nothing under them, which reads as a rounded
                // edge rather than as a staircase of squares.
                boolean below = row + 1 < mask.length
                        && col < mask[row + 1].length() && mask[row + 1].charAt(col) == '#';
                if (!below) {
                    graphics.fill(x, y + scale - Math.max(1, scale / 2), x + scale, y + scale, COLOR_SHADE);
                }
                if (ring != 0) {
                    boolean edge = col == 0 || col + 1 >= line.length()
                            || line.charAt(col - 1) != '#' || line.charAt(col + 1) != '#'
                            || row == 0 || !below;
                    if (edge) {
                        graphics.renderOutline(x, y, scale, scale, ring);
                    }
                }
            }
        }
    }
}
