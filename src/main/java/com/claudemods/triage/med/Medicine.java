package com.claudemods.triage.med;

import com.claudemods.triage.body.Mending;
import com.claudemods.triage.body.Injuries;
import com.claudemods.triage.Config;
import com.claudemods.triage.body.BloodType;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.Complaint;
import com.claudemods.triage.body.Adrenaline;
import com.claudemods.triage.body.Chill;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.Findings;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.Breathing;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.body.Fatigue;
import com.claudemods.triage.body.Downed;
import com.claudemods.triage.body.Necrosis;
import com.claudemods.triage.body.Reactions;
import com.claudemods.triage.body.Trend;
import com.claudemods.triage.body.Reading;
import com.claudemods.triage.item.SeveredLimb;
import com.claudemods.triage.body.Severed;
import com.claudemods.triage.body.Stuck;
import com.claudemods.triage.body.PartEffects;
import com.claudemods.triage.body.Bones;
import com.claudemods.triage.body.Clots;
import com.claudemods.triage.body.PartState;
import com.claudemods.triage.body.Scars;
import com.claudemods.triage.body.Temperature;
import com.claudemods.triage.body.Sick;
import com.claudemods.triage.data.Milestones;
import com.claudemods.triage.item.BloodBag;
import com.claudemods.triage.item.MedicineItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a treatment actually does to a body, once the player has earned it.
 *
 * <p>The mod has its own medical items, and the config list is for pointing this at
 * somebody else's - a pack with a real medical mod can map those instead and never
 * craft a bandage of ours.
 */
public final class Medicine {
    /**
     * What a reading has to be worth to come back as a figure rather than a band.
     *
     * <p>Just under one rather than one, for floats. See {@link #thermometer}.
     */
    private static final float EXACT_READING = 0.995F;
    /** And under this, the column in it was somebody else's. */
    private static final float UNSHAKEN_READING = 0.32F;
    private static final int USE_COOLDOWN = 20;

    private Medicine() {
    }

    /** What this item counts as, or null when it is not medicine. */
    public static @Nullable MedicineKind kindOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof MedicineItem medicine) {
            return medicine.kind(stack);
        }
        for (MedicineKind kind : MedicineKind.VALUES) {
            for (Config.ItemMatcher matcher : Config.values().medicine(kind)) {
                if (matcher.matches(stack)) {
                    return kind;
                }
            }
        }
        return null;
    }

    /** Whether there is anything on this body for that kind of medicine to do. */
    public static boolean hasWork(BodyHealth body, MedicineKind kind) {
        // Somebody on the floor is work for a dressing whatever else is or is not wrong
        // with them: what it does for them is buy time, not close a particular wound.
        if (body.downed() && kind.canStabilise()) {
            return true;
        }
        if (kind == MedicineKind.ANTIBIOTICS) {
            // Not merely "ill": a body whose only complaint is a concussion has nothing
            // a course can do, and offering the screen would promise otherwise.
            return body.worstDisease() != null;
        }
        if (kind == MedicineKind.TRANSFUSION) {
            return body.bloodLoss() > 0.0F;
        }
        if (kind == MedicineKind.ANTICOAGULANT) {
            // A clot in a limb, or one that has already gone. Nothing otherwise, because
            // thinning the blood of somebody who is bleeding is the one use of this that
            // the mod is not going to hand anybody a screen for.
            return Clots.worthThinning(body);
        }
        if (kind == MedicineKind.ANAESTHETIC) {
            // Nothing for somebody already under: a second mask over the first buys no
            // more quiet and is exactly how an overdose happens.
            return Config.values().anaesthesia && !body.under();
        }
        if (kind == MedicineKind.ANTIPSYCHOTIC) {
            // Nothing for a body that does not have it, and nothing for one already
            // holding a course: a second card on top of the first buys no more quiet.
            return body.has(Disease.SCHIZOPHRENIA) && !body.medicated();
        }
        if (kind == MedicineKind.DRAW) {
            // There is always work in an empty bag: whether this particular donor can
            // spare any is decided when it is drawn, with a reason attached.
            return true;
        }
        if (!kind.needsPart()) {
            return true;
        }
        for (BodyPart part : BodyPart.VALUES) {
            if (kind.canTreat(part, body.part(part))) {
                return true;
            }
        }
        return false;
    }

    /** The part that needs this kind of medicine most, or null when none does. */
    public static @Nullable BodyPart autoTarget(BodyHealth body, MedicineKind kind) {
        if (kind == MedicineKind.BANDAGE) {
            return worstBleeding(body);
        }
        return body.worst(part -> kind.canTreat(part, body.part(part)));
    }

    /**
     * Carries the treatment out and spends the item. The part is checked again here:
     * the screen that picked it runs on the client, and a body can change between the
     * two - or the packet can simply be a lie.
     *
     * @return true when the item was used
     */
    public static boolean applyTo(ServerPlayer healer, ServerPlayer patient, MedicineKind kind,
                                  @Nullable BodyPart part, ItemStack stack, float quality) {
        BodyHealth body = Body.of(patient);
        Config.Values config = Config.values();

        // Where this is happening, what it is being done with, and who else has hold of
        // the patient. Worked out before the treatment rather than after, because a hand
        // is steadier or shakier for the whole of a job and not only at the end of one.
        ServerPlayer assistant = Theatre.assistant(patient, healer);
        // What the screen actually earned, before any of that is applied to it.
        //
        // One thing in this list is not a treatment. A thermometer reads what it reads;
        // a tidy room and a rested pair of hands do not move a column of mercury, and
        // running the reading through the same multiplier as a surgeon's stitch meant a
        // flawless reading taken in a field could never be worth a figure. What decides
        // this one is on the screen and nowhere else: shaken down, and held long enough.
        float reading = Mth.clamp(quality, 0.0F, 1.0F);
        quality = Mth.clamp(quality * Theatre.steadiness(patient, healer, assistant), 0.0F, 1.0F);

        // A body on the floor gets packed, whatever the dressing was going to be used
        // for otherwise. No part is chosen and no screen is earned: this is the thing
        // you do in ten seconds with something walking towards you, and asking for a
        // minigame here would be asking for the wrong thing.
        if (body.downed() && kind.canStabilise()) {
            if (!stabilise(healer, patient, body, config)) {
                return false;
            }
        } else if (!kind.needsPart()) {
            if (reacts(healer, patient, body, kind, config)) {
                return true;
            }
            boolean worked = switch (kind) {
                case PAINKILLER -> painkiller(healer, patient, body, config);
                case ANTIBIOTICS -> antibiotics(healer, patient, body);
                case WORMING -> worming(healer, patient, body);
                case TRANSFUSION -> transfusion(healer, patient, body, stack, config);
                case DRAW -> draw(healer, patient, body, config);
                case ANTIPSYCHOTIC -> antipsychotic(healer, patient, body, config);
                case ANAESTHETIC -> anaesthetic(healer, patient, body, config, quality);
                case EPINEPHRINE -> epinephrine(healer, patient, body, config, quality);
                case MORPHINE -> morphine(healer, patient, body, config, quality);
                case NALOXONE -> naloxone(healer, patient, body, config, quality);
                case THERMOMETER -> thermometer(healer, patient, body, reading);
                case STETHOSCOPE -> listen(healer, patient, body, reading);
                case ANTICOAGULANT -> Clots.thin(healer, patient, body, config, quality);
                default -> throw new IllegalStateException("unreachable: " + kind);
            };
            if (!worked) {
                return false;
            }
        } else {
            if (part == null || !kind.canTreat(part, body.part(part))) {
                refuse(healer, nothingToDoKey(kind, part, body));
                return false;
            }
            switch (kind) {
                case BANDAGE -> bandage(healer, patient, body, part, config);
                case TOURNIQUET -> tourniquet(healer, patient, body, part);
                case SPLINT -> splint(healer, patient, body, part);
                case MEDKIT -> medkit(healer, patient, body, part, config);
                case SURGERY -> surgery(healer, patient, body, part, config, quality);
                case ANTISEPTIC -> antiseptic(healer, patient, body, part);
                case SALVE -> salve(healer, patient, body, part, config);
                case DECOMPRESS -> decompress(healer, patient, body, part);
                case SCISSORS -> cutAway(healer, patient, body, part, quality);
                case EXTRACT -> extract(healer, patient, body, part, config, quality);
                case AMPUTATE -> amputate(healer, patient, body, part, config, quality);
                case GRAFT -> graft(healer, patient, body, part, stack, config, quality);
                case PEG, HOOK -> fit(healer, patient, body, part, false);
                case JOINT, HAND -> fit(healer, patient, body, part, true);
                default -> throw new IllegalStateException("unreachable: " + kind);
            }
        }

        Downed.checkRevived(healer, patient, body);
        Milestones.onTreated(healer, kind.id(), healer != patient);

        // The supplies come out of whoever is doing the work, not whoever is bleeding.
        if (!healer.isCreative() && !kind.reusable()) {
            stack.shrink(1);
        }
        healer.getCooldowns().addCooldown(stack.getItem(), USE_COOLDOWN);
        PartEffects.apply(patient, body);
        Body.mirror(patient);
        // What the instrument and the room left in the wound. After the treatment, so
        // that a job which refused itself cannot infect anybody, and only where the skin
        // was opened - a splint is not a way to catch anything.
        soiled(healer, patient, body, kind, part, stack);
        // Said here rather than where the assistant was looked for, because up there the
        // treatment has not happened yet and three of the paths below it turn round and
        // refuse. Telling somebody they held a patient still for a job that never
        // started is worse than not telling them at all.
        if (assistant != null && kind.opensSkin()) {
            held(healer, patient, assistant);
        }
        // And what the part has to be given afterwards, which is time and nothing else.
        if (part != null && kind.opensSkin()) {
            Mending.begins(patient, body, part, quality);
        }
        // And what it cost them in heat. Last, because it can end in an illness and the
        // treatment that caused it should have finished happening first.
        if (kind.opensSkin()) {
            Chill.opened(healer, patient);
        }
        Body.sync(patient);
        return true;
    }


    /**
     * Say so, to both of the people who did something.
     *
     * <p>Nothing here is discoverable by looking at it: an assistant crouching with an
     * empty hand looks exactly like an assistant crouching. So the mod says it out loud
     * the first time it happens, which is the only way anybody would ever find out that
     * the second person in the room is worth anything.
     */
    private static void held(ServerPlayer healer, ServerPlayer patient, ServerPlayer assistant) {
        Body.notify(healer, Component.translatable(
                        healer == patient ? "triage.msg.held_still_you" : "triage.msg.held_still",
                        assistant.getName()),
                ChatFormatting.AQUA, null);
        Body.notify(assistant, Component.translatable("triage.msg.holding", patient.getName()),
                ChatFormatting.AQUA, null);
        // Bracing somebody is work, and it is paid here because here is where it is known
        // to have happened at all.
        Fatigue.braced(assistant);
    }

    /**
     * What the instrument and the room leave behind.
     *
     * <p>Only for a treatment that opens somebody: a splint is a stick tied to a leg and
     * has never given anybody anything. And only when there is something to catch it -
     * a clean tool in a fair room rolls nothing at all, which is the whole shape of this:
     * the way people already played costs them nothing new.
     *
     * <p>The instrument is then dirtier than it was, whatever happened. That is the
     * running cost the three reusable tools never had, and the reason the table is worth
     * standing next to for a second afterwards.
     */
    private static void soiled(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                               MedicineKind kind, @Nullable BodyPart part, ItemStack stack) {
        if (part == null || !kind.opensSkin()) {
            Theatre.used(stack, kind);
            return;
        }
        float chance = Theatre.infectionChance(patient, stack);
        if (chance > 0.0F && healer.getRandom().nextFloat() < chance) {
            PartState state = body.part(part);
            if (!state.gone() && !state.necrotic()) {
                state.infect(PartState.INFECTION_MILD);
                body.setDirty(true);
                Body.notify(healer, Component.translatable("triage.msg.dirty_work", part.title()),
                        ChatFormatting.DARK_GREEN, SoundEvents.SLIME_SQUISH);
                if (healer != patient) {
                    Body.notify(patient, Component.translatable("triage.msg.dirty_work_you", part.title()),
                            ChatFormatting.DARK_GREEN, null);
                }
            }
        }
        Theatre.used(stack, kind);
    }

    private static void bandage(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part,
                                Config.Values config) {
        PartState state = body.part(part);
        boolean strapped = state.tourniquet;
        boolean changed = state.soiled(Config.values().dressingCleanSeconds * 20);
        state.stopBleeding();
        if (strapped) {
            // A dressing over the wound is how a strap is meant to come off, and it is
            // still a strap coming off. Letting the commonest item in the game dodge
            // what that costs would make the cost avoidable rather than a decision.
            state.tourniquet = false;
            Necrosis.released(patient, body, state);
        }
        state.heal(config.bandageHeal);
        state.dress();
        body.setDirty(true);
        done(healer, patient, strapped ? "triage.msg.unstrapped"
                        : changed ? "triage.msg.redressed" : "triage.msg.bandaged",
                part, SoundEvents.WOOL_PLACE);
    }

    /**
     * The strap goes on and the bleeding stops dead. The wound is still open underneath
     * - take it off without dressing what is under it and the blood starts again.
     */
    private static void tourniquet(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part) {
        body.part(part).tourniquet = true;
        body.setDirty(true);
        done(healer, patient, "triage.msg.strapped", part, SoundEvents.LEASH_KNOT_PLACE);
    }

    /**
     * A splint sets a bone. It does not mend one.
     *
     * <p>A break comes back as a crack, which is the state the mod already has for "this
     * knits if you leave it alone and goes again if you do not". So the splint stops
     * being a full stop and becomes the first half of a fix, with the second half being
     * the thing nobody wants to do: rest it. A crack splinted is simply mended, because
     * there is nothing left under it to set.
     */
    private static void splint(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part) {
        PartState state = body.part(part);
        // Nothing goes on over bone that is outside somebody. Strapping a stick to an
        // open fracture is how a broken leg becomes a lost one, and the mod refuses it
        // rather than letting a player find that out.
        if (state.openFracture()) {
            refuse(healer, "triage.msg.bone_is_open");
            return;
        }
        if (!state.fractured()) {
            // A crack, splinted before it became the thing splints are for.
            state.mend();
            body.setDirty(true);
            done(healer, patient, "triage.msg.splinted", part, SoundEvents.BONE_BLOCK_PLACE);
            return;
        }
        state.setOne();
        if (state.fractured()) {
            // Ribs. One down, and the rest of them are still a chest full of broken bone,
            // which is worth saying out loud rather than leaving the player to count pips.
            body.setDirty(true);
            Body.notify(healer, Component.translatable("triage.msg.splinted_more",
                    part.title(), state.breaks), ChatFormatting.GREEN, SoundEvents.BONE_BLOCK_PLACE);
            if (healer != patient) {
                Body.notify(patient, Component.translatable("triage.msg.splinted_more",
                        part.title(), state.breaks), ChatFormatting.GREEN, SoundEvents.BONE_BLOCK_PLACE);
            }
            return;
        }
        // Allowed, and said out loud. A splint on a bone that is out of line holds it
        // exactly where it should not be, and in a week that is permanent - but the mod
        // does not refuse it, because "no time, no hands, splint it anyway" is a real
        // decision, and taking it away would take away the point of the whole rung.
        if (state.outOfLine()) {
            Body.notify(healer, Component.translatable("triage.msg.splint_crooked", part.title()),
                    ChatFormatting.GOLD, null);
            if (healer != patient) {
                Body.notify(patient, Component.translatable("triage.msg.splint_crooked", part.title()),
                        ChatFormatting.GOLD, null);
            }
        }
        // The last one. A bone that has been put back together still has to be rested,
        // and the stick is what does the resting for it: it stays on the limb, it is
        // drawn on the body, and it is on the chart until the bone is finished with it.
        state.crack();
        Config.Values config = Config.values();
        if (config.wornSplints) {
            state.setSplint(config.splintSeconds * 20);
            Body.notify(patient, Component.translatable("triage.msg.splint_on",
                    part.title(), state.splintTicks / 20), ChatFormatting.AQUA, null);
        }
        body.setDirty(true);
        done(healer, patient, "triage.msg.splinted_set", part, SoundEvents.BONE_BLOCK_PLACE);
    }

    private static void antiseptic(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part) {
        PartState state = body.part(part);
        state.disinfect();
        // Cleaning a wound out means taking off whatever was over it. What goes back on
        // is the next bandage, and until then the wound is open to the world again.
        state.undress();
        body.setDirty(true);
        done(healer, patient, "triage.msg.cleaned", part, SoundEvents.BREWING_STAND_BREW);
    }

    /**
     * The salve, and the burn it cannot reach.
     *
     * <p>What a salve does is keep living tissue from drying out while it closes over. A
     * burn through the skin has nothing living left in it to keep, so the jar goes on and
     * achieves nothing - and the mod says so rather than taking the jar.
     *
     * <p>Which is the whole reason a burn is worth testing before it is treated. The
     * deepest burn is the painless one, so the patch nobody is complaining about is the
     * patch the jar is wasted on.
     */
    private static void salve(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part,
                              Config.Values config) {
        PartState state = body.part(part);
        // A rung down rather than gone. What a salve does to a blistered burn is keep it
        // from drying out while it closes over, and what it closes over into is red skin -
        // which then does the last of it on its own, in its own time, with nobody's help.
        // A jar that erased the whole burn would be a jar that made the depth decorative.
        // What it leaves behind it, recorded here because this is the moment the deep
        // burn stops being a burn. Burnt skin closes by pulling its own edges together,
        // and what that leaves is a part that does not straighten.
        Scars.burnHealed(body, part, state.burn);
        state.burn = PartState.BURN_SUPERFICIAL;
        state.burnTicks = 0;
        state.heal(config.salveHeal);
        body.setDirty(true);
        done(healer, patient, "triage.msg.salved", part, SoundEvents.HONEY_BLOCK_SLIDE);
        Body.notify(healer, Component.translatable("triage.msg.burn_eased"),
                ChatFormatting.GRAY, null);
    }

    /**
     * The needle goes in, the air comes out, the lung fills again.
     *
     * <p>Nothing is healed here on purpose. What the chest has already lost it keeps -
     * this stops the loss rather than undoing it, and a chest emptied to the floor by a
     * pneumothorax nobody found still needs a surgeon afterwards.
     */
    private static void decompress(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part) {
        body.part(part).decompress();
        body.setDirty(true);
        done(healer, patient, "triage.msg.decompressed", part, SoundEvents.BOTTLE_EMPTY);
    }

    /**
     * Enough to take somebody out of their own body for a while.
     *
     * <p>The dose is the whole of it. A good one buys the window an operation needs; a
     * poor one buys a fraction of it and rolls, on the way past, for the thing that makes
     * this the most dangerous item in the box - a patient who has stopped breathing.
     *
     * <p>That state is one the mod already has and already has an answer to, which is why
     * it is the right punishment: it does not kill anybody outright, it puts a second
     * emergency in the room while the first one is still open.
     */
    private static boolean anaesthetic(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                       Config.Values config, float quality) {
        if (!config.anaesthesia) {
            return refuse(healer, "triage.msg.aid_off");
        }
        if (body.under()) {
            return refuse(healer, "triage.msg.already_under");
        }
        float clean = Mth.clamp(quality, 0.0F, 1.0F);
        // Rolled here rather than later, so the risk belongs to the hand that gave it.
        if (patient.getRandom().nextFloat() < config.anaesthesiaArrest * (1.0F - clean)) {
            Downed.stopBreathing(patient, body);
            Body.notify(healer, Component.translatable("triage.msg.gas_arrest", patient.getName()),
                    ChatFormatting.DARK_RED, SoundEvents.WITHER_SPAWN);
            return true;
        }
        int seconds = Math.max(5, Math.round(config.anaesthesiaSeconds * (0.35F + 0.65F * clean)));
        body.putUnder(seconds * 20);
        body.setDirty(true);
        Body.notify(healer, Component.translatable("triage.msg.gas_under", patient.getName(), seconds),
                ChatFormatting.AQUA, SoundEvents.BOTTLE_EMPTY);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.gas_under_you", healer.getName()),
                    ChatFormatting.AQUA, null);
        }
        return true;
    }

    /**
     * What it costs to do this to somebody who can feel it.
     *
     * <p>Both operations in the mod used to be silent about the patient. They are not
     * silent about it now: a body that is moving cannot be operated on well however good
     * the hands are, so the quality is capped whatever the screen said, the part pays for
     * it, and the pain alone can put them into shock.
     *
     * @return the quality the operation is actually worth
     */
    private static float awake(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part,
                               Config.Values config, float quality) {
        if (!config.anaesthesia || body.under()) {
            return quality;
        }
        if (config.awakeDamage > 0.0F) {
            body.part(part).damage(config.awakeDamage);
        }
        if (config.shock && patient.getRandom().nextFloat() < 0.5F) {
            body.catchDisease(Disease.SHOCK);
        }
        Body.notify(patient, Component.translatable("triage.msg.operated_awake"),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
        if (healer != patient) {
            Body.notify(healer, Component.translatable("triage.msg.operated_awake_them", patient.getName()),
                    ChatFormatting.DARK_RED, null);
        }
        // Held down, they move less. This is what the operation was before anaesthetic
        // existed and it is why an operating table has straps on it in every picture of
        // one: it does not stop the pain and it was never meant to, it stops the patient.
        // Which makes the second pair of hands worth most in exactly the case where
        // there is nothing else - and still worse than the bottle.
        return Math.min(quality, Theatre.assistant(patient, healer) != null
                ? config.assistAwakeCap : config.awakeQualityCap);
    }

    /**
     * The limb comes off.
     *
     * <p>Everything wrong with it goes with it, which is not a reward - it is what the
     * operation is. What it costs is paid in the one currency this always costs blood in
     * and in the state that follows any big loss, and both of those are the same whether
     * the patient is a hero or an idiot.
     *
     * <p>The strap is the whole rule. With one above the cut there is a light bleed that
     * clots like any other; without one there is an artery open on a stump, it never
     * clots, and a fifth of the volume is on the floor before anybody has decided
     * anything. The mod says so on the guide page and then lets people find out.
     */
    private static void amputate(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part,
                                 Config.Values config, float quality) {
        quality = awake(healer, patient, body, part, config, quality);
        PartState state = body.part(part);
        boolean strapped = state.tourniquet;
        // A limb that was already dead comes off as meat, and the item says so. One that
        // was alive when the saw started is worth something to somebody with a needle -
        // which is a decision the surgeon has just made for them, and a fair one to have
        // to live with.
        boolean wasDead = state.necrotic();
        state.amputate();
        Severed.drop(patient, part, !wasDead);
        Reactions.say(patient, "lost");
        // Twice the volume for a job done crooked, and no way of doing it for less than
        // the full price. Nothing else in the mod charges by how well: everything else
        // can be walked away from and tried again with steadier hands, and this cannot.
        body.addBloodLoss(config.amputationBloodLoss * (2.0F - Mth.clamp(quality, 0.0F, 1.0F)));
        if (strapped) {
            state.startBleeding(PartState.BLEED_LIGHT, config.lightBleedSeconds * 20);
        } else {
            state.startBleeding(PartState.BLEED_HEAVY, 0);
            body.addBloodLoss(config.amputationOpenBloodLoss);
        }
        // Nobody walks away from this one. Shock is what a body does after losing a
        // piece of itself, and it is the reason the patient needs somebody afterwards
        // rather than a pat on the back.
        if (config.shock) {
            body.catchDisease(Disease.SHOCK);
        }
        body.setDirty(true);
        done(healer, patient, strapped ? "triage.msg.amputated" : "triage.msg.amputated_open",
                part, SoundEvents.BONE_BLOCK_BREAK);
        // The one outcome worth saying out loud on its own, because it is not a wound
        // and there is no line for it on the chart: they are not getting up again until
        // somebody brings them something to stand on.
        if (!body.canStand()) {
            Body.notify(patient, Component.translatable("triage.msg.legless"),
                    ChatFormatting.DARK_RED, SoundEvents.WITHER_SPAWN);
        }
        Milestones.onAmputated(healer, patient);
    }

    /** Something in its place. It is not the limb and the chart will keep saying so. */
    /**
     * Sewing their own arm back on.
     *
     * <p>Three things have to be true and only one of them is decided in this room. It has
     * to be the right arm for the shoulder, it has to still be alive, and the surgery has
     * to go well enough - and the first two were settled minutes ago by whoever picked it
     * up and whether they thought to put it somewhere cold.
     *
     * <p>What comes back is not the arm they had. It comes back on a fraction of its
     * health and cracked, which is the mod's way of saying stiff and not to be leaned on
     * yet, and the fraction is what the hands were worth. A bad graft is an arm that is
     * technically attached, which is also what a bad graft is.
     */
    /** Recorded where the limb goes back on: it works, and it does not work as it did. */
    private static void graft(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part,
                              ItemStack stack, Config.Values config, float quality) {
        if (SeveredLimb.partOf(stack) != part) {
            refuse(healer, "triage.msg.limb_wrong");
            return;
        }
        if (!SeveredLimb.usable(stack)) {
            refuse(healer, "triage.msg.limb_dead");
            return;
        }
        PartState state = body.part(part);
        quality = awake(healer, patient, body, part, config, quality);
        state.reattach();
        state.health = Math.max(1.0F, state.max * config.graftRestore * Mth.clamp(quality, 0.2F, 1.0F));
        // Stiff, and not to be used yet. The same state a set bone is left in, because it
        // is the same instruction: leave it alone and let it take.
        state.crack();
        // An arm that has been off somebody is not a clean arm, whatever it was wrapped in.
        state.infect(PartState.INFECTION_MILD);
        // And it is never the arm it was. Everything that had to be joined back up is
        // joined back up by somebody in a hurry, and what that leaves is a limb that
        // works and does not quite straighten.
        Scars.sewn(body, part);
        body.setDirty(true);
        stack.shrink(1);
        if (!body.downed() && body.canStand()) {
            Downed.stand(patient);
        }
        done(healer, patient, "triage.msg.grafted", part, SoundEvents.PLAYER_LEVELUP);
    }

    private static void fit(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part,
                            boolean fine) {
        PartState state = body.part(part);
        state.prosthetic = true;
        state.fineProsthetic = fine;
        // A fresh one is fresh. This is the whole of the maintenance, and it costs what
        // fitting one costs, which is the item.
        state.wear = 0;
        body.setDirty(true);
        // Back on their feet, at the exact moment they get something to stand on. Done
        // here rather than on a tick because there is no way to ask a player whether a
        // pose is being forced on them - only to force one or to stop.
        if (!body.downed() && body.canStand()) {
            Downed.stand(patient);
            Body.notify(patient, Component.translatable("triage.msg.stood_up"),
                    ChatFormatting.GREEN, null);
        }
        done(healer, patient, fine ? "triage.msg.fitted_fine" : "triage.msg.fitted", part,
                SoundEvents.ANVIL_USE);
        Milestones.onFitted(healer);
    }


    /**
     * Fragments out, with jaws.
     *
     * <p>The whole handful in one go, because that is one job: opening a wound five times
     * to take five pieces out of it would be five wounds. What the game decides is how
     * much of it was done cleanly, and that decides what the wound does afterwards.
     */
    private static void extract(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                BodyPart part, Config.Values config, float quality) {
        PartState state = body.part(part);
        // The weapon, if that is what is in there. Forceps and a wound held open turn the
        // worst bleed in the mod into one a dressing answers, and that difference is the
        // entire argument for waiting for somebody with a kit.
        if (state.bladeIn()) {
            boolean clean = quality >= 0.5F;
            ItemStack blade = Stuck.take(patient, body, part,
                    clean ? PartState.BLEED_LIGHT : PartState.BLEED_HEAVY, !clean, config);
            Stuck.hand(healer, blade);
            Body.notify(healer, Component.translatable(clean
                            ? "triage.msg.blade_cut_out" : "triage.msg.blade_cut_out_badly",
                            blade.getHoverName(), part.title()),
                    clean ? ChatFormatting.AQUA : ChatFormatting.RED,
                    clean ? SoundEvents.ITEM_PICKUP : SoundEvents.PLAYER_HURT);
            if (healer != patient) {
                Body.notify(patient, Component.translatable("triage.msg.blade_out_you",
                                blade.getHoverName(), part.title()),
                        clean ? ChatFormatting.AQUA : ChatFormatting.RED, null);
            }
            return;
        }
        boolean fragment = state.shrapnel;
        boolean bullet = state.bullets > 0;
        int had = state.arrows + state.bullets;
        while (state.pull()) {
            // Until the wound is empty.
        }
        while (state.pullBullet()) {
            // Until the wound is empty.
        }
        boolean clean = quality >= 0.5F;
        // Even a clean job leaves what a hole leaves. A poor one leaves a wound that will
        // not close on its own, which is the thing this mod takes most seriously.
        state.startBleeding(clean ? PartState.BLEED_LIGHT : PartState.BLEED_HEAVY,
                config.lightBleedSeconds * 20);
        if (!clean) {
            float tear = fragment ? config.arrowTear : bullet ? config.bulletTear : config.arrowTear;
            Injuries.apply(patient, body, part, tear, false);
        }
        body.setDirty(true);
        Body.notify(healer, Component.translatable(clean
                        ? "triage.msg.extracted" : "triage.msg.extracted_badly", had, part.title()),
                clean ? ChatFormatting.AQUA : ChatFormatting.RED,
                clean ? SoundEvents.ITEM_PICKUP : SoundEvents.PLAYER_HURT);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.extracted_you", part.title()),
                    clean ? ChatFormatting.AQUA : ChatFormatting.RED, null);
        }
    }

    /**
     * The dressing comes off in one piece.
     *
     * <p>Nothing is rolled and nothing can go wrong with the wound, which is the entire
     * value of the item: the blade against the skin is the blunt one. A hand can do this
     * too, and a hand takes the clot with it.
     */
    private static void cutAway(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part,
                                float quality) {
        PartState state = body.part(part);
        state.undress();
        // A blade lifted off the skin nicks it. Nothing that reopens anything - it is a
        // scratch, and it is the only thing scissors can cost.
        float clean = Mth.clamp(quality, 0.0F, 1.0F);
        if (clean < 1.0F) {
            state.damage((1.0F - clean) * 2.0F);
        }
        body.setDirty(true);
        done(healer, patient, "triage.msg.cut_away", part, SoundEvents.SHEEP_SHEAR);
    }

    /** One course, one illness - the worst one the patient has. */
    /**
     * The other card, and the one illness it is for.
     *
     * <p>It does nothing else. Not a cold, not a fever, not sepsis - which is the point of
     * having it at all: the mod now has two cards that look alike, do different jobs, and
     * refuse each other's work. Somebody who reaches for the wrong one finds out by nothing
     * happening, and keeps the card.
     */
    private static boolean worming(ServerPlayer healer, ServerPlayer patient, BodyHealth body) {
        if (!body.has(Disease.PARASITES)) {
            return refuse(healer, "triage.msg.no_worms");
        }
        body.cure(Disease.PARASITES);
        body.setDirty(true);
        Body.notify(healer, Component.translatable("triage.msg.wormed"),
                ChatFormatting.GREEN, SoundEvents.GENERIC_DRINK);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.wormed"),
                    ChatFormatting.GREEN, null);
        }
        return true;
    }

    /**
     * The course that nearly worked.
     *
     * <p>One bottle is one dose, and one dose is not a course - which is true of every
     * antibiotic anybody has ever been given and is the single most common way people make
     * their own illness worse. So sometimes the fever lifts, the patient gets up, and it
     * comes back in waves a few minutes later.
     *
     * <p>Not a punishment for treating somebody. It is the difference between treating them
     * and finishing, and the mod has no way to say that other than by occasionally taking
     * the ending away.
     */
    private static void relapse(ServerPlayer patient, BodyHealth body, Disease cured) {
        Config.Values config = Config.values();
        if (cured == Disease.RELAPSE || cured.viral() || cured.severity() < 1
                || body.has(Disease.RELAPSE)
                || patient.getRandom().nextFloat() >= config.relapseChance) {
            return;
        }
        // Said to nobody, and that is the whole of it: everybody in the room just watched
        // a cure work and walked away believing the job was done.
        body.catchDisease(Disease.RELAPSE);
    }

    private static boolean antibiotics(ServerPlayer healer, ServerPlayer patient, BodyHealth body) {
        Disease worst = body.worstDisease();
        if (worst == null) {
            // Ill, but with something no course reaches. Worth saying which, because the
            // whole point of a virus being in here is that somebody learns the difference
            // - and worth not spending the bottle on it, because nothing happened.
            for (Disease disease : Disease.VALUES) {
                if (disease.viral() && body.has(disease)) {
                    return refuse(healer, "triage.msg.not_bacterial");
                }
            }
            // And the one with its own card, which is worth naming rather than lumping in
            // with "not bacterial": there is something to reach for, and it is not this.
            if (body.has(Disease.PARASITES)) {
                return refuse(healer, "triage.msg.wants_worming");
            }
            return refuse(healer, "triage.msg.not_ill");
        }
        // The one illness a course cannot finish. While the gut is still open the cause
        // is still arriving, so the bottle buys the clock back and nothing else - which
        // is exactly what a course against an open gut does. It is not a refusal either:
        // the time is real, and it is the time the surgeon needed.
        if (!Sick.curable(body, worst)) {
            body.addDiseaseTicks(worst, -body.diseaseTicks(worst));
            Body.notify(healer, Component.translatable("triage.msg.held_off", worst.title()),
                    ChatFormatting.YELLOW, SoundEvents.BREWING_STAND_BREW);
            if (healer != patient) {
                Body.notify(patient, Component.translatable("triage.msg.held_off_you", worst.title()),
                        ChatFormatting.YELLOW, null);
            }
            return true;
        }
        body.cure(worst);
        Body.notify(healer, Component.translatable("triage.msg.cured", worst.title()),
                ChatFormatting.GREEN, SoundEvents.PLAYER_LEVELUP);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.cured_by", healer.getName(), worst.title()),
                    ChatFormatting.GREEN, null);
        }
        // Everything above this line said the illness is over, and for most patients it
        // is. This is the one that is not, and it says nothing at all.
        relapse(patient, body, worst);
        return true;
    }

    /**
     * Holding somebody who is going, without standing them up.
     *
     * <p>The clock stops while the packing holds. It does not heal, it does not revive,
     * and a hit undoes it - and every hold on the same downing is worth less than the
     * last, so this buys the time to fetch a surgical kit and never the argument.
     */
    private static boolean stabilise(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                     Config.Values config) {
        // Not on yourself. A player on the floor cannot move and cannot fight, and the
        // one thing they must not be able to do is hold their own clock open - that
        // would make going down a pause rather than the last of your luck.
        if (healer == patient) {
            return refuse(healer, "triage.msg.hold_yourself");
        }
        // Packing a wound on somebody whose heart has stopped holds a clock that has
        // already run out. What that body needs is a pair of hands on the chest, and
        // saying so is worth more than quietly spending the dressing.
        if (body.arrested()) {
            return refuse(healer, "triage.msg.hold_arrested");
        }
        if (config.stabiliseSeconds <= 0) {
            return refuse(healer, "triage.msg.hold_spent");
        }
        int granted = body.stabilise(config.stabiliseSeconds * 20, config.stabiliseFalloff);
        if (granted <= 0) {
            return refuse(healer, "triage.msg.hold_spent");
        }
        int seconds = granted / 20;
        Body.notify(healer, Component.translatable("triage.msg.stabilised", patient.getName(), seconds),
                ChatFormatting.AQUA, SoundEvents.WOOL_PLACE);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.stabilised_you", healer.getName(), seconds),
                    ChatFormatting.AQUA, SoundEvents.WOOL_PLACE);
        }
        return true;
    }

    /**
     * Volume back into the patient. Refused outright while anything is still open: a
     * transfusion into a body that is still leaking is a bag of saline poured on the
     * floor, and refusing it is what keeps "stop the bleeding first" a rule rather than
     * a suggestion.
     */
    private static boolean transfusion(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                       ItemStack stack, Config.Values config) {
        BloodType donor = BloodBag.typeOf(stack);
        if (body.bloodLoss() <= 0.0F) {
            return refuse(healer, "triage.msg.blood_full");
        }
        if (body.worst(part -> body.part(part).losingBlood()) != null) {
            return refuse(healer, healer == patient
                    ? "triage.msg.still_bleeding" : "triage.msg.still_bleeding_them");
        }

        // Saline is volume and nothing else: it cannot be the wrong type because it is
        // nobody's. Blood can be, and is the only thing in the mod that punishes you for
        // treating somebody rather than for failing to.
        if (donor != null) {
            BloodType recipient = body.bloodType();
            if (recipient == null) {
                return refuse(healer, "triage.msg.unknown_type");
            }
            if (!donor.canGiveTo(recipient)) {
                reaction(healer, patient, body, donor, recipient);
                return true;
            }
        }

        body.addBloodLoss(-(donor != null ? config.bloodRestore : config.transfusionRestore));
        body.setDirty(true);
        Body.notify(healer, Component.translatable("triage.msg.transfused"),
                ChatFormatting.GREEN, SoundEvents.GENERIC_DRINK);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.transfused_by", healer.getName()),
                    ChatFormatting.GREEN, SoundEvents.GENERIC_DRINK);
        }
        return true;
    }

    /**
     * The wrong blood went in. The bag is still spent and the patient is worse off than
     * if nobody had touched them - which is the entire reason the label on the bag is
     * worth reading.
     */
    private static void reaction(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                 BloodType donor, BloodType recipient) {
        body.catchDisease(Disease.REACTION);
        body.setDirty(true);
        Body.notify(patient, Component.translatable("triage.msg.reaction", donor.label(), recipient.label()),
                ChatFormatting.DARK_RED, SoundEvents.WITHER_SPAWN);
        if (healer != patient) {
            Body.notify(healer, Component.translatable("triage.msg.reaction_caused",
                            patient.getName(), donor.label(), recipient.label()),
                    ChatFormatting.DARK_RED, SoundEvents.WITHER_SPAWN);
        }
    }

    /**
     * A course that holds it down.
     *
     * <p>It cures nothing and is not meant to. What it does is buy quiet for as long as
     * it lasts, and the moment it stops the illness comes back louder than it was - so
     * the question a player with this ends up asking is not whether to treat it but
     * which of the two things they would rather put up with. That is the whole of it.
     */
    private static boolean antipsychotic(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                         Config.Values config) {
        if (!body.has(Disease.SCHIZOPHRENIA)) {
            return refuse(healer, "triage.msg.no_course_needed");
        }
        if (body.medicated()) {
            return refuse(healer, "triage.msg.course_running");
        }
        body.takeCourse(config.antipsychoticSeconds * 20);
        // The rare, bad one. Rolled here rather than at some point afterwards, so the
        // risk belongs to the decision that took it on.
        if (patient.getRandom().nextFloat() < config.neurolepticChance
                && body.catchDisease(Disease.NEUROLEPTIC)) {
            Body.notify(patient, Component.translatable("triage.msg.neuroleptic"),
                    ChatFormatting.DARK_RED, SoundEvents.WITHER_SPAWN);
        }
        Body.notify(patient, Component.translatable("triage.msg.course_taken",
                        config.antipsychoticSeconds), ChatFormatting.AQUA, SoundEvents.GENERIC_DRINK);
        if (healer != patient) {
            Body.notify(healer, Component.translatable("triage.msg.course_given", patient.getName()),
                    ChatFormatting.AQUA, null);
        }
        return true;
    }

    /**
     * Blood out of somebody and into a bag. It is a wound the donor chooses: the volume
     * comes off them there and then, so a blood bank is built out of somebody's afternoon
     * rather than out of nothing.
     */
    private static boolean draw(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                Config.Values config) {
        BloodType type = body.bloodType();
        if (type == null) {
            return refuse(healer, "triage.msg.unknown_type");
        }
        if (body.downed()) {
            return refuse(healer, "triage.msg.donor_downed");
        }
        if (body.worst(part -> body.part(part).losingBlood()) != null) {
            return refuse(healer, healer == patient
                    ? "triage.msg.still_bleeding" : "triage.msg.still_bleeding_them");
        }
        if (body.bloodLoss() + config.donationCost > config.maxDonorLoss) {
            return refuse(healer, healer == patient
                    ? "triage.msg.too_little_blood" : "triage.msg.too_little_blood_them");
        }

        body.addBloodLoss(config.donationCost);
        body.setDirty(true);
        // Into the hands of whoever did the drawing, not whoever it came out of.
        healer.getInventory().placeItemBackInInventory(BloodBag.of(type));
        Body.notify(healer, Component.translatable("triage.msg.drew", type.title()),
                ChatFormatting.GREEN, SoundEvents.GENERIC_DRINK);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.drew_from", healer.getName()),
                    ChatFormatting.YELLOW, SoundEvents.GENERIC_DRINK);
        }
        return true;
    }

    private static void medkit(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part,
                               Config.Values config) {
        PartState state = body.part(part);
        state.heal(config.medkitHeal);
        // A kit closes the wound and covers it, which is a dressing however good the
        // rest of it was. It starts the same clock a bandage does.
        state.dress();
        body.setDirty(true);
        done(healer, patient, "triage.msg.treated", part, SoundEvents.BREWING_STAND_BREW);
    }

    /**
     * One operation, and one job.
     *
     * <p>It used to do all six at once: a limb brought back, every organ in the part
     * repaired, the burn excised, the bone mended and the fracture closed, in a single
     * use of a single item, because there was a single game behind it. That made the most
     * expensive thing in the mod into a button that fixed whatever it found, and it made
     * five very different operations into one.
     *
     * <p>Now the screen plays the operation the part is actually asking for - see
     * {@link SurgeryJob} - and this performs that one. Which is chosen by the same method
     * the screen used, so the two cannot drift: a player who played the trephine is never
     * handed the outcome of a debridement.
     *
     * <p>A leg with the bone outside it and a burn on the same leg is two operations now,
     * because it is two operations.
     */
    private static void surgery(ServerPlayer healer, ServerPlayer patient, BodyHealth body, BodyPart part,
                                Config.Values config, float quality) {
        awake(healer, patient, body, part, config, quality);
        PartState state = body.part(part);
        SurgeryJob job = SurgeryJob.of(part, state);
        if (job == null) {
            refuse(healer, "triage.msg.nothing_blacked");
            return;
        }
        // Whatever the job, the field is closed at the end of it. Nothing is left open on
        // a patient a surgeon has finished with.
        state.stopBleeding();

        // And whatever the job, hands that have been inside a part know what is inside it.
        // In realistic that is the one channel here that costs a knife rather than a
        // minute, and each job learns only what it actually exposes.
        body.establish(part, switch (job) {
            case IRRIGATE, DEBRIDE -> Findings.ROT | Findings.FLESH;
            case EXCISE -> Findings.ROT | Findings.FLESH | Findings.METAL;
            case ORGAN -> Findings.ORGAN | Findings.FLESH;
            case TREPHINE -> Findings.ORGAN;
            case OSTEOTOMY -> Findings.BONE | Findings.FLESH;
        }, patient.level().getGameTime());

        switch (job) {
            // Washed out and closed. The bone is still broken and still out of line: what
            // this buys is the end of the race against the floor that got into it.
            case IRRIGATE -> Bones.close(healer, patient, body, part);
            // A skull or a spine, which no splint reaches. This is the only job that
            // mends the bone outright, because it is the only bone here nobody can set.
            case TREPHINE -> {
                state.mend();
                if (state.blacked()) {
                    state.health = Math.max(0.5F, state.max * config.surgeryRestore);
                }
                Body.notify(patient, Component.translatable("triage.msg.pressure_off", part.title()),
                        ChatFormatting.GREEN, null);
            }
            // Everything torn in there, not one thing: a surgeon who has the chest open
            // is not going to sew one tear and close it again.
            case ORGAN -> {
                state.repairOrgans();
                Body.notify(patient, Component.translatable("triage.msg.organ_repaired", part.title()),
                        ChatFormatting.GREEN, null);
            }
            // The dead part comes off. What is left is the burn a salve can reach, and
            // somebody still has to reach it.
            case EXCISE -> {
                Scars.burnHealed(body, part, PartState.BURN_FULL);
                state.burn = PartState.BURN_PARTIAL;
                state.burnTicks = 0;
                Body.notify(patient, Component.translatable("triage.msg.burn_excised", part.title()),
                        ChatFormatting.GREEN, null);
            }
            // The original operation: a limb beaten to nothing, opened, and brought back
            // at a fraction of what it was.
            case DEBRIDE -> state.health = Math.max(0.5F, state.max * config.surgeryRestore);
            // And the elective one, which is the injury done again on purpose.
            case OSTEOTOMY -> Bones.rebreak(healer, patient, body, part);
        }

        body.setDirty(true);
        done(healer, patient, "triage.msg.operated", part, SoundEvents.TOTEM_USE);
    }

    /**
     * A dose, worth less than the one before it.
     *
     * <p>Painkillers used to be a button with no cost: swallow one whenever anything
     * hurt. Now the body remembers - each course inside the window buys a shorter
     * silence, and the tolerance only wears off once the last one is out. The pills are
     * still the answer to a bad ten seconds; they have stopped being the answer to
     * everything.
     */
    /**
     * Adrenaline, and the two right answers it has.
     *
     * <p>In a stopped heart it buys the next round of compressions back to full value. In
     * a body destroying the blood that was hung on it, it is the only answer there is.
     * Anywhere else it is a stimulant with a bill - and on a patient who is bleeding it is
     * the worst thing in the bag, because the wrong use looks exactly like the right one:
     * somebody emptying out looks like somebody fading.
     */
    private static boolean epinephrine(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                       Config.Values config, float quality) {
        // What got into the circulation. A dose in the tissue around a vein is a
        // fraction of a dose, arriving late, and every line below is scaled by it -
        // including the harm, because a dose that did not arrive cannot do damage
        // either.
        if (body.arrested()) {
            body.freshRounds(Math.max(1, Math.round(config.epinephrineRounds * quality)));
            Body.notify(healer, Component.translatable("triage.msg.epi_arrest", patient.getName()),
                    ChatFormatting.AQUA, SoundEvents.WARDEN_HEARTBEAT);
            return true;
        }
        if (body.has(Disease.REACTION) && quality >= 0.5F) {
            // Half a dose into an arm does not answer anaphylaxis. It is the one use
            // here where a miss is simply a miss.
            body.cure(Disease.REACTION);
            Body.notify(healer, Component.translatable("triage.msg.epi_reaction"),
                    ChatFormatting.GREEN, SoundEvents.PLAYER_LEVELUP);
            if (healer != patient) {
                Body.notify(patient, Component.translatable("triage.msg.epi_reaction"),
                        ChatFormatting.GREEN, null);
            }
            return true;
        }

        // Not an emergency, so it is a stimulant: they get the surge and the crash after
        // it, exactly as if an injury had done it to them.
        // A heart that is still paying for the last one counts as a heart already on it.
        // Without the crash in this test, injecting during the crash would be a way to
        // skip the bill - which would make the needle a gift at the one moment it should
        // be the worst idea in the world.
        boolean already = Adrenaline.up(body) || Adrenaline.crashing(body);
        Adrenaline.shot(patient, body);

        // And the mistake. Raising the pressure in a body with an open vessel in it means
        // the bleed pours - every light one turns heavy and a share of the volume goes
        // while the person holding the needle is still feeling useful.
        float lost = 0.0F;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (state.bleed <= PartState.BLEED_NONE) {
                continue;
            }
            if (state.bleed == PartState.BLEED_LIGHT) {
                state.startBleeding(PartState.BLEED_HEAVY, 0);
            }
            lost += config.epinephrineBleed * quality;
        }
        if (lost > 0.0F) {
            body.addBloodLoss(lost);
            body.setDirty(true);
            Body.notify(healer, Component.translatable("triage.msg.epi_bleeding", patient.getName()),
                    ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
            if (healer != patient) {
                Body.notify(patient, Component.translatable("triage.msg.epi_bleeding_you"),
                        ChatFormatting.DARK_RED, null);
            }
        }
        // Twice over is the other way a heart stops.
        if (already && healer.getRandom().nextFloat() < config.epinephrineArrestChance * quality) {
            // The mod already had the way into this state that is nobody's injury - it was
            // written for an anaesthetic dose that worked too well - and a heart driven
            // over is the same emergency with the same answer. Doing it by hand here got
            // the order wrong: an arrest already counts as being down, so claiming them
            // afterwards returned early and left them standing up with a stopped heart.
            Downed.stopBreathing(patient, body);
            Body.notify(patient, Component.translatable("triage.msg.epi_too_much"),
                    ChatFormatting.DARK_RED, SoundEvents.WITHER_SPAWN);
            if (healer != patient) {
                Body.notify(healer, Component.translatable("triage.msg.epi_too_much_them",
                        patient.getName()), ChatFormatting.DARK_RED, null);
            }
        }
        return true;
    }

    /**
     * Morphine, and the thing an opiate puts to sleep first.
     *
     * <p>Instant, strong, works on somebody already on the floor, and untouched by how
     * many courses of pills they have had, because it is not the same drug. What it costs
     * is the drive to breathe - and on a chest that is already failing, the dose that
     * takes the pain away is next to the dose that takes the breathing with it.
     */
    private static boolean morphine(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                    Config.Values config, float quality) {
        // Into the muscle it still works, slowly and for less time, which is what an
        // intramuscular dose does. It is not a failure, it is a worse dose.
        int ticks = Math.round(config.morphineSeconds * 20 * quality);
        if (body.painkillerTicks() >= ticks) {
            return refuse(healer, "triage.msg.already_painkilled");
        }
        // No tolerance taken: the pills keep their own count and this is not on it.
        body.setPainkillerTicks(ticks);
        body.setDirty(true);
        Body.notify(healer, Component.translatable("triage.msg.morphine", ticks / 20),
                ChatFormatting.AQUA, SoundEvents.GENERIC_DRINK);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.morphine", ticks / 20),
                    ChatFormatting.AQUA, null);
        }

        if (!config.morphineDepresses || !badChest(body)
                || healer.getRandom().nextFloat() >= config.morphineStopsBreathing * quality) {
            return true;
        }
        // They stop breathing. Not a death: the mod already has a clock for a body that is
        // not breathing and a pair of hands that answers it, so this mistake arrives with
        // its own way out already built. There is a needle for it too.
        Downed.claim(patient, body);
        body.tickAirway(config.airwaySeconds * 20);
        body.setDirty(true);
        Body.notify(patient, Component.translatable("triage.msg.morphine_stopped"),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
        if (healer != patient) {
            Body.notify(healer, Component.translatable("triage.msg.morphine_stopped_them",
                    patient.getName()), ChatFormatting.DARK_RED, null);
        }
        return true;
    }

    /**
     * A chest that cannot afford to be told to breathe less.
     *
     * <p>Crushed, broken, full of something, or attached to a body that has lost too much
     * to spare the effort. The things the mod already knows how to do to a chest, asked as
     * one question.
     */
    private static boolean badChest(BodyHealth body) {
        PartState chest = body.part(BodyPart.THORAX);
        return chest.pneumo || chest.fractured() || chest.organHurt()
                || body.has(Disease.PNEUMONIA) || body.has(Disease.SMOKE)
                || body.bloodLoss() >= 0.4F;
    }

    /**
     * The undo, and what undoing costs.
     *
     * <p>It answers morphine and nothing else. On somebody whose breathing has gone it is
     * the difference between a patient and a body. On somebody who was being held together
     * by the dose it takes the dose away - all of it, at once - and what arrives instead is
     * every bit of what was being kept off them.
     */
    private static boolean naloxone(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                    Config.Values config, float quality) {
        if (!body.painkilled()) {
            // Genuinely nothing to reverse, and the ampoule is not spent finding out.
            return refuse(healer, "triage.msg.nothing_to_reverse");
        }
        boolean saving = body.airwayTicks() > 0 || body.downed();
        // A partial reversal leaves some of it on board, which is exactly what a
        // partial reversal does - and it is the one case here where that is a mercy.
        body.setPainkillerTicks(Math.round(body.painkillerTicks() * (1.0F - quality)));
        body.openAirway();
        body.setDirty(true);
        Body.notify(healer, Component.translatable(saving
                        ? "triage.msg.naloxone_saved" : "triage.msg.naloxone", patient.getName()),
                saving ? ChatFormatting.GREEN : ChatFormatting.YELLOW, SoundEvents.PLAYER_ATTACK_STRONG);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.naloxone_you"),
                    ChatFormatting.RED, null);
        }
        // Everything that was being held off arrives at once, and a body with enough wrong
        // with it does what a body does when all of it turns up together.
        if (config.shock && !saving && hurtEnough(body) >= config.naloxoneShockPain
                && healer.getRandom().nextFloat() < config.naloxoneShockChance
                && body.catchDisease(Disease.SHOCK)) {
            Body.notify(patient, Component.translatable("triage.msg.caught", Disease.SHOCK.title()),
                    Disease.SHOCK.colour(), SoundEvents.PLAYER_HURT);
        }
        return true;
    }

    /** How many parts are in a state worth screaming about. */
    private static int hurtEnough(BodyHealth body) {
        int count = 0;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (state.fractured() || state.organHurt() || state.gone() || state.blacked()
                    || state.bleed > PartState.BLEED_NONE || state.burned) {
                count++;
            }
        }
        return count;
    }

    /**
     * The reading, and how much of one it is.
     *
     * <p>The only treatment here that treats nothing. What it hands over is a number, and
     * a number taken before the column settled is a number that is too low - which is not
     * a failure, it is a worse reading, and it is the mistake the instrument invites.
     */
    private static boolean thermometer(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                       float reading) {
        // Not an exact comparison with one. A float that has been through a packet and a
        // clamp is a poor thing to ask equality of, and the difference between a reading
        // that earned the figure and one that missed it by a thousandth is not a
        // difference anybody meant.
        boolean figure = reading >= EXACT_READING;
        Temperature warmth = Temperature.of(body);
        boolean self = healer == patient;
        // On the notes, and only when it is a figure. A band somebody guessed off a
        // hand is not a measurement and does not belong on a chart with a time against
        // it - which is the entire difference this instrument exists to make.
        if (figure) {
            body.noteTemp(warmth.tenths(), healer.getGameProfile().getName(),
                    healer.level().getGameTime());
            Trend.say(healer, body, Reading.TEMP);
            Body.sync(patient);
        }
        Component said = figure
                ? Component.translatable("triage.msg.temp_exact", warmth.readout(), warmth.bandTitle())
                : Component.translatable("triage.msg.temp_rough", warmth.bandTitle());
        Body.notify(healer, Component.translatable(self
                        ? "triage.msg.temp_self" : "triage.msg.temp_them", patient.getName(), said),
                warmth.colour(), null);
        if (!figure) {
            // And which of the two it was, because telling somebody to hold it longer
            // when what they actually did was take a reading off the last patient's
            // column is advice that teaches the wrong lesson twice.
            Body.notify(healer, Component.translatable(reading <= UNSHAKEN_READING
                            ? "triage.msg.temp_unshaken" : "triage.msg.temp_short"),
                    ChatFormatting.DARK_GRAY, null);
        }
        return true;
    }

    /**
     * The dose that finds out.
     *
     * <p>Anaphylaxis, which this mod already had and has always answered with the needle
     * it keeps for exactly this. What was missing was the commonest cause of it in the
     * world - not a transfusion, but a drug given to somebody who would have told you.
     *
     * <p>The dose is spent. That is the point of it and the reason it is frightening: it
     * is not a refusal, it is a treatment that went in and did the wrong thing, and the
     * medicine is gone as well. What is left to do about it is the syringe.
     *
     * @return true when this went badly, in which case the treatment itself never happens
     */
    private static boolean reacts(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                  MedicineKind kind, Config.Values config) {
        if (!config.allergies || !body.allergy().is(kind) || body.has(Disease.REACTION)) {
            return false;
        }
        if (!body.catchDisease(Disease.REACTION)) {
            return false;
        }
        body.setDirty(true);
        Body.notify(healer, Component.translatable("triage.msg.allergy_reaction",
                patient.getName(), kind.title()), ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.allergy_reaction_self",
                    kind.title()), ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
        }
        Reactions.say(patient, "allergy");
        return true;
    }

    private static boolean painkiller(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                      Config.Values config) {
        int full = config.painkillerSeconds * 20;
        int granted = body.painkillerWorth(full, config.painkillerFalloff);
        if (granted <= 20) {
            return refuse(healer, "triage.msg.painkiller_spent");
        }
        if (body.painkillerTicks() >= granted) {
            return refuse(healer, "triage.msg.already_painkilled");
        }
        body.takePainkiller();
        body.setPainkillerTicks(granted);
        body.setDirty(true);
        int seconds = granted / 20;
        Body.notify(healer, Component.translatable("triage.msg.painkiller", seconds),
                ChatFormatting.AQUA, SoundEvents.GENERIC_DRINK);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.painkiller", seconds),
                    ChatFormatting.AQUA, SoundEvents.GENERIC_DRINK);
        }
        return true;
    }

    /** Between two bleeds the heavy one wins; between two of a kind, the worse-off part. */
    private static @Nullable BodyPart worstBleeding(BodyHealth body) {
        BodyPart target = null;
        int worstLevel = 0;
        float worstRatio = Float.MAX_VALUE;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!state.bleeding()) {
                continue;
            }
            if (state.bleed > worstLevel || (state.bleed == worstLevel && state.ratio() < worstRatio)) {
                worstLevel = state.bleed;
                worstRatio = state.ratio();
                target = part;
            }
        }
        return target;
    }

    /**
     * What to say when a jar is held over a burn it cannot reach.
     *
     * <p>"Nothing is burnt" is the wrong sentence entirely, and it is the sentence that
     * would have been said: the salve is refused on a full thickness burn, and the part
     * is very obviously burnt. This is the only refusal in the mod that depends on which
     * part was named, because it is the only one where the reason for the refusal is a
     * fact about the wound rather than its absence.
     */
    public static String nothingToDoKey(MedicineKind kind, @Nullable BodyPart part, BodyHealth body) {
        if (kind == MedicineKind.SALVE && part != null) {
            PartState state = body.part(part);
            if (state.burnFull()) {
                return "triage.msg.burn_too_deep";
            }
            if (state.burn == PartState.BURN_SUPERFICIAL) {
                return "triage.msg.burn_will_close";
            }
        }
        return nothingToDoKey(kind);
    }

    public static String nothingToDoKey(MedicineKind kind) {
        return switch (kind) {
            case BANDAGE -> "triage.msg.nothing_bleeding";
            case TOURNIQUET -> "triage.msg.nothing_to_strap";
            case SPLINT -> "triage.msg.nothing_broken";
            case MEDKIT -> "triage.msg.nothing_to_heal";
            case SURGERY -> "triage.msg.nothing_blacked";
            case PAINKILLER -> "triage.msg.already_painkilled";
            case ANTISEPTIC -> "triage.msg.nothing_infected";
            case SALVE -> "triage.msg.nothing_burned";
            case ANTIBIOTICS -> "triage.msg.not_ill";
            case WORMING -> "triage.msg.no_worms";
            case TRANSFUSION -> "triage.msg.blood_full";
            case DRAW -> "triage.msg.too_little_blood";
            case DECOMPRESS -> "triage.msg.nothing_to_decompress";
            case ANTIPSYCHOTIC -> "triage.msg.nothing_to_hold";
            case ANAESTHETIC -> "triage.msg.already_under";
            case SCISSORS -> "triage.msg.nothing_dressed";
            case EXTRACT -> "triage.msg.nothing_stuck";
            case AMPUTATE -> "triage.msg.nothing_dead";
            case PEG, HOOK, JOINT, HAND -> "triage.msg.nothing_to_fit";
            case GRAFT -> "triage.msg.nothing_to_graft";
            // The needles are always work by hasWork, so these are only reached if that
            // ever changes. Each one still says the true thing for its own refusal.
            case EPINEPHRINE, THERMOMETER, STETHOSCOPE -> "triage.msg.nothing_to_do";
            case ANTICOAGULANT -> "triage.msg.nothing_to_thin";
            case MORPHINE -> "triage.msg.already_painkilled";
            case NALOXONE -> "triage.msg.nothing_to_reverse";
        };
    }

    /**
     * Tells the healer what they did, and the patient that it was done to them. Being
     * treated without being told would be the strangest part of playing the wounded one.
     */
    private static void done(ServerPlayer healer, ServerPlayer patient, String key, BodyPart part, SoundEvent sound) {
        Body.notify(healer, Component.translatable(key, part.title()), ChatFormatting.GREEN, sound);
        if (healer != patient) {
            Body.notify(patient, Component.translatable("triage.msg.treated_by", healer.getName(), part.title()),
                    ChatFormatting.GREEN, sound);
        }
    }

    static boolean refuse(ServerPlayer player, String key) {
        Body.notify(player, Component.translatable(key), ChatFormatting.GRAY, null);
        return false;
    }

    /**
     * Four places on a chest, and what is or is not moving under them.
     *
     * <p>The finding is an absence, like the burn press: air on both sides is the answer
     * you want and air on one side is the one that means something. It is also the only
     * finding in this mod with no free way in at all - see {@link MedicineKind#STETHOSCOPE}.
     *
     * <p>A listen that was rushed reports nothing rather than reporting wrongly. There is
     * no such thing as half-hearing a silent lung: you either held still long enough at
     * enough places to be sure, or you have a chest you have not listened to.
     */
    private static boolean listen(ServerPlayer healer, ServerPlayer patient, BodyHealth body,
                                  float reading) {
        PartState chest = body.part(BodyPart.THORAX);
        boolean self = healer == patient;
        if (reading < 0.6F) {
            Body.notify(healer, Component.translatable("triage.msg.listen_rushed"),
                    ChatFormatting.GRAY, null);
            return true;
        }
        body.establish(BodyPart.THORAX, Findings.BREATH, healer.level().getGameTime());
        // And the count itself, which is the only place in the mod a breathing rate is
        // ever actually taken by a person rather than worked out by a screen.
        body.noteBreath(Breathing.of(body, Config.values().airwaySeconds).rate(),
                healer.getGameProfile().getName(), healer.level().getGameTime());
        Trend.say(healer, body, Reading.BREATH);
        Body.sync(patient);

        // Three answers, and they are the three things a chest can be doing. A blocked
        // airway is quiet everywhere and is the one to act on first; a lung that has come
        // away is quiet on its own side; anything else is a chest with air going in and
        // out of it, which is worth hearing too.
        String key = body.airwayBlocked(Config.values().airwaySeconds * 20) ? "triage.msg.listen_silent"
                : chest.pneumo ? "triage.msg.listen_one_side"
                : "triage.msg.listen_clear";
        Body.notify(healer, Component.translatable(key,
                        self ? Component.translatable("triage.screen.you") : patient.getName()),
                chest.pneumo || body.airwayBlocked(Config.values().airwaySeconds * 20) ? ChatFormatting.GOLD : ChatFormatting.GREEN, null);
        return true;
    }
}
