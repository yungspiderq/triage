package com.claudemods.triage.body;

import com.claudemods.triage.med.MedicineKind;
import com.mojang.serialization.Codec;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The one thing in the bag that will kill this particular person.
 *
 * <p>Rolled with the blood type, at the same moment and for the same reason: it is a fact
 * about somebody rather than an event in their day, it is true before anything happens to
 * them, and it is the kind of fact a medic wants before they open the bag rather than
 * after.
 *
 * <p>It costs almost nothing to build because the mod already has both ends of it. The
 * thing that happens is anaphylaxis, which is already here - it is what the wrong blood
 * does - and the answer is already here too, and it is the needle that exists for exactly
 * this. What was missing was the commonest cause of it in the world, which is not a
 * transfusion. It is a drug given to somebody who told you, or would have told you.
 *
 * <p>Which is the whole of why it is interesting: <b>you can ask.</b> A conscious patient
 * knows their own and will say so, and an unconscious one will not - so the same dose is a
 * treatment on a man who can talk and a killing on the same man ten minutes later. Nothing
 * else in this mod turns on whether the patient can still answer a question.
 */
public enum Allergy implements StringRepresentable {
    /** Most people. Kept in the enum rather than as a null so the chart can say it. */
    NONE("none", null, null),
    /**
     * Penicillin, in everything but name. The famous one, and the one most likely to be
     * reached for in a hurry: an infected wound is the commonest thing in the mod that
     * wants a bottle.
     */
    ANTIBIOTICS("antibiotics", MedicineKind.ANTIBIOTICS),
    /**
     * The opiate. Rarer in life than people think, and worse when it happens, because the
     * patient it is given to is by definition somebody in enough pain to need it.
     */
    MORPHINE("morphine", MedicineKind.MORPHINE),
    /**
     * And the mask. The one that arrives at the worst possible moment - the patient is
     * already open, or about to be, and the person holding the knife has both hands full.
     */
    ANAESTHETIC("anaesthetic", MedicineKind.ANAESTHETIC),

    // ---- and the ones that are not in anybody's bag ------------------------
    //
    // Drug allergies are the ones that matter to a medic and they are not the ones
    // people actually have. The commonest allergies in the world are foods, they turn up
    // in childhood, and what they mean is that an ordinary afternoon contains one thing
    // that will put you on the floor - which is a far stranger thing to play around than
    // a bottle somebody else is holding.
    //
    // What makes them work here is that nothing warns you. Food in this game is the one
    // thing every player handles without thinking; it is what you do when you are hurt,
    // when you are bored, and when you are running away from something. Having one item
    // in that set that is a mistake is the whole idea, and it is why these are told to
    // the player on the day they are rolled rather than discovered the hard way.

    /** Fish, in every form the game has. The classic, and easy to meet by accident. */
    FISH("fish", stack -> stack.is(ItemTags.FISHES)),
    /**
     * Wheat, which is bread, which is the first thing anybody eats in this game.
     *
     * <p>The hardest one to live with by a long way, and that is not an accident of
     * design - it is what coeliac disease is like.
     */
    WHEAT("wheat", stack -> stack.is(Items.BREAD) || stack.is(Items.COOKIE)),
    /** Egg, which hides in the baking rather than turning up on its own. */
    EGGS("eggs", stack -> stack.is(Items.PUMPKIN_PIE) || stack.is(Items.EGG)),
    /** Milk - and the one that also takes away the standard answer to everything else. */
    MILK("milk", stack -> stack.is(Items.MILK_BUCKET)),
    /** Honey, which is the sweet thing everybody carries once they have bees. */
    HONEY("honey", stack -> stack.is(Items.HONEY_BOTTLE) || stack.is(Items.HONEYCOMB)),
    /** Mushrooms, in a stew or on their own. */
    MUSHROOM("mushroom", stack -> stack.is(Items.MUSHROOM_STEW) || stack.is(Items.SUSPICIOUS_STEW)
            || stack.is(Items.RED_MUSHROOM) || stack.is(Items.BROWN_MUSHROOM)),
    /** And berries, which are the food you eat when you have nothing else. */
    BERRIES("berries", stack -> stack.is(Items.SWEET_BERRIES) || stack.is(Items.GLOW_BERRIES));

    public static final Allergy[] VALUES = values();
    public static final Codec<Allergy> CODEC = StringRepresentable.fromEnum(Allergy::values);

    private final String id;
    private final @Nullable MedicineKind kind;
    private final @Nullable Predicate<ItemStack> food;

    Allergy(String id, @Nullable MedicineKind kind) {
        this(id, kind, null);
    }

    Allergy(String id, Predicate<ItemStack> food) {
        this(id, null, food);
    }

    Allergy(String id, @Nullable MedicineKind kind, @Nullable Predicate<ItemStack> food) {
        this.id = id;
        this.kind = kind;
        this.food = food;
    }

    public String id() {
        return id;
    }

    /** Whether this is the drug that does it. */
    public boolean is(MedicineKind other) {
        return kind != null && kind == other;
    }

    /** Whether this is the thing on the end of the fork. */
    public boolean eats(ItemStack stack) {
        return food != null && !stack.isEmpty() && food.test(stack);
    }

    /** Whether it is a food rather than a drug, which decides who has to know about it. */
    public boolean dietary() {
        return food != null;
    }

    public boolean any() {
        return kind != null || food != null;
    }

    public Component title() {
        return Component.translatable("triage.allergy." + id);
    }

    /**
     * One in however many, and which one is even odds between the three.
     *
     * <p>Deliberately not weighted towards the antibiotics even though that is the true
     * frequency: the other two are the interesting ones, because they are the drugs given
     * to somebody who is past answering questions.
     */
    public static Allergy roll(RandomSource random, float chance) {
        if (chance <= 0.0F || random.nextFloat() >= chance) {
            return NONE;
        }
        // Weighted towards the foods, because that is the true shape of it: most people
        // who have an allergy have one to something they eat, and only a few have one to
        // something a medic would give them. It also puts the interesting half where the
        // player can act on it - a drug allergy is somebody else's problem to remember,
        // and a food allergy is yours, every time you open your own inventory.
        if (random.nextFloat() < 0.62F) {
            Allergy[] foods = {FISH, WHEAT, EGGS, MILK, HONEY, MUSHROOM, BERRIES};
            return foods[random.nextInt(foods.length)];
        }
        return switch (random.nextInt(3)) {
            case 0 -> ANTIBIOTICS;
            case 1 -> MORPHINE;
            default -> ANAESTHETIC;
        };
    }

    public static Allergy byId(String id) {
        for (Allergy allergy : VALUES) {
            if (allergy.id.equals(id.toLowerCase(Locale.ROOT))) {
                return allergy;
            }
        }
        return NONE;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
