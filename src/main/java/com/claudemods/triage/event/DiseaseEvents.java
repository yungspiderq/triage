package com.claudemods.triage.event;

import net.minecraft.world.effect.MobEffectInstance;
import com.mojang.datafixers.util.Pair;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.Config;
import com.claudemods.triage.body.Ailments;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.body.Reactions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * The one way to fall ill that has nothing to do with wounds: eating something that
 * deserved it.
 *
 * <p>Which foods those are is not a list. Anything whose own effects are bad for you -
 * rotten flesh, spider eyes, raw chicken, pufferfish - qualifies, so food from other
 * mods is covered by the same rule without anyone having to enumerate it.
 */
@Mod.EventBusSubscriber(modid = TriageMod.MODID)
public final class DiseaseEvents {
    private DiseaseEvents() {
    }

    /**
     * The other kind of allergy, and the common one.
     *
     * <p>A drug allergy is a fact somebody else has to remember about you. This is a fact
     * about your own dinner, and it fires with nobody else in the room - which makes it
     * the only thing in this mod a player can do to themselves out of pure habit.
     *
     * <p>No warning and no refusal. The food is eaten, the hunger is gained, and then the
     * throat starts closing - because that is what happens, and because a mod that
     * refused the click would have quietly turned a disease into a tooltip.
     *
     * @return true when it went badly
     */
    private static boolean allergic(ServerPlayer player, ItemStack stack, Config.Values config) {
        BodyHealth body = Body.of(player);
        if (!config.allergies || !body.allergy().eats(stack) || body.has(Disease.REACTION)) {
            return false;
        }
        if (!body.catchDisease(Disease.REACTION)) {
            return false;
        }
        body.setDirty(true);
        Body.notify(player, Component.translatable("triage.msg.allergy_ate",
                        stack.getHoverName()),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_HURT);
        Reactions.say(player, "allergy");
        Body.sync(player);
        return true;
    }

    @SubscribeEvent
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !Body.tracks(player)) {
            return;
        }
        Config.Values config = Config.values();
        // Before the poisoning, and it does not need the illnesses to be on: this is not
        // something caught from bad food, it is this body meeting the one thing in the
        // world that it cannot have.
        if (allergic(player, event.getItem(), config)) {
            return;
        }
        if (!config.diseases || config.foodPoisonChance <= 0.0F) {
            return;
        }
        if (!suspect(event.getItem()) || player.getRandom().nextFloat() >= config.foodPoisonChance) {
            return;
        }
        Ailments.poison(player, Body.of(player));
        Body.sync(player);
    }

    /** Food that carries an effect nobody would choose to have. */
    private static boolean suspect(ItemStack stack) {
        FoodProperties food = stack.getItem().getFoodProperties();
        if (food == null) {
            return false;
        }
        for (Pair<MobEffectInstance, Float> possible : food.getEffects()) {
            if (possible.getFirst() != null && !possible.getFirst().getEffect().isBeneficial()) {
                return true;
            }
        }
        return false;
    }
}
