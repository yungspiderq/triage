package com.claudemods.triage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * The client half of the mod, and the only thing in it that exists for a button.
 *
 * <p>NeoForge puts a "Config" button on every mod in the list. It asks the mod for a
 * screen factory through {@link IConfigScreenFactory}; without one the button is not
 * there at all, which is why a mod with three hundred settings could still only be
 * configured by finding a file. The factory is registered here rather than in
 * {@link TriageMod} because {@link ConfigurationScreen} is client code, and a dedicated
 * server must never be asked to load it.
 */
@Mod(value = TriageMod.MODID, dist = Dist.CLIENT)
public final class TriageClientMod {
    public TriageClientMod(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
