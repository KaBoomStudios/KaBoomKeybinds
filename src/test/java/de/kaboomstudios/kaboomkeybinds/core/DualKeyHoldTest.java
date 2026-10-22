package de.kaboomstudios.kaboomkeybinds.core;

import static de.kaboomstudios.kaboomkeybinds.core.KeySlot.PRIMARY;
import static de.kaboomstudios.kaboomkeybinds.core.KeySlot.SECONDARY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DualKeyHoldTest {
    @Test
    void startsReleased() {
        DualKeyHold hold = new DualKeyHold();

        assertFalse(hold.isDown());
        assertFalse(hold.isDown(PRIMARY));
        assertFalse(hold.isDown(SECONDARY));
    }

    @Test
    void eitherKeyAloneHoldsTheMapping() {
        DualKeyHold primaryOnly = new DualKeyHold();
        assertTrue(primaryOnly.set(PRIMARY, true));
        assertTrue(primaryOnly.isDown());
        assertFalse(primaryOnly.set(PRIMARY, false));

        DualKeyHold secondaryOnly = new DualKeyHold();
        assertTrue(secondaryOnly.set(SECONDARY, true));
        assertTrue(secondaryOnly.isDown());
        assertFalse(secondaryOnly.set(SECONDARY, false));
    }

    @Test
    void releasingOneOfTwoHeldKeysKeepsTheMappingDown() {
        DualKeyHold hold = new DualKeyHold();
        hold.set(PRIMARY, true);
        hold.set(SECONDARY, true);

        assertTrue(hold.set(PRIMARY, false), "secondary key is still held");
        assertTrue(hold.isDown());
        assertFalse(hold.isDown(PRIMARY));
        assertTrue(hold.isDown(SECONDARY));

        assertFalse(hold.set(SECONDARY, false), "both keys released");
        assertFalse(hold.isDown());
    }

    @Test
    void releasingInTheOtherOrderWorksTheSame() {
        DualKeyHold hold = new DualKeyHold();
        hold.set(SECONDARY, true);
        hold.set(PRIMARY, true);

        assertTrue(hold.set(SECONDARY, false));
        assertFalse(hold.set(PRIMARY, false));
    }

    @Test
    void everyPressReportsDownEvenWhileTheOtherKeyIsHeld() {
        DualKeyHold hold = new DualKeyHold();
        hold.set(PRIMARY, true);

        assertTrue(hold.set(SECONDARY, true));
        assertTrue(hold.set(PRIMARY, true), "repeated press of a held key");
    }

    @Test
    void releasingAKeyThatWasNotHeldIsHarmless() {
        DualKeyHold hold = new DualKeyHold();

        assertFalse(hold.set(SECONDARY, false));
        hold.set(PRIMARY, true);
        assertTrue(hold.set(SECONDARY, false), "primary is still held");
    }

    @Test
    void releaseAllForgetsBothKeys() {
        DualKeyHold hold = new DualKeyHold();
        hold.set(PRIMARY, true);
        hold.set(SECONDARY, true);

        hold.releaseAll();

        assertFalse(hold.isDown());
        assertFalse(hold.isDown(PRIMARY));
        assertFalse(hold.isDown(SECONDARY));
    }
}
