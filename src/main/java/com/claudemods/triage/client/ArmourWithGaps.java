package com.claudemods.triage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Armour that has the same gaps in it the body does.
 *
 * <p>Hiding a missing arm on the player model is half a job while a chestplate is being
 * worn: the armour is a second humanoid model with its own four limbs, drawn on top,
 * and it decides for itself which of them to show. Left alone it puts an iron sleeve
 * exactly where the arm is not, which reads as a bug rather than as a wound - and it
 * would have been the first thing anybody saw, because most players are wearing armour.
 *
 * <p>There is no event between the base model and the armour, so this replaces the
 * vanilla layer with the same thing plus one line. The entity is caught on the way past
 * because the method that decides visibility is not told whose armour it is drawing -
 * which is fine, since rendering is one thread doing one player at a time.
 */
public class ArmourWithGaps<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
        extends HumanoidArmorLayer<T, M, A> {
    private @Nullable T wearer;

    public ArmourWithGaps(RenderLayerParent<T, M> parent, A inner, A outer, ModelManager models) {
        super(parent, inner, outer, models);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        wearer = entity;
        try {
            super.render(poseStack, buffer, light, entity, limbSwing, limbSwingAmount, partialTick,
                    ageInTicks, netHeadYaw, headPitch);
        } finally {
            wearer = null;
        }
    }

    @Override
    protected void setPartVisibility(A model, EquipmentSlot slot) {
        super.setPartVisibility(model, slot);
        if (wearer != null) {
            Limbs.hide(model, wearer);
        }
    }
}
