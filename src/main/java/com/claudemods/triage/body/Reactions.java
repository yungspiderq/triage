package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * What somebody says when it happens to them.
 *
 * <p>Everything in this mod has been told to the player in the voice of a chart. A bone
 * breaks and a line appears saying which bone; an illness arrives and a line appears naming
 * it. That is the right voice for a readout and it is the wrong voice for the moment itself,
 * because the moment itself is not information - <b>it is a person making a noise.</b>
 *
 * <p>So the worst things now come with something said out loud, and the people standing
 * near hear it. That is the whole of the idea and it does two things at once. It puts a
 * voice on a mod that has been entirely silent about the experience of being hurt, and it
 * carries information the mod has always refused to hand out for free: somebody across the
 * clearing shouting that their leg has gone is a medic's first warning, and it costs nobody
 * a chart, a walk or a screen.
 *
 * <p><b>Nothing here is a mechanic.</b> No line does anything. They are said and they fade,
 * and the reason to have them is that a mod about bodies with nothing to say about pain was
 * describing bodies rather than people.
 *
 * <p>Variations rather than one line apiece, because the second time you hear the same six
 * words the mod stops having a voice and starts having a string. They are picked at random
 * and the same line will not come back twice running for the same person.
 */
public final class Reactions {
    /** How far a shout carries to somebody who can do something about it. */
    private static final double EARSHOT = 14.0;
    /** How many variations each event has in the language files. */
    private static final Map<String, Integer> LINES = Map.ofEntries(
            Map.entry("break", 5),
            // Hands on a part that is already broken. Four, and none of them is a scream:
            // this is somebody being examined rather than somebody being hurt, and the
            // difference between those two sentences is the whole reason to be careful.
            Map.entry("feel", 4),
            // The jaw, and the leg coming back. Both of them are the patient noticing
            // something the medic has not yet, which is the only reason either line exists.
            Map.entry("spasm", 4),
            Map.entry("crush", 3),
            // Somebody's sword is in them and they can see it. There is not much to say
            // about that, and what there is, is short.
            Map.entry("blade", 4),
            // Pulling away is not a decision. Neither are these.
            Map.entry("panic", 4),
            Map.entry("burn", 4),
            Map.entry("ill", 4),
            Map.entry("lost", 4),
            Map.entry("allergy", 3),
            Map.entry("bleed", 4),
            Map.entry("shot", 4),
            // An old break, talking about the weather. The only one of these that is not
            // about something happening: it is about something that happened, once, and
            // has not entirely finished.
            Map.entry("ache", 4),
            // Coming round on the floor, having missed the last seven seconds.
            Map.entry("faint", 4),
            // And the one nobody says calmly.
            Map.entry("artery", 4),
            // Hands on a joint that does not want to go there.
            Map.entry("stretch", 3),
            // And the loudest thing in the mod, which is a bone going back where it came
            // from through a limb that can still feel it.
            Map.entry("set", 4),
            // A leg that has started aching for no reason anybody can see.
            Map.entry("clot", 3),
            // And the moment it stops being a leg problem.
            Map.entry("embolism", 3));

    /** What each mouth said last, so it does not say it twice running. */
    private static final Map<UUID, String> LAST = new HashMap<>();

    private Reactions() {
    }

    /**
     * Says one, to them and to whoever is close enough to hear.
     *
     * <p>Chat rather than the action bar, and deliberately: the action bar is where the
     * mod puts things that are meant to fade, and this is the one kind of line that other
     * people are supposed to read. It is also the only thing in the mod a bystander gets
     * without walking over, which is exactly what a shout is for.
     */
    public static void say(ServerPlayer player, String event) {
        Config.Values config = Config.values();
        if (!config.reactions) {
            return;
        }
        Integer count = LINES.get(event);
        if (count == null || player.level().isClientSide) {
            return;
        }
        String key = pick(player, event, count);
        Component line = Component.translatable("triage.react." + key)
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
        Component spoken = Component.translatable("triage.react.said", player.getName(), line);
        player.sendSystemMessage(spoken);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double range = EARSHOT * EARSHOT;
        for (ServerPlayer other : level.players()) {
            if (other != player && !other.isSpectator() && other.distanceToSqr(player) <= range) {
                other.sendSystemMessage(spoken);
            }
        }
    }

    /**
     * One of the variations, and not the one just used.
     *
     * <p>A single retry rather than a loop: with four lines the odds of the same one
     * landing twice are low enough that a second draw settles it, and a loop over a map
     * this small is a way of writing more code to solve less.
     */
    private static String pick(ServerPlayer player, String event, int count) {
        String key = event + "." + (1 + player.getRandom().nextInt(count));
        String last = LAST.get(player.getUUID());
        if (key.equals(last)) {
            key = event + "." + (1 + player.getRandom().nextInt(count));
        }
        LAST.put(player.getUUID(), key);
        return key;
    }

    /** Nobody's last line is worth remembering once they have gone. */
    public static void forget(ServerPlayer player) {
        LAST.remove(player.getUUID());
    }
}
