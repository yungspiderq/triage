package com.claudemods.triage.client;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.item.BloodBag;
import com.claudemods.triage.item.ModItems;
import java.util.List;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import com.claudemods.triage.ModParticles;
import com.claudemods.triage.client.particle.BloodParticle;
import com.claudemods.triage.client.particle.StainParticle;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = TriageMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, TriageMod.id("body"), new ClientHud());
        // Over the lot, including the mod's own chart: this is not another readout on
        // the screen, it is what the screen looks like through these particular eyes.
        event.registerAboveAll(TriageMod.id("symptoms"), new Symptoms());
    }

    /**
     * A full blood bag looks like a full blood bag. One item and one component decide
     * which of the two models is drawn, so the inventory answers "have I got any blood"
     * without anybody having to hover over eight identical slots.
     */
    /**
     * Two changes to how a player is drawn, both about limbs that are not there.
     *
     * <p>The prosthetic is an ordinary added layer. The armour is not: it has to be the
     * vanilla layer replaced in place rather than another one stacked on top, because
     * the thing that needs changing is a decision the vanilla one makes about its own
     * model, and there is no event between it and the body underneath. Replaced at the
     * same index so that everything drawn after it - the cape, the elytra, the arrows
     * sticking out of somebody - still comes after it.
     */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer player) {
                replace(player, HumanoidArmorLayer.class, new ArmourWithGaps<>(player,
                        armourModel(event.getContext(), skin, true),
                        armourModel(event.getContext(), skin, false),
                        event.getContext().getModelManager()));
                replace(player, PlayerItemInHandLayer.class,
                        new HandWithGaps<>(player, event.getContext().getItemInHandRenderer()));
                player.addLayer(new ProstheticLayer(player));
                // Before the cloth: a burn under a bandage is under a bandage, and a
                // limb that died under a strap died under it.
                player.addLayer(new SkinLayer(player));
                player.addLayer(new DressingLayer(player));
                // Over the cloth, because a splint goes on over whatever is already
                // wrapped round the limb - which is the order it happens in as well.
                player.addLayer(new SplintLayer(player));
                // After the dressing, so a bandage that has soaked through still reads
                // as a bandage: what is under one is under one.
                player.addLayer(new BloodLayer(player));
            }
        }
    }

    /**
     * Swaps one vanilla layer for a subclass of it, in place.
     *
     * <p>In place rather than appended, because the order of these is the order they are
     * drawn in: armour goes on before the cape and the item goes in the hand before the
     * arrows stick out of anybody, and a layer moved to the end of the list is a layer
     * drawn over things that should be over it.
     */
    private static void replace(PlayerRenderer renderer, Class<?> vanilla,
                                RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> mine) {
        List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> layers =
                ((LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) renderer).layers;
        for (int i = 0; i < layers.size(); i++) {
            if (vanilla.isInstance(layers.get(i))) {
                layers.set(i, mine);
                return;
            }
        }
    }

    private static HumanoidArmorModel<AbstractClientPlayer> armourModel(EntityRendererProvider.Context context,
                                                                        PlayerSkin.Model skin, boolean inner) {
        boolean slim = skin == PlayerSkin.Model.SLIM;
        return new HumanoidArmorModel<>(context.bakeLayer(inner
                ? (slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)
                : (slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)));
    }

    /** The mod's own drop and its own stain, instead of borrowing redstone's sparkle. */
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.BLOOD.get(), BloodParticle.Provider::new);
        event.registerSpriteSet(ModParticles.STAIN.get(), StainParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.BLOOD_BAG.get(), TriageMod.id("filled"),
                (stack, level, entity, seed) -> BloodBag.filled(stack) ? 1.0F : 0.0F));
    }
}
