# BeerCraft

Brewery-lite plugin for Paper 1.21.11+. Brew custom beers in vanilla cauldrons.

[README на русском](README_RU.md)

## Features

- Brew 11 recipes in vanilla cauldrons
- Heat-source detection (campfire, soul campfire, fire, soul fire, lava, magma block)
- Custom potion items with CustomModelData (resource pack included)
- Visual & audio feedback during brewing (particles, sounds, ready/overcooked signals)
- Persistent state via chunk PDC (survives restart)
- Auto-distributed resource pack on player join
- Bilingual messages (en/ru)
- Reloadable config and recipes via `/beercraft reload`

## Installation

1. Download `beercraft-X.Y.Z.jar` from Releases.
2. Drop into `plugins/` folder.
3. Restart server.
4. Edit `plugins/BeerCraft/config.yml` and `recipes.yml` as needed.
5. `/beercraft reload` to apply changes.

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/beercraft reload` | `beercraft.admin` | Reload config and recipes |
| `/beercraft list` | `beercraft.admin` | List all loaded recipes |
| `/beercraft give <recipe> [player]` | `beercraft.admin` | Give a brewed bottle (admin/testing) |
| `/beercraft info` | `beercraft.admin` | Show plugin info |

Permission `beercraft.admin` defaults to op.

## Brewing

1. Place a cauldron, fill with a water bucket.
2. Place a heat source below (lit campfire, fire, lava source block, etc.).
3. Right-click the cauldron with each ingredient (one item per click).
4. Wait. Watch for green particles + level-up sound Ч that's the moment to bottle.
5. Right-click with an empty glass bottle to extract one bottle of the brew.
6. Drink the bottle to gain its effects.

If the ingredients don't match any recipe, or if you brew too long/short, you get swill.
Swill has no effects.

## Recipes

| Name | Ingredients | Boil Time | Effects |
|------|-------------|-----------|--------|
| Lager | 3 Wheat, 1 Sugar | 30s | Speed I (60s) |
| Dark Unfiltered | 4 Wheat, 2 Cocoa Beans | 45s | Strength I (45s), Slowness I (30s) |
| IPA | 3 Wheat, 2 Sweet Berries, 1 Sugar | 60s | Haste II (90s) |
| Honey Mead | 2 Wheat, 1 Honey Bottle, 1 Sugar | 35s | Regeneration I (20s) |
| Mushroom Stout | 3 Wheat, 2 Brown Mushroom, 1 Red Mushroom | 50s | Night Vision (120s) |
| Pumpkin Ale | 2 Wheat, 1 Pumpkin, 1 Sugar | 40s | Absorption II (60s) |
| Berry Cider | 4 Sweet Berries, 2 Sugar | 25s | Jump Boost II (60s) |
| Kelp Grog | 2 Wheat, 3 Dried Kelp, 1 Sugar | 30s | Water Breathing (90s) |
| Golden Lager | 3 Wheat, 1 Golden Carrot, 1 Sugar | 50s | Fire Resistance (90s) |
| Netherwart Grog | 3 Nether Wart, 2 Wheat | 60s | Strength II (60s) |
| Glow Brew | 3 Wheat, 3 Glow Berries | 45s | Glowing (90s) |

You can add or modify recipes freely in `recipes.yml`.

## Configuration

```yaml
cauldron-tick-period: 20          # Ticks between boil increments (20 = 1s, requires restart to change)
boil-time-min-multiplier: 0.8     # Minimum boil-time tolerance
boil-time-max-multiplier: 1.5     # Maximum boil-time tolerance
failed-brew-action: drop_swill    # drop_swill | nothing
state-flush-period-seconds: 60    # How often to persist state to chunk PDC (requires restart to change)
heat-sources: [CAMPFIRE, ...]     # List of valid heat source materials
locale: en                        # en | ru
bstats: false

resource-pack:
  enabled: true
  url: "https://github.com/Pushkin-desu/BeerCraft/releases/download/v1.0.0/beercraft-pack.zip"
  hash: ""        # SHA-1 of the pack zip (40 hex chars). Leave empty to skip verification.
  required: false # if true, kick players who refuse the pack
  prompt-message: "BeerCraft requires its resource pack for custom brewing visuals."
```

> **Note:** `cauldron-tick-period` and `state-flush-period-seconds` require a server restart. All other options apply immediately via `/beercraft reload`.

## Resource pack rebuild

When updating textures or models in `pack/`, do:

**Linux / macOS:**
```bash
cd pack
zip -r ../beercraft-pack.zip *
cd ..
sha1sum beercraft-pack.zip   # put this hash into config.yml resource-pack.hash
```

**Windows (PowerShell):**
```powershell
Compress-Archive -Path pack/* -DestinationPath beercraft-pack.zip -Force
Get-FileHash -Algorithm SHA1 beercraft-pack.zip
```

Upload `beercraft-pack.zip` to the GitHub release as an asset, then update `resource-pack.url` and `resource-pack.hash` in `config.yml`.

## Building

Requires Java 21 and Maven 3.x.

```bash
mvn clean package
```

Output: `target/beercraft-X.Y.Z.jar`

## License

[MIT](LICENSE)