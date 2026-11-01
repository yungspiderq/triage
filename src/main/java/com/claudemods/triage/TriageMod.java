package com.claudemods.triage;

import com.claudemods.triage.block.ModBlocks;
import com.claudemods.triage.compat.Ragdolls;
import com.claudemods.triage.data.ModAttachments;
import com.claudemods.triage.item.ModComponents;
import com.claudemods.triage.item.ModItems;
import com.claudemods.triage.village.ModVillagers;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;

@Mod(TriageMod.MODID)
public class TriageMod {
    public static final String MODID = "triage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TriageMod(IEventBus modBus, ModContainer container) {
        ModAttachments.register(modBus);
        ModComponents.register(modBus);
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModVillagers.register(modBus);
        ModParticles.register(modBus);
        ModSounds.register(modBus);
        // Optional, and nothing here is loaded when the mod it is for is not installed.
        Ragdolls.init();
        // Everything here decides how much damage a body takes, so it belongs to the
        // world and not to the client: SERVER, not COMMON.
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        // And the one thing that is the looker's business rather than the world's.
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        // The numbers are read in the damage path and once a second per player, so they
        // are copied into plain fields whenever the config appears - on the server when
        // the world loads, and on each client when the server sends its copy over.
        modBus.addListener(ModConfigEvent.Loading.class, event -> onConfig(event.getConfig()));
        modBus.addListener(ModConfigEvent.Reloading.class, event -> onConfig(event.getConfig()));
    }

    private static void onConfig(ModConfig config) {
        if (config.getSpec() == Config.SPEC) {
            Config.bake();
        } else if (config.getSpec() == ClientConfig.SPEC) {
            ClientConfig.bake();
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
