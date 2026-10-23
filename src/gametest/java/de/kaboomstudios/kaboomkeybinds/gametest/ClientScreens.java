package de.kaboomstudios.kaboomkeybinds.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * The open screen and the loading overlay of the client. Minecraft 26.2 moved both from {@link Minecraft} to its GUI,
 * so the game tests reach them through this class and read the same for every version.
 */
final class ClientScreens {
    private ClientScreens() {
    }

    /** The open screen, null while playing without one. */
    static Screen current(Minecraft client) {
        //? if >=26.2 {
        return client.gui.screen();
        //?} else {
        /*return client.screen;
        *///?}
    }

    /** Closes the open screen the way the game does, which shows the title screen outside a world. */
    static void close(Minecraft client) {
        //? if >=26.2 {
        client.gui.setScreen(null);
        //?} else {
        /*client.setScreen(null);
        *///?}
    }

    /** Whether an overlay covers the screen, for example the loading overlay that fades out after a resource reload. */
    static boolean hasOverlay(Minecraft client) {
        //? if >=26.2 {
        return client.gui.overlay() != null;
        //?} else {
        /*return client.getOverlay() != null;
        *///?}
    }
}
