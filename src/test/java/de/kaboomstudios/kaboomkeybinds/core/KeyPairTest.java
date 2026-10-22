package de.kaboomstudios.kaboomkeybinds.core;

import static de.kaboomstudios.kaboomkeybinds.core.KeySlot.PRIMARY;
import static de.kaboomstudios.kaboomkeybinds.core.KeySlot.SECONDARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Keys are plain strings here. The rules only rely on equals, exactly like with Minecraft's key objects. */
class KeyPairTest {
    private static final KeyPair<String> INVENTORY = new KeyPair<>("key.keyboard.e", "key.mouse.5");

    @Test
    void slotsCanBeReadIndividually() {
        assertEquals("key.keyboard.e", INVENTORY.get(PRIMARY));
        assertEquals("key.mouse.5", INVENTORY.get(SECONDARY));
        assertTrue(INVENTORY.isBound(PRIMARY));
        assertTrue(INVENTORY.isBound(SECONDARY));

        KeyPair<String> unbound = KeyPair.unbound();
        assertNull(unbound.get(PRIMARY));
        assertFalse(unbound.isBound(SECONDARY));
    }

    @Test
    void bindingTheSecondarySlotKeepsThePrimary() {
        KeyPair<String> pair = new KeyPair<>("key.keyboard.e", null).with(SECONDARY, "key.mouse.5");

        assertEquals(INVENTORY, pair);
    }

    @Test
    void bindingTheSecondaryToThePrimaryKeyClearsThePrimary() {
        KeyPair<String> pair = new KeyPair<>("key.keyboard.e", null).with(SECONDARY, "key.keyboard.e");

        assertEquals(new KeyPair<>(null, "key.keyboard.e"), pair);
    }

    @Test
    void bindingThePrimaryToTheSecondaryKeyClearsTheSecondary() {
        KeyPair<String> pair = INVENTORY.with(PRIMARY, "key.mouse.5");

        assertEquals(new KeyPair<>("key.mouse.5", null), pair);
    }

    @Test
    void unbindingASlotNeverTouchesTheOther() {
        assertEquals(new KeyPair<>("key.keyboard.e", null), INVENTORY.with(SECONDARY, null));
        assertEquals(new KeyPair<>(null, "key.mouse.5"), INVENTORY.with(PRIMARY, null));
    }

    @Test
    void containsAndSlotOfIgnoreUnboundKeys() {
        assertTrue(INVENTORY.contains("key.keyboard.e"));
        assertTrue(INVENTORY.contains("key.mouse.5"));
        assertFalse(INVENTORY.contains("key.keyboard.q"));
        assertFalse(INVENTORY.contains(null));
        assertFalse(KeyPair.<String>unbound().contains(null));

        assertEquals(PRIMARY, INVENTORY.slotOf("key.keyboard.e"));
        assertEquals(SECONDARY, INVENTORY.slotOf("key.mouse.5"));
        assertNull(INVENTORY.slotOf("key.keyboard.q"));
        assertNull(INVENTORY.slotOf(null));
    }

    @Test
    void conflictsAreFoundInEveryDirection() {
        KeyPair<String> primaryHitsPrimary = new KeyPair<>("key.keyboard.e", null);
        KeyPair<String> primaryHitsSecondary = new KeyPair<>("key.mouse.5", null);
        KeyPair<String> secondaryHitsPrimary = new KeyPair<>("key.keyboard.q", "key.keyboard.e");
        KeyPair<String> secondaryHitsSecondary = new KeyPair<>("key.keyboard.q", "key.mouse.5");
        KeyPair<String> noOverlap = new KeyPair<>("key.keyboard.q", "key.mouse.4");

        assertTrue(INVENTORY.sharesKeyWith(primaryHitsPrimary));
        assertTrue(INVENTORY.sharesKeyWith(primaryHitsSecondary));
        assertTrue(INVENTORY.sharesKeyWith(secondaryHitsPrimary));
        assertTrue(INVENTORY.sharesKeyWith(secondaryHitsSecondary));
        assertTrue(secondaryHitsPrimary.sharesKeyWith(INVENTORY));
        assertFalse(INVENTORY.sharesKeyWith(noOverlap));
    }

    @Test
    void unboundSlotsNeverConflict() {
        KeyPair<String> unbound = KeyPair.unbound();
        KeyPair<String> onlySecondary = new KeyPair<>(null, "key.mouse.5");

        assertFalse(unbound.sharesKeyWith(KeyPair.unbound()));
        assertFalse(unbound.sharesKeyWith(onlySecondary));
        assertFalse(onlySecondary.sharesKeyWith(new KeyPair<>(null, "key.mouse.4")));
    }
}
