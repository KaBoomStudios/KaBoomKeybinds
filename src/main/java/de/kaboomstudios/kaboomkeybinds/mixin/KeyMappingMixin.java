package de.kaboomstudios.kaboomkeybinds.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeyMapping;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeys;
import de.kaboomstudios.kaboomkeybinds.core.DualKeyHold;
import de.kaboomstudios.kaboomkeybinds.core.KeyPair;
import de.kaboomstudios.kaboomkeybinds.core.KeySlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives every key mapping a second key that works exactly like the first one.
 *
 * <p>Minecraft keeps one key per mapping and a static lookup table from key to mappings. Pressing a key sets the
 * "down" state and counts a click on every mapping found in the table, menus ask the mapping whether an event
 * matches its key. This mixin adds the alternative key to the lookup table and to every matching method, so all
 * of these paths see both keys without touching their callers.
 *
 * <p>Holding is tracked per key in a {@link DualKeyHold}: while both keys are held and one is released, the mapping
 * stays down. Everything else (click counting, toggle behaviour, saving to the options file) is left to vanilla.
 * The alternative key never reaches the vanilla options file: it is loaded from the mod's own file when the mapping
 * is constructed and saved through {@link AlternativeKeys}.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements AlternativeKeyMapping {
    @Shadow
    @Final
    private static Map<String, KeyMapping> ALL;

    @Shadow
    @Final
    private static Map<InputConstants.Key, List<KeyMapping>> MAP;

    @Shadow
    protected InputConstants.Key key;

    @Unique
    private InputConstants.Key kaboomkeybinds$alternativeKey;

    @Unique
    private DualKeyHold kaboomkeybinds$hold;

    /**
     * Loads the alternative key as soon as the mapping exists. This covers vanilla mappings, which are created with
     * the game options, and mappings of other mods, whenever they create them. Only the constructor that does the
     * work is targeted, the other two delegate to it.
     */
    @Inject(
            method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;I"
                    + "Lnet/minecraft/client/KeyMapping$Category;I)V",
            at = @At("TAIL"))
    private void loadAlternativeKey(String name, InputConstants.Type type, int value, KeyMapping.Category category,
            int order, CallbackInfo ci) {
        this.kaboomkeybinds$hold = new DualKeyHold();
        this.kaboomkeybinds$alternativeKey = AlternativeKeys.stored(name);
        this.kaboomkeybinds$registerAlternativeKey();
    }

    /** Vanilla rebuilds the lookup table from the primary keys here. The alternative keys are added afterwards. */
    @Inject(method = "resetMapping", at = @At("TAIL"))
    private static void registerAlternativeKeys(CallbackInfo ci) {
        for (KeyMapping mapping : ALL.values()) {
            ((KeyMappingMixin) (Object) mapping).kaboomkeybinds$registerAlternativeKey();
        }
    }

    /**
     * Replaces the press and release handling for a key. Vanilla sets every mapping of the key to the new state.
     * Here each mapping first records which of its two keys changed and only takes the new state when it applies:
     * a press always, a release only when it leaves no key held. A release that leaves the other key held is not
     * passed on at all, because toggle mappings treat every call with "down" as a new press.
     */
    @Inject(method = "set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V", at = @At("HEAD"), cancellable = true)
    private static void setBothKeys(InputConstants.Key key, boolean down, CallbackInfo ci) {
        List<KeyMapping> mappings = MAP.get(key);
        if (mappings != null) {
            for (KeyMapping mapping : mappings) {
                KeyMappingMixin self = (KeyMappingMixin) (Object) mapping;
                KeySlot slot = key.equals(self.key) ? KeySlot.PRIMARY : KeySlot.SECONDARY;
                boolean held = self.kaboomkeybinds$hold.set(slot, down);
                if (down || !held) {
                    mapping.setDown(down);
                }
            }
        }
        ci.cancel();
    }

    /**
     * When the game regains the mouse after a screen, vanilla reads the physical state of each primary keyboard key
     * from the window. The alternative key is read the same way, and the mapping counts as down when either key is.
     * Vanilla skips mappings whose primary key is not a keyboard key, so an alternative keyboard key of such a
     * mapping is not restored either. It works again with the next press.
     */
    @WrapOperation(method = "setAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V"))
    private static void restoreBothKeys(KeyMapping mapping, boolean primaryDown, Operation<Void> original) {
        KeyMappingMixin self = (KeyMappingMixin) (Object) mapping;
        InputConstants.Key alternative = self.kaboomkeybinds$alternativeKey;
        boolean alternativeDown = self.kaboomkeybinds$hasAlternativeKey()
                && alternative.getType() == InputConstants.Type.KEYSYM
                && InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), alternative.getValue());
        self.kaboomkeybinds$hold.set(KeySlot.PRIMARY, primaryDown);
        self.kaboomkeybinds$hold.set(KeySlot.SECONDARY, alternativeDown);
        original.call(mapping, primaryDown || alternativeDown);
    }

    /**
     * Vanilla releases every mapping when a screen opens. The held keys are forgotten here rather than only in
     * {@code release()}, because toggle mappings override that method without calling the base version.
     */
    @Inject(method = "releaseAll", at = @At("TAIL"))
    private static void forgetHeldKeysOfAll(CallbackInfo ci) {
        for (KeyMapping mapping : ALL.values()) {
            ((KeyMappingMixin) (Object) mapping).kaboomkeybinds$hold.releaseAll();
        }
    }

    /** Covers direct calls of {@code release()} on plain mappings as well. */
    @Inject(method = "release", at = @At("HEAD"))
    private void forgetHeldKeys(CallbackInfo ci) {
        this.kaboomkeybinds$hold.releaseAll();
    }

    @Inject(method = "matches(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("RETURN"), cancellable = true)
    private void matchAlternativeKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && this.kaboomkeybinds$isAlternativeKey(InputConstants.getKey(event))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "matchesMouse", at = @At("RETURN"), cancellable = true)
    private void matchAlternativeMouseButton(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
                && this.kaboomkeybinds$isAlternativeKey(InputConstants.Type.MOUSE.getOrCreate(event.button()))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "matches(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Z", at = @At("RETURN"), cancellable = true)
    private void matchAlternativeKey(InputConstants.Key key, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && this.kaboomkeybinds$isAlternativeKey(key)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Two mappings also conflict when a primary key equals the alternative key of the other mapping, in either
     * direction, or when both alternative keys are equal. Unbound alternative keys never conflict; the vanilla
     * check for two unbound primary keys stays as it is.
     */
    @Inject(method = "same", at = @At("RETURN"), cancellable = true)
    private void sameAlternativeKey(KeyMapping that, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
                && this.kaboomkeybinds$keys().sharesKeyWith(((AlternativeKeyMapping) that).kaboomkeybinds$keys())) {
            cir.setReturnValue(true);
        }
    }

    /** A mapping with an alternative key is not in its default state, so the reset buttons of the menu activate. */
    @Inject(method = "isDefault", at = @At("RETURN"), cancellable = true)
    private void notDefaultWithAlternativeKey(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && this.kaboomkeybinds$hasAlternativeKey()) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public InputConstants.Key kaboomkeybinds$getAlternativeKey() {
        return this.kaboomkeybinds$alternativeKey;
    }

    @Override
    public void kaboomkeybinds$setAlternativeKey(InputConstants.Key key) {
        this.kaboomkeybinds$alternativeKey = key == null ? InputConstants.UNKNOWN : key;
    }

    @Override
    public boolean kaboomkeybinds$hasAlternativeKey() {
        return !InputConstants.UNKNOWN.equals(this.kaboomkeybinds$alternativeKey);
    }

    @Override
    public KeyPair<InputConstants.Key> kaboomkeybinds$keys() {
        return new KeyPair<>(
                InputConstants.UNKNOWN.equals(this.key) ? null : this.key,
                this.kaboomkeybinds$hasAlternativeKey() ? this.kaboomkeybinds$alternativeKey : null);
    }

    @Unique
    private boolean kaboomkeybinds$isAlternativeKey(InputConstants.Key key) {
        return this.kaboomkeybinds$hasAlternativeKey() && this.kaboomkeybinds$alternativeKey.equals(key);
    }

    /**
     * Adds the alternative key to the lookup table. A key equal to the primary key is skipped, otherwise the table
     * would list the mapping twice for one key and every press would count two clicks.
     */
    @Unique
    private void kaboomkeybinds$registerAlternativeKey() {
        if (this.kaboomkeybinds$hasAlternativeKey() && !this.kaboomkeybinds$alternativeKey.equals(this.key)) {
            MAP.computeIfAbsent(this.kaboomkeybinds$alternativeKey, k -> new ArrayList<>()).add((KeyMapping) (Object) this);
        }
    }
}
