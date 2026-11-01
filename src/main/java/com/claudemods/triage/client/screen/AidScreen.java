package com.claudemods.triage.client.screen;

import com.claudemods.triage.net.ModNetwork;
import com.claudemods.triage.body.AidKind;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.client.ClientBody;
import com.claudemods.triage.item.ModItems;
import com.claudemods.triage.body.PartState;
import com.claudemods.triage.body.Pulse;
import com.claudemods.triage.net.AidDonePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Where the free things are earned.
 *
 * <p>The same screen the treatments get, minus the two things aid does not have: no limb
 * to pick, because hands go where the trouble is, and no item along the bottom, because
 * there is not one. What is left is the work itself.
 *
 * <p>Small on purpose. Every one of these is done over somebody who is dying, so the
 * games behind it are the shortest in the mod and none of them can be failed - a press
 * held badly is still a press. What is lost by doing it badly is how much it was worth,
 * which the server works out from the number this screen sends when it closes.
 */
public final class AidScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 176;
    private static final int MARGIN = 8;
    private static final int HEADER = 22;


    private final AidKind kind;
    private final int patientId;
    /** The part that was clicked to open this, when one was. */
    private final @Nullable BodyPart part;
    private BodyHealth patient;

    private @Nullable Minigame game;
    private boolean finished;

    private int left;
    private int top;
    private int wellLeft;
    private int wellTop;
    private int wellWidth;
    private int wellHeight;

    public AidScreen(AidKind kind, int patientId, BodyHealth patient, @Nullable BodyPart part) {
        super(kind.title());
        this.kind = kind;
        this.patientId = patientId;
        this.patient = patient;
        this.part = part;
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        wellLeft = left + MARGIN;
        wellTop = top + HEADER + MARGIN;
        wellWidth = PANEL_WIDTH - MARGIN * 2;
        wellHeight = PANEL_HEIGHT - HEADER - MARGIN * 2 - 26;
        if (game == null) {
            game = switch (kind) {
                case AIRWAY -> new AirwayMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8, wellHeight - 8);
                case COMPRESSIONS -> new CompressionsMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8, wellHeight - 8);
                // Which of the two it is comes off the chart this screen was handed,
                // the same way the arrow and the fragments are told apart: they are not
                // the same job, and a player who has seen one should be able to see at
                // a glance that this is the other.
                case PRESSURE -> new PressMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8,
                        wellHeight - 8, arterial());
                // Which game depends on what is in there, and the two are not the same
                // job: an arrow is one thing you can see, and fragments are several you
                // cannot. The screen works it out from the body it was handed.
                case PULL -> {
                    PartState hit = stuckPart();
                    // A bullet is not a shaft either: it is under the skin, so it is the
                    // forceps game with however many pieces are in there - fragments and
                    // rounds together, because one opening takes the lot. The counts go
                    // through separately so the game draws and names each kind. A plain
                    // arrow is one shaft and comes out one at a time; blast fragments come
                    // out as the handful they are.
                    int fragments = hit == null ? 0
                            : hit.shrapnel ? hit.arrows : Math.min(1, hit.arrows);
                    yield hit != null && (hit.shrapnel || hit.bullets > 0)
                            ? new ShrapnelMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8,
                                    wellHeight - 8, fragments, hit.bulletTypes, hasForceps())
                            : new ArrowMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8, wellHeight - 8);
                }
                case ROLL -> new RollMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8, wellHeight - 8);
                case UNWRAP -> new PeelMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8, wellHeight - 8);
                // Never reached: a strap is untied on the spot without a screen, because
                // there is nothing in untying one worth playing.
                case UNSTRAP -> new PeelMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8, wellHeight - 8);
                // The heart it plays at is the patient's real one, read off the same
                // body the server will read. Nothing is synced: both sides do the
                // arithmetic, so the beats under the fingers are the beats.
                // The helper's ears, not the patient's: counting by ear is the helper's
                // job, and it is the helper who was standing next to the same blast.
                case PULSE -> new PulseMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8, wellHeight - 8,
                        Pulse.of(patient), ClientBody.get().has(Disease.DEAFNESS));
                // Never reached: asking somebody a question is not a thing to play at,
                // and putting a game in front of it would have made the one channel in
                // the mod that is a conversation into another procedure.
                // Never reached, either of them: asking somebody a question is not a
                // procedure, and putting a game in front of the two things in this mod
                // that are a conversation would turn them into one.
                case ASK, SCORE -> new PeelMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8, wellHeight - 8);
                // The depth drawn is the depth the chart already holds, which is the same
                // one the server will report. Nothing is hidden from the client and
                // nothing needs to be: what the player does not know is which patch is
                // quiet, and they find that out by pressing it.
                case TOUCH -> new TouchMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8,
                        wellHeight - 8, burntDepth());
                // And this one is told nothing on purpose rather than by accident. In
                // realistic difficulty whether that bone is broken is the answer the
                // player is here to buy, so it does not travel: the game is about the
                // hand, and the finding arrives afterwards the way a pulse does.
                case FEEL -> new FeelMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8,
                        wellHeight - 8, self());
                // And the sweep, which is told nothing either. What it draws is an outline
                // and the record of where the hands have been; what they found comes back
                // from the server, because that is the part being bought.
                case SURVEY -> new SurveyMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8,
                        wellHeight - 8, self());
                // Nothing about the patient is passed in, and nothing could be: the only
                // number this game is about is where their pain starts, and that is not
                // on the chart, not in the packet and not knowable by anybody but them.
                case STRETCH -> new StretchMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8,
                        wellHeight - 8);
                // Nothing about the patient goes in here either. Which way the bone has
                // gone is rolled on the screen and read off the picture, because that is
                // how it is read off a limb: by looking at the shape of it.
                case SET -> new SetMinigame(wellLeft + 4, wellTop + 4, wellWidth - 8, wellHeight - 8);
            };
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * How deep the burn this screen was opened for goes.
     *
     * <p>The part that was clicked, and only if there is not one the worst burn on the
     * body - which is the same fallback the server uses, so the two cannot disagree about
     * what is being pressed on.
     */
    private int burntDepth() {
        if (part != null) {
            return patient.part(part).burn;
        }
        int worst = 0;
        for (BodyPart candidate : BodyPart.VALUES) {
            worst = Math.max(worst, patient.part(candidate).burn);
        }
        return worst;
    }

    /** Whether the wound under the hand is a vessel rather than a cut. */
    private boolean arterial() {
        if (part != null) {
            return patient.part(part).arterial();
        }
        for (BodyPart candidate : BodyPart.VALUES) {
            if (patient.part(candidate).arterial()) {
                return true;
            }
        }
        return false;
    }

    /** The part with something in it, which is the one this screen was opened for. */
    private @org.jetbrains.annotations.Nullable PartState stuckPart() {
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = patient.part(part);
            if (state.stuck()) {
                return state;
            }
        }
        return null;
    }

    /** Whether the helper is carrying a pair of jaws, or is about to use their fingers. */
    private static boolean hasForceps() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null
                && client.player.getInventory().contains(new ItemStack(ModItems.FORCEPS.get()));
    }

    public static void updatePatient(BodyHealth body) {
        if (Minecraft.getInstance().screen instanceof AidScreen aid) {
            aid.patient = body;
        }
    }

    /** The server has decided the player was knocked off this. */
    public static void interruptCurrent() {
        if (Minecraft.getInstance().screen instanceof AidScreen aid) {
            aid.leave("triage.msg.treatment_interrupted");
        }
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null) {
            onClose();
            return;
        }
        if (game == null) {
            return;
        }
        game.tick();
        if (game.complete()) {
            send(game.quality());
        } else if (game.failed()) {
            // Only pulling can fail, and a failed pull is not a wasted trip: the arrow
            // still comes out. It comes out badly, which the server works out from the
            // nothing this sends it.
            send(0.0F);
        }
    }

    private void send(float quality) {
        if (finished || game == null) {
            return;
        }
        finished = true;
        ModNetwork.toServer(new AidDonePayload(kind, quality));
        onClose();
    }

    private void leave(String messageKey) {
        if (finished) {
            return;
        }
        finished = true;
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.translatable(messageKey).withStyle(ChatFormatting.GRAY), true);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_BREAK, 0.8F));
        }
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return (game != null && game.mouseClicked(mouseX, mouseY, button))
                || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return (game != null && game.mouseDragged(mouseX, mouseY, button))
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (game != null) {
            game.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Whether these are your own hands on your own limb. Changes only the wording. */
    private boolean self() {
        return minecraft != null && minecraft.player != null && minecraft.player.getId() == patientId;
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
        Paper.sync();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        Paper.sheet(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawString(font, title, left + MARGIN, top + 9, Paper.INK, false);
        Component name = patientName();
        graphics.drawString(font, name, left + PANEL_WIDTH - MARGIN - font.width(name),
                top + 9, Paper.BLUE, false);
        Paper.heavyRule(graphics, left + MARGIN, top + HEADER - 6, PANEL_WIDTH - MARGIN * 2);

        Paper.box(graphics, wellLeft, wellTop, wellWidth, wellHeight);

        if (game == null) {
            return;
        }
        game.render(graphics, font, mouseX, mouseY, partialTick);

        int barTop = top + PANEL_HEIGHT - MARGIN - 12;
        Paper.gauge(graphics, wellLeft, barTop, wellWidth, 7, game.progress(), Paper.GREEN);
        Component instruction = game.instruction();
        graphics.drawString(font, instruction, left + (PANEL_WIDTH - font.width(instruction)) / 2,
                barTop - 12, Paper.INK_SOFT, false);
    }
}
