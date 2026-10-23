package de.kaboomstudios.kaboomkeybinds.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeySelection;
import de.kaboomstudios.kaboomkeybinds.client.AlternativeKeys;
import de.kaboomstudios.kaboomkeybinds.client.ControlsMenu;
import de.kaboomstudios.kaboomkeybinds.client.KeyEntryAccess;
import de.kaboomstudios.kaboomkeybinds.core.KeyRowLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Drives the controls menu with real mouse and keyboard input: binding an alternative key with the keyboard and
 * the mouse, Escape, conflicts, the one key per mapping rule, the reset buttons, and the row layout. Finally takes
 * screenshots of the menu in both languages at several GUI scales and window sizes for the layout review.
 *
 * <p>The build runs the test a second time with the Controlling mod on request, which replaces the menu with its own
 * screen, list and rows. The same checks apply there, except where Controlling behaves differently on its own: it
 * focuses its search box when the menu opens, marks every duplicate primary key including the defaults Minecraft
 * ships, asks before "Reset Keys" and updates that button only when the screen is laid out again.
 *
 * <p>Runs after {@link AlternativeKeyGameTest}, which leaves the alternative keys of the prepared file in place.
 */
public final class ControlsMenuGameTest implements FabricClientGameTest {
    private static final boolean CONTROLLING = FabricLoader.getInstance().isModLoaded("controlling");
    /** The header height Controlling sets for its title and search box. */
    private static final int CONTROLLING_HEADER_HEIGHT = 48;

    private static final InputConstants.Key KEYPAD_4 = InputConstants.getKey("key.keyboard.keypad.4");
    private static final InputConstants.Key S = InputConstants.getKey("key.keyboard.s");
    private static final InputConstants.Key ESCAPE = InputConstants.getKey("key.keyboard.escape");
    private static final InputConstants.Key F12 = InputConstants.getKey("key.keyboard.f12");
    private static final InputConstants.Key MOUSE_4 = InputConstants.getKey("key.mouse.4");

    /** Positions of the three buttons in a row, see {@code KeyEntryMixin}. */
    private static final int PRIMARY = 0;
    private static final int ALTERNATIVE = 1;
    private static final int RESET = 2;

    /** One screenshot of the menu: language, window size in pixels and GUI scale. */
    private record Shot(String language, int width, int height, int guiScale, String name) {
    }

    private static final List<Shot> SHOTS = List.of(
            new Shot("en_us", 1920, 1080, 4, "controls-en-1080p-scale4"),
            new Shot("en_us", 1280, 720, 1, "controls-en-720p-scale1"),
            new Shot("en_us", 1280, 720, 2, "controls-en-720p-scale2"),
            new Shot("en_us", 1280, 720, 3, "controls-en-720p-scale3"),
            new Shot("en_us", 854, 480, 2, "controls-en-small-window-scale2"),
            new Shot("de_de", 1920, 1080, 4, "controls-de-1080p-scale4"),
            new Shot("de_de", 1280, 720, 3, "controls-de-720p-scale3"),
            new Shot("de_de", 640, 480, 2, "controls-de-narrow-window-scale2"));

    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        TestInput input = context.getInput();
        int[] windowSize = context.computeOnClient(
                client -> new int[] {client.getWindow().getScreenWidth(), client.getWindow().getScreenHeight()});
        try {
            openKeyBinds(context);
            checkRows(context);
            checkBindingAKey(context, input);
            boolean backMarkedBefore = checkConflictMarkers(context, input);
            checkEscapeClears(context, input, backMarkedBefore);
            checkBindingAMouseButton(context, input);
            checkOneKeyPerMapping(context, input);
            checkRowReset(context, input);
            checkResetAll(context);
            takeLayoutScreenshots(context, input);
        } finally {
            setLanguage(context, "en_us");
            input.resizeWindow(windowSize[0], windowSize[1]);
            context.restoreDefaultGameOptions();
            context.runOnClient(ClientScreens::close);
        }
    }

    /**
     * Opens the menu the way the game does. Controlling swaps in its own screen and focuses its search box, whose
     * suggestions cover the first rows; Escape leaves the search box as a player would, without closing the menu.
     */
    private static void openKeyBinds(ClientGameTestContext context) {
        context.setScreen(() -> new KeyBindsScreen(new TitleScreen(), Minecraft.getInstance().options));
        context.waitForScreen(KeyBindsScreen.class);
        context.waitTicks(2);
        if (CONTROLLING) {
            context.getInput().pressKey(ESCAPE);
            context.waitTick();
            context.runOnClient(client -> assertTrue(ClientScreens.current(client) instanceof KeyBindsScreen,
                    "Escape in Controlling's search box keeps the menu open"));
        }
    }

    private static void checkRows(ClientGameTestContext context) {
        context.runOnClient(client -> {
            KeyBindsList list = list(client);
            assertEquals(CONTROLLING ? "com.blamejared.controlling.client.NewKeyBindsList" : KeyBindsList.class.getName(),
                    list.getClass().getName(), "the list of the menu under test");
            assertTrue(list.getRowWidth() >= KeyRowLayout.VANILLA_ROW_WIDTH, "rows are at least as wide as in vanilla");
            assertTrue(list.getRowWidth() + KeyRowLayout.LIST_MARGIN <= list.getWidth(), "rows leave room for the scrollbar");
            assertEquals((CONTROLLING ? CONTROLLING_HEADER_HEIGHT : HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT)
                    + ControlsMenu.HEADING_LINE_HEIGHT, list.getY(), "the header has a line for the column headings");

            int rows = 0;
            for (KeyBindsList.Entry entry : list.children()) {
                if (!(entry instanceof KeyEntryAccess)) {
                    // Category rows hold a text widget. Every row with buttons must have been extended.
                    for (GuiEventListener child : entry.children()) {
                        assertTrue(!(child instanceof Button), "a row with key buttons lacks the alternative button: "
                                + entry.getClass().getName());
                    }
                }
                if (entry instanceof KeyEntryAccess row) {
                    rows++;
                    List<? extends GuiEventListener> children = entry.children();
                    assertEquals(3, children.size(), "a row has a primary, an alternative and a reset button");
                    assertTrue(children.get(ALTERNATIVE) == row.kaboomkeybinds$alternativeButton(),
                            "the alternative button sits between the two vanilla buttons");
                    assertEquals(3, entry.narratables().size(), "the alternative button is narrated");
                    // The label may carry a conflict marker, for example "I" also serves the F3 debug keys.
                    String label = row.kaboomkeybinds$alternativeButton().getMessage().getString();
                    assertTrue(label.contains(AlternativeKeys.get(row.kaboomkeybinds$mapping()).getDisplayName().getString()),
                            "the alternative button shows the alternative key of " + row.kaboomkeybinds$mapping().getName()
                                    + ", label: " + label);
                }
            }
            // Controlling hides categories whose name ends in ".hidden", vanilla shows every mapping.
            assertTrue(CONTROLLING ? rows > 0 : rows == client.options.keyMappings.length,
                    "one row per key mapping, found " + rows);

            KeyMapping longName = Objects.requireNonNull(KeyMapping.get(GameTestKeyMappings.LONG_NAME),
                    "the game test key mapping is registered");
            assertEquals(F12, AlternativeKeys.get(longName), "a mod key mapping gets its alternative key from the file");
        });

        scrollTo(context, options -> options.keyUp);
        context.runOnClient(client -> {
            KeyBindsList list = list(client);
            KeyMapping forward = client.options.keyUp;
            Button primary = button(client, forward, PRIMARY);
            Button alternative = button(client, forward, ALTERNATIVE);
            Button reset = button(client, forward, RESET);
            int width = KeyRowLayout.keyButtonWidth(list.getRowWidth());
            assertEquals(width, primary.getWidth(), "primary button width");
            assertEquals(width, alternative.getWidth(), "alternative button width");
            assertEquals(primary.getRight() + KeyRowLayout.BUTTON_GAP, alternative.getX(),
                    "the alternative button follows the primary button");
            assertEquals(alternative.getRight() + KeyRowLayout.BUTTON_GAP, reset.getX(),
                    "the reset button follows the alternative button");
            assertEquals(list.getRowRight() - KeyRowLayout.RESET_BUTTON_RIGHT_INSET, reset.getX(),
                    "the reset button keeps its vanilla position");
            assertEquals(primary.getY(), alternative.getY(), "both key buttons share the row");
            assertEquals(KeyRowLayout.alternativeButtonX(list.getRowRight(), width), alternative.getX(),
                    "layout rules and rendered position agree");
        });
        context.takeScreenshot("controls-menu");
    }

    private static void checkBindingAKey(ClientGameTestContext context, TestInput input) {
        context.runOnClient(client -> assertEquals(InputConstants.UNKNOWN, AlternativeKeys.get(client.options.keyLeft),
                "key.left starts without an alternative key"));

        clickRowButton(context, input, options -> options.keyLeft, ALTERNATIVE);
        context.runOnClient(client -> {
            KeyMapping left = client.options.keyLeft;
            assertTrue(selection(client).kaboomkeybinds$selectedAlternative() == left,
                    "clicking the alternative button starts binding the alternative key");
            assertTrue(screen(client).selectedKey == null, "the primary key is not being bound");
            assertTrue(button(client, left, ALTERNATIVE).getMessage().getString().startsWith("> "),
                    "the alternative button shows the waiting marker");
            assertTrue(!button(client, left, PRIMARY).getMessage().getString().startsWith("> "),
                    "the primary button does not show the waiting marker");
        });
        context.takeScreenshot("controls-waiting-for-alternative-key");

        input.pressKey(KEYPAD_4);
        context.waitTick();
        context.runOnClient(client -> {
            KeyMapping left = client.options.keyLeft;
            assertEquals(KEYPAD_4, AlternativeKeys.get(left), "the pressed key is the new alternative key");
            assertTrue(selection(client).kaboomkeybinds$selectedAlternative() == null, "binding is finished");
            assertTrue(screen(client).lastKeySelection > 0, "the time stamp is set like vanilla does for primary keys");
            assertEquals(KEYPAD_4.getDisplayName().getString(), button(client, left, ALTERNATIVE).getMessage().getString(),
                    "the alternative button shows the new key");
            assertEquals("key.keyboard.keypad.4", storedKeyName("key.left"), "the file is written at once");
            assertTrue(ClientScreens.current(client) instanceof KeyBindsScreen, "the screen stays open");
        });
    }

    /**
     * Checks the markers for conflicts of alternative keys. The primary keys of key.left (A), key.right (D) and
     * key.back (S) are defaults that some F3 debug keys share; vanilla hides such duplicates, Controlling marks them.
     * Either way the markers of the primary buttons must not change for a conflict of an alternative key.
     *
     * @return whether the primary button of key.back was marked before its key became an alternative key elsewhere
     */
    private static boolean checkConflictMarkers(ClientGameTestContext context, TestInput input) {
        boolean[] primaryMarked = context.computeOnClient(client -> new boolean[] {
                conflictMarked(client, client.options.keyLeft, PRIMARY),
                conflictMarked(client, client.options.keyRight, PRIMARY),
                conflictMarked(client, client.options.keyDown, PRIMARY)});
        if (!CONTROLLING) {
            assertTrue(!primaryMarked[0] && !primaryMarked[1] && !primaryMarked[2],
                    "vanilla hides duplicates of two default keys");
        }

        // Alternative key against alternative key: key.right gets the same key as key.left.
        clickRowButton(context, input, options -> options.keyRight, ALTERNATIVE);
        input.pressKey(KEYPAD_4);
        context.waitTick();
        context.runOnClient(client -> {
            Options options = client.options;
            assertConflict(client, options.keyLeft, ALTERNATIVE, true, "key.left alternative conflicts with key.right");
            assertConflict(client, options.keyRight, ALTERNATIVE, true, "key.right alternative conflicts with key.left");
            assertConflict(client, options.keyLeft, PRIMARY, primaryMarked[0],
                    "the primary button is not marked for a conflict of the alternative key");
            assertConflict(client, options.keyRight, PRIMARY, primaryMarked[1],
                    "the primary button is not marked for a conflict of the alternative key");
        });
        context.takeScreenshot("controls-conflict-alternative-keys");

        // Primary key against alternative key: S is the primary key of key.back.
        clickRowButton(context, input, options -> options.keyLeft, ALTERNATIVE);
        input.pressKey(S);
        context.waitTick();
        context.runOnClient(client -> {
            Options options = client.options;
            assertConflict(client, options.keyDown, PRIMARY, true,
                    "the primary button marks a conflict with the alternative key of another mapping");
            assertConflict(client, options.keyLeft, ALTERNATIVE, true,
                    "the alternative button marks a conflict with the primary key of another mapping");
            assertConflict(client, options.keyRight, ALTERNATIVE, false, "key.right has no conflict any more");
            assertConflict(client, options.keyDown, ALTERNATIVE, false, "an unbound alternative key never conflicts");
        });
        context.takeScreenshot("controls-conflict-primary-and-alternative");
        return primaryMarked[2];
    }

    private static void checkEscapeClears(ClientGameTestContext context, TestInput input, boolean backMarkedBefore) {
        clickRowButton(context, input, options -> options.keyLeft, ALTERNATIVE);
        input.pressKey(ESCAPE);
        context.waitTick();
        context.runOnClient(client -> {
            KeyMapping left = client.options.keyLeft;
            assertEquals(InputConstants.UNKNOWN, AlternativeKeys.get(left), "Escape unbinds the alternative key");
            assertTrue(selection(client).kaboomkeybinds$selectedAlternative() == null, "binding is finished");
            assertTrue(ClientScreens.current(client) instanceof KeyBindsScreen, "Escape does not close the screen while binding");
            assertEquals(null, storedKeyName("key.left"), "the entry is removed from the file");
            assertEquals(InputConstants.UNKNOWN.getDisplayName().getString(),
                    button(client, left, ALTERNATIVE).getMessage().getString(), "the button shows the unbound state");
            assertConflict(client, client.options.keyDown, PRIMARY, backMarkedBefore, "the conflict on key.back is gone");
        });
    }

    private static void checkBindingAMouseButton(ClientGameTestContext context, TestInput input) {
        clickRowButton(context, input, options -> options.keyRight, ALTERNATIVE);
        input.pressMouse(MOUSE_4.getValue());
        context.waitTick();
        context.runOnClient(client -> {
            KeyMapping right = client.options.keyRight;
            assertEquals(MOUSE_4, AlternativeKeys.get(right), "a mouse button becomes the alternative key");
            assertTrue(selection(client).kaboomkeybinds$selectedAlternative() == null, "binding is finished");
            assertEquals(MOUSE_4.getDisplayName().getString(), button(client, right, ALTERNATIVE).getMessage().getString(),
                    "the button shows the mouse button");
            assertEquals("key.mouse.4", storedKeyName("key.right"), "the file holds the mouse button");
        });
    }

    /**
     * One key never sits on both slots of a mapping. Primary keys are bound once with the keyboard and once with the
     * mouse, because the two take different ways: vanilla handles both in its screen, Controlling binds keyboard keys
     * through a helper of its own.
     */
    private static void checkOneKeyPerMapping(ClientGameTestContext context, TestInput input) {
        clickRowButton(context, input, options -> options.keyRight, ALTERNATIVE);
        input.pressKey(KEYPAD_4);
        context.waitTick();
        clickRowButton(context, input, options -> options.keyRight, PRIMARY);
        input.pressKey(KEYPAD_4);
        context.waitTick();
        context.runOnClient(client -> {
            KeyMapping right = client.options.keyRight;
            assertEquals(KEYPAD_4, AlternativeKeys.keys(right).primary(), "the typed key is the primary key now");
            assertEquals(InputConstants.UNKNOWN, AlternativeKeys.get(right), "the alternative slot was cleared");
            assertEquals(null, storedKeyName("key.right"), "the file no longer holds the key");
        });

        clickRowButton(context, input, options -> options.keyRight, ALTERNATIVE);
        input.pressMouse(MOUSE_4.getValue());
        context.waitTick();
        context.runOnClient(client -> assertEquals(MOUSE_4, AlternativeKeys.get(client.options.keyRight),
                "the mouse button is the alternative key"));

        // Binding the alternative key as primary key moves it: the alternative slot becomes empty.
        clickRowButton(context, input, options -> options.keyRight, PRIMARY);
        context.runOnClient(client -> assertTrue(screen(client).selectedKey == client.options.keyRight,
                "clicking the primary button starts the vanilla binding"));
        input.pressMouse(MOUSE_4.getValue());
        context.waitTick();
        context.runOnClient(client -> {
            KeyMapping right = client.options.keyRight;
            assertEquals(MOUSE_4, AlternativeKeys.keys(right).primary(), "the mouse button is the primary key now");
            assertEquals(InputConstants.UNKNOWN, AlternativeKeys.get(right), "the alternative slot was cleared");
            assertEquals(null, storedKeyName("key.right"), "the file no longer holds the key");
            assertEquals(MOUSE_4.getDisplayName().getString(), button(client, right, PRIMARY).getMessage().getString(),
                    "the primary button shows the mouse button");
        });

        // And back: binding the primary key as alternative key empties the primary slot.
        clickRowButton(context, input, options -> options.keyRight, ALTERNATIVE);
        input.pressMouse(MOUSE_4.getValue());
        context.waitTick();
        context.runOnClient(client -> {
            KeyMapping right = client.options.keyRight;
            assertEquals(MOUSE_4, AlternativeKeys.get(right), "the mouse button is the alternative key again");
            assertTrue(right.isUnbound(), "the primary slot was cleared");
            assertEquals(InputConstants.UNKNOWN.getDisplayName().getString(),
                    button(client, right, PRIMARY).getMessage().getString(), "the primary button shows the unbound state");
        });
    }

    private static void checkRowReset(ClientGameTestContext context, TestInput input) {
        clickRowButton(context, input, options -> options.keyRight, RESET);
        context.runOnClient(client -> {
            KeyMapping right = client.options.keyRight;
            assertEquals(right.getDefaultKey(), AlternativeKeys.keys(right).primary(), "reset restores the default primary key");
            assertEquals(InputConstants.UNKNOWN, AlternativeKeys.get(right), "reset clears the alternative key");
            assertTrue(right.isDefault(), "the mapping is default again");
            assertEquals(null, storedKeyName("key.right"), "the file no longer holds an entry");
            assertTrue(!button(client, right, RESET).active, "the reset button of a default row is inactive");
        });
    }

    private static void checkResetAll(ClientGameTestContext context) {
        context.runOnClient(client -> {
            AlternativeKeys.setAlternative(client.options.keyLeft, KEYPAD_4);
            list(client).refreshEntries();
        });
        context.waitTicks(2);
        if (!CONTROLLING) {
            context.runOnClient(client -> assertTrue(resetAllButton(client).active,
                    "Reset All is active while alternative keys are bound"));
        }

        context.clickScreenButton("controls.resetAll");
        if (CONTROLLING) {
            context.waitTick();
            context.clickScreenButton("options.confirmReset");
        }
        context.waitTicks(2);
        context.runOnClient(client -> {
            for (KeyMapping mapping : client.options.keyMappings) {
                assertTrue(mapping.isDefault(), "default after Reset All: " + mapping.getName());
                assertEquals(InputConstants.UNKNOWN, AlternativeKeys.get(mapping),
                        "no alternative key after Reset All: " + mapping.getName());
            }
            assertEquals(null, storedKeyName("key.jump"), "entries of present mappings are removed");
            assertEquals(null, storedKeyName("key.chat"), "an unreadable entry of a present mapping is removed as well");
            assertEquals("key.keyboard.x", storedKeyName("key.othermod.example"), "entries of absent mappings are kept");
            assertTrue(CONTROLLING || !resetAllButton(client).active, "Reset All is inactive when everything is default");
        });
    }

    private static void takeLayoutScreenshots(ClientGameTestContext context, TestInput input) {
        // A few bound keys for a realistic picture, none of them used by a vanilla mapping.
        context.runOnClient(client -> {
            Options options = client.options;
            AlternativeKeys.setAlternative(options.keyUp, InputConstants.getKey("key.keyboard.up"));
            AlternativeKeys.setAlternative(options.keyJump, InputConstants.getKey("key.keyboard.keypad.0"));
            AlternativeKeys.setAlternative(options.keyShift, InputConstants.getKey("key.keyboard.right.shift"));
            AlternativeKeys.setAlternative(options.keyInventory, InputConstants.getKey("key.keyboard.r"));
            AlternativeKeys.setAlternative(options.keyTogglePerspective, InputConstants.getKey("key.mouse.5"));
            AlternativeKeys.setAlternative(Objects.requireNonNull(KeyMapping.get(GameTestKeyMappings.LONG_NAME)), F12);
        });

        for (Shot shot : SHOTS) {
            setLanguage(context, shot.language());
            input.resizeWindow(shot.width(), shot.height());
            context.waitTicks(2);
            // The scale option is clamped to what the window allows, so it is set after the window has its size.
            context.runOnClient(client -> client.options.guiScale().set(shot.guiScale()));
            context.waitTick();
            openKeyBinds(context);
            context.runOnClient(client -> {
                Window window = client.getWindow();
                assertEquals(shot.width(), window.getScreenWidth(), "window width for " + shot.name());
                KeyBindsList list = list(client);
                assertTrue(list.getRowWidth() + KeyRowLayout.LIST_MARGIN <= list.getWidth(),
                        "the row fits the window for " + shot.name());
            });
            context.takeScreenshot(shot.name());
            if (CONTROLLING && shot.name().equals("controls-en-720p-scale2")) {
                checkControllingFreeKeys(context);
            }
        }
    }

    /** Controlling's list of free keys shows keys instead of mappings: no headings, no wider rows. */
    private static void checkControllingFreeKeys(ClientGameTestContext context) {
        context.clickScreenButton("options.toggleFree");
        context.waitTicks(2);
        context.runOnClient(client -> assertEquals(KeyRowLayout.VANILLA_ROW_WIDTH, list(client).getRowWidth(),
                "the list of free keys keeps its row width"));
        context.takeScreenshot("controls-controlling-free-keys");
        context.clickScreenButton("options.toggleFree");
        context.waitTicks(2);
    }

    /** Switches the game language the way the language screen does and waits for the resource reload. */
    private static void setLanguage(ClientGameTestContext context, String code) {
        boolean sameLanguage = context.computeOnClient(client -> client.getLanguageManager().getSelected().equals(code));
        if (sameLanguage) {
            return;
        }
        CompletableFuture<Void> reload = context.computeOnClient(client -> {
            client.getLanguageManager().setSelected(code);
            client.options.languageCode = code;
            return client.reloadResourcePacks();
        });
        // The loading overlay stays on screen and fades out after the reload itself has finished.
        context.waitFor(client -> reload.isDone() && !ClientScreens.hasOverlay(client), 1200);
        context.waitTicks(2);
    }

    private static KeyBindsScreen screen(Minecraft client) {
        return (KeyBindsScreen) Objects.requireNonNull(ClientScreens.current(client), "the key binds screen is open");
    }

    private static AlternativeKeySelection selection(Minecraft client) {
        return (AlternativeKeySelection) screen(client);
    }

    private static KeyBindsList list(Minecraft client) {
        for (GuiEventListener child : screen(client).children()) {
            if (child instanceof KeyBindsList list) {
                return list;
            }
        }
        throw new AssertionError("the key binds list is missing from the screen");
    }

    private static KeyBindsList.Entry entry(Minecraft client, KeyMapping mapping) {
        for (KeyBindsList.Entry entry : list(client).children()) {
            if (entry instanceof KeyEntryAccess row && row.kaboomkeybinds$mapping() == mapping) {
                return entry;
            }
        }
        throw new AssertionError("no row for " + mapping.getName());
    }

    private static Button button(Minecraft client, KeyMapping mapping, int index) {
        return (Button) entry(client, mapping).children().get(index);
    }

    private static Button resetAllButton(Minecraft client) {
        for (GuiEventListener child : screen(client).children()) {
            if (child instanceof Button button
                    && button.getMessage().getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("controls.resetAll")) {
                return button;
            }
        }
        throw new AssertionError("the Reset All button is missing from the screen");
    }

    /** Scrolls the row of the mapping to the top of the list and waits for a frame, which positions its buttons. */
    private static void scrollTo(ClientGameTestContext context, Function<Options, KeyMapping> mapping) {
        context.runOnClient(client -> {
            KeyBindsList list = list(client);
            KeyBindsList.Entry row = entry(client, mapping.apply(client.options));
            list.setScrollAmount(list.scrollAmount() + row.getY() - list.getY() - 2);
        });
        context.waitTicks(2);
    }

    /** Clicks one of the three buttons in the row of the mapping with the mouse, at its rendered position. */
    private static void clickRowButton(ClientGameTestContext context, TestInput input, Function<Options, KeyMapping> mapping,
            int index) {
        scrollTo(context, mapping);
        double[] center = context.computeOnClient(client -> {
            Button target = button(client, mapping.apply(client.options), index);
            Window window = client.getWindow();
            double scale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
            return new double[] {
                    (target.getX() + target.getWidth() / 2.0) * scale,
                    (target.getY() + target.getHeight() / 2.0) * scale};
        });
        input.setCursorPos(center[0], center[1]);
        context.waitTick();
        input.pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTick();
    }

    private static void assertConflict(Minecraft client, KeyMapping mapping, int index, boolean expected, String message) {
        assertEquals(expected, conflictMarked(client, mapping, index), message);
    }

    private static boolean conflictMarked(Minecraft client, KeyMapping mapping, int index) {
        return button(client, mapping, index).getMessage().getString().startsWith("[ ");
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
