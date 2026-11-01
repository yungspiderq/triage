package com.claudemods.triage.body;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * What somebody wrote on a patient after looking at them.
 *
 * <p>The four colours of real triage, in the order they are decided: this one now, this
 * one can wait, this one can walk, this one is past helping. They are a judgement rather
 * than a measurement - the mod does not work out which of them is correct and never
 * will, because the whole difficulty of sorting the wounded is that the person doing it
 * has to decide.
 *
 * <p>The symbol matters as much as the colour. Four coloured marks that differ only in
 * colour are four identical marks to anybody who cannot tell red from green, and this is
 * the one readout in the mod that is meant to be read across a field at a glance.
 */
public enum TriageTag implements StringRepresentable {
    /** Now, or not at all. */
    IMMEDIATE("immediate", "!", ChatFormatting.RED, 0xFFC4282C),
    /** Hurt, and will still be hurt in five minutes. */
    DELAYED("delayed", "~", ChatFormatting.YELLOW, 0xFFD9B12A),
    /** On their feet and able to carry their own bandage. */
    MINOR("minor", "-", ChatFormatting.GREEN, 0xFF4E9E3E),
    /** Nothing anybody here is carrying would change this. */
    EXPECTANT("expectant", "x", ChatFormatting.DARK_GRAY, 0xFF3A3A42);

    public static final TriageTag[] VALUES = values();
    public static final Codec<TriageTag> CODEC = StringRepresentable.fromEnum(TriageTag::values);

    private final String id;
    private final String symbol;
    private final ChatFormatting colour;
    private final int argb;

    TriageTag(String id, String symbol, ChatFormatting colour, int argb) {
        this.id = id;
        this.symbol = symbol;
        this.colour = colour;
        this.argb = argb;
    }

    public String id() {
        return id;
    }

    public Component title() {
        return Component.translatable("triage.tag." + id).withStyle(colour);
    }

    public ChatFormatting colour() {
        return colour;
    }

    /** The same colour as a fill, for the swatches on the body screen. */
    public int argb() {
        return argb;
    }

    /**
     * The mark itself, as it is worn: one character in brackets, in the tag's colour.
     *
     * <p>Deliberately plain ASCII. This is drawn on a nameplate in whatever font the
     * player is using, and a symbol that renders as a box in half of them would be worse
     * than no symbol at all.
     */
    public Component mark() {
        return Component.literal("[" + symbol + "]").withStyle(colour);
    }

    public static @Nullable TriageTag byId(String id) {
        for (TriageTag tag : VALUES) {
            if (tag.id.equals(id.toLowerCase(Locale.ROOT))) {
                return tag;
            }
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
