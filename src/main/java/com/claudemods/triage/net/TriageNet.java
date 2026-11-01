package com.claudemods.triage.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Every packet the mod has, declared once on both ends.
 *
 * <p>The records themselves are carried from the NeoForge tree untouched, because
 * CustomPacketPayload belongs to Minecraft rather than to a loader - the wire format is
 * the same mod on both. Only this file, which says which direction each one travels and
 * who answers it, is written twice.
 *
 * <p>Order matters: both sides register the same types, and a client that knows one packet
 * the server does not is a client that gets disconnected on the first one sent.
 */
public final class TriageNet {
    private TriageNet() {
    }

    /** Called from both initialisers - the types have to exist before either end talks. */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(SyncBodyPayload.TYPE, SyncBodyPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenTreatmentPayload.TYPE, OpenTreatmentPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CloseTreatmentPayload.TYPE, CloseTreatmentPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(UpdatePatientPayload.TYPE, UpdatePatientPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ShowBodyPayload.TYPE, ShowBodyPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ShowFilmPayload.TYPE, ShowFilmPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncTagPayload.TYPE, SyncTagPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenAidPayload.TYPE, OpenAidPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigPayload.TYPE, ConfigPayload.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(TreatmentDonePayload.TYPE, TreatmentDonePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(GiveUpPayload.TYPE, GiveUpPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(PutDownPayload.TYPE, PutDownPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(WriteTagPayload.TYPE, WriteTagPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AidPayload.TYPE, AidPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AidDonePayload.TYPE, AidDonePayload.STREAM_CODEC);
    }

    /**
     * The server side of the conversation.
     *
     * <p>Each payload answers for itself, exactly as it does on NeoForge: the handler
     * there is a method on the record, and all this does is hand it the one thing it asks
     * about, which is who sent it.
     */
    public static void listen() {
        ServerPlayNetworking.registerGlobalReceiver(TreatmentDonePayload.TYPE,
                (payload, context) -> payload.handle(context::player));
        ServerPlayNetworking.registerGlobalReceiver(GiveUpPayload.TYPE,
                (payload, context) -> payload.handle(context::player));
        ServerPlayNetworking.registerGlobalReceiver(PutDownPayload.TYPE,
                (payload, context) -> payload.handle(context::player));
        ServerPlayNetworking.registerGlobalReceiver(WriteTagPayload.TYPE,
                (payload, context) -> payload.handle(context::player));
        ServerPlayNetworking.registerGlobalReceiver(AidPayload.TYPE,
                (payload, context) -> payload.handle(context::player));
        ServerPlayNetworking.registerGlobalReceiver(AidDonePayload.TYPE,
                (payload, context) -> payload.handle(context::player));
    }

    /** Only to a client that has the mod; a vanilla client is left alone. */
    public static void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
