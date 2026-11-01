package com.claudemods.triage.item;

import com.claudemods.triage.body.Allergy;
import com.claudemods.triage.body.BloodType;
import org.jetbrains.annotations.Nullable;
import com.claudemods.triage.body.Body;
import com.claudemods.triage.body.BodyHealth;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A strip of metal that says the thing you will not be able to say.
 *
 * <p>The allergy this version adds is interesting for one reason: <b>you can be asked, and
 * you can be past answering.</b> A conscious patient tells the medic what they cannot have.
 * The same patient ten minutes later, on the floor, tells them nothing at all - and the
 * dose that was a treatment becomes the thing that finishes them.
 *
 * <p>That is a real problem and it has a real answer, and the answer is not a mechanic
 * anybody had to invent. People who cannot afford that conversation to fail wear the
 * sentence on their wrist. It is the oldest piece of medical technology that is purely
 * about communication, and it exists because of exactly the gap this mod now has.
 *
 * <p>So: stamp one with your own name for it, keep it on you, and anybody who opens your
 * chart reads it whether you are talking or not. It answers nothing else, protects nothing,
 * and costs two nuggets - which is right, because what it is worth is entirely decided by
 * whether anybody thinks to look before they open the bag.
 */
public final class Bracelet extends Item {
    public Bracelet(Properties properties) {
        super(properties);
    }

    public static Allergy stamped(ItemStack stack) {
        Allergy allergy = stack.get(ModComponents.ALLERGY.get());
        return allergy == null ? Allergy.NONE : allergy;
    }

    /** The blood group on this band, or null on a blank one. */
    public static @Nullable BloodType group(ItemStack stack) {
        return stack.get(ModComponents.BLOOD.get());
    }

    public static boolean blank(ItemStack stack) {
        return !stamped(stack).any();
    }

    /**
     * Stamped with what the person holding it cannot have.
     *
     * <p>Their own, and only their own. A blank one is worth nothing and a stamped one is
     * worth exactly what it says, so there is no version of this that lets somebody carry
     * a lie about themselves - the item knows what the body knows.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        BodyHealth body = Body.of(server);
        Allergy allergy = body.allergy();
        if (!allergy.any()) {
            Body.notify(server, Component.translatable("triage.msg.bracelet_nothing"),
                    ChatFormatting.GRAY, null);
            return InteractionResultHolder.fail(stack);
        }
        if (stamped(stack) == allergy) {
            return InteractionResultHolder.fail(stack);
        }
        stack.set(ModComponents.ALLERGY.get(), allergy);
        // And the other sentence nobody unconscious can say. It goes on the same band
        // because it is the same kind of fact and because a person wearing two bands is a
        // person who has thought about this harder than the mod needs them to.
        if (body.bloodType() != null) {
            stack.set(ModComponents.BLOOD.get(), body.bloodType());
        }
        Body.notify(server, Component.translatable("triage.msg.bracelet_stamped", allergy.title()),
                ChatFormatting.GOLD, SoundEvents.ANVIL_USE);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        Allergy allergy = stamped(stack);
        if (!allergy.any()) {
            tooltip.add(Component.translatable("triage.tooltip.bracelet_blank")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("triage.tooltip.bracelet", allergy.title())
                .withStyle(ChatFormatting.GOLD));
        BloodType group = group(stack);
        if (group != null) {
            tooltip.add(Component.translatable("triage.tooltip.bracelet_blood", group.title())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
