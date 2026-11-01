package com.claudemods.triage.client;

import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.LimbMark;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
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
 * The stick, on the outside, where a stick is.
 *
 * <p>A splint used to be an event: the item was spent, the bone was set, and nothing about
 * the body changed. It is a thing now, and a thing that is tied to somebody is a thing other
 * people can see - which makes this the first mark in the mod that is <b>good news</b>.
 * Every other thing drawn on a body here says something is wrong with it. This one says
 * somebody has already been, which is exactly what a medic walking up on a stranger wants
 * to know before anything else.
 *
 * <p>Two boards and two ties, and deliberately crude: this is not a cast from a hospital,
 * it is what anybody makes out of what they have, and it should look like it.
 */
public final class SplintLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Plain white, tinted: a board and a strip of cloth are shapes rather than textures. */
    private static final ResourceLocation PLAIN =
            ResourceLocation.withDefaultNamespace("textures/block/white_wool.png");

    private static final int WOOD = 0xFFA57F4A;
    private static final int TIE = 0xFFD8CFB4;

    /** The boards: thin, flat, and standing off the limb on either side. */
    private final ModelPart leftArmBoards = boards(1.0F, -2.0F, 4.0F);
    private final ModelPart rightArmBoards = boards(-3.0F, -2.0F, 4.0F);
    private final ModelPart legBoards = boards(-2.0F, 2.0F, 4.0F);
    /** And the ties holding them on, which is what makes it a splint rather than a stick. */
    private final ModelPart leftArmTies = ties(-1.0F, -2.0F);
    private final ModelPart rightArmTies = ties(-3.0F, -2.0F);
    private final ModelPart legTies = ties(-2.0F, 2.0F);

    public SplintLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /**
     * Two boards down the sides of a four-wide limb.
     *
     * @param x    the near edge of the limb in model space
     * @param y    where the splint starts down the limb
     * @param span how wide the limb is
     */
    private static ModelPart boards(float x, float y, float span) {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder cubes = CubeListBuilder.create()
                .texOffs(0, 0).addBox(x - 1.2F, y, -2.0F, 1, 9, 4, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(x + span + 0.2F, y, -2.0F, 1, 9, 4, new CubeDeformation(0.0F));
        mesh.getRoot().addOrReplaceChild("splint", cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("splint");
    }

    /** Two bands round the lot, near each end of the boards. */
    private static ModelPart ties(float x, float y) {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder cubes = CubeListBuilder.create()
                .texOffs(0, 0).addBox(x, y + 0.6F, -2.0F, 4, 1, 4, new CubeDeformation(0.62F))
                .texOffs(0, 0).addBox(x, y + 6.4F, -2.0F, 4, 1, 4, new CubeDeformation(0.62F));
        mesh.getRoot().addOrReplaceChild("ties", cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("ties");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        int splints = ClientTags.splints(player.getId());
        if (splints == 0 || player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        int limbs = Limbs.of(player);

        tie(splints, limbs, BodyPart.LEFT_ARM, model.leftArm, leftArmBoards, leftArmTies,
                poseStack, buffer, light);
        tie(splints, limbs, BodyPart.RIGHT_ARM, model.rightArm, rightArmBoards, rightArmTies,
                poseStack, buffer, light);
        tie(splints, limbs, BodyPart.LEFT_LEG, model.leftLeg, legBoards, legTies,
                poseStack, buffer, light);
        tie(splints, limbs, BodyPart.RIGHT_LEG, model.rightLeg, legBoards, legTies,
                poseStack, buffer, light);
        // Nothing on a chest and nothing on a head. Ribs are strapped rather than
        // splinted and a skull is neither, so there is nothing to draw and the mod is
        // not going to invent a picture for a treatment it refuses.
    }

    private void tie(int splints, int limbs, BodyPart part, ModelPart joint, ModelPart boards,
                     ModelPart bands, PoseStack poseStack, MultiBufferSource buffer, int light) {
        // A splint on a limb that is not there would be two boards in mid-air.
        if (!LimbMark.splinted(splints, part) || LimbMark.state(limbs, part) != LimbMark.THERE) {
            return;
        }
        poseStack.pushPose();
        joint.translateAndRotate(poseStack);
        boards.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(PLAIN)), light,
                OverlayTexture.NO_OVERLAY, WOOD);
        bands.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(PLAIN)), light,
                OverlayTexture.NO_OVERLAY, TIE);
        poseStack.popPose();
    }

}
