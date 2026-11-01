package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;

/**
 * Burns, which this mod has been handing out since the first version and has never had.
 *
 * <p>There was a flag. A part was burnt or it was not; a burnt part did not heal until
 * somebody rubbed a salve on it, and that was the whole of it. For an injury Minecraft
 * deals out more freely than any other - lava, fire, a campfire walked over, a blaze, a
 * bed in the wrong dimension - that is the thinnest thing in here, and it was thin in the
 * way that matters: it had no decisions in it.
 *
 * <p>Four facts turn it into an injury, and they are the four anybody who has dealt with
 * one would name.
 *
 * <p><b>A burn has a depth, and depth is everything.</b> Red heals itself. Blistered wants
 * a salve. Through the skin will not close at all and no salve will reach it. Nothing else
 * about a burn decides as much - not where it is, not how big it is, not what caused it.
 *
 * <p><b>The worst one does not hurt.</b> The nerve endings that would have done the hurting
 * are part of what burnt. So the burn somebody is complaining loudest about is the one
 * that is going to be fine, and the quiet patch beside it is the one that needs a knife.
 * This mod has never had an injury that lies by being painless, and now it has the one
 * that does it in life.
 *
 * <p><b>It is not over when the fire is out.</b> Heat sitting in tissue keeps cooking it
 * for a good while afterwards, and a burn that was superficial when the flames went out
 * can be full thickness a minute later. That is the reason the first thing anybody is
 * taught about burns is water, and to keep it there - and it is the only window in which
 * anything anybody does changes how deep it ends up. Everything after that window is
 * treating what you already have.
 *
 * <p><b>What kills is the size, not the depth.</b> A hand burnt to the bone is a bad hand.
 * A third of somebody burnt at all is a person who empties out through it: fluid leaves
 * through the missing skin for hours, and what they need is volume, faster than it is
 * going. The mod already had a bag of saline and a way to lose volume and a shock that
 * arrives when there is not enough, so all this had to supply was the reason - and the
 * reason is arithmetic anybody can do at a glance, which is what the rule of nines is.
 *
 * @see BodyPart#surface()
 */
public final class Burns {
    /** How far off a hundred somebody has to be burnt before it starts costing volume. */
    private static final float SMALL = 0.001F;

    private Burns() {
    }

    // ---- catching one ------------------------------------------------------

    /**
     * Fire has reached somebody.
     *
     * <p>What arrives is always the shallowest kind, however hot the thing was, and then
     * it is given a window in which it can become the others. That is deliberate and it
     * is the whole design: a burn should never be decided by the tick it happened on,
     * because in life it is not, and because a thing decided instantly is a thing nobody
     * can do anything about.
     *
     * @return true when this put a new burn on
     */
    public static boolean caught(ServerPlayer player, BodyHealth body, BodyPart part) {
        Config.Values config = Config.values();
        PartState state = body.part(part);
        if (state.gone()) {
            return false;
        }
        boolean fresh = !state.burned;
        if (!state.burn(PartState.BURN_SUPERFICIAL, config.burnCookSeconds * 20)) {
            return false;
        }
        body.setDirty(true);
        if (fresh) {
            Reactions.say(player, "burn");
            Body.notify(player, Component.translatable("triage.msg.burned", part.title()),
                    ChatFormatting.GOLD, SoundEvents.FIRE_EXTINGUISH);
        }
        return true;
    }

    // ---- one second of having them -----------------------------------------

    public static void tick(ServerPlayer player, BodyHealth body, int interval) {
        Config.Values config = Config.values();
        if (!config.burns) {
            return;
        }
        cooking(player, body, config, interval);
        fades(player, body, config, interval);
        breathed(player, body, config);
        airway(player, body, config);
        float extent = extent(body);
        if (extent > SMALL) {
            fluid(player, body, config, extent, interval);
            heat(body, config, extent, interval);
        }
    }

    /**
     * The window, and the two things that can happen in it.
     *
     * <p>Water is the answer and it is free, which is the point: this is the second rule
     * in the mod whose answer costs no item and no craft, and the first one - stand still
     * and your arms come back - is the one the mod is proudest of. Get in the water, stay
     * in it, and a burn stops where it is and can be talked back a step. Walk it off and
     * it goes on cooking.
     *
     * <p>Snow and ice are not water. Packing a burn in something freezing shuts down what
     * little blood is still reaching it and takes it deeper, which is a mistake people
     * make in the real world often enough that every first aid course says not to.
     */
    private static void cooking(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        boolean cooling = player.isInWater();
        boolean freezing = player.isInPowderSnow || player.isFullyFrozen();
        boolean wet = cooling || player.isInWaterOrRain();
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!state.burnFresh()) {
                continue;
            }
            // Water shortens the window as well as holding the depth: what it is doing
            // is taking the heat out, and a burn with the heat out of it has stopped
            // deciding anything.
            state.burnTicks = Math.max(0, state.burnTicks - interval * (cooling ? 2 : 1));
            body.setDirty(true);

            if (cooling) {
                // Held under long enough and the burn does not merely stop: some of what
                // was going to be lost is not lost. This is the only thing in the mod
                // that undoes an injury rather than treating it, and it is the one place
                // where that is true in life as well.
                if (state.burnTicks <= 0 && state.burn > PartState.BURN_SUPERFICIAL
                        && player.getRandom().nextFloat() < config.burnCoolBack) {
                    state.burn--;
                    Body.notify(player, Component.translatable("triage.msg.burn_cooled",
                            part.title()), ChatFormatting.AQUA, SoundEvents.FIRE_EXTINGUISH);
                }
                continue;
            }
            float chance = config.burnDeepenChance * (freezing ? config.burnIceFactor : 1.0F);
            if (!wet && player.getRandom().nextFloat() < chance
                    && state.burn < PartState.BURN_FULL) {
                state.burn++;
                state.burned = true;
                Body.notify(player, Component.translatable(
                                freezing ? "triage.msg.burn_iced" : "triage.msg.burn_deeper",
                                part.title(), depth(state.burn)),
                        ChatFormatting.RED, SoundEvents.LAVA_POP);
            }
        }
    }

    /**
     * The shallow one closing on its own.
     *
     * <p>Red skin heals, and a mod that made every brush with a campfire into an errand
     * for a jar of salve would be wrong about burns in the direction that annoys rather
     * than the direction that teaches. Only once the window is shut: a burn still deciding
     * how deep it is has not started closing.
     */
    private static void fades(ServerPlayer player, BodyHealth body, Config.Values config, int interval) {
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (state.burn != PartState.BURN_SUPERFICIAL || state.burnTicks > 0) {
                continue;
            }
            // Counted on the same tick field, which is free the moment the window shuts:
            // it runs negative and the fade is how far below zero it has got.
            state.burnTicks -= interval;
            if (state.burnTicks > -config.burnFadeSeconds * 20) {
                continue;
            }
            state.clearBurn();
            body.setDirty(true);
            Body.notify(player, Component.translatable("triage.msg.burn_faded", part.title()),
                    ChatFormatting.GREEN, null);
        }
    }

    /**
     * A lungful of it.
     *
     * <p>Standing in fire means breathing what is coming off it, and the air above a fire
     * is hot enough to scald an airway from the inside. This is the half of burning that
     * kills quickly: everything on the outside of somebody is measured in hours, and an
     * airway is measured in minutes.
     *
     * <p>Nothing about it is visible. It is caught in the fire and it shows up as a voice
     * and a rate, which is why the mod says it rather than drawing it - and why somebody
     * who walked out of a burning room looking fine is the patient worth watching.
     */
    private static void breathed(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (config.smokeChance <= 0.0F || body.has(Disease.SMOKE)) {
            return;
        }
        // Being on fire is the test: the air you are drawing is the air coming off you.
        // Fire resistance means nothing is coming off you at all.
        boolean breathing = player.getRemainingFireTicks() > 0 || player.isInLava();
        if (!breathing || player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return;
        }
        if (player.getRandom().nextFloat() >= config.smokeChance) {
            return;
        }
        if (body.catchDisease(Disease.SMOKE)) {
            Body.notify(player, Component.translatable("triage.msg.smoke"),
                    Disease.SMOKE.colour(), SoundEvents.PLAYER_HURT);
        }
    }

    /**
     * And the way it ends, when it ends badly.
     *
     * <p>Not a dice roll on everybody who breathed smoke. The thing that closes an airway
     * is the same heat having reached the face, and a face burnt through is the oldest
     * warning sign in the trade - it is what makes an airway burn something you can see
     * coming rather than something that happens. So the two together do it, and either
     * one alone does not.
     *
     * <p>What answers it is a pair of hands, which the mod already has: the airway can be
     * held open by anybody standing there, over and over, for as long as it takes. There
     * is no item for this and there should not be. The item would be a tube down the
     * throat, and that is further than this mod goes.
     */
    private static void airway(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!body.has(Disease.SMOKE) || body.arrested() || body.part(BodyPart.HEAD).burn < PartState.BURN_FULL) {
            return;
        }
        // Late rather than immediate: swelling takes the time it takes, and a warning
        // nobody has time to act on is not a warning.
        int run = Math.max(1, config.smokeSeconds * 20);
        if (body.diseaseTicks(Disease.SMOKE) < run / 3) {
            return;
        }
        if (player.getRandom().nextFloat() >= config.smokeChance * 0.1F) {
            return;
        }
        Body.notify(player, Component.translatable("triage.msg.airway_shut"),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
        Downed.stopBreathing(player, body);
    }

    /**
     * What the size of it costs, every second, for as long as it is open.
     *
     * <p>Only the ones deep enough to have taken the skin off count. A superficial burn
     * is left out of the arithmetic here for the same reason it is left out of it in a
     * hospital: it is painful and it is not a hole.
     */
    private static void fluid(ServerPlayer player, BodyHealth body, Config.Values config,
                              float extent, int interval) {
        float share = extent - config.burnFluidFloor;
        if (share <= 0.0F) {
            return;
        }
        float seconds = interval / 20.0F;
        body.addBloodLoss(config.burnFluidDrain * share * seconds);
        if (player.tickCount % 200 == 0 && share > 0.15F) {
            Body.notify(player, Component.translatable("triage.msg.burn_drying"),
                    ChatFormatting.GOLD, null);
        }
    }

    /**
     * And what it costs in heat.
     *
     * <p>Skin is the thing that keeps heat in, so somebody missing a third of theirs is
     * cooling whatever they do. It feeds the same number an operation feeds, which means
     * the circle the mod already has closes around burns too: cold does not clot, and a
     * big burn is a cold patient.
     */
    private static void heat(BodyHealth body, Config.Values config, float extent, int interval) {
        float seconds = interval / 20.0F;
        body.chill = Mth.clamp(body.chill + config.burnChill * extent * seconds, 0.0F, 1.0F);
    }

    // ---- reading them ------------------------------------------------------

    /**
     * How much of this body is burnt, 0 to 1, counting only what is deep enough to matter.
     *
     * <p>The rule of nines, added up. Which is worth saying plainly: this is not a number
     * the mod invented to have a number. It is the sum a medic does in their head in the
     * first ten seconds, and it decides more about what happens next than anything else
     * they will work out that day.
     */
    public static float extent(BodyHealth body) {
        int burnt = 0;
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!state.gone() && state.burn >= PartState.BURN_PARTIAL) {
                burnt += part.surface();
            }
        }
        return burnt / 100.0F;
    }

    /**
     * Whether this body is losing fluid through its own surface.
     *
     * <p>Which is the same question as whether it is bleeding, and gets the same answer
     * from the mod: a body cannot make volume faster than an open wound the size of a
     * limb is taking it. Resting is no use here and there is nothing to close, so the
     * only thing that answers a big burn is a bag - which is the whole reason burns and
     * saline are in the same version of this changelog.
     */
    public static boolean draining(BodyHealth body) {
        Config.Values config = Config.values();
        return config.burns && extent(body) > config.burnFluidFloor;
    }

    /** The deepest burn on this body, for the one line that has to summarise them. */
    public static int worst(BodyHealth body) {
        int worst = PartState.BURN_NONE;
        for (BodyPart part : BodyPart.VALUES) {
            worst = Math.max(worst, body.part(part).burn);
        }
        return worst;
    }

    /** What a depth is called. */
    public static Component depth(int burn) {
        return Component.translatable(switch (burn) {
            case PartState.BURN_SUPERFICIAL -> "triage.burn.superficial";
            case PartState.BURN_PARTIAL -> "triage.burn.partial";
            case PartState.BURN_FULL -> "triage.burn.full";
            default -> "triage.burn.none";
        });
    }

    public static ChatFormatting colour(int burn) {
        return switch (burn) {
            case PartState.BURN_SUPERFICIAL -> ChatFormatting.YELLOW;
            case PartState.BURN_PARTIAL -> ChatFormatting.GOLD;
            case PartState.BURN_FULL -> ChatFormatting.DARK_RED;
            default -> ChatFormatting.GRAY;
        };
    }
}
