package de.kaboomstudios.kaboomkeybinds.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeys;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Checks that alternative keys are loaded at startup and trigger their mappings everywhere a primary key would.
 *
 * <p>The build copies {@code src/gametest/fixtures/kaboomkeybinds.json} into the config directory of the test
 * client before it starts, so the keys below are already bound when the game options are created. The keys are
 * unbound in vanilla, which keeps the vanilla mappings out of the way.
 */
public final class AlternativeKeyGameTest implements FabricClientGameTest {
    private static final InputConstants.Key UP = InputConstants.getKey("key.keyboard.up");
    private static final InputConstants.Key KEYPAD_0 = InputConstants.getKey("key.keyboard.keypad.0");
    private static final InputConstants.Key KEYPAD_1 = InputConstants.getKey("key.keyboard.keypad.1");
    private static final InputConstants.Key KEYPAD_2 = InputConstants.getKey("key.keyboard.keypad.2");
    private static final InputConstants.Key RIGHT_SHIFT = InputConstants.getKey("key.keyboard.right.shift");
    private static final InputConstants.Key I = InputConstants.getKey("key.keyboard.i");
    private static final InputConstants.Key MOUSE_5 = InputConstants.getKey("key.mouse.5");
    private static final int MOUSE_5_BUTTON = 4;

    /** Size of the survival inventory screen, used to find the slot under the cursor. */
    private static final int INVENTORY_IMAGE_WIDTH = 176;
    private static final int INVENTORY_IMAGE_HEIGHT = 166;
    /** First slot of the main inventory, the rows below the hotbar. */
    private static final int MAIN_INVENTORY_SLOT = 9;
    private static final int HOTBAR_SLOT_1 = 0;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        checkLoadedAtStartup(context);

        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL))
                .create()) {
            world.getConnection().waitForChunksRender();
            TestInput input = context.getInput();

            checkWalkingWithAlternativeKey(context, input);
            checkHoldingBothKeys(context, input);
            checkJumpingWithAlternativeKey(context, input);
            checkSneakingWithAlternativeKey(context, input);
            checkPerspectiveWithMouseButton(context, input);
            checkInventoryWithAlternativeKey(context, world, input);
            checkScreenReleasesHeldKeys(context, input);
            checkBridgeSavesAndAppliesRules(context, input);

            context.takeScreenshot("alternative-keys-world");
        } finally {
            context.restoreDefaultGameOptions();
        }
    }

    private static void checkLoadedAtStartup(ClientGameTestContext context) {
        context.runOnClient(client -> {
            Options options = client.options;
            assertEquals(UP, AlternativeKeys.get(options.keyUp), "alternative key of key.forward");
            assertEquals(KEYPAD_0, AlternativeKeys.get(options.keyJump), "alternative key of key.jump");
            assertEquals(RIGHT_SHIFT, AlternativeKeys.get(options.keyShift), "alternative key of key.sneak");
            assertEquals(I, AlternativeKeys.get(options.keyInventory), "alternative key of key.inventory");
            assertEquals(KEYPAD_1, AlternativeKeys.get(options.keyHotbarSlots[0]), "alternative key of key.hotbar.1");
            assertEquals(MOUSE_5, AlternativeKeys.get(options.keyTogglePerspective),
                    "alternative key of key.togglePerspective");
            assertEquals(InputConstants.UNKNOWN, AlternativeKeys.get(options.keyChat),
                    "an unknown key name must leave the alternative key unbound");
            assertEquals(InputConstants.UNKNOWN, AlternativeKeys.get(options.keyDrop),
                    "a mapping without an entry must have no alternative key");
            assertEquals(Optional.of("key.keyboard.x"), AlternativeKeys.store().get("key.othermod.example"),
                    "entries of mappings that do not exist must be kept");

            assertTrue(!options.keyUp.isDefault(), "a mapping with an alternative key is not default");
            assertTrue(options.keyDrop.isDefault(), "a mapping without an alternative key stays default");
            assertTrue(options.keyUp.matches(new KeyEvent(UP.getValue(), 0, 0)),
                    "matches(KeyEvent) sees the alternative key");
            assertTrue(options.keyUp.matches(UP), "matches(Key) sees the alternative key");
            assertTrue(options.keyTogglePerspective.matchesMouse(
                    new MouseButtonEvent(0, 0, new MouseButtonInfo(MOUSE_5_BUTTON, 0))),
                    "matchesMouse sees the alternative mouse button");
            assertTrue(!options.keyDrop.matches(new KeyEvent(UP.getValue(), 0, 0)),
                    "a mapping without alternative key must not match the alternative key of another mapping");
        });
    }

    private static void checkWalkingWithAlternativeKey(ClientGameTestContext context, TestInput input) {
        Vec3 start = context.computeOnClient(client -> client.player.position());
        input.holdKey(UP);
        context.waitTicks(10);
        assertTrue(isDown(context, options -> options.keyUp),
                "key.forward must be down while its alternative key is held");
        input.releaseKey(UP);
        context.waitTick();
        assertTrue(!isDown(context, options -> options.keyUp),
                "key.forward must be up after the alternative key is released");

        Vec3 end = context.computeOnClient(client -> client.player.position());
        double distance = Math.hypot(end.x - start.x, end.z - start.z);
        assertTrue(distance > 0.5, "the player must walk while the alternative forward key is held, moved " + distance);
    }

    private static void checkHoldingBothKeys(ClientGameTestContext context, TestInput input) {
        input.holdKey(options -> options.keyUp);
        input.holdKey(UP);
        context.waitTick();
        assertTrue(isDown(context, options -> options.keyUp), "down with both keys held");
        input.releaseKey(options -> options.keyUp);
        context.waitTick();
        assertTrue(isDown(context, options -> options.keyUp),
                "still down after releasing the primary key while the alternative key is held");
        input.releaseKey(UP);
        context.waitTick();
        assertTrue(!isDown(context, options -> options.keyUp), "up after both keys are released");

        input.holdKey(UP);
        input.holdKey(options -> options.keyUp);
        context.waitTick();
        input.releaseKey(UP);
        context.waitTick();
        assertTrue(isDown(context, options -> options.keyUp),
                "still down after releasing the alternative key while the primary key is held");
        input.releaseKey(options -> options.keyUp);
        context.waitTick();
        assertTrue(!isDown(context, options -> options.keyUp), "up after both keys are released, other order");
    }

    private static void checkJumpingWithAlternativeKey(ClientGameTestContext context, TestInput input) {
        context.waitFor(client -> client.player.onGround());
        double startY = context.computeOnClient(client -> client.player.getY());
        input.holdKey(KEYPAD_0);
        context.waitTicks(2);
        input.releaseKey(KEYPAD_0);
        context.waitFor(client -> client.player.getY() > startY + 0.3, 20);
        context.waitFor(client -> client.player.onGround(), 40);
    }

    private static void checkSneakingWithAlternativeKey(ClientGameTestContext context, TestInput input) {
        input.holdKey(RIGHT_SHIFT);
        context.waitTicks(2);
        assertTrue(context.computeOnClient(client -> client.player.isShiftKeyDown()),
                "the player sneaks while the alternative sneak key is held");
        input.releaseKey(RIGHT_SHIFT);
        context.waitTicks(2);
        assertTrue(!context.computeOnClient(client -> client.player.isShiftKeyDown()),
                "the player stops sneaking when the alternative key is released");

        // Toggle mode: every press of either key toggles, releasing a key never does.
        context.runOnClient(client -> client.options.toggleCrouch().set(true));
        input.pressKey(RIGHT_SHIFT);
        assertTrue(isDown(context, options -> options.keyShift), "toggle sneak switches on with the alternative key");
        input.pressKey(RIGHT_SHIFT);
        assertTrue(!isDown(context, options -> options.keyShift), "toggle sneak switches off with the alternative key");
        input.holdKey(options -> options.keyShift);
        context.waitTick();
        assertTrue(isDown(context, options -> options.keyShift), "toggle sneak switches on with the primary key");
        input.holdKey(RIGHT_SHIFT);
        context.waitTick();
        assertTrue(!isDown(context, options -> options.keyShift),
                "toggle sneak switches off with the alternative key while the primary key is held");
        input.releaseKey(options -> options.keyShift);
        context.waitTick();
        assertTrue(!isDown(context, options -> options.keyShift),
                "releasing the primary key while the alternative key is held must not toggle");
        input.releaseKey(RIGHT_SHIFT);
        context.waitTick();
        assertTrue(!isDown(context, options -> options.keyShift), "releasing the last key must not toggle");
        context.runOnClient(client -> client.options.toggleCrouch().set(false));
    }

    private static void checkPerspectiveWithMouseButton(ClientGameTestContext context, TestInput input) {
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        input.pressMouse(MOUSE_5_BUTTON);
        context.waitTick();
        assertEquals(CameraType.THIRD_PERSON_BACK, context.computeOnClient(client -> client.options.getCameraType()),
                "the alternative mouse button toggles the perspective");
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
    }

    private static void checkInventoryWithAlternativeKey(ClientGameTestContext context, TestSingleplayerContext world,
            TestInput input) {
        world.getServer().runCommand("give @a minecraft:stone");
        context.waitFor(client -> client.player.getInventory().getItem(HOTBAR_SLOT_1).is(Items.STONE));

        input.pressKey(I);
        context.waitForScreen(InventoryScreen.class);

        // Hover the first main inventory slot, then swap it with hotbar slot 1 through the alternative hotbar key.
        double[] cursor = context.computeOnClient(client -> {
            InventoryScreen screen = (InventoryScreen) Objects.requireNonNull(client.gui.screen());
            Slot slot = screen.getMenu().slots.stream()
                    .filter(candidate -> candidate.container == client.player.getInventory()
                            && candidate.getContainerSlot() == MAIN_INVENTORY_SLOT)
                    .findFirst()
                    .orElseThrow();
            int left = (screen.width - INVENTORY_IMAGE_WIDTH) / 2;
            int top = (screen.height - INVENTORY_IMAGE_HEIGHT) / 2;
            Window window = client.getWindow();
            double scale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
            return new double[] {(left + slot.x + 8) * scale, (top + slot.y + 8) * scale};
        });
        input.setCursorPos(cursor[0], cursor[1]);
        context.waitTicks(2);
        context.takeScreenshot("inventory-opened-with-alternative-key");

        input.pressKey(KEYPAD_1);
        context.waitFor(client -> client.player.getInventory().getItem(MAIN_INVENTORY_SLOT).is(Items.STONE), 20);
        assertTrue(context.computeOnClient(client -> client.player.getInventory().getItem(HOTBAR_SLOT_1).isEmpty()),
                "the hotbar key swapped the hovered slot with hotbar slot 1");

        input.pressKey(I);
        context.waitFor(client -> client.gui.screen() == null, 20);
    }

    private static void checkScreenReleasesHeldKeys(ClientGameTestContext context, TestInput input) {
        input.holdKey(UP);
        context.waitTick();
        assertTrue(isDown(context, options -> options.keyUp), "down before the screen opens");
        input.pressKey(I);
        context.waitForScreen(InventoryScreen.class);
        assertTrue(!isDown(context, options -> options.keyUp), "opening a screen releases the mapping like vanilla");
        input.pressKey(I);
        context.waitFor(client -> client.gui.screen() == null, 20);
        input.releaseKey(UP);
        context.waitTick();
        assertTrue(!isDown(context, options -> options.keyUp), "still up after the forgotten key is released");
    }

    private static void checkBridgeSavesAndAppliesRules(ClientGameTestContext context, TestInput input) {
        context.runOnClient(client -> {
            KeyMapping drop = client.options.keyDrop;
            KeyMapping inventory = client.options.keyInventory;
            InputConstants.Key dropDefault = drop.getDefaultKey();
            InputConstants.Key inventoryPrimary = Objects.requireNonNull(AlternativeKeys.keys(inventory).primary());

            AlternativeKeys.setAlternative(drop, KEYPAD_2);
            assertEquals(KEYPAD_2, AlternativeKeys.get(drop), "alternative key set through the bridge");
            assertTrue(!drop.isDefault(), "not default with an alternative key");
            assertEquals("key.keyboard.keypad.2", storedKeyName("key.drop"), "the file holds the new key at once");
            assertEquals("key.keyboard.x", storedKeyName("key.othermod.example"), "unknown mappings survive a save");
            assertEquals("not-a-key", storedKeyName("key.chat"), "unknown key names survive a save");

            AlternativeKeys.setAlternative(drop, inventoryPrimary);
            assertTrue(drop.same(inventory), "conflict: alternative key against the primary key of another mapping");
            assertTrue(inventory.same(drop), "conflict: primary key against the alternative key of another mapping");
            AlternativeKeys.setAlternative(drop, I);
            assertTrue(drop.same(inventory) && inventory.same(drop), "conflict: alternative key against alternative key");

            AlternativeKeys.setAlternative(drop, dropDefault);
            assertEquals(null, AlternativeKeys.keys(drop).primary(),
                    "binding the primary key as alternative key unbinds the primary key");
            assertEquals(dropDefault, AlternativeKeys.get(drop), "the alternative key took the key");
            AlternativeKeys.setPrimary(drop, dropDefault);
            assertEquals(dropDefault, AlternativeKeys.keys(drop).primary(), "the primary key is back");
            assertEquals(InputConstants.UNKNOWN, AlternativeKeys.get(drop),
                    "binding the alternative key as primary key unbinds the alternative key");
            assertTrue(drop.isDefault(), "default again without alternative key");
            assertTrue(!drop.same(inventory), "no conflict without alternative key");
            assertEquals(null, storedKeyName("key.drop"), "an unbound alternative key leaves no entry");

            AlternativeKeys.setAlternative(drop, KEYPAD_2);
        });

        // The lookup table knows the new key without a restart.
        input.holdKey(KEYPAD_2);
        context.waitTick();
        assertTrue(isDown(context, options -> options.keyDrop), "a key bound through the bridge works in the lookup table");
        input.releaseKey(KEYPAD_2);
        context.waitTick();

        context.runOnClient(client -> AlternativeKeys.clearAlternative(client.options.keyDrop));
    }

    private static String storedKeyName(String mappingName) {
        try {
            String json = Files.readString(AlternativeKeys.file(), StandardCharsets.UTF_8);
            JsonObject keys = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("keys");
            return keys.has(mappingName) ? keys.get(mappingName).getAsString() : null;
        } catch (IOException e) {
            throw new AssertionError("could not read " + AlternativeKeys.file(), e);
        }
    }

    private static boolean isDown(ClientGameTestContext context, Function<Options, KeyMapping> mapping) {
        return context.computeOnClient(client -> mapping.apply(client.options).isDown());
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }
}
