package de.kaboomstudios.kaboomkeybinds.mixin;

import de.kaboomstudios.kaboomkeybinds.client.KeyBindsListAccess;
import de.kaboomstudios.kaboomkeybinds.core.KeyRowLayout;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Widens the rows of the key binds list so that two key buttons fit next to the names.
 *
 * <p>Vanilla measures the longest name while it builds the list but never uses the result and returns a fixed row
 * width. Here the row width follows the longest name and the width of the window, see {@link KeyRowLayout}. The
 * rows ask the list for their width whenever they are positioned, so the result applies to every row at once.
 */
@Mixin(KeyBindsList.class)
public abstract class KeyBindsListMixin extends ContainerObjectSelectionList<KeyBindsList.Entry> implements KeyBindsListAccess {
    @Shadow
    @Final
    private KeyBindsScreen keyBindsScreen;

    @Shadow
    private int maxNameWidth;

    private KeyBindsListMixin() {
        super(null, 0, 0, 0, 0);
    }

    @Inject(method = "getRowWidth", at = @At("HEAD"), cancellable = true)
    private void fitTwoKeyButtons(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(KeyRowLayout.rowWidth(this.maxNameWidth, this.getWidth()));
    }

    @Override
    public KeyBindsScreen kaboomkeybinds$screen() {
        return this.keyBindsScreen;
    }
}
