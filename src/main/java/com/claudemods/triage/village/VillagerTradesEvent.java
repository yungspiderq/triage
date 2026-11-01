package com.claudemods.triage.village;

import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

/**
 * The trade list being built, in the shape the list itself is written against.
 *
 * <p>One more of the wrappers described in {@link com.claudemods.triage.event.Event}, and
 * the smallest of them: what the medic sells is thirty lines of prices and the reasons for
 * them, and those reasons are the same on every loader. NeoForge hands a mod a map of
 * lists to add to, so that is what this is; {@link ModVillagers} empties it into Fabric's
 * registry afterwards.
 */
public final class VillagerTradesEvent {
    private final VillagerProfession type;
    private final Map<Integer, List<VillagerTrades.ItemListing>> trades;

    public VillagerTradesEvent(VillagerProfession type, Map<Integer, List<VillagerTrades.ItemListing>> trades) {
        this.type = type;
        this.trades = trades;
    }

    public VillagerProfession getType() {
        return type;
    }

    /** Keyed by the five villager levels, novice through master. */
    public Map<Integer, List<VillagerTrades.ItemListing>> getTrades() {
        return trades;
    }
}
