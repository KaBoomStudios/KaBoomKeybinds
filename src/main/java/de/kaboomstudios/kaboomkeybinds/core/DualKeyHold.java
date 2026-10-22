package de.kaboomstudios.kaboomkeybinds.core;

/**
 * Tracks which of the two keys of a mapping are physically held, so that the mapping stays active as long as at
 * least one of them is down. Vanilla keeps a single "is down" flag per mapping, which would end the action as soon
 * as either key is released while the other is still held.
 *
 * <p>{@link #set(KeySlot, boolean)} records the physical state of one key and returns whether the mapping should
 * still count as held: a press always yields "down", a release yields "down" only while the other key is still held.
 * The client layer passes every press on as a press, as vanilla does, so toggle mappings (toggle sneak, toggle
 * sprint) toggle on every press of either key. A release is passed on only when it leaves no key held: passing it
 * on as a release would end the action the other key still holds, passing it on as a press would flip a toggle
 * mapping.
 *
 * <p>Minecraft resets its own flags when a screen opens or the mouse is grabbed. The client layer calls
 * {@link #releaseAll()} or {@link #set(KeySlot, boolean)} at those points so that this state never goes stale.
 */
public final class DualKeyHold {
    private boolean primaryDown;
    private boolean secondaryDown;

    /** Records the physical state of one key and returns whether the mapping should count as held. */
    public boolean set(KeySlot slot, boolean down) {
        if (slot == KeySlot.PRIMARY) {
            primaryDown = down;
        } else {
            secondaryDown = down;
        }
        return isDown();
    }

    /** Whether at least one of the two keys is held. */
    public boolean isDown() {
        return primaryDown || secondaryDown;
    }

    /** Whether the key in the given slot is held. */
    public boolean isDown(KeySlot slot) {
        return slot == KeySlot.PRIMARY ? primaryDown : secondaryDown;
    }

    /** Forgets both keys, for example when a screen opens and the game releases all mappings. */
    public void releaseAll() {
        primaryDown = false;
        secondaryDown = false;
    }
}
