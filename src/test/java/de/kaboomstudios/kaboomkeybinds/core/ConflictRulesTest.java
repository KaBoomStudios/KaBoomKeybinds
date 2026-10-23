package de.kaboomstudios.kaboomkeybinds.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConflictRulesTest {
    private static final String A = "a";
    private static final String B = "b";
    private static final String C = "c";

    @Test
    void twoDefaultPrimaryKeysAreNotMarkedLikeInVanilla() {
        KeyPair<String> movement = new KeyPair<>(A, null);
        KeyPair<String> debug = new KeyPair<>(A, null);

        assertFalse(ConflictRules.primaryConflictShown(movement, A, debug, A));
        assertFalse(ConflictRules.primaryConflictShown(debug, A, movement, A));
    }

    @Test
    void aChangedPrimaryKeyIsMarkedAgainstADefaultOne() {
        KeyPair<String> changed = new KeyPair<>(A, null);
        KeyPair<String> untouched = new KeyPair<>(A, null);

        assertTrue(ConflictRules.primaryConflictShown(changed, B, untouched, A), "the changed key sees the conflict");
        assertTrue(ConflictRules.primaryConflictShown(untouched, A, changed, B), "and so does the default one");
    }

    @Test
    void anAlternativeKeyOnTheSameMappingDoesNotHideADefaultConflict() {
        // The mapping is not "default" any more because of its alternative key, but its primary key still is.
        KeyPair<String> withAlternative = new KeyPair<>(A, C);
        KeyPair<String> debug = new KeyPair<>(A, null);

        assertFalse(ConflictRules.primaryConflictShown(withAlternative, A, debug, A));
        assertFalse(ConflictRules.primaryConflictShown(debug, A, withAlternative, A));
    }

    @Test
    void aPrimaryKeyIsMarkedAgainstTheAlternativeKeyOfAnotherMapping() {
        KeyPair<String> self = new KeyPair<>(A, null);
        KeyPair<String> other = new KeyPair<>(B, A);

        assertTrue(ConflictRules.primaryConflictShown(self, A, other, B), "alternative keys have no default");
    }

    @Test
    void anUnboundPrimaryKeyNeverConflicts() {
        KeyPair<String> unbound = new KeyPair<>(null, A);
        KeyPair<String> other = new KeyPair<>(null, null);

        assertFalse(ConflictRules.primaryConflictShown(unbound, A, other, null));
    }

    @Test
    void differentKeysDoNotConflict() {
        assertFalse(ConflictRules.primaryConflictShown(new KeyPair<>(A, null), B, new KeyPair<>(B, C), B));
    }

    @Test
    void theRuleWithoutDefaultsMarksDefaultDuplicatesToo() {
        KeyPair<String> movement = new KeyPair<>(A, null);
        KeyPair<String> debug = new KeyPair<>(A, null);

        assertTrue(ConflictRules.primaryConflict(movement, debug), "menus like Controlling show every duplicate");
        assertTrue(ConflictRules.primaryConflict(movement, new KeyPair<>(B, A)), "including alternative keys");
    }

    @Test
    void theRuleWithoutDefaultsLooksAtThePrimaryKeyOnly() {
        KeyPair<String> self = new KeyPair<>(A, C);

        assertFalse(ConflictRules.primaryConflict(self, new KeyPair<>(C, null)),
                "a conflict of the alternative key belongs to the alternative button");
        assertFalse(ConflictRules.primaryConflict(new KeyPair<>(null, A), new KeyPair<>(null, null)),
                "an unbound primary key never conflicts");
        assertFalse(ConflictRules.primaryConflict(self, new KeyPair<>(B, null)));
    }

    @Test
    void theAlternativeKeyIsMarkedAgainstPrimaryAndAlternativeKeys() {
        KeyPair<String> self = new KeyPair<>(B, A);

        assertTrue(ConflictRules.secondaryConflictShown(self, new KeyPair<>(A, null)), "against a primary key");
        assertTrue(ConflictRules.secondaryConflictShown(self, new KeyPair<>(C, A)), "against an alternative key");
        assertFalse(ConflictRules.secondaryConflictShown(self, new KeyPair<>(C, null)), "not against other keys");
    }

    @Test
    void anUnboundAlternativeKeyNeverConflicts() {
        assertFalse(ConflictRules.secondaryConflictShown(new KeyPair<>(A, null), new KeyPair<>(A, null)));
        assertFalse(ConflictRules.secondaryConflictShown(new KeyPair<>(A, null), new KeyPair<>(null, null)));
    }
}
