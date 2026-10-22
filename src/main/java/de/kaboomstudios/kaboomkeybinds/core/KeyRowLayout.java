package de.kaboomstudios.kaboomkeybinds.core;

/**
 * Geometry of one row in the key binds list, as plain arithmetic without any Minecraft class.
 *
 * <p>A vanilla row is 340 pixels wide and holds the name on the left and two buttons on the right: the key button
 * (75 pixels) and the reset button (50 pixels). This mod replaces the single key button with two buttons of the
 * same width, so a row needs more space. The rules here keep the vanilla look where possible:
 *
 * <ul>
 * <li>A row is never narrower than the vanilla row, so short names leave the familiar amount of space.
 * <li>If the longest name does not fit next to the buttons, the row grows by the missing amount.
 * <li>The row never grows beyond what the list can show. The list centres the row and draws the scrollbar to its
 * right, so a margin stays free on both sides.
 * <li>If the window forces the row below the vanilla width, both key buttons become narrower so that the name
 * keeps some room. A row never shrinks below a minimum that still fits the narrow buttons and a short name.
 * </ul>
 *
 * <p>All positions are measured from the right edge of the row, as vanilla does: the reset button sits at a fixed
 * distance from the right edge, the key buttons follow to its left with a fixed gap, and the name starts at the
 * left edge.
 */
public final class KeyRowLayout {
    /** Row width of the vanilla list. Rows are never narrower unless the window forces it. */
    public static final int VANILLA_ROW_WIDTH = 340;
    /** Width of each of the two key buttons. Vanilla uses 75 pixels for its single button. */
    public static final int KEY_BUTTON_WIDTH = 65;
    /** Width of each key button when the window is too narrow for the regular width. */
    public static final int NARROW_KEY_BUTTON_WIDTH = 55;
    /** Width of the vanilla reset button. */
    public static final int RESET_BUTTON_WIDTH = 50;
    /** Space between neighbouring buttons, as in vanilla. */
    public static final int BUTTON_GAP = 5;
    /**
     * Distance from the right edge of the row to the left edge of the reset button. Vanilla places the reset button
     * 10 pixels left of the scrollbar, and the scrollbar starts 8 pixels right of the row (2 pixels gap plus the 6
     * pixel wide bar, measured by vanilla from the right edge of the row). 50 + 10 - 8 = 52.
     */
    public static final int RESET_BUTTON_RIGHT_INSET = 52;
    /** Left padding of the row content: the name starts this far right of the left edge of the row. */
    public static final int CONTENT_PADDING = 2;
    /**
     * Space between the end of the name and the first key button. Vanilla draws a 3 pixel conflict stripe 6 pixels
     * left of the key button, so the name has to end 2 pixels before the stripe.
     */
    public static final int NAME_TO_BUTTON_GAP = 8;
    /**
     * Space the list needs beyond the row on both sides: the scrollbar ends 14 pixels right of the row (2 pixels gap
     * plus 6 pixels bar plus the 6 pixels vanilla adds to its position), and the row is centred, so the same amount
     * stays free on the left.
     */
    public static final int LIST_MARGIN = 28;
    /** Smallest space a name gets, so that at least a few letters stay readable in tiny windows. */
    private static final int MIN_NAME_WIDTH = 40;

    private KeyRowLayout() {
    }

    /**
     * The row width for a list of the given width whose longest name is {@code maxNameWidth} pixels wide.
     *
     * @param maxNameWidth width of the longest name in the list, in pixels
     * @param listWidth    width of the whole list widget, usually the window width in GUI pixels
     */
    public static int rowWidth(int maxNameWidth, int listWidth) {
        int preferred = Math.max(VANILLA_ROW_WIDTH, fixedWidth(KEY_BUTTON_WIDTH) + maxNameWidth);
        int available = listWidth - LIST_MARGIN;
        return Math.max(Math.min(preferred, available), minimumRowWidth());
    }

    /** The width of both key buttons for a row of the given width: regular, or narrow below the vanilla row width. */
    public static int keyButtonWidth(int rowWidth) {
        return rowWidth < VANILLA_ROW_WIDTH ? NARROW_KEY_BUTTON_WIDTH : KEY_BUTTON_WIDTH;
    }

    /** The space available for the name in a row of the given width. */
    public static int nameWidth(int rowWidth) {
        return rowWidth - fixedWidth(keyButtonWidth(rowWidth));
    }

    /** The left edge of the alternative key button for a row that ends at {@code rowRight}. */
    public static int alternativeButtonX(int rowRight, int keyButtonWidth) {
        return rowRight - RESET_BUTTON_RIGHT_INSET - BUTTON_GAP - keyButtonWidth;
    }

    /** The left edge of the primary key button for a row that ends at {@code rowRight}. */
    public static int primaryButtonX(int rowRight, int keyButtonWidth) {
        return alternativeButtonX(rowRight, keyButtonWidth) - BUTTON_GAP - keyButtonWidth;
    }

    /** Everything in a row that is not the name: padding, gap, both key buttons, the reset button and its inset. */
    private static int fixedWidth(int keyButtonWidth) {
        return CONTENT_PADDING + NAME_TO_BUTTON_GAP + 2 * keyButtonWidth + 2 * BUTTON_GAP + RESET_BUTTON_RIGHT_INSET;
    }

    private static int minimumRowWidth() {
        return fixedWidth(NARROW_KEY_BUTTON_WIDTH) + MIN_NAME_WIDTH;
    }
}
