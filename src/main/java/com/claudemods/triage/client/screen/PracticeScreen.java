package com.claudemods.triage.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The same game, over nobody.
 *
 * <p>Every procedure in this mod is learnt in the worst possible place. The first saw a
 * player ever holds is over a friend who is going to lose the leg either way; the first
 * time anybody meets the syringe game is the first time somebody's heart has stopped. The
 * guide can tell you a needle wants blood in the hub, and reading that sentence is not the
 * same as having done it once.
 *
 * <p>So the guide runs them. A page with a procedure behind it has a line at the bottom
 * saying so, and pressing it opens the game with <b>nothing on the other end of it</b> - no
 * patient, no packet, no item spent, no outcome. It cannot go well and it cannot go badly.
 * It is the medical manikin that sits in the corner of every teaching room, and it is here
 * for the same reason that one is: so the first time is not the time it counts.
 *
 * <p>Deliberately not a reward and not a requirement. Nothing is unlocked by it and nothing
 * is refused without it, because a mod that made players practise before treating would
 * have invented homework. What it changes is only this: the option to have done it before.
 */
public final class PracticeScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 186;
    private static final int MARGIN = 8;
    private static final int HEADER = 22;

    private final @Nullable Screen parent;
    private final Component subject;
    private final Builder builder;

    private Minigame game;
    private int left;
    private int top;
    /** How long the finished line has been up, so it clears itself rather than sitting there. */
    private int finished;

    /** Makes the game once the panel knows where it is. */
    @FunctionalInterface
    public interface Builder {
        Minigame build(int left, int top, int width, int height);
    }

    public PracticeScreen(@Nullable Screen parent, Component subject, Builder builder) {
        super(Component.translatable("triage.practice.title"));
        this.parent = parent;
        this.subject = subject;
        this.builder = builder;
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        reset();
    }

    /**
     * A fresh one.
     *
     * <p>Offered rather than forced, and offered <em>every time</em>: the whole use of a
     * manikin is that it can be done again immediately, which is the one thing a patient
     * can never offer.
     */
    private void reset() {
        game = builder.build(left + MARGIN, top + HEADER,
                PANEL_WIDTH - MARGIN * 2, PANEL_HEIGHT - HEADER - MARGIN - 14);
        finished = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        game.tick();
        if (game.complete() || game.failed()) {
            finished++;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Paper.sheet(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawString(font, Component.translatable("triage.practice.header", subject),
                left + MARGIN, top + 8, Paper.INK, false);
        Paper.heavyRule(graphics, left + MARGIN, top + HEADER - 6, PANEL_WIDTH - MARGIN * 2);

        game.render(graphics, font, mouseX, mouseY, partialTick);

        Component line;
        int colour;
        if (game.complete() || game.failed()) {
            line = Component.translatable("triage.practice.again");
            colour = Paper.GREEN;
        } else {
            line = game.instruction();
            colour = Paper.INK_SOFT;
        }
        graphics.drawString(font, line, left + MARGIN, top + PANEL_HEIGHT - MARGIN - 8, colour, false);
        // And what it is not, said once on the card rather than left to be worked out.
        graphics.drawString(font, Component.translatable("triage.practice.nobody")
                        .withStyle(ChatFormatting.ITALIC),
                left + PANEL_WIDTH - MARGIN - font.width(Component.translatable("triage.practice.nobody")),
                top + 8, Paper.INK_FAINT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (game.complete() || game.failed()) {
            if (finished > 4) {
                reset();
            }
            return true;
        }
        return game.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return game.mouseDragged(mouseX, mouseY, button)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        game.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null && parent != null) {
            minecraft.setScreen(parent);
            return;
        }
        super.onClose();
    }
}
