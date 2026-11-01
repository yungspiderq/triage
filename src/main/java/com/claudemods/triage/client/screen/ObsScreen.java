package com.claudemods.triage.client.screen;

import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.Reading;
import com.claudemods.triage.body.Temperature;
import com.claudemods.triage.body.Trend;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The observation sheet: every reading anybody has taken off this body, in order.
 *
 * <p>The chart says what a body is. This says what it has been doing, which is the half
 * the mod has never had - and it is not a new kind of information so much as the oldest
 * one there is. A ward is a room where somebody writes a number on a page every so often,
 * and the entire value of that page is that the numbers are under each other.
 *
 * <p>Three columns and no cleverness: what was measured, what it read, and how long ago -
 * with the name of whoever took it, because a reading is something a person did. That last
 * column is what makes a handover work: the second medic to arrive can see what the first
 * one already did, and stop doing it again.
 *
 * <p>Nothing here is redacted, in either difficulty. Every line on this page was earned by
 * somebody doing something, and realistic difficulty hides what nobody has found out - not
 * what somebody has.
 */
public final class ObsScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 190;
    private static final int MARGIN = 10;
    private static final int HEADER = 26;
    private static final int ROW_HEIGHT = 11;

    private final @Nullable Screen parent;
    private final BodyHealth body;
    private final @Nullable Component who;

    private int left;
    private int top;

    public ObsScreen(@Nullable Screen parent, BodyHealth body, @Nullable Component who) {
        super(Component.translatable("triage.obs.title"));
        this.parent = parent;
        this.body = body;
        this.who = who;
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

    @Override
    public void onClose() {
        if (minecraft != null && parent != null) {
            minecraft.setScreen(parent);
            return;
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Paper.sync();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        Paper.sheet(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawString(font, title, left + MARGIN, top + 9, Paper.INK, false);
        if (who != null) {
            graphics.drawString(font, who,
                    left + PANEL_WIDTH - MARGIN - font.width(who), top + 9, Paper.BLUE, false);
        }
        Paper.heavyRule(graphics, left + MARGIN, top + HEADER - 8, PANEL_WIDTH - MARGIN * 2);

        List<Reading> log = body.readings();
        if (log.isEmpty()) {
            // Not an error and not an empty list: it is the honest state of a patient
            // nobody has measured, and it is the same sentence the chart uses for a part
            // nobody has looked at.
            graphics.drawString(font, Component.translatable("triage.obs.empty"),
                    left + MARGIN, top + HEADER + 4, Paper.INK_FAINT, false);
            hint(graphics);
            return;
        }

        long now = now();
        int y = top + HEADER;
        // Newest at the top, because the question a sheet like this answers is almost
        // always "what is it now, and what was it before" rather than "how did this
        // start" - and the two readings that answer it end up next to each other.
        for (int i = log.size() - 1; i >= 0 && y < top + PANEL_HEIGHT - 26; i--) {
            row(graphics, log.get(i), now, y);
            y += ROW_HEIGHT;
        }
        hint(graphics);
    }

    /** One line: what, how much, how long ago, and whose hands. */
    private void row(GuiGraphics graphics, Reading reading, long now, int y) {
        graphics.drawString(font, name(reading.kind()), left + MARGIN, y, Paper.INK, false);

        Component value = value(reading);
        int valueX = left + MARGIN + 74;
        graphics.drawString(font, value, valueX, y, colour(reading), false);

        long ago = reading.secondsBefore(now);
        Component when = ago < 60L
                ? Component.translatable("triage.obs.seconds", ago)
                : Component.translatable("triage.obs.minutes", ago / 60L);
        graphics.drawString(font, when, valueX + 52, y, Paper.INK_SOFT, false);

        if (!reading.by().isEmpty()) {
            // The table has no name, so it gets a word instead of one. Nobody can be
            // called this, which is why it is spelt this way.
            Component by = com.claudemods.triage.body.Bedside.BY.equals(reading.by())
                    ? Component.translatable("triage.obs.by_table")
                    : Component.literal(reading.by());
            graphics.drawString(font, by,
                    left + PANEL_WIDTH - MARGIN - font.width(by), y, Paper.INK_FAINT, false);
        }
    }

    /**
     * The line under the page, which is the one thing on it that is an instruction.
     *
     * <p>Worth printing because the mechanic is not discoverable by looking: nothing about
     * a sheet with one line on it says that the second line is where the information is.
     */
    private void hint(GuiGraphics graphics) {
        Component hint = Component.translatable("triage.obs.hint");
        graphics.drawString(font, hint, left + MARGIN, top + PANEL_HEIGHT - 18,
                Paper.INK_FAINT, false);
    }

    private static Component name(int kind) {
        return Component.translatable(switch (kind) {
            case Reading.PULSE -> "triage.trend.pulse";
            case Reading.TEMP -> "triage.trend.temp";
            case Reading.BREATH -> "triage.trend.breath";
            default -> "triage.trend.pain";
        });
    }

    /** Each kind printed the way that kind is printed everywhere else in the mod. */
    private static Component value(Reading reading) {
        return switch (reading.kind()) {
            case Reading.TEMP -> Component.literal(Temperature.readout(reading.value()));
            case Reading.PAIN -> Component.translatable("triage.obs.out_of_ten", reading.value());
            default -> Component.literal(String.valueOf(reading.value()));
        };
    }

    /**
     * Only the newest of each kind is coloured, and only for having moved.
     *
     * <p>A page where every line is coloured is a page nobody reads. What is worth the ink
     * is the top line of each kind, because that is the one being compared against the one
     * under it - which is the whole reason this sheet exists.
     */
    private int colour(Reading reading) {
        Reading newest = body.latest(reading.kind());
        if (newest == null || newest.at() != reading.at()) {
            return Paper.INK_SOFT;
        }
        Trend trend = Trend.of(body, reading.kind());
        if (trend == null || !trend.worthSaying(reading.kind())) {
            return Paper.INK;
        }
        return trend.colour(reading.kind()) == net.minecraft.ChatFormatting.GREEN
                ? Paper.GREEN : Paper.AMBER;
    }

    private static long now() {
        Minecraft client = Minecraft.getInstance();
        return client.level == null ? 0L : client.level.getGameTime();
    }
}
