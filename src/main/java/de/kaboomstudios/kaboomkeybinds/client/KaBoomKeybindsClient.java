package de.kaboomstudios.kaboomkeybinds.client;

import de.kaboomstudios.kaboomkeybinds.KaBoomKeybinds;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entry point, called by Fabric Loader. The interface comes from Fabric Loader itself, not from Fabric API,
 * so the mod stays free of a Fabric API dependency.
 *
 * <p>The actual work of the mod happens inside the mixins, which Mixin applies before any entry point runs.
 * This class only reports that the mod is present.
 */
public final class KaBoomKeybindsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KaBoomKeybinds.LOGGER.info("KaBoom Keybinds initialized");
    }
}
