package de.kaboomstudios.kaboomkeybinds.client;

import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

/**
 * Access to the screen a key binds list belongs to. The mixin on {@code KeyBindsList} implements this interface,
 * because the field is private in vanilla and the rows need the screen to start binding an alternative key.
 */
public interface KeyBindsListAccess {
    KeyBindsScreen kaboomkeybinds$screen();

    /**
     * Whether the rows of this list show a primary and an alternative key column, which the screen labels with
     * headings and the list makes room for. True for key mapping lists. Lists that reuse the key binds list for other
     * rows, such as the list of free keys in the Controlling mod, return false and keep their own layout.
     */
    default boolean kaboomkeybinds$showsKeyColumns() {
        return true;
    }
}
