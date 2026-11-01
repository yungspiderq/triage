package com.claudemods.triage.body;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * Which blood a body has, and whose it will accept.
 *
 * <p>The real ABO and Rh rules, because they already are a good mechanic: blood may only
 * go into somebody who has every antigen it carries. That makes O- worth more than
 * anything else in the box and AB+ the luckiest thing to be born, and neither of those
 * had to be invented.
 *
 * <p>Rolled fresh on every spawn, so the matched bags a player hoarded stop matching the
 * moment they die - the stock is worth what it is worth to the group, not to its owner.
 */
public enum BloodType implements StringRepresentable {
    O_NEG("o_neg", "O−", false, false, false, 7),
    O_POS("o_pos", "O+", false, false, true, 38),
    A_NEG("a_neg", "A−", true, false, false, 6),
    A_POS("a_pos", "A+", true, false, true, 34),
    B_NEG("b_neg", "B−", false, true, false, 2),
    B_POS("b_pos", "B+", false, true, true, 9),
    AB_NEG("ab_neg", "AB−", true, true, false, 1),
    AB_POS("ab_pos", "AB+", true, true, true, 3);

    public static final BloodType[] VALUES = values();
    public static final Codec<BloodType> CODEC = StringRepresentable.fromEnum(BloodType::values);

    private static final int TOTAL_WEIGHT;

    static {
        int sum = 0;
        for (BloodType type : VALUES) {
            sum += type.weight;
        }
        TOTAL_WEIGHT = sum;
    }

    private final String id;
    private final String label;
    private final boolean a;
    private final boolean b;
    private final boolean rh;
    /** Roughly how common this is in real people, so O- stays rare enough to matter. */
    private final int weight;

    BloodType(String id, String label, boolean a, boolean b, boolean rh, int weight) {
        this.id = id;
        this.label = label;
        this.a = a;
        this.b = b;
        this.rh = rh;
        this.weight = weight;
    }

    public String id() {
        return id;
    }

    /**
     * "A+", "O-" and so on. Not translated on purpose: the notation is the same in every
     * language a player is likely to be reading this in, and inventing local spellings
     * for it would only make two players describe the same bag differently.
     */
    public String label() {
        return label;
    }

    public Component title() {
        return Component.literal(label).withStyle(colour());
    }

    /** Universal donors read as something worth keeping; universal recipients as lucky. */
    public ChatFormatting colour() {
        if (this == O_NEG) {
            return ChatFormatting.GREEN;
        }
        return rh ? ChatFormatting.RED : ChatFormatting.GOLD;
    }

    /**
     * Whether blood of this type can go into somebody of that one.
     *
     * <p>One rule: every antigen the donor carries, the recipient must already have.
     * Anything else and the recipient's body attacks it.
     */
    public boolean canGiveTo(BloodType recipient) {
        return (!a || recipient.a) && (!b || recipient.b) && (!rh || recipient.rh);
    }

    /** True for O-, which anybody can take. Worth saying out loud in a tooltip. */
    public boolean universalDonor() {
        return this == O_NEG;
    }

    public static BloodType random(RandomSource random) {
        int roll = random.nextInt(TOTAL_WEIGHT);
        for (BloodType type : VALUES) {
            roll -= type.weight;
            if (roll < 0) {
                return type;
            }
        }
        return O_POS;
    }

    public static @Nullable BloodType byId(String id) {
        for (BloodType type : VALUES) {
            if (type.id.equals(id.toLowerCase(Locale.ROOT))) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
