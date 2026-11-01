package com.claudemods.triage.client.screen;

import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.BulletKinds;
import com.claudemods.triage.body.PartState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * The film, on a lightbox.
 *
 * <p>Every other screen in this mod is printed on the same cream card, because every other
 * screen is somebody's notes. This one is not notes and does not look like them: it is
 * black, and the only things on it are the things that stopped the beam.
 *
 * <p>Which is the whole point of having it. A chart is what a person believed; a film is
 * what is in the arm. It cannot be fooled by morphine, it does not need the patient awake,
 * and it says the same thing to whoever holds it up - so it is the first reading in the mod
 * with no opinion in it anywhere.
 *
 * <p>And it is deliberately, usefully <b>blind</b>. Bone and metal, and nothing else: no
 * bleeding, no infection, no clot, no torn spleen. A film of somebody dying of a ruptured
 * organ is a clean film. Anybody who learns to trust this instead of their hands will kill
 * a patient with it, and that is not a flaw in the mod - it is the instrument.
 */
public final class FilmScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 208;
    private static final int MARGIN = 10;
    private static final int HEADER = 20;

    /** The film itself, and what the beam got through. */
    private static final int FILM = 0xFF0B0F14;
    private static final int FILM_EDGE = 0xFF1B242E;
    private static final int SOFT = 0xFF27333F;
    private static final int BONE = 0xFFD9E4EC;
    private static final int BONE_DIM = 0xFF9AAAB8;
    private static final int CRACKED = 0xFF7FD46B;
    private static final int BROKEN = 0xFFFFD27F;
    private static final int DISPLACED = 0xFFFF9A5A;
    private static final int CROOKED = 0xFFB59BE0;
    private static final int METAL = 0xFFFFFFFF;
    private static final int LABEL = 0xFF7F93A3;

    private final int patientId;
    private final BodyHealth body;

    private int left;
    private int top;

    public FilmScreen(int patientId, BodyHealth body) {
        super(Component.translatable("triage.film.title"));
        this.patientId = patientId;
        this.body = body;
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component patientName() {
        if (minecraft == null || minecraft.level == null) {
            return Component.translatable("triage.screen.them");
        }
        return minecraft.level.getEntity(patientId) instanceof Player player
                ? player.getName() : Component.translatable("triage.screen.them");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(left - 2, top - 2, left + PANEL_WIDTH + 2, top + PANEL_HEIGHT + 2, FILM_EDGE);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, FILM);

        graphics.drawString(font, Component.translatable("triage.film.header", patientName()),
                left + MARGIN, top + 7, LABEL, false);

        skeleton(graphics);
        legend(graphics);
    }

    /**
     * The body, as the beam sees it.
     *
     * <p>Drawn from the bones rather than from the figure the chart uses, because the two
     * are not the same picture and should not be: a chart shows parts, and a film shows
     * what is inside them. Ribs are ribs, the skull is a skull, and the limbs are the long
     * bones that break.
     */
    private void skeleton(GuiGraphics graphics) {
        int cx = left + PANEL_WIDTH / 2 - 30;
        int cy = top + HEADER + 8;

        // Skull.
        PartState head = body.part(BodyPart.HEAD);
        int headColour = colourOf(head);
        Draw.disc(graphics, cx, cy + 12, 11, headColour);
        graphics.fill(cx - 4, cy + 20, cx + 4, cy + 26, headColour);

        // Ribs and spine.
        PartState chest = body.part(BodyPart.THORAX);
        int chestColour = colourOf(chest);
        for (int i = 0; i < 4; i++) {
            int y = cy + 30 + i * 8;
            graphics.fill(cx - 22, y, cx + 22, y + 3, chestColour);
        }
        graphics.fill(cx - 2, cy + 28, cx + 2, cy + 74, BONE_DIM);

        // Pelvis, which the mod calls a stomach and which is still where the belly is.
        PartState belly = body.part(BodyPart.STOMACH);
        graphics.fill(cx - 16, cy + 66, cx + 16, cy + 76, colourOf(belly));

        // The four long bones.
        limb(graphics, cx - 26, cy + 30, -1, body.part(BodyPart.LEFT_ARM), true);
        limb(graphics, cx + 26, cy + 30, 1, body.part(BodyPart.RIGHT_ARM), true);
        limb(graphics, cx - 10, cy + 76, -1, body.part(BodyPart.LEFT_LEG), false);
        limb(graphics, cx + 10, cy + 76, 1, body.part(BodyPart.RIGHT_LEG), false);

        // And the words beside it, because a picture of a broken femur is not a diagnosis
        // until somebody has written which femur.
        int line = top + HEADER + 6;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            Component finding = findingOf(state);
            if (finding == null) {
                continue;
            }
            graphics.drawString(font, Component.translatable("triage.film.line", part.title(), finding),
                    left + PANEL_WIDTH - 140, line, colourOf(state), false);
            line += 11;
        }
        if (line == top + HEADER + 6) {
            graphics.drawString(font, Component.translatable("triage.film.clean"),
                    left + PANEL_WIDTH - 140, line, BONE_DIM, false);
            graphics.drawString(font, Component.translatable("triage.film.clean_warning"),
                    left + PANEL_WIDTH - 140, line + 13, LABEL, false);
        }
    }

    /** One long bone, at whatever state it is in, with whatever metal is in the limb. */
    private void limb(GuiGraphics graphics, int x, int y, int side, PartState state, boolean arm) {
        int length = arm ? 42 : 56;
        if (state.gone()) {
            // Nothing to photograph. The stump is drawn short and the film says so.
            graphics.fill(x - 2, y, x + 3, y + 10, BONE_DIM);
            return;
        }
        int colour = colourOf(state);
        int offset = state.outOfLine() ? side * 5 : 0;
        int bend = state.bent() ? side * 3 : 0;

        graphics.fill(x - 3, y, x + 4, y + length / 2, colour);
        graphics.fill(x - 3 + offset + bend, y + length / 2, x + 4 + offset + bend, y + length, colour);
        if (state.fractured() || state.cracked) {
            // The line itself: across the bone, and jagged for a break rather than a crack.
            int breakY = y + length / 2;
            for (int i = -4; i <= 4; i++) {
                int wobble = state.fractured() ? (i % 2 == 0 ? -1 : 1) : 0;
                graphics.fill(x + i, breakY + wobble, x + i + 1, breakY + wobble + 2, FILM);
            }
        }
        // And what else is in there. Metal is the brightest thing on any film.
        int metal = state.arrows + state.bullets;
        for (int i = 0; i < metal; i++) {
            int fx = x + (i % 2 == 0 ? -7 : 6);
            int fy = y + 12 + i * 9;
            graphics.fill(fx, fy, fx + 4, fy + 3, METAL);
            graphics.fill(fx + 1, fy - 1, fx + 3, fy + 4, METAL);
        }
    }

    /** What colour the beam leaves this bone, which is the finding. */
    private static int colourOf(PartState state) {
        if (state.openFracture() || state.outOfLine()) {
            return DISPLACED;
        }
        if (state.fractured()) {
            return BROKEN;
        }
        if (state.bent()) {
            return CROOKED;
        }
        if (state.cracked) {
            return CRACKED;
        }
        return BONE;
    }

    /** And the line of words for it, or null when the bone is sound and empty. */
    private static Component findingOf(PartState state) {
        if (state.openFracture()) {
            return Component.translatable("triage.film.open");
        }
        if (state.outOfLine()) {
            return Component.translatable("triage.film.displaced");
        }
        if (state.fractured()) {
            return Component.translatable("triage.film.broken", state.breaks);
        }
        if (state.cracked) {
            return Component.translatable("triage.film.crack");
        }
        if (state.bent()) {
            return Component.translatable("triage.film.crooked");
        }
        // The one thing an X-ray was made for. A machine that can find a hairline crack
        // has no trouble at all with a sword, and saying so is the film agreeing with the
        // room for once.
        if (state.bladeIn()) {
            return Component.translatable("triage.film.blade");
        }
        if (state.arrows > 0) {
            return Component.translatable("triage.film.metal", state.arrows + state.bullets);
        }
        if (state.bullets > 0) {
            return Component.translatable("triage.film.bullet", state.bullets,
                    BulletKinds.summary(state.bulletTypes));
        }
        return null;
    }

    /** What the colours mean, and the one sentence that matters more than any of them. */
    private void legend(GuiGraphics graphics) {
        int y = top + PANEL_HEIGHT - 26;
        graphics.fill(left + MARGIN, y - 4, left + PANEL_WIDTH - MARGIN, y - 3, SOFT);
        graphics.drawString(font, Component.translatable("triage.film.legend"),
                left + MARGIN, y, LABEL, false);
        graphics.drawString(font, Component.translatable("triage.film.blind"),
                left + MARGIN, y + 11, LABEL, false);
    }
}
