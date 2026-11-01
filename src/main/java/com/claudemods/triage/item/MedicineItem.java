package com.claudemods.triage.item;

import com.claudemods.triage.med.MedicineKind;
import com.claudemods.triage.med.Treatments;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import com.claudemods.triage.med.Theatre;
import com.claudemods.triage.body.Body;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A piece of medicine. The item itself does nothing except say what kind it is and
 * hand the player over to {@link Treatments}, which decides whether that is worth
 * opening the treatment screen for.
 */
public class MedicineItem extends Item {
    private final MedicineKind kind;

    public MedicineItem(MedicineKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public MedicineKind kind() {
        return kind;
    }

    /**
     * What this particular stack counts as. Almost always the same thing the item always
     * is - but a blood bag is two treatments in one item depending on whether there is
     * anything in it, and everything that asks "what kind of medicine is this" has to go
     * through here rather than through the field.
     */
    public MedicineKind kind(ItemStack stack) {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Antiseptic, crouching: the bottle goes over the instruments instead of over a
        // wound. The field version of the table, and the trade is the obvious one - the
        // table is free and does not travel, and the bottle travels and is gone.
        if (kind(stack) == MedicineKind.ANTISEPTIC && player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer && scrubKit(serverPlayer, stack)) {
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (player instanceof ServerPlayer serverPlayer) {
            Treatments.begin(serverPlayer, serverPlayer, kind(stack), hand);
        }
        // No item is spent here: the screen has not been through yet, and a treatment
        // that is refused or abandoned should cost nothing.
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Right-clicking somebody else with medicine works on them instead of on you. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (!(target instanceof Player patient)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer healer && patient instanceof ServerPlayer treated) {
            Treatments.begin(healer, treated, kind(stack), hand);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    /**
     * Lying on the bottom of something.
     *
     * <p>The one hook that fires for a dropped item and nothing else, which is why the
     * rinsing lives here rather than in a tick that would have to look at every entity in
     * the world to find three knives.
     */
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level().isClientSide || !entity.isInWater()) {
            return false;
        }
        if (Theatre.rinse(stack, kind(stack), entity.level().random)
                && entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.BUBBLE, entity.getX(), entity.getY() + 0.2,
                    entity.getZ(), 6, 0.12, 0.05, 0.12, 0.0);
            level.playSound(null, entity.blockPosition(), SoundEvents.BUCKET_FILL,
                    SoundSource.PLAYERS, 0.35F, 1.5F);
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("triage.tooltip." + kind(stack).id()).withStyle(ChatFormatting.GRAY));
        grime(stack, tooltip);
    }

    /**
     * How many patients this has been through.
     *
     * <p>It decides better than half the chance of leaving something in the next wound,
     * and until this line existed there was no way at all to find it out - the saw looked
     * the same on its sixth patient as on its first. A hidden number that dangerous is
     * not difficulty, it is the mod keeping a secret.
     */
    private void grime(ItemStack stack, List<Component> tooltip) {
        MedicineKind kind = kind(stack);
        if (!kind.reusable() || !kind.opensSkin()) {
            return;
        }
        int used = Theatre.grime(stack);
        String key = used == 0 ? "clean" : used >= 6 ? "filthy" : "used";
        ChatFormatting colour = used == 0 ? ChatFormatting.GREEN
                : used >= 6 ? ChatFormatting.RED : ChatFormatting.YELLOW;
        tooltip.add(used == 0
                ? Component.translatable("triage.tooltip.grime_clean").withStyle(colour)
                : Component.translatable("triage.tooltip.grime_" + key, used).withStyle(colour));
    }

    /**
     * A bottle over everything in the bag.
     *
     * <p>All of them at once rather than one at a time, because nobody has ever cleaned
     * one instrument and put the rest back dirty, and because a chore repeated six times
     * is a chore the mod invented rather than one it modelled.
     */
    private static boolean scrubKit(ServerPlayer player, ItemStack bottle) {
        int cleaned = 0;
        for (ItemStack held : player.getInventory().items) {
            if (held.getItem() instanceof MedicineItem tool
                    && tool.kind(held).reusable() && Theatre.grime(held) > 0) {
                Theatre.clean(held);
                cleaned++;
            }
        }
        if (cleaned == 0) {
            Body.notify(player, Component.translatable("triage.msg.nothing_to_clean"),
                    ChatFormatting.GRAY, null);
            return false;
        }
        bottle.shrink(1);
        Body.notify(player, Component.translatable("triage.msg.kit_scrubbed", cleaned),
                ChatFormatting.AQUA, SoundEvents.BREWING_STAND_BREW);
        return true;
    }
}
