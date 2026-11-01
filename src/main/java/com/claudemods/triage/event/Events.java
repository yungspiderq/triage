package com.claudemods.triage.event;

import com.claudemods.triage.med.MedicineEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

/**
 * Where Fabric's callbacks meet the handlers, which are the NeoForge tree's handlers.
 *
 * <p>Nothing here decides anything. Each callback builds the wrapper the handlers read
 * (see {@link Event}), offers it to each of them in the order NeoForge would have, and
 * hands back whatever they made of it.
 *
 * <p>Four moments are missing from this file because Fabric has no callback for them -
 * being hit, being healed, finishing a meal, and something choosing its next target. Those
 * arrive through mixins instead, which call the same handlers the same way.
 */
public final class Events {
    private Events() {
    }

    public static void register() {
        // Right click on another player: three handlers want this one gesture, and they
        // are offered it in the order their conditions narrow. Hands full first, then
        // medicine, then the plain look - which is the one with no modifier on it.
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            ItemStack held = player.getItemInHand(hand);
            PlayerInteractEvent.EntityInteract event =
                    new PlayerInteractEvent.EntityInteract(player, hand, held, entity);
            CarryEvents.onInteractWithPlayer(event);
            if (!event.isCanceled()) {
                MedicineEvents.onInteractWithPlayer(event);
            }
            if (!event.isCanceled()) {
                AssessEvents.onInteractWithPlayer(event);
            }
            return event.result();
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            PlayerInteractEvent.RightClickBlock event =
                    new PlayerInteractEvent.RightClickBlock(player, hand, player.getItemInHand(hand));
            MedicineEvents.onRightClickBlock(event);
            if (!event.isCanceled()) {
                CarryEvents.onRightClickBlock(event);
            }
            return event.result();
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack held = player.getItemInHand(hand);
            PlayerInteractEvent.RightClickItem event =
                    new PlayerInteractEvent.RightClickItem(player, hand, held);
            MedicineEvents.onRightClickItem(event);
            return event.isCanceled()
                    ? new InteractionResultHolder<>(event.result(), held)
                    : InteractionResultHolder.pass(held);
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            LivingEvents.AttackEntityEvent event = new LivingEvents.AttackEntityEvent(player);
            CarryEvents.onAttack(event);
            return event.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) ->
                CarryEvents.onDeath(new LivingEvents.LivingDeathEvent(entity)));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PlayerLifecycle.onLogin(new PlayerEvent.PlayerLoggedInEvent(handler.getPlayer())));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerEvent.PlayerLoggedOutEvent event = new PlayerEvent.PlayerLoggedOutEvent(handler.getPlayer());
            CarryEvents.onLogout(event);
            MedicineEvents.onLogout(event);
            PlayerLifecycle.onLogout(event);
        });

        // Fabric's "alive" is NeoForge's "end conquered": both mean the player walked out
        // of somewhere rather than died, and a body is only put back together by dying.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                PlayerLifecycle.onRespawn(new PlayerEvent.PlayerRespawnEvent(newPlayer, alive)));

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, from, to) ->
                PlayerLifecycle.onChangedDimension(new PlayerEvent.PlayerChangedDimensionEvent(player)));

        EntityTrackingEvents.START_TRACKING.register((tracked, viewer) ->
                PlayerLifecycle.onStartTracking(new PlayerEvent.StartTracking(viewer, tracked)));

        // Once a player per tick, which is the shape NeoForge's player tick already had -
        // the handler itself is what decides that only every twentieth one does anything.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                BodyTicker.onTick(player);
            }
        });

        ServerTickEvents.END_WORLD_TICK.register(BodyTicker::onLevelTick);
        ServerWorldEvents.UNLOAD.register((server, level) -> BodyTicker.onLevelUnload(level));
    }
}
