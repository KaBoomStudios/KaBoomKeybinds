package de.kaboomstudios.kaboomkeybinds.mixin.controlling;

import de.kaboomstudios.kaboomkeybinds.client.KeyBindsListAccess;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Controlling's list of free keys reuses the key binds list for rows that show keys instead of key mappings. It has
 * no key columns, so the screen draws no headings above it and the list keeps its own row width.
 */
@Mixin(targets = "com.blamejared.controlling.client.FreeKeysList")
public abstract class ControllingFreeKeysListMixin implements KeyBindsListAccess {
    @Override
    public boolean kaboomkeybinds$showsKeyColumns() {
        return false;
    }
}
