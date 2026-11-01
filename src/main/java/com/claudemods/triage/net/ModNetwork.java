package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.client.ClientBody;
import com.claudemods.triage.client.ClientTags;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import io.netty.buffer.Unpooled;

/**
 * The channel, on Fabric 1.20.1.
 *
 * <p>The 1.21 line registers a payload type and a codec and is done. Here, as on Forge
 * 1.20.1, every message needs a name, an encoder, a decoder and a handler by hand - the
 * difference being that the name is a ResourceLocation rather than an index, which is one
 * thing less to get wrong: a message added in the middle renumbers nothing.
 *
 * <p>The thirteen messages themselves are the Forge tree's, untouched. They write themselves
 * into a buffer and read themselves back out; nothing in them knows what a channel is.
 */
public final class ModNetwork {
    private ModNetwork() {
    }

    /** Both ends: the server learns what it may receive, the client what it may send. */
    public static void register() {
        listen(TreatmentDonePayload.class, TreatmentDonePayload::decode);
        listen(GiveUpPayload.class, GiveUpPayload::decode);
        listen(PutDownPayload.class, PutDownPayload::decode);
        listen(WriteTagPayload.class, WriteTagPayload::decode);
        listen(AidPayload.class, AidPayload::decode);
        listen(AidDonePayload.class, AidDonePayload::decode);
    }

    /**
     * The client's half. Called from the client initialiser only.
     *
     * <p>Every handler is a lambda rather than a method reference, and that is not a style
     * choice - the same one the Forge tree makes, for the same reason. A method reference has
     * to resolve a handle to ClientBody the moment the line runs, which loads a Screen,
     * which a dedicated server may not do. A lambda body links to a synthetic method here
     * instead, so nothing client-side is touched until a packet actually arrives.
     */
    public static void listen() {
        receive(SyncBodyPayload.class, SyncBodyPayload::decode, message -> ClientBody.accept(message));
        receive(OpenTreatmentPayload.class, OpenTreatmentPayload::decode,
                message -> ClientBody.openTreatment(message));
        receive(CloseTreatmentPayload.class, CloseTreatmentPayload::decode,
                message -> ClientBody.interruptTreatment());
        receive(UpdatePatientPayload.class, UpdatePatientPayload::decode,
                message -> ClientBody.updatePatient(message));
        receive(ShowBodyPayload.class, ShowBodyPayload::decode, message -> ClientBody.showBody(message));
        receive(ShowFilmPayload.class, ShowFilmPayload::decode, message -> ClientBody.showFilm(message));
        receive(SyncTagPayload.class, SyncTagPayload::decode, message -> ClientTags.accept(message));
        receive(OpenAidPayload.class, OpenAidPayload::decode, message -> ClientBody.openAid(message));
        receive(ConfigPayload.class, ConfigPayload::decode, message -> message.apply());
    }

    // ---- sending ----------------------------------------------------------

    public static void toPlayer(ServerPlayer player, Object message) {
        ResourceLocation channel = name(message.getClass());
        if (ServerPlayNetworking.canSend(player, channel)) {
            ServerPlayNetworking.send(player, channel, write(message));
        }
    }

    public static void toServer(Object message) {
        Client.send(name(message.getClass()), write(message));
    }

    /**
     * Everybody who can see this player, and the player themselves.
     *
     * <p>Fabric's tracking list leaves a player out of their own, which is right for what it
     * is for and wrong here: what is being sent is what somebody looks like, and they are
     * the one person certain to be looking.
     */
    public static void trackingAndSelf(ServerPlayer player, Object message) {
        for (ServerPlayer watcher : PlayerLookup.tracking((Entity) player)) {
            toPlayer(watcher, message);
        }
        toPlayer(player, message);
    }

    // ---- the plumbing -----------------------------------------------------

    /** One channel per message class, named after it: triage:sync_body_payload and so on. */
    private static ResourceLocation name(Class<?> type) {
        StringBuilder path = new StringBuilder();
        for (char c : type.getSimpleName().toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (path.length() > 0) {
                    path.append('_');
                }
                path.append(Character.toLowerCase(c));
            } else {
                path.append(c);
            }
        }
        return TriageMod.id(path.toString());
    }

    private static FriendlyByteBuf write(Object message) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            message.getClass().getMethod("encode", FriendlyByteBuf.class).invoke(message, buf);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no encoder on " + message.getClass(), e);
        }
        return buf;
    }

    /**
     * A message the server receives.
     *
     * <p>Reflection for the handler rather than an interface, the same call the Forge tree
     * makes: giving all thirteen records a shared supertype would be a change to the shared
     * half of the mod for the sake of the unshared half, and the shared half is the part
     * that must not know which platform it is on.
     */
    private static <T> void listen(Class<T> type, Function<FriendlyByteBuf, T> decode) {
        ServerPlayNetworking.registerGlobalReceiver(name(type), (server, player, handler, buf, sender) -> {
            T message = decode.apply(buf);
            NetworkEvent.Context context = new NetworkEvent.Context(player);
            try {
                type.getMethod("handle", java.util.function.Supplier.class)
                        .invoke(message, (java.util.function.Supplier<NetworkEvent.Context>) () -> context);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("no handler on " + type, e);
            }
        });
    }

    private static <T> void receive(Class<T> type, Function<FriendlyByteBuf, T> decode, Consumer<T> onClient) {
        Client.receive(name(type), decode, onClient);
    }

    /**
     * The two lines that name a client-only class, behind a door the server never opens.
     *
     * <p>A dedicated server loads this class the first time it sends anything. Keeping the
     * client calls in a nested class means the client half is not loaded with it, because
     * Java does not load a class until something actually touches it.
     */
    private static final class Client {
        private Client() {
        }

        static void send(ResourceLocation channel, FriendlyByteBuf buf) {
            ClientPlayNetworking.send(channel, buf);
        }

        static <T> void receive(ResourceLocation channel, Function<FriendlyByteBuf, T> decode,
                Consumer<T> onClient) {
            ClientPlayNetworking.registerGlobalReceiver(channel, (client, handler, buf, sender) -> {
                T message = decode.apply(buf);
                client.execute(() -> onClient.accept(message));
            });
        }
    }
}
