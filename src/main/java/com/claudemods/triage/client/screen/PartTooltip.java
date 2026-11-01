package com.claudemods.triage.client.screen;

import com.claudemods.triage.Config;
import com.claudemods.triage.body.BodyHealth;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.PartState;
import com.claudemods.triage.client.BodyFigure;
import com.claudemods.triage.client.ClientBody;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * What a limb says when you point at it: its name, what is left of it, and then one
 * line per thing wrong with it - a picture and a word each.
 *
 * <p>Drawn rather than handed to the vanilla tooltip, because the vanilla one only
 * takes text, and a list of things in brackets after a name was exactly the problem:
 * it reads as a sentence to be parsed instead of as a state to be seen.
 */
public final class PartTooltip {
    private static final int PADDING = 5;
    private static final int LINE = 11;
    private static final int GAP = 4;


    private PartTooltip() {
    }

    public static void render(GuiGraphics graphics, Font font, BodyPart part, PartState state,
                              int mouseX, int mouseY, int screenWidth, int screenHeight) {
        render(graphics, font, part, state, null, mouseX, mouseY, screenWidth, screenHeight);
    }

    /**
     * @param body the whole body this part belongs to, when the caller has it. Only one
     *             thing is read off it and only in realistic: how long ago somebody last
     *             put their hands on this part. A reading with no time against it is a
     *             reading somebody has to remember taking, and the mod already refuses to
     *             do that to a pulse.
     */
    public static void render(GuiGraphics graphics, Font font, BodyPart part, PartState state,
                              @Nullable BodyHealth body,
                              int mouseX, int mouseY, int screenWidth, int screenHeight) {
        Component title = ClientBody.shownTitle(part);
        Component numbers = ClientBody.readout(state, " / ");
        List<ConditionIcon> icons = ConditionIcon.of(state);
        Component age = examined(body, part);

        int width = font.width(title) + GAP + font.width(numbers);
        for (ConditionIcon icon : icons) {
            width = Math.max(width, ConditionIcon.SIZE + GAP + font.width(icon.label()) + 24);
        }
        if (age != null) {
            width = Math.max(width, font.width(age));
        }
        width += PADDING * 2;
        int height = PADDING * 2 + LINE + icons.size() * LINE + (age != null ? LINE : 0);

        // Sit beside the cursor, and never off the edge of the screen.
        int x = Math.min(mouseX + 10, screenWidth - width - 2);
        int y = Math.min(mouseY - 6, screenHeight - height - 2);
        x = Math.max(2, x);
        y = Math.max(2, y);

        Paper.sheet(graphics, x, y, width, height);

        int textX = x + PADDING;
        int line = y + PADDING;
        graphics.drawString(font, title, textX, line,
                state.blacked() ? Paper.RED : Paper.INK, false);
        graphics.drawString(font, numbers, x + width - PADDING - font.width(numbers), line, Paper.INK_SOFT, false);

        if (age != null) {
            graphics.drawString(font, age, textX, line + LINE - 2, Paper.INK_SOFT, false);
        }

        // A bar of what is left, tucked under the name, so the numbers have a shape.
        line += LINE - 2;
        Paper.gauge(graphics, textX, line, width - PADDING * 2, 4, ClientBody.shownRatio(state),
                BodyFigure.fillColour(state));
        line += 5;

        for (ConditionIcon icon : icons) {
            icon.draw(graphics, textX, line + 1);
            // Ribs are the one thing there can be several of, and "broken" three times
            // over is a different problem from "broken" once.
            Component label = icon == ConditionIcon.FRACTURE && state.breaks > 1
                    ? Component.translatable("triage.condition.fracture_many", state.breaks)
                    : icon.label();
            graphics.drawString(font, label, textX + ConditionIcon.SIZE + GAP, line, icon.colour(), false);
            line += LINE;
        }
    }

    /**
     * When this part was last examined, as a line to put under everything else.
     *
     * <p>Null in arcade, where nothing is ever examined because everything is simply
     * known, and null for a part nobody has touched - a blank row says that already and
     * saying it twice is noise.
     */
    private static @Nullable Component examined(@Nullable BodyHealth body, BodyPart part) {
        if (body == null || !Config.values().realistic) {
            return null;
        }
        long at = body.examinedAt(part);
        if (at <= 0L || Minecraft.getInstance().level == null) {
            return null;
        }
        long ago = Math.max(0L, Minecraft.getInstance().level.getGameTime() - at) / 20L;
        return Component.translatable(ago < 60L ? "triage.screen.examined_seconds"
                : "triage.screen.examined_minutes", ago < 60L ? ago : ago / 60L);
    }
}
