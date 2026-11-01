package com.claudemods.triage.item;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TriageMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TriageMod.MODID);

    public static final DeferredItem<Item> BANDAGE = ITEMS.register("bandage",
            () -> new MedicineItem(MedicineKind.BANDAGE, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> TOURNIQUET = ITEMS.register("tourniquet",
            () -> new MedicineItem(MedicineKind.TOURNIQUET, new Item.Properties().stacksTo(8)));
    public static final DeferredItem<Item> SPLINT = ITEMS.register("splint",
            () -> new MedicineItem(MedicineKind.SPLINT, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> MEDKIT = ITEMS.register("medkit",
            () -> new MedicineItem(MedicineKind.MEDKIT, new Item.Properties().stacksTo(8)));
    public static final DeferredItem<Item> SURGICAL_KIT = ITEMS.register("surgical_kit",
            () -> new MedicineItem(MedicineKind.SURGERY, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<Item> PAINKILLERS = ITEMS.register("painkillers",
            () -> new MedicineItem(MedicineKind.PAINKILLER, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> ANTISEPTIC = ITEMS.register("antiseptic",
            () -> new MedicineItem(MedicineKind.ANTISEPTIC, new Item.Properties().stacksTo(8)));
    public static final DeferredItem<Item> BURN_SALVE = ITEMS.register("burn_salve",
            () -> new MedicineItem(MedicineKind.SALVE, new Item.Properties().stacksTo(8)));
    public static final DeferredItem<Item> ANTIBIOTICS = ITEMS.register("antibiotics",
            () -> new MedicineItem(MedicineKind.ANTIBIOTICS, new Item.Properties().stacksTo(8)));
    // Not a cure and not sold as one. Sixteen to a card, because a course is a thing
    // you are on rather than a thing you use.
    public static final DeferredItem<Item> ANTIPSYCHOTIC = ITEMS.register("antipsychotic",
            () -> new MedicineItem(MedicineKind.ANTIPSYCHOTIC, new Item.Properties().stacksTo(16)));
    // One to a hand and never spent. A saw is a tool, and the question this operation
    // asks is whether to do it at all rather than whether there is one left in the bag.
    public static final DeferredItem<Item> BONE_SAW = ITEMS.register("bone_saw",
            () -> new MedicineItem(MedicineKind.AMPUTATE, new Item.Properties().stacksTo(1)));
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
    public static final DeferredItem<Item> FORCEPS = ITEMS.register("forceps",
            () -> new MedicineItem(MedicineKind.EXTRACT, new Item.Properties().stacksTo(1)));
    /**
     * The three that go in through a needle.
     *
     * <p>Everything else in the bag is something you do with your hands over a minute.
     * These are decisions made in a second, and all three of them can be the wrong one.
     */
    public static final DeferredItem<Item> EPINEPHRINE = ITEMS.register("epinephrine",
            () -> new MedicineItem(MedicineKind.EPINEPHRINE, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<Item> MORPHINE = ITEMS.register("morphine",
            () -> new MedicineItem(MedicineKind.MORPHINE, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<Item> ANTICOAGULANT = ITEMS.register("anticoagulant",
            () -> new MedicineItem(MedicineKind.ANTICOAGULANT, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<Item> NALOXONE = ITEMS.register("naloxone",
            () -> new MedicineItem(MedicineKind.NALOXONE, new Item.Properties().stacksTo(4)));
    /**
     * The only instrument here that treats nothing.
     *
     * <p>It is not spent and it never runs out, because what it costs is not the item, it
     * is the half minute of standing still while the column stops climbing.
     */
    public static final DeferredItem<Item> THERMOMETER = ITEMS.register("thermometer",
            () -> new MedicineItem(MedicineKind.THERMOMETER, new Item.Properties().stacksTo(1)));
    /**
     * The second instrument that treats nothing, and the only way into a chest.
     *
     * <p>Carried, not spent, like the thermometer and the saw. What it costs is the four
     * seconds of quiet it wants, and on a battlefield that is not a small price.
     */
    public static final DeferredItem<Item> STETHOSCOPE = ITEMS.register("stethoscope",
            () -> new MedicineItem(MedicineKind.STETHOSCOPE, new Item.Properties().stacksTo(1)));
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
    public static final DeferredItem<Item> WORMING = ITEMS.register("worming",
            () -> new MedicineItem(MedicineKind.WORMING, new Item.Properties()));
    /** The sentence you will not be able to say, on a strip of metal. */
    public static final DeferredItem<Item> BRACELET = ITEMS.register("bracelet",
            () -> new Bracelet(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SEVERED_LIMB = ITEMS.register("severed_limb",
            () -> new SeveredLimb(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SCISSORS = ITEMS.register("bandage_scissors",
            () -> new MedicineItem(MedicineKind.SCISSORS, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WOODEN_LEG = ITEMS.register("wooden_leg",
            () -> new MedicineItem(MedicineKind.PEG, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<Item> HOOK = ITEMS.register("hook",
            () -> new MedicineItem(MedicineKind.HOOK, new Item.Properties().stacksTo(4)));
    // The second tier, and the whole difference is one part: a joint.
    public static final DeferredItem<Item> JOINTED_LEG = ITEMS.register("jointed_leg",
            () -> new MedicineItem(MedicineKind.JOINT, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<Item> MECHANICAL_HAND = ITEMS.register("mechanical_hand",
            () -> new MedicineItem(MedicineKind.HAND, new Item.Properties().stacksTo(4)));
    // Four to a bottle, and it is the most dangerous thing in the box: the dose that
    // stops somebody feeling an operation is a short way from the dose that stops them
    // breathing.
    public static final DeferredItem<Item> ANAESTHETIC = ITEMS.register("anaesthetic",
            () -> new MedicineItem(MedicineKind.ANAESTHETIC, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<Item> SALINE = ITEMS.register("saline",
            () -> new MedicineItem(MedicineKind.TRANSFUSION, new Item.Properties().stacksTo(4)));
    // Single use and awkward to make, because it answers the fastest thing in the mod and
    // a stack of them in every pocket would take the deadline away again.
    public static final DeferredItem<Item> DECOMPRESSION_NEEDLE = ITEMS.register("decompression_needle",
            () -> new MedicineItem(MedicineKind.DECOMPRESS, new Item.Properties().stacksTo(4)));
    // Stacks by what is in it, which components give for free: two A+ bags sit in one
    // slot, an A+ and a B- do not, and that is exactly the right behaviour.
    public static final DeferredItem<Item> BLOOD_BAG = ITEMS.register("blood_bag",
            () -> new BloodBagItem(new Item.Properties().stacksTo(8)));
    // Not medicine, and deliberately the cheapest thing in the box: what a tag costs is
    // the walk over and the decision, not the paper.
    public static final DeferredItem<Item> TRIAGE_TAG = ITEMS.register("triage_tag",
            () -> new TriageTagItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<BlockItem> MEDICAL_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.MEDICAL_TABLE);
    public static final DeferredItem<BlockItem> XRAY_MACHINE = ITEMS.registerSimpleBlockItem(ModBlocks.XRAY_MACHINE);

    public static final Supplier<CreativeModeTab> TAB = TABS.register("medicine",
            () -> CreativeModeTab.builder()
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

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
