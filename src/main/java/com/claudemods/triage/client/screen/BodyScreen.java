package com.claudemods.triage.client.screen;

import com.claudemods.triage.net.ModNetwork;
import com.claudemods.triage.Config;
import com.claudemods.triage.body.BloodType;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.AidKind;
import com.claudemods.triage.body.Bones;
import com.claudemods.triage.body.Scars;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.Findings;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.body.Organ;
import com.claudemods.triage.body.PartState;
import com.claudemods.triage.body.Breathing;
import com.claudemods.triage.body.Pulse;
import com.claudemods.triage.body.Trend;
import com.claudemods.triage.body.Reading;
import com.claudemods.triage.body.Temperature;
import com.claudemods.triage.body.TriageTag;
import com.claudemods.triage.client.BodyFigure;
import com.claudemods.triage.client.ClientBody;
import com.claudemods.triage.client.OrganFigure;
import com.claudemods.triage.net.AidPayload;
import com.claudemods.triage.net.WriteTagPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

/**
 * The whole body, read-only: every part with the health it has left, everything wrong
 * with it, how much blood is missing and what illness is running.
 *
 * <p>The corner HUD has to stay small enough to ignore, so it can only ever show colour.
 * This is where the numbers live, and it is worth opening before deciding whether to
 * spend a surgical kit or walk it off.
 *
 * <p>Laid out as the thing it is: a printed casualty card, filled in. See {@link Paper}
 * for why that is worth the trouble.
 *
 * <p>When it is somebody else's chart, it is also where it gets written on. The mark
 * goes in the header rather than at the bottom because it is the conclusion, not a
 * footnote: everything below it is the working that led there.
 */
public final class BodyScreen extends Screen {
    private static final int PANEL_WIDTH = 342;
    private static final int PANEL_HEIGHT = 258;
    private static final int MARGIN = 12;
    private static final int HEADER = 44;
    private static final int FIGURE_SCALE = 2;
    private static final int FIGURE_COLUMN = 100;
    private static final int ROW_HEIGHT = 18;
    private static final int BAR_WIDTH = 54;

    /** One row of the little list that opens on a limb with more than one answer. */
    private static final int MENU_ROW = 12;

    private static final int SWATCH_WIDTH = 22;
    private static final int SWATCH_HEIGHT = 13;
    private static final int SWATCH_GAP = 3;
    private static final int TEAR_WIDTH = 13;
    /** The way into the guide, next to the title where the eye already is. */
    private static final int HELP_SIZE = 11;
    /** And the switch for which paper it is printed on, next to that. */
    private static final int THEME_SIZE = 11;
    /** And the switch for skin or insides, which sits on the figure it changes. */
    private static final int ORGAN_SIZE = 11;


    private int left;
    private int top;
    private @Nullable BodyPart menuPart;
    private List<AidKind> menu = List.of();
    private int menuX;
    private int menuY;
    private int figureX;
    private int figureY;

    /** Whether the figure is showing the outside of somebody or the inside. */
    private boolean showOrgans;
    private @Nullable Organ hoveredOrgan;
    private int listLeft;
    private int listTop;
    private int swatchesLeft;
    private int swatchesTop;
    private int ticks;

    /**
     * Somebody else's body, sent over, or null when this is your own.
     *
     * <p>A snapshot rather than a live feed on purpose: this is a look, not a treatment.
     * You walked up, you saw what you saw, and if you want to know whether it has got
     * worse you look again. Writing a mark redraws it, which is honest enough - you have
     * just looked again.
     */
    private @Nullable BodyHealth patient;
    private final @Nullable Component patientName;
    private final int patientId;

    public BodyScreen() {
        super(Component.translatable("triage.screen.body"));
        this.patient = null;
        this.patientName = null;
        this.patientId = -1;
    }

    public BodyScreen(int patientId, Component name, BodyHealth patient) {
        super(Component.translatable("triage.screen.body"));
        this.patient = patient;
        this.patientName = name;
        this.patientId = patientId;
    }

    /** Whether this screen already shows that player, so it can be redrawn in place. */
    public boolean showing(int id) {
        return patient != null && patientId == id;
    }

    public void update(BodyHealth body) {
        patient = body;
    }

    @Override
    protected void init() {
        layout(patient != null ? patient : ClientBody.charted());
    }

    private void layout(BodyHealth body) {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        figureX = left + MARGIN - 2 + (FIGURE_COLUMN - 8 - BodyFigure.WIDTH * FIGURE_SCALE) / 2;
        figureY = top + HEADER + 8;
        listLeft = left + MARGIN + FIGURE_COLUMN;
        listTop = top + HEADER + 6;
        swatchesTop = top + 24;
        swatchesLeft = left + PANEL_WIDTH - MARGIN
                - (SWATCH_WIDTH * TriageTag.VALUES.length + SWATCH_GAP * (TriageTag.VALUES.length - 1));
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Paper.sync();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        BodyHealth body = patient != null ? patient : ClientBody.charted();
        layout(body);
        Paper.sheet(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawString(font, title, left + MARGIN, top + 10, Paper.INK, false);
        // A chart tells you what is wrong and never told anybody what to do about it,
        // which left the answer to guesswork and the wiki. The way to it belongs here,
        // beside the diagnosis, rather than on a key nobody knows about.
        int helpX = helpLeft();
        boolean overHelp = within(mouseX, mouseY, helpX, top + 8, HELP_SIZE, HELP_SIZE);
        Paper.box(graphics, helpX, top + 8, HELP_SIZE, HELP_SIZE);
        if (overHelp) {
            Paper.outline(graphics, helpX, top + 8, HELP_SIZE, HELP_SIZE, Paper.INK);
        }
        graphics.drawString(font, "?", helpX + 4, top + 10, Paper.INK, false);
        // Which paper this is printed on. A half-filled square rather than a word,
        // because it means the same thing in both states and needs no room.
        int themeX = themeLeft();
        boolean overTheme = within(mouseX, mouseY, themeX, top + 8, THEME_SIZE, THEME_SIZE);
        Paper.box(graphics, themeX, top + 8, THEME_SIZE, THEME_SIZE);
        graphics.fill(themeX + 2, top + 10, themeX + THEME_SIZE / 2, top + 8 + THEME_SIZE - 2, Paper.INK);
        Paper.outline(graphics, themeX, top + 8, THEME_SIZE, THEME_SIZE,
                overTheme ? Paper.INK : Paper.INK_SOFT);
        // The page of readings. Three horizontal strokes, because that is what a sheet of
        // written-down numbers looks like from across a room and because a chart does not
        // have words on its buttons.
        int obsX = obsLeft();
        boolean overObs = within(mouseX, mouseY, obsX, top + 8, HELP_SIZE, HELP_SIZE);
        Paper.box(graphics, obsX, top + 8, HELP_SIZE, HELP_SIZE);
        for (int i = 0; i < 3; i++) {
            graphics.fill(obsX + 3, top + 11 + i * 3, obsX + HELP_SIZE - 3, top + 12 + i * 3,
                    Paper.INK_SOFT);
        }
        Paper.outline(graphics, obsX, top + 8, HELP_SIZE, HELP_SIZE,
                overObs ? Paper.INK : Paper.INK_SOFT);
        if (patientName != null) {
            graphics.drawString(font, patientName,
                    left + PANEL_WIDTH - MARGIN - font.width(patientName), top + 10, Paper.BLUE, false);
        }
        Component tagTip = tagRow(graphics, body, mouseX, mouseY);
        Paper.heavyRule(graphics, left + MARGIN, top + HEADER - 6, PANEL_WIDTH - MARGIN * 2);

        BodyPart hovered = showOrgans
                ? null : BodyFigure.at(figureX, figureY, FIGURE_SCALE, mouseX, mouseY);
        BodyPart hoveredRow = rowAt(mouseX, mouseY);
        BodyPart lit = hovered != null ? hovered : hoveredRow;

        // The figure sits in a printed box, the way a diagram does on a real form.
        Paper.box(graphics, left + MARGIN - 2, top + HEADER + 2, FIGURE_COLUMN - 8,
                BodyFigure.HEIGHT * FIGURE_SCALE + 12);
        if (showOrgans) {
            // The same person, from the inside, in the same place at the same size. It
            // is a view of the figure rather than another screen, so that switching is
            // turning a body over rather than going somewhere and coming back.
            hoveredOrgan = OrganFigure.at(figureX, figureY, FIGURE_SCALE, mouseX, mouseY);
            OrganFigure.draw(graphics, body, figureX, figureY, FIGURE_SCALE, ticks, hoveredOrgan);
        } else {
            hoveredOrgan = null;
            BodyFigure.draw(graphics, body, figureX, figureY, FIGURE_SCALE, 255, ticks,
                    lit == null ? null : part -> part == lit);
            // A limb with something a bare hand can do says so on the figure, because
            // the alternative is a player who never finds out their hands are a
            // treatment.
            hands(graphics, body);
        }
        boolean overOrgans = organSwitch(graphics, body, mouseX, mouseY);

        for (BodyPart part : BodyPart.VALUES) {
            row(graphics, body, part, listTop + part.ordinal() * ROW_HEIGHT, part == lit);
        }

        footer(graphics, body);

        // This screen exists to be read, so the explanation goes where the eye is. The
        // little list is drawn over the top of all of it and swallows the tooltip: while
        // it is open it is the only thing being asked.
        if (!menu.isEmpty()) {
            drawMenu(graphics, mouseX, mouseY);
        } else if (hoveredOrgan != null) {
            organTooltip(graphics, body, mouseX, mouseY);
        } else if (overOrgans) {
            graphics.renderTooltip(font, Component.translatable(showOrgans
                    ? "triage.organs.to_body" : "triage.organs.to_organs"), mouseX, mouseY);
        } else if (lit != null) {
            PartTooltip.render(graphics, font, lit, body.part(lit), body, mouseX, mouseY, width, height);
        } else if (tagTip != null) {
            graphics.renderTooltip(font, tagTip, mouseX, mouseY);
        }
    }

    /**
     * What can be done for this patient with nothing in your hands.
     *
     * <p>Never on your own chart. Both of these are things somebody else does to you,
     * and offering them to a player reading their own body would be offering a way out
     * of being down - which is the one thing being down has never had.
     *
     * <p>They live on the chart rather than on a key or a click in the world because
     * this is where the reason to do either of them is written. You walked over, you
     * looked, and the reason is on the screen in front of you; making the player close
     * it and find a gesture would put a step between seeing and acting in the one place
     * the mod cannot afford one.
     */
    private List<AidKind> actionsFor(BodyPart part, BodyHealth body) {
        Config.Values config = Config.values();
        PartState state = body.part(part);
        List<AidKind> out = new ArrayList<>(3);
        // An arrow first, because nothing else about that limb can be dealt with while
        // it is in - including the hand that would otherwise go on the wound.
        if ((config.arrows || config.bulletsStick || config.weaponsStick) && state.stuck()) {
            out.add(AidKind.PULL);
        }
        // Taking things off is free, works on yourself, and is the only way to change
        // your mind without spending something.
        if (state.tourniquet) {
            out.add(AidKind.UNSTRAP);
        }
        if (config.dressings && state.dressed) {
            out.add(AidKind.UNWRAP);
        }
        // The sweep, first in the list because it is first in the job: one pass over the
        // whole body says which of these seven rows are worth opening.
        out.add(AidKind.SURVEY);
        // Feeling along the bone. Offered on any part that still has one, and offered in
        // both difficulties: in arcade it is a slower way to learn what the chart already
        // says, and somebody will still do it, because it is what hands are for. In
        // realistic it is the only way there is without a machine.
        if (!state.gone()) {
            out.add(AidKind.FEEL);
        }
        // A pulse is taken at a wrist, so there has to be one. With both arms gone it is
        // the neck instead, which is where it is taken in earnest anyway - and it stays
        // above the line below, because feeling your own is a thing people do.
        if (config.pulse) {
            boolean wrist = (part == BodyPart.LEFT_ARM || part == BodyPart.RIGHT_ARM) && !state.gone();
            boolean neck = part == BodyPart.HEAD
                    && body.part(BodyPart.LEFT_ARM).gone() && body.part(BodyPart.RIGHT_ARM).gone();
            if (wrist || neck) {
                out.add(AidKind.PULSE);
            }
        }
        // Asking lives on the head because that is where the answer comes from, and it
        // is offered even when they cannot answer: finding out that somebody has stopped
        // being able to talk is most of what asking is for.
        if (config.ask && part == BodyPart.HEAD) {
            out.add(AidKind.ASK);
            // The other half of the same conversation: where, and how bad.
            out.add(AidKind.SCORE);
        }
        // And pressing on a burn, which goes on the part that is burnt and nowhere else.
        // Above the line as well: a burn on your own arm is one you can press on, and
        // finding out that your own does not hurt is the worst thing you will learn today.
        if (config.burns && state.burned && !state.gone()) {
            out.add(AidKind.TOUCH);
        }
        // And working a stiff joint loose, which is the only thing on this list that is
        // about something months old. Above the line with the others that can be done to
        // yourself - badly, because you stop at your own pain, but done.
        // Before the stretching and before everything below it: a bone out of line is
        // the one thing on this chart with a deadline measured in the week it takes to
        // knit, and after that week it is not a treatment any more, it is an operation.
        if (Bones.worthSetting(state)) {
            out.add(AidKind.SET);
        }
        if (Scars.worthStretching(state)
                && !Scars.workedToday(minecraft == null ? null : minecraft.level, state)) {
            out.add(AidKind.STRETCH);
        }
        // The rest are done to somebody who cannot help themselves, so your own chart
        // stops here.
        if (patient == null) {
            return out;
        }
        if (config.pressure && state.losingBlood() && !state.stuck()) {
            out.add(AidKind.PRESSURE);
        }
        if (body.downed() && part == BodyPart.HEAD && config.airway) {
            out.add(AidKind.AIRWAY);
            // Under it, because it is the thing to do after opening one and it is the
            // thing that is still doing it after you have gone. Both live on the head
            // for the same reason: that is the part the airway is in.
            out.add(AidKind.ROLL);
        }
        if (body.arrested() && part == BodyPart.THORAX && config.cpr) {
            out.add(AidKind.COMPRESSIONS);
        }
        return out;
    }

    /** Whether any limb on this body has something a pair of hands could do. */
    private boolean anyHands(BodyHealth body) {
        for (BodyPart part : BodyPart.VALUES) {
            if (!actionsFor(part, body).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether this is the thing to be doing right now, for the marker on the figure.
     *
     * <p>A mark that shows all the time is a mark nobody reads when it means something,
     * so a limb is only lit by its own emergency: no air, no heartbeat, a shaft still in
     * it, or blood going onto the floor with nobody holding it.
     */
    private boolean urgent(BodyPart part, BodyHealth body) {
        PartState state = body.part(part);
        if (state.stuck()) {
            return true;
        }
        if (state.losingBlood() && state.bleed == PartState.BLEED_HEAVY && !state.pressed()) {
            return true;
        }
        if (part == BodyPart.THORAX && body.arrested()) {
            return true;
        }
        return part == BodyPart.HEAD && body.airwayBlocked(Config.values().airwaySeconds * 20);
    }

    /**
     * The little list that opens where the cursor is when a limb has more than one
     * answer.
     *
     * <p>It used to be a strip of buttons along the bottom of the panel, which read as a
     * toolbar and meant the player picked the action and the mod picked the limb - the
     * wrong way round for a screen whose whole subject is which part of somebody is
     * wrong. Now the limb is the click and the list only appears when that limb really
     * does offer a choice: a chest with an arrow in it and a heart that has stopped is
     * two different jobs, and which one comes first is the player's call.
     */
    private void drawMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.isEmpty() || menuPart == null) {
            return;
        }
        int width = menuWidth();
        int height = menu.size() * MENU_ROW + 4;
        Paper.sheet(graphics, menuX, menuY, width, height);
        for (int i = 0; i < menu.size(); i++) {
            AidKind kind = menu.get(i);
            int y = menuY + 2 + i * MENU_ROW;
            boolean over = within(mouseX, mouseY, menuX + 2, y, width - 4, MENU_ROW);
            if (over) {
                Paper.highlight(graphics, menuX + 2, y, width - 4, MENU_ROW);
            }
            graphics.drawString(font, kind.title(), menuX + 5, y + 2,
                    over ? Paper.INK : Paper.INK_SOFT, false);
        }
    }

    private int menuWidth() {
        int width = 0;
        for (AidKind kind : menu) {
            width = Math.max(width, font.width(kind.title()));
        }
        return width + 12;
    }

    /** @return true when the click landed on the little list */
    private boolean clickMenu(double mouseX, double mouseY) {
        if (menu.isEmpty() || menuPart == null) {
            return false;
        }
        int width = menuWidth();
        for (int i = 0; i < menu.size(); i++) {
            int y = menuY + 2 + i * MENU_ROW;
            if (within(mouseX, mouseY, menuX + 2, y, width - 4, MENU_ROW)) {
                send(new AidPayload(patientId, menu.get(i), Optional.of(menuPart)));
                closeMenu();
                return true;
            }
        }
        closeMenu();
        return true;
    }

    private void closeMenu() {
        menu = List.of();
        menuPart = null;
    }

    /**
     * A limb was clicked. One thing to do happens; several open the list.
     */
    private boolean handsOn(BodyPart part, BodyHealth body, double mouseX, double mouseY) {
        List<AidKind> options = actionsFor(part, body);
        if (options.isEmpty()) {
            return false;
        }
        if (options.size() == 1) {
            send(new AidPayload(patientId, options.get(0), Optional.of(part)));
            return true;
        }
        menu = options;
        menuPart = part;
        menuX = (int) mouseX + 6;
        menuY = (int) mouseY - 4;
        return true;
    }

    /**
     * What somebody decided this person is, and the four ways of deciding it.
     *
     * @return the tooltip for whatever is under the cursor, or null
     */
    private @Nullable Component tagRow(GuiGraphics graphics, BodyHealth body, int mouseX, int mouseY) {
        if (!Config.values().tags) {
            return null;
        }
        graphics.drawString(font, written(body), left + MARGIN, top + 26, Paper.INK_SOFT, false);

        // Your own chart offers no colours: being sorted is a thing done to you by
        // somebody who walked over and looked. All you may do to a mark is take it off.
        if (patient == null) {
            if (body.tag() == null) {
                return null;
            }
            int x = left + PANEL_WIDTH - MARGIN - TEAR_WIDTH;
            boolean over = within(mouseX, mouseY, x, swatchesTop, TEAR_WIDTH, SWATCH_HEIGHT);
            Paper.box(graphics, x, swatchesTop, TEAR_WIDTH, SWATCH_HEIGHT);
            if (over) {
                Paper.outline(graphics, x, swatchesTop, TEAR_WIDTH, SWATCH_HEIGHT, Paper.INK);
            }
            centred(graphics, "x", x, TEAR_WIDTH, Paper.INK);
            return over ? Component.translatable("triage.screen.tear_off") : null;
        }

        Component tip = null;
        for (TriageTag tag : TriageTag.VALUES) {
            int x = swatchesLeft + tag.ordinal() * (SWATCH_WIDTH + SWATCH_GAP);
            boolean over = within(mouseX, mouseY, x, swatchesTop, SWATCH_WIDTH, SWATCH_HEIGHT);
            // The four colours are the one thing on this sheet that is not ink, because
            // they are not a readout - they are the printed tabs you tear off a real card.
            graphics.fill(x, swatchesTop, x + SWATCH_WIDTH, swatchesTop + SWATCH_HEIGHT, tag.argb());
            Paper.outline(graphics, x, swatchesTop, SWATCH_WIDTH, SWATCH_HEIGHT,
                    body.tag() == tag ? Paper.INK : over ? Paper.INK_SOFT : 0x60000000);
            if (body.tag() == tag) {
                Paper.outline(graphics, x - 2, swatchesTop - 2, SWATCH_WIDTH + 4, SWATCH_HEIGHT + 4, Paper.INK);
            }
            centred(graphics, tag.mark().getString(), x, SWATCH_WIDTH, ink(tag.argb()));
            if (over) {
                tip = tag.title();
            }
        }
        return tip;
    }

    /** The line that says what was written, by whom, and how long ago. */
    private Component written(BodyHealth body) {
        TriageTag tag = body.tag();
        if (tag == null) {
            return Component.translatable("triage.screen.untagged");
        }
        Component age = body.tagStale(Config.values().tagStaleSeconds * 20)
                ? Component.translatable("triage.screen.tag_old")
                : Component.translatable("triage.screen.tag_age", body.tagTicks() / 20);
        return Component.empty()
                .append(tag.mark())
                .append(Component.literal(" "))
                .append(Component.translatable("triage.screen.tag_written", tag.title(), body.tagBy(), age));
    }

    private int helpLeft() {
        return left + MARGIN + 6 + font.width(title);
    }

    private int themeLeft() {
        return helpLeft() + HELP_SIZE + 4;
    }

    private int obsLeft() {
        return themeLeft() + THEME_SIZE + 4;
    }

    /**
     * The switch, in the top right corner of the figure's own box.
     *
     * <p>On the thing it changes rather than up in the header with the rest, because it
     * is not a setting about the chart - it is which side of one body you are looking at,
     * and the place to say that is on the body.
     */
    private int organSwitchLeft() {
        return left + MARGIN - 2 + FIGURE_COLUMN - 8 - ORGAN_SIZE - 3;
    }

    private int organSwitchTop() {
        return top + HEADER + 5;
    }

    /** Draws it, and says whether the cursor is on it. */
    private boolean organSwitch(GuiGraphics graphics, BodyHealth body, int mouseX, int mouseY) {
        int x = organSwitchLeft();
        int y = organSwitchTop();
        boolean over = within(mouseX, mouseY, x, y, ORGAN_SIZE, ORGAN_SIZE);
        Paper.box(graphics, x, y, ORGAN_SIZE, ORGAN_SIZE);
        // The square carries the worst news from inside, so nobody has to open it on
        // spec to find out whether there is anything in there to find.
        boolean anyTorn = false;
        boolean anyHurt = false;
        for (BodyPart part : BodyPart.VALUES) {
            anyTorn |= body.part(part).organTorn();
            anyHurt |= body.part(part).organHurt();
        }
        int ink = anyTorn ? Paper.RED : anyHurt ? Paper.AMBER : Paper.INK_SOFT;
        if (showOrgans) {
            // Showing insides: the pip is a little figure, because that is what the
            // switch would take you back to.
            graphics.fill(x + 5, y + 2, x + 7, y + 4, ink);
            graphics.fill(x + 3, y + 4, x + 9, y + 8, ink);
            graphics.fill(x + 4, y + 8, x + 8, y + 9, ink);
        } else {
            graphics.fill(x + 3, y + 3, x + ORGAN_SIZE - 3, y + ORGAN_SIZE - 3, ink);
            graphics.fill(x + 5, y + 5, x + 6, y + 6, Paper.SHEET);
        }
        Paper.outline(graphics, x, y, ORGAN_SIZE, ORGAN_SIZE, over ? Paper.INK : Paper.INK_SOFT);
        return over;
    }

    /**
     * What one organ is, and what is wrong with it.
     *
     * <p>Including the multiplier when it is torn, because a diagram can say where and
     * cannot say that a spleen empties somebody nearly three times as fast as a gut -
     * and that is the number somebody has to have before choosing which belly to open.
     */
    private void organTooltip(GuiGraphics graphics, BodyHealth body, int mouseX, int mouseY) {
        Organ organ = hoveredOrgan;
        if (organ == null) {
            return;
        }
        int level = body.part(organ.part()).organ(organ);
        List<Component> lines = new ArrayList<>();
        lines.add(organ.title().copy().withStyle(ChatFormatting.WHITE));
        lines.add(organ.part().title().copy().withStyle(ChatFormatting.DARK_GRAY));
        if (level >= PartState.ORGAN_TORN) {
            lines.add(Component.translatable("triage.organs.torn",
                    String.format("%.1f", organ.bleed())).withStyle(ChatFormatting.RED));
            if (organ.spills()) {
                lines.add(Component.translatable("triage.organs.spills").withStyle(ChatFormatting.DARK_GREEN));
            }
            if (organ.lung()) {
                lines.add(Component.translatable("triage.organs.crushes")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            lines.add(Component.translatable("triage.organs.surgeon").withStyle(ChatFormatting.GRAY));
        } else if (level == PartState.ORGAN_BRUISED) {
            lines.add(Component.translatable("triage.organs.bruised").withStyle(ChatFormatting.GOLD));
        } else {
            lines.add(Component.translatable("triage.organs.intact").withStyle(ChatFormatting.DARK_GREEN));
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && within(mouseX, mouseY, themeLeft(), top + 8, THEME_SIZE, THEME_SIZE)) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
            com.claudemods.triage.ClientConfig.setDark(!com.claudemods.triage.ClientConfig.dark);
            return true;
        }
        if (button == 0 && within(mouseX, mouseY, organSwitchLeft(), organSwitchTop(),
                ORGAN_SIZE, ORGAN_SIZE)) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
            showOrgans = !showOrgans;
            return true;
        }
        if (button == 0 && within(mouseX, mouseY, obsLeft(), top + 8, HELP_SIZE, HELP_SIZE)) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
            Minecraft.getInstance().setScreen(new ObsScreen(this,
                    patient != null ? patient : ClientBody.charted(),
                    patientName));
            return true;
        }
        if (button == 0 && within(mouseX, mouseY, helpLeft(), top + 8, HELP_SIZE, HELP_SIZE)) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
            Minecraft.getInstance().setScreen(new GuideScreen(this));
            return true;
        }
        if (button == 0 && !menu.isEmpty()) {
            return clickMenu(mouseX, mouseY);
        }
        if (button == 0) {
            BodyHealth body = patient != null ? patient : ClientBody.charted();
            BodyPart part = BodyFigure.at(figureX, figureY, FIGURE_SCALE, mouseX, mouseY);
            if (part == null) {
                part = rowAt(mouseX, mouseY);
            }
            if (part != null && handsOn(part, body, mouseX, mouseY)) {
                return true;
            }
        }
        if (button == 0 && Config.values().tags) {
            if (patient == null) {
                int x = left + PANEL_WIDTH - MARGIN - 2 - TEAR_WIDTH;
                if (ClientBody.get().tag() != null
                        && within(mouseX, mouseY, x, swatchesTop, TEAR_WIDTH, SWATCH_HEIGHT)) {
                    send(new WriteTagPayload(-1, Optional.empty()));
                    return true;
                }
            } else {
                for (TriageTag tag : TriageTag.VALUES) {
                    int x = swatchesLeft + tag.ordinal() * (SWATCH_WIDTH + SWATCH_GAP);
                    if (within(mouseX, mouseY, x, swatchesTop, SWATCH_WIDTH, SWATCH_HEIGHT)) {
                        send(new WriteTagPayload(patientId, Optional.of(tag)));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void send(Object payload) {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
        ModNetwork.toServer(payload);
    }

    /** One line of the list: name, a bar of what is left, the numbers, then the trouble. */
    private void row(GuiGraphics graphics, BodyHealth body, BodyPart part, int y, boolean lit) {
        PartState state = body.part(part);
        int rowWidth = PANEL_WIDTH - MARGIN * 2 - FIGURE_COLUMN;
        if (lit) {
            Paper.highlight(graphics, listLeft - 4, y - 2, rowWidth + 4, ROW_HEIGHT - 3);
        }
        // Every line on a form sits on a rule, and it is what turns seven rows into a
        // table instead of seven floating labels.
        Paper.rule(graphics, listLeft - 4, y + ROW_HEIGHT - 5, rowWidth + 4);

        graphics.drawString(font, ClientBody.shownTitle(part), listLeft, y + 2,
                state.blacked() ? Paper.RED : Paper.INK, false);

        int barX = listLeft + rowWidth - BAR_WIDTH - 40;
        // In realistic, a part nobody has examined gets a rule rather than a bar. This is
        // the line the whole difficulty is about: an empty row is not a healthy one, and
        // the form says so by refusing to answer rather than by answering zero.
        if (charted(part, state)) {
            Paper.gauge(graphics, barX, y + 2, BAR_WIDTH, 8, ClientBody.shownRatio(state),
                    BodyFigure.fillColour(state));

            // How old the answer is, printed in the ink rather than in a tooltip. A
            // finding from ten minutes ago is a fact about ten minutes ago, and the mod
            // has been saying so in a hover for two versions - which is the one place a
            // person reading a chart in a hurry will never look.
            Component numbers = ClientBody.readout(state, "/");
            graphics.drawString(font, numbers, listLeft + rowWidth - font.width(numbers), y + 2,
                    old(part) ? Paper.INK_FAINT : Paper.INK_SOFT, false);
        } else {
            Paper.rule(graphics, barX, y + 6, BAR_WIDTH);
            Component blank = Component.translatable("triage.screen.unexamined");
            graphics.drawString(font, blank, listLeft + rowWidth - font.width(blank), y + 2,
                    Paper.INK_SOFT, false);
        }

        // The conditions, as pips under the bar - the same colours the figure uses.
        int pipX = barX;
        for (int colour : TreatmentIcons.conditions(state)) {
            graphics.fill(pipX, y + 12, pipX + 5, y + 14, colour);
            pipX += 7;
        }
    }

    /**
     * Whether what is on this row was found out a while ago.
     *
     * <p>Only in realistic, and only for a part somebody actually examined: in arcade
     * nothing is ever found out, so nothing can go out of date. Grey rather than gone -
     * an old reading is still the last thing anybody knows, and throwing it away would
     * punish the one person who did the work.
     */
    private boolean old(BodyPart part) {
        if (!Config.values().realistic) {
            return false;
        }
        BodyHealth body = patient != null ? patient : ClientBody.charted();
        long at = body.examinedAt(part);
        if (at <= 0L) {
            return false;
        }
        return (gameTime() - at) / 20L > Config.values().findingFreshSeconds;
    }

    /**
     * Whether this row is allowed to print a number.
     *
     * <p>Always, in arcade. In realistic, only once somebody has established how bad the
     * part is - or when it is the kind of bad nobody has to establish: an arm that is not
     * there, a limb gone black, one that does not work at all.
     */
    private boolean charted(BodyPart part, PartState state) {
        BodyHealth body = patient != null ? patient : ClientBody.charted();
        return !Config.values().realistic
                || body.found(part, Findings.FLESH)
                || Findings.plain(state, Findings.FLESH);
    }

    /** Blood and illness: the two things that belong to no single part. */
    private void footer(GuiGraphics graphics, BodyHealth body) {
        // The pulse gets its own row, and it is paid for out of the slack above rather
        // than by pushing everything below it down: the lines under here are the ones
        // with clocks on them, and shoving an arrest countdown into the hint at the
        // bottom of the card would cost more than the row is worth.
        int pulseRoom = hasVitals() ? 12 : 0;
        int y = top + PANEL_HEIGHT - 52 - pulseRoom;
        Paper.heavyRule(graphics, left + MARGIN, y - 8, PANEL_WIDTH - MARGIN * 2);

        Component label = Component.translatable("triage.screen.blood");
        graphics.drawString(font, label, left + MARGIN, y, Paper.INK, false);
        int barX = left + MARGIN + font.width(label) + 6;
        int barWidth = 120;
        Paper.gauge(graphics, barX, y - 1, barWidth, 8, ClientBody.shownBlood(body), Paper.RED);
        Component percent = ClientBody.bloodReadout(body);
        graphics.drawString(font, percent, barX + barWidth + 6, y, Paper.INK_SOFT, false);

        // Which blood this body has, right-aligned so it never runs into the bar. Not
        // hidden by a concussion: it is something you know about yourself, not something
        // you are reading off yourself - and it is the one fact somebody standing over
        // you with a bag needs, so burying it would only make transfusion a coin toss.
        BloodType type = body.bloodType();
        if (type != null) {
            Component group = Component.translatable("triage.screen.blood_type", type.title());
            graphics.drawString(font, group,
                    left + PANEL_WIDTH - MARGIN - font.width(group), y, Paper.INK_SOFT, false);
        }

        // What the heart is doing, under the volume it is pushing round - because the
        // two are one reading. A body holds its pressure up by beating faster as it
        // loses, so the pair together say more than either does: half the volume and a
        // steady seventy is a body that has stopped compensating, and that is worse
        // news than the fast one beside it.
        vitals(graphics, body, y + 12);

        int line = y + 14 + pulseRoom;
        // How long they have got, above everything else that is wrong with them, because
        // it is the only line here with a deadline on it. Until now the number lived
        // only on the patient's own screen, which left "how bad is it" a question
        // nobody standing over them could answer - and that is the question.
        if (body.arrested()) {
            // No clock of their own left to read. What this number counts down is
            // somebody else - the last round of compressions somebody gave them - and
            // it is the only line on this screen that is about the reader.
            graphics.drawString(font, Component.translatable("triage.screen.arrest", body.arrestTicks() / 20),
                    left + MARGIN, line, Paper.RED_DEEP, false);
            line += 10;
        } else if (body.downed()) {
            Component clock = body.stabilised()
                    ? Component.translatable("triage.screen.held",
                            body.stabilisedTicks() / 20, body.downedTicks() / 20)
                    : Component.translatable("triage.screen.downed", body.downedTicks() / 20);
            graphics.drawString(font, clock, left + MARGIN, line, Paper.RED, false);
            line += 10;
        }
        // Not breathing is neither a wound nor an illness, so it has nowhere else to be
        // written - and it belongs beside the clock, because it is what is emptying it.
        if (Config.values().airway && body.airwayBlocked(Config.values().airwaySeconds * 20)) {
            graphics.drawString(font, Component.translatable("triage.screen.airway_blocked"),
                    left + MARGIN, line, Paper.RED, false);
            line += 10;
        }
        // Illness in plain ink with a pip for how bad it is. The chat colours these
        // carry are built for a black screen: on paper an aqua "cold" is a smudge.
        // A body can carry more news than there is paper. The list stops at the last
        // line that fits rather than writing over the bottom of the card, and the ones
        // it drops are the mildest: severity is the order they are declared in.
        for (Disease disease : Disease.VALUES) {
            if (body.has(disease) && line <= hintTop() - 12) {
                graphics.fill(left + MARGIN, line + 2, left + MARGIN + 4, line + 6,
                        disease.severity() >= 2 ? Paper.RED
                                : disease.severity() == 1 ? Paper.AMBER : Paper.INK_SOFT);
                graphics.drawString(font, disease.title(), left + MARGIN + 8, line, Paper.INK, false);
                line += 10;
            }
        }
        // Above the painkillers, because it is the bigger fact about the person: one of
        // them is somebody not feeling a wound and the other is somebody not being here.
        if (body.under() && line <= hintTop() - 12) {
            graphics.drawString(font, Component.translatable("triage.screen.under", body.underTicks() / 20),
                    left + MARGIN, line, Paper.BLUE, false);
            line += 10;
        }
        if (body.painkilled() && line <= hintTop() - 12) {
            graphics.drawString(font, Component.translatable("triage.hud.painkiller", body.painkillerTicks() / 20),
                    left + MARGIN, line, Paper.BLUE, false);
        }
        // How much the next course will be worth is worth knowing before you need it.
        if (body.painkillerTolerance() > 0) {
            Component tolerance = Component.translatable("triage.screen.tolerance", body.painkillerTolerance());
            graphics.drawString(font, tolerance,
                    left + PANEL_WIDTH - MARGIN - font.width(tolerance), line, Paper.INK_SOFT, false);
        }
    }

    /**
     * A pip beside every limb a bare hand could do something to.
     *
     * <p>Small, and only where there is something: the chart is a diagnosis and this is
     * the one thing on it that is an offer.
     */
    private void hands(GuiGraphics graphics, BodyHealth body) {
        boolean any = false;
        for (BodyPart part : BodyPart.VALUES) {
            if (actionsFor(part, body).isEmpty()) {
                continue;
            }
            any = true;
            int y = listTop + part.ordinal() * ROW_HEIGHT;
            graphics.fill(listLeft - 6, y + 3, listLeft - 2, y + 7,
                    urgent(part, body) ? Paper.RED : Paper.GREEN);
        }
        if (any) {
            Component hint = Component.translatable("triage.screen.hands_hint");
            graphics.drawString(font, hint,
                    left + PANEL_WIDTH - MARGIN - font.width(hint), hintTop(), Paper.INK_SOFT, false);
        }
    }

    /** The line the hint sits on, which is also the floor the conditions stop above. */

    /** Whether there is a vitals row to print at all. */
    private boolean hasVitals() {
        Config.Values config = Config.values();
        return config.pulse || config.breathing || config.temperature;
    }

    /**
     * Pulse, breathing and temperature on one line, the way a chart carries them.
     *
     * <p>Together rather than stacked, because they are read together and because three
     * rows would have pushed the clocks below them off the bottom of the card. They also
     * only say what is worth saying: a chest doing its job and a normal temperature print
     * their number and nothing else, so the words on this line are always the news.
     */
    /**
     * How long a reading stays news.
     *
     * <p>Not a cliff for the sake of one: a pulse from four minutes ago is a fact about
     * four minutes ago, and on a patient who is bleeding that is the difference between
     * a number and a lie. Past this the chart keeps the figure and says how old it is,
     * which is what a real one does.
     */
    private static final long STALE_SECONDS = 45L;
    /**
     * And how long a temperature stays worth printing.
     *
     * <p>Longer than a pulse by a distance, because it is a slower fact: a heart answers
     * a bleed in seconds and a temperature takes an hour to mean anything new.
     */
    private static final long STALE_TEMP = 240L;

    private static long gameTime() {
        return Minecraft.getInstance().level == null ? 0L
                : Minecraft.getInstance().level.getGameTime();
    }

    private void vitals(GuiGraphics graphics, BodyHealth body, int y) {
        Config.Values config = Config.values();
        if (!hasVitals()) {
            return;
        }
        int x = left + MARGIN;
        if (config.pulse) {
            // What is on the notes, not what is true.
            //
            // The chart used to print a live pulse for anybody it was opened over, which
            // made the taking of one worth nothing: walk up to a stranger and the screen
            // handed you a heart rate nobody had ever felt for. A chart is a record of
            // what somebody found out, and a record of something nobody has done yet is
            // an empty line.
            Pulse reading = body.notedPulse();
            long age = reading == null ? 0L
                    : Math.max(0L, gameTime() - body.pulseAt()) / 20L;
            boolean stale = age > STALE_SECONDS;
            Component value = reading == null
                    ? Component.translatable("triage.screen.not_taken")
                    : reading.character() == Pulse.ABSENT
                            ? Component.translatable("triage.pulse.absent")
                            : Component.translatable(stale ? "triage.screen.pulse_stale"
                                    : "triage.screen.pulse_value",
                                    reading.rate(), stale ? age : reading.characterTitle());
            x = entry(graphics, x, y, Component.translatable("triage.screen.pulse"), value,
                    reading == null ? Paper.INK_FAINT
                            : stale ? Paper.INK_SOFT
                            : reading.character() == Pulse.ABSENT ? Paper.RED_DEEP
                            : reading.character() == Pulse.THREADY ? Paper.RED
                            : reading.character() == Pulse.WEAK ? Paper.AMBER : Paper.INK_SOFT);
            x = direction(graphics, x, y, body, Reading.PULSE);
        }
        if (config.breathing) {
            Breathing air = Breathing.of(body, config.airwaySeconds);
            Component value = air.character() == Breathing.ABSENT
                    ? Component.translatable("triage.breathing.absent")
                    : air.character() == Breathing.EASY
                            ? Component.literal(String.valueOf(air.rate()))
                            : Component.translatable("triage.screen.breath_value",
                                    air.rate(), air.characterTitle());
            x = entry(graphics, x, y, Component.translatable("triage.screen.breath"), value,
                    air.character() == Breathing.ABSENT ? Paper.RED_DEEP
                            : air.character() == Breathing.SHALLOW
                                    || air.character() == Breathing.SLOW ? Paper.RED
                            : air.character() == Breathing.LABOURED ? Paper.AMBER : Paper.INK_SOFT);
        }
        if (config.temperature) {
            Temperature warmth = Temperature.of(body);
            // The word off a hand, or the figure off an instrument. A chart that printed
            // a decimal nobody had measured was the mod telling a small lie every time it
            // was opened - and now that a measured one is written down, the chart can
            // print the real thing whenever somebody has actually taken it.
            long tempAge = Math.max(0L, gameTime() - body.tempAt()) / 20L;
            boolean measured = config.thermometer && body.notedTemp() > 0 && tempAge <= STALE_TEMP;
            Component value = measured
                    ? Component.literal(Temperature.readout(body.notedTemp()))
                    : config.thermometer ? warmth.bandTitle() : Component.literal(warmth.readout());
            x = entry(graphics, x, y, Component.translatable("triage.screen.temp"), value,
                    warmth.band() == Temperature.HIGH ? Paper.RED
                            : warmth.band() == Temperature.RAISED ? Paper.AMBER
                            : warmth.band() == Temperature.LOW ? Paper.BLUE : Paper.INK_SOFT);
            if (measured) {
                direction(graphics, x, y, body, Reading.TEMP);
            }
        }
    }

    /**
     * The arrow, which is the only thing on this row that is not a measurement.
     *
     * <p>Nothing at all until somebody has taken the same reading twice, and nothing when
     * it has not moved enough to mean anything. A chart that prints an arrow beside a
     * single reading is a chart pretending to know a direction, and that is the one lie
     * this screen has spent three versions removing.
     */
    private int direction(GuiGraphics graphics, int x, int y, BodyHealth body, int kind) {
        Trend trend = Trend.of(body, kind);
        if (trend == null || !trend.worthSaying(kind)) {
            return x;
        }
        Component arrow = Component.literal(trend.arrow());
        int colour = trend.colour(kind) == net.minecraft.ChatFormatting.GREEN
                ? Paper.GREEN : Paper.AMBER;
        graphics.drawString(font, arrow, x - 4, y, colour, false);
        return x + font.width(arrow) + 2;
    }

    /** One labelled reading, returning where the next one starts. */
    private int entry(GuiGraphics graphics, int x, int y, Component label, Component value, int colour) {
        graphics.drawString(font, label, x, y, Paper.INK, false);
        int valueX = x + font.width(label) + 4;
        graphics.drawString(font, value, valueX, y, colour, false);
        return valueX + font.width(value) + 12;
    }

    private int hintTop() {
        return top + PANEL_HEIGHT - MARGIN - 8;
    }

    private @Nullable BodyPart rowAt(double mouseX, double mouseY) {
        int rowWidth = PANEL_WIDTH - MARGIN * 2 - FIGURE_COLUMN;
        if (mouseX < listLeft - 2 || mouseX > listLeft + rowWidth) {
            return null;
        }
        int index = (int) ((mouseY - listTop + 2) / ROW_HEIGHT);
        return index >= 0 && index < BodyPart.VALUES.length ? BodyPart.VALUES[index] : null;
    }

    private static boolean within(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void border(GuiGraphics graphics, int x, int y, int width, int height, int colour) {
        graphics.fill(x, y, x + width, y + 1, colour);
        graphics.fill(x, y + height - 1, x + width, y + height, colour);
        graphics.fill(x, y, x + 1, y + height, colour);
        graphics.fill(x + width - 1, y, x + width, y + height, colour);
    }

    private void centred(GuiGraphics graphics, String text, int x, int width, int colour) {
        graphics.drawString(font, text, x + (width - font.width(text)) / 2, swatchesTop + 3, colour, false);
    }

    /** Ink that can be read on that swatch: dark on the bright ones, light on the black. */
    private static int ink(int argb) {
        int luma = (((argb >> 16) & 0xFF) * 30 + ((argb >> 8) & 0xFF) * 59 + (argb & 0xFF) * 11) / 100;
        return luma > 110 ? 0xFF15121C : Paper.INK;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
