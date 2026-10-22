package de.kaboomstudios.kaboomkeybinds.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeySelection;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeys;
import de.kaboomstudios.kaboomkeybinds.client.ControlsMenu;
import de.kaboomstudios.kaboomkeybinds.client.KeyBindsListAccess;
import de.kaboomstudios.kaboomkeybinds.client.KeyEntryAccess;
import de.kaboomstudios.kaboomkeybinds.core.ConflictRules;
import de.kaboomstudios.kaboomkeybinds.core.KeyPair;
import de.kaboomstudios.kaboomkeybinds.core.KeyRowLayout;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the alternative key button to every row of the key binds list.
 *
 * <p>A vanilla row holds the name, the key button and the reset button. The new button sits between the key
 * button and the reset button and behaves like the key button: it shows the name Minecraft gives the bound key,
 * the waiting marker while a key is being chosen and the conflict marker with a tooltip when another mapping uses
 * the same key. Both key buttons share the width the row layout assigns them, and names that do not fit next to
 * the buttons are clipped with an ellipsis instead of running into them.
 *
 * <p>Each button marks the conflicts of its own key, following {@link ConflictRules}; the stripe at the left of the
 * buttons shows for either.
 */
@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyEntryMixin extends KeyBindsList.Entry implements KeyEntryAccess {
    /** The list this row belongs to: the field the compiler generates for the enclosing instance of the inner class. */
    @Shadow
    @Final
    KeyBindsList this$0;

    @Shadow
    @Final
    private KeyMapping key;

    @Shadow
    @Final
    private Button changeButton;

    @Shadow
    @Final
    private Button resetButton;

    @Shadow
    private boolean hasCollision;

    @Unique
    private Button kaboomkeybinds$alternativeButton;

    @Unique
    private List<Button> kaboomkeybinds$children;

    /**
     * Creates the alternative key button once the vanilla buttons exist. Vanilla refreshes the row at the end of its
     * constructor, before this runs, so the new button is refreshed here as well.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addAlternativeButton(KeyBindsList list, KeyMapping mapping, Component name, CallbackInfo ci) {
        this.kaboomkeybinds$alternativeButton = Button.builder(ControlsMenu.alternativeLabel(mapping), button -> {
                    this.kaboomkeybinds$selection().kaboomkeybinds$selectAlternative(mapping);
                    list.resetMappingAndUpdateButtons();
                })
                .bounds(0, 0, KeyRowLayout.KEY_BUTTON_WIDTH, this.changeButton.getHeight())
                .createNarration(defaultNarration -> ControlsMenu.alternativeNarration(mapping, name, defaultNarration))
                .build();
        this.kaboomkeybinds$children = List.of(this.changeButton, this.kaboomkeybinds$alternativeButton, this.resetButton);
        this.kaboomkeybinds$refreshAlternativeButton();
    }

    /**
     * The reset button of the row is the second button built in the constructor. Its handler is wrapped so that the
     * alternative key is cleared before vanilla restores the default primary key and refreshes the list.
     */
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;"
                            + "Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;",
                    ordinal = 1),
            index = 1)
    private Button.OnPress clearAlternativeKeyOnReset(Button.OnPress reset) {
        return button -> {
            AlternativeKeys.clearAlternative(this.key);
            reset.onPress(button);
        };
    }

    /** Both key buttons take the width the layout assigns to a row of this width (regular or narrow). */
    @Inject(method = "extractContent", at = @At("HEAD"))
    private void sizeKeyButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        int width = KeyRowLayout.keyButtonWidth(this.getWidth());
        this.changeButton.setWidth(width);
        this.kaboomkeybinds$alternativeButton.setWidth(width);
    }

    /**
     * Vanilla places the key button directly left of the reset button. The second {@code setPosition} call in
     * {@code extractContent} is the one for the key button; it moves left by one gap and the alternative button.
     */
    @ModifyArg(
            method = "extractContent",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;setPosition(II)V", ordinal = 1),
            index = 0)
    private int moveKeyButtonLeft(int x) {
        return x - KeyRowLayout.BUTTON_GAP - this.kaboomkeybinds$alternativeButton.getWidth();
    }

    /** A name that would run into the buttons is clipped with an ellipsis, as other vanilla lists do. */
    @WrapOperation(
            method = "extractContent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;"
                            + "Lnet/minecraft/network/chat/Component;III)V"))
    private void clipLongName(GuiGraphicsExtractor graphics, Font font, Component name, int x, int y, int color,
            Operation<Void> original) {
        int available = this.changeButton.getX() - KeyRowLayout.NAME_TO_BUTTON_GAP - x;
        if (font.width(name) > available) {
            graphics.text(font, ComponentRenderUtils.clipText(name, font, available), x, y, color);
        } else {
            original.call(graphics, font, name, x, y, color);
        }
    }

    /** The alternative button follows the key button, which vanilla has positioned by now. */
    @Inject(method = "extractContent", at = @At("TAIL"))
    private void drawAlternativeButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        Button alternative = this.kaboomkeybinds$alternativeButton;
        alternative.setPosition(this.changeButton.getRight() + KeyRowLayout.BUTTON_GAP, this.changeButton.getY());
        alternative.extractRenderState(graphics, mouseX, mouseY, a);
    }

    /** The alternative button takes part in mouse handling and keyboard navigation between the two vanilla buttons. */
    @Inject(method = "children", at = @At("RETURN"), cancellable = true)
    private void includeAlternativeButton(CallbackInfoReturnable<List<? extends GuiEventListener>> cir) {
        if (this.kaboomkeybinds$children != null) {
            cir.setReturnValue(this.kaboomkeybinds$children);
        }
    }

    @Inject(method = "narratables", at = @At("RETURN"), cancellable = true)
    private void narrateAlternativeButton(CallbackInfoReturnable<List<? extends NarratableEntry>> cir) {
        if (this.kaboomkeybinds$children != null) {
            cir.setReturnValue(this.kaboomkeybinds$children);
        }
    }

    /**
     * Vanilla marks the primary button when {@code same()} reports a conflict and hides it when both mappings are
     * default. With the alternative keys included in {@code same()} and {@code isDefault()}, that would light up
     * for conflicts the alternative key has and would stop hiding the default duplicates Minecraft ships as soon as
     * a mapping has an alternative key. So the primary button applies the vanilla rule to the primary keys alone,
     * see {@link ConflictRules}. The vanilla result is not needed: it is a subset of this check.
     */
    @WrapOperation(
            method = "refreshEntry",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;same(Lnet/minecraft/client/KeyMapping;)Z"))
    private boolean primaryKeyConflicts(KeyMapping mapping, KeyMapping other, Operation<Boolean> original) {
        return ConflictRules.primaryConflictShown(
                AlternativeKeys.keys(mapping), mapping.getDefaultKey(),
                AlternativeKeys.keys(other), other.getDefaultKey());
    }

    /** Runs after every vanilla refresh. Skipped during the vanilla constructor, when the button does not exist yet. */
    @Inject(method = "refreshEntry", at = @At("TAIL"))
    private void refreshAlternativeButton(CallbackInfo ci) {
        if (this.kaboomkeybinds$alternativeButton != null) {
            this.kaboomkeybinds$refreshAlternativeButton();
        }
    }

    /**
     * Label, conflict marker, tooltip and waiting marker of the alternative button, mirroring what vanilla does for
     * the primary button in {@code refreshEntry}. A conflict of the alternative key also raises the stripe of the row.
     */
    @Unique
    private void kaboomkeybinds$refreshAlternativeButton() {
        Button button = this.kaboomkeybinds$alternativeButton;
        InputConstants.Key alternative = AlternativeKeys.get(this.key);
        button.setMessage(alternative.getDisplayName());

        boolean conflict = false;
        if (!InputConstants.UNKNOWN.equals(alternative)) {
            KeyPair<InputConstants.Key> keys = AlternativeKeys.keys(this.key);
            MutableComponent otherNames = Component.empty();
            for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
                if (other != this.key && ConflictRules.secondaryConflictShown(keys, AlternativeKeys.keys(other))) {
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
                this.hasCollision = true;
            }
        }
        if (!conflict) {
            button.setTooltip(null);
        }

        if (this.kaboomkeybinds$selection().kaboomkeybinds$selectedAlternative() == this.key) {
            button.setMessage(ControlsMenu.markSelecting(button.getMessage()));
        }
    }

    @Unique
    private AlternativeKeySelection kaboomkeybinds$selection() {
        return (AlternativeKeySelection) ((KeyBindsListAccess) this.this$0).kaboomkeybinds$screen();
    }

    @Override
    public KeyMapping kaboomkeybinds$mapping() {
        return this.key;
    }

    @Override
    public Button kaboomkeybinds$alternativeButton() {
        return this.kaboomkeybinds$alternativeButton;
    }
}
