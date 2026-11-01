package com.claudemods.triage.block;

import net.minecraftforge.registries.ForgeRegistries;
import com.claudemods.triage.TriageMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

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
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TriageMod.MODID);

    public static final RegistryObject<Block> MEDICAL_TABLE = BLOCKS.register("medical_table",
            () -> new MedicalTable(BlockBehaviour.Properties.copy(Blocks.CARTOGRAPHY_TABLE)));

    /**
     * And the second: the one thing in the mod that looks inside somebody.
     *
     * <p>A block rather than an item on purpose. Everything else a medic owns travels in
     * a bag, which is what makes this mod's medicine portable and why a clearing full of
     * casualties is a place you can work. A machine that sees bone is the opposite of
     * that: it is heavy, it is where you left it, and finding out what is actually broken
     * means bringing the patient to it.
     */
    public static final RegistryObject<Block> XRAY_MACHINE = BLOCKS.register("xray_machine",
            () -> new XrayMachine(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
