package de.kaboomstudios.kaboomkeybinds.mixin.controlling;

import de.kaboomstudios.kaboomkeybinds.client.ControlsMenu;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes room for the column headings in the screen of the Controlling mod. Controlling builds its own header with the
 * title and a search box and sets its height in the constructor, so the vanilla screen mixin, which sets the header
 * height while the list is added, does not reach it. The header grows by the heading line instead; it centres its
 * contents, which leaves the line between the search box and the list. Everything else the vanilla screen mixin does
 * (binding the alternative key, drawing the headings) works here as well, because Controlling's screen extends the
 * vanilla one and calls its methods for those parts.
 */
@Mixin(targets = "com.blamejared.controlling.client.NewKeyBindsScreen")
public abstract class ControllingKeyBindsScreenMixin extends KeyBindsScreen {
    private ControllingKeyBindsScreenMixin() {
        super(null, null);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void makeRoomForColumnHeadings(CallbackInfo ci) {
        this.layout.setHeaderHeight(this.layout.getHeaderHeight() + ControlsMenu.HEADING_LINE_HEIGHT);
    }
}
