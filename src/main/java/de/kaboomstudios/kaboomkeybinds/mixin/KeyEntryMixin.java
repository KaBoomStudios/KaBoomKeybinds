package de.kaboomstudios.kaboomkeybinds.mixin;

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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the alternative key button to every row of the vanilla key binds list. The button itself lives in
 * {@link AlternativeKeyRow}, which the row of the Controlling mod uses as well; this mixin connects it to the places
 * where the vanilla row builds, lays out, draws and refreshes its buttons.
 *
 * <p>Each button marks the conflicts of its own key, following {@link ConflictRules}; the stripe at the left of the
 * buttons shows for either.
 */
@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyEntryMixin extends KeyBindsList.Entry implements KeyEntryAccess {
    @Shadow
    @Final
    private KeyMapping key;

    @Shadow
    @Final
    private Button changeButton;

    @Unique
    private AlternativeKeyRow kaboomkeybinds$row;

    /**
     * Creates the alternative key button once the vanilla buttons exist. Vanilla refreshes the row at the end of its
     * constructor, before this runs, so the new button refreshes itself when it is created.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addAlternativeButton(KeyBindsList list, KeyMapping mapping, Component name, CallbackInfo ci) {
        this.kaboomkeybinds$row = new AlternativeKeyRow(list, mapping, name, this.changeButton);
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

    @Inject(method = "extractContent", at = @At("HEAD"))
    private void sizeKeyButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        this.kaboomkeybinds$row.sizeKeyButtons(this.getWidth());
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
        return this.kaboomkeybinds$row.primaryButtonX(x);
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
        FormattedCharSequence clipped = this.kaboomkeybinds$row.clippedName(font, name, x);
        if (clipped != null) {
            graphics.text(font, clipped, x, y, color);
        } else {
            original.call(graphics, font, name, x, y, color);
        }
    }

    /**
     * The stripe left of the buttons marks a conflict on either button. Vanilla draws it from its collision flag,
     * which also decides the marker on the primary button (since 26.3 on every frame). So a conflict of the
     * alternative key is added where the stripe reads the flag, and the flag itself is left to vanilla.
     */
    @ModifyExpressionValue(
            method = "extractContent",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList$KeyEntry;hasCollision:Z",
                    opcode = Opcodes.GETFIELD))
    private boolean markAlternativeConflictWithStripe(boolean primaryConflict) {
        return this.kaboomkeybinds$row.showsConflictStripe(primaryConflict);
    }

    @Inject(method = "extractContent", at = @At("TAIL"))
    private void drawAlternativeButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        this.kaboomkeybinds$row.extract(graphics, mouseX, mouseY, a);
    }

    @Inject(method = "children", at = @At("RETURN"), cancellable = true)
    private void includeAlternativeButton(CallbackInfoReturnable<List<? extends GuiEventListener>> cir) {
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
        return this.kaboomkeybinds$row.button();
    }
}
