package de.kaboomstudios.kaboomkeybinds.mixin.controlling;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeyRow;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeys;
import de.kaboomstudios.kaboomkeybinds.client.KeyEntryAccess;
import de.kaboomstudios.kaboomkeybinds.core.ConflictRules;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the alternative key button to the rows of the Controlling mod, which replaces the vanilla key binds list with
 * its own list and row class. Controlling's row is built like the vanilla one (name, primary key button, reset button,
 * conflict stripe), so the same {@link AlternativeKeyRow} is hooked into the same places as in the vanilla row.
 *
 * <p>The target is named as a string and the shadowed fields have Minecraft types, so the mod compiles without
 * Controlling. The row's constructor takes Controlling's list, which the handler receives as the vanilla list it
 * extends. The injectors of this configuration may miss a future Controlling version without stopping the game (see
 * {@code ControllingMixinPlugin}), so every handler checks that the button exists.
 *
 * <p>Controlling resets a row through its platform helper, which {@link ControllingPlatformHelperMixin} hooks, so the
 * reset button needs nothing here.
 */
@Mixin(targets = "com.blamejared.controlling.client.NewKeyBindsList$KeyEntry")
public abstract class ControllingKeyEntryMixin extends KeyBindsList.Entry implements KeyEntryAccess {
    @Shadow
    @Final
    private KeyMapping key;

    @Shadow
    @Final
    private Button btnChangeKeyBinding;

    @Unique
    private AlternativeKeyRow kaboomkeybinds$row;

    /** Controlling refreshes the row at the end of its constructor, before this runs, like vanilla does. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addAlternativeButton(@Coerce KeyBindsList list, KeyMapping mapping, Component name, CallbackInfo ci) {
        this.kaboomkeybinds$row = new AlternativeKeyRow(list, mapping, name, this.btnChangeKeyBinding);
    }

    @Inject(method = "extractContent", at = @At("HEAD"))
    private void sizeKeyButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        if (this.kaboomkeybinds$row != null) {
            this.kaboomkeybinds$row.sizeKeyButtons(this.getWidth());
        }
    }

    /** Controlling positions the reset button first and the key button second, left of it, as vanilla does. */
    @ModifyArg(
            method = "extractContent",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;setPosition(II)V", ordinal = 1),
            index = 0)
    private int moveKeyButtonLeft(int x) {
        return this.kaboomkeybinds$row != null ? this.kaboomkeybinds$row.primaryButtonX(x) : x;
    }

    @WrapOperation(
            method = "extractContent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;"
                            + "Lnet/minecraft/network/chat/Component;III)V"))
    private void clipLongName(GuiGraphicsExtractor graphics, Font font, Component name, int x, int y, int color,
            Operation<Void> original) {
        FormattedCharSequence clipped = this.kaboomkeybinds$row != null ? this.kaboomkeybinds$row.clippedName(font, name, x) : null;
        if (clipped != null) {
            graphics.text(font, clipped, x, y, color);
        } else {
            original.call(graphics, font, name, x, y, color);
        }
    }

    @ModifyExpressionValue(
            method = "extractContent",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/blamejared/controlling/client/NewKeyBindsList$KeyEntry;hasCollision:Z",
                    opcode = Opcodes.GETFIELD))
    private boolean markAlternativeConflictWithStripe(boolean primaryConflict) {
        return this.kaboomkeybinds$row != null ? this.kaboomkeybinds$row.showsConflictStripe(primaryConflict) : primaryConflict;
    }

    @Inject(method = "extractContent", at = @At("TAIL"))
    private void drawAlternativeButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        if (this.kaboomkeybinds$row != null) {
            this.kaboomkeybinds$row.extract(graphics, mouseX, mouseY, a);
        }
    }

    /** Controlling collects the widgets of its rows through an event other mods may add to; their widgets stay. */
    @Inject(method = "children", at = @At("RETURN"), cancellable = true)
    private void includeAlternativeButton(CallbackInfoReturnable<List<GuiEventListener>> cir) {
        if (this.kaboomkeybinds$row != null) {
            cir.setReturnValue(this.kaboomkeybinds$row.withButton(cir.getReturnValue()));
        }
    }

    @Inject(method = "narratables", at = @At("RETURN"), cancellable = true)
    private void narrateAlternativeButton(CallbackInfoReturnable<List<? extends NarratableEntry>> cir) {
        if (this.kaboomkeybinds$row != null) {
            cir.setReturnValue(this.kaboomkeybinds$row.withNarratedButton(cir.getReturnValue()));
        }
    }

    /**
     * Controlling marks the primary button for every duplicate, defaults included, through {@code same()}. That
     * call would include the conflicts of this row's alternative key, which belong to the alternative button, so the
     * primary button keeps Controlling's rule for its own key only, see {@link ConflictRules#primaryConflict}.
     */
    @WrapOperation(
            method = "refreshEntry",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;same(Lnet/minecraft/client/KeyMapping;)Z"))
    private boolean primaryKeyConflicts(KeyMapping mapping, KeyMapping other, Operation<Boolean> original) {
        return ConflictRules.primaryConflict(AlternativeKeys.keys(mapping), AlternativeKeys.keys(other));
    }

    @Inject(method = "refreshEntry", at = @At("TAIL"))
    private void refreshAlternativeButton(CallbackInfo ci) {
        if (this.kaboomkeybinds$row != null) {
            this.kaboomkeybinds$row.refresh();
        }
    }

    @Override
    public KeyMapping kaboomkeybinds$mapping() {
        return this.key;
    }

    @Override
    public Button kaboomkeybinds$alternativeButton() {
        return this.kaboomkeybinds$row != null ? this.kaboomkeybinds$row.button() : null;
    }
}
