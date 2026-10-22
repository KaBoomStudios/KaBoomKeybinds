package de.kaboomstudios.kaboomkeybinds.client;

import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

/**
 * Access to the screen a key binds list belongs to. The mixin on {@code KeyBindsList} implements this interface,
 * because the field is private in vanilla and the rows need the screen to start binding an alternative key.
 */
public interface KeyBindsListAccess {
    KeyBindsScreen kaboomkeybinds$screen();
}
