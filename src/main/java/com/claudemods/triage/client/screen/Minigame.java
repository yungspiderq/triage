package com.claudemods.triage.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The part of a treatment the player has to actually do.
 *
 * <p>Each one owns a rectangle of the screen, given to it when it is built, and is
 * driven by the mouse inside that rectangle. None of them can heal anything by
 * themselves: the screen sends a packet when {@link #complete()} turns true, and the
 * server decides what that is worth.
 */
public interface Minigame {
    /** Once per client tick, while the screen is open. */
    void tick();

    void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick);

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button) {
        return false;
    }

    default void mouseReleased(double mouseX, double mouseY, int button) {
    }

    /** 0 to 1, for the bar under the game. */
    float progress();

    /**
     * How well it was done, 0 to 1.
     *
     * <p>Only the free ones ask. Everything with an item behind it is pass or fail -
     * the bandage is spent either way and the wound is closed either way - but a press
     * or a rhythm is a thing you can do badly and still have done, so those hand the
     * number on and the server decides what it was worth.
     */
    default float quality() {
        return 1.0F;
    }

    boolean complete();

    /** True when it has gone wrong badly enough to stop. The item is not spent. */
    boolean failed();

    /** One line telling the player what to do with their hands. */
    Component instruction();
}
