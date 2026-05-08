# HereCroppy

A [Paper](https://papermc.io) Minecraft plugin for **auto-farming** — automatically harvest ripe crops, collect drops, and replant seeds in a player-defined rectangular area.

## Features

- **Area selection** — Shift-right-click with a hoe to set two corners (Point A and Point B) of your farming area.
- **Area scanning** — After setting Point B, the area is scanned and classified as farmable, passable, door, or obstructed. You get a summary before starting.
- **Smart pathfinding** — Generates a safe snake path that includes farmable and passable blocks (e.g. cobblestone paths) while avoiding obstacles like lava, water, fences, and stairs.
- **Auto-farming toggle** — Start or stop auto-farming with `/herecroppy start` or `/herecroppy stop`.
- **Restart from last block** — If farming stops because your inventory is full, use `/herecroppy restart` to resume from the last visited block.
- **Rescan on loop** — After completing a full loop, the area is rescanned to detect new obstacles or changes.
- **Ripe crop detection** — Detects and harvests fully grown crops (Wheat, Carrots, Potatoes, Beetroots, Nether Wart).
- **Automatic replanting** — Replants crops immediately after harvesting using seeds from your inventory.
- **Auto-tilling** — If your selected area contains untilled dirt or grass, plugin uses your held hoe to prepare it into farmland.
- **Auto-planting** — On empty farmland or soul sand, plugin automatically plants first available seeds from your inventory.
- **Drop collection** — Automatically collects dropped items after breaking crops.
- **Inventory full detection** — Automatically stops when your inventory is full and allows restart.
- **Offline cleanup** — Automatically removes players from auto-farming when they disconnect.
- **AuraSkills integration** — When AuraSkills is installed, plugin inspects player's Farming skill level for extra buffs:
  - **Bonus AuraSkills XP** — AuraSkills Farming XP gains are boosted based on farming level.
  - **Fortune drops** — Chance for double crop drops based on farming level.
  - **Faster farming** — Slightly increased movement speed between crops based on farming level.

## Requirements

- Paper 1.21+ server
- Java 21+
- (Optional) AuraSkills 2.x for farming skill XP

## Installation

1. Download the latest `HereCroppy-*.jar` from the [releases page](../../releases).
2. Drop the JAR into your server's `plugins/` folder.
3. Restart the server.

## Building from Source

**Using Gradle (recommended):**
```bash
./gradlew build
```

The compiled JAR will be in `build/libs/HereCroppy-1.0.6.jar`.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/herecroppy start` | Start auto-farming in your selected area | `herecroppy.use` |
| `/herecroppy stop` | Stop auto-farming | `herecroppy.use` |
| `/herecroppy restart` | Resume from last block after inventory-full stop | `herecroppy.use` |
| `/herecroppy clear` | Clear your current selection | `herecroppy.use` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `herecroppy.use` | Allows use of the `/herecroppy` command | `true` |

## How to Use

1. Hold any hoe in your main hand.
2. Shift-right-click a block to set **Point A**.
3. Shift-right-click another block to set **Point B**. The area will be scanned and you'll see a summary.
4. Run `/herecroppy start` to begin auto-farming.
5. The plugin will move your character through the area, harvesting ripe crops and replanting them.
6. If your inventory fills up, farming pauses. Use `/herecroppy restart` to resume from the last block.
7. Run `/herecroppy stop` to stop manually.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
