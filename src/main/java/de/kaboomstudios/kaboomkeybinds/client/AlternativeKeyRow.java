package de.kaboomstudios.kaboomkeybinds.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.kaboomstudios.kaboomkeybinds.core.ConflictRules;
import de.kaboomstudios.kaboomkeybinds.core.KeyPair;
import de.kaboomstudios.kaboomkeybinds.core.KeyRowLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * The alternative key button of one row in the key binds list, with everything the row needs to show it.
 *
 * <p>Two kinds of rows get this button: the vanilla row and the row of the Controlling mod, which replaces the vanilla
 * list with its own. Both hold a name, a primary key button and a reset button, and both place the primary button
 * left of the reset button when they are drawn. The mixins on the two rows hook into those places and hand over to
 * this class, so the button behaves the same in either menu.
 *
 * <p>The button sits between the primary key button and the reset button and behaves like the primary button: it
 * shows the name Minecraft gives the bound key, the waiting marker while a key is being chosen and the conflict
 * marker with a tooltip when another mapping uses the same key. Both key buttons share the width the row layout
 * assigns them, and names that do not fit next to the buttons are clipped with an ellipsis.
 */
public final class AlternativeKeyRow {
    private final KeyBindsList list;
    private final KeyMapping mapping;
    private final Button primaryButton;
    private final Button alternativeButton;
    /** Whether the alternative key conflicts with another mapping, set with the label of the button. */
    private boolean conflict;

    /**
     * Creates the button for a row whose own buttons exist already, and gives it its label.
     *
     * @param list          the list the row belongs to, refreshed when the button is clicked
     * @param mapping       the mapping shown in the row
     * @param name          the name the row shows for the mapping, used for narration
     * @param primaryButton the button of the primary key, which the alternative button follows
     */
    public AlternativeKeyRow(KeyBindsList list, KeyMapping mapping, Component name, Button primaryButton) {
        this.list = list;
        this.mapping = mapping;
        this.primaryButton = primaryButton;
        this.alternativeButton = Button.builder(ControlsMenu.alternativeLabel(mapping), button -> {
                    this.selection().kaboomkeybinds$selectAlternative(mapping);
                    list.resetMappingAndUpdateButtons();
                })
                .bounds(0, 0, KeyRowLayout.KEY_BUTTON_WIDTH, primaryButton.getHeight())
                .createNarration(defaultNarration -> ControlsMenu.alternativeNarration(mapping, name, defaultNarration))
                .build();
        this.refresh();
    }

    public Button button() {
        return this.alternativeButton;
    }

    /**
     * The widgets of the row with the alternative button inserted right after the primary button, so mouse handling,
     * keyboard navigation and narration visit it between the primary and the reset button. Other widgets of the row,
     * for example ones another mod added, keep their place.
     */
    public List<GuiEventListener> withButton(List<? extends GuiEventListener> children) {
        return insertAfterPrimary(children, this.alternativeButton);
    }

    /** The narrated widgets of the row with the alternative button after the primary button, see {@link #withButton}. */
    public List<NarratableEntry> withNarratedButton(List<? extends NarratableEntry> narratables) {
        return insertAfterPrimary(narratables, this.alternativeButton);
    }

    private <T> List<T> insertAfterPrimary(List<? extends T> widgets, T button) {
        List<T> result = new ArrayList<>(widgets.size() + 1);
        result.addAll(widgets);
        int primary = result.indexOf(this.primaryButton);
        result.add(primary < 0 ? result.size() : primary + 1, button);
        return result;
    }

    /** Both key buttons take the width the layout assigns to a row of this width (regular or narrow). */
    public void sizeKeyButtons(int rowWidth) {
        int width = KeyRowLayout.keyButtonWidth(rowWidth);
        this.primaryButton.setWidth(width);
        this.alternativeButton.setWidth(width);
    }

    /** Where the primary button goes: the row places it left of the reset button, and it moves left by one button. */
    public int primaryButtonX(int x) {
        return x - KeyRowLayout.BUTTON_GAP - this.alternativeButton.getWidth();
    }

    /**
     * The name of the row clipped with an ellipsis to the space left of the buttons, or {@code null} if it fits. The
     * primary button must already have its position.
     */
    public @Nullable FormattedCharSequence clippedName(Font font, Component name, int x) {
        int available = this.primaryButton.getX() - KeyRowLayout.NAME_TO_BUTTON_GAP - x;
        return font.width(name) > available ? ComponentRenderUtils.clipText(name, font, available) : null;
    }

    /**
     * Whether the stripe left of the buttons shows. It marks a conflict on either button, the row decides about the
     * primary one.
     */
    public boolean showsConflictStripe(boolean primaryConflict) {
        return primaryConflict || this.conflict;
    }

    /** Places the button next to the primary button, which the row has positioned by now, and draws it. */
    public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.alternativeButton.setPosition(this.primaryButton.getRight() + KeyRowLayout.BUTTON_GAP, this.primaryButton.getY());
        this.alternativeButton.extractRenderState(graphics, mouseX, mouseY, a);
    }

    /**
     * Label, conflict marker, tooltip and waiting marker of the button, mirroring what vanilla does for the primary
     * button. Called whenever the row is refreshed.
     */
    public void refresh() {
        Button button = this.alternativeButton;
        InputConstants.Key alternative = AlternativeKeys.get(this.mapping);
        button.setMessage(alternative.getDisplayName());

        boolean conflict = false;
        if (!InputConstants.UNKNOWN.equals(alternative)) {
            KeyPair<InputConstants.Key> keys = AlternativeKeys.keys(this.mapping);
            MutableComponent otherNames = Component.empty();
            for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
                if (other != this.mapping && ConflictRules.secondaryConflictShown(keys, AlternativeKeys.keys(other))) {
                    if (conflict) {
                        otherNames.append(", ");
                    }
                    conflict = true;
                    otherNames.append(Component.translatable(other.getName()));
                }
            }
            if (conflict) {
                button.setMessage(ControlsMenu.markConflict(button.getMessage()));
                button.setTooltip(ControlsMenu.conflictTooltip(otherNames));
            }
        }
        if (!conflict) {
            button.setTooltip(null);
        }
        this.conflict = conflict;

        if (this.selection().kaboomkeybinds$selectedAlternative() == this.mapping) {
            button.setMessage(ControlsMenu.markSelecting(button.getMessage()));
        }
    }

    private AlternativeKeySelection selection() {
        return (AlternativeKeySelection) ((KeyBindsListAccess) this.list).kaboomkeybinds$screen();
    }
}
