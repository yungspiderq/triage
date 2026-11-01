package com.claudemods.triage.body;

import com.claudemods.triage.Config;
import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * The seven parts a player is made of. Two of them are vital: empty a vital part and
 * the player dies, no matter how healthy the rest of the body is. The other five only
 * "black out" - they stop working and start dragging the rest of the body down with
 * them, which is the whole point of the mod.
 */
public enum BodyPart implements StringRepresentable {
    HEAD("head", true),
    THORAX("thorax", true),
    STOMACH("stomach", false),
    LEFT_ARM("left_arm", false),
    RIGHT_ARM("right_arm", false),
    LEFT_LEG("left_leg", false),
    RIGHT_LEG("right_leg", false);

    public static final BodyPart[] VALUES = values();
    public static final Codec<BodyPart> CODEC = StringRepresentable.fromEnum(BodyPart::values);
    /** For the packets that name a limb: which one a pair of hands is going on. */

    private final String id;
    private final boolean vital;

    BodyPart(String id, boolean vital) {
        this.id = id;
        this.vital = vital;
    }

    public String id() {
        return id;
    }

    /** True when emptying this part kills the player outright. */
    public boolean vital() {
        return vital;
    }

    public boolean isArm() {
        return this == LEFT_ARM || this == RIGHT_ARM;
    }

    public boolean isLeg() {
        return this == LEFT_LEG || this == RIGHT_LEG;
    }

    /**
     * Whether this is a thing a body can be without.
     *
     * <p>Four of the seven. The stomach is not on the list even though it is not vital
     * either - a body without one is not a body somebody keeps playing, and the mod has
     * no answer for it that is not just a slower death.
     */
    public boolean limb() {
        return isArm() || isLeg();
    }

    /**
     * How much of a person this is, as a percentage of the skin.
     *
     * <p>The rule of nines, which is how anybody without a chart works out how big a
     * burn is: head nine, each arm nine, each leg eighteen, and the trunk thirty-six
     * split between the chest and the belly. It is the oldest bedside arithmetic in
     * trauma and it exists because how much of somebody is burnt decides what happens
     * to them far more than how deep any one patch of it went.
     *
     * <p>It lands on these seven parts almost exactly - ninety-nine of a hundred, the
     * missing one being the part of the original rule this mod does not model - which is
     * not luck. Both lists are a body divided the way a body divides.
     */
    public int surface() {
        return switch (this) {
            case HEAD -> 9;
            case THORAX -> 18;
            case STOMACH -> 18;
            case LEFT_ARM, RIGHT_ARM -> 9;
            case LEFT_LEG, RIGHT_LEG -> 18;
        };
    }

    /** What is under this part, and therefore how breaking it behaves. */
    public Bone bone() {
        return switch (this) {
            case HEAD -> Bone.SKULL;
            case THORAX -> Bone.RIBS;
            case STOMACH -> Bone.SPINE;
            case LEFT_ARM, RIGHT_ARM -> Bone.FOREARM;
            case LEFT_LEG, RIGHT_LEG -> Bone.FEMUR;
        };
    }

    public Component title() {
        return Component.translatable("triage.part." + id);
    }

    /** Maximum health of this part, as the server config has it. */
    public float maxHealth() {
        Config.Values values = Config.values();
        return switch (this) {
            case HEAD -> values.headMax;
            case THORAX -> values.thoraxMax;
            case STOMACH -> values.stomachMax;
            case LEFT_ARM, RIGHT_ARM -> values.armMax;
            case LEFT_LEG, RIGHT_LEG -> values.legMax;
        };
    }

    /** Relative chance of a plain physical hit landing here. */
    public int hitWeight() {
        Config.Values values = Config.values();
        return switch (this) {
            case HEAD -> values.headWeight;
            case THORAX -> values.thoraxWeight;
            case STOMACH -> values.stomachWeight;
            case LEFT_ARM, RIGHT_ARM -> values.armWeight;
            case LEFT_LEG, RIGHT_LEG -> values.legWeight;
        };
    }

    public static @Nullable BodyPart byId(String id) {
        for (BodyPart part : VALUES) {
            if (part.id.equals(id.toLowerCase(Locale.ROOT))) {
                return part;
            }
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
