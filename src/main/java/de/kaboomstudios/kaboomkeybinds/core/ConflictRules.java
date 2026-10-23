package de.kaboomstudios.kaboomkeybinds.core;

/**
 * Which conflicts the controls menu marks, as plain rules without any Minecraft class.
 *
 * <p>Vanilla marks a key when another mapping uses the same key, but hides the conflict when both keys are the
 * defaults of their mappings. Minecraft ships such duplicates on purpose (the F3 debug keys share letters with the
 * movement keys, for example) and they would otherwise light up the whole list. A player who changes a key wants
 * to see every clash, so the conflict appears as soon as one of the two keys is not at its default.
 *
 * <p>Alternative keys have no default, so a conflict with an alternative key is always shown, on both buttons
 * involved. The rules work per slot: each button of a row marks the conflicts of its own key.
 *
 * <p>Menus that replace the vanilla one may mark every duplicate, defaults included. The Controlling mod does, and its
 * primary button keeps that rule, extended by the alternative keys of the other mappings.
 */
public final class ConflictRules {
    private ConflictRules() {
    }

    /**
     * Whether the primary key of {@code self} visibly conflicts with a key of {@code other}.
     *
     * @param self         the keys of the mapping whose primary button is refreshed
     * @param selfDefault  the default primary key of that mapping
     * @param other        the keys of another mapping
     * @param otherDefault the default primary key of the other mapping
     */
    public static <K> boolean primaryConflictShown(KeyPair<K> self, K selfDefault, KeyPair<K> other, K otherDefault) {
        if (!primaryConflict(self, other)) {
            return false;
        }
        K key = self.primary();
        boolean selfAtDefault = key.equals(selfDefault);
        boolean otherAtDefault = key.equals(other.primary()) && key.equals(otherDefault);
        return !(selfAtDefault && otherAtDefault);
    }

    /**
     * Whether the primary key of {@code self} is one of the keys of {@code other}, whatever their defaults are. This is
     * the rule for menus that mark every duplicate.
     */
    public static <K> boolean primaryConflict(KeyPair<K> self, KeyPair<K> other) {
        return self.primary() != null && other.contains(self.primary());
    }

    /** Whether the alternative key of {@code self} conflicts with a key of {@code other}. Always shown when it does. */
    public static <K> boolean secondaryConflictShown(KeyPair<K> self, KeyPair<K> other) {
        return self.secondary() != null && other.contains(self.secondary());
    }
}
