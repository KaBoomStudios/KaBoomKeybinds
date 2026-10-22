package de.kaboomstudios.kaboomkeybinds.client;

import net.minecraft.client.KeyMapping;
import org.jspecify.annotations.Nullable;

/**
 * The binding state that this mod adds to the key binds screen: which mapping is waiting for its alternative key.
 * The mixin on {@code KeyBindsScreen} implements this interface, so the screen object can be cast to it.
 *
 * <p>Vanilla keeps the mapping that waits for its primary key in a public field of the screen. The alternative key
 * gets its own state, so the primary button never shows the waiting marker while the alternative key is bound and
 * the vanilla code that reads the field stays untouched.
 */
public interface AlternativeKeySelection {
    /** The mapping whose alternative key is being bound, or {@code null} if none is. */
    @Nullable KeyMapping kaboomkeybinds$selectedAlternative();

    /** Starts binding the alternative key of a mapping, or stops with {@code null}. */
    void kaboomkeybinds$selectAlternative(@Nullable KeyMapping mapping);
}
