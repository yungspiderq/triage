package com.claudemods.triage.data;

import com.claudemods.triage.TriageMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The mod's advancements.
 *
 * <p>They exist to teach the thing, not to reward it. A system this large - bleeding
 * into infection into fever, seven kinds of medicine, a screen full of small games -
 * has no way of telling a new player that any of it is there. An advancement tree is
 * the one place Minecraft players already look to find out what a mod expects of them.
 *
 * <p>All of them use the {@code minecraft:impossible} trigger, so the code below is the
 * only way to earn one.
 */
public final class Milestones {
    private static final String CRITERION = "unlocked";

    private Milestones() {
    }

    private static void award(ServerPlayer player, String path) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        AdvancementHolder holder = server.getAdvancements().get(TriageMod.id(path));
        if (holder != null) {
            player.getAdvancements().award(holder, CRITERION);
        }
    }

    /** Opens the tab the moment somebody joins, so the goals are visible from the start. */
    public static void onJoin(ServerPlayer player) {
        award(player, "root");
    }

    public static void onBleedingStarted(ServerPlayer player) {
        award(player, "root");
        award(player, "first_wound");
    }

    public static void onInfected(ServerPlayer player) {
        award(player, "infected");
    }

    public static void onRevived(ServerPlayer healer) {
        award(healer, "saved");
    }

    public static void onSorted(ServerPlayer author) {
        award(author, "sorted");
    }

    public static void onFever(ServerPlayer player) {
        award(player, "fever");
    }

    /**
     * The one nobody is pleased about.
     *
     * <p>Both of them earn it, which is the point: whoever held the saw did the hardest
     * thing the mod asks for, and whoever it was done to is the one who is going to be
     * living with it. An advancement that only went to the surgeon would be telling the
     * wrong half of the story.
     */
    public static void onAmputated(ServerPlayer healer, ServerPlayer patient) {
        award(healer, "sawbones");
        award(healer, "amputation");
        if (patient != healer) {
            award(patient, "amputation");
        }
    }

    public static void onFitted(ServerPlayer healer) {
        award(healer, "prosthetic");
    }

    /**
     * @param onSomebodyElse whether the hands doing the work belonged to another player
     */
    public static void onTreated(ServerPlayer healer, String kindId, boolean onSomebodyElse) {
        switch (kindId) {
            case "bandage" -> award(healer, "field_dressing");
            case "splint" -> award(healer, "bone_setter");
            case "surgery" -> award(healer, "sawbones");
            default -> {
            }
        }
        if (onSomebodyElse) {
            award(healer, "medic");
        }
    }
}
