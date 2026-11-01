package com.claudemods.triage.village;

import com.claudemods.triage.Reach;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerAboutToStartEvent;

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
@Mod.EventBusSubscriber(modid = TriageMod.MODID)
public final class VillageHouses {
    /**
     * The two lists a pool keeps, opened by hand.
     *
     * <p>There is no API for adding to a village pool - a datapack can replace one and
     * never append to one - and the transformer that would open these did not apply on
     * this toolchain. So they are opened at runtime, once.
     *
     * <p>By type rather than by name, and this is the whole of the bug that shipped in
     * 1.9.0: the names are {@code templates} and {@code rawTemplates} while the mod is
     * being written and {@code f_210560_} and {@code f_210559_} when anybody plays it, so
     * the lookup failed on every machine that was not this one - in a static initialiser,
     * which took the mod down with it. The types cannot be remapped: one of the two lists
     * is an {@code ObjectArrayList} and the other is a plain {@code List}, which is enough
     * to tell them apart under any names at all.
     *
     * <p>Null when a future version moves them. Then there is no clinic in the villages
     * and everything else in the mod still works, which is the correct price for a
     * building.
     */
    private static final java.lang.reflect.Field TEMPLATES =
            Reach.sole(StructureTemplatePool.class, it.unimi.dsi.fastutil.objects.ObjectArrayList.class);
    private static final java.lang.reflect.Field RAW_TEMPLATES =
            Reach.sole(StructureTemplatePool.class, List.class);

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
            Registries.PROCESSOR_LIST, new ResourceLocation("empty"));

    private VillageHouses() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        // Nothing to add to. Said once, plainly, and then the mod carries on without a
        // building in the villages - which is a thing players will never notice and a
        // crash is a thing they cannot do anything else about.
        if (TEMPLATES == null || RAW_TEMPLATES == null) {
            TriageMod.LOGGER.warn("Village pools cannot be opened on this build; "
                    + "no clinic will generate. Everything else works.");
            return;
        }
        Registry<StructureTemplatePool> pools =
                event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processors =
                event.getServer().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
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
                new ResourceLocation("minecraft", "village/" + style + "/houses"));
        StructureTemplatePool pool = pools.get(key);
        if (pool == null) {
            // A pack may well have removed a whole village style. That is their business.
            TriageMod.LOGGER.info("No {} house pool; the medic will not build there", style);
            return false;
        }
        List<StructurePoolElement> templates = Reach.get(TEMPLATES, pool);
        List<Pair<StructurePoolElement, Integer>> raw = Reach.get(RAW_TEMPLATES, pool);
        if (templates == null || raw == null) {
            return false;
        }
        int before = templates.size();

        StructurePoolElement piece = StructurePoolElement
                .single(TriageMod.MODID + ":village/houses/clinic_" + style, processors)
                .apply(StructureTemplatePool.Projection.RIGID);

        // Two lists, and both matter: templates is what generation picks from, and
        // rawTemplates is what the pool would be written back out as. Keeping them in
        // step costs nothing and stops the pool describing itself wrongly to anything
        // that reads it later.
        for (int i = 0; i < WEIGHT; i++) {
            templates.add(piece);
        }
        try {
            raw.add(Pair.of(piece, WEIGHT));
        } catch (UnsupportedOperationException immutable) {
            // Loaded straight from the codec, so it is a fixed list. Generation only
            // reads the other one, so this is worth noting and not worth failing over.
            TriageMod.LOGGER.debug("{} house pool keeps an immutable raw list", style);
        }
        TriageMod.LOGGER.debug("{} houses: {} entries -> {}", style, before, templates.size());
        return templates.size() > before;
    }
}
