package com.claudemods.triage;

import com.claudemods.triage.net.ModNetwork;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import com.claudemods.triage.block.ModBlocks;
import com.claudemods.triage.compat.Ragdolls;
import com.claudemods.triage.item.ModItems;
import com.claudemods.triage.village.ModVillagers;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;

@Mod(TriageMod.MODID)
public class TriageMod {
    public static final String MODID = "triage";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * No arguments, and that is not an oversight.
     *
     * <p>Forge 47.4.0 looks for {@code TriageMod()} and nothing else. The form that takes
     * a context is a later thing, and reaching for it here - which is what the compiler's
     * own deprecation warning suggests - makes a mod that compiles cleanly and refuses to
     * load. Chased once already; written down so it is not chased twice.
     */
    public TriageMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModVillagers.register(modBus);
        ModParticles.register(modBus);
        ModSounds.register(modBus);
        // The channel, which on 1.20.1 is built once rather than registered from an
        // event. Nothing sends anything before this returns.
        ModNetwork.register();
        // Optional, and nothing here is loaded when the mod it is for is not installed.
        Ragdolls.init();
        // Everything here decides how much damage a body takes, so it belongs to the
        // world and not to the client: SERVER, not COMMON.
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        // And the one thing that is the looker's business rather than the world's.
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        // The numbers are read in the damage path and once a second per player, so they
        // are copied into plain fields whenever the config appears - on the server when
        // the world loads, and on each client when the server sends its copy over.
        // 1.20.1's bus takes the listener and works the type out from it, rather than
        // being told the class first. Same two events, same handler.
        modBus.addListener((ModConfigEvent.Loading event) -> onConfig(event.getConfig()));
        modBus.addListener((ModConfigEvent.Reloading event) -> onConfig(event.getConfig()));
    }

    private static void onConfig(ModConfig config) {
        if (config.getSpec() == Config.SPEC) {
            Config.bake();
        } else if (config.getSpec() == ClientConfig.SPEC) {
            ClientConfig.bake();
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
