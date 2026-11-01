package com.claudemods.triage.body;

import com.claudemods.triage.net.ModNetwork;
import com.claudemods.triage.Config;
import com.claudemods.triage.data.Milestones;
import com.claudemods.triage.item.ModItems;
import com.claudemods.triage.net.ShowBodyPayload;
import com.claudemods.triage.net.SyncTagPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Writing on somebody, and letting everybody else see what was written.
 *
 * <p>Looking a player over has been possible since the chart existed, and it was worth
 * exactly nothing to anybody but the person doing the looking: they walked up, they read
 * the screen, and the answer died with them. A group of three ended up assessing the
 * same casualty three times because there was nowhere to put the answer.
 *
 * <p>A body's own chart travels only to its owner - that is what makes the mod's
 * information private and worth going to get. The mark is the deliberate exception, and
 * the only thing about a body that everybody around it can read: it is pinned to their
 * chest, and a thing pinned to a chest is a thing other people can see.
 */
public final class Tags {
    /** Writing on somebody means standing over them, the same as treating them does. */
    private static final double REACH = 5.0;

    /**
     * What each body last told the room, so a mark that has not changed is not sent
     * again once a second forever. Server-side and keyed by player, like the note
     * {@link com.claudemods.triage.med.Treatments} keeps of a treatment in progress.
     */
    private record Mark(@Nullable TriageTag tag, boolean stale, boolean downed, long marks, long worn) {
        /** Whether this body has anything at all to say to the room. */
        private boolean silent() {
            // The colour of somebody is in the second long, so a body whose only news is
            // that it has gone white is a body with something to say.
            return tag == null && !downed && marks == 0 && worn == 0;
        }
    }

    private static final Map<UUID, Mark> SENT = new HashMap<>();

    private Tags() {
    }

    /**
     * Somebody has decided what this patient is.
     *
     * @return true when the mark was written
     */
    public static boolean write(ServerPlayer author, ServerPlayer patient, TriageTag tag) {
        if (!Config.values().tags || author == patient) {
            return false;
        }
        if (!Body.tracks(patient)) {
            Body.notify(author, Component.translatable("triage.msg.untracked_them"), ChatFormatting.GRAY, null);
            return false;
        }
        if (patient.level() != author.level() || author.distanceTo(patient) > REACH) {
            Body.notify(author, Component.translatable("triage.msg.too_far"), ChatFormatting.GRAY, null);
            return false;
        }
        if (!spend(author)) {
            Body.notify(author, Component.translatable("triage.msg.no_tags"), ChatFormatting.GRAY, null);
            return false;
        }

        BodyHealth body = Body.of(patient);
        body.writeTag(tag, author.getName().getString());
        Body.notify(author, Component.translatable("triage.msg.tagged", patient.getName(), tag.title()),
                ChatFormatting.GRAY, null);
        // The patient is told in the action bar like everything else that happens to
        // them. Being sorted is something done to you, and being marked expectant by
        // somebody who then walks away is worth knowing about.
        Body.notify(patient, Component.translatable("triage.msg.tagged_by", author.getName(), tag.title()),
                ChatFormatting.GRAY, SoundEvents.BOOK_PAGE_TURN);
        Milestones.onSorted(author);

        Body.sync(patient);
        refresh(patient);
        // The author is still looking at the chart they wrote on, so it is worth
        // redrawing - and a fresh snapshot is honest, because they have just looked again.
        ModNetwork.toPlayer(author,
                new ShowBodyPayload(patient.getId(), Body.seen(body, author == patient)));
        return true;
    }

    /**
     * Taking your own tag off.
     *
     * <p>The only way a mark comes off other than somebody writing a new one. Nobody has
     * to walk around wearing a stranger's opinion of them forever, and a player who
     * disagrees with what was written on them is welcome to say so with their hands.
     */
    public static boolean tearOff(ServerPlayer patient) {
        if (!Body.of(patient).tearOffTag()) {
            return false;
        }
        Body.notify(patient, Component.translatable("triage.msg.tag_torn"), ChatFormatting.GRAY, null);
        Body.sync(patient);
        refresh(patient);
        return true;
    }

    /**
     * Once a second, from the body tick: tells the room about this body only when what
     * it would say has changed.
     */
    public static void refresh(ServerPlayer patient) {
        Mark now = markOf(patient);
        Mark before = SENT.get(patient.getUUID());
        if (now.equals(before)) {
            return;
        }
        SENT.put(patient.getUUID(), now);
        // Nothing to say, and nobody was ever told otherwise. Most players are wearing
        // nothing, standing up and in one piece, and saying so out loud once a session
        // for each of them is a packet that buys nobody anything.
        if (before == null && now.silent()) {
            return;
        }
        ModNetwork.trackingAndSelf(patient, payload(patient, now));
    }

    /**
     * Says it again whether or not it has changed.
     *
     * <p>Respawning and changing dimension rebuild the player's entity on every client
     * that can see them, and a rebuilt entity has forgotten everything it was ever told
     * about the old one - including, on their own client, about themselves. The memo of
     * what was last sent has to go with it, or the mark comes back invisible.
     */
    public static void resend(ServerPlayer patient) {
        SENT.remove(patient.getUUID());
        refresh(patient);
    }

    /** A client that has just started seeing this player needs the current mark. */
    public static void sendTo(ServerPlayer viewer, ServerPlayer patient) {
        Mark mark = markOf(patient);
        if (mark.silent()) {
            return;
        }
        ModNetwork.toPlayer(viewer, payload(patient, mark));
    }

    public static void forget(UUID playerId) {
        SENT.remove(playerId);
    }

    private static Mark markOf(ServerPlayer patient) {
        BodyHealth body = Body.of(patient);
        int stale = Config.values().tagStaleSeconds * 20;
        // Being on the floor is not an opinion and does not need a tag behind it: it is
        // the one thing about a body that anybody standing near it can already see, and
        // the nameplate is only saying it from further away than that.
        Config.Values config = Config.values();
        return new Mark(body.tag(), body.tagStale(stale), body.downed() && config.tags,
                LimbMark.pack(LimbMark.of(body),
                        LimbMark.dressings(body, config.dressingCleanSeconds * 20),
                        LimbMark.straps(body, config.strapDeadSeconds * 20),
                        LimbMark.bleeds(body),
                        LimbMark.dying(body),
                        LimbMark.burns(body)),
                LimbMark.packWorn(LimbMark.splints(body), LimbMark.scars(body),
                        LimbMark.bones(body), LimbMark.face(body),
                        LimbMark.blades(body)));
    }

    private static SyncTagPayload payload(ServerPlayer patient, Mark mark) {
        return new SyncTagPayload(patient.getId(), Optional.ofNullable(mark.tag()), mark.stale(),
                mark.downed(), mark.marks(), mark.worn());
    }

    /** @return true when a tag was found and used up, or the author is in creative */
    private static boolean spend(ServerPlayer author) {
        if (author.isCreative()) {
            return true;
        }
        for (ItemStack stack : author.getInventory().items) {
            if (stack.is(ModItems.TRIAGE_TAG.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
