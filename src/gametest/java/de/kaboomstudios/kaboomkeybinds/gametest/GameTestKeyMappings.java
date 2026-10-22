package de.kaboomstudios.kaboomkeybinds.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * Registers a key mapping with an unusually long name through Fabric API, the way other mods add their key
 * mappings. The controls menu tests use it to check that mod key mappings get an alternative key and that long
 * names are handled by the row layout.
 */
public final class GameTestKeyMappings implements ClientModInitializer {
    public static final String LONG_NAME = "key.kaboomkeybinds-gametest.long_name";

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(
                new KeyMapping(LONG_NAME, InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC));
    }
}
