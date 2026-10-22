package de.kaboomstudios.kaboomkeybinds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyRowLayoutTest {
    /** Window width in GUI pixels at 1080p with GUI scale 4, a common setting. */
    private static final int COMMON_LIST_WIDTH = 480;
    /** Smallest GUI size Minecraft allows. */
    private static final int MINIMUM_LIST_WIDTH = 320;

    @Test
    void shortNamesKeepTheVanillaRowWidth() {
        assertEquals(KeyRowLayout.VANILLA_ROW_WIDTH, KeyRowLayout.rowWidth(0, COMMON_LIST_WIDTH));
        assertEquals(KeyRowLayout.VANILLA_ROW_WIDTH, KeyRowLayout.rowWidth(100, COMMON_LIST_WIDTH));
    }

    @Test
    void longNamesWidenTheRowByTheMissingAmount() {
        int fits = KeyRowLayout.nameWidth(KeyRowLayout.VANILLA_ROW_WIDTH);
        assertEquals(KeyRowLayout.VANILLA_ROW_WIDTH, KeyRowLayout.rowWidth(fits, COMMON_LIST_WIDTH));
        assertEquals(KeyRowLayout.VANILLA_ROW_WIDTH + 1, KeyRowLayout.rowWidth(fits + 1, COMMON_LIST_WIDTH));
        assertEquals(KeyRowLayout.VANILLA_ROW_WIDTH + 30, KeyRowLayout.rowWidth(fits + 30, COMMON_LIST_WIDTH));
    }

    @Test
    void theNameFitsExactlyWhenTheRowWasWidenedForIt() {
        int rowWidth = KeyRowLayout.rowWidth(200, COMMON_LIST_WIDTH);
        assertEquals(200, KeyRowLayout.nameWidth(rowWidth));
    }

    @Test
    void theListWidthCapsTheRow() {
        int rowWidth = KeyRowLayout.rowWidth(400, COMMON_LIST_WIDTH);
        assertEquals(COMMON_LIST_WIDTH - KeyRowLayout.LIST_MARGIN, rowWidth);
        assertTrue(KeyRowLayout.nameWidth(rowWidth) < 400, "the name no longer fits and has to be clipped");
    }

    @Test
    void regularButtonsDownToTheVanillaWidth() {
        assertEquals(KeyRowLayout.KEY_BUTTON_WIDTH, KeyRowLayout.keyButtonWidth(KeyRowLayout.VANILLA_ROW_WIDTH));
        assertEquals(KeyRowLayout.KEY_BUTTON_WIDTH, KeyRowLayout.keyButtonWidth(500));
    }

    @Test
    void narrowButtonsBelowTheVanillaWidth() {
        int rowWidth = KeyRowLayout.rowWidth(120, MINIMUM_LIST_WIDTH);
        assertEquals(MINIMUM_LIST_WIDTH - KeyRowLayout.LIST_MARGIN, rowWidth);
        assertEquals(KeyRowLayout.NARROW_KEY_BUTTON_WIDTH, KeyRowLayout.keyButtonWidth(rowWidth));
        assertTrue(KeyRowLayout.nameWidth(rowWidth) >= 100, "the narrow buttons leave room for the name");
    }

    @Test
    void theRowNeverShrinksBelowTheMinimum() {
        int rowWidth = KeyRowLayout.rowWidth(50, 100);
        assertTrue(rowWidth > 100, "a tiny list still gets a usable row");
        assertEquals(KeyRowLayout.NARROW_KEY_BUTTON_WIDTH, KeyRowLayout.keyButtonWidth(rowWidth));
        assertTrue(KeyRowLayout.nameWidth(rowWidth) >= 40, "even then the name keeps a few letters");
    }

    @Test
    void buttonsSitRightOfEachOtherWithTheVanillaGap() {
        int rowRight = 400;
        int width = KeyRowLayout.KEY_BUTTON_WIDTH;
        int alternativeX = KeyRowLayout.alternativeButtonX(rowRight, width);
        int primaryX = KeyRowLayout.primaryButtonX(rowRight, width);
        int resetX = rowRight - KeyRowLayout.RESET_BUTTON_RIGHT_INSET;

        assertEquals(resetX, alternativeX + width + KeyRowLayout.BUTTON_GAP);
        assertEquals(alternativeX, primaryX + width + KeyRowLayout.BUTTON_GAP);
    }

    @Test
    void theNameEndsBeforeTheConflictStripe() {
        int rowWidth = KeyRowLayout.VANILLA_ROW_WIDTH;
        int rowLeft = 0;
        int primaryX = KeyRowLayout.primaryButtonX(rowLeft + rowWidth, KeyRowLayout.keyButtonWidth(rowWidth));
        int nameEnd = rowLeft + KeyRowLayout.CONTENT_PADDING + KeyRowLayout.nameWidth(rowWidth);

        assertEquals(primaryX - KeyRowLayout.NAME_TO_BUTTON_GAP, nameEnd);
    }
}
