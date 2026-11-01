package com.claudemods.triage;

import com.claudemods.triage.block.ModBlocks;
import com.claudemods.triage.command.TriageCommand;
import com.claudemods.triage.data.BodyStore;
import com.claudemods.triage.event.Events;
import com.claudemods.triage.item.ModItems;
import com.claudemods.triage.net.ConfigPayload;
import com.claudemods.triage.net.ModNetwork;
import com.claudemods.triage.village.ModVillagers;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * Triage on Fabric 1.20.1: the fourth tree, and the one that is two ports deep.
 *
 * <p>The Forge 1.20.1 tree next door has already taken the mod down a Minecraft version -
 * data components back to NBT, a different damage pipeline, a hundred renamed methods - and
 * every one of those decisions is in the files this tree copies from it. What is left is the
 * loader, which is the same difference as in triage-fabric: registries, the event bus, where
 * a body is kept, how a packet is sent, and the config. See tools/carry.py.
 */
public final class TriageMod implements ModInitializer {
    public static final String MODID = "triage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    @Override
    public void onInitialize() {
        // Read first: everything below this line is allowed to ask how the mod is tuned.
        Config.load();

        BodyStore.register();
        ModBlocks.register();
        ModItems.register();
        ModParticles.register();
        ModSounds.register();
        ModVillagers.register();

        ModNetwork.register();
        Events.register();

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, environment) -> TriageCommand.register(dispatcher));

        // Forge hands a server config to its own clients. Nothing here does, so the server
        // says what the numbers are the moment somebody arrives - before the first body is
        // sent, which is the first thing that is drawn using them.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ModNetwork.toPlayer(handler.getPlayer(), new ConfigPayload(Config.values())));
    }
}
