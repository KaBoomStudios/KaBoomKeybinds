package de.kaboomstudios.kaboomkeybinds.core;

/**
 * The two places where a key mapping can hold a key: the vanilla key and the alternative key added by this mod.
 */
public enum KeySlot {
    PRIMARY,
    SECONDARY;

    /** The other slot of the same mapping. */
    public KeySlot other() {
        return this == PRIMARY ? SECONDARY : PRIMARY;
    }
}
