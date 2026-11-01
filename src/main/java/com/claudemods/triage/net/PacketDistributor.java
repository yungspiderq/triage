package com.claudemods.triage.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Sending a packet, under the name the rest of the mod already calls it by.
 *
 * <p>This is a shim and says so. Everything above the loader in this mod is copied from
 * the NeoForge tree unchanged, and about a dozen of those files send a packet; wearing
 * NeoForge's name here means each of them needs one import line rewritten instead of a
 * fork that has to be fixed twice forever. The three methods are the three the mod uses.
 */
public final class PacketDistributor {
    private PacketDistributor() {
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    /**
     * Everybody who can see this entity, and the entity itself when it is a player.
     *
     * <p>Fabric's tracking list leaves a player out of their own, which is right for what
     * it is for and wrong here: the thing being sent is usually what somebody looks like,
     * and they are the one person certain to be looking.
     */
    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        for (ServerPlayer watcher : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(watcher, payload);
        }
        if (entity instanceof ServerPlayer self) {
            ServerPlayNetworking.send(self, payload);
        }
    }

    /** Client to server. Only ever called from a screen, which only exists on a client. */
    public static void sendToServer(CustomPacketPayload payload) {
        Client.send(payload);
    }

    /**
     * The one line that names a client-only class, behind a door the server never opens.
     *
     * <p>A dedicated server loads this class the first time it sends anything to anybody.
     * Keeping the client call in a nested class means the client half is not loaded with
     * it, because Java does not load a class until something actually touches it.
     */
    private static final class Client {
        private Client() {
        }

        static void send(CustomPacketPayload payload) {
            ClientPlayNetworking.send(payload);
        }
    }
}
