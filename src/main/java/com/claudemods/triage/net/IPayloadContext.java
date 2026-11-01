package com.claudemods.triage.net;

import net.minecraft.world.entity.player.Player;

/**
 * What a packet's handler is told about where it came from.
 *
 * <p>The second shim, and the same bargain as {@link PacketDistributor}: six payload
 * records carried from the NeoForge tree have a {@code handle(IPayloadContext)} on them,
 * and all six ask it exactly one question. Answering that one question here costs a file
 * and keeps those six byte for byte the same in both trees.
 */
@FunctionalInterface
public interface IPayloadContext {
    /** Whoever sent it. On this side of the wire that is always a ServerPlayer. */
    Player player();
}
