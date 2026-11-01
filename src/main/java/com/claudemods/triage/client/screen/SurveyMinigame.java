package com.claudemods.triage.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Hands down a body, once, at the speed hands go down a body.
 *
 * <p>The whole game is pace. A sweep that races is a sweep that went past the quiet leg, and
 * a sweep that crawls is four minutes nobody has - so what is drawn is a band moving down at
 * the speed this should be done at, and the job is to keep the hands inside it. Not
 * difficult. It is not supposed to be difficult: it is supposed to take the time it takes,
 * which is the one thing a player in a hurry will not give it.
 *
 * <p>Told nothing about the patient, like the other two examinations. The figure on the
 * screen is an outline and the marks left down it are a record of where the hands have been,
 * not of what they found - that comes back from the server, because that is the part the
 * player is buying.
 */
public final class SurveyMinigame implements Minigame {
    /** How long the sweep should take, in ticks. Four seconds of somebody's life. */
    private static final int LENGTH = 80;

    /** How far off the band the hands may be before that stretch stops counting. */
    private static final float SLACK = 0.11F;

    private static final int OUTLINE = 0xFF8B8578;
    private static final int BODY = 0xFF4A443B;
    private static final int BAND = 0x667FD46B;
    private static final int BAND_EDGE = 0xFF7FD46B;
    private static final int HAND = 0xFFD9A886;
    private static final int HAND_DARK = 0xFF9C6E51;
    private static final int TRACK_GOOD = 0xFF7FD46B;
    private static final int TRACK_POOR = 0xFFD8C36A;

    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final boolean self;

    /** How the sweep went at each twentieth of the way down. */
    private final float[] track = new float[20];

    private int ticks;
    private float hand;
    private boolean started;

    public SurveyMinigame(int left, int top, int width, int height, boolean self) {
        this.left = left;
        this.top = top;
        this.width = Math.max(90, width);
        this.height = Math.max(70, height);
        this.self = self;
    }

    /** Where the band is, as a share of the way down, this tick. */
    private float band() {
        return Mth.clamp(ticks / (float) LENGTH, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        if (!started) {
            return;
        }
        ticks++;
        int slot = Math.min(track.length - 1, (int) (band() * track.length));
        float off = Math.abs(hand - band());
        // Kept rather than averaged: a stretch done badly stays done badly, and a player
        // who catches up afterwards has still gone past whatever was there.
        track[slot] = Math.max(track[slot], off <= SLACK ? 1.0F : Math.max(0.0F, 1.0F - off * 3.0F));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        started = true;
        hand = position(mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        hand = position(mouseY);
        return true;
    }

    private float position(double mouseY) {
        return Mth.clamp((float) ((mouseY - (top + 6)) / Math.max(1, height - 12)), 0.0F, 1.0F);
    }

    @Override
    public float progress() {
        return band();
    }

    @Override
    public float quality() {
        float sum = 0.0F;
        for (float one : track) {
            sum += one;
        }
        return Mth.clamp(sum / track.length, 0.0F, 1.0F);
    }

    @Override
    public boolean complete() {
        return ticks >= LENGTH;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public Component instruction() {
        return Component.translatable(self ? "triage.game.survey_self" : "triage.game.survey");
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        int figureX = left + width / 2;
        int figureTop = top + 6;
        int figureHeight = height - 12;

        // An outline of a person, because what is being swept is a person and not a bar.
        // Head, shoulders, trunk, legs: four rectangles is enough to read as one.
        int headSize = Math.max(6, figureHeight / 9);
        graphics.fill(figureX - headSize / 2, figureTop, figureX + headSize / 2, figureTop + headSize, BODY);
        int shoulder = Math.round(figureHeight * 0.16F) + figureTop;
        graphics.fill(figureX - headSize, shoulder, figureX + headSize, shoulder + figureHeight / 3, BODY);
        int hip = shoulder + figureHeight / 3;
        graphics.fill(figureX - headSize / 2 - 1, hip, figureX - 1, figureTop + figureHeight, BODY);
        graphics.fill(figureX + 1, hip, figureX + headSize / 2 + 1, figureTop + figureHeight, BODY);
        graphics.fill(figureX - headSize - 1, shoulder, figureX - headSize, hip, OUTLINE);
        graphics.fill(figureX + headSize, shoulder, figureX + headSize + 1, hip, OUTLINE);

        // Where the hands should be, and how much room there is around it.
        int bandY = figureTop + Math.round(band() * figureHeight);
        int slack = Math.max(2, Math.round(SLACK * figureHeight));
        graphics.fill(left + 4, bandY - slack, left + width - 4, bandY + slack, BAND);
        graphics.fill(left + 4, bandY - 1, left + width - 4, bandY + 1, BAND_EDGE);

        // And where they are.
        int handY = figureTop + Math.round(hand * figureHeight);
        graphics.fill(figureX - headSize - 6, handY - 2, figureX - headSize + 2, handY + 3, HAND);
        graphics.fill(figureX - headSize - 6, handY + 2, figureX - headSize + 2, handY + 3, HAND_DARK);
        graphics.fill(figureX + headSize - 2, handY - 2, figureX + headSize + 6, handY + 3, HAND);
        graphics.fill(figureX + headSize - 2, handY + 2, figureX + headSize + 6, handY + 3, HAND_DARK);

        // The record down the side: which stretches were done properly and which were not.
        for (int i = 0; i < track.length; i++) {
            if (track[i] <= 0.0F) {
                continue;
            }
            int y = figureTop + figureHeight * i / track.length;
            int tall = Math.max(1, figureHeight / track.length - 1);
            graphics.fill(left + 2, y, left + 4, y + tall, track[i] >= 0.9F ? TRACK_GOOD : TRACK_POOR);
        }
    }
}
