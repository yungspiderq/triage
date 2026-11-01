package com.claudemods.triage.client;

import com.claudemods.triage.Config;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.Carry;
import com.claudemods.triage.body.Disease;
import net.minecraft.network.chat.Component;
import com.claudemods.triage.client.screen.Draw;
import com.claudemods.triage.client.screen.Paper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * The figure in the corner. It is the only place a player can see what is actually
 * wrong with them - the vanilla bar only shows the total - so it stays on screen at all
 * times, and fades back rather than disappearing when there is nothing to report.
 */
public final class ClientHud implements LayeredDraw.Layer {
    private static final int ORIGIN_X = 6;
    private static final int ORIGIN_Y = 6;
    private static final int PADDING = 5;


    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.player.isSpectator()) {
            return;
        }
        Paper.sync();
        // The chart reads this and the corner did not, which made the corner the one
        // screen in the mod that quietly told you everything. A figure coloured by the
        // truth is a diagnosis: the leg with the crack nobody has felt for was already a
        // different colour up here while the chart was honestly printing "not examined".
        BodyHealth body = ClientBody.charted();

        // An untouched body is worth showing, but not worth shouting about.
        int alpha = body.anythingWrong() ? 255 : 120;
        int width = BodyFigure.WIDTH + PADDING * 2;
        int height = BodyFigure.HEIGHT + PADDING * 2;

        // A corner of the same card the chart is printed on, torn off and pinned up.
        graphics.fill(ORIGIN_X + 2, ORIGIN_Y + 2, ORIGIN_X + width + 2, ORIGIN_Y + height + 2,
                BodyFigure.withAlpha(0x50000000, alpha));
        graphics.fill(ORIGIN_X, ORIGIN_Y, ORIGIN_X + width, ORIGIN_Y + height,
                BodyFigure.withAlpha(Paper.SHEET, alpha));
        Paper.outline(graphics, ORIGIN_X, ORIGIN_Y, width, height,
                BodyFigure.withAlpha(Paper.SHEET_DEEP, alpha));

        BodyFigure.draw(graphics, body, ORIGIN_X + PADDING, ORIGIN_Y + PADDING, 1,
                alpha, minecraft.player.tickCount, null);

        // Illness has no limb to sit on, so it is named under the figure instead - as a
        // coloured chip rather than loose text, so several at once still read as a list.
        int line = ORIGIN_Y + height + 3;
        // And the names. A redacted body has no diseases on it at all - the mode takes
        // the list away on purpose, because a person knows they are hot and does not know
        // the word for why - so in realistic this loop finds nothing and draws nothing.
        // Nothing here had to be gated: the data simply stopped arriving.
        for (Disease disease : Disease.VALUES) {
            if (body.has(disease)) {
                line = chip(graphics, minecraft, disease.title().copy().withStyle(disease.colour()), line);
            }
        }
        Player carried = Carry.carrying(minecraft.player);
        if (carried != null) {
            line = chip(graphics, minecraft, Component.translatable("triage.hud.carrying",
                    carried.getName(), ModKeys.GIVE_UP.getTranslatedKeyMessage()), line);
        }
        // What is left is not a diagnosis and stays: a dose you took yourself, a clock
        // that is yours, and somebody over your shoulder. Read off the whole body rather
        // than the redacted copy, because none of it is a thing anybody had to examine
        // you to know.
        BodyHealth own = ClientBody.get();
        if (own.painkilled()) {
            chip(graphics, minecraft, Component.translatable("triage.hud.painkiller", own.painkillerTicks() / 20),
                    line);
        }
        if (own.downed()) {
            downed(graphics, minecraft, own);
        }
        whisper(graphics, minecraft);
    }

    /**
     * A word across the middle of everything.
     *
     * <p>Drawn last, over the top of the mod's own readouts and everything else, because
     * it is not a readout: it is the one thing on this screen that is not information.
     * It fades in and out and never appears in the same place twice, so that it cannot
     * be read as part of the interface - which is the entire difference between a
     * hallucination and a label.
     */
    private static void whisper(GuiGraphics graphics, Minecraft minecraft) {
        Component text = Whispers.screenText();
        if (text == null) {
            return;
        }
        int alpha = Math.round(Whispers.screenAlpha() * 255.0F);
        if (alpha <= 8) {
            return;
        }
        int x = Math.round(graphics.guiWidth() * Whispers.screenX()) - minecraft.font.width(text) / 2;
        int y = Math.round(graphics.guiHeight() * Whispers.screenY());
        graphics.drawString(minecraft.font, text, x, y, (alpha << 24) | 0xE8DFC6, true);
    }

    /**
     * The only thing on screen that matters while you are on the floor: how long is
     * left, and the way out if nobody is coming.
     */
    private static void downed(GuiGraphics graphics, Minecraft minecraft, BodyHealth body) {
        int centreX = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 30;

        // While a dressing holds, the clock is stopped and saying so is the whole point:
        // a number that has quietly stopped falling looks exactly like one that has not,
        // and the player needs to know whether anybody has time to reach them.
        boolean stabilised = body.stabilised();
        // In arrest the number is not yours any more. Saying so plainly matters more
        // here than anywhere else on this screen: a countdown that looks like the last
        // one reads as more of the same, and this one only moves while somebody is
        // kneeling on your chest.
        Component title = body.arrested()
                ? Component.translatable("triage.hud.arrest", body.arrestTicks() / 20)
                : stabilised
                        ? Component.translatable("triage.hud.stabilised",
                                body.stabilisedTicks() / 20, body.downedTicks() / 20)
                        : Component.translatable("triage.hud.downed", body.downedTicks() / 20);
        graphics.drawString(minecraft.font, title, centreX - minecraft.font.width(title) / 2, y,
                body.arrested() ? Paper.RED_DEEP : stabilised ? Paper.BLUE : Paper.RED, true);

        // You can hear this one already. The line is for the half of it a sound cannot
        // carry: that it is a thing somebody else can fix, in a second, for nothing.
        if (Config.values().airway && body.airwayBlocked(Config.values().airwaySeconds * 20)) {
            Component airway = Component.translatable("triage.hud.airway");
            graphics.drawString(minecraft.font, airway,
                    centreX - minecraft.font.width(airway) / 2, y - 24, Paper.RED_DEEP, true);
        }

        Component prompt = Component.translatable("triage.hud.give_up", ModKeys.GIVE_UP.getTranslatedKeyMessage());
        graphics.drawString(minecraft.font, prompt,
                centreX - minecraft.font.width(prompt) / 2, y + 12, Paper.INK, true);

        // Somebody has hold of you, and the clock knows it. Worth saying: a number that
        // has quietly started falling at half speed is otherwise indistinguishable from
        // a number that has not.
        if (minecraft.player != null && minecraft.player.getVehicle() instanceof Player carrier) {
            Component carried = Component.translatable("triage.hud.carried", carrier.getName());
            graphics.drawString(minecraft.font, carried,
                    centreX - minecraft.font.width(carried) / 2, y - 12, Paper.BLUE, true);
        }

        // The bar only appears once the key is actually down, so it reads as a
        // consequence of holding rather than as another countdown to watch.
        int held = ClientBody.giveUpHeld();
        if (held > 0) {
            int width = 80;
            int filled = Math.round(width * Math.min(1.0F, held / (float) ClientBody.GIVE_UP_TICKS));
            graphics.fill(centreX - width / 2, y + 24, centreX + width / 2, y + 28, chipFill());
            graphics.fill(centreX - width / 2, y + 24, centreX - width / 2 + filled, y + 28, Paper.RED);
        }
    }

    /** The chip a line of text sits on: the same card, a shade off it. */
    private static int chipFill() {
        return Paper.dark() ? 0xE0302D25 : 0xD8E8DFC6;
    }

    /** @return the y the next chip should go at */
    private static int chip(GuiGraphics graphics, Minecraft minecraft, Component label, int y) {
        int width = minecraft.font.width(label) + 8;
        graphics.fill(ORIGIN_X, y, ORIGIN_X + width, y + 11, chipFill());
        Paper.outline(graphics, ORIGIN_X, y, width, 11, Paper.SHEET_DEEP);
        graphics.drawString(minecraft.font, label, ORIGIN_X + 4, y + 2, Paper.INK, false);
        return y + 13;
    }
}
