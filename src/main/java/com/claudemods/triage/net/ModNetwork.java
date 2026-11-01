package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.client.ClientBody;
import com.claudemods.triage.client.ClientTags;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The channel, on 1.20.1.
 *
 * <p>The other line registers a payload type and a codec and is done. Here there is one
 * channel with a version string on it, and every message is given an index, an encoder,
 * a decoder and a handler by hand. It is more typing and it is the same thirteen
 * messages, in the same order, doing the same things.
 *
 * <p>The order matters and is the one thing in this file that can silently break a
 * server: the index is what goes on the wire, so inserting a message in the middle
 * renumbers everything after it and a client of the old build will read one packet as
 * another. New ones go on the end.
 */
public final class ModNetwork {
    private static final String VERSION = "1";
    private static int id;

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(TriageMod.id("main"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();

    private ModNetwork() {
    }

    public static void register() {
        // To the client, and every one of these handlers is written out as a lambda on
        // purpose. A method reference - ClientBody::accept - reads better and is wrong:
        // its invokedynamic has to resolve a handle to that method the moment this line
        // runs, which loads ClientBody, which loads Screen, which a dedicated server is
        // forbidden to load. The mod then fails to construct at all and the server dies
        // on startup. A lambda body links to a synthetic method in this class instead,
        // so ClientBody is not touched until something actually calls it, which only
        // ever happens on a client.
        //
        // The difference is invisible in single player, because there the client classes
        // are present and nothing complains. It only shows up on a real server.
        toClient(SyncBodyPayload.class, SyncBodyPayload::decode,
                message -> ClientBody.accept(message));
        toClient(OpenTreatmentPayload.class, OpenTreatmentPayload::decode,
                message -> ClientBody.openTreatment(message));
        toClient(CloseTreatmentPayload.class, CloseTreatmentPayload::decode,
                message -> ClientBody.interruptTreatment());
        toClient(UpdatePatientPayload.class, UpdatePatientPayload::decode,
                message -> ClientBody.updatePatient(message));
        toClient(ShowBodyPayload.class, ShowBodyPayload::decode,
                message -> ClientBody.showBody(message));
        // New on the end, because the index order here is the wire format.
        toClient(ShowFilmPayload.class, ShowFilmPayload::decode,
                message -> ClientBody.showFilm(message));
        toClient(SyncTagPayload.class, SyncTagPayload::decode,
                message -> ClientTags.accept(message));
        toClient(OpenAidPayload.class, OpenAidPayload::decode,
                message -> ClientBody.openAid(message));

        // And to the server, where the handler is on the message itself because it needs
        // the sender and the sender is in the context.
        toServer(TreatmentDonePayload.class, TreatmentDonePayload::decode);
        toServer(GiveUpPayload.class, GiveUpPayload::decode);
        toServer(PutDownPayload.class, PutDownPayload::decode);
        toServer(WriteTagPayload.class, WriteTagPayload::decode);
        toServer(AidPayload.class, AidPayload::decode);
        toServer(AidDonePayload.class, AidDonePayload::decode);
    }

    /**
     * One message the server sends.
     *
     * <p>The work is queued rather than run where it arrives, because a packet lands on
     * the network thread and everything it touches belongs to the game one.
     */
    private static <T> void toClient(Class<T> type, Function<FriendlyByteBuf, T> decode,
                                     java.util.function.Consumer<T> onClient) {
        CHANNEL.messageBuilder(type, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buf) -> encode(message, buf))
                .decoder(decode)
                .consumerMainThread((message, ctx) -> {
                    NetworkEvent.Context context = ctx.get();
                    context.enqueueWork(() -> onClient.accept(message));
                    context.setPacketHandled(true);
                })
                .add();
    }

    private static <T> void toServer(Class<T> type, Function<FriendlyByteBuf, T> decode) {
        CHANNEL.messageBuilder(type, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> encode(message, buf))
                .decoder(decode)
                .consumerMainThread((message, ctx) -> handle(message, ctx))
                .add();
    }

    /**
     * Every message writes itself; this only works out which one is asking.
     *
     * <p>Reflection rather than an interface, because giving all thirteen records a
     * shared supertype is a change to the shared half of the mod for the sake of the
     * unshared half, and the shared half is the part that must not know which platform
     * it is on.
     */
    private static void encode(Object message, FriendlyByteBuf buf) {
        try {
            message.getClass().getMethod("encode", FriendlyByteBuf.class).invoke(message, buf);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no encoder on " + message.getClass(), e);
        }
    }

    private static void handle(Object message, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        try {
            message.getClass().getMethod("handle", java.util.function.Supplier.class)
                    .invoke(message, ctx);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no handler on " + message.getClass(), e);
        }
    }

    // ---- sending ----------------------------------------------------------

    public static void toPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void toServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void trackingAndSelf(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message);
    }
}
