package de.kaboomstudios.kaboomkeybinds.client;

import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.CommonColors;

/**
 * Texts and layout constants of the controls menu additions, shared by the mixins on the key binds screen and its
 * rows. The markers for conflicts and for the waiting state copy the vanilla style of the primary key button, so
 * both buttons of a row look the same.
 */
public final class ControlsMenu {
    /** Heading above the primary key column. */
    public static final Component PRIMARY_HEADING = Component.translatable("kaboomkeybinds.controls.primary");
    /** Heading above the alternative key column. */
    public static final Component ALTERNATIVE_HEADING = Component.translatable("kaboomkeybinds.controls.alternative");
    /** Narration name of the alternative key button, takes the mapping name as argument. */
    private static final String ALTERNATIVE_NARRATION = "kaboomkeybinds.controls.alternative.narration";

    /** The room the column headings need in the header: a line of their own below the header contents. */
    public static final int HEADING_LINE_HEIGHT = 15;
    /**
     * Header height of the key binds screen. The vanilla header (33 pixels) only holds the title. The column headings
     * need a line of their own below the title and above the list separator.
     */
    public static final int HEADER_HEIGHT = HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT + HEADING_LINE_HEIGHT;
    /** Distance from the top of the list to the top of the heading text: 2 pixel separator, 1 pixel gap, 9 pixel font. */
    public static final int HEADING_OFFSET_ABOVE_LIST = 12;
    public static final int HEADING_COLOR = CommonColors.LIGHT_GRAY;

    private ControlsMenu() {
    }

    /** The label of the alternative key button: the name Minecraft gives the key, "Not Bound" if there is none. */
    public static Component alternativeLabel(KeyMapping mapping) {
        return AlternativeKeys.get(mapping).getDisplayName();
    }

    /**
     * Narration of the alternative key button, built like the vanilla narration of the primary key button but for
     * "alternative key for <name>" instead of the bare name.
     */
    public static MutableComponent alternativeNarration(KeyMapping mapping, Component name,
            Supplier<MutableComponent> defaultNarration) {
        Component alternativeName = Component.translatable(ALTERNATIVE_NARRATION, name);
        return AlternativeKeys.of(mapping).kaboomkeybinds$hasAlternativeKey()
                ? Component.translatable("narrator.controls.bound", alternativeName, defaultNarration.get())
                : Component.translatable("narrator.controls.unbound", alternativeName);
    }

    /** The vanilla conflict marker: yellow brackets around the white key name. */
    public static MutableComponent markConflict(Component message) {
        return Component.literal("[ ")
                .append(message.copy().withStyle(ChatFormatting.WHITE))
                .append(" ]")
                .withStyle(ChatFormatting.YELLOW);
    }

    /** The vanilla waiting marker while a key is being bound: yellow angle brackets around the underlined key name. */
    public static MutableComponent markSelecting(Component message) {
        return Component.literal("> ")
                .append(message.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                .append(" <")
                .withStyle(ChatFormatting.YELLOW);
    }

    /** The vanilla conflict tooltip listing the names of the other mappings that use the same key. */
    public static Tooltip conflictTooltip(Component otherMappingNames) {
        return Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", otherMappingNames));
    }
}
