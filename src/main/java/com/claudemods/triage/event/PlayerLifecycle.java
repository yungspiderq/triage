package com.claudemods.triage.event;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.Allergy;
import com.claudemods.triage.body.BloodType;
import com.claudemods.triage.Config;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.body.Downed;
import com.claudemods.triage.body.Faint;
import com.claudemods.triage.body.PartEffects;
import com.claudemods.triage.body.Bedside;
import com.claudemods.triage.body.Scars;
import com.claudemods.triage.body.Tags;
import com.claudemods.triage.compat.Ragdolls;
import com.claudemods.triage.data.Milestones;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * The client has no idea what state a body is in until it is told, and attribute
 * modifiers are transient, so both have to be rebuilt every time a player entity is.
 *
 * <p>Dying is not handled here on purpose: the attachment is not marked
 * {@code copyOnDeath}, so a respawned player gets a fresh body by default, while a
 * dimension change copies the old one over. That is exactly the wanted behaviour, and
 * it needs no code.
 */
@Mod.EventBusSubscriber(modid = TriageMod.MODID)
public final class PlayerLifecycle {
    private PlayerLifecycle() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // The config may have changed since this player last played.
            Body.of(player).reconcile();
            refresh(player);
            Milestones.onJoin(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BodyHealth body = Body.of(player);
        if (!event.isEndConquered()) {
            // Death is the one thing that puts a body back together - which now means
            // more than it used to. Everything a body was carrying permanently was
            // carried by a body that no longer exists.
            body.resetAll();
        }
        body.reconcile();
        PartEffects.clear(player);
        Downed.release(player);
        Faint.release(player, body);
        refresh(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    /**
     * A mark is worn rather than held, so a client that has just started seeing somebody
     * has to be told what they are wearing. Nothing else about their body travels: the
     * chart still costs a walk over and a look.
     */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer seen) {
            Tags.sendTo(viewer, seen);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Tags.forget(event.getEntity().getUUID());
        Ragdolls.forget(event.getEntity().getUUID());
        Scars.forget(event.getEntity().getUUID());
        Bedside.forget(event.getEntity().getUUID());
    }

    private static void refresh(ServerPlayer player) {
        BodyHealth body = Body.of(player);
        rollTraits(player, body);
        PartEffects.apply(player, body);
        Body.mirror(player);
        Body.sync(player);
        Tags.resend(player);
    }

    /**
     * A body with no blood type has never been given one: a player who has just arrived,
     * or one who has just died and had theirs cleared. Rolled here rather than in the
     * constructor because the client builds bodies too, and only the server may decide
     * this.
     *
     * <p>Announced when it happens, because it changes on every death and a player who
     * is not told would have to go and look - and the moment it matters is the moment
     * somebody is holding a bag over them.
     */
    private static void rollTraits(ServerPlayer player, BodyHealth body) {
        if (body.bloodType() != null) {
            return;
        }
        BloodType type = BloodType.random(player.getRandom());
        body.setBloodType(type);
        // Real chat, not the action bar every other message in the mod uses. Those are
        // events - you bandaged something, something started bleeding - and they are
        // meant to fade. This is reference: it is needed minutes later, by somebody
        // else, standing over you with a bag. A line that vanishes in two seconds while
        // the respawn screen is still fading is the wrong place to keep it.
        player.sendSystemMessage(Component.translatable("triage.msg.blood_type", type.title())
                .withStyle(ChatFormatting.GRAY));

        // The other thing that is true about a body rather than done to it, and the only
        // condition in the mod that arrives with the person. Told in chat for the same
        // reason the blood type is: it is not an event, it is a fact you will want to
        // remember later, and the action bar is where things go to be forgotten.
        Config.Values config = Config.values();
        if (config.schizophrenia && player.getRandom().nextFloat() < config.schizophreniaChance
                && body.catchDisease(Disease.SCHIZOPHRENIA)) {
            player.sendSystemMessage(Component.translatable("triage.msg.schizophrenia_rolled")
                    .withStyle(ChatFormatting.GRAY));
        }

        // And the third thing that is true about a body rather than done to it. Told the
        // same way and for the same reason: it is wanted minutes later, by somebody else,
        // holding the exact thing it is about.
        Allergy allergy = Allergy.roll(player.getRandom(), config.allergyChance);
        body.setAllergy(allergy);
        if (allergy.any()) {
            player.sendSystemMessage(Component.translatable("triage.msg.allergy_rolled",
                    allergy.title()).withStyle(ChatFormatting.GRAY));
        }
    }
}
