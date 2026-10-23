package de.kaboomstudios.kaboomkeybinds.client;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the mixins for the Controlling mod only when Controlling is installed.
 *
 * <p>Controlling replaces the controls menu with its own screen, list and rows. The mixins in the package
 * {@code mixin.controlling} give its rows the alternative key button and route its reset and key binding paths through
 * {@link AlternativeKeys}. They name Controlling's classes as strings, so this mod neither compiles against Controlling
 * nor needs it at runtime. Without Controlling this plugin skips them before Mixin looks for their targets.
 *
 * <p>Controlling is released on its own schedule, so its internals may change. Its mixin configuration is therefore
 * not required and its injectors need no match: with a Controlling version that no longer fits, Mixin logs a warning
 * and leaves out what does not match, and the game starts with Controlling's plain rows instead of crashing. The
 * plugin lives outside the mixin packages, because Mixin refuses to load other classes from them.
 */
public final class ControllingMixinPlugin implements IMixinConfigPlugin {
    private static final String CONTROLLING_MOD_ID = "controlling";

    private boolean controllingLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        this.controllingLoaded = FabricLoader.getInstance().isModLoaded(CONTROLLING_MOD_ID);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.controllingLoaded;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
