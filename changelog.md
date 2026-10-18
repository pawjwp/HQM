### 6.0.0 for 1.20.1 (17.10.2026 by pawjwp):
This update adds the Multipurpose Assistance Terminal (MAT), an item that combines the quest book with several other tools. This includes a comprehensive tutorial system that can be used by modpack authors to explain thing in-game with on-screen text boxes. See the [wiki](https://github.com/pawjwp/HQM/wiki) for more information.

#### The MAT
- Added the MAT itself, which can be toggled on/off with the `ENABLE_MAT` config option
    - Right-clicking opens its current mode, and sneak + right-click switches to the next mode
    - The item's name and color changes to show its current mode
    - Each mode can be toggled separately with their own config options
- The MAT has a tab bar at the top of each screen to switch between modes
    - If the `TABS_SWITCH_DEFAULT_MODE` config option is enabled, switching tabs also changes the mode of the MAT item itself
- Each MAT mode has a keybind, which are unbound by default
    - If the MAT is enabled, the "Open Quests" keybind will prioritize opening the MAT over the quest book
- The `LOSE_QUEST_BOOK` config option applies to the MAT as well

#### MAT Modes
- Info mode lists the player's unlocked tutorials and statistics
    - Statistics
        - Time played, deaths, and mob kills are unlocked by default shown in the Info screen's statistics list
        - Any vanilla or modded statistic can be unlocked (see the [Minecraft Wiki](https://minecraft.wiki/w/Statistics) for information about each statistic)
        - Three custom "life signs" statistics are available that show the number of hostile, neutral, and friendly mobs near the player (the nearby search radius can be changed with the `LIFE_SIGNS_RADIUS` config option)
    - Tutorials
        - The tutorial system allows modpack authors to explain things in-game with text boxes (see the [wiki](https://github.com/pawjwp/HQM/wiki) for more detailed documentation)
        - Tutorials are defined as JSON files in `config/hqm/default/tutorials/`
        - A tutorial called "The MAT" is present in the tutorial folder by default and serves to both introduce players to the MAT and be used as a reference for making new tutorials
        - Tutorials can be reloaded with `/hqm mat reload` or the vanilla `/reload` command
- Questing mode opens a copy of the quest book with the MAT's theming
- Crafting mode acts as a portable crafting table
- Tracking mode lists the player's tracked locations
    - Selecting a location in the tracking mode UI shows its distance, dimension, and biome, and a map of the area
    - When in tracking mode with a selected location, the MAT item will get a pointer that points toward the selected location like a compass
    - Exact coordinates and distances are hidden unless the `SHOW_TRACKING_COORDINATES` config option is enabled
- Storage mode is still WIP and shows a placeholder screen

#### Unlocking MAT Content
- A series of three new "data chip" items allow the player to unlock content in their MAT
    - Seperate items exist for unlocking tutorials, statistics, and locations
    - The NBT of each chip controls what it unlocks and must be set by modpack authors
    - Location chips can search for the coordinates of the nearest of a certain structure or biome and unlock those coordinates
- Added the `/hqm mat` command
    - `unlock` and `remove` adds/removes tutorials, statistics, or locations
    - `reload` reloads tutorial files without requiring a restart
    - `tutorial play` and `tutorial pause` start or pause tutorials for players
- Unlocking content in the MAT provides a toast popup by default stating what was unlocked

### 5.20.0 for 1.20.1 (16.10.2026 by pawjwp):
This is the first update by [pawjwp](https://github.com/pawjwp), as part of the new Multipurpose Assistance Terminal mod, forked from HQM [here](https://github.com/pawjwp/HQM). This update includes a keybind to open the quest book, automatic quest file backups, JEI integration, biome and structure versions of location tasks, a new trigger type, grid snapping in the quest editor, configurable quest book colors, and various other improvements and bug fixes. See the [Questing Changes](https://github.com/pawjwp/HQM/wiki/Questing-Changes) wiki page for more information.

#### Quest Book
- Added an "Open Quests" keybind
    - If the `REQUIRE_BOOK` config option is disabled, players don't need a quest book in their inventory to use the keybind
- Aligned/centered quests, quest icons, and connecting lines
    - The hexagonal quest markers are now properly centered, symmetrical, and an even number of pixels wide
    - The items used as quest icons are now centered in their frame
    - Connecting lines between quests are no longer drawn at 0.96x scale, meaning they are now centered to their quests instead of offset slightly to the top left corner
    - Connecting lines are slightly thinner than before, but can be adjusted from the `LINE_THICKNESS` config option
- Added an experimental UI scale option
    - The `UI_SCALE_MULTIPLIER` config option can be edited to draw the UI at a larger scale when HQM screens are open (to make small text easier to read)
- Added JEI integration
    - JEI's "show recipe" and "show uses" hotkeys now work on items in quest tasks and rewards
    - If the `SHOW_JEI_SIDEBAR` config option is enabled, JEI can display on the side of the quest book as well
- Configurable quest book colors
    - Most hardcoded color values can now be changed in the config, including all text colors
    - Text colors are automatically reloaded from the config whenever it changes (along with most other clientside config options)
- Fixed quest set completion percentage calculation
    - The calculation now excludes quests that can't be completed, so trigger quests and quests locked by option links no longer count against them

#### Quest Files
- Added quest save file backups (in the style of the vanilla `level.dat` backups)
    - All data files like player quest progress and team data now saves the previous version when saving
    - Missing or invalid files automatically restore from their backups
    - Files are written safely so crashing mid-save won't erase a save file and will just fall back to the previous one, usually from a few minutes earlier
- Each quest in quest set files now stores the coordinates of its centerpoint instead of its top-left corner
    - Old files are automatically migrated on first load/save
- Quest and team data is now stored in UUID order so new save files don't reorder the whole file, and it shows up cleanly in version control

#### Quest Editing
- Location tasks can now require a specific biome or structure to be detected, as well as the position and dimension they required before
- Added the "Preview Reversed Trigger" trigger type
    - Makes a quest visible when one of its parent quests is unlocked, so players can see one quest ahead
- Specific item tag precision
    - Item tasks set to tag precision can now require a specific tag instead of all tags that item matches against
    - A new "Cycle display" toggle lets the item icon cycle through each item in the tag
- Detect item tasks now count items in the armor slots and offhand
    - Consume item tasks still only take items from the main inventory
- Added optional grid snapping in the quest editor
    - When the `SNAP_TO_GRID` config option is enabled, quest markers snap to a configured grid while being moved
    - Hold Shift to ignore the grid
- Quest set names and the quest book's main description can now use translation keys

#### Bug Fixes
- Fixed `LOSE_QUEST_BOOK` config option not working on Fabric
- Fixed the front page image (`front.png`) being drawn without proper transparency
- Fixed heart recipes not unlocking in the recipe book when picking them up for the first time

#### Upgrade notes
- Older quest packs are automatically migrated upon load
    - `description.txt` is replaced by `description.json` to support more fields and translation
    - Quest set files
        - Quest marker coordinates are converted to the marker's center instead of its top-left corner 
        - Quest set file names are changed to be safe on every file system
        - Quest set files now store a version number so they only attempt to migrate older versions
- Resource pack changes
    - The quest markers at `textures/gui/questmap.png` were slightly moved and increased in size to properly center them
    - The texture `threequarts.png` was renamed to `threequartsheart.png` for consistency

### 5.16.0 for 1.20.1 (30.10.2023 by Kirderf1):
- Added "/hqm reset" command
- Fix world load crash when the world has quest progress data with a missing quest

### 5.15.0 for 1.20.1 (23.08.2023 by Kirderf1):
- Updated to mc 1.20.1

### 5.14.2 for 1.19.4 (30.10.2023 by Kirderf1):
- Fix world load crash when the world has quest progress data with a missing quest

### 5.14.1 for 1.19.4 (15.08.2023 by Kirderf1):
- Fix multiplayer data sync issue introduced in 5.13.0

### 5.14.0 for 1.19.4 (26.05.2023 by Kirderf1):
- Updated to minecraft 1.19.4

### 5.13.1 for 1.19.2 (24.05.2023 by Kirderf1):
- Fix line wrapping for item reward tooltips in the quest book

### 5.13.0 for 1.19.2 (22.01.2023 by Kirderf1):
- Support for addons to add new quest task types
- When SPAWN_BOOK is turned on, spawn a quest book regardless if edit mode is on or not
- Potentially fix an obscure crash while editing quests
- Fix loot tier deletion crash

### 5.12.2 for 1.19.2 (18.10.2022 by Kirderf1):
- Fix reputation and reputation marker names
- Fix bag loot group renaming crash

### 5.12.1 for 1.19.2 (25.08.2022 by Kirderf1):
- Updated block and item textures

### 5.12.0 for 1.19.2 (25.08.2022 by Kirderf1):
- Updated to minecraft 1.19.2
- Now requires architectury api as a dependency

### 5.11.3 for 1.18.2 (24.05.2023 by Kirderf1):
- Fix line wrapping for item reward tooltips in the quest book

### 5.11.2 for 1.18.2 (18.10.2022 by Kirderf1):
- Fix reputation and reputation marker names
- Fix bag loot group renaming crash

### 5.11.1 for 1.18.2 (11.08.2022 by Kirderf1):
- Add loot tables to blocks
- Restore functionality of the tracker block
- Fix issue with the delivery system block entity
- Fluid insertion into the delivery system is now more correctly handled

### 5.11.0 for 1.18.2 (10.06.2022 by Kirderf1):
- Editable text can now optionally be translated

### 5.10.1 for 1.18.2 (15.04.2022 by Kirderf1):
- Fix issue with certain item tag data in quests

### 5.10.0 for 1.18.2 (20.03.2022 by Kirderf1):
- Update to minecraft 1.18.2

### 5.9.3 for 1.18.1 (17.02.2022 by Kirderf1):
- Fix singleplayer crash related to the party menu

### 5.9.1 for 1.18.1 (25.01.2022 by Kirderf1):
- Fixes a crash from deserializing an item stack special case

### 5.9.0 for 1.18.1 (18.12.2021 by Kirderf1):
- Update to 1.18.1
- Fix buttons for item quest tasks

### 5.8.2 for 1.17.1 (03.02.2022 by Kirderf1):
- Fixes a crash from deserializing an item stack special case

### 5.8.1 for 1.17.1 (19.12.2021 by Kirderf1):
- Fix buttons for item quest tasks

### 5.8.0 for 1.17.1 (01.12.2021 by Kirderf1):
- Checkbox quest task
- Text boxes are now more feature-complete
- Tweak how some lang entries work

### 5.7.2 for 1.17.1 (08.10.2021 by canitzp):
- Fixed numerical hotkeys for edit modes during quest editing
- Fixed missing scroll bar when picking reputation type for reputation bars on quest maps
- Fixed some mouse-wheel-scrolling issues with HQM's scroll bars
- Fixed incorrect item reward limits
- Removed SERVER_SYNC config option
- Fix issue with loading reputation tasks across multiple quest sets

### 5.7.0 for 1.17.1 (23.09.2021 by canitzp):
- Fluid support for detection tasks
- Fluid support for quest and task icons
- When setting an item or fluid to a quest task, it will also be used as the quest icon if the quest doesn't already have an icon
- Added recipe type `hardcorequesting:book_catalyst_shaped` to handle recipes where the quest book shouldn't be consumed
- Quest book items are no longer considered a container of themselves for recipes (recipes from modpacks that depend on this functionality need to be updated)
- Fix reward screen issue
- Tweak to mod loading
- Fluid rendering fixes
- Minor tweak to how item icons are serialized/deserialized

### 5.6.2 for 1.17.1 (02.09.2021 by canitzp):
- Includes changes meant for 5.6.1 that were not actually in 5.6.1
- Fixes an extra rendering issue
- Presence of reputation rewards should no longer prevent command rewards on the same quest from executing

### 5.6.1 for 1.17.1 (31.08.2021 by canitzp):
- Fix crash from old line draw code
- Fix some rendering issues

### 5.6.0 for 1.17.1 (30.08.2021 by canitzp):
- Update to 1.17.1

### 5.5.17 for 1.16.5 (15.04.2022 by Kirderf1):
- Fix singleplayer crash related to the party menu
- Fix issue with certain item tag data in quests

### 5.5.16 for 1.16.5 (03.02.2022 by Kirderf1):
- Fixes a crash from deserializing an item stack special case

### 5.5.15 for 1.16.5 (08.10.2021 by canitzp):
- Removed SERVER_SYNC config option
- Fix issue with loading reputation tasks across multiple quest sets
- Fixed numerical hotkeys for edit modes during quest editing
- Fixed missing scroll bar when picking reputation type for reputation bars on quest maps
- Fixed some mouse-wheel-scrolling issues with HQM's scroll bars
- Fixed incorrect item reward limits

### 5.5.13 for 1.16.5 (30.09.2021 by canitzp):
- Includes fixes meant for 5.5.12 that was missing from last version
- Some minor backported changes

### 5.5.12 for 1.16.5 (08.09.2021 by canitzp):
- Fix place block task consuming items instead of observing items
- Tweak how certain lines are scaled with the display window
- Fix an issue with combining reputation and command rewards on the same quest
- Small logging message about loaded quests

### 5.5.10 for 1.16.5 (27.08.2021 by canitzp):
- Tame tasks now default icons to mob eggs again
- Added soft limits on the number of parts in a task based on graphical space
- Fix quest task description issues by dropping translation key support for task descriptions (translation key support will be added back in more properly at a later date)
- Fix missing data packets when connecting to a LAN-world
- Fix reward bag issue on dedicated servers
- Fix issues with command rewards
- Fix performance issue with setting a quest parent

### 5.5.8 for 1.16.4-1.16.5 (15.08.2021 by canitzp):
- Fix various problems with repeatable quests
- Fix crash during multiplayer with certain quest tasks
- Fix issue with deserialization of reputation quest tasks from json
- Fix certain tasks getting triggered extra times under certain circumstances
- Fix problem with task requirements dissapearing on load when setting item amounts higher than 127 through the gui
- Fix bag group text bar
- Fix crash from deleting an empty reward under certain circumstances
- Fix reputation not getting unselected on deletion
- Fluids in the player inventory now show up again when selecting items for consumable tasks

### 5.5.6 for 1.16.2-1.16.5 (31.01.2021 by shedaniel):
- no changelog was provided on curseforge

### 5.5.5 for 1.16.2-1.16.5 (30.01.2021 by shedaniel):
- no changelog was provided on curseforge

### 5.5.4 for 1.16.1-1.16.5 (08.11.2020 by shedaniel):
- no changelog was provided on curseforge

### 5.5.3 for 1.16.2-1.16.3 (28.10.2020 by shedaniel):
Fix #516, fix #520

### 5.5.2 for 1.16.2-1.16.3 (13.10.2020 by shedaniel):
Fix #551

### 5.5.1 for 1.16.2-1.16.3 (10.10.2020 by shedaniel):
Fix multiplayer party
Fix crash on server launch
Fix tasks not saving properly

### 5.5.0 for 1.16.2-1.16.3 (09.10.2020 by shedaniel):
- no changelog was provided on curseforge

### 5.4.0-hotfix1 for 1.12.2 (13.05.2019 by Noobanidus):
- Fix a bug with EnderIO conduits causing a Null Pointer Error. This may make the detection of breaking these less precise or prevent them from functioning as a quest objective, but they didn't previously function as a quest objective without crashing, so it's not a huge loss.

### 5.4.0 for 1.12.x (xx.xx.201x by canitzp, duely):
To accomplish this release of HQM we used a special made modpack with a special server to "field test" the mod completely. Most bugs are fixed through that!

- A lot of internal changes, especially changing string uuid instances to proper UUID instances
- **Fixing server bug, that quests are missing after a rejoin or server restart! This was a very often heard, but difficult to reproduce bug**
- Quest sync from a server does now work properly, even if the server quests are updated
- The QDS is now functioning properly again
- Click on a requirement item in the quest books opens the jei recipes for it. thanks duely
- Added TUMAT and TheOneProbe integration (same behaviour as Waila/Hwyla)

### 5.3.5 for 1.12.x (19.12.2018 by canitzp, duely, codewarrior0, TataTawa):
- Some code cleanup and therefor fixing a team creation bug #407 (and several similar issues)
- Added command to toggle edit mode, without game restart! Thanks duely
- Fixing grey tooltip in reward gui. Thanks to codewarrior0 and duely
- Performance increase in large quest books up to 5000%! Thanks TataTawa
- Fixed some null ItemStacks. Thanks duely

### 5.3.4 for 1.12.x (04.10.2018 by canitzp & duely):
- [Support larger quest data sizes by splitting information. thanks to duely](https://github.com/lorddusk/HQM/commit/c2ba8e2c1be5dfea8c25a98223b4696468bead8d)
- Fixed some QDS issues (#396)
- Blind fixed Location Task issue (#400)

### 5.3.3 for 1.12.x (?):
- no record of changes since this version is completely removed from curseforge

### 5.3.2 for 1.12.x (15.08.2018 by canitzp):
- Blind fix #392 and prevented potential bugs
- Fix for reward bag lags and crashes #318
- Fixed Bags are in all Creative Tabs

BETA version since 20.08.2018, cause I got no new issues at GitHub. pls provide every error found, at the issue tracker

### 5.3.1 for 1.12.x (14.08.2018 by canitzp):
- Fixed startup crash due to not obfuscation of methods and fields. I don't know why this sometimes happens

### 5.3.0 for 1.12.x (14.08.2018 by canitzp):
_This version has some serious compiler issues and is not usable. -> 5.3.1_
- General fore and dependences update
- Update Russian language (thx to yaroslav4167)
- Fix #284 (Fluid NPE)
- Blind Fix #275 (Reward Label NPE)
- Blind Fix #377 (Crash on new world start cause ConcurrentModificationException)
- Blind Fix for QuestData syncing error, when too much quests


### 5.2.0 Alpha1 for 1.12.x (24.10.2017 by canitzp):
- Update to all 1.12 versions