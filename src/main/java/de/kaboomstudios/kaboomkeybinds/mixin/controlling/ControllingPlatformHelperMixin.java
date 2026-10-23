package de.kaboomstudios.kaboomkeybinds.mixin.controlling;

import com.mojang.blaze3d.platform.InputConstants;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes the key changes the Controlling mod makes past the vanilla screen through {@link AlternativeKeys}.
 *
 * <p>Controlling's reset button of a row and its "Reset Keys" button both reset each mapping through this helper, so
 * clearing the alternative key here covers both, just as vanilla's reset buttons clear it. A primary key typed on the
 * keyboard is bound through this helper as well, instead of through the vanilla screen whose binding the main mixins
 * wrap. Mouse buttons still take the vanilla way.
 */
@Mixin(targets = "com.blamejared.controlling.platform.FabricPlatformHelper")
public abstract class ControllingPlatformHelperMixin {
    @Inject(method = "setToDefault", at = @At("HEAD"))
    private void clearAlternativeKey(Options options, KeyMapping mapping, CallbackInfo ci) {
        AlternativeKeys.clearAlternative(mapping);
    }

    /**
     * Applies the one key per mapping rule to the primary key Controlling has just bound: if it is the mapping's
     * alternative key, the alternative key is unbound. The key is read back from the mapping rather than taken from
     * the call, because other mods may handle Controlling's key event and bind something else.
     */
    @Inject(method = "setKey", at = @At("TAIL"))
    private void applyPrimaryKeyRule(Options options, KeyMapping mapping, InputConstants.Key key, CallbackInfo ci) {
        AlternativeKeys.setPrimary(mapping, AlternativeKeys.keys(mapping).primary());
    }
}
