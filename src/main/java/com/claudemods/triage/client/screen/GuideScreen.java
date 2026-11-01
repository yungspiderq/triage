package com.claudemods.triage.client.screen;

import com.claudemods.triage.Config;
import com.claudemods.triage.body.AidKind;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.Organ;
import com.claudemods.triage.body.PartState;
import com.claudemods.triage.body.Pulse;
import com.claudemods.triage.med.MedicineKind;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * What to do about it.
 *
 * <p>The mod has thirteen things that can be wrong with a limb, twelve items, three
 * things you can do with empty hands, and no way at all of finding any of that out
 * except by being hurt and guessing. An arm that says "Bleeding" is a diagnosis, and a
 * diagnosis is only half of what a player standing there needs.
 *
 * <p>The answers are not written down here. They are <em>asked for</em>: every entry
 * about a limb builds the state it describes and puts it to
 * {@link MedicineKind#canTreat}, which is the same method the treatment screen uses to
 * grey out the parts not worth treating. A guide that repeated the rules in prose would
 * be wrong within two versions - the fracture entry would still be recommending a splint
 * for a skull long after that stopped working. This one cannot say that, because it does
 * not know it: it knows how to ask.
 */
public final class GuideScreen extends Screen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 214;
    private static final int MARGIN = 8;
    private static final int HEADER = 24;
    private static final int LIST_WIDTH = 116;
    private static final int ROW_HEIGHT = 13;


    /**
     * One page of the guide.
     *
     * <p>The prose says what the thing is and what it does to you. What closes it is
     * never prose - it is whatever {@code answers} finds by asking the code, so the two
     * halves of a page can never disagree with each other.
     */
    private record Entry(String id, @Nullable ConditionIcon icon, List<Component> answers,
                         List<Component> free) {
    }

    /**
     * A heading, and the pages under it.
     *
     * <p>The list was flat and forty-eight long, in the order the pages were written over
     * eight versions - which is the order they happened to the mod rather than any order a
     * reader has. The page about a strap sat nineteen rows from the page about bleeding.
     *
     * <p>One open at a time. That is not a space saving, it is the point: a reader looking
     * for what to do about a burn wants the four burn pages and not the other forty-four,
     * and a list that shows them everything is a list that shows them nothing.
     */
    private record Section(String id, List<Entry> pages) {
    }

    /** One row of the list, which is either a heading or a page under the open one. */
    private record Line(@Nullable Section section, @Nullable Entry entry, int index) {
    }

    private final List<Section> sections = build();
    private final List<Entry> entries = flatten(sections);
    private final @Nullable Screen parent;
    /** Which heading is open. One, always: a guide with everything open is the old list. */
    private int open;
    private int selected;
    /** First row drawn. There are more pages than there is room for them. */
    private int scroll;

    private int left;
    private int top;

    public GuideScreen(@Nullable Screen parent) {
        super(Component.translatable("triage.guide.title"));
        this.parent = parent;
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

    // ---- the pages --------------------------------------------------------

    /**
     * The twelve headings, and what lives under each.
     *
     * <p>Grouped by what a reader is looking at rather than by when it was written. The
     * order inside a heading is the order the thing happens: something bleeds before
     * anybody straps it, and a strap kills a limb before anybody saws it off.
     */
    private static List<Section> build() {
        List<Section> out = new ArrayList<>();

        // What is coming out, and everything that is about stopping it.
        out.add(new Section("bleeding", List.of(
                limb("bleeding", ConditionIcon.HEAVY_BLEEDING,
                        state -> state.startBleeding(PartState.BLEED_HEAVY, 0), AidKind.PRESSURE),
                limb("artery", ConditionIcon.ARTERY,
                        state -> state.startBleeding(PartState.BLEED_ARTERIAL, 0), AidKind.PRESSURE),
                arrowPage(),
                bulletPage(),
                limb("tourniquet", ConditionIcon.TOURNIQUET, state -> {
                    state.startBleeding(PartState.BLEED_HEAVY, 0);
                    state.tourniquet = true;
                }, null),
                whole("blood_loss", MedicineKind.TRANSFUSION),
                whole("trail"),
                whole("reperfusion"))));

        // Bone, in the order it gives way.
        out.add(new Section("bones", List.of(
                limb("crack", ConditionIcon.CRACK, PartState::crack, null),
                limb("fracture", ConditionIcon.FRACTURE, state -> state.fracture(1), null),
                limb("displaced", ConditionIcon.DISPLACED, state -> {
                    state.fracture(1);
                    state.displaced = true;
                }, AidKind.SET),
                limb("set", ConditionIcon.DISPLACED, state -> {
                    state.fracture(1);
                    state.displaced = true;
                }, AidKind.SET),
                limb("open_bone", ConditionIcon.OPEN_BONE, state -> {
                    state.fracture(1);
                    state.displaced = true;
                    state.openBreak = true;
                    state.startBleeding(PartState.BLEED_HEAVY, 0);
                }, null),
                limb("splint_worn", ConditionIcon.SPLINT, state -> {
                    state.crack();
                    state.setSplint(Config.values().splintSeconds * 20);
                }, null),
                limb("crooked", ConditionIcon.CROOKED, state -> state.crooked = 1, null),
                head("skull", ConditionIcon.FRACTURE, state -> state.fracture(1)))));

        // An open wound, and the road it goes down if nobody closes it.
        out.add(new Section("wounds", List.of(
                // Beside the arrow, because it is the same sentence with a different
                // object in it, and the answer is the same pair of hands.
                free("blade", AidKind.PULL),
                limb("dressing", ConditionIcon.SOILED, state -> {
                    state.dress();
                    state.dressedTicks = Integer.MAX_VALUE / 2;
                    state.health = state.max - 1.0F;
                }, null),
                limb("infection", ConditionIcon.INFECTION,
                        state -> state.infect(PartState.INFECTION_MILD), null),
                limb("blacked", ConditionIcon.BLACKED, state -> state.health = 0.0F, null),
                whole("theatre"),
                whole("mending"))));

        // Fire, inside and out.
        out.add(new Section("burns", List.of(
                limb("burn", ConditionIcon.BURN,
                        state -> state.burn(PartState.BURN_PARTIAL, 0), null),
                free("touch", AidKind.TOUCH),
                whole("smoke"),
                whole("chill"))));

        // What is under the ribs, and what goes wrong with it.
        out.add(new Section("inside", List.of(
                chest("pneumo", ConditionIcon.PNEUMO, state -> state.pneumo = true),
                chest("organ", ConditionIcon.ORGAN,
                        state -> state.tearOrgan(Organ.HEART, PartState.ORGAN_TORN)),
                whole("organ_illness"),
                whole("referred"))));

        // Things caught rather than done to somebody.
        out.add(new Section("illness", List.of(
                whole("illness", MedicineKind.ANTIBIOTICS),
                whole("catching"),
                whole("virus"),
                whole("parasites"),
                whole("rabies"),
                limb("clot", ConditionIcon.CLOT, state -> state.clot = true, null),
                whole("embolism", MedicineKind.ANTICOAGULANT),
                // The only illness in this section that is not about dirt but about the
                // shape of the hole, and the only one with a deadline nobody is shown.
                whole("tetanus", MedicineKind.ANTIBIOTICS),
                // Listed under the instrument rather than under the bottle, because the
                // whole difficulty of this one is the reading, not the cure.
                whole("relapse", MedicineKind.THERMOMETER))));

        // A limb that is going, gone, or back on.
        out.add(new Section("limbs", List.of(
                limb("dead", ConditionIcon.DEAD, PartState::die, null),
                limb("missing", ConditionIcon.MISSING, PartState::amputate, null),
                whole("sever"),
                whole("graft"),
                limb("prosthetic", ConditionIcon.PROSTHETIC, state -> {
                    state.amputate();
                    state.prosthetic = true;
                }, null),
                limb("wear", ConditionIcon.WORN, state -> {
                    state.amputate();
                    state.prosthetic = true;
                    state.wear = Integer.MAX_VALUE / 2;
                }, null))));

        // Somebody on the floor, in the order the floor kills them.
        out.add(new Section("downed", List.of(
                whole("faint"),
                free("downed", AidKind.AIRWAY),
                free("arrest", AidKind.COMPRESSIONS),
                free("roll", AidKind.ROLL),
                whole("shock"),
                whole("carry"))));

        // What a body will tell you, and what it takes to hear it.
        out.add(new Section("readings", List.of(
                // First of the readings, because it is the rule the rest of them obey:
                // anything measured once is a number and anything measured twice is news.
                whole("obs"),
                free("pulse", AidKind.PULSE),
                whole("breathing"),
                whole("temperature"),
                whole("thermometer"),
                free("ask", AidKind.ASK),
                free("score", AidKind.SCORE),
                // The two the second difficulty is built on: the only way to a bone
                // without a machine, and the only way into a chest at all.
                free("survey", AidKind.SURVEY),
                free("feel", AidKind.FEEL),
                // Before the instrument, because it happens before the instrument: this
                // is the one finding that arrives while you are still walking over.
                whole("earshot"),
                whole("listen", MedicineKind.STETHOSCOPE),
                whole("film"),
                whole("notes"))));

        // What is in the bag.
        out.add(new Section("medicine", List.of(
                whole("pain", MedicineKind.PAINKILLER),
                whole("syringes"),
                whole("anaesthetic", MedicineKind.ANAESTHETIC),
                // Directly under the ether, because it is the reason the ether is there.
                whole("panic", MedicineKind.ANAESTHETIC),
                whole("allergy"),
                free("unwrap", AidKind.UNWRAP),
                free("unstrap", AidKind.UNSTRAP),
                // Directly after the page about taking a strap off, because it is the
                // half of that job the last page does not mention.
                free("crush", AidKind.UNSTRAP))));

        // The head, which is the part that stops reporting on itself.
        out.add(new Section("head", List.of(
                whole("concussion"),
                whole("deafness"),
                whole("blast"),
                whole("adrenaline"))));

        // And what the work costs whoever is doing it.
        out.add(new Section("yourself", List.of(
                // First page of the last section on purpose: it is the one that changes
                // what every other page in the book means.
                whole("difficulty"),
                whole("sight"),
                whole("assist"),
                whole("fatigue"),
                whole("sleepless"),
                whole("hypothermia"),
                whole("tags"))));

        // The six operations. Their own heading rather than scattered through the
        // sections they belong to, because what a player needs when they are holding the
        // kit is the list of what it does - and until this version that list was one
        // line long.
        out.add(new Section("operating", List.of(
                whole("operations", MedicineKind.SURGERY),
                limb("op_irrigate", ConditionIcon.OPEN_BONE, state -> {
                    state.fracture(1);
                    state.displaced = true;
                    state.openBreak = true;
                }, null),
                head("op_trephine", ConditionIcon.FRACTURE, state -> state.fracture(1)),
                chest("op_organ", ConditionIcon.ORGAN,
                        state -> state.tearOrgan(Organ.HEART, PartState.ORGAN_TORN)),
                limb("op_excise", ConditionIcon.BURN_DEEP,
                        state -> state.burn(PartState.BURN_FULL, 0), null),
                limb("op_debride", ConditionIcon.BLACKED, state -> state.health = 0.0F, null),
                limb("op_osteotomy", ConditionIcon.CROOKED, state -> state.crooked = 1, null))));

        // Last, and last on purpose: everything above it is something happening, and
        // this is what is still true afterwards.
        out.add(new Section("marks", List.of(
                limb("scar", ConditionIcon.SCAR, state -> {
                    state.scarred(0.4F);
                    state.knitted();
                }, null),
                whole("ache"),
                limb("stiff", ConditionIcon.SCAR, state -> state.scarred(0.7F), AidKind.STRETCH),
                free("stretch", AidKind.STRETCH))));
        return out;
    }

    /**
     * The one page where the answer is not an item at all.
     *
     * <p>Built the same way as the rest - the state is made and the question asked - so
     * the empty list under "what answers it" is the mod's own answer rather than a
     * decision taken while writing this.
     */
    private static Entry arrowPage() {
        return new Entry("arrow", ConditionIcon.ARROW,
                answers(BodyPart.LEFT_ARM, state -> {
                    state.arrows = 1;
                    state.startBleeding(PartState.BLEED_LIGHT, 0);
                }),
                List.of(AidKind.PULL.title()));
    }

    /** Under the skin rather than standing out of it, which changes the way out. */
    private static Entry bulletPage() {
        return new Entry("bullet", ConditionIcon.BULLET,
                answers(BodyPart.LEFT_ARM, state -> {
                    // A rifle round rather than the generic one: the page is about the
                    // difference in shape, and the blunt pistol stub every old screenshot
                    // showed is the one calibre a player has already seen.
                    state.setBullets(1, "tacz:762x39");
                    state.startBleeding(PartState.BLEED_LIGHT, 0);
                }),
                List.of(AidKind.PULL.title()));
    }

    private static Entry limb(String id, ConditionIcon icon, Consumer<PartState> shape,
                              @Nullable AidKind hands) {
        return new Entry(id, icon, answers(BodyPart.LEFT_ARM, shape), hands == null ? List.of()
                : List.of(hands.title()));
    }

    private static Entry head(String id, ConditionIcon icon, Consumer<PartState> shape) {
        return new Entry(id, icon, answers(BodyPart.HEAD, shape), List.of());
    }

    private static Entry chest(String id, ConditionIcon icon, Consumer<PartState> shape) {
        return new Entry(id, icon, answers(BodyPart.THORAX, shape), List.of());
    }

    private static Entry whole(String id, MedicineKind... kinds) {
        List<Component> answers = new ArrayList<>(kinds.length);
        for (MedicineKind kind : kinds) {
            answers.add(kind.title());
        }
        return new Entry(id, null, answers, List.of());
    }

    private static Entry free(String id, AidKind... kinds) {
        List<Component> hands = new ArrayList<>(kinds.length);
        for (AidKind kind : kinds) {
            hands.add(kind.title());
        }
        return new Entry(id, null, List.of(), hands);
    }

    /** Asks the mod, rather than telling the player. */
    private static List<Component> answers(BodyPart part, Consumer<PartState> shape) {
        PartState state = PartState.full(part);
        shape.accept(state);
        List<Component> out = new ArrayList<>(3);
        for (MedicineKind kind : MedicineKind.VALUES) {
            if (kind.canTreat(part, state)) {
                out.add(kind.title());
            }
        }
        return out;
    }

    // ---- drawing ----------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Paper.sync();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        Paper.sheet(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawString(font, title, left + MARGIN, top + 9, Paper.INK, false);
        Paper.heavyRule(graphics, left + MARGIN, top + HEADER - 6, PANEL_WIDTH - MARGIN * 2);

        int listLeft = left + MARGIN;
        int listTop = top + HEADER;
        int visible = visibleRows();
        List<Line> lines = lines();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - visible)));
        for (int row = 0; row < visible; row++) {
            int i = scroll + row;
            if (i >= lines.size()) {
                break;
            }
            Line line = lines.get(i);
            int y = listTop + row * ROW_HEIGHT;
            boolean over = mouseX >= listLeft && mouseX < listLeft + LIST_WIDTH
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (line.section() != null) {
                // A heading. Marked open or shut rather than drawn as a button, because
                // the whole card is paper and paper does not have buttons on it.
                boolean opened = sections.indexOf(line.section()) == open;
                if (over) {
                    Paper.highlight(graphics, listLeft - 2, y - 1, LIST_WIDTH + 2, ROW_HEIGHT - 1);
                }
                graphics.drawString(font, Component.literal(opened ? "- " : "+ ")
                                .append(Component.translatable("triage.guide.section." + line.section().id())),
                        listLeft, y + 1, Paper.INK, false);
                continue;
            }
            Entry entry = line.entry();
            boolean chosen = line.index() == selected;
            if (chosen || over) {
                Paper.highlight(graphics, listLeft - 2, y - 1, LIST_WIDTH + 2, ROW_HEIGHT - 1);
            }
            if (entry.icon() != null) {
                entry.icon().draw(graphics, listLeft + 6, y + 1);
            }
            // Cut to the column. A title longer than the list used to be drawn straight
            // over the page beside it, which is the sort of thing that only shows up in
            // the language whose words are longest - so it is clipped rather than
            // trusted to be short.
            graphics.drawString(font, clipped(Component.translatable("triage.guide." + entry.id()),
                            LIST_WIDTH - 19),
                    listLeft + 17, y + 1, chosen ? Paper.INK : Paper.INK_SOFT, false);
        }
        // A hint that the list goes on, rather than a scrollbar: there is no room for
        // one and the wheel is what anybody tries first anyway.
        if (scroll + visible < lines.size()) {
            graphics.drawString(font, Component.literal("..."), listLeft + 4,
                    listTop + visible * ROW_HEIGHT - 2, Paper.INK_SOFT, false);
        }

        int paneLeft = listLeft + LIST_WIDTH + 6;
        graphics.fill(paneLeft - 4, top + HEADER, paneLeft - 3, top + PANEL_HEIGHT - MARGIN, Paper.INK_FAINT);
        detail(graphics, entries.get(selected), paneLeft, listTop,
                PANEL_WIDTH - MARGIN * 2 - LIST_WIDTH - 6);
    }

    /** One drawn line of the page, so it can be laid out before anything is painted. */
    private record Row(FormattedCharSequence text, int indent, int colour, int after) {
    }

    /**
     * The page beside the list.
     *
     * <p>Laid out into rows first and painted second, because a page that is longer than
     * the paper has to know that before it starts. What used to happen instead was that a
     * long entry simply ran off the bottom of the card and the rest of it did not exist.
     */
    private void detail(GuiGraphics graphics, Entry entry, int x, int y, int width) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(Component.translatable("triage.guide." + entry.id()).getVisualOrderText(),
                0, Paper.INK, 4));
        for (FormattedCharSequence row
                : font.split(Component.translatable("triage.guide." + entry.id() + ".body"), width)) {
            rows.add(new Row(row, 0, Paper.INK_SOFT, 0));
        }
        if (!entry.free().isEmpty()) {
            rows.add(new Row(Component.translatable("triage.guide.hands").getVisualOrderText(),
                    0, Paper.INK, 1));
            for (Component hands : entry.free()) {
                rows.add(new Row(Component.literal("- ").append(hands).getVisualOrderText(),
                        4, Paper.GREEN, 0));
            }
            rows.add(new Row(null, 0, 0, 0));
        }
        if (!entry.answers().isEmpty()) {
            rows.add(new Row(Component.translatable("triage.guide.answers").getVisualOrderText(),
                    0, Paper.INK, 1));
            for (Component answer : entry.answers()) {
                rows.add(new Row(Component.literal("- ").append(answer).getVisualOrderText(),
                        4, Paper.BLUE, 0));
            }
        } else if (entry.free().isEmpty()) {
            rows.add(new Row(Component.translatable("triage.guide.nothing").getVisualOrderText(),
                    0, Paper.INK_SOFT, 0));
        }

        int room = top + PANEL_HEIGHT - MARGIN - 4 - y;
        int fits = Math.max(1, room / 10);
        pageRows = rows.size();
        pageFits = fits;
        page = Math.max(0, Math.min(page, rows.size() - fits));

        int line = y;
        // The rule under the title only when the title is the row being shown, so a
        // scrolled page does not carry a stray line across the middle of a sentence.
        if (page == 0) {
            Paper.rule(graphics, x, y + 10, width);
        }
        for (int i = page; i < rows.size() && line < y + fits * 10; i++) {
            Row row = rows.get(i);
            if (row.text() != null) {
                graphics.drawString(font, row.text(), x + row.indent(), line, row.colour(), false);
            }
            line += 10 + row.after();
            if (i == 0 && page == 0) {
                line += 4;
            }
        }
        if (page + fits < rows.size()) {
            graphics.drawString(font, Component.literal("..."), x,
                    y + fits * 10 - 2, Paper.INK_SOFT, false);
        }
        // The offer, on the last line of the card rather than in the text: it is a thing
        // to do, and things to do do not belong in the middle of a paragraph.
        tryLine = practice(entry.id()) == null ? null
                : Component.translatable("triage.guide.try");
        if (tryLine != null) {
            tryY = top + PANEL_HEIGHT - MARGIN - 9;
            graphics.drawString(font, tryLine, x, tryY, Paper.GREEN, false);
            tryX = x;
            tryWidth = font.width(tryLine);
        }
    }

    /** A title cut to the width it has, with the ellipsis the cut deserves. */
    private FormattedCharSequence clipped(Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        int room = width - font.width("...");
        return Component.literal(font.plainSubstrByWidth(text.getString(), room) + "...")
                .getVisualOrderText();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int listLeft = left + MARGIN;
            int row = (int) ((mouseY - (top + HEADER) + 1) / ROW_HEIGHT);
            List<Line> lines = lines();
            int index = scroll + row;
            if (mouseX >= listLeft - 2 && mouseX < listLeft + LIST_WIDTH
                    && row >= 0 && row < visibleRows() && index < lines.size()) {
                Line line = lines.get(index);
                if (line.section() != null) {
                    // Opening a heading closes the last one. Shutting the open one leaves
                    // every heading shut, which is the view worth having when you have
                    // lost your place: twelve rows and nothing else.
                    int at = sections.indexOf(line.section());
                    open = open == at ? -1 : at;
                    scroll = 0;
                    return true;
                }
                selected = line.index();
                // A new page starts at its top, not wherever the last one was left.
                page = 0;
                return true;
            }
        }
        if (button == 0 && tryLine != null && minecraft != null
                && mouseX >= tryX && mouseX < tryX + tryWidth
                && mouseY >= tryY - 1 && mouseY < tryY + 9) {
            Entry entry = entries.get(selected);
            PracticeScreen.Builder builder = practice(entry.id());
            if (builder != null) {
                minecraft.setScreen(new PracticeScreen(this,
                        Component.translatable("triage.guide." + entry.id()), builder));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (scrollY != 0) {
            // Whichever half the cursor is over. Two lists on one card need two wheels,
            // and the one under the hand is the one that was meant.
            if (mouseX >= left + MARGIN + LIST_WIDTH) {
                page = Math.max(0, Math.min(page - (int) Math.signum(scrollY),
                        Math.max(0, pageRows - pageFits)));
                return true;
            }
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY),
                    Math.max(0, lines().size() - visibleRows())));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    /** Where the offer to run the thing was drawn, so a click can find it. */
    private @Nullable Component tryLine;
    private int tryX;
    private int tryY;
    private int tryWidth;

    /** How far down the open page is, and what it takes to fill the card. */
    private int page;
    private int pageRows;
    private int pageFits = 1;

    private int visibleRows() {
        return Math.max(1, (PANEL_HEIGHT - HEADER - MARGIN) / ROW_HEIGHT);
    }

    /**
     * The procedure a page describes, when the page describes one.
     *
     * <p>Every one of these is learnt in the worst place there is: the first saw anybody
     * holds is over a friend who is losing the leg anyway. A page that says a needle wants
     * blood in the hub has said a true thing, and reading it is not the same as having done
     * it once.
     *
     * <p>So the page runs it, over nobody. Matched by page rather than declared on the
     * entry, because the pages and the games were written years apart and the list of which
     * belongs to which is the sort of thing that should live in one place and be short.
     */
    private static @Nullable PracticeScreen.Builder practice(String id) {
        return switch (id) {
            // The three examinations, which are the only games here whose value is a fact
            // rather than a repair - and the only ones a manikin teaches honestly, since a
            // manikin has nothing wrong with it and these are all about the hands.
            case "survey" -> (x, y, w, h) -> new SurveyMinigame(x, y, w, h, false);
            case "feel" -> (x, y, w, h) -> new FeelMinigame(x, y, w, h, false);
            case "listen" -> (x, y, w, h) -> new ListenMinigame(x, y, w, h, false);
            case "bleeding" -> (x, y, w, h) -> new PressMinigame(x, y, w, h);
            case "artery" -> (x, y, w, h) -> new PressMinigame(x, y, w, h, true);
            case "stiff", "stretch" -> StretchMinigame::new;
            case "arrow" -> (x, y, w, h) -> new ArrowMinigame(x, y, w, h);
            // The blast screen with one round in it, because the way out is the same job
            // with a different thing in the wound, and the page should show that.
            // The round is a rifle round rather than the generic one the game falls back
            // to, so the page shows the shape the bullet entry staged rather than a
            // second, different-looking bullet.
            case "bullet" -> (x, y, w, h) ->
                    new ShrapnelMinigame(x, y, w, h, 0, List.of("tacz:762x39"), true);
            case "blast" -> (x, y, w, h) -> new ShrapnelMinigame(x, y, w, h, 3, true);
            case "crack", "fracture", "splint_worn" ->
                    (x, y, w, h) -> new SplintMinigame(x, y, w, h, BodyPart.LEFT_LEG, true);
            // The same screen over each of the three, because seeing them side by side is
            // most of what tells them apart - and the third one refuses itself, which is
            // the lesson.
            case "skull" -> (x, y, w, h) -> new SplintMinigame(x, y, w, h, BodyPart.HEAD, true);
            case "displaced" ->
                    (x, y, w, h) -> new SplintMinigame(x, y, w, h, BodyPart.LEFT_LEG, true, true, false);
            case "open_bone" ->
                    (x, y, w, h) -> new SplintMinigame(x, y, w, h, BodyPart.LEFT_LEG, true, true, true);
            case "set" -> SetMinigame::new;
            case "op_irrigate" -> IrrigateMinigame::new;
            case "op_trephine" -> (x, y, w, h) -> new TrephineMinigame(x, y, w, h, true);
            case "op_organ" -> OrganMinigame::new;
            case "op_excise" -> ExciseMinigame::new;
            case "op_debride", "operations" -> SurgeryMinigame::new;
            case "op_osteotomy" -> OsteotomyMinigame::new;
            case "embolism" -> (x, y, w, h) -> new VeinMinigame(x, y, w, h, 0.0F, 0xFF8ACBD8);
            case "dressing" -> (x, y, w, h) -> new WrapMinigame(x, y, w, h);
            case "tourniquet" -> (x, y, w, h) -> WrapMinigame.forTourniquet(x, y, w, h);
            case "infection" -> (x, y, w, h) -> new CleanMinigame(x, y, w, h);
            case "burn" -> (x, y, w, h) -> new SalveMinigame(x, y, w, h, PartState.BURN_PARTIAL);
            case "touch" -> (x, y, w, h) -> new TouchMinigame(x, y, w, h, PartState.BURN_FULL);
            case "blacked" -> (x, y, w, h) -> new SurgeryMinigame(x, y, w, h);
            case "graft" -> (x, y, w, h) -> new GraftMinigame(x, y, w, h);
            case "missing" -> (x, y, w, h) -> new SawMinigame(x, y, w, h);
            case "prosthetic", "wear" -> (x, y, w, h) -> new FitMinigame(x, y, w, h, true);
            case "pneumo" -> (x, y, w, h) -> new NeedleMinigame(x, y, w, h);
            case "blood_loss" -> (x, y, w, h) -> new LineMinigame(x, y, w, h);
            case "downed" -> (x, y, w, h) -> new AirwayMinigame(x, y, w, h);
            case "arrest" -> (x, y, w, h) -> new CompressionsMinigame(x, y, w, h);
            case "roll" -> (x, y, w, h) -> new RollMinigame(x, y, w, h);
            case "unwrap" -> (x, y, w, h) -> new PeelMinigame(x, y, w, h);
            case "pulse" -> (x, y, w, h) -> new PulseMinigame(x, y, w, h, Pulse.of(new BodyHealth()), false);
            case "thermometer", "temperature" -> (x, y, w, h) -> new ThermometerMinigame(x, y, w, h);
            case "pain" -> (x, y, w, h) -> new PillMinigame(x, y, w, h, 3);
            case "illness" -> (x, y, w, h) -> new CourseMinigame(x, y, w, h);
            case "parasites" -> (x, y, w, h) -> new WormingMinigame(x, y, w, h);
            case "syringes" -> (x, y, w, h) -> new VeinMinigame(x, y, w, h, 0.0F, 0xFFE08A1C);
            case "anaesthetic" -> (x, y, w, h) -> new MaskMinigame(x, y, w, h);
            case "unstrap" -> (x, y, w, h) -> new CutMinigame(x, y, w, h);
            default -> null;
        };
    }

    private static List<Entry> flatten(List<Section> sections) {
        List<Entry> out = new ArrayList<>();
        for (Section section : sections) {
            out.addAll(section.pages());
        }
        return out;
    }

    /**
     * The list as it is actually drawn: every heading, and the pages of the open one.
     *
     * <p>Built fresh rather than kept, because it changes whenever a heading is opened and
     * a cached copy of a list like this is a bug waiting for somebody to forget it.
     */
    private List<Line> lines() {
        List<Line> out = new ArrayList<>();
        int index = 0;
        for (int s = 0; s < sections.size(); s++) {
            Section section = sections.get(s);
            out.add(new Line(section, null, -1));
            if (s == open) {
                for (Entry entry : section.pages()) {
                    out.add(new Line(null, entry, index + section.pages().indexOf(entry)));
                }
            }
            index += section.pages().size();
        }
        return out;
    }
}
