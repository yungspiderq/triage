package com.claudemods.triage.client;

import com.claudemods.triage.ClientConfig;
import com.claudemods.triage.Config;
import com.claudemods.triage.ModParticles;
import com.claudemods.triage.Reach;
import com.claudemods.triage.TriageMod;
import com.claudemods.triage.client.particle.BloodParticle;
import com.claudemods.triage.client.particle.StainParticle;
import com.claudemods.triage.item.BloodBag;
import com.claudemods.triage.item.ModItems;
import com.claudemods.triage.net.ModNetwork;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;

/**
 * Everything the looker needs, and nothing the world does.
 *
 * <p>The counterpart of {@link TriageMod}: same order, same jobs - config, packets, then the
 * things that draw. What is not here is in the two mixin files, because five of the moments
 * this mod draws at are not events on this loader.
 */
public final class TriageClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        ModNetwork.listen();
        ModKeys.register();
        layers();
        particles();

        ItemProperties.register(ModItems.BLOOD_BAG.get(), TriageMod.id("filled"),
                (stack, level, entity, seed) -> BloodBag.filled(stack) ? 1.0F : 0.0F);

        // The hotbar first and then over everything, chart included: the second one is not
        // another readout on the screen, it is what the screen looks like through these
        // particular eyes.
        ClientHud hud = new ClientHud();
        Symptoms symptoms = new Symptoms();
        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            hud.render(graphics, partialTick, width, height);
            symptoms.render(graphics, partialTick, width, height);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientEvents.onClientTick());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientEvents.onLoggingOut();
            // And the server's numbers go with the server; back to this machine's own.
            Config.load();
        });

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) ->
                ClientEvents.onEntityLeft(new ClientHooks.EntityLeaveLevelEvent(entity)));

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                ClientEvents.onInventoryOpened(new ClientHooks.ScreenInit(screen)));
    }

    /**
     * Two changes to how a player is drawn, both about limbs that are not there.
     *
     * <p>The prosthetic and the rest are ordinary added layers. The armour and the held item
     * are not: those replace vanilla layers in place rather than stacking on top, because what
     * needs changing is a decision the vanilla one makes about its own model, and there is no
     * seam between it and the body underneath.
     */
    private static void layers() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, renderer, helper, context) -> {
                    if (!(renderer instanceof PlayerRenderer player)) {
                        return;
                    }
                    boolean slim = player.getModel().slim;
                    replace(player, HumanoidArmorLayer.class, new ArmourWithGaps<>(player,
                            armourModel(context, slim, true),
                            armourModel(context, slim, false),
                            context.getModelManager()));
                    replace(player, PlayerItemInHandLayer.class,
                            new HandWithGaps<>(player, context.getItemInHandRenderer()));
                    player.addLayer(new ProstheticLayer(player));
                    // Before the cloth: a burn under a bandage is under a bandage, and a limb
                    // that died under a strap died under it.
                    player.addLayer(new SkinLayer(player));
                    player.addLayer(new DressingLayer(player));
                    // Over the cloth, because a splint goes on over whatever is already
                    // wrapped round the limb - which is the order it happens in as well.
                    player.addLayer(new SplintLayer(player));
                    // After the dressing, so a bandage that has soaked through still reads as
                    // a bandage: what is under one is under one.
                    player.addLayer(new BloodLayer(player));
                });
    }

    /**
     * Swaps one vanilla layer for a subclass of it, in place.
     *
     * <p>In place rather than appended, because the order of these is the order they are drawn
     * in: armour goes on before the cape and the item goes in the hand before the arrows stick
     * out of anybody, and a layer moved to the end of the list is a layer drawn over things
     * that should be over it.
     *
     * <p>Through {@link Reach}, the same as in the Forge tree. An access widener opens a field
     * for reading and writing but not for replacing what a private final list contains, and a
     * replacement is the whole point of this one.
     *
     * <p><b>By type, never by name.</b> On this loader the field is called {@code layers}
     * while the mod is being written and {@code field_3901} when anybody plays it, so a
     * lookup by name works only in a development environment - which is exactly the bug
     * that stopped the 1.20.1 builds from starting at all. There is one {@code List} on a
     * living-entity renderer, and a type cannot be remapped.
     */
    private static final java.lang.reflect.Field LAYERS =
            Reach.sole(LivingEntityRenderer.class, List.class);

    private static void replace(PlayerRenderer renderer, Class<?> vanilla,
            RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> mine) {
        List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> layers =
                Reach.get(LAYERS, renderer);
        if (layers == null) {
            // Armour draws the vanilla way and the mod keeps working. Nobody loses a game
            // because a bandage is under a sleeve instead of over it.
            return;
        }
        for (int i = 0; i < layers.size(); i++) {
            if (vanilla.isInstance(layers.get(i))) {
                layers.set(i, mine);
                return;
            }
        }
    }

    private static HumanoidArmorModel<AbstractClientPlayer> armourModel(EntityRendererProvider.Context context,
            boolean slim, boolean inner) {
        return new HumanoidArmorModel<>(context.bakeLayer(inner
                ? (slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)
                : (slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)));
    }

    /** The mod's own drop and its own stain, instead of borrowing redstone's sparkle. */
    private static void particles() {
        ParticleFactoryRegistry.getInstance().register(ModParticles.BLOOD.get(), BloodParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.STAIN.get(), StainParticle.Provider::new);
    }
}
