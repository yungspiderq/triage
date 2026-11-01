package com.claudemods.triage.client;

import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.LimbMark;
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
import com.claudemods.triage.TriageMod;
import net.minecraft.resources.ResourceLocation;

/**
 * The bandage, on the arm it was put on.
 *
 * <p>A dressing became a thing with a clock on it and stayed invisible, which made the
 * commonest treatment in the mod the only one nobody could see. That is exactly backwards:
 * a bandage is white, it is on somebody, and whether it has gone through is the one thing
 * about another player's treatment that needs no walking over, no screen and no
 * permission. It is the third thing a body says about itself out loud, after the mark
 * pinned to its chest and the arm that is not there.
 *
 * <p>Seven places, one band each, hung off the same joints the limbs are: the head, two
 * round the trunk for the chest and the belly, the forearms and the shins. Clean it is
 * cream. Gone through it is the colour of a dressing nobody has changed, and that is the
 * whole of the warning - somebody looks at you and knows before you do.
 *
 * <p>It used to be a box of white wool tinted flat, which reads as a painted cube rather
 * than as cloth wound round an arm. What a wrap looks like is <b>turns</b>: overlapping
 * edges running at an angle, each a shade off its neighbour. And what has come through one
 * is not a colour change, it is a <b>blot</b> - one thing, in one place, where the wound is -
 * so the soaked band is its own skin rather than the same skin in a browner tint. Both of
 * those are four lines of drawing and they are the whole difference between a bandage and a
 * cube.
 */
public final class DressingLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Turns of cloth, and the same cloth with what came through it on the front. */
    private static final ResourceLocation WRAP = TriageMod.id("textures/entity/dressing.png");
    private static final ResourceLocation SOAKED = TriageMod.id("textures/entity/dressing_soaked.png");
    /** A strap is not a dressing and does not want a weave: it is one turn, pulled tight. */
    private static final ResourceLocation CLOTH =
            ResourceLocation.withDefaultNamespace("textures/block/white_wool.png");

    /** Cream while it is doing its job, and the colour of old cloth once it has stopped. */
    private static final int CLEAN = 0xFFF2ECE0;
    private static final int SOILED = 0xFFD8CCB0;
    /** And a strap, which is darker cloth and gets darker as its time goes. */
    private static final int STRAP = 0xFF7A5230;
    private static final int STRAP_LATE = 0xFF3E2718;

    private final ModelPart head = band(-4.6F, -6.8F, -4.6F, 9.2F, 3.2F, 9.2F);
    private final ModelPart chest = band(-4.6F, 0.8F, -2.6F, 9.2F, 4.0F, 5.2F);
    private final ModelPart belly = band(-4.6F, 7.0F, -2.6F, 9.2F, 4.0F, 5.2F);
    private final ModelPart leftArm = band(-1.6F, 4.0F, -2.6F, 5.2F, 4.0F, 5.2F);
    private final ModelPart rightArm = band(-3.6F, 4.0F, -2.6F, 5.2F, 4.0F, 5.2F);
    private final ModelPart leg = band(-2.6F, 4.0F, -2.6F, 5.2F, 4.0F, 5.2F);
    // Higher up, and thinner: a strap is a strap and not a dressing.
    private final ModelPart leftCuff = band(-1.7F, 0.6F, -2.7F, 5.4F, 2.6F, 5.4F);
    private final ModelPart rightCuff = band(-3.7F, 0.6F, -2.7F, 5.4F, 2.6F, 5.4F);
    private final ModelPart cuff = band(-2.7F, 0.6F, -2.7F, 5.4F, 2.6F, 5.4F);

    public DressingLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /**
     * One turn of cloth as a box, sized and placed where it goes.
     *
     * <p>Baked into the box rather than translated at draw time, because the numbers are
     * in model units either way and a box that already knows where it lives is a box
     * nobody has to remember to move.
     */
    private static ModelPart band(float x, float y, float z, float w, float h, float d) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("band",
                CubeListBuilder.create().texOffs(0, 0).addBox(x, y, z, w, h, d), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("band");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        int bits = ClientTags.dressings(player.getId());
        if ((bits == 0 && ClientTags.straps(player.getId()) == 0) || player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        int limbs = Limbs.of(player);

        tie(bits, BodyPart.HEAD, model.head, head, poseStack, buffer, light, limbs);
        tie(bits, BodyPart.THORAX, model.body, chest, poseStack, buffer, light, limbs);
        tie(bits, BodyPart.STOMACH, model.body, belly, poseStack, buffer, light, limbs);
        tie(bits, BodyPart.LEFT_ARM, model.leftArm, leftArm, poseStack, buffer, light, limbs);
        tie(bits, BodyPart.RIGHT_ARM, model.rightArm, rightArm, poseStack, buffer, light, limbs);
        tie(bits, BodyPart.LEFT_LEG, model.leftLeg, leg, poseStack, buffer, light, limbs);
        tie(bits, BodyPart.RIGHT_LEG, model.rightLeg, leg, poseStack, buffer, light, limbs);

        // Straps go higher up the limb than dressings do, because that is where a strap
        // goes: above the wound, between it and the heart.
        int straps = ClientTags.straps(player.getId());
        strap(straps, BodyPart.LEFT_ARM, model.leftArm, leftCuff, poseStack, buffer, light, limbs);
        strap(straps, BodyPart.RIGHT_ARM, model.rightArm, rightCuff, poseStack, buffer, light, limbs);
        strap(straps, BodyPart.LEFT_LEG, model.leftLeg, cuff, poseStack, buffer, light, limbs);
        strap(straps, BodyPart.RIGHT_LEG, model.rightLeg, cuff, poseStack, buffer, light, limbs);
    }

    /**
     * A strap, and how long it has been on.
     *
     * <p>The colour is the whole of it. A tourniquet is a decision with a clock, and the
     * clock could only be read by walking over and opening somebody's chart - which is
     * the wrong place for it, because the person wearing one may be in no state to be
     * asked. It darkens as the time goes, and that is visible from the doorway.
     */
    private static void strap(int bits, BodyPart part, ModelPart joint, ModelPart piece,
                              PoseStack poseStack, MultiBufferSource buffer, int light, int limbs) {
        int state = LimbMark.strap(bits, part);
        if (state == LimbMark.LOOSE || LimbMark.state(limbs, part) != LimbMark.THERE) {
            return;
        }
        poseStack.pushPose();
        joint.translateAndRotate(poseStack);
        piece.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(CLOTH)), light,
                OverlayTexture.NO_OVERLAY, state == LimbMark.LATE ? STRAP_LATE : STRAP);
        poseStack.popPose();
    }

    /**
     * One band, on the joint the part hangs off.
     *
     * <p>Not on a limb that is not there. A bandage floating where an arm used to be is
     * the same class of mistake as an iron sleeve in the same place, and this mod has
     * made it once already.
     */
    private static void tie(int bits, BodyPart part, ModelPart joint, ModelPart piece,
                            PoseStack poseStack, MultiBufferSource buffer, int light, int limbs) {
        int state = LimbMark.dressing(bits, part);
        if (state == LimbMark.BARE || LimbMark.state(limbs, part) != LimbMark.THERE) {
            return;
        }
        boolean through = state == LimbMark.SOILED;
        poseStack.pushPose();
        joint.translateAndRotate(poseStack);
        piece.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(through ? SOAKED : WRAP)),
                light, OverlayTexture.NO_OVERLAY, through ? SOILED : CLEAN);
        poseStack.popPose();
    }
}
