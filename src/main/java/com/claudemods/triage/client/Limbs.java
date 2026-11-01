package com.claudemods.triage.client;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.body.BodyPart;
import com.claudemods.triage.body.LimbMark;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;

/**
 * A body with a piece of it not drawn.
 *
 * <p>Everything else this mod does to a body happens on a chart. That was the right
 * place for all of it right up until a limb came off: bleeding is a thing you read about
 * somebody, and an arm that is not there is a thing you see. Leaving it as a line of
 * text would have made the biggest permanent event in the mod the least visible one.
 *
 * <p>So the model part simply stops being drawn - on the player themselves, on everybody
 * around them, in the first person, and under whatever armour they are wearing. What
 * goes in its place is drawn by {@link ProstheticLayer}, and only when there is
 * something to put there.
 *
 * <p>The state comes off the same broadcast that carries a triage mark rather than off
 * anybody's chart, because it is the same kind of fact: something about a person that is
 * true from across a field and costs nobody a walk over to find out.
 */
@Mod.EventBusSubscriber(modid = TriageMod.MODID, value = Dist.CLIENT)
public final class Limbs {
    private Limbs() {
    }

    /** Which of the four this player is without, as {@link LimbMark} bits. */
    public static int of(Player player) {
        return ClientTags.limbs(player.getId());
    }

    public static boolean gone(Player player, BodyPart part) {
        return LimbMark.state(of(player), part) != LimbMark.THERE;
    }

    /**
     * Stops the parts that are not there from being drawn.
     *
     * <p>Works on any humanoid model, which is the point: the same call hides the arm on
     * the player and the sleeve of the chestplate over it, because both of those are
     * {@link HumanoidModel}s with the same four parts on them.
     */
    public static void hide(HumanoidModel<?> model, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int bits = of(player);
        if (bits == 0) {
            return;
        }
        if (LimbMark.state(bits, BodyPart.LEFT_ARM) != LimbMark.THERE) {
            model.leftArm.visible = false;
        }
        if (LimbMark.state(bits, BodyPart.RIGHT_ARM) != LimbMark.THERE) {
            model.rightArm.visible = false;
        }
        if (LimbMark.state(bits, BodyPart.LEFT_LEG) != LimbMark.THERE) {
            model.leftLeg.visible = false;
        }
        if (LimbMark.state(bits, BodyPart.RIGHT_LEG) != LimbMark.THERE) {
            model.rightLeg.visible = false;
        }
        if (model instanceof PlayerModel<?> skin) {
            skin.leftSleeve.visible = model.leftArm.visible;
            skin.rightSleeve.visible = model.rightArm.visible;
            skin.leftPants.visible = model.leftLeg.visible;
            skin.rightPants.visible = model.rightLeg.visible;
        }
    }

    @SubscribeEvent
    public static void before(RenderPlayerEvent.Pre event) {
        hide(event.getRenderer().getModel(), event.getEntity());
    }

    /**
     * Puts them back.
     *
     * <p>One model is shared by every player on the screen, so anything switched off for
     * one of them has to be switched on again before the next - otherwise the first
     * one-armed player in the room takes everybody else's arm with them.
     */
    @SubscribeEvent
    public static void after(RenderPlayerEvent.Post event) {
        show(event.getRenderer().getModel());
    }

    /** Every part this class can switch off, switched back on. */
    public static void show(HumanoidModel<?> model) {
        model.leftArm.visible = true;
        model.rightArm.visible = true;
        model.leftLeg.visible = true;
        model.rightLeg.visible = true;
        if (model instanceof PlayerModel<?> skin) {
            skin.leftSleeve.visible = true;
            skin.rightSleeve.visible = true;
            skin.leftPants.visible = true;
            skin.rightPants.visible = true;
        }
    }

    /**
     * The first person, where the arm you are looking at is the one that is gone.
     *
     * <p>Both halves of it: the bare arm swinging, and whatever it is holding. A sword
     * floating in front of a shoulder is worse than no sword, and it is also honest -
     * that hand is not holding anything, whatever the hotbar says.
     */
    @SubscribeEvent
    public static void arm(RenderArmEvent event) {
        if (missing(event.getPlayer(), event.getArm())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        HumanoidArm arm = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite();
        if (missing(player, arm)) {
            event.setCanceled(true);
        }
    }

    private static boolean missing(Player player, HumanoidArm arm) {
        return gone(player, arm == HumanoidArm.LEFT ? BodyPart.LEFT_ARM : BodyPart.RIGHT_ARM);
    }
}
