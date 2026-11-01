package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.claudemods.triage.data.Milestones;
import com.claudemods.triage.net.CloseTreatmentPayload;
import com.claudemods.triage.item.Bracelet;
import com.claudemods.triage.item.ModItems;
import com.claudemods.triage.net.OpenAidPayload;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import com.claudemods.triage.net.ShowBodyPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import com.claudemods.triage.net.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * What one player can do for another with nothing in their hands.
 *
 * <p>Until this, every answer the mod had to somebody on the floor was an item. Turn up
 * without the right one and there was nothing to do but watch the clock, which quietly
 * made the mod about inventory rather than about people: the question was never "who is
 * here" but "who packed a surgical kit this morning".
 *
 * <p>All four are free and none of them heals anything. Three of them have to be done
 * again, and that is not a limitation to design around - it is what they are. An airway
 * closes as soon as nobody is holding it open, a hand comes off a wound, and compressions
 * stop working the second the hands stop. What they buy is the minute in which somebody
 * else can arrive with the kit, and the whole mod is about that minute.
 *
 * <p>The fourth is the exception in every direction: pulling an arrow out is permanent,
 * can be done to yourself, and is the only thing here that can leave somebody worse off
 * than before you helped.
 *
 * <p>Each one is earned on a screen, like every treatment here and for the same reason:
 * a thing worth doing should be a thing you did. The games behind them are the shortest
 * in the mod and none of them can be failed - what a bad one costs is how much it was
 * worth, which is the honest shape of a press held badly or a rhythm lost.
 *
 * @see AidKind
 */
public final class Aid {
    /** Kneeling over somebody: the same distance every other treatment is done from. */
    private static final double REACH = 5.0;
    /** Long enough for a fumbled press, short enough not to be worth banking. */
    private static final int EXPIRY_TICKS = 20 * 60;
    /** What the worst possible attempt is still worth. Nobody who tried gets nothing. */
    private static final float FLOOR = 0.35F;

    /** Not a record: the damage soaked up while working has to be added to. */
    private static final class Pending {
        private final AidKind kind;
        private final UUID patient;
        private final @Nullable BodyPart part;
        private final long expiresAt;
        private float damageTaken;

        private Pending(AidKind kind, UUID patient, @Nullable BodyPart part, long expiresAt) {
            this.kind = kind;
            this.patient = patient;
            this.part = part;
            this.expiresAt = expiresAt;
        }
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private Aid() {
    }

    /**
     * Somebody has asked to put their hands on a patient.
     *
     * <p>Everything that could refuse it is decided here rather than when the screen
     * closes, so a player never earns something and is then told it was never on offer.
     *
     * @param part the limb the player put their hands on, or null to let the worst one
     *             decide - which is what the commands and any caller without a chart do
     * @return true when a screen was opened, or when the thing was simply done
     */
    /**
     * Whether this pair of hands is carrying the jaws.
     *
     * <p>Asked on the server rather than trusted from the screen, and asked for one job
     * only: fragments. An arrow is a shaft with an end sticking out of somebody and
     * fingers are a perfectly good answer to one. A handful of metal in a channel is not
     * a thing fingers finish - the deepest piece is past them by design - so a player
     * without the forceps is told that before they spend the attempt rather than after.
     */
    private static boolean hasForceps(ServerPlayer helper) {
        for (ItemStack stack : helper.getInventory().items) {
            if (stack.is(ModItems.FORCEPS.get())) {
                return true;
            }
        }
        return helper.getOffhandItem().is(ModItems.FORCEPS.get()) || helper.isCreative();
    }

    public static boolean give(ServerPlayer helper, ServerPlayer patient, AidKind kind,
                               @Nullable BodyPart part) {
        // Not on yourself, and not because the check is tidy: a player on the floor
        // cannot hold their own airway open or push their own heart round, and being
        // able to would turn going down into a pause rather than the last of your luck.
        // It is the same rule that already stops anybody packing their own wound.
        if (helper == patient && !kind.onSelf()) {
            return refuse(helper, "triage.msg.aid_yourself");
        }
        if (!Body.tracks(patient)) {
            return refuse(helper, "triage.msg.untracked_them");
        }
        if (patient.level() != helper.level() || helper.distanceTo(patient) > REACH) {
            return refuse(helper, "triage.msg.too_far");
        }
        BodyHealth body = Body.of(patient);
        String refusal = refusal(kind, body, Config.values(), part);
        if (refusal != null) {
            return refuse(helper, refusal);
        }
        // The one refusal that is about the helper rather than about the patient, which
        // is why it is here and not in the list above: what is wrong with them is the
        // same either way, and what is missing is in your own pockets.
        if (kind == AidKind.PULL && !hasForceps(helper)) {
            BodyPart stuck = pullPart(body, part);
            if (stuck != null) {
                PartState state = body.part(stuck);
                // A round is under the skin, so there is nothing for fingers to close on.
                // A blast fragment is the same problem with a different shape; both want
                // the forceps, and saying so before the screen opens is kinder than an
                // unwinnable game.
                if (state.bullets > 0 && !state.shrapnel) {
                    return refuse(helper, "triage.msg.need_forceps_bullet");
                }
                if (state.shrapnel) {
                    return refuse(helper, "triage.msg.need_forceps");
                }
            }
        }

        // With the games turned off it simply happens, at full worth - the same bargain
        // the treatments make, and the same setting turns both off.
        //
        // A strap is the same either way. There is nothing in untying one worth playing,
        // and opening a screen to say so would be a game about a knot.
        //
        // Asking somebody a question is the same, and more so. It is the one thing in
        // this mod that is a conversation rather than a procedure, and putting a game in
        // front of it would turn it into one.
        if (!Config.values().minigame || kind == AidKind.UNSTRAP || kind == AidKind.ASK
                || kind == AidKind.SCORE) {
            return apply(helper, patient, kind, part, 1.0F);
        }
        PENDING.put(helper.getUUID(),
                new Pending(kind, patient.getUUID(), part, helper.level().getGameTime() + EXPIRY_TICKS));
        PacketDistributor.sendToPlayer(helper,
                new OpenAidPayload(kind, patient.getId(), Body.seen(body, helper == patient),
                        Optional.ofNullable(part)));
        return true;
    }

    /** The screen says the hands did their part. Everything about that is checked again. */
    public static void finish(ServerPlayer helper, AidKind kind, float quality) {
        Pending pending = PENDING.remove(helper.getUUID());
        if (pending == null || pending.kind != kind || helper.level().getGameTime() > pending.expiresAt) {
            return;
        }
        ServerPlayer patient = helper.server.getPlayerList().getPlayer(pending.patient);
        if (patient == null || patient.isDeadOrDying() || patient.level() != helper.level()) {
            return;
        }
        if (helper.distanceTo(patient) > REACH) {
            refuse(helper, "triage.msg.too_far");
            return;
        }
        // A minute is a long time in this state: what was worth doing when the screen
        // opened may have stopped being worth doing while it was open.
        String refusal = refusal(kind, Body.of(patient), Config.values(), pending.part);
        if (refusal != null) {
            refuse(helper, refusal);
            return;
        }
        apply(helper, patient, kind, pending.part, Mth.clamp(quality, 0.0F, 1.0F));
    }

    /**
     * Something hit the helper while they were working.
     *
     * <p>The same rule the treatments live under, for the same reason: this is not a
     * pause screen, and a hand on somebody's chest is the first thing to go when
     * something starts hitting you.
     */
    public static void onDamaged(ServerPlayer player, float damage) {
        // A body that has just been hit is a body that has moved. Whatever was holding
        // the tongue clear of the throat is not holding it now.
        if (ON_SIDE.remove(player.getUUID())) {
            Body.notify(player, Component.translatable("triage.msg.rolled_back"),
                    ChatFormatting.GRAY, null);
        }
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || damage <= 0.0F) {
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

    public static void forget(UUID playerId) {
        PENDING.remove(playerId);
        ON_SIDE.remove(playerId);
    }

    /**
     * Who has been rolled onto their side.
     *
     * <p>Server-side and not saved, because a posture is not a wound: it is not on the
     * chart, it does not travel with the body, and a player who logs out on the floor
     * has larger problems than which way up they were. It lasts exactly as long as the
     * thing it is about.
     */
    private static final java.util.Set<UUID> ON_SIDE = new java.util.HashSet<>();

    public static boolean onSide(ServerPlayer player) {
        return ON_SIDE.contains(player.getUUID());
    }

    /**
     * Why this cannot be done just now, or null when it can.
     *
     * <p>One place rather than two. The screen opens on this answer and closes on it
     * again, and a rule that lived in only one of those two moments would be a rule a
     * player could walk around by being slow.
     */
    private static @Nullable String refusal(AidKind kind, BodyHealth body, Config.Values config,
                                            @Nullable BodyPart part) {
        if (kind.needsDowned() && !body.downed()) {
            return "triage.msg.aid_standing";
        }
        return switch (kind) {
            case AIRWAY -> !config.airway ? "triage.msg.aid_off"
                    : body.airwayTicks() <= 0 ? "triage.msg.airway_clear"
                    : null;
            case COMPRESSIONS -> {
                if (!config.cpr) {
                    yield "triage.msg.aid_off";
                }
                if (!body.arrested()) {
                    yield "triage.msg.not_arrested";
                }
                int worth = body.compressionWorth(config.arrestSeconds * 20, config.cprFalloff);
                if (worth <= 0) {
                    yield "triage.msg.cpr_spent";
                }
                // Nothing to add yet, so nothing is spent: no round counted and no rib
                // risked. Compressions have a rhythm, and the mod should teach it by
                // refusing the press rather than by quietly charging for it.
                yield worth <= body.arrestTicks() ? "triage.msg.cpr_too_soon" : null;
            }
            // A named limb has to be the limb it says it is. Without that check the
            // chart could ask for a hand on an arm that stopped bleeding while the
            // screen was open, and get one anyway.
            case PRESSURE -> !config.pressure ? "triage.msg.aid_off"
                    : pressPart(body, part) == null ? "triage.msg.nothing_bleeding"
                    : null;
            case PULL -> !config.arrows && !config.bulletsStick && !config.weaponsStick ? "triage.msg.aid_off"
                    : pullPart(body, part) == null ? "triage.msg.nothing_stuck"
                    : null;
            // Nothing to check but whether they are already there. Rolling somebody who
            // is on their side is turning them onto their face.
            case ROLL -> !config.airway ? "triage.msg.aid_off" : null;
            case UNWRAP -> !config.dressings ? "triage.msg.aid_off"
                    : dressedPart(body, part) == null ? "triage.msg.nothing_dressed"
                    : null;
            case UNSTRAP -> strappedPart(body, part) == null ? "triage.msg.nothing_strapped" : null;
            // Nothing to check. There is always a wrist, and a heart that has stopped is
            // a reading rather than a refusal - finding nothing is the finding.
            case PULSE -> !config.pulse ? "triage.msg.aid_off" : null;
            // Nothing to check. A patient who cannot answer is an answer.
            case ASK -> !config.ask ? "triage.msg.aid_off" : null;
            // Same: a number nobody is in a state to give is a finding of its own.
            case SCORE -> !config.ask ? "triage.msg.aid_off" : null;
            // There has to be a burn. Pressing on unburnt skin is not a reading, and
            // refusing it is how the mod says there is nothing here to find out.
            case FEEL -> part == null ? "triage.msg.feel_nowhere" : null;
            case SURVEY -> null;
            case TOUCH -> !config.burns ? "triage.msg.aid_off"
                    : burntPart(body, part) == null ? "triage.msg.nothing_burned"
                    : null;
            // There has to be something tight, and it has to be a part nobody has
            // already worked today. The second rule is the one that makes this a course
            // of treatment rather than a slider: a joint worked four times in an
            // afternoon is not four times looser, it is a torn joint.
            case STRETCH -> !config.stretch || !config.scars ? "triage.msg.aid_off"
                    : stiffPart(body, part) == null ? "triage.msg.nothing_stiff"
                    : null;
            // There has to be a bone out of line, and it has to be inside them. An open
            // fracture is refused here and refused loudly: pulling on a bone that is
            // sticking out of somebody drags whatever it has touched back in with it.
            case SET -> !config.displacement ? "triage.msg.aid_off"
                    : bentPart(body, part) == null
                    ? (openPart(body, part) != null ? "triage.msg.bone_is_open"
                    : "triage.msg.nothing_displaced")
                    : null;
        };
    }

    private static boolean apply(ServerPlayer helper, ServerPlayer patient, AidKind kind,
                                 @Nullable BodyPart part, float quality) {
        Config.Values config = Config.values();
        BodyHealth body = Body.of(patient);
        boolean worked = switch (kind) {
            case AIRWAY -> airway(helper, patient, body, config, quality);
            case COMPRESSIONS -> compressions(helper, patient, body, config, quality);
            case PRESSURE -> pressure(helper, patient, body, config, part, quality);
            case PULL -> pull(helper, patient, body, config, part, quality);
            case ROLL -> roll(helper, patient);
            case UNWRAP -> unwrap(helper, patient, body, config, part, quality);
            case UNSTRAP -> unstrap(helper, patient, body, part);
            case PULSE -> pulse(helper, patient, body, quality);
            case ASK -> ask(helper, patient, body);
            case SCORE -> score(helper, patient, body);
            case TOUCH -> touch(helper, patient, body, part, quality);
            case FEEL -> feel(helper, patient, body, part, quality);
            case SURVEY -> survey(helper, patient, body, quality);
            case STRETCH -> stretch(helper, patient, body, part, quality);
            case SET -> setBone(helper, patient, body, part, quality);
        };
        if (!worked) {
            return false;
        }
        Body.sync(patient);
        // The helper is looking at this chart and has just changed what it says. A fresh
        // copy is honest for the same reason writing a mark redraws it: they have their
        // hands on the body, so they have just looked again.
        PacketDistributor.sendToPlayer(helper,
                new ShowBodyPayload(patient.getId(), Body.seen(body, helper == patient)));
        return true;
    }

    /**
     * The burn under the fingers, and how deep it goes.
     *
     * <p>What comes back depends on how much of it was actually pressed. A thorough job
     * hands over the depth by name - which is what decides whether the jar in somebody's
     * pocket is any use - and a couple of pokes hand over the fact that it is burnt, which
     * anybody could see.
     *
     * <p>Nothing is repaired and nothing can go wrong. It is the same shape as the
     * thermometer: an instrument, or in this case a finger, that turns a word into a fact.
     */
    private static boolean touch(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                 @Nullable BodyPart part, float quality) {
        BodyPart target = burntPart(body, part);
        if (target == null) {
            return refuse(helper, "triage.msg.nothing_burned");
        }
        PartState state = body.part(target);
        boolean self = helper == patient;
        if (quality < 1.0F) {
            Body.notify(helper, Component.translatable("triage.msg.touch_rough", target.title()),
                    ChatFormatting.GOLD, null);
            return true;
        }
        Body.notify(helper, Component.translatable(self ? "triage.msg.touch_self" : "triage.msg.touch_them",
                        patient.getName(), target.title(), Burns.depth(state.burn)),
                Burns.colour(state.burn), null);
        // And it is written down. In arcade nothing reads this; in realistic it is the
        // only way that burn's depth ever gets onto the chart.
        body.establish(target, Findings.BURN, patient.level().getGameTime());
        // And the sentence that is the whole reason for pressing on somebody: what this
        // one wants. A jar, or a knife.
        Body.notify(helper, Component.translatable(switch (state.burn) {
                    case PartState.BURN_FULL -> "triage.msg.touch_wants_knife";
                    case PartState.BURN_PARTIAL -> "triage.msg.touch_wants_salve";
                    default -> "triage.msg.touch_wants_nothing";
                }), ChatFormatting.GRAY, null);
        // Somebody being pressed on knows about it.
        if (!self && !state.burnFull()) {
            Body.notify(patient, Component.translatable("triage.msg.touch_hurt", target.title()),
                    ChatFormatting.RED, null);
        }
        return true;
    }

    /** The burn asked about, or the worst one on the body when nothing was named. */
    private static @Nullable BodyPart burntPart(BodyHealth body, @Nullable BodyPart part) {
        if (part != null) {
            return body.part(part).burned ? part : null;
        }
        BodyPart worst = null;
        int deepest = PartState.BURN_NONE;
        for (BodyPart candidate : BodyPart.VALUES) {
            PartState state = body.part(candidate);
            if (state.burn > deepest) {
                deepest = state.burn;
                worst = candidate;
            }
        }
        return worst;
    }

    /**
     * A dressing off, by hand.
     *
     * <p>Two things decide whether the wound comes with it. How long it has been on -
     * a fresh one lifts, one that has dried on is held there by the very thing that was
     * closing underneath - and how carefully it was done, which is what the screen was
     * measuring.
     *
     * <p>It is the second thing in the mod that can leave somebody worse off than before
     * you helped, and the first was pulling an arrow out. Both are free, and both are the
     * argument for the item that does the same job properly.
     */
    private static boolean unwrap(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                  Config.Values config, @Nullable BodyPart part, float quality) {
        BodyPart target = dressedPart(body, part);
        if (target == null) {
            return refuse(helper, "triage.msg.nothing_dressed");
        }
        PartState state = body.part(target);
        boolean stuck = state.dressedTicks >= config.dressingStuckSeconds * 20;
        state.undress();
        body.setDirty(true);

        // A wound that had closed under a stuck dressing opens again when the dressing
        // takes the clot with it. Nothing to roll if there was nothing closing.
        float clean = Mth.clamp(quality, 0.0F, 1.0F);
        float risk = stuck ? 0.85F - 0.65F * clean : 0.35F - 0.35F * clean;
        if (state.worthDressing() && patient.getRandom().nextFloat() < risk) {
            state.startBleeding(PartState.BLEED_LIGHT, config.lightBleedSeconds * 20);
            Body.notify(patient, Component.translatable("triage.msg.tore_open", target.title()),
                    ChatFormatting.DARK_RED, SoundEvents.SLIME_SQUISH);
            if (helper != patient) {
                Body.notify(helper, Component.translatable("triage.msg.tore_open_them",
                        patient.getName(), target.title()), ChatFormatting.DARK_RED, null);
            }
            return true;
        }
        Body.notify(helper, Component.translatable("triage.msg.unwrapped", target.title()),
                ChatFormatting.GRAY, SoundEvents.WOOL_BREAK);
        return true;
    }

    /**
     * A strap off.
     *
     * <p>Nothing here goes wrong and nothing needs to. The wound under it was never
     * closed - that is the whole of what a tourniquet is - so what happens when it comes
     * off is exactly what it was stopping, and the mod says so rather than pretending
     * this is a treatment.
     */
    /**
     * Feeling for one, and saying what was found.
     *
     * <p>The only treatment in this mod that repairs nothing at all, and the only one
     * whose quality buys precision instead of outcome. A steady hold on the artery for
     * six beats comes away with the number; a hand that kept sliding off comes away
     * with "fast", which is most of what the number was for anyway. Nobody is ever made
     * worse by a clumsy hand on a wrist, and inventing a way for them to be - so that
     * this game would feel like the others - would have been a lie about what taking a
     * pulse is.
     *
     * <p>The line about nothing being open is the whole reason this exists. It is not a
     * diagnosis and the mod does not offer one: it is the second half of an observation
     * that a medic then has to put together themselves. A racing heart on a body with
     * no wound on it went somewhere, and the only place left is inside.
     */
    private static boolean pulse(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                 float quality) {
        Pulse reading = Pulse.of(body);
        boolean self = helper == patient;
        if (reading.character() == Pulse.ABSENT) {
            body.notePulse(reading, helper.getGameProfile().getName(), helper.level().getGameTime());
            Trend.say(helper, body, Reading.PULSE);
            Body.sync(patient);
            // Nothing under the fingers, and no amount of skill turns that into a
            // number. Said plainly, because this is the one reading that is an order.
            Body.notify(helper, Component.translatable(self
                            ? "triage.msg.pulse_none_self" : "triage.msg.pulse_none",
                    patient.getName()), ChatFormatting.DARK_RED, SoundEvents.WARDEN_HEARTBEAT);
            return true;
        }

        // Written on the notes before it is said out loud, because the saying is for
        // whoever is standing here and the writing is for whoever is not.
        body.notePulse(reading, helper.getGameProfile().getName(), helper.level().getGameTime());
        Trend.say(helper, body, Reading.PULSE);
        Body.sync(patient);

        boolean counted = quality >= 0.7F;
        Component found = counted
                ? Component.translatable("triage.msg.pulse_counted",
                        reading.rate(), reading.characterTitle())
                : Component.translatable("triage.msg.pulse_felt",
                        reading.band(), reading.characterTitle());
        Body.notify(helper, Component.translatable(self
                        ? "triage.msg.pulse_self" : "triage.msg.pulse_them",
                patient.getName(), found), reading.colour(), null);

        // And the observation that makes it worth having taken. Only when the heart is
        // running hard and there is genuinely nothing on them to explain it - a bleed
        // anybody can see needs no deduction, and saying this over one would train
        // people to ignore the line.
        // Ninety rather than a hundred: a torn spleen is noticeable long before it is
        // dramatic, and a tell that only fires once somebody is nearly dead is not a
        // tell. Nothing healthy sits here - pain alone tops out around eighty.
        // Fast *and* thin, not merely fast: pain alone can drive a heart to ninety with
        // no blood lost at all, and a tell that fires on a man with four broken bones is
        // a tell people learn to ignore. A thin pulse is the one that means volume.
        if (reading.rate() >= 90 && reading.character() < Pulse.STRONG && !open(body)) {
            Body.notify(helper, Component.translatable(self
                            ? "triage.msg.pulse_nothing_open_self" : "triage.msg.pulse_nothing_open"),
                    ChatFormatting.GRAY, null);
        }
        return true;
    }

    /**
     * Asking them where it hurts, and repeating what they said.
     *
     * <p>The mod repeats it and does not check it. That is the whole design: this is the
     * only reading here that is somebody's account rather than a measurement, and an
     * account that the game quietly corrected would not be an account. A rattled head
     * names something that hurts instead of the worst thing, and the medic is told that
     * the answer came from a rattled head - which is as much help as anybody gets in
     * life, and is enough.
     *
     * <p>The patient hears themselves being asked, because being asked is a thing that
     * happens to you.
     */
    /**
     * The one question with an answer that is not about pain.
     *
     * <p>Asked along with where it hurts, because it is the same question in life: a medic
     * kneeling over somebody asks what is wrong and what they cannot have, in that order
     * and in one breath. An unconscious patient answers neither - which is the whole of
     * why the allergy is interesting, and why the same dose is a treatment on a man who
     * can talk and a killing on the same man ten minutes later.
     */
    private static void allergyLine(ServerPlayer helper, ServerPlayer patient, BodyHealth body) {
        if (!Config.values().allergies || !body.allergy().any()) {
            return;
        }
        // A strip of metal saying the thing they cannot say. It is read off the body
        // rather than out of it, which is the whole of why anybody wears one: this is the
        // one line here that works on somebody who has stopped talking.
        if (wearsBracelet(patient, body)) {
            Body.notify(helper, Component.translatable("triage.msg.allergy_bracelet",
                    patient.getName(), body.allergy().title()), ChatFormatting.GOLD, null);
            return;
        }
        Body.notify(helper, Component.translatable(helper == patient
                        ? "triage.msg.allergy_own" : "triage.msg.allergy_says",
                patient.getName(), body.allergy().title()), ChatFormatting.GOLD, null);
    }

    /** Whether they are carrying one, stamped with what is actually true of them. */
    private static boolean wearsBracelet(ServerPlayer patient, BodyHealth body) {
        for (ItemStack stack : patient.getInventory().items) {
            if (stack.getItem() instanceof Bracelet && Bracelet.stamped(stack) == body.allergy()) {
                // A band that names their group names it to whoever is reading the band,
                // which is the whole point of wearing one. This is the only finding in the
                // mod established by an object rather than by somebody's hands.
                if (Bracelet.group(stack) == body.bloodType() && body.bloodType() != null) {
                    body.establishWhole(Findings.GROUP);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Out of ten.
     *
     * <p>Nothing is repaired and nothing can go wrong. What comes back is a number and
     * whatever the mod can say about whether to believe it - and the second half is the
     * half worth having, because the number alone is only ever an opinion.
     */
    private static boolean score(ServerPlayer helper, ServerPlayer patient, BodyHealth body) {
        boolean self = helper == patient;
        int said = Pain.said(body, helper.getRandom());
        if (said < 0) {
            Body.notify(helper, Component.translatable(self
                    ? "triage.msg.score_silent_self" : "triage.msg.score_silent",
                    patient.getName()), ChatFormatting.GRAY, null);
            return true;
        }
        Body.notify(helper, Component.translatable(self ? "triage.msg.score_self" : "triage.msg.score_them",
                        patient.getName(), said, Pain.WORST, Pain.title(said)),
                Pain.colour(said), null);
        // Written on the notes, which it never was. An opinion nobody wrote down cannot
        // be compared with the same opinion twenty minutes later, and comparing it is the
        // only thing a pain score has ever been for.
        body.noteSaid(said, helper.getGameProfile().getName(), helper.level().getGameTime());
        Trend.say(helper, body, Reading.PAIN);
        // The line the whole reading exists for. Said to whoever asked and not to the
        // patient, because it is a thing worked out rather than a thing they said.
        if (Config.values().pulse && Pain.disagrees(body, said, Pulse.of(body))) {
            Body.notify(helper, Component.translatable("triage.msg.score_quiet"),
                    ChatFormatting.GOLD, null);
        }
        return true;
    }

    private static boolean ask(ServerPlayer helper, ServerPlayer patient, BodyHealth body) {
        boolean self = helper == patient;
        // Asked in the same breath as where it hurts, because that is when it is asked.
        allergyLine(helper, patient, body);
        // And which blood they have, which is the other thing a person knows about
        // themselves and can say while they are still able to say anything. A patient who
        // has answered once has answered: the chart keeps it, the way a chart does.
        if (body.bloodType() != null && Complaint.ask(body, helper.getRandom()).kind() != Complaint.SILENT) {
            body.establishWhole(Findings.GROUP);
        }
        Complaint.Answer answer = Complaint.ask(body, helper.getRandom());

        if (answer.kind() == Complaint.UNHEARD) {
            Body.notify(helper, Component.translatable(self
                    ? "triage.msg.ask_unheard_self" : "triage.msg.ask_unheard",
                    patient.getName()), ChatFormatting.GRAY, null);
            return true;
        }
        if (answer.kind() == Complaint.SILENT) {
            // Not a refusal. A patient who cannot say where it hurts is a patient you
            // now have to find it on, and knowing that is worth the four seconds.
            Body.notify(helper, Component.translatable(self
                    ? "triage.msg.ask_silent_self" : "triage.msg.ask_silent",
                    patient.getName()), ChatFormatting.GRAY, null);
            return true;
        }
        // The jaw, before anything they said with it. This is the only warning lockjaw
        // ever gives while it is still treatable, and it is deliberately not a reading:
        // no instrument in this mod finds it, no chart shows it, and a medic who never
        // talks to anybody never sees it at all. Asking a patient a question is the
        // oldest diagnostic there is and this is the mod finally paying for it.
        jaw(helper, patient, body, self);

        if (answer.kind() == Complaint.NUMB) {
            Body.notify(helper, Component.translatable(self
                    ? "triage.msg.ask_numb_self" : "triage.msg.ask_numb",
                    patient.getName()), ChatFormatting.GRAY, null);
            // And the reason, when there is one worth knowing - because it is a reason
            // the person asking probably caused.
            if (body.painkilled()) {
                Body.notify(helper, Component.translatable("triage.msg.ask_dosed"),
                        ChatFormatting.DARK_GRAY, null);
            }
            return true;
        }

        BodyPart part = answer.part();
        // A referred complaint is said in its own words, and the words are the only help
        // anybody gets: it is the top of the shoulder rather than the arm. Nothing marks
        // it as suspect, because the patient is not suspect - they are telling the truth
        // about what they feel. The chart is what disagrees with them.
        boolean referred = answer.kind() == Complaint.REFERRED;
        Component said = Component.translatable(switch (answer.severity()) {
            case Complaint.AGONY -> referred ? "triage.msg.ask_agony_tip" : "triage.msg.ask_agony";
            case Complaint.SORE -> referred ? "triage.msg.ask_sore_tip" : "triage.msg.ask_sore";
            default -> referred ? "triage.msg.ask_ache_tip" : "triage.msg.ask_ache";
        }, part.title());
        Body.notify(helper, Component.translatable(self
                        ? "triage.msg.ask_said_self" : "triage.msg.ask_said",
                patient.getName(), said),
                answer.severity() == Complaint.AGONY ? ChatFormatting.RED : ChatFormatting.YELLOW, null);

        // Said plainly rather than hidden, because a medic can see that somebody is not
        // making sense. What they cannot see is which part of the answer to throw away.
        if (answer.kind() == Complaint.MUDDLED) {
            Body.notify(helper, Component.translatable(self
                            ? "triage.msg.ask_muddled_self" : "triage.msg.ask_muddled"),
                    ChatFormatting.DARK_GRAY, null);
        }
        if (!self) {
            Body.notify(patient, Component.translatable("triage.msg.ask_asked",
                    helper.getName()), ChatFormatting.GRAY, null);
        }
        return true;
    }

    /** Whether anything on this body is visibly bleeding or strapped shut. */
    private static boolean open(BodyHealth body) {
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (state.bleeding() || state.tourniquet) {
                return true;
            }
        }
        return false;
    }

    private static boolean unstrap(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                   @Nullable BodyPart part) {
        BodyPart target = strappedPart(body, part);
        if (target == null) {
            return refuse(helper, "triage.msg.nothing_strapped");
        }
        PartState state = body.part(target);
        // How long it has been on. There is no separate clock for that and there does not
        // need to be: the one that has been killing the limb has been running the whole
        // time, and it is the same number - which is the neatest thing about this injury.
        // The clock that is costing them the leg is the clock that decides whether taking
        // the strap off costs them the rest of it.
        int held = state.deadTicks;
        state.tourniquet = false;
        Necrosis.released(patient, body, state);
        crush(helper, patient, body, target, held);
        body.setDirty(true);
        boolean running = state.bleeding();
        Body.notify(helper, Component.translatable(running
                        ? "triage.msg.unstrapped_open" : "triage.msg.unstrapped_hand", target.title()),
                running ? ChatFormatting.DARK_RED : ChatFormatting.GRAY, SoundEvents.LEASH_KNOT_BREAK);
        if (helper != patient && running) {
            Body.notify(patient, Component.translatable("triage.msg.unstrapped_you", target.title()),
                    ChatFormatting.DARK_RED, null);
        }
        return true;
    }

    private static @Nullable BodyPart dressedPart(BodyHealth body, @Nullable BodyPart chosen) {
        if (chosen != null) {
            return body.part(chosen).dressed ? chosen : null;
        }
        return body.worst(part -> body.part(part).dressed);
    }

    private static @Nullable BodyPart strappedPart(BodyHealth body, @Nullable BodyPart chosen) {
        if (chosen != null) {
            return body.part(chosen).tourniquet ? chosen : null;
        }
        return body.worst(part -> body.part(part).tourniquet);
    }

    /**
     * Onto their side, and left there.
     *
     * <p>The only thing in this class that goes on working after the hands have gone. An
     * airway held open buys twenty seconds and has to be found again; a body on its side
     * closes at half the rate for as long as nobody moves it. That is the difference
     * between a treatment and a position, and it is the whole reason the recovery
     * position is the first thing anybody is taught to do with an unconscious casualty.
     */
    private static boolean roll(ServerPlayer helper, ServerPlayer patient) {
        if (onSide(patient)) {
            return refuse(helper, "triage.msg.already_rolled");
        }
        ON_SIDE.add(patient.getUUID());
        Body.notify(helper, Component.translatable("triage.msg.rolled", patient.getName()),
                ChatFormatting.AQUA, SoundEvents.WOOL_PLACE);
        if (helper != patient) {
            Body.notify(patient, Component.translatable("triage.msg.rolled_you", helper.getName()),
                    ChatFormatting.AQUA, SoundEvents.WOOL_PLACE);
        }
        return true;
    }

    /**
     * Holding a slack airway open.
     *
     * <p>A clean job puts the count back to nothing and buys the whole grace period; a
     * sloppy one gives back only part of it, so the head has to be found again sooner.
     * Worth doing before it has closed as well as after - refusing an early one would
     * teach exactly the wrong lesson about the one thing here that has to be repeated.
     */
    private static boolean airway(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                  Config.Values config, float quality) {
        boolean wasBlocked = blocked(body, config);
        int grace = config.airwaySeconds * 20;
        body.openAirway();
        body.tickAirway(Math.round(grace * (1.0F - Mth.clamp(quality, 0.0F, 1.0F))));
        Body.notify(helper, Component.translatable("triage.msg.airway_opened", patient.getName()),
                ChatFormatting.AQUA, SoundEvents.PLAYER_BREATH);
        if (wasBlocked) {
            Body.notify(patient, Component.translatable("triage.msg.airway_opened_you", helper.getName()),
                    ChatFormatting.AQUA, null);
        }
        return true;
    }

    /**
     * Pushing the blood round by hand.
     *
     * <p>This does not restart the heart, and refusing to let it is the design rather
     * than a shortcut. Compressions that occasionally stood somebody up would make the
     * surgical kit optional and the arrest a slot machine; compressions that only ever
     * buy time make it a job somebody has to keep doing while somebody else runs for the
     * kit. That is two people, which is what the mod is for.
     *
     * <p>They cost the patient their ribs, because that is what they cost. A chest
     * pushed hard enough to move blood is a chest that cracks, and every round is
     * another go at it - so "keep going" is not a free answer either. The rib is rolled
     * whatever the rhythm was: a badly timed push is still a push.
     */
    private static boolean compressions(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                        Config.Values config, float quality) {
        // Each round is worth less than the one before, the same way each dressing on
        // the same downing is. Without that, one pair of hands and enough patience is
        // immortality and the arrest stops being an ending at all.
        int worth = body.compressionWorth(config.arrestSeconds * 20, config.cprFalloff);
        int granted = scaled(worth, quality);
        body.compress(granted);

        PartState chest = body.part(BodyPart.THORAX);
        if (helper.getRandom().nextFloat() < config.cprRibChance
                && chest.fracture(BodyPart.THORAX.bone().count())) {
            body.setDirty(true);
            Body.notify(patient, Component.translatable("triage.msg.cpr_ribs"),
                    ChatFormatting.RED, SoundEvents.BONE_BLOCK_BREAK);
            Body.notify(helper, Component.translatable("triage.msg.cpr_ribs_them", patient.getName()),
                    ChatFormatting.GRAY, null);
        }

        int seconds = granted / 20;
        Body.notify(helper, Component.translatable("triage.msg.compressions", patient.getName(), seconds),
                ChatFormatting.AQUA, null);
        Body.notify(patient, Component.translatable("triage.msg.compressions_you", helper.getName(), seconds),
                ChatFormatting.AQUA, null);
        // The only beat there is. Everybody near enough to help can hear it, and it is
        // the one thing in the mod that says "somebody is working on them" out loud.
        patient.level().playSound(null, patient.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS, 0.8F, 0.7F);
        Milestones.onRevived(helper);
        return true;
    }

    /**
     * A hand on the wound.
     *
     * <p>Aimed for them rather than chosen: hands go where the blood is, and asking a
     * player to pick a limb off a list while somebody empties out in front of them would
     * be a menu standing in for an instinct. The worst bleed wins, and a strapped limb
     * is skipped because a strap is already doing this job, worse.
     *
     * <p>It stops nothing permanently. A heavy bleed comes back the moment the hand comes
     * off, which is the honest version and also the useful one: it is the walk to the
     * bandage, bought at the price of standing there.
     */
    private static boolean pressure(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                    Config.Values config, @Nullable BodyPart chosen, float quality) {
        BodyPart worst = pressPart(body, chosen);
        if (worst == null) {
            return refuse(helper, "triage.msg.nothing_bleeding");
        }
        PartState state = body.part(worst);
        state.pressedTicks = scaled(config.pressureSeconds * 20, quality);
        body.setDirty(true);
        Body.notify(helper, Component.translatable("triage.msg.pressure",
                        worst.title(), state.pressedTicks / 20),
                ChatFormatting.AQUA, SoundEvents.WOOL_HIT);
        Body.notify(patient, Component.translatable("triage.msg.pressure_you",
                        helper.getName(), worst.title()),
                ChatFormatting.AQUA, null);
        return true;
    }

    /**
     * Getting the shaft out.
     *
     * <p>It always comes out - you pulled it, and a hand that has hold of an arrow does
     * not put it back. What the work decides is what it takes with it. Done carefully it
     * leaves an ordinary cut; jerked out, it tears, and the patient is worse off than
     * before somebody helped. That is the honest shape of the thing and it is why this
     * is worth a game rather than a button.
     *
     * <p>Nothing else in the mod can be made worse by trying, which is exactly why this
     * one should be: an arrow in a leg is the one injury where doing nothing is a real
     * option and waiting for somebody steadier is a real plan.
     */
    private static boolean pull(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                Config.Values config, @Nullable BodyPart chosen, float quality) {
        BodyPart part = pullPart(body, chosen);
        if (part == null) {
            return refuse(helper, "triage.msg.nothing_stuck");
        }
        PartState state = body.part(part);
        // A weapon first, and on its own: it is one object rather than a handful, it is
        // the thing holding the wound shut, and what it leaves behind when it comes out
        // with bare hands is the worst bleed in the mod. Anybody can do this. That is the
        // problem with it - it is always available, and it is almost never the right
        // moment, and the game the helper just played is the only thing standing between
        // the patient and an artery.
        if (state.bladeIn()) {
            boolean steady = quality >= 0.75F;
            ItemStack blade = Stuck.take(patient, body, part,
                    steady ? PartState.BLEED_HEAVY : PartState.BLEED_ARTERIAL, !steady, config);
            Stuck.hand(helper, blade);
            Body.notify(helper, Component.translatable(steady
                            ? "triage.msg.blade_out" : "triage.msg.blade_out_badly",
                            blade.getHoverName(), part.title()),
                    steady ? ChatFormatting.YELLOW : ChatFormatting.RED,
                    steady ? SoundEvents.ITEM_PICKUP : SoundEvents.PLAYER_HURT);
            if (helper != patient) {
                Body.notify(patient, Component.translatable("triage.msg.blade_out_you",
                                blade.getHoverName(), part.title()),
                        ChatFormatting.RED, null);
            }
            return true;
        }
        // Read before pulling, because the last one out clears the word.
        boolean fragment = state.shrapnel;
        boolean bullet = state.bullets > 0;
        if (fragment) {
            // Fragments come out together, because that is one job. Opening a wound five
            // times to take five pieces out of it would be five wounds, and the game the
            // helper just played was about the whole handful.
            while (state.pull()) {
                // Until the wound is empty.
            }
        } else {
            state.pull();
        }
        // Bullets come out together for the same reason fragments do: one wound, opened
        // once, and however many rounds are in it come out with it.
        if (bullet) {
            while (state.pullBullet()) {
                // Until the wound is empty.
            }
        }
        boolean clean = quality >= 0.5F;
        // A clean pull leaves a cut that will close on its own. A torn one does not, and
        // that is a heavy bleed, which is the thing this mod takes most seriously.
        state.startBleeding(clean ? PartState.BLEED_LIGHT : PartState.BLEED_HEAVY,
                config.lightBleedSeconds * 20);
        if (!clean) {
            // A bullet is short and sits under the skin, so a bad cut-out tears more of
            // what is around it than a shaft does on its way back out through its own
            // hole. The two have separate prices because they are separate jobs.
            float tear = fragment ? config.arrowTear : bullet ? config.bulletTear : config.arrowTear;
            Injuries.apply(patient, body, part, tear, false);
        }
        body.setDirty(true);

        boolean bulletOnly = bullet && !fragment;
        Body.notify(helper, Component.translatable(clean
                        ? (fragment ? "triage.msg.pulled_fragment"
                                : bulletOnly ? "triage.msg.pulled_bullet" : "triage.msg.pulled")
                        : (fragment ? "triage.msg.pulled_fragment_badly"
                                : bulletOnly ? "triage.msg.pulled_bullet_badly" : "triage.msg.pulled_badly"),
                        part.title()),
                clean ? ChatFormatting.AQUA : ChatFormatting.RED,
                clean ? SoundEvents.ITEM_PICKUP : SoundEvents.PLAYER_HURT);
        if (helper != patient) {
            Body.notify(patient, Component.translatable(
                            (bulletOnly
                                    ? clean ? "triage.msg.pulled_bullet_you"
                                            : "triage.msg.pulled_bullet_badly_you"
                                    : clean ? "triage.msg.pulled_you" : "triage.msg.pulled_badly_you"),
                            helper.getName(), part.title()),
                    clean ? ChatFormatting.AQUA : ChatFormatting.RED, null);
        }
        return true;
    }

    /**
     * The limb a shaft is coming out of: the one that was clicked, if it still has one.
     *
     * <p>A null choice means nobody named a limb - a command, or anything else without a
     * chart in front of it - and then the worst one decides, which is what this did
     * before the chart learned to point.
     */
    private static @Nullable BodyPart pullPart(BodyHealth body, @Nullable BodyPart chosen) {
        if (chosen != null) {
            return body.part(chosen).stuck() ? chosen : null;
        }
        BodyPart worst = null;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!state.stuck()) {
                continue;
            }
            if (worst == null || state.arrows + state.bullets
                    > body.part(worst).arrows + body.part(worst).bullets) {
                worst = part;
            }
        }
        return worst;
    }

    /**
     * Hands on a joint that has stopped going where it is put.
     *
     * <p>Everything that decides how much this was worth has already happened on the
     * screen: how close to the edge the hands got, and whose hands they were. All that is
     * left here is to write it down - and to charge for a session that went past the
     * joint rather than to it, which is the one way this can leave somebody worse.
     */
    private static boolean stretch(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                   @Nullable BodyPart part, float quality) {
        BodyPart target = stiffPart(body, part);
        if (target == null) {
            return refuse(helper, "triage.msg.nothing_stiff");
        }
        Scars.stretch(helper, patient, body, target, quality);
        return true;
    }

    /** Hands on a bone that is out of line. Everything about it is decided on the screen. */
    private static boolean setBone(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                   @Nullable BodyPart part, float quality) {
        BodyPart target = bentPart(body, part);
        if (target == null) {
            return refuse(helper, "triage.msg.nothing_displaced");
        }
        Bones.set(helper, patient, body, target, quality);
        return true;
    }

    /** The bone that wants pulling straight: the one that was clicked, or the worst one. */
    private static @Nullable BodyPart bentPart(BodyHealth body, @Nullable BodyPart chosen) {
        if (chosen != null) {
            return Bones.worthSetting(body.part(chosen)) ? chosen : null;
        }
        for (BodyPart part : BodyPart.VALUES) {
            if (Bones.worthSetting(body.part(part))) {
                return part;
            }
        }
        return null;
    }

    /** And the one that is refused, so the refusal can say which of the two it is. */
    private static @Nullable BodyPart openPart(BodyHealth body, @Nullable BodyPart chosen) {
        if (chosen != null) {
            return body.part(chosen).openFracture() ? chosen : null;
        }
        for (BodyPart part : BodyPart.VALUES) {
            if (body.part(part).openFracture()) {
                return part;
            }
        }
        return null;
    }

    /**
     * The stiffest thing on this body that has not had its session today.
     *
     * <p>A day is not a cooldown dressed up. It is the actual shape of the treatment:
     * what loosens a contracture is the same short session repeated over weeks, and a
     * player who could do six in a minute would not be doing physiotherapy, they would
     * be dragging a slider.
     */
    private static @Nullable BodyPart stiffPart(BodyHealth body, @Nullable BodyPart chosen) {
        if (chosen != null) {
            PartState state = body.part(chosen);
            return Scars.worthStretching(state) ? chosen : null;
        }
        BodyPart worst = null;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!Scars.worthStretching(state)) {
                continue;
            }
            if (worst == null || state.stiff > body.part(worst).stiff) {
                worst = part;
            }
        }
        return worst;
    }

    /** The wound a hand goes on: the one that was clicked, or the worst one running. */
    private static @Nullable BodyPart pressPart(BodyHealth body, @Nullable BodyPart chosen) {
        if (chosen != null) {
            return body.part(chosen).losingBlood() ? chosen : null;
        }
        BodyPart worst = null;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!state.losingBlood()) {
                continue;
            }
            if (worst == null || state.bleed > body.part(worst).bleed) {
                worst = part;
            }
        }
        return worst;
    }

    /** What an attempt of that quality is worth. */
    private static int scaled(int full, float quality) {
        return Math.max(1, Math.round(full * (FLOOR + (1.0F - FLOOR) * Mth.clamp(quality, 0.0F, 1.0F))));
    }

    /**
     * One second of an unattended airway, and the noise it makes.
     *
     * <p>The sound is the point. Everything else the mod knows about a body is private
     * to that body and has to be gone and looked at on purpose - but somebody who cannot
     * breathe is not a fact you read off a chart, it is a noise from the floor, and
     * anybody near enough to do something about it is near enough to hear it.
     */
    public static void tick(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        if (!body.downed()) {
            // Standing up is getting off your side, and so is dying. Either way the
            // posture stops being a fact about anybody.
            ON_SIDE.remove(player.getUUID());
            return;
        }
        if (!config.airway) {
            return;
        }
        // On their side, everything in the mouth goes the way gravity takes it, and
        // gravity has stopped taking it into the throat.
        body.tickAirway(onSide(player) ? Math.max(1, interval / 2) : interval);
        if (!blocked(body, config)) {
            return;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BREATH,
                SoundSource.PLAYERS, 0.7F, 0.6F);
    }

    /** Whether air has stopped getting in on its own. */
    public static boolean blocked(BodyHealth body, Config.Values config) {
        return config.airway && body.airwayBlocked(config.airwaySeconds * 20);
    }

    private static boolean refuse(ServerPlayer helper, String key) {
        Body.notify(helper, Component.translatable(key), ChatFormatting.GRAY, null);
        return false;
    }

    /**
     * Hands along a bone, and what they find.
     *
     * <p>What comes back is deliberately coarse: broken or not, and roughly how badly. Not
     * a count, not which way it went - a hand can feel that a bone grates and cannot feel
     * that it grates in three places, and the machine that can is the X-ray. What the hand
     * does have over the machine is that it is a hand: it is here, it is now, and it costs
     * nothing but the patient.
     *
     * <p>A limb that is swollen with a fresh bleed under it answers wrongly, and it answers
     * wrongly in the direction that gets somebody splinted who did not need it. That is not
     * a punishment for playing badly; it is what an examination is worth on a limb like
     * that, and it is why the film exists.
     */
    private static boolean feel(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                @Nullable BodyPart part, float quality) {
        if (part == null) {
            return refuse(helper, "triage.msg.feel_nowhere");
        }
        PartState state = body.part(part);
        boolean self = helper == patient;
        long now = patient.level().getGameTime();

        // Hands that learnt nothing. Too light, too quick, or your own hand on your own
        // fracture, which stops before it has pressed hard enough to tell.
        float reach = self ? quality * 0.7F : quality;
        if (reach < 0.45F) {
            Body.notify(helper, Component.translatable("triage.msg.feel_nothing", part.title()),
                    ChatFormatting.GRAY, null);
            hurt(patient, state, 0.3F);
            return true;
        }

        // What the hand is entitled to find. A clot is a hard cord in a limb and a fragment
        // near the surface is a lump; both are things fingers genuinely find, and both are
        // in the mod already with no other way of finding them in the field.
        int facts = Findings.BONE;
        if (state.shrapnel && reach > 0.75F) {
            facts |= Findings.METAL;
        }
        if (state.clot && reach > 0.7F) {
            facts |= Findings.CLOT;
        }
        body.establish(part, facts, now);

        boolean grates = state.fractured() || state.cracked;
        // A limb still swollen from a bleed that has not clotted answers the same way a
        // broken one does, and there is no telling the two apart with a hand.
        boolean misleading = !grates && state.bleeding() && reach < 0.8F;
        Body.notify(helper, Component.translatable(
                        grates || misleading ? "triage.msg.feel_broken" : "triage.msg.feel_sound",
                        part.title()),
                grates || misleading ? ChatFormatting.GOLD : ChatFormatting.GREEN, null);

        // And the price. A press hard enough to tell is a press hard enough to hurt, and on
        // a break that is still fresh it is hard enough to move it.
        hurt(patient, state, self ? 0.6F : 1.0F);
        if (!self && state.fractured() && !state.splint && state.crackTicks == 0
                && reach > 0.9F && patient.getRandom().nextFloat() < 0.25F) {
            Bones.rebreak(helper, patient, body, part);
            Body.notify(helper, Component.translatable("triage.msg.feel_moved", part.title()),
                    ChatFormatting.RED, null);
        }
        Body.sync(patient);
        return true;
    }

    /** What an examination costs the person being examined. */
    private static void hurt(ServerPlayer patient, PartState state, float share) {
        if (share <= 0.0F) {
            return;
        }
        Reactions.say(patient, "feel");
    }

    /**
     * The sweep, and what it is allowed to have found.
     *
     * <p>Every part gets its <em>state</em> - how much of it is left - and not one gets its
     * diagnosis. That split is the whole of why this action is worth having: afterwards the
     * chart has seven numbers on it and seven blanks beside them, and the blanks are the
     * question the next four minutes answer.
     *
     * <p>A hurried sweep misses the quiet ones. What it keeps is whatever was obvious - the
     * parts that are visibly bad - and what it drops is the leg somebody is not complaining
     * about, which in this mod as in life is the one that kills them an hour later.
     */
    private static boolean survey(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                                  float quality) {
        boolean self = helper == patient;
        long now = patient.level().getGameTime();
        // Your own sweep over your own body is worth less: you cannot look at your own back
        // and you are not going to press your own broken leg the way somebody else will.
        float reach = self ? quality * 0.75F : quality;
        if (reach < 0.3F) {
            Body.notify(helper, Component.translatable("triage.msg.survey_rushed"),
                    ChatFormatting.GRAY, null);
            return true;
        }

        int found = 0;
        int missed = 0;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            // A part that is plainly wrong is caught however fast the hands went over it.
            // A part that is quietly wrong is caught if they went over it properly.
            boolean obvious = state.bleeding() || state.gone() || state.blacked()
                    || state.burned || state.displaced || state.openBreak;
            if (obvious || reach > 0.62F) {
                body.establish(part, Findings.FLESH, now);
                if (state.ratio() < 0.999F) {
                    found++;
                }
            } else if (state.ratio() < 0.9F) {
                missed++;
            }
        }
        Body.sync(patient);

        Body.notify(helper, Component.translatable(
                        found == 0 ? "triage.msg.survey_clear"
                                : self ? "triage.msg.survey_self" : "triage.msg.survey_them",
                        patient.getName(), found),
                found == 0 ? ChatFormatting.GREEN : ChatFormatting.GOLD, null);
        // And the honest admission, which is the half of this that teaches anything: hands
        // that hurried do not know what they went past.
        if (missed > 0) {
            Body.notify(helper, Component.translatable("triage.msg.survey_quick"),
                    ChatFormatting.GRAY, null);
        }
        return true;
    }

    /**
     * What comes back out of a limb that has had nothing going through it.
     *
     * <p>Not a punishment for using a tourniquet - a strap is still the only answer to an
     * artery, and leaving it on is still how a limb dies. It is a price on <em>when</em>,
     * and it is the one price in this mod that is paid by doing the right thing too late.
     *
     * <p>The way out is the one a medic actually uses: fill them up first. A patient who is
     * not short of volume takes what comes back out of that leg diluted, and nothing
     * happens. A patient who is already down on blood takes it neat.
     */
    private static void crush(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                              BodyPart part, int held) {
        Config.Values config = Config.values();
        if (held < config.crushStrapSeconds * 20 || body.bloodLoss() <= 0.15F) {
            return;
        }
        if (!body.catchDisease(Disease.CRUSH)) {
            return;
        }
        Body.notify(helper, Component.translatable("triage.msg.crush_released", part.title()),
                ChatFormatting.RED, null);
        if (helper != patient) {
            Body.notify(patient, Component.translatable("triage.msg.crush_felt"),
                    ChatFormatting.RED, null);
        }
        Reactions.say(patient, "crush");
    }

    /**
     * A mouth that is not opening properly, said by the person it belongs to.
     *
     * <p>Only while the illness is silent - once the spasms start there is nothing subtle
     * left to notice - and only to somebody who asked. It says nothing about what it is:
     * a stiff jaw is a stiff jaw, and turning it into "they have tetanus" would be the
     * mod doing the one piece of thinking the realistic difficulty exists to hand back.
     */
    private static void jaw(ServerPlayer helper, ServerPlayer patient, BodyHealth body,
                            boolean self) {
        if (!body.has(Disease.TETANUS)) {
            return;
        }
        int had = body.diseaseTicks(Disease.TETANUS);
        if (had >= Config.values().tetanusSeconds * 20) {
            return;
        }
        // Not from the first second. Something that appeared the moment it was caught
        // would be an alarm rather than a symptom, and this illness is not allowed one.
        if (had < 600) {
            return;
        }
        Body.notify(helper, Component.translatable(self
                        ? "triage.msg.jaw_self" : "triage.msg.jaw", patient.getName()),
                ChatFormatting.GRAY, null);
    }
}
