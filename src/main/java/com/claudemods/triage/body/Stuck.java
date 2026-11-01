package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

/**
 * A weapon that stayed in.
 *
 * <p>The mod used to say, in a comment, that only arrows lodge and that "a sword does not
 * stay in anybody". That is true of almost every swing and false of the one in a hundred
 * that matters - and the one that matters is worth more than the ninety-nine, because
 * three things happen at once that nothing else here does.
 *
 * <p><b>The thing in the wound is holding the wound shut.</b> This is the only injury in
 * the mod where the object is doing something useful: it is in the hole, and the hole is
 * not bleeding because it is in it. Which makes taking it out the injury, and makes the
 * order of operations the whole of the skill - dressings ready, hands ready, then pull.
 * It is also the one piece of real first aid everybody has heard and nobody believes:
 * <em>do not pull it out.</em>
 *
 * <p><b>Whoever put it there does not have it any more.</b> The only disarm in the mod,
 * and it happens to the winner of the exchange. A zombie with an iron sword can be taken
 * apart by one unlucky swing; so can a player.
 *
 * <p><b>And it is visible from across a field</b>, on somebody who is still walking
 * around. Every other finding this mod hands out for free is a colour or a posture. This
 * one is an object.
 */
public final class Stuck {
    private Stuck() {
    }

    /**
     * The item, written down.
     *
     * <p>These two methods are the whole platform seam: a stack is saved and read back
     * differently on each line and identically everywhere else, so everything above them
     * is written once. See PORTING.md.
     */
    public static CompoundTag pack(ItemStack stack, RegistryAccess registries) {
        return stack.save(new CompoundTag());
    }

    public static ItemStack unpack(CompoundTag tag, RegistryAccess registries) {
        if (tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(tag);
    }

    /** What it is called, for a chart that has to name it without holding it. */
    public static Component title(CompoundTag tag, RegistryAccess registries) {
        ItemStack stack = unpack(tag, registries);
        return stack.isEmpty() ? Component.translatable("triage.msg.blade_unknown")
                : stack.getHoverName();
    }

    /**
     * Whether this is the sort of thing that can stay in somebody.
     *
     * <p>Anything with a tier - a sword, an axe, a pick, and every modded equivalent that
     * was built the ordinary way - and a trident. Not a fist, not a stick, not a shovel
     * full of gravel. The test is deliberately about the item rather than about a list in
     * the config, so a mod that adds a sword gets this without anybody writing it down.
     */
    private static boolean bites(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof TieredItem || stack.getItem() instanceof TridentItem);
    }

    /**
     * The swing that did not come back.
     *
     * <p>Only on a blow heavy enough to have gone in deep, only from a hand rather than
     * from a bowstring - an arrow that lodges is already its own thing - and only into a
     * part that has nothing in it yet. Everything else about the blow has already
     * happened: this does not add damage. It takes somebody's weapon and puts it
     * somewhere inconvenient for both of them.
     */
    public static void caught(ServerPlayer player, BodyHealth body, BodyPart part,
                              DamageSource source, float amount) {
        Config.Values config = Config.values();
        if (!config.weaponsStick || amount < config.stickDamage) {
            return;
        }
        // A hand, not a bowstring: the direct entity and the attacker have to be the same
        // thing, which is only true when somebody hit somebody.
        if (!(source.getDirectEntity() instanceof LivingEntity attacker)
                || source.getEntity() != attacker) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        PartState state = body.part(part);
        if (!bites(weapon) || state.missing || state.bladeIn()
                || player.getRandom().nextFloat() >= config.stickChance) {
            return;
        }
        ItemStack lodged = weapon.copy();
        lodged.setCount(1);
        state.blade = pack(lodged, player.server.registryAccess());
        weapon.shrink(1);
        // And the part everybody has heard and nobody believes: it is in the hole, so the
        // hole is not bleeding. That is not a bonus - it is the entire shape of this
        // injury, and it is why taking it out is the dangerous half. A part that was
        // already running stops, which reads exactly as wrong as it should.
        boolean wasBleeding = state.bleeding();
        state.stopBleeding();
        body.setDirty(true);

        Body.notify(player, Component.translatable("triage.msg.blade_in",
                        lodged.getHoverName(), part.title()),
                ChatFormatting.DARK_RED, SoundEvents.PLAYER_ATTACK_CRIT);
        if (wasBleeding) {
            Body.notify(player, Component.translatable("triage.msg.blade_plug", part.title()),
                    ChatFormatting.GRAY, null);
        }
        if (attacker instanceof ServerPlayer owner) {
            Body.notify(owner, Component.translatable("triage.msg.blade_lost",
                            lodged.getHoverName(), player.getName()),
                    ChatFormatting.GOLD, null);
        }
        Reactions.say(player, "blade");
    }

    /**
     * Walking about with it in.
     *
     * <p>A blade in a leg is not a state, it is a thing being moved by a leg, and this is
     * the cost of every step taken before somebody deals with it. Small, and it never
     * stops, and sprinting on it is four times the price - which is the mod saying "sit
     * down" in the only language a player actually hears.
     */
    public static void jostle(ServerPlayer player, BodyHealth body, Config.Values config) {
        if (!config.weaponsStick || config.stickJostle <= 0.0F) {
            return;
        }
        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.004
                || player.walkDist - player.walkDistO > 0.01F;
        if (!moving) {
            return;
        }
        float cost = config.stickJostle * (player.isSprinting() ? 4.0F : 1.0F);
        for (BodyPart part : BodyPart.VALUES) {
            PartState state = body.part(part);
            if (!state.bladeIn()) {
                continue;
            }
            Injuries.apply(player, body, part, cost, false);
            // And sometimes the thing moves enough to let the wound out round it. Which
            // is the whole argument for getting it seen to rather than carrying it home.
            if (player.getRandom().nextFloat() < config.stickJostleBleed
                    && state.bleed < PartState.BLEED_LIGHT) {
                state.startBleeding(PartState.BLEED_LIGHT, config.lightBleedSeconds * 20);
                Body.notify(player, Component.translatable("triage.msg.blade_shifted",
                        part.title()), ChatFormatting.RED, null);
            }
        }
    }

    /**
     * It comes out.
     *
     * <p>The wound it was holding shut is now a wound. How badly depends on who did it and
     * how: a surgeon with forceps leaves a bleed that can be dressed, and a pair of hands
     * in a hurry leaves the other kind. That difference is the entire decision this injury
     * asks for, and it is asked at the worst possible moment every time.
     *
     * @return the weapon, for whoever is holding it now
     */
    public static ItemStack take(ServerPlayer patient, BodyHealth body, BodyPart part,
                                 int bleed, boolean tear, Config.Values config) {
        PartState state = body.part(part);
        ItemStack blade = unpack(state.blade, patient.server.registryAccess());
        state.blade = new CompoundTag();
        if (blade.isEmpty()) {
            return ItemStack.EMPTY;
        }
        state.startBleeding(bleed, config.lightBleedSeconds * 20);
        if (tear) {
            Injuries.apply(patient, body, part, config.stickTear, false);
        }
        body.setDirty(true);
        Reactions.say(patient, bleed >= PartState.BLEED_ARTERIAL ? "artery" : "blade");
        return blade;
    }

    /** Into the hands of whoever took it out, or onto the floor at their feet. */
    public static void hand(ServerPlayer helper, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!helper.getInventory().add(stack)) {
            helper.drop(stack, false);
        }
    }
}
