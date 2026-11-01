package com.claudemods.triage.block;

import com.claudemods.triage.med.MedicineKind;
import com.claudemods.triage.med.Theatre;
import com.claudemods.triage.item.MedicineItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Somewhere for a medic to work, and now somewhere to clean what they work with.
 *
 * <p>The table has been decoration with a job title: it told a village where to put a
 * medic and did nothing else. Which was a strange thing for the one block in a mod about
 * medicine to be.
 *
 * <p>Now it is the answer to the other half of the theatre. An instrument that has been
 * through somebody is dirty and stays dirty; a dirty instrument is the oldest way there
 * is of killing a patient you were trying to help; and this is where that is put right.
 * Hold the thing against the table and it comes back clean.
 *
 * <p>Antiseptic does the same in the field and costs a bottle. That is the trade: the
 * table is free and does not travel, and the bottle travels and is gone.
 */
public class MedicalTable extends Block {
    public MedicalTable(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof MedicineItem medicine)) {
            return InteractionResult.PASS;
        }
        MedicineKind kind = medicine.kind(stack);
        // Only the ones that are carried rather than spent. Scrubbing a bandage is not a
        // thing, and offering it would be the mod inventing a chore.
        if (!kind.reusable()) {
            return InteractionResult.PASS;
        }
        if (Theatre.grime(stack) <= 0) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("triage.msg.already_clean").withStyle(ChatFormatting.GRAY),
                        true);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide) {
            Theatre.clean(stack);
            player.displayClientMessage(
                    Component.translatable("triage.msg.scrubbed").withStyle(ChatFormatting.AQUA), true);
        }
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6F, 1.4F);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
