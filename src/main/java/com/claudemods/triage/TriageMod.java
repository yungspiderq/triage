package com.claudemods.triage;

import com.claudemods.triage.block.ModBlocks;
import com.claudemods.triage.command.TriageCommand;
import com.claudemods.triage.data.ModAttachments;
import com.claudemods.triage.event.Events;
import com.claudemods.triage.item.ModComponents;
import com.claudemods.triage.item.ModItems;
import com.claudemods.triage.med.MedicineEvents;
import com.claudemods.triage.net.ConfigPayload;
import com.claudemods.triage.net.TriageNet;
import com.claudemods.triage.village.ModVillagers;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * The Fabric side of the same mod.
 *
 * <p>Third tree, and the reason there is a third rather than a branch is the reason there
 * is a second: these three lines cannot share a toolchain, a way of hanging data off a
 * player, a networking layer, a registry or an event bus. What they can share is
 * everything above that line - the injuries, the illnesses, the organs, the readings, the
 * minigames - which is most of the mod by weight, and which is why this tree is written
 * against Mojang mappings rather than Yarn. See tools/carry.py.
 */
public final class TriageMod implements ModInitializer {
    public static final String MODID = "triage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        // Read first: everything below this line is allowed to ask how the mod is tuned.
        Config.load();

        ModAttachments.register();
        ModComponents.register();
        ModBlocks.register();
        ModItems.register();
        ModParticles.register();
        ModSounds.register();
        ModVillagers.register();

        TriageNet.register();
        TriageNet.listen();
        Events.register();

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, environment) -> TriageCommand.register(dispatcher));

        // NeoForge hands a server config to its own clients. Nothing here does, so the
        // server says what the numbers are the moment somebody arrives - before the first
        // body is sent, which is the first thing that is drawn using them.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                TriageNet.send(handler.getPlayer(), new ConfigPayload(Config.values())));
    }
}
