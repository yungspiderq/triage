package com.claudemods.triage.client;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.LimbMark;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Blood, on the part it is coming out of.
 *
 * <p>Everything in this mod worth seeing from across a field was made visible one at a
 * time - the mark on a chest, the arm that is not there, the bandage, the strap. Bleeding
 * was the one left, and it needed the least arguing for: blood on somebody is not
 * information you walk over and read off a screen. It is the thing you see first and from
 * furthest away, and it is <em>why</em> anybody walks over at all.
 *
 * <p>Painted rather than built. The first attempt stuck red boxes on the limbs, which is
 * exactly what a slab of wool looks like: a red box. Blood is not a shape, it is a stain -
 * ragged at the edge, running downhill, thinning as it goes and hanging in a drop at the
 * end - and none of that survives being made out of cubes.
 *
 * <p>So it is a texture in the skin's own layout, hung on the pieces the model already
 * wears over the skin: the hat, the jacket, the sleeves and the trousers. Vanilla inflates
 * those a quarter of a pixel, which means the stain sits <em>on</em> somebody instead of
 * fighting them for the same surface, and no new geometry exists anywhere.
 *
 * <p>Light and heavy are told apart by how much of it there is rather than by shade. Two
 * reds side by side are a thing nobody can rank.
 *
 * <p>Nothing is drawn under a dressing. What is under a bandage is under a bandage, and a
 * bandage that has soaked through is already its own answer in {@link DressingLayer}.
 */
public final class BloodLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation BLOOD = TriageMod.id("textures/entity/blood.png");

    /** Fresh, wet, and all of it. */
    private static final int RUNNING = 0xFFBE1108;
    /**
     * And a seep, which is the same stain with most of it taken away.
     *
     * <p>The alpha does the work rather than the colour: a light bleed is not a different
     * colour of blood, there is simply less of it on you.
     */
    private static final int SEEPING = 0x9E7A120C;

    /**
     * And the one that is not the same colour of red.
     *
     * <p>Bright, because it is: what comes out of an artery has not been anywhere yet.
     * It is also the only stain in the mod that <b>moves</b> - it brightens and fades on
     * the beat, because that is precisely what tells one apart from a bad cut at forty
     * paces, and forty paces is where this needs to be readable.
     */
    private static final int SPURTING = 0xFFF03A22;
    private static final int SPURTING_LOW = 0xFFC01E10;

    public BloodLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        int bits = ClientTags.bleeds(player.getId());
        if (bits == 0 || player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        int limbs = Limbs.of(player);

        // The chest and the belly share one piece of the model, so whichever of the two
        // is worse decides how much shows. A body does not have two torsos.
        int trunk = Math.max(LimbMark.bleed(bits, BodyPart.THORAX),
                LimbMark.bleed(bits, BodyPart.STOMACH));

        stain(LimbMark.bleed(bits, BodyPart.HEAD), model.hat, poseStack, buffer, light, ageInTicks);
        stain(trunk, model.jacket, poseStack, buffer, light, ageInTicks);
        stain(limb(bits, limbs, BodyPart.LEFT_ARM), model.leftSleeve, poseStack, buffer, light, ageInTicks);
        stain(limb(bits, limbs, BodyPart.RIGHT_ARM), model.rightSleeve, poseStack, buffer, light, ageInTicks);
        stain(limb(bits, limbs, BodyPart.LEFT_LEG), model.leftPants, poseStack, buffer, light, ageInTicks);
        stain(limb(bits, limbs, BodyPart.RIGHT_LEG), model.rightPants, poseStack, buffer, light, ageInTicks);
    }

    /**
     * A limb bleeds only while it is still there.
     *
     * <p>A stump is the part above it and carries its own state; painting blood down a leg
     * that is not being drawn would put a stain in mid-air.
     */
    private static int limb(int bits, int limbs, BodyPart part) {
        return LimbMark.state(limbs, part) == LimbMark.THERE ? LimbMark.bleed(bits, part) : LimbMark.DRY;
    }

    private void stain(int level, ModelPart piece, PoseStack poseStack,
                       MultiBufferSource buffer, int light, float age) {
        if (level == LimbMark.DRY) {
            return;
        }
        Tint.render(piece, poseStack, buffer, BLOOD, true, light, colour(level, age));
    }

    /**
     * Which red, and how bright it is this frame.
     *
     * <p>The beat is a fixed one rather than the patient's real rate. It has to be: the
     * rate lives on a chart nobody standing across the clearing has read, and a stain
     * that could only be read correctly by somebody who had already done the reading
     * would be a signal for people who no longer need one.
     */
    private static int colour(int level, float age) {
        if (level != LimbMark.SPURTING) {
            return level == LimbMark.RUNNING ? RUNNING : SEEPING;
        }
        return (age % 20.0F) < 5.0F ? SPURTING : SPURTING_LOW;
    }
}
