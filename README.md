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
- Works in menus as well, for example closing the inventory with the alternative inventory key
- Bound in the vanilla controls menu through a second button next to the first one. Escape clears the alternative
  key, reset clears it as well, conflicts are highlighted like in vanilla
- Alternative keys are stored in their own file, the vanilla options file is never touched

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.5 or newer
- Java 25 or newer (the Minecraft launcher ships a suitable runtime)

Fabric API is not required.

## Installation

1. Install Fabric Loader for Minecraft 26.2 with the Fabric installer from https://fabricmc.net/use/installer/
2. Copy the mod jar (for example `kaboomkeybinds-0.1.0+26.2.jar`) into the `mods` folder of your Minecraft installation
3. Start the game with the Fabric profile

The mod is client side only. Servers do not need it and players without it are not affected.

## Usage

Open Options, Controls, Key Binds. Each row shows the key button, a second button for the alternative key and the
reset button. The two columns are labelled "Key" and "Alternative". Click the alternative button and press the key or
mouse button you want. Escape clears the alternative key.

A key can only sit in one slot of the same keybind: binding the current alternative key as key (or the other way
round) empties the other slot. Reset restores the default key and clears the alternative key, Reset All does that for
every keybind. Conflicts are marked like in vanilla, on whichever button holds the conflicting key, and the tooltip
names the other keybinds that use it.

## Configuration

Alternative keys are saved immediately to `config/kaboomkeybinds.json` inside the game folder. The file maps keybind
names to Minecraft key names:

```json
{
  "version": 1,
  "keys": {
    "key.inventory": "key.mouse.5"
  }
}
```

Entries for keybinds that are currently not present (for example from a mod that was removed) are kept so they return
when the mod is installed again. A broken file does not crash the game. It is reported in the log and the affected
alternative keys stay empty.

## Building from source

Requires an installed JDK 25. Gradle itself runs on it as well: the daemon JVM criteria in
`gradle/gradle-daemon-jvm.properties` make the wrapper pick an installed JDK 25 for the build, whatever Java is on the
`PATH` or in `JAVA_HOME`. Gradle finds JDKs in the usual places (`JAVA_HOME`, IntelliJ IDEA installations, the
locations of the operating system). If it cannot find one, point `org.gradle.java.installations.paths` in your
personal `~/.gradle/gradle.properties` at the JDK. Run

```
./gradlew build
```

The jar is written to `build/libs`. The version carries the Minecraft version as build metadata, for example
`kaboomkeybinds-0.1.0+26.2.jar`.

Useful tasks:

- `./gradlew test` runs the unit tests
- `./gradlew runClient` starts a development client with the mod
- `./gradlew runClientGameTest` runs the automated client tests and writes screenshots to
  `build/run/clientGameTest/screenshots`

## Status

In development. Version 0.1.0 is not released yet.

## Known limitations

To be documented with the first release.
