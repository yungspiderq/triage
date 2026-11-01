package com.claudemods.triage.data;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyHealth;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Where a body is kept.
 *
 * <p>Fabric grew attachments after NeoForge did and they work the same way, which is the
 * single largest reason this is a port rather than a rewrite: a body is one object hung
 * off a player and written out with the player, and neither tree needs a mixin, a
 * capability or a map keyed by UUID to say so.
 */
public final class ModAttachments {
    /**
     * Deliberately not copied on death: dying is what heals you here, and a fresh body is
     * the reward for it. Carrying it over is one line away if a pack ever wants a harsher
     * version.
     */
    public static final AttachmentType<BodyHealth> BODY = AttachmentRegistry.<BodyHealth>builder()
            .initializer(BodyHealth::new)
            .persistent(BodyHealth.CODEC)
            .buildAndRegister(TriageMod.id("body"));

    private ModAttachments() {
    }

    /** Touching the class is what registers it; called once from the initialiser. */
    public static void register() {
    }
}
