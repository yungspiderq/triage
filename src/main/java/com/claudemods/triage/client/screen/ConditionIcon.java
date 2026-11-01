package com.claudemods.triage.client.screen;

import com.claudemods.triage.Config;
import com.claudemods.triage.body.PartState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The little pictures for the things that can be wrong with a limb.
 *
 * <p>Eight pixels square, drawn from a bitmap of fills like everything else this mod
 * puts on screen. Colour alone was never enough - a red outline says "something", not
 * "you are bleeding" - so each one has a shape and a name to go with it.
 */
public enum ConditionIcon {
    /** A drop. */
    BLEEDING(TreatmentIcons.BLEED, "bleeding", new String[]{
            "...#....",
            "..###...",
            "..###...",
            ".#####..",
            ".#####..",
            ".#####..",
            "..###...",
            "........",
    }),
    HEAVY_BLEEDING(TreatmentIcons.BLEED, "heavy_bleeding", new String[]{
            "..#..#..",
            ".###.##.",
            ".###.##.",
            "#####.#.",
            "#####...",
            "#####.#.",
            ".###.##.",
            "......#.",
    }),
    /**
     * Not a drop. A jet, leaving under pressure, in three beats.
     *
     * <p>Drawn as the only thing on a chart that is going sideways rather than down,
     * because that is the entire difference: everything else here leaks, and this is
     * thrown out of somebody by a pump that has not been told to stop.
     */
    ARTERY(TreatmentIcons.ARTERY, "artery", new String[]{
            "........",
            "#.......",
            "##..#...",
            "###.##.#",
            "####.##.",
            "###.##..",
            "#..#....",
            "........",
    }),
    /**
     * A bone in two pieces, and the pieces are not where they were.
     *
     * <p>Drawn as the fracture icon with the halves stepped apart, because that is the
     * difference: same break, and the ends have gone somewhere. A player who has learnt
     * the first one reads this one without being told.
     */
    DISPLACED(TreatmentIcons.DISPLACED, "displaced", new String[]{
            "..##....",
            "..##....",
            "..##....",
            "...###..",
            ".....##.",
            ".....##.",
            ".....##.",
            ".....##.",
    }),
    /** And the end of it, outside, with what comes out beside it. */
    OPEN_BONE(TreatmentIcons.OPEN_BONE, "open_bone", new String[]{
            "..##....",
            "..##....",
            "..##.#..",
            "..###.#.",
            "....####",
            "...###..",
            "..#.#...",
            ".#......",
    }),
    /** A bone that healed with the angle still in it: one piece, and bent. */
    CROOKED(TreatmentIcons.CROOKED, "crooked", new String[]{
            "..##....",
            "..##....",
            "..###...",
            "...###..",
            "....###.",
            ".....##.",
            ".....##.",
            ".....##.",
    }),
    /**
     * Something solid in a vessel.
     *
     * <p>Drawn as the vessel rather than as a warning: a line with a lump in it, and the
     * line stops on the far side of the lump. It is the only thing on a chart that is a
     * picture of what is <b>not</b> happening.
     */
    CLOT(TreatmentIcons.CLOT, "clot", new String[]{
            "........",
            "########",
            "..####..",
            ".######.",
            ".######.",
            "..####..",
            "###.....",
            "........",
    }),
    /** Two sticks and the ties holding them on. */
    SPLINT(TreatmentIcons.SPLINT, "splint", new String[]{
            "##....##",
            "##....##",
            "########",
            "##....##",
            "##....##",
            "########",
            "##....##",
            "##....##",
    }),
    /**
     * A line of stitches across something that closed.
     *
     * <p>The only icon in here that is not a problem, and it is drawn as the thing
     * itself rather than as a warning: a seam, crossed, healed over.
     */
    SCAR(TreatmentIcons.SCAR, "scar", new String[]{
            "........",
            "..#.....",
            ".###....",
            "..#.#...",
            "...###..",
            "....#.#.",
            ".....###",
            "......#.",
    }),
    /** A bolt: something snapped. */
    FRACTURE(TreatmentIcons.FRACTURE, "fracture", new String[]{
            "....##..",
            "...##...",
            "..##....",
            ".#####..",
            "...##...",
            "..##....",
            ".##.....",
            "##......",
    }),
    /** The same, thinner - not broken yet. */
    CRACK(TreatmentIcons.CRACK, "crack", new String[]{
            "....#...",
            "...#....",
            "..##....",
            "...#....",
            "..#.....",
            "..##....",
            ".#......",
            "#.......",
    }),
    /** A blob of something that should not be there. */
    INFECTION(TreatmentIcons.INFECTION, "infection", new String[]{
            "..####..",
            ".######.",
            "######..",
            "###..###",
            "##..####",
            ".######.",
            "..####..",
            "........",
    }),
    SEVERE_INFECTION(TreatmentIcons.INFECTION, "severe_infection", new String[]{
            "..####..",
            ".##..##.",
            "##.##.##",
            "#.####.#",
            "#.####.#",
            "##.##.##",
            ".##..##.",
            "..####..",
    }),
    /**
     * The three burns, and they are drawn as three different things on purpose.
     *
     * <p>Depth decides everything about a burn - whether it hurts, whether it closes,
     * whether the jar in your pocket is any use - so it is the last thing that should be
     * carried by two shades of orange eight pixels across. A player compares shapes at a
     * glance and compares colours by looking twice.
     *
     * <p>Red skin is a small flame. Blistered is the flame with the blisters beside it,
     * which is the thing the salve game is careful of. And through the skin is not a flame
     * at all: it is a patch with a hard edge and nothing alive in the middle, which is
     * what one looks like and why it is the only one that wants a knife.
     */
    BURN_LIGHT(0xFFFFC266, "burn_light", new String[]{
            "........",
            "...#....",
            "..##....",
            "..###...",
            ".####...",
            ".####...",
            "..###...",
            "........",
    }),
    BURN(TreatmentIcons.BURN, "burn", new String[]{
            "..#.....",
            ".###....",
            ".####.##",
            "#####.##",
            "#####...",
            ".####.##",
            "..###.##",
            "........",
    }),
    BURN_DEEP(0xFF8C4A3C, "burn_deep", new String[]{
            "..####..",
            ".######.",
            "##.##.##",
            "###..###",
            "##..#.##",
            "##.###.#",
            ".######.",
            "..####..",
    }),
    /** A strap round a limb with the windlass rod turned down out of it. */
    TOURNIQUET(TreatmentIcons.TOURNIQUET, "tourniquet", new String[]{
            "........",
            "########",
            "#.####.#",
            "########",
            "...##...",
            "..##....",
            ".##.....",
            "##......",
    }),
    /** Something inside, open, with nothing outside to show for it. */
    ORGAN(TreatmentIcons.ORGAN, "organ", new String[]{
            "..####..",
            ".#....#.",
            "#.####.#",
            "#.#..#.#",
            "#.#..#.#",
            "#.####.#",
            ".#....#.",
            "..####..",
    }),
    /** The same, only bruised. */
    ORGAN_BRUISE(TreatmentIcons.ORGAN, "organ_bruise", new String[]{
            "........",
            "..####..",
            ".#....#.",
            "#......#",
            "#......#",
            ".#....#.",
            "..####..",
            "........",
    }),
    /** A lung squeezed to one side by the air beside it. */
    PNEUMO(TreatmentIcons.PNEUMO, "pneumo", new String[]{
            "..####..",
            ".#....#.",
            "#..##..#",
            "#.####.#",
            "#.####.#",
            "#..##..#",
            ".#....#.",
            "..####..",
    }),
    /** A shaft, still in. */
    ARROW(TreatmentIcons.ARROW, "arrow", new String[]{
            "......##",
            ".....###",
            "....###.",
            "...###..",
            "..###...",
            ".###....",
            "###.....",
            "##......",
    }),
    /**
     * A round, under the skin.
     *
     * <p>Short and blunt where the arrow is long and thin, because that is the whole
     * difference in the wound: a shaft stands out and can be drawn back along itself,
     * and a bullet cannot. The chart has to say which one it is before anybody reaches
     * for the forceps.
     */
    BULLET(TreatmentIcons.ARROW, "bullet", new String[]{
            "........",
            "...###..",
            "..#####.",
            "..#####.",
            "..#####.",
            "...###..",
            "....#...",
            "........",
    }),
    /**
     * Several small ones, still in.
     *
     * <p>The same problem an arrow is and the same answer, so it shares the treatment and
     * everything about it - and gets its own pip, because "an arrow" after a creeper went
     * off is a lie about what happened, and this mod's whole job is getting the word right.
     */
    SHRAPNEL(TreatmentIcons.ARROW, "shrapnel", new String[]{
            "##....##",
            "###...##",
            "......#.",
            "..##....",
            ".###....",
            "......##",
            "....#.##",
            "....##..",
    }),
    /**
     * Somebody's weapon, still in.
     *
     * <p>Drawn straight on rather than at an angle, because that is the difference between
     * this and an arrow and the difference is the whole of it: an arrow is a shaft lying
     * along a limb and this is an object standing out of one, with a handle on it that
     * belongs to whoever put it there.
     */
    BLADE(TreatmentIcons.ARROW, "blade", new String[]{
            "...##...",
            "...##...",
            ".######.",
            "...##...",
            "...##...",
            "...##...",
            "...##...",
            "....#...",
    }),
    /**
     * Closed, and not finished.
     *
     * <p>Drawn as the stitches it is: nothing is wrong with the part and nothing is being
     * taken away, so it cannot look like an injury - it has to look like work that has
     * been done and is still setting.
     */
    MENDING(TreatmentIcons.DRESSED, "mending", new String[]{
            "..#..#..",
            ".###.###",
            "########",
            "..#..#..",
            "..#..#..",
            "########",
            ".###.###",
            "..#..#..",
    }),
    /** A pad, taped down over it. */
    DRESSED(TreatmentIcons.DRESSED, "dressed", new String[]{
            "........",
            ".######.",
            "#......#",
            "#.####.#",
            "#.####.#",
            "#......#",
            ".######.",
            "........",
    }),
    /** The same pad, gone through. */
    SOILED(TreatmentIcons.SOILED, "soiled", new String[]{
            "........",
            ".######.",
            "#..##..#",
            "#.####.#",
            "#.####.#",
            "#..##..#",
            ".##..##.",
            "..#..#..",
    }),
    /** A hand, pushed down on it. */
    PRESSED(TreatmentIcons.PRESSED, "pressed", new String[]{
            ".#.#.#..",
            ".######.",
            "#######.",
            "#######.",
            ".#####..",
            "..###...",
            "...#....",
            "........",
    }),
    /** A limb with the life going out of it from the far end up. */
    DEAD(TreatmentIcons.DEAD, "dead", new String[]{
            "..####..",
            "..####..",
            "..####..",
            ".######.",
            ".##..##.",
            "#.#..#.#",
            "#......#",
            "##....##",
    }),
    /** A cut line, and nothing under it. */
    MISSING(TreatmentIcons.MISSING, "missing", new String[]{
            "..####..",
            "..####..",
            "..####..",
            "########",
            "........",
            "........",
            "........",
            "........",
    }),
    /** A peg under the cut. */
    PROSTHETIC(TreatmentIcons.PROSTHETIC, "prosthetic", new String[]{
            "..####..",
            "..####..",
            "########",
            "...##...",
            "...##...",
            "...##...",
            "..####..",
            "..####..",
    }),
    /** The same peg, with play in it. */
    WORN(TreatmentIcons.PROSTHETIC, "worn", new String[]{
            "..####..",
            "..####..",
            "########",
            "..##....",
            "....##..",
            "..##....",
            ".####...",
            "..####..",
    }),
    /** A cross through it: this one is gone. */
    BLACKED(0xFFB4443C, "blacked", new String[]{
            "##....##",
            "###..###",
            ".######.",
            "..####..",
            "..####..",
            ".######.",
            "###..###",
            "##....##",
    });

    public static final int SIZE = 8;

    private final int colour;
    private final String key;
    private final String[] glyph;

    ConditionIcon(int colour, String key, String[] glyph) {
        this.colour = colour;
        this.key = key;
        this.glyph = glyph;
    }

    public Component label() {
        return Component.translatable("triage.condition." + key);
    }

    public int colour() {
        return colour;
    }

    public void draw(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < glyph.length; row++) {
            String line = glyph[row];
            for (int column = 0; column < line.length(); column++) {
                if (line.charAt(column) == '#') {
                    graphics.fill(x + column, y + row, x + column + 1, y + row + 1, colour);
                }
            }
        }
    }

    /** Everything wrong with this part, worst first. */
    public static List<ConditionIcon> of(PartState state) {
        List<ConditionIcon> icons = new ArrayList<>(4);
        // A limb that is not there is not blacked out - "gone" would be two different
        // words for two very different situations, and only one of them has an answer.
        if (state.gone()) {
            icons.add(MISSING);
            if (state.prosthetic) {
                icons.add(state.wornShare(
                        com.claudemods.triage.Config.values().prostheticWearSeconds * 20) >= 0.7F
                        ? WORN : PROSTHETIC);
            }
        } else if (state.necrotic()) {
            icons.add(DEAD);
        } else if (state.blacked()) {
            icons.add(BLACKED);
        }
        if (state.organHurt()) {
            icons.add(state.organTorn() ? ORGAN : ORGAN_BRUISE);
        }
        // One pip is all the figure gets. Which organ it is lives on its own chart,
        // because eight of them will not fit beside a limb and should not have to.
        if (state.pneumo) {
            icons.add(PNEUMO);
        }
        if (state.mending()) {
            icons.add(MENDING);
        }
        // Two different objects with two different pips, because a chart that called a
        // sword an arrow would be the mod getting the word wrong on the one screen whose
        // whole job is getting the word right.
        if (state.bladeIn()) {
            icons.add(BLADE);
        }
        if (state.arrows > 0) {
            icons.add(state.shrapnel ? SHRAPNEL : ARROW);
        }
        if (state.bullets > 0) {
            icons.add(BULLET);
        }
        if (state.tourniquet) {
            icons.add(TOURNIQUET);
        }
        if (state.arterial()) {
            icons.add(ARTERY);
        } else if (state.bleed == PartState.BLEED_HEAVY) {
            icons.add(HEAVY_BLEEDING);
        } else if (state.bleed == PartState.BLEED_LIGHT) {
            icons.add(BLEEDING);
        }
        if (state.splinted()) {
            icons.add(SPLINT);
        }
        // Last, because it is the only thing on a chart that is not a problem: it is
        // what is left of one. A reader scanning for what to do next should reach it
        // after everything they can actually do something about.
        if (state.marked()) {
            icons.add(SCAR);
        }
        if (state.pressed()) {
            icons.add(PRESSED);
        }
        if (state.dressed) {
            icons.add(state.soiled(Config.values().dressingCleanSeconds * 20) ? SOILED : DRESSED);
        }
        if (state.infected()) {
            icons.add(state.infection >= PartState.INFECTION_SEVERE ? SEVERE_INFECTION : INFECTION);
        }
        if (state.burned) {
            icons.add(switch (state.burn) {
                case PartState.BURN_SUPERFICIAL -> BURN_LIGHT;
                case PartState.BURN_FULL -> BURN_DEEP;
                default -> BURN;
            });
        }
        // The bone, at whichever of the four it is. Worst first, and only one of them:
        // a broken bone is broken once however many ways there are of saying it.
        if (state.openFracture()) {
            icons.add(OPEN_BONE);
        } else if (state.outOfLine()) {
            icons.add(DISPLACED);
        } else if (state.fractured()) {
            icons.add(FRACTURE);
        } else if (state.cracked) {
            icons.add(CRACK);
        }
        // And separately, because it is not a break and never goes away on its own.
        if (state.bent()) {
            icons.add(CROOKED);
        }
        if (state.clotted()) {
            icons.add(CLOT);
        }
        return icons;
    }
}
