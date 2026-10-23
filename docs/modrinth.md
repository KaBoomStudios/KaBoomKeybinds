# Modrinth project page

Texts and settings for the KaBoom Keybinds project on Modrinth, ready to copy into the project settings. The
description is a shorter version of the README for players. When the README changes, this file should follow.

Written against the Modrinth Content Rules last modified on 13 August 2026 and the support article on content
disclosures. Check both again before a release.

## Project settings

| Field        | Value |
|--------------|-------|
| Project type | Mod |
| Title        | `KaBoom Keybinds` |
| URL slug     | `kaboom-keybinds` |
| Summary      | see below |
| Categories   | Utility |
| Environment  | Client only (API value `client_only`): the client needs the mod, servers do not |
| License      | MIT (SPDX identifier `MIT`), license URL `https://github.com/KaBoomStudios/KaBoomKeybinds/blob/main/LICENSE` |
| Source code  | `https://github.com/KaBoomStudios/KaBoomKeybinds` |
| Issues       | `https://github.com/KaBoomStudios/KaBoomKeybinds/issues` |
| Icon         | The mod icon as a square PNG. Not created with AI (Content Rules 6.2) |

### Content disclosures

- **Contains AI-generated content:** on, for the areas **Code** and **Text**. Code: a substantial part of the code was
  written with AI tools (Content Rules 6.1). Text: the description, the summary, the changelogs and the German
  translation were drafted with AI tools (support article, section on text). **Assets** stay off: the icon is not
  AI-generated and the gallery shows real screenshots
- All other disclosures stay off: no AI functionality (the mod contains no AI), no advertisements, no paid features,
  no telemetry (the mod sends nothing anywhere), no derivative content, no photosensitivity warning, no interaction
  with external systems (the mod only reads and writes its own file in the `config` folder)

### Summary

Plain text without formatting and without the project name:

~~~~text
Gives every keybind an optional second key, keyboard or mouse, bound in a second column of the vanilla controls menu.
~~~~

### Gallery

Only real screenshots from the game, nothing generated or retouched (Content Rules 5.5 and 6.2). Suggested shots,
taken in a normal game with the English language and a GUI scale where the whole row is readable:

1. Controls menu with the "Key" and "Alternative" columns and a few alternative keys bound. Title: "Controls menu".
   Description: "Every keybind gets a second column for its alternative key."
2. An alternative button waiting for a key (`> <`). Title: "Binding an alternative key". Description: "Click the
   button and press any key or mouse button. Escape clears it."
3. A conflict with its tooltip. Title: "Conflicts". Description: "Keys used twice are marked on whichever button holds
   them, the tooltip names the other keybinds."
4. The menu of Controlling with the alternative column. Title: "Works with Controlling". Description: "The
   alternative column appears in Controlling's menu as well."

## Description

Markdown for the description field. Headings only start sections, the body is plain text (Content Rules 2.2).

~~~~markdown
KaBoom Keybinds gives every keybind an optional second key, the way shooters do it. The second key can be a keyboard key or a mouse button, and you bind it right in the normal controls menu, in a second column next to the first key. Both keys trigger the action equally.

## Features

- An optional alternative key for every keybind: all vanilla keybinds and the keybinds of other mods
- Keyboard keys and all mouse buttons, side buttons included
- Both keys are equal. Held actions (walking, sneaking, sprinting, attacking, using) and pressed actions (inventory, hotbar slots, drop, chat, perspective, screenshot) work with either key
- Holding both keys and releasing one keeps the action going
- Works in menus as well, for example closing the inventory or moving an item to a hotbar slot with the alternative key
- Conflicts are marked like in vanilla, on whichever button holds the conflicting key
- Works with [Controlling](https://modrinth.com/mod/controlling): its controls menu gets the alternative column as well
- Nothing changes until you bind an alternative key. The vanilla options file is never touched, so removing the mod leaves your controls as they were
- Needs nothing but Fabric Loader. Fabric API is not required
- English and German, following the game language

## How to use

Open Options, Controls, Key Binds. Every row has two key buttons, labelled "Key" and "Alternative", followed by the reset button. Click the alternative button and press a key or mouse button. Escape clears the alternative key. Reset sets the row back to its default key and clears the alternative key, Reset All does that for every keybind.

A key can only sit in one slot of a keybind: binding the alternative key as the first key (or the other way round) empties the other slot.

## Installation

1. Install Fabric Loader for your Minecraft version with the Fabric installer
2. Download the jar for your Minecraft version from the Versions tab and put it into the mods folder
3. Start the game with the Fabric profile

Supported are Minecraft 26.1, 26.1.1 and 26.1.2 (one jar for all three), 26.2 and 26.3, with Fabric Loader 0.19.5 or newer. The mod is client side only: it works in singleplayer and on any server, and servers do not need it.

## Configuration

Alternative keys are saved immediately to config/kaboomkeybinds.json and loaded at startup. Entries for keybinds of mods that are currently not installed stay in the file and return with the mod. A broken file does not crash the game: it is reported in the log and kept as kaboomkeybinds.json.broken.

Minecraft 26.3 renamed three keys: the keypad decimal point, the context menu key and the extra key next to left shift on ISO keyboards. The mod converts these names in both directions, so 26.3 and older versions can share one game folder.

## Known limitations

- Texts that name a key, such as tutorial hints and the debug overlay, show the first key only
- A few F3 debug shortcuts read the first key of "Debug modifier" and "Debug crash" directly. Alternative keys for these two keybinds do not work there
- After a menu closes, Minecraft restores held keys and toggled sneaking or sprinting only for keybinds whose first key is a keyboard key. A held alternative key follows that rule
- In 26.1.x, "Toggle Fullscreen" and "Take Screenshot" react to keyboard keys only. A mouse button does nothing there, as first or as alternative key
- In Controlling's menu, the filters "Show Unbound" and "Show Conflicts", the list of free keys and the search by key look at the first keys only
- Other mods that replace the controls menu are untested

## Source code and license

The source code is on [GitHub](https://github.com/KaBoomStudios/KaBoomKeybinds), bugs and suggestions are welcome in the [issue tracker](https://github.com/KaBoomStudios/KaBoomKeybinds/issues). The code is licensed under the MIT License. The icon is not covered by it: all rights reserved by KaBoom Studios.

Developed by KaBoom Studios with the help of AI tools for code and texts. Design decisions, testing and the icon are by KaBoom Studios.
~~~~

## Versions

One Modrinth version per jar. The jars are built with `./gradlew buildAndCollect` and collected in
`build/libs/1.0.0/`.

| Field          | Jar for 26.1.x                       | Jar for 26.2                       | Jar for 26.3                       |
|----------------|--------------------------------------|------------------------------------|------------------------------------|
| File           | `kaboomkeybinds-1.0.0+26.1.2.jar`    | `kaboomkeybinds-1.0.0+26.2.jar`    | `kaboomkeybinds-1.0.0+26.3.jar`    |
| Version number | `1.0.0+26.1.2`                       | `1.0.0+26.2`                       | `1.0.0+26.3`                       |
| Version name   | `1.0.0 for 26.1.x`                   | `1.0.0 for 26.2`                   | `1.0.0 for 26.3`                   |
| Channel        | Release                              | Release                            | Release                            |
| Loaders        | Fabric                               | Fabric                             | Fabric                             |
| Game versions  | 26.1, 26.1.1, 26.1.2                 | 26.2                               | 26.3                               |
| Dependencies   | Controlling, optional                | Controlling, optional              | Controlling, optional              |

Dependencies: there are no required ones (Content Rules 5.6). Fabric API must not be listed as required. Controlling
is listed as optional because the mod supports its menu, without a specific version. The sources jar
(`versions/<version>/build/libs/kaboomkeybinds-1.0.0+<version>-sources.jar`) may be added as an additional file
(Content Rules 5.7), but it is not needed.

### Changelog for 26.1.x

~~~~markdown
First public release.

- Optional alternative key for every keybind, keyboard key or mouse button, bound in a second column of the controls menu
- Both keys trigger held and pressed actions equally, in game and in menus
- Alternative keys are saved to config/kaboomkeybinds.json, the vanilla options file stays untouched
- Support for the controls menu of Controlling
- Shares the alternative key file with Minecraft 26.3 in both directions, including the three keys that 26.3 renamed
- English and German
- One jar for Minecraft 26.1, 26.1.1 and 26.1.2. In these versions, "Toggle Fullscreen" and "Take Screenshot" react to keyboard keys only
~~~~

### Changelog for 26.2

~~~~markdown
First public release.

- Optional alternative key for every keybind, keyboard key or mouse button, bound in a second column of the controls menu
- Both keys trigger held and pressed actions equally, in game and in menus
- Alternative keys are saved to config/kaboomkeybinds.json, the vanilla options file stays untouched
- Support for the controls menu of Controlling
- Shares the alternative key file with Minecraft 26.3 in both directions, including the three keys that 26.3 renamed
- English and German
~~~~

### Changelog for 26.3

~~~~markdown
First public release.

- Optional alternative key for every keybind, keyboard key or mouse button, bound in a second column of the controls menu
- Both keys trigger held and pressed actions equally, in game and in menus
- Alternative keys are saved to config/kaboomkeybinds.json, the vanilla options file stays untouched
- Support for the controls menu of Controlling
- Reads alternative key files from older Minecraft versions and converts the three keys that 26.3 renamed (keypad decimal point, context menu key, the extra key next to left shift on ISO keyboards), and older versions convert them back
- English and German
~~~~
