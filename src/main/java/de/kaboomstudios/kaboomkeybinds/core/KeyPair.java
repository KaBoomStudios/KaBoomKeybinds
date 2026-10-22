package de.kaboomstudios.kaboomkeybinds.core;

/**
 * The two keys of one key mapping. {@code null} means that a slot is unbound.
 *
 * <p>The key type is generic so that this class stays free of Minecraft classes. The client layer uses Minecraft's
 * key objects and maps Minecraft's "unknown" key to {@code null} before calling in here, otherwise two unbound
 * mappings would look like a conflict.
 *
 * <p>The record is immutable. Changing a slot returns a new pair and applies the one rule that keeps both slots
 * consistent: a key can only occupy one slot of the same mapping. Binding a key that already sits in the other slot
 * clears that other slot, because the game would otherwise trigger the mapping twice for one key press.
 *
 * @param primary   the vanilla key, or {@code null} if unbound
 * @param secondary the alternative key, or {@code null} if unbound
 */
public record KeyPair<K>(K primary, K secondary) {
    /** A pair with both slots unbound. */
    public static <K> KeyPair<K> unbound() {
        return new KeyPair<>(null, null);
    }

    /** The key in the given slot, or {@code null} if that slot is unbound. */
    public K get(KeySlot slot) {
        return slot == KeySlot.PRIMARY ? primary : secondary;
    }

    /** Whether the given slot holds a key. */
    public boolean isBound(KeySlot slot) {
        return get(slot) != null;
    }

    /**
     * Returns a pair with the given slot set to {@code key} ({@code null} unbinds the slot). If the key already
     * occupies the other slot, that other slot becomes unbound.
     */
    public KeyPair<K> with(KeySlot slot, K key) {
        K other = get(slot.other());
        if (key != null && key.equals(other)) {
            other = null;
        }
        return slot == KeySlot.PRIMARY ? new KeyPair<>(key, other) : new KeyPair<>(other, key);
    }

    /** Whether the key is bound in either slot. An unbound key ({@code null}) never matches. */
    public boolean contains(K key) {
        return key != null && (key.equals(primary) || key.equals(secondary));
    }

    /** The slot the key is bound in, or {@code null} if the key is not bound in this pair. */
    public KeySlot slotOf(K key) {
        if (key == null) {
            return null;
        }
        if (key.equals(primary)) {
            return KeySlot.PRIMARY;
        }
        if (key.equals(secondary)) {
            return KeySlot.SECONDARY;
        }
        return null;
    }

    /**
     * Conflict rule: two mappings conflict when any bound key of one equals any bound key of the other. This covers
     * primary against primary (the vanilla check), primary against secondary in both directions and secondary
     * against secondary.
     */
    public boolean sharesKeyWith(KeyPair<K> other) {
        return other.contains(primary) || other.contains(secondary);
    }
}
