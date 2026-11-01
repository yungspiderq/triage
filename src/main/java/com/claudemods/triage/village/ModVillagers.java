package com.claudemods.triage.village;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.block.ModBlocks;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The medic: a villager who sells what the mod expects you to carry.
 *
 * <p>Crafting was the only source of medicine, which meant a player who had not gone
 * looking for sugar cane and nether wart simply had none. A trader does not replace
 * crafting - the good things are still deep in the trade list - it gives the supply line
 * a second end.
 */
public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, TriageMod.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, TriageMod.MODID);

    public static final DeferredHolder<PoiType, PoiType> MEDIC_POI = POI_TYPES.register("medic",
            () -> new PoiType(states(), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> MEDIC =
            PROFESSIONS.register("medic", () -> new VillagerProfession(
                    "medic",
                    holder -> holder.value() == MEDIC_POI.get(),
                    holder -> holder.value() == MEDIC_POI.get(),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CLERIC));

    private ModVillagers() {
    }

    /** Every state of the table counts as the job site, since it has only the one. */
    private static Set<BlockState> states() {
        return ImmutableSet.copyOf(ModBlocks.MEDICAL_TABLE.get().getStateDefinition().getPossibleStates());
    }

    public static void register(IEventBus modBus) {
        POI_TYPES.register(modBus);
        PROFESSIONS.register(modBus);
    }
}
