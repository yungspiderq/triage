package com.claudemods.triage.event;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.Config;
import com.claudemods.triage.body.Aid;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.BodyHealth;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.DamageRouting;
import com.claudemods.triage.body.Disease;
import com.claudemods.triage.body.Downed;
import com.claudemods.triage.body.Injuries;
import com.claudemods.triage.body.ModDamageTypes;
import com.claudemods.triage.med.Treatments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingHealEvent;

/**
 * Where the mod takes over from the vanilla health bar.
 *
 * <p>Damage arrives here already reduced by armour, resistance and enchantments, which
 * is deliberate: all of that is supposed to keep working. What changes is where the
 * damage goes afterwards. The vanilla bar is then set to mirror what is left of the
 * body, so every other mod that reads a player's health still sees something sensible.
 */
@Mod.EventBusSubscriber(modid = TriageMod.MODID)
public final class DamageEvents {
    private DamageEvents() {
    }

    @SubscribeEvent
    public static void onDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !Body.tracks(player)) {
            return;
        }
        DamageSource source = event.getSource();

        // Our own finishing blow, or something nothing may stand in the way of.
        if (ModDamageTypes.isInternal(source)) {
            return;
        }
        DamageRouting.Kind kind = DamageRouting.classify(source);
        if (kind == DamageRouting.Kind.BYPASS) {
            return;
        }

        float incoming = event.getAmount();
        if (incoming <= 0.0F) {
            return;
        }

        // A blow lands on the player whether or not they were in the middle of
        // bandaging themselves; this is the only place that can tell it apart from
        // their own bleeding.
        Treatments.onDamaged(player, incoming);
        Aid.onDamaged(player, incoming);

        BodyHealth body = Body.of(player);

        // Whatever the blow turns out to be worth, the teeth that made it are the
        // teeth that made it. This has to happen where the source is, and the source
        // does not reach as far as the wound.
        Injuries.bite(player, body, source);

        // Somebody already on the floor cannot be hurt any further in the ordinary
        // sense - their chest is empty. What a blow does instead is take time off
        // the clock, and enough of them end it here and now, with the death message
        // naming whoever was standing over them.
        if (body.downed()) {
            if (Downed.hasten(player, body, incoming)) {
                event.setAmount(player.getHealth() * 2.0F + 1000.0F);
            } else {
                event.setAmount(0.0F);
            }
            Body.sync(player);
            return;
        }

        float amount = incoming * Config.values().damageScale;

        // Vanilla will not get the chance to spend absorption, since the damage it is
        // handed ends up at zero, so it is spent here instead.
        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0F) {
            float eaten = Math.min(absorption, amount);
            player.setAbsorptionAmount(absorption - eaten);
            amount -= eaten;
        }

        if (amount > 0.0F) {
            switch (kind) {
                case FALL -> Injuries.fall(player, body, amount, incoming);
                case SYSTEMIC -> Injuries.systemic(player, body, amount, source.is(DamageTypeTags.IS_FIRE));
                default -> Injuries.blow(player, body, amount, source);
            }
        }

        if (body.isDead() && !Downed.claim(player, body)) {
            // Let vanilla do the killing, with the original source, so the death message
            // names whatever actually did it.
            event.setAmount(player.getHealth() * 2.0F + 1000.0F);
        } else {
            event.setAmount(0.0F);
            Body.mirror(player);
        }
        Body.sync(player);
    }

    /**
     * Healing is spread over the parts that can still use it. Blacked-out parts refuse
     * it - those need surgery, and that refusal is what gives medicine a job.
     */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !Body.tracks(player)) {
            return;
        }
        BodyHealth body = Body.of(player);
        Config.Values config = Config.values();

        // A world that has turned regeneration off means it. Everything that calls
        // vanilla's heal is refused here; bandages and kits do not come through this
        // path at all, so they keep working.
        if (!config.regeneration) {
            event.setCanceled(true);
            return;
        }

        float amount = event.getAmount() * config.healScale;
        // A wrecked chest is a body that cannot look after itself.
        float penalty = 1.0F - config.regenPenalty * (1.0F - body.part(BodyPart.THORAX).ratio());
        amount *= Mth.clamp(penalty, 0.0F, 1.0F);

        // An ill body has other things to spend itself on than mending.
        if (body.has(Disease.SEPSIS)) {
            amount = 0.0F;
        } else if (body.has(Disease.FEVER)) {
            amount *= 0.3F;
        } else if (body.has(Disease.POISONING)) {
            amount *= 0.4F;
        }

        body.healAll(amount);

        // The bar follows the body, never the other way round.
        event.setCanceled(true);
        Body.mirror(player);
        Body.flush(player);
    }
}
