package de.kaboomstudios.kaboomkeybinds.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;

/**
 * What this mod adds to a row of the key binds list. The mixin on {@code KeyBindsList.KeyEntry} implements this
 * interface, so a row can be cast to it, for example by tests or by other mods that want to find the button.
 */
public interface KeyEntryAccess {
    /** The mapping shown in this row. */
    KeyMapping kaboomkeybinds$mapping();

    /** The button for the alternative key, placed between the primary key button and the reset button. */
    Button kaboomkeybinds$alternativeButton();
}
