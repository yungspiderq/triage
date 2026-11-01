package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.claudemods.triage.compat.Ragdolls;
import com.claudemods.triage.compat.Tacz;
import com.claudemods.triage.data.Milestones;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import org.jetbrains.annotations.Nullable;

/**
 * Turns one hit into what it does to a body: which part loses health, whether it starts
 * bleeding, whether the bone under it cracks or breaks outright - and telling the
 * player about it, because none of this is visible from the vanilla health bar.
 */
public final class Injuries {
    /** However hard the hit, a cut is never certain. */
    private static final float MAX_BLEED_CHANCE = 0.6F;
    private static final float MAX_BURN_CHANCE = 0.7F;

    private Injuries() {
    }

    /**
     * An ordinary blow. Where it lands is measured if it can be measured and rolled if
     * it cannot, and it hurts more where there is no armour over it.
     */
    public static void blow(ServerPlayer player, BodyHealth body, float amount, DamageSource source) {
        BodyPart aimed = Config.values().aimedHits ? DamageRouting.aimed(source, player) : null;
        BodyPart part = present(body, aimed != null
                ? aimed : DamageRouting.roll(player.getRandom(), source, player));
        float multiplier = DamageRouting.covered(player, part) ? 1.0F : Config.values().uncoveredMultiplier;
        apply(player, body, part, amount * multiplier, true, source);
        arrow(player, body, part, source);
        bullet(player, body, part, source);
        // And the other thing that can stay in, which is not ammunition but somebody's
        // weapon - so it is taken off them as well as left in here. See Stuck.
        Stuck.caught(player, body, part, source, amount * multiplier);
    }

    /**
     * The one that stays in.
     *
     * <p>An arrow that lodges is not more damage - the damage already happened on the way
     * in. What it is, is a wound that cannot be dealt with while it is there: it will not
     * clot, it will not mend, and a bandage over the top of it is a bandage over an arrow.
     *
     * <p>Only arrows here. A blade can stay in somebody too, but it is not this: an arrow
     * is spent ammunition and a sword belongs to whoever was holding it, so the two have
     * different odds, a different price and different owners. See {@link Stuck}.
     */
    private static void arrow(ServerPlayer player, BodyHealth body, BodyPart part, DamageSource source) {
        Config.Values config = Config.values();
        if (!config.arrows || !source.is(DamageTypes.ARROW)) {
            return;
        }
        if (player.getRandom().nextFloat() >= config.arrowChance) {
            return;
        }
        body.part(part).arrows++;
        body.setDirty(true);
        Reactions.say(player, "shot");
        Body.notify(player, Component.translatable("triage.msg.arrow_stuck", part.title()),
                ChatFormatting.RED, SoundEvents.ARROW_HIT);
    }

    /**
     * The other thing that stays in, and the one that does not stand out to be pulled.
     *
     * <p>A round is under the skin. It is the same problem as an arrow - nothing here
     * will clot or mend around it, and a dressing over it is a dressing over a bullet -
     * but it cannot be drawn back out along its own track, so the only way out is to
     * open the wound and pick it out. That is the same forceps job as a fragment, and
     * it is deliberately not the arrow's game.
     *
     * <p>No class from TaCZ is named. {@link Tacz#bullet} asks the two things that
     * survive a version bump, so a bullet from a build this was not written against is
     * still a bullet.
     */
    private static void bullet(ServerPlayer player, BodyHealth body, BodyPart part, DamageSource source) {
        Config.Values config = Config.values();
        if (!config.bulletsStick || !Tacz.bullet(source)) {
            return;
        }
        if (player.getRandom().nextFloat() >= config.bulletStickChance) {
            return;
        }
        body.part(part).addBullet(Tacz.ammo(source));
        body.setDirty(true);
        Reactions.say(player, "shot");
        Body.notify(player, Component.translatable("triage.msg.bullet_stuck", part.title()),
                ChatFormatting.RED, SoundEvents.ARROW_HIT);
    }

    /**
     * A broken rib pushed the wrong way.
     *
     * <p>Only ever after the ribs have already gone, because that is what actually does
     * it: the chance is not on the blow, it is on what the blow did to the bone first.
     * That makes it the one injury in the mod that is a consequence of another injury
     * rather than of a hit, and it is why a cracked chest is worth splinting even though
     * a crack by itself is survivable.
     */
    private static void pneumothorax(ServerPlayer player, BodyHealth body, PartState state,
                                     float amount, Config.Values config) {
        if (!config.pneumothorax || state.pneumo || (!state.fractured() && !state.cracked)) {
            return;
        }
        // Every broken rib is another end that can go the wrong way, so the chance rides
        // on how many there are. A crack alone is worth half of one.
        float chance = Math.min(1.0F, config.pneumoChance
                * (state.fractured() ? state.breaks : 0.5F));
        if (amount < config.pneumoBlow || player.getRandom().nextFloat() >= chance) {
            return;
        }
        state.pneumo = true;
        state.pneumoTicks = 0;
        body.setDirty(true);
        Body.notify(player, Component.translatable("triage.msg.pneumo"),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_BIG_FALL);
    }

    /**
     * A fall. Both legs take it, the one that landed first takes more, and a hard enough
     * landing breaks it. Whatever the legs cannot absorb runs on into the thorax, so a
     * long drop still kills.
     */
    public static void fall(ServerPlayer player, BodyHealth body, float amount, float rawDamage) {
        // Only a body that is on its feet lands on them. One that has been tumbling
        // through the air on a physics engine arrives on whatever happened to be
        // underneath, and sending that into the legs is how somebody ends up with two
        // broken shins after landing head first.
        //
        // Not also checking for downed, though a body on the floor is just as sideways:
        // a blow on somebody who is already down never reaches here at all. It is turned
        // into seconds off their clock before the damage is ever routed - see DamageEvents.
        if (Ragdolls.holding(player)) {
            tumble(player, body, amount, rawDamage);
            return;
        }
        BodyPart rolled = DamageRouting.rollLeg(player.getRandom());
        BodyPart first = present(body, rolled);
        BodyPart other = present(body, rolled == BodyPart.LEFT_LEG
                ? BodyPart.RIGHT_LEG : BodyPart.LEFT_LEG);

        apply(player, body, first, amount * 0.6F, false);
        apply(player, body, other, amount * 0.4F, false);

        Config.Values config = Config.values();
        if (!config.fractures) {
            return;
        }
        PartState state = body.part(first);
        if (rawDamage >= config.fractureFall && player.getRandom().nextFloat() < config.fractureChance) {
            if (state.fracture(first.bone().count())) {
                body.setDirty(true);
                announceFracture(player, first);
                Bones.broke(player, body, first, amount);
            }
        } else if (rawDamage >= config.fractureFall * 0.5F && !state.cracked && !state.fractured()
                && player.getRandom().nextFloat() < config.crackChance) {
            state.crack();
            body.setDirty(true);
            announceCrack(player, first);
        }
    }

    /**
     * A landing Triage has to deal itself, because nothing else will.
     *
     * <p>Armour is not consulted and neither is resistance, which is the honest answer
     * rather than a shortcut: a body bouncing off the ground on its shoulder did not land
     * on its boots, and there is nothing between it and the floor to reduce anything.
     *
     * @param blocks how far it fell, measured rather than guessed
     */
    public static void landed(ServerPlayer player, BodyHealth body, double blocks) {
        float raw = (float) Math.max(0.0, blocks - 3.0);
        if (raw <= 0.0F) {
            return;
        }
        tumble(player, body, raw * Config.values().damageScale, raw);
    }

    /**
     * A fall taken by a body that was not standing up.
     *
     * <p>Which part hits first is rolled on the ordinary hit weights rather than being
     * the legs, so a chest or a head is a real outcome. It cannot be measured - the
     * ragdoll mod publishes no way to ask where a limb is, and reading that out of its
     * internals would be a compat that breaks on their next release - but a body that has
     * been rolling down a hill genuinely does land on an arbitrary part of itself, so the
     * roll is not a stand-in for the truth. It is the truth.
     *
     * <p>Most of the blow goes into whatever struck, and the rest travels through the
     * body, which is what an impact does. The bone that breaks is the bone under the part
     * that hit, and landing on your head is how you get a concussion out of a fall.
     */
    private static void tumble(ServerPlayer player, BodyHealth body, float amount, float rawDamage) {
        BodyPart landed = present(body, DamageRouting.roll(player.getRandom()));
        apply(player, body, landed, amount * 0.6F, false);
        body.hurtAll(amount * 0.4F);

        Config.Values config = Config.values();
        PartState state = body.part(landed);
        if (landed == BodyPart.HEAD) {
            concussion(player, body, amount, config);
        }
        if (!config.fractures) {
            return;
        }
        if (rawDamage >= config.fractureFall && player.getRandom().nextFloat() < config.fractureChance) {
            if (state.fracture(landed.bone().count())) {
                body.setDirty(true);
                announceFracture(player, landed);
                Bones.broke(player, body, landed, amount);
            }
            // Ribs that have just gone are the only way a lung gets crushed, and landing
            // chest first is exactly how ribs go.
            pneumothorax(player, body, state, amount, config);
        } else if (rawDamage >= config.fractureFall * 0.5F && !state.cracked && !state.fractured()
                && player.getRandom().nextFloat() < config.crackChance) {
            state.crack();
            body.setDirty(true);
            announceCrack(player, landed);
        }
    }

    /**
     * Bitten by something that was not well.
     *
     * <p>Nothing about the bite says so. That is the whole design: there is no sign to
     * look for, no test to run, and the wound heals like any other. What there is, is the
     * memory of an animal that bit you - and a decision about whether to spend a course
     * of antibiotics on somebody who is perfectly well.
     */
    public static void bite(ServerPlayer player, BodyHealth body, DamageSource source) {
        Config.Values config = Config.values();
        if (!config.rabies || !config.diseases || body.has(Disease.RABIES) || body.has(Disease.FRENZY)) {
            return;
        }
        Entity attacker = source.getEntity();
        boolean carrier = attacker instanceof Wolf || attacker instanceof Fox
                || attacker instanceof Bat || attacker instanceof PolarBear
                || attacker instanceof Cat || attacker instanceof Ocelot;
        if (!carrier || player.getRandom().nextFloat() >= config.rabiesChance) {
            return;
        }
        if (body.catchDisease(Disease.RABIES)) {
            // Said once, and never mentioned again until it is too late. Which is as much
            // warning as anybody has ever had about this.
            Body.notify(player, Component.translatable("triage.msg.bitten"),
                    ChatFormatting.DARK_RED, SoundEvents.WOLF_GROWL);
        }
    }

    /**
     * Something inside gives.
     *
     * <p>Only in the three parts that have anything inside them, and only from a blow
     * hard enough to matter - which is deliberately about the same size as the one that
     * breaks a bone, because it is the same blow. A rib going in and a spleen going are
     * the same kick from different angles.
     *
     * <p>Nothing is announced to the room and nothing appears on the figure. The patient
     * is told, once, that something inside them has gone - which is what it feels like -
     * and after that the only evidence is a volume that keeps falling with nothing open.
     */
    private static void organs(ServerPlayer player, BodyHealth body, PartState state, BodyPart part, float amount,
                               Config.Values config) {
        if (!config.organs || !Organ.inside(part) || amount < config.organBlow) {
            return;
        }
        // A ribcage is a shield. The mod has said from the start that a rib going in
        // and a spleen going are the same kick from different angles - so a kick that
        // finds a chest already caved in finds what is behind it far more easily, and
        // one that finds it whole mostly does not get through at all.
        //
        // Which quietly turns a broken rib from a thing that hurts into a thing that
        // has stopped protecting you, and gives the splint a second reason to exist
        // that nobody has to be told about: the second blow is the dangerous one.
        float chance = config.organChance;
        if (state.fractured()) {
            chance *= config.organBrokenBonus;
        } else if (state.cracked) {
            chance *= 1.0F + (config.organBrokenBonus - 1.0F) * 0.5F;
        }
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }
        // Which one is luck, because which one it is in life is luck: a kick to a belly
        // finds the spleen or it finds the gut, and the difference is which way the boot
        // was pointing.
        Organ[] inside = Organ.in(part);
        Organ organ = inside[player.getRandom().nextInt(inside.length)];
        int was = state.organ(organ);
        // A hard blow tears; a merely hard one bruises. Twice the threshold is the line.
        state.tearOrgan(organ, amount >= config.organBlow * 2.0F
                ? PartState.ORGAN_TORN : PartState.ORGAN_BRUISED);
        int now = state.organ(organ);
        if (now <= was) {
            return;
        }
        if (now >= PartState.ORGAN_TORN) {
            Body.notify(player, Component.translatable("triage.msg.organ_torn", organ.title()),
                    ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
            // A lung with a hole in it does the other thing this mod already knows about.
            if (organ.lung() && config.pneumothorax) {
                state.pneumo = true;
            }
            // And the heart is the one organ that does not give anybody three minutes.
            // It tears at the same threshold as the rest; when it does, this is the
            // chance the body stops rather than merely emptying into its own chest.
            if (organ == Organ.HEART && config.downed && config.cpr && config.arrestSeconds > 0
                    && player.getRandom().nextFloat() < config.organHeartStopChance) {
                Downed.stopBreathing(player, body);
            }
        } else {
            Body.notify(player, Component.translatable("triage.msg.organ_bruised", organ.title()),
                    ChatFormatting.GOLD, null);
        }
    }

    /**
     * Damage with no point of impact: spread it. Nothing bleeds or breaks from this,
     * but fire leaves its mark on whatever it caught.
     */
    public static void systemic(ServerPlayer player, BodyHealth body, float amount, boolean fire) {
        boolean[] wasBlacked = new boolean[BodyPart.VALUES.length];
        for (int i = 0; i < BodyPart.VALUES.length; i++) {
            wasBlacked[i] = body.part(BodyPart.VALUES[i]).blacked();
        }

        body.hurtAll(amount);

        Config.Values config = Config.values();
        if (fire && config.burns && player.getRandom().nextFloat() < Math.min(MAX_BURN_CHANCE, amount * config.burnChance)) {
            // What lands is always the shallowest kind, and then it cooks. How deep it
            // ends up is not decided here and is not decided by how hot the thing was:
            // it is decided over the next half minute, by whoever is standing in it and
            // whether they get to water. See Burns.
            BodyPart burnt = present(body, DamageRouting.roll(player.getRandom()));
            if (Burns.caught(player, body, burnt)) {
                return;
            }
        }

        for (int i = 0; i < BodyPart.VALUES.length; i++) {
            BodyPart part = BodyPart.VALUES[i];
            if (!wasBlacked[i] && body.part(part).blacked() && !part.vital()) {
                announceBlackout(player, part);
                return;
            }
        }
    }

    /**
     * The one place a part actually loses health to an outside hit.
     *
     * @param canWound whether this kind of damage is able to cut or break anything
     */
    /**
     * Where a blow lands when the place it was aimed at is not on the body any more.
     *
     * <p>A swing at the space where somebody's arm used to be does not miss - it hits
     * them. Without this the safest thing in the mod would be to have no arms: a quarter
     * of every roll would land on nothing and quietly vanish, which would make the worst
     * outcome in the game into armour.
     */
    public static BodyPart present(BodyHealth body, BodyPart part) {
        return body.part(part).gone() ? BodyPart.THORAX : part;
    }

    public static void apply(ServerPlayer player, BodyHealth body, BodyPart part, float amount, boolean canWound) {
        apply(player, body, part, amount, canWound, null);
    }

    /**
     * The same blow, with the thing that swung it still attached.
     *
     * <p>Only one rule in here asks: whether a limb comes away depends on whether there
     * was an edge on it, and that is a fact about the weapon rather than about the
     * damage. Everything that calls the shorter form is something with no weapon behind
     * it - a bleed, an infection, a strap - and none of those has ever taken an arm off.
     */
    public static void apply(ServerPlayer player, BodyHealth body, BodyPart part, float amount,
                             boolean canWound, @Nullable DamageSource source) {
        if (amount <= 0.0F) {
            return;
        }
        PartState state = body.part(part);
        amount = capOneShot(state, part, amount);
        boolean wasBlacked = state.blacked();
        int wasBleeding = state.bleed;
        int wasBreaks = state.breaks;
        boolean wasCracked = state.cracked;

        body.hurt(part, amount);

        // And the chart no longer describes this part. A finding is a statement about a
        // body at a moment, and the moment ended when something hit it: the leg somebody
        // felt along two minutes ago and found sound is not the leg that has just taken a
        // fall. In arcade nothing reads this; in realistic it is what stops one careful
        // examination from being true forever.
        body.forget(part);

        // Any real blow is a reason for a knitting crack to start its count again.
        if (state.cracked && amount >= 1.0F) {
            state.crackTicks = 0;
        }

        if (canWound) {
            Config.Values config = Config.values();
            float chance = Math.min(MAX_BLEED_CHANCE, amount * config.bleedChancePerDamage);
            if (player.getRandom().nextFloat() < chance) {
                int level = amount >= config.heavyBleedThreshold ? PartState.BLEED_HEAVY : PartState.BLEED_LIGHT;
                int ticks = config.lightBleedSeconds <= 0 ? PartState.BLEED_FOREVER : config.lightBleedSeconds * 20;
                // And, rarely, the one underneath. A blow that was already going to open
                // a heavy bleed is the only one that can reach a vessel, because that is
                // the honest relationship between the two: an artery is not a worse cut,
                // it is the same cut that happened to land half an inch deeper.
                if (level == PartState.BLEED_HEAVY && arterial(player, state, part, amount, config)) {
                    level = PartState.BLEED_ARTERIAL;
                }
                state.startBleeding(level, ticks);
            }
            bones(player, body, state, part, amount, config);
            organs(player, body, state, part, amount, config);
            // And the other way a limb comes off without anybody choosing it. Dead flesh
            // is not attached to anything - it is held on by what has already stopped
            // working - so a hard enough blow to a limb that has died takes it away.
            //
            // Which quietly turns the necrosis clock from a deadline into a warning: past
            // the end of it you are not only carrying something useless, you are carrying
            // something that can be knocked off you.
            if (config.amputation && state.necrotic() && !state.gone()
                    && amount >= config.tearOffBlow
                    && player.getRandom().nextFloat() < config.tearOffChance) {
                Blast.torn(player, body, part, config);
                return;
            }
            // And the way limbs actually come off, which the mod did not have: an edge,
            // swung hard, into a limb. Everything else here was rare enough to be a
            // story - a blast, or a saw somebody chose to pick up - so in eight versions
            // nobody ever lost an arm to the thing that takes arms off.
            if (severed(player, body, state, part, amount, source, config)) {
                return;
            }
            if (part == BodyPart.HEAD) {
                concussion(player, body, amount, config);
            }
            if (part == BodyPart.THORAX) {
                pneumothorax(player, body, state, amount, config);
            }
        }

        // Something sudden enough for a body to answer chemically. Bones and blackouts,
        // not scratches: a body that did this every time it was grazed would never be
        // off it.
        if (state.breaks > wasBreaks || (!wasBlacked && state.blacked())) {
            Adrenaline.surge(player, body);
        }

        // One line per hit, worst news first.
        if (!wasBlacked && state.blacked() && !part.vital()) {
            announceBlackout(player, part);
        } else if (state.breaks > wasBreaks) {
            announceFracture(player, part);
        } else if (!wasCracked && state.cracked) {
            announceCrack(player, part);
        } else if (state.bleed > wasBleeding) {
            if (state.arterial()) {
                // Its own line, its own colour and its own noise, because it is not the
                // same news. Everything else on this list is something to deal with; this
                // is something to deal with now, and the mod says so in the only way it
                // has of saying anything at all.
                Reactions.say(player, "artery");
                Body.notify(player, Component.translatable("triage.msg.arterial_bleed", part.title()),
                        ChatFormatting.DARK_RED, SoundEvents.PLAYER_ATTACK_CRIT);
            } else {
                Body.notify(player, Component.translatable(
                                state.bleed == PartState.BLEED_HEAVY ? "triage.msg.heavy_bleed" : "triage.msg.light_bleed",
                                part.title()),
                        ChatFormatting.RED, null);
            }
        }
        if (state.bleed > wasBleeding) {
            Milestones.onBleedingStarted(player);
        }
    }

    /**
     * Whether this blow found a vessel rather than the meat around it.
     *
     * <p>Three things have to be true at once, which is what keeps it rare enough to
     * stay frightening. It has to be a hard blow; it has to be somewhere there is a big
     * vessel close to the surface, which is the limbs and the neck and not the belly;
     * and it has to be unlucky.
     *
     * <p>A part that is already open is likelier, because the wound is already there and
     * the second blow is going into it. That is the same rule the ribs and the organs
     * already live under, and it is the same reason: the dangerous hit is the next one.
     */
    private static boolean arterial(ServerPlayer player, PartState state, BodyPart part,
                                    float amount, Config.Values config) {
        if (!config.arterial || state.arterial() || amount < config.arterialBlow) {
            return false;
        }
        // The femoral, the brachial, the carotid. There is nothing in a belly or a chest
        // that this mod can pretend a bandage would reach, and inventing an artery there
        // would be inventing a wound with no answer - which is not difficulty, it is a
        // death sentence with extra steps.
        if (part == BodyPart.THORAX || part == BodyPart.STOMACH) {
            return false;
        }
        float chance = config.arterialChance * (state.bleeding() ? 1.6F : 1.0F);
        return player.getRandom().nextFloat() < chance;
    }

    /**
     * A limb taken off by the thing that takes limbs off.
     *
     * <p>It needs an edge. A zombie's fists do not do this, a skeleton's arrow does not,
     * and a fall certainly does not - what does it is a blade, swung hard, and mostly
     * into a limb that has already been beaten past the point of being a limb.
     *
     * <p>A clean stroke through a sound arm is in here too and is deliberately close to
     * impossible, because that is how close to impossible it is: what takes a whole
     * healthy limb off in one blow is not a sword fight, it is an accident with
     * machinery. Set the chance to zero for a game where only ruined limbs come away.
     */
    private static boolean severed(ServerPlayer player, BodyHealth body, PartState state, BodyPart part,
                                   float amount, @Nullable DamageSource source, Config.Values config) {
        if (!config.severing || !config.amputation || part.vital() || state.gone()) {
            return false;
        }
        if (amount < config.severBlow || !sharp(source)) {
            return false;
        }
        float chance = state.blacked() ? config.severChance : config.severHealthyChance;
        if (chance <= 0.0F || player.getRandom().nextFloat() >= chance) {
            return false;
        }
        Blast.torn(player, body, part, config);
        return true;
    }

    /**
     * Whether whatever did this had an edge on it.
     *
     * <p>Read off the weapon in the attacker's hand rather than off the damage type,
     * because the damage type for a zombie's fists and for a zombie holding a stolen axe
     * is the same damage type - and the difference between those two is the entire rule.
     */
    private static boolean sharp(@Nullable DamageSource source) {
        if (source == null || !(source.getDirectEntity() instanceof LivingEntity attacker)) {
            return false;
        }
        ItemStack weapon = attacker.getMainHandItem();
        return weapon.getItem() instanceof AxeItem || weapon.getItem() instanceof SwordItem;
    }

    /**
     * Stops a single blow from taking a healthy head or chest to nothing.
     *
     * <p>A drawn bow to the chest killing outright is perfectly realistic, and it
     * is also the whole game ending before the player had anything to decide. Once
     * they are already hurt the protection lapses and a finishing blow lands like
     * any other - being wounded is meant to be dangerous.
     */
    private static float capOneShot(PartState state, BodyPart part, float amount) {
        Config.Values config = Config.values();
        if (!part.vital() || config.oneShotProtectionAbove <= 0.0F) {
            return amount;
        }
        if (state.ratio() <= config.oneShotProtectionAbove) {
            return amount;
        }
        float floor = Math.min(config.oneShotFloor, state.health);
        return Math.min(amount, state.health - floor);
    }

    /**
     * A hard knock to the head. Nothing cuts and no bone moves, so it leaves no mark on
     * the part itself - what it takes away is the player's read on their own body, until
     * it wears off. A second knock on a ringing head starts the count again rather than
     * doing nothing, which is the difference between a rule and a formality.
     */
    private static void concussion(ServerPlayer player, BodyHealth body, float amount, Config.Values config) {
        if (!config.diseases || config.concussionBlow <= 0.0F || amount < config.concussionBlow) {
            return;
        }
        if (player.getRandom().nextFloat() >= config.concussionChance) {
            return;
        }
        if (body.has(Disease.CONCUSSION)) {
            body.addDiseaseTicks(Disease.CONCUSSION, -body.diseaseTicks(Disease.CONCUSSION));
            return;
        }
        body.catchDisease(Disease.CONCUSSION);
        // Its own line rather than the one the illnesses share: nobody comes down with a
        // concussion, they get hit in the head.
        Body.notify(player, Component.translatable("triage.msg.concussed"),
                Disease.CONCUSSION.colour(), SoundEvents.ANVIL_LAND);
    }

    /**
     * What the blow did to the bone. A hit hard enough breaks it outright; a lesser one
     * cracks it; and a hit on a bone that is already cracked is how most fractures
     * actually happen - which is the point of cracks existing at all.
     */
    private static void bones(ServerPlayer player, BodyHealth body, PartState state, BodyPart part,
                              float amount, Config.Values config) {
        int limit = part.bone().count();
        if (!config.fractures || state.breaks >= limit) {
            return;
        }
        // Where a bone has knitted before, it takes less of a blow to find it again.
        // Not because knitted bone is weak - it is not - but because whatever put this
        // body on the floor hard enough to break that femur is the thing this body keeps
        // doing, and the femur is where it keeps landing.
        float fragility = Scars.fragility(state);
        if (amount >= config.fractureBlow * fragility
                && player.getRandom().nextFloat() < config.fractureChance) {
            if (state.fracture(limit)) {
                Bones.broke(player, body, part, amount);
            }
        } else if (state.cracked && amount >= config.crackBlow * fragility
                && player.getRandom().nextFloat() < config.crackBreakChance) {
            if (state.fracture(limit)) {
                Bones.broke(player, body, part, amount);
            }
        } else if (!state.cracked && !state.fractured() && amount >= config.crackBlow * fragility
                && player.getRandom().nextFloat() < config.crackChance) {
            state.crack();
        }
    }

    private static void announceBlackout(ServerPlayer player, BodyPart part) {
        Body.notify(player, Component.translatable("triage.msg.blacked", part.title()),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_BIG_FALL);
    }

    private static void announceFracture(ServerPlayer player, BodyPart part) {
        Reactions.say(player, "break");
        Body.notify(player, Component.translatable("triage.msg.fractured", part.title()),
                ChatFormatting.GOLD, SoundEvents.BONE_BLOCK_BREAK);
    }

    private static void announceCrack(ServerPlayer player, BodyPart part) {
        Body.notify(player, Component.translatable("triage.msg.cracked", part.title()),
                ChatFormatting.YELLOW, SoundEvents.BONE_BLOCK_HIT);
    }
}
