package com.claudemods.triage.village;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.block.ModBlocks;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

/**
 * The medic: a villager who sells what the mod expects you to carry.
 *
 * <p>Crafting was the only source of medicine, which meant a player who had not gone
 * looking for sugar cane and nether wart simply had none. A trader does not replace
 * crafting - the good things are still deep in the trade list - it gives the supply line
 * a second end.
 *
 * <p>The job site is the medical table, and a point of interest is not registered the way
 * other things are: the game keeps a map from every block state to the job it belongs to,
 * and putting one in the registry without filling that in gives a profession no villager
 * can ever take. Fabric has a helper that does both; NeoForge has an event.
 */
public final class ModVillagers {
    public static final PoiType MEDIC_POI = PointOfInterestHelper.register(
            TriageMod.id("medic"), 1, 1, ModBlocks.MEDICAL_TABLE.get());

    public static final VillagerProfession MEDIC = Registry.register(BuiltInRegistries.VILLAGER_PROFESSION,
            TriageMod.id("medic"), new VillagerProfession(
                    "medic",
                    holder -> holder.value() == MEDIC_POI,
                    holder -> holder.value() == MEDIC_POI,
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CLERIC));

    private ModVillagers() {
    }

    public static void register() {
        trades();
        ServerLifecycleEvents.SERVER_STARTING.register(VillageHouses::onServerAboutToStart);
    }

    /**
     * What the medic sells, gathered the way the other tree gathers it and then handed to
     * Fabric a level at a time. The list itself is in {@link MedicTrades}, which is the
     * same file in all three trees.
     */
    private static void trades() {
        Map<Integer, List<VillagerTrades.ItemListing>> levels = new HashMap<>();
        for (int level = 1; level <= 5; level++) {
            levels.put(level, new ArrayList<>());
        }
        MedicTrades.onTrades(new VillagerTradesEvent(MEDIC, levels));
        levels.forEach((level, listings) -> {
            if (!listings.isEmpty()) {
                TradeOfferHelper.registerVillagerOffers(MEDIC, level, factories -> factories.addAll(listings));
            }
        });
    }
}
