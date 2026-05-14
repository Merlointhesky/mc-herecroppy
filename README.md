# HereCroppy

A [Paper](https://papermc.io) Minecraft plugin for **auto-farming** — automatically harvest ripe crops, collect drops, and replant seeds in a player-defined rectangular area with advanced configuration options.

## Features

- **Area selection** — Shift-right-click with a hoe to set two corners (Point A and Point B) of your farming area.
- **Area scanning** — After setting Point B, the area is scanned and classified as farmable, passable, door, or obstructed. You get a summary before starting.
- **Smart pathfinding** — Generates a safe snake path that includes farmable and passable blocks (e.g. cobblestone paths) while avoiding obstacles like lava, water, fences, and stairs.
- **Auto-farming toggle** — Start or stop auto-farming with `/herecroppy start` or `/herecroppy stop`.
- **Restart from last block** — If farming stops because your inventory is full, use `/herecroppy restart` to resume from the last visited block.
- **Rescan on loop** — After completing a full loop, the area is rescanned to detect new obstacles or changes.
- **Ripe crop detection** — Detects and harvests fully grown crops (Wheat, Carrots, Potatoes, Beetroots, Nether Wart, Sugar Cane, Pumpkins, Watermelons).
- **Automatic replanting** — Replants crops immediately after harvesting using seeds from your inventory.
- **Auto-tilling** — If your selected area contains untilled dirt or grass, plugin uses your held hoe to prepare it into farmland.
- **Auto-planting** — On empty farmland or soul sand, plugin automatically plants first available seeds from your inventory.
- **Drop collection** — Automatically collects dropped items after breaking crops.
- **Inventory full detection** — Automatically stops when your inventory is full and allows restart.
- **Offline cleanup** — Automatically removes players from auto-farming when they disconnect.
- **Setup Wizard** — Configure optional dump boxes and bonemeal collection with `/herecroppy setup`:
   - **Dump unwanted crops** — Automatically deposit unwanted crops in a designated box.
   - **Dump keep crops** — Automatically deposit desired crops in a separate storage box.
   - **Bonemeal collection** — Automatically collect and apply bonemeal to crops during farming.
- **Crop Configuration UI** — Fine-tune farming behavior per crop with `/herecroppy config`:
   - **Enable/disable seeding** — Control which crops are planted.
   - **Enable/disable collecting** — Control which crops are harvested.
   - **Enable/disable bonemeal** — Control which crops receive bonemeal.
   - **Enable/disable seed dumping** — Control which seeds are dumped to storage (default: dump all seeds).
- **Smart Bonemeal Usage** — Bonemeal is automatically collected and applied to unripe crops to speed up growth (respects per-crop configuration).
- **Proper Pumpkin/Watermelon Harvesting** — Collects fruit blocks without breaking stems, allowing continuous production.
- **Correct Sugar Cane Harvesting** — Breaks sugar cane at height 2 above ground, leaving 1 block to regrow naturally.
- **Soul Sand Support** — Properly recognizes and farms nether wart on soul sand blocks.
- **AuraSkills integration** — When AuraSkills is installed, plugin inspects player's Farming skill level for extra buffs:
   - **Bonus AuraSkills XP** — AuraSkills Farming XP gains are boosted based on farming level.
   - **Fortune drops** — Chance for double crop drops based on farming level.
   - **Faster farming** — Slightly increased movement speed between crops based on farming level.

## Supported Crops

The plugin supports the following crops with automatic harvesting and replanting:

| Crop | Seed Item | Ground Type | Special Requirements |
|------|-----------|-------------|----------------------|
| Wheat | Wheat Seeds | Farmland | None |
| Carrots | Carrot | Farmland | None |
| Potatoes | Potato | Farmland | None |
| Beetroots | Beetroot Seeds | Farmland | None |
| Nether Wart | Nether Wart | Soul Sand | None |
| Sugar Cane | Sugar Cane | Dirt/Grass/Farmland | Must be adjacent to water |
| Pumpkins | Pumpkin Seeds | Farmland | Harvests from stem, produces pumpkin block |
| Watermelons | Melon Seeds | Farmland | Harvests from stem, produces melon block |

**Note:** Sugar cane requires water adjacency and will only be planted on blocks next to water. Pumpkins and watermelons grow from stems on farmland and produce their respective blocks when fully grown.

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

The compiled JAR will be in `build/libs/HereCroppy-1.1.2.jar`.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/herecroppy start` | Start auto-farming in your selected area | `herecroppy.use` |
| `/herecroppy stop` | Stop auto-farming | `herecroppy.use` |
| `/herecroppy restart` | Resume from last block after inventory-full stop | `herecroppy.use` |
| `/herecroppy clear` | Clear your current selection and setup configuration | `herecroppy.use` |
| `/herecroppy setup` | Start the setup wizard to configure dump boxes and bonemeal collection | `herecroppy.setup` |
| `/herecroppy config` | Open the crop configuration UI to enable/disable per-crop settings | `herecroppy.config` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `herecroppy.use` | Allows use of the `/herecroppy` command | `true` |
| `herecroppy.setup` | Allows use of the setup wizard | `true` |
| `herecroppy.config` | Allows use of the crop configuration UI | `true` |

## How to Use

### Basic Farming Setup

1. Hold any hoe in your main hand.
2. Shift-right-click a block to set **Point A**.
3. Shift-right-click another block to set **Point B**. The area will be scanned and you'll see a summary.
4. Run `/herecroppy start` to begin auto-farming.
5. The plugin will move your character through the area, harvesting ripe crops and replanting them.
6. If your inventory fills up, farming pauses. Use `/herecroppy restart` to resume from the last block.
7. Run `/herecroppy stop` to stop manually.

### Advanced Usage: Setup Wizard

The setup wizard allows you to configure optional dump boxes and bonemeal collection:

1. Run `/herecroppy setup` to start the setup wizard.
2. Hold a hoe and shift-right-click the box where you want to dump **unwanted crops**.
3. Shift-right-click the box where you want to dump **crops to keep**.
4. Shift-right-click the box where you want to **collect bonemeal from**.
5. Type the amount of bonemeal to collect per farming loop (the plugin will suggest the available amount).
6. Setup is complete! Your configuration is saved automatically.

**Note:** The setup wizard is optional. You can farm without it, and all crops will go directly to your inventory.

### Advanced Usage: Crop Configuration

Fine-tune which crops are seeded, collected, and receive bonemeal:

1. Run `/herecroppy config` to open the crop configuration UI.
2. Click on a crop category (Farmland Crops or Special Crops).
3. Click on a specific crop to open its settings.
4. Click on the toggles to enable/disable:
   - **Seeding** — Whether to plant this crop
   - **Collecting** — Whether to harvest this crop
   - **Bonemeal** — Whether to apply bonemeal to this crop
5. Click "Back" to return to the crop list or "Close" to exit.

**Default Settings:**
- All crops: Seeding and Collecting enabled
- Bonemeal: Only enabled for Pumpkins and Watermelons by default

## Configuration Files

HereCroppy stores player configurations in JSON files:

- **Setup configurations:** `plugins/HereCroppy/setup-configs/{playerId}.json`
- **Crop configurations:** `plugins/HereCroppy/crop-configs/{playerId}.json`

These files are created automatically and can be edited manually if needed.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
