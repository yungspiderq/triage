package com.claudemods.triage.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The look of every screen this mod owns: a printed form, on one of two papers.
 *
 * <p>They used to be dark bevelled panels with a coloured line round the edge, which is
 * the shape every mod menu has and says nothing about this one. The mod is named after
 * the practice of writing on wounded people and pinning the result to their chest - the
 * paperwork <em>is</em> the subject - so the screens are the paperwork now: ruled lines,
 * ink, printed boxes, and red where something is wrong.
 *
 * <p>There are two of them because a chart is a thing you read at four in the morning
 * with a monitor in your face, and a cream sheet at that hour is a lamp. The dark one is
 * the carbon copy: the same form, the same rules and the same one accent, printed pale
 * on card instead of dark on paper. Neither is a filter over the other - every colour is
 * mixed twice, because ink that works on cream is a smudge on charcoal and the other way
 * round.
 *
 * <p>The palette is mutable on purpose. Everything that draws reads these fields at the
 * moment it draws, so flipping the theme is one assignment rather than a screen rebuild.
 */
public final class Paper {
    /** Starts as the light sheet so that the fields below are the truth until synced. */
    private static boolean dark;

    /** Aged paper, and the two darker shades of it that give a sheet its edges. */
    public static int SHEET = 0xFFE8DFC6;
    public static int SHEET_SHADE = 0xFFD5C9A8;
    public static int SHEET_DEEP = 0xFFB9AA85;
    /** A second sheet behind the first, so a panel looks like more than one page. */
    public static int BEHIND = 0xFFCFC2A0;
    /** What a printed box is filled with: paper, slightly bleached by the press. */
    public static int FIELD = 0xFFF3ECD9;

    public static int INK = 0xFF2A2118;
    public static int INK_SOFT = 0xFF6E6250;
    public static int INK_FAINT = 0x40241F16;
    /** The one accent. Nothing else in the mod may be this colour. */
    public static int RED = 0xFFA8281E;
    /** The same accent with the weight of a full stop, for a stopped heart. */
    public static int RED_DEEP = 0xFF7A1712;
    /** Pencil, for the things somebody else wrote on you. */
    public static int BLUE = 0xFF2C5878;
    public static int GREEN = 0xFF3D6B33;
    public static int AMBER = 0xFF9A6B15;
    /** What sits behind the shadow of a sheet, and behind a chip on the HUD. */
    public static int SHADOW = 0x50000000;

    private Paper() {
    }

    public static boolean dark() {
        return dark;
    }

    /**
     * Brings the palette in line with the setting, if it has moved.
     *
     * <p>Called at the top of everything that draws rather than from a config listener.
     * A listener would have to exist on the client only, fire before the first screen
     * opens and again after every reload; a comparison of two booleans once a frame is
     * cheaper than getting all of that right.
     */
    public static void sync() {
        if (dark != com.claudemods.triage.ClientConfig.dark) {
            theme(com.claudemods.triage.ClientConfig.dark);
        }
    }

    /**
     * Mixes the whole palette for one of the two papers.
     *
     * <p>Called when the client config loads and whenever somebody flips it on the
     * chart. It also retunes the condition colours, which are the same decision one
     * layer down: a bleed pip has to be found at four pixels wide on whichever sheet
     * this is.
     */
    public static void theme(boolean useDark) {
        dark = useDark;
        if (useDark) {
            SHEET = 0xFF25231D;
            SHEET_SHADE = 0xFF302D25;
            SHEET_DEEP = 0xFF141310;
            BEHIND = 0xFF1B1915;
            FIELD = 0xFF17160F;
            INK = 0xFFE8DFC6;
            INK_SOFT = 0xFF9A907A;
            INK_FAINT = 0x40E8DFC6;
            RED = 0xFFDE5A48;
            RED_DEEP = 0xFFB03A2C;
            BLUE = 0xFF7FB6DC;
            GREEN = 0xFF7FBF6A;
            AMBER = 0xFFD9A93E;
            SHADOW = 0x70000000;
        } else {
            SHEET = 0xFFE8DFC6;
            SHEET_SHADE = 0xFFD5C9A8;
            SHEET_DEEP = 0xFFB9AA85;
            BEHIND = 0xFFCFC2A0;
            FIELD = 0xFFF3ECD9;
            INK = 0xFF2A2118;
            INK_SOFT = 0xFF6E6250;
            INK_FAINT = 0x40241F16;
            RED = 0xFFA8281E;
            RED_DEEP = 0xFF7A1712;
            BLUE = 0xFF2C5878;
            GREEN = 0xFF3D6B33;
            AMBER = 0xFF9A6B15;
            SHADOW = 0x50000000;
        }
        TreatmentIcons.theme(useDark);
    }

    /**
     * A sheet of paper with a page behind it.
     *
     * <p>Drawn with a hard shadow and no bevel at all: paper does not have a lit top
     * edge, and giving it one is what made the old panels read as a plastic dialog.
     */
    public static void sheet(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 4, y + 4, x + width + 4, y + height + 4, SHADOW);
        // The page underneath, showing along two edges.
        graphics.fill(x + 3, y - 2, x + width + 2, y + height - 1, BEHIND);
        graphics.fill(x, y, x + width, y + height, SHEET);
        // Paper is not a rectangle of one colour: the edges are darker where it curls.
        graphics.fill(x, y, x + width, y + 1, SHEET_SHADE);
        graphics.fill(x, y + height - 1, x + width, y + height, SHEET_DEEP);
        graphics.fill(x, y, x + 1, y + height, SHEET_SHADE);
        graphics.fill(x + width - 1, y, x + width, y + height, SHEET_DEEP);
    }

    /** A printed rule across the form. Thin, and never pure black. */
    public static void rule(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, INK_FAINT);
    }

    /** The double rule a form puts under its heading. */
    public static void heavyRule(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, INK);
        graphics.fill(x, y + 2, x + width, y + 3, INK_FAINT);
    }

    /**
     * A box printed onto the form for something to be written in.
     *
     * <p>An outline and a bleached fill rather than a sunken bevel. The bevel was there
     * to say "this is a container", and on paper the outline already says it.
     */
    public static void box(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, FIELD);
        outline(graphics, x, y, width, height, INK_SOFT);
    }

    /** The plain one, in the faint ink: a thing that is drawn but is not news. */
    public static void outline(GuiGraphics graphics, int x, int y, int width, int height) {
        outline(graphics, x, y, width, height, INK_SOFT);
    }

    public static void outline(GuiGraphics graphics, int x, int y, int width, int height, int colour) {
        graphics.fill(x, y, x + width, y + 1, colour);
        graphics.fill(x, y + height - 1, x + width, y + height, colour);
        graphics.fill(x, y, x + 1, y + height, colour);
        graphics.fill(x + width - 1, y, x + width, y + height, colour);
    }

    /** A faint wash over a row, for the one being pointed at. */
    public static void highlight(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, dark ? 0x22FFFFFF : 0x18000000);
    }

    /**
     * A printed gauge: an outlined box filled in solid as far as it goes.
     *
     * <p>No gradient and no gloss. A gauge on a form is filled in with a pen, and the
     * gradient bars were the single most video-game thing on the old screens.
     */
    public static void gauge(GuiGraphics graphics, int x, int y, int width, int height,
                             float progress, int colour) {
        graphics.fill(x, y, x + width, y + height, FIELD);
        int inner = Math.round((width - 2) * Mth.clamp(progress, 0.0F, 1.0F));
        if (inner > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + inner, y + height - 1, colour);
        }
        outline(graphics, x, y, width, height, INK_SOFT);
        // Quarter ticks along the top, so a bar is a measurement rather than a glow.
        for (int i = 1; i < 4; i++) {
            int tick = x + width * i / 4;
            graphics.fill(tick, y, tick + 1, y + 2, INK_SOFT);
        }
    }

    /**
     * A heading in small type with a rule under it, the way a form labels a section.
     *
     * @return the y the first line under it should sit at
     */
    public static int caption(GuiGraphics graphics, Font font, Component text, int x, int y, int width) {
        graphics.drawString(font, text, x, y, INK_SOFT, false);
        rule(graphics, x, y + 10, width);
        return y + 14;
    }

    /** Ink that can be read on a given fill: the dark one unless the fill is dark. */
    public static int inkOn(int argb) {
        int luma = (((argb >> 16) & 0xFF) * 30 + ((argb >> 8) & 0xFF) * 59 + (argb & 0xFF) * 11) / 100;
        return luma > 120 ? 0xFF2A2118 : 0xFFE8DFC6;
    }
}
