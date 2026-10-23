# KaBoom Keybinds

A client side Fabric mod for Minecraft Java Edition 26.2 that gives every keybind an optional second key, the way
shooters do it. The second key can be a keyboard key or a mouse button and is bound directly in the normal controls
menu through a second column. Both keys trigger the action equally.

## Features

- Every keybind gets an optional alternative key: all vanilla keybinds and the keybinds of other mods that use
  normal Minecraft keybinds
- The alternative key can be a keyboard key or any mouse button, including side buttons
- Both keys are equal. Held actions (walking, sneaking, sprinting, attacking) and pressed actions (inventory,
  hotbar slots, drop, chat, screenshot) work with either key
- Holding both keys and releasing one keeps the action active
- Works in menus as well, for example closing the inventory with the alternative inventory key or moving an item
  with an alternative hotbar key
- Bound in the vanilla controls menu through a second button next to the first one. Escape clears the alternative
  key, reset clears it as well, conflicts are highlighted like in vanilla
- Alternative keys are stored in their own file, the vanilla options file is never touched
- Needs nothing but Fabric Loader. Fabric API is not required
- English and German, following the game language

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.5 or newer
- Java 25 or newer (the Minecraft launcher ships a suitable runtime)

Fabric API is not required. It can be installed alongside, as most other mods need it.

## Installation

1. Install Fabric Loader for Minecraft 26.2 with the Fabric installer from https://fabricmc.net/use/installer/
2. Copy the mod jar (`kaboomkeybinds-0.1.0+26.2.jar`) into the `mods` folder of your Minecraft installation
3. Start the game with the Fabric profile

The mod is client side only. Servers do not need it and players without it are not affected. It can be removed at any
time: the vanilla controls stay as they are, and the alternative keys wait in their own file until the mod is back.

## Usage

Open Options, Controls, Key Binds. Each row shows the key button, a second button for the alternative key and the
reset button. The two columns are labelled "Key" and "Alternative". Click the alternative button and press the key or
mouse button you want. Escape clears the alternative key. Key names come from Minecraft and follow the game language.

A key can only sit in one slot of the same keybind: binding the current alternative key as key (or the other way
round) empties the other slot. An alternative key counts as a change, so the reset button of the row and Reset All
become active as soon as one is bound. Reset restores the default key and clears the alternative key, Reset All does
that for every keybind. Conflicts are marked like in vanilla, on whichever button holds the conflicting key, and the
tooltip names the other keybinds that use it.

## Configuration

There is no settings screen besides the controls menu. Alternative keys are saved immediately to
`config/kaboomkeybinds.json` inside the game folder. The file maps keybind names to the key names Minecraft uses:

```json
{
  "version": 1,
  "keys": {
    "key.inventory": "key.mouse.5",
    "key.sneak": "key.keyboard.right.shift"
  }
}
```

Mouse buttons are called `key.mouse.left`, `key.mouse.right`, `key.mouse.middle` and `key.mouse.4` upwards. Entries
for keybinds that are currently not present (for example from a mod that was removed) are kept, so they return when
that mod is installed again. A broken file does not crash the game: it is reported in the log, the alternative keys
stay empty, and the file is moved to `kaboomkeybinds.json.broken` before the next save so nothing is lost. An entry
with a key name Minecraft does not know is reported, left in the file and treated as empty.

## Building from source

Requires an installed JDK 25. Gradle itself runs on it as well: the daemon JVM criteria in
`gradle/gradle-daemon-jvm.properties` make the wrapper pick an installed JDK 25 for the build, whatever Java is on the
`PATH` or in `JAVA_HOME`. Gradle finds JDKs in the usual places (`JAVA_HOME`, IntelliJ IDEA installations, the
locations of the operating system). If it cannot find one, point `org.gradle.java.installations.paths` in your
personal `~/.gradle/gradle.properties` at the JDK.

One source tree builds a separate jar for every supported Minecraft version, using
[Stonecutter](https://stonecutter.kikugie.dev/). Each version is a Gradle subproject in `versions/<version>/` with its
own `gradle.properties` (the Minecraft versions the jar accepts and the Fabric API version for development). The build
script `build.gradle.kts` and the sources in `src/` are shared. Code that differs between Minecraft versions is marked
with Stonecutter comments such as `//? if >=26.3 {`. Run

```
./gradlew buildAndCollect
```

to build and test every version and copy the jars to `build/libs/<mod version>/`. The version of each jar carries the
Minecraft version as build metadata, for example `kaboomkeybinds-1.0.0+26.2.jar`.

A task name without a project runs the task for every version. To run it for one version, put the version in front:

- `./gradlew :26.2:build` builds and tests the jar for Minecraft 26.2, written to `versions/26.2/build/libs`
- `./gradlew :26.2:test` runs the unit tests
- `./gradlew :26.2:runClient` starts a development client with the mod. Fabric API is loaded there as well, so
  keybinds of other mods can be tried. The clients of all versions share the `run` folder in the project root
- `./gradlew :26.2:runClientGameTest` runs the automated client tests. They start the game, bind keys with simulated
  input and write screenshots of the controls menu to `versions/26.2/build/run/clientGameTest/screenshots`. That
  folder is cleared before every test run

The sources in `src/` are always in the state for one active version, which IntelliJ IDEA uses for highlighting and
code completion. In the repository that is Minecraft 26.2. `./gradlew "Set active project to 26.3"` switches the
sources to another version, `./gradlew "Reset active project"` switches them back and should be run before every
commit. IntelliJ IDEA lists these tasks in the Gradle tool window under `stonecutter`.

## Status

Version 0.1.0 is the first release. It supports Minecraft 26.2 with Fabric Loader, in English and German. Version 1.0.0
is in development: it adds jars for Minecraft 26.1.x and 26.3 and a release on Modrinth.

## Known limitations

- Texts that name a key show the primary key only: tutorial hints, the demo screen, the debug overlay and similar
  messages
- The debug shortcuts (F3 combinations) read the primary key of the "Debug modifier" (F3) and "Debug crash" (C)
  keybinds directly in a few places: while a screen is open, and for holding F3 and C together. Alternative keys for
  these two keybinds do not work there
- After a menu closes, Minecraft re-reads held keys only for keybinds whose primary key is a keyboard key, and
  restores toggled sneaking and sprinting under the same condition. A held alternative key follows that rule, so it is
  picked up again only when the primary key of its keybind is a keyboard key
- In very narrow windows, names that do not fit next to the buttons are shortened with an ellipsis, and at the
  smallest row width the button labels can overflow the buttons, as they do in vanilla
- Mods that replace the controls menu (for example Controlling) are untested. Mods that only add keybinds work, the
  automated tests cover one registered through Fabric API
