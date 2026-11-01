package com.claudemods.triage.mixin;

import com.claudemods.triage.item.MedicineItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An instrument lying on the bottom of something.
 *
 * <p>NeoForge has a hook that fires for a dropped item and nothing else, which is why the
 * rinsing lives on the item there rather than in a tick that would have to look at every
 * entity in the world to find three knives. This is that hook: the same one method, called
 * from the one place where it is cheap.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void triage$rinse(CallbackInfo callback) {
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getItem();
        if (stack.getItem() instanceof MedicineItem medicine) {
            medicine.onEntityItemUpdate(stack, self);
        }
    }
}
