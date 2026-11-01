package com.claudemods.triage.item;

/*
 * Translated from the Forge 1.20.1 tree by tools/registries.py. What each thing is, and
 * why, is the same file there; only the lines that hand it to a registry are different.
 */

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.block.ModBlocks;
import com.claudemods.triage.body.BloodType;
import com.claudemods.triage.med.MedicineKind;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModItems {

    public static final Supplier<Item> BANDAGE = item("bandage", new MedicineItem(MedicineKind.BANDAGE, new Item.Properties().stacksTo(16)));
    public static final Supplier<Item> TOURNIQUET = item("tourniquet", new MedicineItem(MedicineKind.TOURNIQUET, new Item.Properties().stacksTo(8)));
    public static final Supplier<Item> SPLINT = item("splint", new MedicineItem(MedicineKind.SPLINT, new Item.Properties().stacksTo(16)));
    public static final Supplier<Item> MEDKIT = item("medkit", new MedicineItem(MedicineKind.MEDKIT, new Item.Properties().stacksTo(8)));
    public static final Supplier<Item> SURGICAL_KIT = item("surgical_kit", new MedicineItem(MedicineKind.SURGERY, new Item.Properties().stacksTo(4)));
    public static final Supplier<Item> PAINKILLERS = item("painkillers", new MedicineItem(MedicineKind.PAINKILLER, new Item.Properties().stacksTo(16)));
    public static final Supplier<Item> ANTISEPTIC = item("antiseptic", new MedicineItem(MedicineKind.ANTISEPTIC, new Item.Properties().stacksTo(8)));
    public static final Supplier<Item> BURN_SALVE = item("burn_salve", new MedicineItem(MedicineKind.SALVE, new Item.Properties().stacksTo(8)));
    public static final Supplier<Item> ANTIBIOTICS = item("antibiotics", new MedicineItem(MedicineKind.ANTIBIOTICS, new Item.Properties().stacksTo(8)));
    // Not a cure and not sold as one. Sixteen to a card, because a course is a thing
    // you are on rather than a thing you use.
    public static final Supplier<Item> ANTIPSYCHOTIC = item("antipsychotic", new MedicineItem(MedicineKind.ANTIPSYCHOTIC, new Item.Properties().stacksTo(16)));
    // One to a hand and never spent. A saw is a tool, and the question this operation
    // asks is whether to do it at all rather than whether there is one left in the bag.
    public static final Supplier<Item> BONE_SAW = item("bone_saw", new MedicineItem(MedicineKind.AMPUTATE, new Item.Properties().stacksTo(1)));
    // Neither of these is the limb. They are made of what anybody has - a plank, a
    // strap, a bit of iron - because the point of them is that they exist at all.
    // The second tool in the box, and the same bargain as the saw: carried, not spent.
    /**
     * Forceps, and the third tool in the box that is carried rather than spent.
     *
     * <p>Not a treatment - it never appears on the medicine screen and it does not do
     * anything on its own. It is a pair of jaws, and what it changes is a job you can
     * already do badly with your fingers: getting fragments out of somebody.
     *
     * <p>Fingers reach what is just under the skin and tear everything they have to dig
     * for. Jaws hold what fingers cannot and hold it for as long as it takes. That is the
     * whole of the difference and it is the whole of what forceps are for.
     */
    public static final Supplier<Item> FORCEPS = item("forceps", new MedicineItem(MedicineKind.EXTRACT, new Item.Properties().stacksTo(1)));
    /**
     * The three that go in through a needle.
     *
     * <p>Everything else in the bag is something you do with your hands over a minute.
     * These are decisions made in a second, and all three of them can be the wrong one.
     */
    public static final Supplier<Item> EPINEPHRINE = item("epinephrine", new MedicineItem(MedicineKind.EPINEPHRINE, new Item.Properties().stacksTo(4)));
    public static final Supplier<Item> MORPHINE = item("morphine", new MedicineItem(MedicineKind.MORPHINE, new Item.Properties().stacksTo(4)));
    public static final Supplier<Item> ANTICOAGULANT = item("anticoagulant", new MedicineItem(MedicineKind.ANTICOAGULANT, new Item.Properties().stacksTo(4)));
    public static final Supplier<Item> NALOXONE = item("naloxone", new MedicineItem(MedicineKind.NALOXONE, new Item.Properties().stacksTo(4)));
    /**
     * The only instrument here that treats nothing.
     *
     * <p>It is not spent and it never runs out, because what it costs is not the item, it
     * is the half minute of standing still while the column stops climbing.
     */
    public static final Supplier<Item> THERMOMETER = item("thermometer", new MedicineItem(MedicineKind.THERMOMETER, new Item.Properties().stacksTo(1)));
    /**
     * The second instrument that treats nothing, and the only way into a chest.
     *
     * <p>Carried, not spent, like the thermometer and the saw. What it costs is the four
     * seconds of quiet it wants, and on a battlefield that is not a small price.
     */
    public static final Supplier<Item> STETHOSCOPE = item("stethoscope", new MedicineItem(MedicineKind.STETHOSCOPE, new Item.Properties().stacksTo(1)));
    /**
     * Somebody's arm, on the floor, with a clock on it.
     *
     * <p>Not crafted and not sold: the only way to have one is for somebody to have lost
     * it, which is the whole of what it means.
     */
    /**
     * Worming tablets, which answer one illness and nothing else.
     *
     * <p>A second card that looks like the first and does a different job, on purpose:
     * two cards that refuse each other's work is the clearest way this mod has of saying
     * that medicine is specific.
     */
    public static final Supplier<Item> WORMING = item("worming", new MedicineItem(MedicineKind.WORMING, new Item.Properties()));
    /** The sentence you will not be able to say, on a strip of metal. */
    public static final Supplier<Item> BRACELET = item("bracelet", new Bracelet(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> SEVERED_LIMB = item("severed_limb", new SeveredLimb(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> SCISSORS = item("bandage_scissors", new MedicineItem(MedicineKind.SCISSORS, new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> WOODEN_LEG = item("wooden_leg", new MedicineItem(MedicineKind.PEG, new Item.Properties().stacksTo(4)));
    public static final Supplier<Item> HOOK = item("hook", new MedicineItem(MedicineKind.HOOK, new Item.Properties().stacksTo(4)));
    // The second tier, and the whole difference is one part: a joint.
    public static final Supplier<Item> JOINTED_LEG = item("jointed_leg", new MedicineItem(MedicineKind.JOINT, new Item.Properties().stacksTo(4)));
    public static final Supplier<Item> MECHANICAL_HAND = item("mechanical_hand", new MedicineItem(MedicineKind.HAND, new Item.Properties().stacksTo(4)));
    // Four to a bottle, and it is the most dangerous thing in the box: the dose that
    // stops somebody feeling an operation is a short way from the dose that stops them
    // breathing.
    public static final Supplier<Item> ANAESTHETIC = item("anaesthetic", new MedicineItem(MedicineKind.ANAESTHETIC, new Item.Properties().stacksTo(4)));
    public static final Supplier<Item> SALINE = item("saline", new MedicineItem(MedicineKind.TRANSFUSION, new Item.Properties().stacksTo(4)));
    // Single use and awkward to make, because it answers the fastest thing in the mod and
    // a stack of them in every pocket would take the deadline away again.
    public static final Supplier<Item> DECOMPRESSION_NEEDLE = item("decompression_needle", new MedicineItem(MedicineKind.DECOMPRESS, new Item.Properties().stacksTo(4)));
    // Stacks by what is in it, which components give for free: two A+ bags sit in one
    // slot, an A+ and a B- do not, and that is exactly the right behaviour.
    public static final Supplier<Item> BLOOD_BAG = item("blood_bag", new BloodBagItem(new Item.Properties().stacksTo(8)));
    // Not medicine, and deliberately the cheapest thing in the box: what a tag costs is
    // the walk over and the decision, not the paper.
    public static final Supplier<Item> TRIAGE_TAG = item("triage_tag", new TriageTagItem(new Item.Properties().stacksTo(16)));

    public static final Supplier<Item> MEDICAL_TABLE = item("medical_table", new BlockItem(ModBlocks.MEDICAL_TABLE.get(), new Item.Properties()));
    public static final Supplier<BlockItem> XRAY_MACHINE = item("xray_machine", new BlockItem(ModBlocks.XRAY_MACHINE.get(), new Item.Properties()));

    public static final Supplier<CreativeModeTab> TAB = tab("medicine", FabricItemGroup.builder()
                    .title(Component.translatable("triage.tab.medicine"))
                    .icon(() -> new ItemStack(MEDKIT.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(MEDICAL_TABLE.get());
                        output.accept(XRAY_MACHINE.get());
                        output.accept(BANDAGE.get());
                        output.accept(TOURNIQUET.get());
                        output.accept(SPLINT.get());
                        output.accept(MEDKIT.get());
                        output.accept(SURGICAL_KIT.get());
                        output.accept(PAINKILLERS.get());
                        output.accept(ANTISEPTIC.get());
                        output.accept(BURN_SALVE.get());
                        output.accept(ANTIBIOTICS.get());
                        output.accept(ANTIPSYCHOTIC.get());
                        output.accept(SALINE.get());
                        output.accept(DECOMPRESSION_NEEDLE.get());
                        output.accept(ANAESTHETIC.get());
                        // The three needles, next to the other thing that goes in through
                        // one. Registering an item is not the same as putting it in the
                        // box, and the box is where anybody actually looks for it.
                        output.accept(THERMOMETER.get());
                        output.accept(STETHOSCOPE.get());
                        output.accept(WORMING.get());
                        output.accept(BRACELET.get());
                        // In the tab because a creative player has no other way to get
                        // one: it is the only item here that cannot be crafted, traded
                        // or found, because the only way to have one is for somebody to
                        // have lost it.
                        output.accept(SEVERED_LIMB.get());
                        output.accept(EPINEPHRINE.get());
                        output.accept(MORPHINE.get());
                        output.accept(NALOXONE.get());
                        output.accept(ANTICOAGULANT.get());
                        output.accept(FORCEPS.get());
                        output.accept(SCISSORS.get());
                        output.accept(BONE_SAW.get());
                        output.accept(WOODEN_LEG.get());
                        output.accept(HOOK.get());
                        output.accept(JOINTED_LEG.get());
                        output.accept(MECHANICAL_HAND.get());
                        output.accept(BLOOD_BAG.get());
                        output.accept(TRIAGE_TAG.get());
                        // Every type, so a creative pack can hand out matched blood
                        // without having to draw it out of somebody first.
                        for (BloodType type : BloodType.VALUES) {
                            output.accept(BloodBag.of(type));
                        }
                    })
                    .build());

    private ModItems() {
    }

    /** Touching the class is what registers it; called once from the initialiser. */
    public static void register() {
    }

    private static <T extends Item> Supplier<T> item(String name, T item) {
        Registry.register(BuiltInRegistries.ITEM, TriageMod.id(name), item);
        return () -> item;
    }

    private static Supplier<CreativeModeTab> tab(String name, CreativeModeTab tab) {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TriageMod.id(name), tab);
        return () -> tab;
    }
}
