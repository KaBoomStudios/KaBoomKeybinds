package de.kaboomstudios.kaboomkeybinds.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.kaboomstudios.kaboomkeybinds.core.KeyPair;

/**
 * The alternative key that this mod adds to every {@link net.minecraft.client.KeyMapping}. The mixin on
 * {@code KeyMapping} implements this interface, so every key mapping object can be cast to it.
 *
 * <p>The method names carry the mod id as prefix so that they cannot clash with methods other mods add to the
 * same class.
 */
public interface AlternativeKeyMapping {
    /** The alternative key, {@link InputConstants#UNKNOWN} when none is bound. Never {@code null}. */
    InputConstants.Key kaboomkeybinds$getAlternativeKey();

    /**
     * Sets the alternative key without saving it or updating the key lookup table. {@code null} unbinds.
     * Callers go through {@link AlternativeKeys}, which keeps the file and the lookup table in sync.
     */
    void kaboomkeybinds$setAlternativeKey(InputConstants.Key key);

    /** Whether an alternative key is bound. */
    boolean kaboomkeybinds$hasAlternativeKey();

    /** Both keys of the mapping, with {@link InputConstants#UNKNOWN} mapped to {@code null} as the core rules expect. */
    KeyPair<InputConstants.Key> kaboomkeybinds$keys();
}
