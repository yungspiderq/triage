package com.claudemods.triage.client.screen;

import com.claudemods.triage.body.PartState;
import com.claudemods.triage.body.BloodType;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.BulletKinds;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.client.BodyFigure;
import com.claudemods.triage.client.ClientBody;
import com.claudemods.triage.item.BloodBag;
import com.claudemods.triage.med.MedicineKind;
import com.claudemods.triage.med.SurgeryJob;
import net.minecraft.world.InteractionHand;
import com.claudemods.triage.net.TreatmentDonePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Where a treatment is actually earned: pick the part, then do the work by hand.
 *
 * <p>Deliberately not a pause screen. Bandaging in the open while something is walking
 * towards you is the point, and a hit closes this screen without spending the item.
 */
public final class TreatmentScreen extends Screen {
    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 210;
    private static final int FIGURE_SCALE = 2;
    private static final int MARGIN = 8;
    private static final int HEADER = 22;
    private static final int FIGURE_COLUMN = 84;


    private final MedicineKind kind;
    /** The body being worked on - this client's own, or the one the server sent over. */
    private BodyHealth patient;
    private final int patientId;

    private @Nullable BodyPart selected;
    private @Nullable Minigame game;
    private boolean finished;

    private int left;
    private int top;
    private int figureX;
    private int figureY;
    private int wellLeft;
    private int wellTop;
    private int wellWidth;
    private int wellHeight;
    private int gameLeft;
    private int gameTop;
    private int gameWidth;
    private int gameHeight;
    private int ticks;

    public TreatmentScreen(MedicineKind kind, int patientId, BodyHealth patient) {
        super(Component.translatable("triage.screen.treatment", initialTitle(kind, patient)));
        this.kind = kind;
        this.patientId = patientId;
        this.patient = patient;
    }

    /**
     * What to call the job before a limb has been chosen.
     *
     * <p>Once one is, {@link #getTitle()} names the thing about to be opened instead,
     * because a body can be carrying a round in one arm and casing in the other.
     */
    private static Component initialTitle(MedicineKind kind, BodyHealth patient) {
        if (kind != MedicineKind.EXTRACT) {
            return kind.title();
        }
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = patient.part(part);
            if (state.stuck()) {
                return extractTitle(kind, state);
            }
        }
        return kind.title();
    }

    /** A round is not a fragment, and the title should not call it one. */
    private static Component extractTitle(MedicineKind kind, @Nullable PartState state) {
        if (kind != MedicineKind.EXTRACT || state == null || state.bullets <= 0) {
            return kind.title();
        }
        var title = Component.translatable(state.arrows > 0
                ? "triage.medicine.extract_mixed" : "triage.medicine.extract_bullet");
        // The round on the table is not a generic one. Naming the calibre in the title
        // is the one place a player can see it before the wound is open, and it is also
        // how a test tells a 9mm from a rifle round without reading the save.
        title.append(Component.literal(" · ")).append(BulletKinds.summary(state.bulletTypes));
        return title;
    }

    /**
     * The headline follows the chosen limb, so a round is never called a fragment.
     *
     * <p>{@code title} is final on the screen, so the name is composed here rather than
     * replaced when the selection changes.
     */
    @Override
    public Component getTitle() {
        if (kind == MedicineKind.EXTRACT && selected != null) {
            return Component.translatable("triage.screen.treatment",
                    extractTitle(kind, patient.part(selected)));
        }
        return super.getTitle();
    }

    private boolean onSomebodyElse() {
        return patientId >= 0;
    }

    /** Whose body this is, for the line beside the title. */
    private Component patientName() {
        if (minecraft == null || minecraft.level == null) {
            return Component.translatable("triage.screen.them");
        }
        return minecraft.level.getEntity(patientId) instanceof Player player
                ? player.getName() : Component.translatable("triage.screen.them");
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;

        // Medicine aimed at the whole body has no limb to pick, so it gets the panel.
        boolean wholeBody = !kind.needsPart();
        int column = wholeBody ? 0 : FIGURE_COLUMN;

        wellLeft = left + MARGIN + column;
        wellTop = top + HEADER + MARGIN;
        wellWidth = PANEL_WIDTH - MARGIN * 2 - column;
        wellHeight = PANEL_HEIGHT - HEADER - MARGIN * 2 - 30;

        // The game keeps clear of its own frame so nothing it draws lands on the bevel.
        gameLeft = wellLeft + 4;
        gameTop = wellTop + 4;
        gameWidth = wellWidth - 8;
        gameHeight = wellHeight - 8;

        figureX = left + MARGIN + (FIGURE_COLUMN - MARGIN - BodyFigure.WIDTH * FIGURE_SCALE) / 2;
        figureY = wellTop + (wellHeight - BodyFigure.HEIGHT * FIGURE_SCALE) / 2;

        if (wholeBody && game == null) {
            game = build();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ticks++;
        if (minecraft == null || minecraft.player == null) {
            onClose();
            return;
        }
        if (game == null) {
            return;
        }
        game.tick();
        if (game.complete()) {
            succeed();
        } else if (game.failed()) {
            leave("triage.msg.treatment_failed", SoundEvents.ITEM_BREAK);
        }
    }

    /** The patient bled a little more while you were working on them. */
    public static void updatePatient(BodyHealth body) {
        if (Minecraft.getInstance().screen instanceof TreatmentScreen treatment) {
            treatment.patient = body;
        }
    }

    /**
     * Closes whatever treatment is on screen, because the server has decided the player
     * has been knocked off it.
     *
     * <p>The server makes that call, not this screen: only it can tell an axe from the
     * player's own bleeding, and both of those come down the same health bar.
     */
    public static void interruptCurrent() {
        if (Minecraft.getInstance().screen instanceof TreatmentScreen treatment) {
            treatment.leave("triage.msg.treatment_interrupted", SoundEvents.ITEM_BREAK);
        }
    }

    private void succeed() {
        if (finished || (kind.needsPart() && selected == null)) {
            return;
        }
        finished = true;
        PacketDistributor.sendToServer(new TreatmentDonePayload(kind, selected,
                game == null ? 1.0F : game.quality()));
        onClose();
    }

    private void leave(String messageKey, net.minecraft.sounds.SoundEvent sound) {
        if (finished) {
            return;
        }
        finished = true;
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.translatable(messageKey).withStyle(ChatFormatting.GRAY), true);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 0.8F));
        }
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (game == null) {
            BodyPart part = BodyFigure.at(figureX, figureY, FIGURE_SCALE, mouseX, mouseY);
            if (part != null && treatable(part)) {
                selected = part;
                game = build();
                if (minecraft != null) {
                    minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
                }
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        return game.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (game != null && game.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (game != null) {
            game.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private static <T> T firstNonNull(@org.jetbrains.annotations.Nullable T value, T fallback) {
        return value == null ? fallback : value;
    }

    private boolean treatable(BodyPart part) {
        return kind.canTreat(part, patient.part(part));
    }

    /**
     * How far gone this patient's veins are, 0 to 1.
     *
     * <p>A vein is held open by what is inside it, so the three things that empty somebody
     * or shut their circulation down are the three things that make one hard to hit. Which
     * means the needle is hardest to get in exactly when it is most wanted, and for the
     * reason it is in life rather than because a number was turned up.
     */
    private float collapse() {
        float collapse = patient.bloodLoss() * 1.2F;
        if (patient.has(Disease.SHOCK)) {
            collapse += 0.3F;
        }
        if (patient.has(Disease.HYPOTHERMIA)) {
            collapse += 0.25F;
        }
        return Math.min(1.0F, collapse);
    }

    private Minigame build() {
        PartState stuck = selected == null ? null : patient.part(selected);
        int fragments = stuck == null ? 0 : Math.max(0, stuck.arrows);
        int bullets = stuck == null ? 0 : Math.max(0, stuck.bullets);
        if (kind == MedicineKind.EXTRACT) {
            if (fragments + bullets <= 0) {
                // A blade, or a part whose counts have already gone: there is still one
                // opening to make, so draw one thing to take out of it.
                fragments = 1;
            }
        }
        return switch (kind) {
            // All three needles share one game, because all three have the same problem:
            // not the plunger, which anybody can push, but getting the tip into a vein and
            // knowing that it is there. How hard that is comes off the patient.
            // All three share one game, because all three have the same problem: not the
            // plunger, which anybody can push, but getting the tip into a vein and knowing
            // it is there. The barrel shows the drug's own colour, so what is in your hand
            // is the thing on the screen.
            // Not a treatment at all: the only game here whose content is waiting.
            case THERMOMETER -> new ThermometerMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            // Told nothing about the chest it is on, and in realistic it could not be told
            // anyway: that answer has already been taken out of the body this screen was
            // handed. So the game is the bell and the hand, and what was heard comes back
            // from the server with everything else.
            case STETHOSCOPE -> new ListenMinigame(gameLeft, gameTop, gameWidth, gameHeight,
                    !onSomebodyElse());
            case EPINEPHRINE, MORPHINE, NALOXONE, ANTICOAGULANT -> new VeinMinigame(gameLeft, gameTop,
                    gameWidth, gameHeight, collapse(), switch (kind) {
                        case EPINEPHRINE -> 0xFFE08A1C;
                        case MORPHINE -> 0xFF5B63C4;
                        case ANTICOAGULANT -> 0xFF8ACBD8;
                        default -> 0xFFCFE8D5;
                    });
            case BANDAGE -> new WrapMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            case TOURNIQUET -> WrapMinigame.forTourniquet(gameLeft, gameTop, gameWidth, gameHeight);
            // Handed the bone as it is rather than as one picture for all of them: ends
            // that meet, ends that do not, or one that is outside the skin.
            case SPLINT -> new SplintMinigame(gameLeft, gameTop, gameWidth, gameHeight, selected,
                    selected != null && patient.part(selected).fractured(),
                    selected != null && patient.part(selected).outOfLine(),
                    selected != null && patient.part(selected).openFracture());
            case MEDKIT -> new DressMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            // Surgery has its own, and it is the only game here that is a fight for
            // the view rather than a test of the hands.
            // Six operations, six games, chosen by the same method the server will use
            // to decide the outcome - so nobody ever plays one job and is handed another.
            case SURGERY -> switch (selected == null ? SurgeryJob.DEBRIDE
                    : firstNonNull(SurgeryJob.of(selected, patient.part(selected)), SurgeryJob.DEBRIDE)) {
                case IRRIGATE -> new IrrigateMinigame(gameLeft, gameTop, gameWidth, gameHeight);
                case TREPHINE -> new TrephineMinigame(gameLeft, gameTop, gameWidth, gameHeight,
                        selected == BodyPart.HEAD);
                case ORGAN -> new OrganMinigame(gameLeft, gameTop, gameWidth, gameHeight);
                case EXCISE -> new ExciseMinigame(gameLeft, gameTop, gameWidth, gameHeight);
                case OSTEOTOMY -> new OsteotomyMinigame(gameLeft, gameTop, gameWidth, gameHeight);
                case DEBRIDE -> new SurgeryMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            };
            // Its own, because joining two pieces is not repairing one: what decides a
            // graft is whether the bone was lined up before the first stitch went in.
            case GRAFT -> new GraftMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            case ANTISEPTIC -> new CleanMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            // How deep it went decides whether there is anything on it to be careful of.
            case SALVE -> new SalveMinigame(gameLeft, gameTop, gameWidth, gameHeight,
                    selected == null ? PartState.BURN_PARTIAL : patient.part(selected).burn);
            // A course for a cold is one tablet more than nothing; a course for blood
            // poisoning is not. The bottle counts out however many this one is.
            case ANTIBIOTICS -> new PillMinigame(gameLeft, gameTop, gameWidth, gameHeight,
                    2 + worstSeverity());
            // Not the card game. Pressing foil is the same job either way and this is
            // the half that is not: a worming tablet is the one that will not go down a
            // patient ill enough to need it, so it gets crushed and stirred into water.
            case WORMING -> new WormingMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            case TRANSFUSION -> new LineMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            // Its own, because the whole difficulty of it is which of the two ribs the
            // needle goes along, and that cannot be drawn as a mark in mid-air.
            case DECOMPRESS -> new NeedleMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            case DRAW -> LineMinigame.forDraw(gameLeft, gameTop, gameWidth, gameHeight);
            // Counting a dose out of a bottle. The lightest game in the mod, because it
            // is the lightest thing in the mod - and the one mistake tablets have is how
            // many, which is the whole of it.
            case PAINKILLER -> new PillMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            case ANTIPSYCHOTIC -> new CourseMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            // The only game here with no band on it. You are reading a person.
            case ANAESTHETIC -> new MaskMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            case SCISSORS -> new CutMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            // The same wound in section the bare hands get, with jaws in it rather than
            // fingers - which is the whole of what the item is. What is inside decides
            // whether the pieces are drawn as casing or as rounds, and which rounds.
            case EXTRACT -> new ShrapnelMinigame(gameLeft, gameTop, gameWidth, gameHeight,
                    fragments, stuck == null ? List.of() : stuck.bulletTypes, true);
            case AMPUTATE -> new SawMinigame(gameLeft, gameTop, gameWidth, gameHeight);
            // Seat the socket off the end of the bone, then line it up against the limb
            // that is left. Both of those are what decides whether one is worth wearing.
            case PEG, HOOK, JOINT, HAND -> new FitMinigame(gameLeft, gameTop, gameWidth, gameHeight,
                    kind == MedicineKind.PEG || kind == MedicineKind.JOINT);
        };
    }

    /** A course for a cold is one pill; a course for sepsis is not. */
    private int worstSeverity() {
        Disease worst = patient.worstDisease();
        return worst == null ? 0 : worst.severity();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Paper.sync();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        Paper.sheet(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        // Header: which treatment on the left, whose body on the right, and a rule under
        // both so the working area starts somewhere definite. Whose body it is earns the
        // headline over which limb: bandaging the wrong person is the worse mistake, and
        // the limb is already lit up on the figure.
        BodyHealth body = patient;
        graphics.drawString(font, getTitle(), left + MARGIN, top + 9, Paper.INK, false);
        Component subject = onSomebodyElse() ? patientName() : subject(body);
        graphics.drawString(font, subject,
                left + PANEL_WIDTH - MARGIN - font.width(subject), top + 9,
                onSomebodyElse() ? Paper.BLUE : Paper.INK_SOFT, false);
        Paper.heavyRule(graphics, left + MARGIN, top + HEADER - 6, PANEL_WIDTH - MARGIN * 2);

        Paper.box(graphics, wellLeft, wellTop, wellWidth, wellHeight);

        if (kind.needsPart()) {
            BodyFigure.draw(graphics, body, figureX, figureY, FIGURE_SCALE, 255, ticks,
                    game == null ? this::treatable : part -> part == selected);
        }

        if (game == null) {
            Component prompt = Component.translatable("triage.screen.pick_part");
            graphics.drawString(font, prompt, gameLeft + gameWidth / 2 - font.width(prompt) / 2,
                    gameTop + gameHeight / 2 - 10, Paper.INK, false);
            graphics.drawString(font, kind.hint(), gameLeft + gameWidth / 2 - font.width(kind.hint()) / 2,
                    gameTop + gameHeight / 2 + 2, Paper.INK_SOFT, false);

            BodyPart hovered = BodyFigure.at(figureX, figureY, FIGURE_SCALE, mouseX, mouseY);
            if (hovered != null) {
                PartTooltip.render(graphics, font, hovered, body.part(hovered), body,
                        mouseX, mouseY, width, height);
            }
            return;
        }

        game.render(graphics, font, mouseX, mouseY, partialTick);

        Component instruction = game.instruction();
        int footer = top + PANEL_HEIGHT - 24;
        graphics.drawString(font, instruction,
                left + PANEL_WIDTH / 2 - font.width(instruction) / 2, footer, Paper.INK, false);
        Paper.gauge(graphics, left + MARGIN + 30, footer + 12, PANEL_WIDTH - MARGIN * 2 - 60, 7,
                game.progress(), Paper.GREEN);
    }

    /** The line in the top right: which limb, how much blood, or which illness. */
    private Component subject(BodyHealth body) {
        if (kind.needsPart()) {
            return selected == null ? Component.empty() : selected.title();
        }
        if (kind == MedicineKind.DRAW) {
            BloodType type = body.bloodType();
            return type == null ? Component.empty() : type.title();
        }
        if (kind == MedicineKind.TRANSFUSION) {
            return transfusionSubject(body);
        }
        Disease worst = body.worstDisease();
        return worst == null ? Component.empty() : worst.title().copy().withStyle(worst.colour());
    }

    /**
     * For a drip: how much they have lost, or - when there is blood rather than saline in
     * hand - which type is going into which.
     *
     * <p>Read off the player's own hands rather than sent with the packet, because the
     * bag is in this client's inventory and it already knows what is in it. Saying it out
     * loud before the work starts is the whole difference between a rule and a trap:
     * nothing is spent until the game is finished, so a player who sees the wrong pair
     * here can still close the screen and keep the bag.
     */
    private Component transfusionSubject(BodyHealth body) {
        BloodType donor = heldBlood();
        if (donor == null) {
            return Component.translatable("triage.screen.blood_lost",
                    Math.round(body.bloodLoss() * 100.0F)).withStyle(ChatFormatting.RED);
        }
        BloodType recipient = body.bloodType();
        if (recipient == null) {
            return donor.title();
        }
        return Component.translatable("triage.screen.blood_match", donor.title(), recipient.title())
                .withStyle(donor.canGiveTo(recipient) ? ChatFormatting.GREEN : ChatFormatting.DARK_RED);
    }

    /** The blood in either hand, if any. */
    private @Nullable BloodType heldBlood() {
        if (minecraft == null || minecraft.player == null) {
            return null;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            BloodType type = BloodBag.typeOf(minecraft.player.getItemInHand(hand));
            if (type != null) {
                return type;
            }
        }
        return null;
    }
}
