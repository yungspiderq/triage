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
import net.minecraft.resources.ResourceLocation;

/**
 * The thing screwed on where the limb was.
 *
 * <p>A layer rather than anything cleverer, for the reason vanilla hangs held items off
 * one: inside a layer the pose stack is already in model space, so the peg can simply be
 * drawn at the hip the model was going to draw the leg from - and it swings with the
 * walk for free, because it is hanging off the same joint.
 *
 * <p>Both are built out of boxes here rather than shipped as models, and both borrow a
 * block texture: planks for the leg, iron for the hook. A wooden leg made out of oak
 * planks is not a compromise. It is what one is made out of.
 */
public final class ProstheticLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation WOOD =
            ResourceLocation.withDefaultNamespace("textures/block/oak_planks.png");
    private static final ResourceLocation IRON =
            ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    /** The second tier is iron and brass rather than a plank and a bent bar. */
    private static final int BRASS = 0xFFBA944E;

    private final ModelPart peg = buildPeg();
    private final ModelPart hook = buildHook();
    private final ModelPart jointed = buildJointed();
    private final ModelPart knee = buildKnee();
    private final ModelPart hand = buildHand();

    public ProstheticLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /**
     * A cup at the top and a post under it, the length of a leg.
     *
     * <p>Deliberately thinner than the leg it replaces. Half the reason to look at
     * somebody and know is that the shape is wrong, and a peg the same width as a leg is
     * a leg painted brown.
     */
    private static ModelPart buildPeg() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("peg", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.2F, 0.0F, -2.2F, 4.4F, 3.0F, 4.4F)
                        .texOffs(0, 0).addBox(-1.5F, 3.0F, -1.5F, 3.0F, 7.0F, 3.0F)
                        .texOffs(0, 0).addBox(-1.0F, 10.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("peg");
    }

    /**
     * A cup, a rod, and something bent on the end of it that is not a hand.
     *
     * <p>The bend goes towards <em>-Z</em>, which is the way the model faces. A hook
     * curled behind the elbow is a hook nobody could put anything on, and it was the
     * first thing anybody said about it.
     */
    private static ModelPart buildHook() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("hook", CubeListBuilder.create()
                        // The cup over what is left of the arm.
                        .texOffs(0, 0).addBox(-1.7F, -2.0F, -1.7F, 3.4F, 3.5F, 3.4F)
                        // The rod down to where a wrist would be.
                        .texOffs(0, 0).addBox(-0.7F, 1.5F, -0.7F, 1.4F, 6.5F, 1.4F)
                        // The hook itself: forward along the bottom, then back up.
                        .texOffs(0, 0).addBox(-0.7F, 8.0F, -3.6F, 1.4F, 1.4F, 3.6F)
                        .texOffs(0, 0).addBox(-0.7F, 6.2F, -3.6F, 1.4F, 1.8F, 1.4F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("hook");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        int bits = Limbs.of(player);
        if (bits == 0 || player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        arm(bits, BodyPart.LEFT_ARM, model.leftArm, poseStack, buffer, light);
        arm(bits, BodyPart.RIGHT_ARM, model.rightArm, poseStack, buffer, light);
        leg(bits, BodyPart.LEFT_LEG, model.leftLeg, poseStack, buffer, light);
        leg(bits, BodyPart.RIGHT_LEG, model.rightLeg, poseStack, buffer, light);
    }

    /**
     * A peg, or a leg with a knee in it.
     *
     * <p>Two silhouettes rather than one recoloured, because the difference between them
     * is the point of the second one: a peg is a stick that meets the ground and a jointed
     * one bends where a leg bends. Somebody looking at you should be able to tell which
     * you are walking on.
     */
    private void leg(int bits, BodyPart part, ModelPart joint, PoseStack poseStack,
                     MultiBufferSource buffer, int light) {
        int state = LimbMark.state(bits, part);
        if (state == LimbMark.FITTED) {
            fit(joint, peg, WOOD, 0xFFFFFFFF, poseStack, buffer, light);
        } else if (state == LimbMark.FITTED_FINE) {
            fit(joint, jointed, IRON, 0xFFFFFFFF, poseStack, buffer, light);
            fit(joint, knee, IRON, BRASS, poseStack, buffer, light);
        }
    }

    /** A hook, or a hand that opens. */
    private void arm(int bits, BodyPart part, ModelPart joint, PoseStack poseStack,
                     MultiBufferSource buffer, int light) {
        int state = LimbMark.state(bits, part);
        if (state == LimbMark.FITTED) {
            fit(joint, hook, IRON, 0xFFFFFFFF, poseStack, buffer, light);
        } else if (state == LimbMark.FITTED_FINE) {
            fit(joint, hand, IRON, 0xFFFFFFFF, poseStack, buffer, light);
        }
    }

    /**
     * Draws one, hanging off the joint the limb would have hung off.
     *
     * <p>Only when there is one to draw. A stump with nothing fitted gets nothing at all
     * - the gap is the whole of what it has to say, and putting a stub of something on
     * the end of it would soften the one thing in the mod that is not supposed to soften.
     */
    private static void fit(ModelPart joint, ModelPart piece, ResourceLocation texture, int colour,
                            PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        joint.translateAndRotate(poseStack);
        piece.render(poseStack, buffer.getBuffer(RenderType.entitySolid(texture)), light,
                OverlayTexture.NO_OVERLAY, colour);
        poseStack.popPose();
    }

    /**
     * A leg in two pieces with a gap where the knee is.
     *
     * <p>Drawn as thigh and shin rather than one post, so that the brass band between
     * them reads as a joint and not as a stripe.
     */
    private static ModelPart buildJointed() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("leg", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F)
                        .texOffs(0, 0).addBox(-1.4F, 3.0F, -1.4F, 2.8F, 3.5F, 2.8F)
                        .texOffs(0, 0).addBox(-1.4F, 8.5F, -1.4F, 2.8F, 3.0F, 2.8F)
                        .texOffs(0, 0).addBox(-2.2F, 11.5F, -2.6F, 4.4F, 1.4F, 5.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("leg");
    }

    /** The knee itself, which is the whole of the difference and is drawn in brass. */
    private static ModelPart buildKnee() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("knee", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.9F, 6.4F, -1.9F, 3.8F, 2.2F, 3.8F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("knee");
    }

    /** A hand with fingers, rather than a bar bent into a J. */
    private static ModelPart buildHand() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("hand", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.8F, -2.0F, -1.8F, 3.6F, 3.6F, 3.6F)
                        .texOffs(0, 0).addBox(-0.8F, 1.6F, -0.8F, 1.6F, 5.4F, 1.6F)
                        .texOffs(0, 0).addBox(-1.8F, 7.0F, -1.8F, 3.6F, 2.4F, 3.6F)
                        // Three fingers and a thumb, which is what tells it from a hook.
                        .texOffs(0, 0).addBox(-1.6F, 9.4F, -1.6F, 1.0F, 2.6F, 1.0F)
                        .texOffs(0, 0).addBox(-0.3F, 9.4F, -1.6F, 1.0F, 3.0F, 1.0F)
                        .texOffs(0, 0).addBox(0.9F, 9.4F, -1.6F, 1.0F, 2.6F, 1.0F)
                        .texOffs(0, 0).addBox(-2.4F, 8.6F, -0.5F, 1.0F, 2.4F, 1.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("hand");
    }
}
