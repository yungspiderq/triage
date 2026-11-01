package com.claudemods.triage.net;

import com.claudemods.triage.Config;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server -> client, once, on joining: the server's own tuning.
 *
 * <p>This message has no counterpart in the other two trees because it does not need one -
 * a server config there is sent to the client by the loader. Fabric has no such idea, and
 * without this every client would be reading its own config/triage-server.toml: two people
 * in the same world would disagree about how much a broken leg costs, and the one drawing the
 * screens would be the one who is wrong.
 *
 * <p>Sent whole rather than as a diff. Three hundred numbers is a few kilobytes once per
 * join, which is nothing against getting it wrong.
 */
public record ConfigPayload(Config.Values values) {
    public void encode(FriendlyByteBuf buf) {
        Config.encode(buf, values());
    }

    public static ConfigPayload decode(FriendlyByteBuf buf) {
        return new ConfigPayload(Config.decode(buf));
    }

    /** On the client: the server's numbers in place of this machine's own. */
    public void apply() {
        Config.accept(values());
    }
}
