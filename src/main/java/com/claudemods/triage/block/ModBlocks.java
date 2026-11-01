package com.claudemods.triage.block;

/*
 * Translated from the Forge 1.20.1 tree by tools/registries.py. What each thing is, and
 * why, is the same file there; only the lines that hand it to a registry are different.
 */

import com.claudemods.triage.TriageMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The one block the mod has: somewhere for a medic to work.
 *
 * <p>It was a workstation and nothing else - no inventory, no screen - which was a
 * strange thing for the one block in a mod about medicine to be. It still gives a village
 * somewhere to put a medic, and it is now also where instruments are cleaned and the
 * reason an operation goes better indoors than in a hole.
 *
 * <p>Still no inventory and still no screen. Treating people happens on the body,
 * wherever they fell; the table is somewhere to put them, not somewhere to put items.
 */
public final class ModBlocks {

    public static final Supplier<Block> MEDICAL_TABLE = block("medical_table", new MedicalTable(BlockBehaviour.Properties.copy(Blocks.CARTOGRAPHY_TABLE)));

    /**
     * And the second: the one thing in the mod that looks inside somebody.
     *
     * <p>A block rather than an item on purpose. Everything else a medic owns travels in
     * a bag, which is what makes this mod's medicine portable and why a clearing full of
     * casualties is a place you can work. A machine that sees bone is the opposite of
     * that: it is heavy, it is where you left it, and finding out what is actually broken
     * means bringing the patient to it.
     */
    public static final Supplier<Block> XRAY_MACHINE = block("xray_machine", new XrayMachine(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    private ModBlocks() {
    }

    /** Touching the class is what registers it; called once from the initialiser. */
    public static void register() {
    }

    private static Supplier<Block> block(String name, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, TriageMod.id(name), block);
        return () -> block;
    }
}
