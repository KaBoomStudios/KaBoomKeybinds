package de.kaboomstudios.kaboomkeybinds.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeySelection;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeys;
import de.kaboomstudios.kaboomkeybinds.client.ControlsMenu;
import de.kaboomstudios.kaboomkeybinds.client.KeyBindsListAccess;
import de.kaboomstudios.kaboomkeybinds.core.KeyRowLayout;
import java.util.Arrays;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the key binds screen bind alternative keys the same way it binds primary keys.
 *
 * <p>Vanilla remembers the mapping that waits for a key in {@code selectedKey} and takes the next key press or
 * mouse click for it. The alternative key gets a second such state here. While it is set, key presses and mouse
 * clicks are taken for the alternative key and the vanilla handling is skipped, so Escape clears the alternative
 * key instead of closing the screen, exactly as it clears a primary key.
 *
 * <p>Changes to primary keys made in this screen go through the bridge as well, so a key that is bound as primary
 * key leaves the alternative slot of the same mapping, and "Reset All" clears every alternative key.
 */
@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin extends OptionsSubScreen implements AlternativeKeySelection {
    @Shadow
    private KeyBindsList keyBindsList;

    @Shadow
    public long lastKeySelection;

    @Unique
    private @Nullable KeyMapping kaboomkeybinds$selectedAlternative;

    private KeyBindsScreenMixin() {
        super(null, null, null);
    }

    /** The headings need a line between the title and the list, so the header grows before the list is laid out. */
    @Inject(method = "addContents", at = @At("HEAD"))
    private void makeRoomForColumnHeadings(CallbackInfo ci) {
        this.layout.setHeaderHeight(ControlsMenu.HEADER_HEIGHT);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void bindAlternativeMouseButton(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        KeyMapping mapping = this.kaboomkeybinds$selectedAlternative;
        if (mapping != null) {
            AlternativeKeys.setAlternative(mapping, InputConstants.Type.MOUSE.getOrCreate(event.button()));
            this.kaboomkeybinds$selectedAlternative = null;
            this.keyBindsList.resetMappingAndUpdateButtons();
            cir.setReturnValue(true);
        }
    }

    /**
     * Binds the pressed key as alternative key, Escape unbinds it. The time stamp is set like vanilla does for the
     * primary key, so the keyboard handler does not also treat the key as a global shortcut (for example F11).
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void bindAlternativeKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        KeyMapping mapping = this.kaboomkeybinds$selectedAlternative;
        if (mapping != null) {
            if (event.isEscape()) {
                AlternativeKeys.clearAlternative(mapping);
            } else {
                AlternativeKeys.setAlternative(mapping, InputConstants.getKey(event));
            }
            this.kaboomkeybinds$selectedAlternative = null;
            this.lastKeySelection = Util.getMillis();
            this.keyBindsList.resetMappingAndUpdateButtons();
            cir.setReturnValue(true);
        }
    }

    /**
     * Vanilla sets the primary key directly. The bridge applies the same key afterwards so that an alternative key
     * equal to the new primary key is unbound and the file is kept in sync. The original call stays in place for
     * other mods that hook into it.
     */
    @WrapOperation(
            method = {"mouseClicked", "keyPressed"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;setKey(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V"))
    private void bindPrimaryKeyThroughBridge(KeyMapping mapping, InputConstants.Key key, Operation<Void> original) {
        original.call(mapping, key);
        AlternativeKeys.setPrimary(mapping, key);
    }

    /**
     * "Reset All" is the first button built in {@code addFooter}. Its handler is wrapped so that all alternative keys
     * are cleared before vanilla resets the primary keys and refreshes the list.
     */
    @ModifyArg(
            method = "addFooter",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;"
                            + "Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;",
                    ordinal = 0),
            index = 1)
    private Button.OnPress clearAlternativeKeysOnResetAll(Button.OnPress resetAll) {
        return button -> {
            AlternativeKeys.clearAll(Arrays.asList(this.options.keyMappings));
            resetAll.onPress(button);
        };
    }

    /**
     * Draws the column headings centred above the two key buttons, in the line the larger header provides. Lists
     * without key columns, which the Controlling mod shows in the same screen, get no headings.
     */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void drawColumnHeadings(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (this.keyBindsList == null || !((KeyBindsListAccess) this.keyBindsList).kaboomkeybinds$showsKeyColumns()) {
            return;
        }
        int buttonWidth = KeyRowLayout.keyButtonWidth(this.keyBindsList.getRowWidth());
        int rowRight = this.keyBindsList.getRowRight();
        int y = this.keyBindsList.getY() - ControlsMenu.HEADING_OFFSET_ABOVE_LIST;
        graphics.centeredText(this.font, ControlsMenu.PRIMARY_HEADING,
                KeyRowLayout.primaryButtonX(rowRight, buttonWidth) + buttonWidth / 2, y, ControlsMenu.HEADING_COLOR);
        graphics.centeredText(this.font, ControlsMenu.ALTERNATIVE_HEADING,
                KeyRowLayout.alternativeButtonX(rowRight, buttonWidth) + buttonWidth / 2, y, ControlsMenu.HEADING_COLOR);
    }

    @Override
    public @Nullable KeyMapping kaboomkeybinds$selectedAlternative() {
        return this.kaboomkeybinds$selectedAlternative;
    }

    @Override
    public void kaboomkeybinds$selectAlternative(@Nullable KeyMapping mapping) {
        this.kaboomkeybinds$selectedAlternative = mapping;
    }
}
