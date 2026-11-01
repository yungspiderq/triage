package com.claudemods.triage.med;

import com.claudemods.triage.Config;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.net.CloseTreatmentPayload;
import com.claudemods.triage.net.OpenTreatmentPayload;
import com.claudemods.triage.net.UpdatePatientPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.claudemods.triage.body.Pain;
import com.claudemods.triage.body.Reactions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.claudemods.triage.net.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A treatment in progress, on yourself or on somebody else.
 *
 * <p>The screen where the player does the work runs on their client, so the server
 * keeps a note of what it let them start and checks the answer against it: which kind
 * of medicine, in which hand, on whom, and not older than a minute. Without the note,
 * the packet that finishes a treatment would be a free heal for anyone who felt like
 * sending one - and, since 0.6.0, a free heal aimed at anybody they liked.
 */
public final class Treatments {
    /** Long enough for a fumbled bandage, short enough not to be worth banking. */
    private static final int EXPIRY_TICKS = 20 * 60;
    /** Working on someone means standing over them, not shouting from a roof. */
    private static final double REACH = 5.0;

    /** Not a record: the damage soaked up while treating has to be added to. */
    private static final class Pending {
        private final MedicineKind kind;
        private final InteractionHand hand;
        private final UUID patient;
        private final long expiresAt;
        private float damageTaken;

        private Pending(MedicineKind kind, InteractionHand hand, UUID patient, long expiresAt) {
            this.kind = kind;
            this.hand = hand;
            this.patient = patient;
            this.expiresAt = expiresAt;
        }
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private Treatments() {
    }

    /**
     * Starts a treatment: either straight away, or by opening the screen where the
     * healer has to earn it.
     *
     * @return true when something happened
     */
    public static boolean begin(ServerPlayer healer, ServerPlayer patient, MedicineKind kind, InteractionHand hand) {
        // Creative and spectator bodies are left alone on purpose - but saying so out
        // loud matters, because a right click that does absolutely nothing reads as a
        // broken mod rather than as a rule.
        if (!Body.tracks(healer)) {
            return Medicine.refuse(healer, "triage.msg.untracked_you");
        }
        if (!Body.tracks(patient)) {
            return Medicine.refuse(healer, "triage.msg.untracked_them");
        }
        ItemStack stack = healer.getItemInHand(hand);
        if (healer.getCooldowns().isOnCooldown(stack.getItem())) {
            return false;
        }
        BodyHealth body = Body.of(patient);
        boolean onSomebodyElse = healer != patient;
        if (!Medicine.hasWork(body, kind)) {
            return Medicine.refuse(healer, onSomebodyElse
                    ? "triage.msg.nothing_to_do_for_them" : Medicine.nothingToDoKey(kind));
        }

        // Packing a wound on somebody who is going does not get a screen either. It is
        // the ten seconds you have with something walking towards you, and a minigame
        // here would be asking for exactly the wrong thing.
        if (body.downed() && kind.canStabilise()) {
            return Medicine.applyTo(healer, patient, kind, null, stack, 1.0F);
        }

        // A pack may want the screen gone entirely; short of that, everything earns its
        // effect, including the two aimed at the body as a whole rather than at a limb.
        // The screen has always been able to skip the figure and go straight to the
        // work, and gating on "needs a part" instead of "has anything to do" is what
        // quietly kept the dose game off the antibiotics.
        //
        // Painkillers used to be the exception here on the grounds that a pill is
        // swallowed rather than performed. Shaking a dose out of a bottle is a thing
        // your hands do, and the exception mostly meant one item in the box was a button
        // rather than a treatment. Being interrupted is still off for them, which is the
        // half of that argument that was actually about being ill.
        if (!Config.values().minigame) {
            return Medicine.applyTo(healer, patient, kind, Medicine.autoTarget(body, kind), stack, 1.0F);
        }

        // A fresh note: damage soaked up during an earlier, abandoned attempt should
        // not count against this one.
        PENDING.put(healer.getUUID(),
                new Pending(kind, hand, patient.getUUID(), healer.level().getGameTime() + EXPIRY_TICKS));
        PacketDistributor.sendToPlayer(healer,
                new OpenTreatmentPayload(kind, onSomebodyElse ? patient.getId() : -1,
                        Body.seen(body, !onSomebodyElse)));
        return true;
    }

    /** The client says the treatment went well. Everything about that is checked here. */
    public static void finish(ServerPlayer healer, MedicineKind kind, @Nullable BodyPart part, float quality) {
        Pending pending = PENDING.remove(healer.getUUID());
        if (pending == null || pending.kind != kind || healer.level().getGameTime() > pending.expiresAt) {
            return;
        }
        ItemStack stack = healer.getItemInHand(pending.hand);
        if (Medicine.kindOf(stack) != kind) {
            // Swapped hands, dropped it, or ran out mid-treatment.
            return;
        }
        ServerPlayer patient = patient(healer, pending);
        if (patient == null) {
            return;
        }
        Medicine.applyTo(healer, patient, kind, part, stack, quality);
    }

    /**
     * The player being treated, or null when they are no longer somebody this healer
     * can work on - gone, dead, or walked off mid-stitch.
     */
    private static @Nullable ServerPlayer patient(ServerPlayer healer, Pending pending) {
        if (healer.getUUID().equals(pending.patient)) {
            return healer;
        }
        ServerPlayer patient = healer.server.getPlayerList().getPlayer(pending.patient);
        if (patient == null || patient.isDeadOrDying() || patient.level() != healer.level()) {
            return null;
        }
        if (healer.distanceTo(patient) > REACH) {
            Medicine.refuse(healer, "triage.msg.too_far");
            return null;
        }
        return patient;
    }

    /**
     * Something hit the healer while they were working.
     *
     * <p>The judgement lives here rather than on the client because only the server can
     * tell a hit from the player's own bleeding - both come off the same health bar, and
     * a heavy bleed would otherwise cancel the very bandage meant to stop it. Swallowing
     * a pill is never interrupted: being ill is the state you cannot fight your way out
     * of first, so making the cure need a clear field would be a trap with no exit. A
     * drip is not swallowed, and a hard knock pulls the needle out like anything else.
     *
     * @param damage the blow as the vanilla health bar would have shown it
     */
    public static void onDamaged(ServerPlayer player, float damage) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || pending.kind.swallowed() || damage <= 0.0F) {
            return;
        }
        float threshold = Config.values().interruptDamage;
        if (threshold <= 0.0F) {
            return;
        }
        pending.damageTaken += damage;
        if (pending.damageTaken >= threshold) {
            PENDING.remove(player.getUUID());
            PacketDistributor.sendToPlayer(player, new CloseTreatmentPayload());
        }
    }

    /**
     * The patient pulls away.
     *
     * <p>Until now the only thing that could stop a treatment was somebody hitting the
     * person doing it. The patient has been perfectly still for a saw going through their
     * leg, awake, with nothing in them, because nobody ever wrote the line that says a
     * person in agony does not hold still.
     *
     * <p>That absence made three of this mod's own systems decorative. Ether existed and
     * nobody needed it; a second pair of hands existed and did nothing a first pair could
     * not; painkillers were for the patient's comfort, which is not a thing a game can
     * make anybody care about. All three are now the difference between a job that
     * finishes and a job that does not, and none of them had to be changed to do it.
     *
     * <p>Only jobs that open skin, only on somebody awake, and never on somebody who is
     * under, dosed, or being held. Which is to say: it never happens to a medic who did
     * the thing the mod has been quietly recommending for ten versions.
     */
    public static void panic(ServerPlayer healer) {
        Pending pending = PENDING.get(healer.getUUID());
        if (pending == null || !pending.kind.opensSkin()) {
            return;
        }
        Config.Values config = Config.values();
        if (!config.panic || config.panicChance <= 0.0F) {
            return;
        }
        ServerPlayer patient = healer.server.getPlayerList().getPlayer(pending.patient);
        if (patient == null) {
            return;
        }
        BodyHealth body = Body.of(patient);
        // Anybody who is not there cannot object, and that is exactly what ether buys.
        if (body.under() || body.downed() || body.arrested() || body.painkilled()) {
            return;
        }
        // And a second pair of hands is the other answer - the one that costs nothing but
        // a friend, which is the answer this mod would rather people reached for.
        if (Theatre.assistant(patient, healer) != null) {
            return;
        }
        float pain = Pain.burden(body) / Pain.WORST;
        if (patient.getRandom().nextFloat() >= config.panicChance * pain) {
            return;
        }
        PENDING.remove(healer.getUUID());
        PacketDistributor.sendToPlayer(healer, new CloseTreatmentPayload());
        Body.notify(healer, Component.translatable("triage.msg.pulled_away", patient.getName()),
                ChatFormatting.RED, SoundEvents.PLAYER_HURT);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.pulled_away_you"),
                    ChatFormatting.RED, null);
        }
        Reactions.say(patient, "panic");
    }

    /**
     * Sends the healer a fresh copy of whoever they are working on, once a second.
     * Only for work on somebody else: your own screen already follows your own body.
     */
    public static void resendPatient(ServerPlayer healer) {
        Pending pending = PENDING.get(healer.getUUID());
        if (pending == null || healer.getUUID().equals(pending.patient)) {
            return;
        }
        ServerPlayer patient = healer.server.getPlayerList().getPlayer(pending.patient);
        if (patient != null) {
            PacketDistributor.sendToPlayer(healer,
                    new UpdatePatientPayload(Body.seen(Body.of(patient), healer == patient)));
        }
    }

    public static void forget(UUID playerId) {
        PENDING.remove(playerId);
    }
}
