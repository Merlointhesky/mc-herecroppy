# HereCroppy

A [Paper](https://papermc.io) Minecraft plugin for **auto-farming** — automatically harvest ripe crops, collect drops, and replant seeds in a player-defined rectangular area.

## Features

- **Area selection** — Shift-right-click with a hoe to set two corners (Point A and Point B) of your farming area.
- **Auto-farming toggle** — Start or stop auto-farming with `/herecroppy start` or `/herecroppy stop`.
- **Snake-pattern path** — Automatically traverses the selected area in an efficient snake-like pattern.
- **Ripe crop detection** — Detects and harvests fully grown crops (Wheat, Carrots, Potatoes, Beetroots, Nether Wart).
- **Automatic replanting** — Replants crops immediately after harvesting using seeds from your inventory.
- **Auto-tilling** — If your selected area contains untilled dirt or grass, the plugin uses your held hoe to prepare it into farmland.
- **Auto-planting** — On empty farmland or soul sand, the plugin automatically plants the first available seeds from your inventory.
- **Drop collection** — Automatically collects dropped items after breaking crops.
- **Inventory full detection** — Automatically stops when your inventory is full.
- **Offline cleanup** — Automatically removes players from auto-farming when they disconnect.
- **AuraSkills integration** — When AuraSkills is installed, the plugin inspects the player's Farming skill level for extra buffs:
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

The compiled JAR will be in `build/libs/HereCroppy-1.0.4.jar`.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/herecroppy start` | Start auto-farming in your selected area | `herecroppy.use` |
| `/herecroppy stop` | Stop auto-farming | `herecroppy.use` |
| `/herecroppy clear` | Clear your current selection | `herecroppy.use` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `herecroppy.use` | Allows use of the `/herecroppy` command | `true` |

## How to Use

1. Hold any hoe in your main hand.
2. Shift-right-click a block to set **Point A**.
3. Shift-right-click another block to set **Point B**.
4. Run `/herecroppy start` to begin auto-farming.
5. The plugin will move your character through the area, harvesting ripe crops and replanting them.
6. Run `/herecroppy stop` to stop, or it will stop automatically if your inventory fills up.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
