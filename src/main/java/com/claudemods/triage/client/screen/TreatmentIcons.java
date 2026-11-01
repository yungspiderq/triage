package com.claudemods.triage.client.screen;

import com.claudemods.triage.Config;
import com.claudemods.triage.body.PartState;
import java.util.ArrayList;
import java.util.List;

/**
 * The colours that stand for each thing that can be wrong with a part, in one place.
 *
 * <p>The figure can only ever show the worst one as an outline; the body screen has room
 * for all of them at once. Both read from here, so a colour never means two things.
 */
public final class TreatmentIcons {
    // Inks rather than lights, and mixed twice. A colour that glowed on a black panel
    // is a smudge on a cream one - the bleed red vanished into the sheet at pip size -
    // and the cream mix disappears just as completely on the dark card.
    public static int BLEED = 0xFFA8281E;
    public static int INFECTION = 0xFF4E7A2E;
    public static int BURN = 0xFFC2560F;
    public static int FRACTURE = 0xFF7A5A10;
    public static int CRACK = 0xFFA08A3C;
    public static int TOURNIQUET = 0xFF2C5878;
    public static int PNEUMO = 0xFF6B3A8A;
    public static int ORGAN = 0xFF6E1030;
    public static int PRESSED = 0xFF2A2118;
    public static int ARROW = 0xFF4A3A22;
    public static int DEAD = 0xFF4A2A4E;
    public static int MISSING = 0xFF2A2118;
    public static int PROSTHETIC = 0xFF6B4A28;
    public static int DRESSED = 0xFF8A8266;
    public static int SOILED = 0xFF6E7A2E;
    /**
     * Brighter than the bleed red, and deliberately the brightest thing on a chart.
     *
     * <p>Blood that has not been anywhere yet is a different colour from blood that has,
     * which is a fact rather than a design decision - and it is the fact that lets one of
     * these be told from a bad cut at a glance.
     */
    public static int ARTERY = 0xFFD8261A;
    public static int SPLINT = 0xFF8A6A3A;
    /** Pale, flat and quiet. It is not a problem, it is what is left of one. */
    public static int SCAR = 0xFF9A8F7A;
    /** The bone ones, from "moved" through "outside" to "healed that way". */
    public static int DISPLACED = 0xFF8A4A10;
    public static int OPEN_BONE = 0xFFD8D0B8;
    public static int CROOKED = 0xFF6E5A2E;
    /** Dark, still, and the colour of blood that has stopped being blood. */
    public static int CLOT = 0xFF4A2340;

    /** Retuned with the sheet. Called by {@link Paper#theme(boolean)} and nowhere else. */
    static void theme(boolean dark) {
        BLEED = dark ? 0xFFDE5A48 : 0xFFA8281E;
        INFECTION = dark ? 0xFF83C060 : 0xFF4E7A2E;
        BURN = dark ? 0xFFE8873A : 0xFFC2560F;
        FRACTURE = dark ? 0xFFD9B45A : 0xFF7A5A10;
        CRACK = dark ? 0xFFB9A472 : 0xFFA08A3C;
        TOURNIQUET = dark ? 0xFF7FB6DC : 0xFF2C5878;
        PNEUMO = dark ? 0xFFB07FD8 : 0xFF6B3A8A;
        ORGAN = dark ? 0xFFD05070 : 0xFF6E1030;
        PRESSED = dark ? 0xFFE8DFC6 : 0xFF2A2118;
        ARROW = dark ? 0xFFC0AA82 : 0xFF4A3A22;
        DEAD = dark ? 0xFF9A6AA8 : 0xFF4A2A4E;
        MISSING = dark ? 0xFFE8DFC6 : 0xFF2A2118;
        PROSTHETIC = dark ? 0xFFC59A62 : 0xFF6B4A28;
        DRESSED = dark ? 0xFFCFC7AC : 0xFF8A8266;
        SOILED = dark ? 0xFFA8B45C : 0xFF6E7A2E;
        ARTERY = dark ? 0xFFFF5B44 : 0xFFD8261A;
        SPLINT = dark ? 0xFFD2AE78 : 0xFF8A6A3A;
        SCAR = dark ? 0xFFBFB6A2 : 0xFF9A8F7A;
        DISPLACED = dark ? 0xFFE0913F : 0xFF8A4A10;
        OPEN_BONE = dark ? 0xFFF2ECD8 : 0xFF6B6350;
        CROOKED = dark ? 0xFFC2AE72 : 0xFF6E5A2E;
        CLOT = dark ? 0xFFA878C0 : 0xFF4A2340;
    }

    private TreatmentIcons() {
    }

    /** Everything wrong with this part, worst first. */
    public static int[] conditions(PartState state) {
        List<Integer> colours = new ArrayList<>(6);
        // Before everything, because they are not conditions of a limb - they are what
        // happened to it. Nothing under them can be treated and most of it cannot even
        // be true any more.
        if (state.gone()) {
            colours.add(MISSING);
            if (state.prosthetic) {
                colours.add(PROSTHETIC);
            }
        }
        if (state.necrotic()) {
            colours.add(DEAD);
        }
        // Beside the crushed lung, and for the same reason: nothing to see, gets worse
        // the longer it waits, and one answer only.
        if (state.organHurt()) {
            colours.add(ORGAN);
        }
        // First of the rest, and the only other one with nothing to see on the figure.
        if (state.pneumo) {
            colours.add(PNEUMO);
        }
        // Before the bleeding, because it is the reason the bleeding cannot be dealt
        // with: everything else on this part is waiting for it to come out.
        if (state.stuck()) {
            colours.add(ARROW);
        }
        if (state.tourniquet) {
            colours.add(TOURNIQUET);
        }
        if (state.bleeding()) {
            colours.add(state.arterial() ? ARTERY : BLEED);
        }
        if (state.splinted()) {
            colours.add(SPLINT);
        }
        if (state.openFracture()) {
            colours.add(OPEN_BONE);
        } else if (state.outOfLine()) {
            colours.add(DISPLACED);
        }
        if (state.bent()) {
            colours.add(CROOKED);
        }
        if (state.clotted()) {
            colours.add(CLOT);
        }
        // Right after the bleed it is holding, because that is the only thing it says.
        if (state.pressed()) {
            colours.add(PRESSED);
        }
        if (state.dressed) {
            colours.add(state.soiled(Config.values().dressingCleanSeconds * 20) ? SOILED : DRESSED);
        }
        if (state.infected()) {
            colours.add(INFECTION);
        }
        if (state.burned) {
            colours.add(BURN);
        }
        // One pip per break, so a chest with three broken ribs looks like three broken
        // ribs on the chart rather than like any other fracture.
        for (int i = 0; i < state.breaks; i++) {
            colours.add(FRACTURE);
        }
        if (state.cracked) {
            colours.add(CRACK);
        }
        int[] result = new int[colours.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = colours.get(i);
        }
        return result;
    }
}
