package com.claudemods.triage.client;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.TriageTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/**
 * The one place the mod says anything about a body without being asked.
 *
 * <p>Sorting the wounded means deciding who to reach first, and deciding that from
 * across a field is the whole job. The nameplate is already the thing a player's eye
 * goes to at that distance, so the answer goes on the nameplate: the name turns dark
 * red while they are on the floor, and whatever somebody wrote on them hangs off the
 * end of it.
 *
 * <p>Vanilla decides when a name is drawn at all, and that is left alone on purpose -
 * out of range, hidden by F1 or invisible, and the mark is out of range too. This is a
 * label on a person, not a tracker.
 */
@EventBusSubscriber(modid = TriageMod.MODID, value = Dist.CLIENT)
public final class NamePlate {
    private NamePlate() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ClientTags.Mark mark = ClientTags.of(player.getId());
        if (mark == null) {
            return;
        }

        MutableComponent name = event.getContent().copy();
        if (mark.downed()) {
            name.withStyle(ChatFormatting.DARK_RED);
        }
        TriageTag tag = mark.tag();
        if (tag != null) {
            name.append(Component.literal(" ")).append(tag.mark());
            // A question mark, because that is what an old note is worth. The mark still
            // says what somebody thought; it has stopped saying when.
            if (mark.stale()) {
                name.append(Component.literal("?").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        event.setContent(name);
    }
}
