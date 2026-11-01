package com.claudemods.triage.village;

import com.claudemods.triage.TriageMod;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.server.MinecraftServer;

/**
 * Puts the clinic into village generation.
 *
 * <p>There is no API for adding a building to a village and no datapack way either: a
 * template pool can only be replaced, never appended to, and replacing
 * {@code minecraft:village/plains/houses} would throw away every other mod's buildings
 * along with it. So the piece is added at server start to the list the jigsaw generator
 * actually reads, which is what every mod that ships a village building does.
 *
 * <p>One clinic per style, because a village builds out of its own materials and an oak
 * cabin in a desert village reads as somebody else's building dropped into the street.
 */
public final class VillageHouses {
    /**
     * How often the clinic comes up against the other houses. Vanilla weights run from 2
     * to 10; this sits low on purpose. One medic in a village is a find. Two is a shop.
     */
    private static final int WEIGHT = 3;

    private static final List<String> STYLES = List.of("plains", "desert", "savanna", "snowy", "taiga");

    /**
     * The do-nothing processor list. Named here rather than taken from
     * {@code ProcessorLists.EMPTY}, which is private: the key is public knowledge and
     * spelling it out is cheaper than another access transformer.
     */
    private static final ResourceKey<StructureProcessorList> NO_PROCESSORS = ResourceKey.create(
            Registries.PROCESSOR_LIST, ResourceLocation.withDefaultNamespace("empty"));

    private VillageHouses() {
    }

    public static void onServerAboutToStart(MinecraftServer server) {
        Registry<StructureTemplatePool> pools =
                server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processors =
                server.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
        Holder<StructureProcessorList> empty = processors.getHolderOrThrow(NO_PROCESSORS);

        int added = 0;
        for (String style : STYLES) {
            added += add(pools, style, empty) ? 1 : 0;
        }
        // One line, at startup, saying whether this worked. A village building that
        // fails to register fails silently and then simply never appears, which is
        // indistinguishable from bad luck for a very long time.
        TriageMod.LOGGER.info("Clinic added to {} of {} village house pools", added, STYLES.size());
    }

    private static boolean add(Registry<StructureTemplatePool> pools, String style,
                               Holder<StructureProcessorList> processors) {
        ResourceKey<StructureTemplatePool> key = ResourceKey.create(Registries.TEMPLATE_POOL,
                ResourceLocation.fromNamespaceAndPath("minecraft", "village/" + style + "/houses"));
        StructureTemplatePool pool = pools.get(key);
        if (pool == null) {
            // A pack may well have removed a whole village style. That is their business.
            TriageMod.LOGGER.info("No {} house pool; the medic will not build there", style);
            return false;
        }
        int before = pool.templates.size();

        StructurePoolElement piece = StructurePoolElement
                .single(TriageMod.MODID + ":village/houses/clinic_" + style, processors)
                .apply(StructureTemplatePool.Projection.RIGID);

        // Two lists, and both matter: templates is what generation picks from, and
        // rawTemplates is what the pool would be written back out as. Keeping them in
        // step costs nothing and stops the pool describing itself wrongly to anything
        // that reads it later.
        for (int i = 0; i < WEIGHT; i++) {
            pool.templates.add(piece);
        }
        try {
            pool.rawTemplates.add(Pair.of(piece, WEIGHT));
        } catch (UnsupportedOperationException immutable) {
            // Loaded straight from the codec, so it is a fixed list. Generation only
            // reads the other one, so this is worth noting and not worth failing over.
            TriageMod.LOGGER.debug("{} house pool keeps an immutable raw list", style);
        }
        TriageMod.LOGGER.debug("{} houses: {} entries -> {}", style, before, pool.templates.size());
        return pool.templates.size() > before;
    }
}
