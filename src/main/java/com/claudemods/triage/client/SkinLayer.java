package com.claudemods.triage.client;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.LimbMark;
import com.claudemods.triage.body.PartState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The two things that happen to skin, drawn on the skin.
 *
 * <p>Everything this mod lets a body say out loud is either tied on - a dressing, a strap -
 * or missing, or running down a leg. The two that are the skin itself were the two nobody
 * could see, and both of them are the sort of thing you notice from further away than any
 * of the others.
 *
 * <p><b>A burn.</b> The mod spent a whole version making burns real - three depths, a window
 * in which they get worse, a surface that decides what happens to the person - and then drew
 * them nowhere at all. A burn is on the outside of somebody by definition. It has a colour,
 * and the difference between the depths is a difference in <em>how it looks</em>, which is
 * the strongest case any of these has ever had for being on the model.
 *
 * <p><b>A limb that has died on the body.</b> The mark has always said there or gone and
 * never said the state in between - and that is the one with the decision in it. A limb that
 * is gone asks nothing of anybody. A limb that is dying asks for a saw today, and it went
 * unseen while the two states either side of it were obvious from across a field. A strap
 * that has been on too long is already visible; this is what that strap did.
 */
public final class SkinLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation BURN = TriageMod.id("textures/entity/burn.png");
    /** Dead flesh has no texture of its own: it is skin, and skin is what is under it. */
    private static final ResourceLocation CLOTH =
            new ResourceLocation("textures/block/white_wool.png");

    /** Red, blistered, and through. The third is the darkest thing the mod draws on a body. */
    private static final int SHALLOW = 0xFFD8674F;
    private static final int BLISTERED = 0xFFB4552F;
    private static final int THROUGH = 0xFF4A3A33;
    /** And what a limb goes when it stops being fed. */
    private static final int NECROTIC = 0xC0403A33;

    /**
     * The colour of a person, which is drawn over all of them rather than on a part.
     *
     * <p>Everything else in this file is local - this burn, that limb - because everything
     * else in this file happened to a place. These two happened to the whole person, and
     * that is exactly what makes them readable at a distance: a white arm is a trick of the
     * light and a white <em>body</em> is a casualty.
     *
     * <p>Low alphas on purpose. What is wanted is the thing you notice without being able
     * to say what you noticed, because that is how it works at the door of a room.
     */
    /** Steel, and the thing somebody was holding it by. */
    private static final int STEEL = 0xFFB4B8C0;
    private static final int GRIP = 0xFF4A3628;

    private static final int[] PALE = {0, 0x14E8E4DC, 0x2CF0ECE2, 0x4CFFFCF4};
    private static final int[] BLUE = {0, 0x1C4E6E9E, 0x38375C90, 0x582A4880};

    /**
     * A limb with the wrong shape in it.
     *
     * <p>Drawn as a lump rather than by bending the model, and that is a real limit
     * honestly worked around: the player model is posed by vanilla every frame and a
     * layer draws after it, so there is no point at which this mod can put an elbow
     * somewhere an elbow is not. What it can do is put the thing you would actually see
     * on the outside of the limb - a shoulder of bone standing where no bone should be -
     * and that reads at exactly the distance it needs to.
     */
    private static final int LUMP = 0xE0A87C5E;
    private static final int LUMP_OLD = 0xC08A6A52;
    private static final int BONE_OUT = 0xFFEFE7D2;
    private static final int BONE_BLOOD = 0xE08E1616;

    /**
     * Slightly proud of the skin, so it never fights the model for the same pixels.
     *
     * <p>The burn texture is a patch with nothing round it, so each face shows one blotch
     * rather than a painted box - which is what a burn is, and why it needs no shape of
     * its own beyond the part it is on.
     */
    private final ModelPart head = skin(-4.0F, -8.0F, -4.0F, 8, 8, 8, 0.26F);
    private final ModelPart chest = skin(-4.0F, 0.0F, -2.0F, 8, 6, 4, 0.26F);
    private final ModelPart belly = skin(-4.0F, 6.0F, -2.0F, 8, 6, 4, 0.26F);
    private final ModelPart leftArm = skin(-1.0F, -2.0F, -2.0F, 4, 12, 4, 0.26F);
    private final ModelPart rightArm = skin(-3.0F, -2.0F, -2.0F, 4, 12, 4, 0.26F);
    private final ModelPart leg = skin(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.26F);
    /** A dead limb is the whole limb, so this one is a solid sleeve rather than a patch. */
    /** The lump itself: short, wide and off to one side of the limb. */
    private final ModelPart lumpLeft = skin(0.0F, 3.0F, -2.0F, 4, 4, 4, 0.55F);
    private final ModelPart lumpRight = skin(-4.0F, 3.0F, -2.0F, 4, 4, 4, 0.55F);
    private final ModelPart lumpLeg = skin(-2.0F, 5.0F, -2.0F, 4, 4, 4, 0.55F);
    /** And what comes through it when the break is open. */
    private final ModelPart spikeLeft = skin(2.5F, 4.0F, -1.0F, 2, 2, 2, 0.3F);
    private final ModelPart spikeRight = skin(-4.5F, 4.0F, -1.0F, 2, 2, 2, 0.3F);
    private final ModelPart spikeLeg = skin(1.5F, 6.0F, -1.0F, 2, 2, 2, 0.3F);

    /**
     * A weapon standing out of a part, in two pieces: what went in and what did not.
     *
     * <p>Built per part rather than once, because a blade in a head and a blade in a shin
     * are at different heights and a single piece of geometry would put one of them in
     * mid-air. Seven of each, indexed the way everything else in the mod indexes parts.
     */
    private final ModelPart[] steel = new ModelPart[BodyPart.VALUES.length];
    private final ModelPart[] grips = new ModelPart[BodyPart.VALUES.length];

    private final ModelPart deadLeft = skin(-1.0F, -2.0F, -2.0F, 4, 12, 4, 0.3F);
    private final ModelPart deadRight = skin(-3.0F, -2.0F, -2.0F, 4, 12, 4, 0.3F);
    private final ModelPart deadLeg = skin(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.3F);

    public SkinLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
        for (BodyPart part : BodyPart.VALUES) {
            float y = switch (part) {
                case HEAD -> -5.0F;
                case THORAX -> 2.0F;
                case STOMACH -> 8.0F;
                case LEFT_ARM, RIGHT_ARM -> 3.0F;
                default -> 5.0F;
            };
            // Off-centre, so it is never hidden by the exact middle of a limb, and long
            // enough to be read from further away than the wound under it ever could be.
            float x = part == BodyPart.THORAX || part == BodyPart.STOMACH || part == BodyPart.HEAD
                    ? 1.5F : -0.5F;
            steel[part.ordinal()] = bar(x, y, -7.0F, 1, 2, 6);
            grips[part.ordinal()] = bar(x - 0.5F, y - 0.5F, -9.5F, 2, 3, 3);
        }
    }

    /** One box, no growth: this is an object rather than something on the skin. */
    private static ModelPart bar(float x, float y, float z, int w, int h, int d) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("bar", CubeListBuilder.create()
                .texOffs(0, 0).addBox(x, y, z, w, h, d), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("bar");
    }

    private static ModelPart skin(float x, float y, float z, int w, int h, int d, float grow) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("skin", CubeListBuilder.create()
                .texOffs(0, 0).addBox(x, y, z, w, h, d, new net.minecraft.client.model.geom.builders.CubeDeformation(grow)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("skin");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        int burns = ClientTags.burns(player.getId());
        int dying = ClientTags.dying(player.getId());
        int bones = ClientTags.bones(player.getId());
        // Under a shader pack the translucent whole-body wash can turn the player and
        // every other casualty semi-transparent. The local marks - burns, bones, dead
        // limbs, dressings - are small enough to survive the same compositing, so only
        // the full-body tint is dropped.
        boolean tinted = !IrisCompat.shadersActive();
        int pale = tinted ? ClientTags.pale(player.getId()) : 0;
        int blue = tinted ? ClientTags.blue(player.getId()) : 0;
        int blades = ClientTags.blades(player.getId());
        if ((burns == 0 && dying == 0 && bones == 0 && pale == 0 && blue == 0 && blades == 0)
                || player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        int limbs = Limbs.of(player);

        // Dead first, burnt over the top of it: a limb can be both, and what a burn looks
        // like on dead flesh is a burn.
        rot(dying, BodyPart.LEFT_ARM, model.leftArm, deadLeft, poseStack, buffer, light, limbs);
        rot(dying, BodyPart.RIGHT_ARM, model.rightArm, deadRight, poseStack, buffer, light, limbs);
        rot(dying, BodyPart.LEFT_LEG, model.leftLeg, deadLeg, poseStack, buffer, light, limbs);
        rot(dying, BodyPart.RIGHT_LEG, model.rightLeg, deadLeg, poseStack, buffer, light, limbs);

        // Under the burns and over nothing: this is the skin, so anything that happened
        // to the skin is on top of it.
        if (pale > 0) {
            colour(PALE[pale], model, poseStack, buffer, light, limbs);
        }
        if (blue > 0) {
            colour(BLUE[blue], model, poseStack, buffer, light, limbs);
        }

        scorch(burns, BodyPart.HEAD, model.head, head, poseStack, buffer, light, limbs);
        scorch(burns, BodyPart.THORAX, model.body, chest, poseStack, buffer, light, limbs);
        scorch(burns, BodyPart.STOMACH, model.body, belly, poseStack, buffer, light, limbs);
        scorch(burns, BodyPart.LEFT_ARM, model.leftArm, leftArm, poseStack, buffer, light, limbs);
        scorch(burns, BodyPart.RIGHT_ARM, model.rightArm, rightArm, poseStack, buffer, light, limbs);
        scorch(burns, BodyPart.LEFT_LEG, model.leftLeg, leg, poseStack, buffer, light, limbs);
        scorch(burns, BodyPart.RIGHT_LEG, model.rightLeg, leg, poseStack, buffer, light, limbs);

        // Over everything, because it is not on the person - it is in them, and it is the
        // one thing this layer draws that is somebody else's property.
        if (blades != 0) {
            for (BodyPart part : BodyPart.VALUES) {
                if (LimbMark.bladeIn(blades, part)) {
                    weapon(part, model, poseStack, buffer, light, limbs);
                }
            }
        }

        // Last, over everything: the shape of the bone is the loudest thing a limb can
        // say, and a burn on top of a deformity does not hide the deformity.
        shape(bones, BodyPart.LEFT_ARM, model.leftArm, lumpLeft, spikeLeft, poseStack, buffer, light, limbs);
        shape(bones, BodyPart.RIGHT_ARM, model.rightArm, lumpRight, spikeRight, poseStack, buffer, light, limbs);
        shape(bones, BodyPart.LEFT_LEG, model.leftLeg, lumpLeg, spikeLeg, poseStack, buffer, light, limbs);
        shape(bones, BodyPart.RIGHT_LEG, model.rightLeg, lumpLeg, spikeLeg, poseStack, buffer, light, limbs);
    }

    /**
     * One limb that is not the shape a limb is.
     *
     * <p>Three answers and three pictures: a pale old lump for a bone that healed out of
     * line, a fresh angry one for a bone that is out of line now, and for the worst of
     * them a piece of white standing out of the skin with blood under it.
     */
    private static void shape(int bits, BodyPart part, ModelPart joint, ModelPart lump, ModelPart spike,
                              PoseStack poseStack, MultiBufferSource buffer, int light, int limbs) {
        int shape = LimbMark.bone(bits, part);
        if (shape == LimbMark.BONE_FINE || LimbMark.state(limbs, part) != LimbMark.THERE) {
            return;
        }
        poseStack.pushPose();
        joint.translateAndRotate(poseStack);
        Tint.render(lump, poseStack, buffer, CLOTH, true, light,
                shape == LimbMark.BONE_CROOKED ? LUMP_OLD : LUMP);
        if (shape == LimbMark.BONE_OPEN) {
            Tint.render(spike, poseStack, buffer, CLOTH, true, light, BONE_OUT);
            Tint.render(lump, poseStack, buffer, BURN, true, light, BONE_BLOOD);
        }
        poseStack.popPose();
    }

    /** The object, on the part it went into. */
    private void weapon(BodyPart part, PlayerModel<AbstractClientPlayer> model, PoseStack poseStack,
                        MultiBufferSource buffer, int light, int limbs) {
        if (LimbMark.state(limbs, part) != LimbMark.THERE) {
            return;
        }
        ModelPart joint = switch (part) {
            case HEAD -> model.head;
            case THORAX, STOMACH -> model.body;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_LEG -> model.leftLeg;
            default -> model.rightLeg;
        };
        ModelPart bar = steel[part.ordinal()];
        ModelPart grip = grips[part.ordinal()];
        poseStack.pushPose();
        joint.translateAndRotate(poseStack);
        Tint.render(bar, poseStack, buffer, CLOTH, true, light, STEEL);
        Tint.render(grip, poseStack, buffer, CLOTH, true, light, GRIP);
        poseStack.popPose();
    }

    /**
     * All of somebody, one colour.
     *
     * <p>Every part that is still there, including the ones a dressing is over - a bandage
     * covers a wound and not a face, and what this is drawing is the person rather than the
     * skin under the cloth.
     */
    private void colour(int tint, PlayerModel<AbstractClientPlayer> model, PoseStack poseStack,
                        MultiBufferSource buffer, int light, int limbs) {
        wash(tint, BodyPart.HEAD, model.head, head, poseStack, buffer, light, limbs);
        wash(tint, BodyPart.THORAX, model.body, chest, poseStack, buffer, light, limbs);
        wash(tint, BodyPart.STOMACH, model.body, belly, poseStack, buffer, light, limbs);
        wash(tint, BodyPart.LEFT_ARM, model.leftArm, leftArm, poseStack, buffer, light, limbs);
        wash(tint, BodyPart.RIGHT_ARM, model.rightArm, rightArm, poseStack, buffer, light, limbs);
        wash(tint, BodyPart.LEFT_LEG, model.leftLeg, leg, poseStack, buffer, light, limbs);
        wash(tint, BodyPart.RIGHT_LEG, model.rightLeg, leg, poseStack, buffer, light, limbs);
    }

    private static void wash(int tint, BodyPart part, ModelPart joint, ModelPart piece,
                             PoseStack poseStack, MultiBufferSource buffer, int light, int limbs) {
        if (LimbMark.state(limbs, part) != LimbMark.THERE) {
            return;
        }
        poseStack.pushPose();
        joint.translateAndRotate(poseStack);
        Tint.render(piece, poseStack, buffer, CLOTH, true, light, tint);
        poseStack.popPose();
    }

    /** One burn, on the part it is on, in the colour of how deep it went. */
    private static void scorch(int bits, BodyPart part, ModelPart joint, ModelPart piece,
                               PoseStack poseStack, MultiBufferSource buffer, int light, int limbs) {
        int depth = LimbMark.burn(bits, part);
        if (depth == PartState.BURN_NONE || LimbMark.state(limbs, part) != LimbMark.THERE) {
            return;
        }
        int colour = switch (depth) {
            case PartState.BURN_SUPERFICIAL -> SHALLOW;
            case PartState.BURN_FULL -> THROUGH;
            default -> BLISTERED;
        };
        poseStack.pushPose();
        joint.translateAndRotate(poseStack);
        Tint.render(piece, poseStack, buffer, BURN, true, light, colour);
        poseStack.popPose();
    }

    /** And a limb nobody has taken off yet. */
    private static void rot(int bits, BodyPart part, ModelPart joint, ModelPart piece,
                            PoseStack poseStack, MultiBufferSource buffer, int light, int limbs) {
        if (!LimbMark.dying(bits, part) || LimbMark.state(limbs, part) != LimbMark.THERE) {
            return;
        }
        poseStack.pushPose();
        joint.translateAndRotate(poseStack);
        Tint.render(piece, poseStack, buffer, CLOTH, true, light, NECROTIC);
        poseStack.popPose();
    }
}
