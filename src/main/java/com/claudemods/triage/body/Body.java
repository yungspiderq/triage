package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.claudemods.triage.data.ModAttachments;
import com.claudemods.triage.net.SyncBodyPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side entry points: everything that touches a body goes through here. */
public final class Body {
    /** Enough to get through any armour and any absorption the player has left. */
    private static final float LETHAL = 1000.0F;

    private Body() {
    }

    public static BodyHealth of(Player player) {
        return player.getData(ModAttachments.BODY);
    }

    /** Creative and spectator players are left alone entirely. */
    public static boolean tracks(Player player) {
        return !player.isCreative() && !player.isSpectator();
    }

    public static void sync(ServerPlayer player) {
        BodyHealth body = of(player);
        body.setDirty(false);
        PacketDistributor.sendToPlayer(player, new SyncBodyPayload(body.copy()));
    }

    /** Sends the body if anything moved since the last packet. */
    public static void flush(ServerPlayer player) {
        if (of(player).isDirty()) {
            sync(player);
        }
    }

    /**
     * Points the vanilla health bar at what is left of the body. The bar is a readout
     * here, not the truth - the truth is in the seven parts, and only the HUD shows it.
     */
    public static void mirror(ServerPlayer player) {
        BodyHealth body = of(player);
        if (body.isDead() || player.isDeadOrDying()) {
            return;
        }
        float target = Math.max(1.0F, player.getMaxHealth() * body.mirrorRatio());
        if (Math.abs(player.getHealth() - target) > 0.01F) {
            player.setHealth(target);
        }
    }

    /**
     * Kills the player if a vital part has run out, using one of the mod's own damage
     * types so the death message says what happened.
     *
     * @return true when the blow was dealt
     */
    public static boolean killIfEmpty(ServerPlayer player, ResourceKey<DamageType> cause) {
        if (!of(player).isDead() || player.isDeadOrDying()) {
            return false;
        }
        player.hurt(ModDamageTypes.source(player.level(), cause), LETHAL);
        return true;
    }

    /**
     * Which of the mod's own deaths this is. Blood poisoning outranks the rest:
     * once it has hold of someone, it is what is killing them whatever else is
     * also true.
     */
    public static ResourceKey<DamageType> deathCause(BodyHealth body) {
        if (body.has(Disease.SEPSIS)) {
            return ModDamageTypes.SEPSIS;
        }
        for (BodyPart part : BodyPart.VALUES) {
            if (body.part(part).losingBlood()) {
                return ModDamageTypes.BLEEDING;
            }
        }
        return ModDamageTypes.PAIN;
    }

    /** A short line above the hotbar plus a sound - the mod's only way of speaking up. */
    public static void notify(ServerPlayer player, Component message, ChatFormatting colour, SoundEvent sound) {
        player.sendSystemMessage(message.copy().withStyle(colour), true);
        if (sound != null) {
            player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.7F, 1.0F);
        }
    }

    /**
     * The copy of a body that goes to somebody looking at it.
     *
     * <p>In arcade this is the body, exactly as it always was. In realistic it is the body
     * with everything nobody has established taken out of it - see
     * {@link BodyHealth#asSeen()}.
     *
     * <p>Your own body is never redacted, and that is not an oversight. What the mode takes
     * away is the <em>readout</em>, not what a person feels: the symptoms on the screen, the
     * ringing in the ears and the leg that will not take weight all run off this body, and
     * they are exactly what somebody is supposed to have about themselves. The chart is what
     * changes for the self - it prints what you feel instead of what is true - and that is a
     * decision about drawing, taken in the one place that draws.
     */
    public static BodyHealth seen(BodyHealth body, boolean ofSelf) {
        return ofSelf || !Config.values().realistic ? body.copy() : body.asSeen();
    }
}
