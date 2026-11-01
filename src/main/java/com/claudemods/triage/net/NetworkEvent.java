package com.claudemods.triage.net;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * What a packet's handler is told about where it came from.
 *
 * <p>The six messages the client sends have a {@code handle(Supplier<Context>)} on them,
 * written against Forge's shape, and between them they ask four questions. Answering those
 * four here costs one file and keeps all six identical to the Forge tree's copies.
 *
 * <p>The queueing one is a no-op rather than a queue: Fabric already hands a packet over on
 * the thread that owns the world, which is the only reason Forge needs to queue at all.
 */
public final class NetworkEvent {
    private NetworkEvent() {
    }

    public static final class Context {
        private final @Nullable ServerPlayer sender;

        public Context(@Nullable ServerPlayer sender) {
            this.sender = sender;
        }

        public @Nullable ServerPlayer getSender() {
            return sender;
        }

        /** Already on the game thread; see above. */
        public void enqueueWork(Runnable work) {
            work.run();
        }

        /** Nothing to answer to here. Kept so the messages need no edit. */
        public void setPacketHandled(boolean handled) {
        }
    }
}
