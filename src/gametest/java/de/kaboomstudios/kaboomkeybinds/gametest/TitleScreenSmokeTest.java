package de.kaboomstudios.kaboomkeybinds.gametest;

import de.kaboomstudios.kaboomkeybinds.KaBoomKeybinds;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Smoke test: the client starts with the mod loaded, its mixins apply without errors and the title screen appears.
 * The screenshot lands in build/run/clientGameTest/screenshots.
 */
public final class TitleScreenSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!FabricLoader.getInstance().isModLoaded(KaBoomKeybinds.MOD_ID)) {
            throw new AssertionError("mod " + KaBoomKeybinds.MOD_ID + " is not loaded");
        }

        context.waitForScreen(TitleScreen.class);
        context.takeScreenshot("title-screen");
    }
}
