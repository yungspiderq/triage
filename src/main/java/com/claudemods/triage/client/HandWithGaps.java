package com.claudemods.triage.client;

import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.LimbMark;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Nothing in the hand that is not there.
 *
 * <p>Hiding the arm without this leaves the sword: the layer that draws a held item
 * hangs it off the arm's joint, and a joint is still where it was whether or not
 * anything is drawn at it. A pickaxe floating at a shoulder is the sort of thing that
 * makes everything around it look unfinished.
 *
 * <p>A hook keeps its item. There is a real question about what a hook can hold and the
 * honest answer is "less than a hand", but the mod does not take items out of anybody's
 * inventory for it, and drawing an empty hand for a player who is holding a sword would
 * be the render lying about the game. The empty stump does not have that problem: there
 * is nothing there at all, and nothing is what gets drawn.
 */
public class HandWithGaps<T extends Player, M extends EntityModel<T> & ArmedModel & HeadedModel>
        extends PlayerItemInHandLayer<T, M> {
    public HandWithGaps(RenderLayerParent<T, M> parent, ItemInHandRenderer renderer) {
        super(parent, renderer);
    }

    @Override
    protected void renderArmWithItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                     HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (entity instanceof Player player) {
            BodyPart part = arm == HumanoidArm.LEFT ? BodyPart.LEFT_ARM : BodyPart.RIGHT_ARM;
            if (LimbMark.state(Limbs.of(player), part) == LimbMark.GONE) {
                return;
            }
        }
        super.renderArmWithItem(entity, stack, context, arm, poseStack, buffer, light);
    }
}
