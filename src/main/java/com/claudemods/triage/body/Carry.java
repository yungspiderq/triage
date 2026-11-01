package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.claudemods.triage.compat.Ragdolls;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Picking a downed player up and taking them somewhere else.
 *
 * <p>Until this existed, the only surgery available was surgery where they fell: in the
 * fire, in the open, in front of whatever put them there. A medic could reach a body and
 * still have nowhere to work. Carrying does not save anybody by itself - it costs the
 * carrier their weapon and most of their speed, and the clock keeps running - it just
 * makes "get them out first" a move that exists.
 *
 * <p>It is ordinary vanilla riding underneath. That buys correct movement, collision and
 * chunk loading for free, and it means every client already knows how to draw it.
 */
public final class Carry {
    private Carry() {
    }

    /**
     * Tells a carrier's own client that somebody is riding them.
     *
     * <p>Vanilla never does. {@code ServerEntity.sendChanges} announces a passenger list
     * with {@code broadcast}, which reaches everyone <em>tracking</em> the vehicle - and
     * a player does not track themselves. The class has a {@code broadcastAndSend} that
     * would also reach the entity itself, and uses it for entity data and for motion,
     * but deliberately not for passengers: in vanilla a player is never a vehicle.
     *
     * <p>So the carrier's client never learned it had a passenger, never ran the position
     * maths for one, and left the body standing where it was picked up - while the
     * carried player, who does track the carrier, saw themselves riding along. Each of
     * them was watching a different truth.
     */
    private static void tellCarrier(@Nullable Entity vehicle) {
        if (vehicle instanceof ServerPlayer carrier) {
            carrier.connection.send(new ClientboundSetPassengersPacket(carrier));
        }
    }

    /** Whoever this player has over their shoulder, or null. */
    public static @Nullable Player carrying(Player carrier) {
        for (Entity passenger : carrier.getPassengers()) {
            if (passenger instanceof Player carried) {
                return carried;
            }
        }
        return null;
    }

    /** Whether this player is the one being carried. */
    public static boolean carried(Player player) {
        return player.getVehicle() instanceof Player;
    }

    /**
     * Lifts a downed player.
     *
     * <p>A refusal still counts as handled: the player reached for somebody on the floor,
     * and doing whatever the click would otherwise have done is not what they asked for.
     *
     * @return true when the click was a carry and should do nothing else
     */
    public static boolean pickUp(ServerPlayer carrier, ServerPlayer patient) {
        Config.Values config = Config.values();
        // Nobody who is on their feet was ever being reached for, so this is silent: it
        // was not a refused carry, it was not a carry. Everything past this point is a
        // player visibly trying to lift somebody off the floor, and every one of those
        // gets told why it did not work - a click that does nothing at all reads as a
        // broken mod rather than as a rule.
        if (!config.carry || carrier == patient || !Body.of(patient).floored()) {
            return false;
        }
        if (!Body.tracks(carrier)) {
            return refuse(carrier, "triage.msg.untracked_you");
        }
        if (!Body.tracks(patient)) {
            return refuse(carrier, "triage.msg.untracked_them");
        }
        // Nobody carries two, nobody carries while down, and nobody carries their own
        // carrier - all three are the same rule: one pair of hands, one job.
        if (carrying(carrier) != null) {
            return refuse(carrier, "triage.msg.carry_already");
        }
        if (Body.of(carrier).floored() || carried(carrier)) {
            return refuse(carrier, "triage.msg.carry_cannot");
        }
        // Let go of the doll before reading the vehicle. With a physics mod installed a
        // downed player rides their own ragdoll's seat, and a vehicle is how this asks
        // "has somebody else got them" - so without this the answer would be yes, always,
        // and carrying would quietly stop existing the day somebody added ragdolls.
        // Hands beat physics: this is the one thing that is allowed to interrupt it.
        Ragdolls.getUp(patient);
        if (patient.isPassenger()) {
            return refuse(carrier, "triage.msg.carry_taken");
        }
        if (!patient.startRiding(carrier, true)) {
            return false;
        }
        // Off the floor, so stop posing them as if they were on it. Cleared here rather
        // than waiting for the next body tick, or they spend up to a second in a pose
        // half the clients in the game disagree about.
        com.claudemods.triage.compat.Poses.force(patient, null);
        tellCarrier(carrier);
        Body.notify(carrier, Component.translatable("triage.msg.carrying", patient.getName()),
                ChatFormatting.AQUA, SoundEvents.LEASH_KNOT_PLACE);
        Body.notify(patient, Component.translatable("triage.msg.carried_by", carrier.getName()),
                ChatFormatting.AQUA, null);
        return true;
    }

    /**
     * Sets down whoever this player is carrying.
     *
     * @return true when there was somebody to set down
     */
    public static boolean putDown(ServerPlayer carrier) {
        Player carried = carrying(carrier);
        if (carried == null) {
            return false;
        }
        carried.stopRiding();
        tellCarrier(carrier);
        layOut(carrier, carried);
        Body.notify(carrier, Component.translatable("triage.msg.put_down", carried.getName()),
                ChatFormatting.GRAY, null);
        if (carried instanceof ServerPlayer patient) {
            Body.notify(patient, Component.translatable("triage.msg.put_down_by", carrier.getName()),
                    ChatFormatting.GRAY, null);
        }
        return true;
    }

    /**
     * Sets somebody down in front of you rather than inside you.
     *
     * <p>Dismounting on its own drops them at the carrier's own feet, which reads as one
     * body standing in another. A step forward and onto the ground puts them where a
     * person would actually be laid, and gets the pose back on them at once instead of
     * a second later.
     */
    private static void layOut(ServerPlayer carrier, Player carried) {
        Vec3 ahead = carrier.position().add(Vec3.directionFromRotation(0.0F, carrier.getYRot()).scale(1.0));
        BlockPos ground = BlockPos.containing(ahead);
        // Only move them somewhere they actually fit; a wall in front is not a reason to
        // put somebody inside it.
        if (carrier.level().noCollision(carried, carried.getBoundingBox().move(
                ahead.x - carried.getX(), ahead.y - carried.getY(), ahead.z - carried.getZ()))
                && carrier.level().isLoaded(ground)) {
            carried.teleportTo(ahead.x, ahead.y, ahead.z);
        }
        carried.setYRot(carrier.getYRot());
        if (carried instanceof ServerPlayer patient && Body.of(patient).floored() && !Ragdolls.drop(patient)) {
            com.claudemods.triage.compat.Poses.force(patient, Pose.SWIMMING);
        }
    }

    /** Gets this player off whoever is holding them, quietly. Used when the reason ends. */
    public static void release(Player player) {
        if (carried(player)) {
            Entity vehicle = player.getVehicle();
            player.stopRiding();
            tellCarrier(vehicle);
        }
        Player carried = carrying(player);
        if (carried != null) {
            carried.stopRiding();
            tellCarrier(player);
        }
    }

    /**
     * Once a second, for both ends of the arrangement: a carry that no longer makes sense
     * ends by itself rather than lasting until somebody thinks to end it. Standing up is
     * the common one - a revived player should be on their own feet, not on a shoulder.
     */
    public static void tick(ServerPlayer player) {
        if (carried(player) && !Body.of(player).floored()) {
            Entity vehicle = player.getVehicle();
            player.stopRiding();
            tellCarrier(vehicle);
            return;
        }
        Player carried = carrying(player);
        if (carried == null) {
            return;
        }
        boolean stillWorth = Config.values().carry
                && !Body.of(player).floored()
                && !player.isDeadOrDying()
                && carried instanceof ServerPlayer patient
                && Body.of(patient).floored()
                && !patient.isDeadOrDying();
        if (!stillWorth) {
            carried.stopRiding();
            tellCarrier(player);
        }
    }

    private static boolean refuse(ServerPlayer player, String key) {
        Body.notify(player, Component.translatable(key), ChatFormatting.GRAY, null);
        return true;
    }
}
