# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.1.6]

### Added
- `KettleDisplayManager`: spawns a `PAPER` `ItemDisplay` (CMD 100 → `item/kettle` model)
  above each active brewing cauldron; despawns when state is removed; re-spawns on
  chunk load and if externally killed (detected each cauldron tick)
- `pack/assets/minecraft/models/item/kettle.json` — full-cube block model using
  `kettle_*` textures; referenced via `item/paper.json` CMD 100 override
- `pack/assets/minecraft/models/item/paper.json` — CMD override map for PAPER item
- `pack/assets/minecraft/textures/item/kettle_{side,top,bottom,inner}.png`
  (placeholder PNGs; replace with final art from product)

### Changed
- Cauldron display no longer overrides vanilla cauldron block textures.
  Only cauldrons with an active `CauldronState` show the kettle overlay.
- `pack/assets/minecraft/textures/block/cauldron_*.png` removed from pack

### Convention
- `PAPER` CMD 100 — kettle display overlay
- `PAPER` CMD 101+ — reserved for future custom items (hops, barley, etc.)
- `POTION` CMD 1–11 — beer bottle variants (existing)

## [0.1.5]

### Added
- Auto-distributed resource pack via `PlayerJoinEvent` (Adventure `ResourcePackRequest` API);
  configurable via `resource-pack.*` keys in `config.yml`
- `pack/` folder with 11 beer-bottle item models, copper cauldron block textures,
  `pack.mcmeta` (format 34, 1.21.x) and placeholder PNG textures pending final art
- `ResourcePackHandler` listener class (no deprecated `player.setResourcePack` calls)
- `config.yml` keys: `resource-pack.enabled`, `url`, `hash`, `required`, `prompt-message`
- README: "Resource pack rebuild" section with Linux/macOS and PowerShell instructions

## [1.0.0] – Initial Release

### Added
- Vanilla-cauldron brewing system with heat-source detection
  (campfire, soul campfire, fire, soul fire, lava, magma block)
- 11 built-in recipes: Lager, Dark Unfiltered, IPA, Honey Mead,
  Mushroom Stout, Pumpkin Ale, Berry Cider, Kelp Grog,
  Golden Lager, Netherwart Grog, Glow Brew
- Custom POTION items with PotionMeta color + CustomModelData
- Visual feedback: bubble particles while boiling,
  HAPPY_VILLAGER + level-up sound on ready,
  smoke particles + fire-extinguish sound on overcooking
- Audio cue: brewing stand sound every 5 seconds while active
- Persistent cauldron state via chunk PDC (survives restart)
- Per-chunk hot-cache for zero-overhead tick iteration
- Reloadable config (`/beercraft reload`):
  tick period, boil multipliers, failed-brew action, heat sources, locale
- Admin commands with tab completion:
  `reload`, `list`, `give`, `info` (all require `beercraft.admin`)
- Bilingual messages: English (`messages_en.yml`) and Russian (`messages_ru.yml`)
- Configurable failed-brew action: `drop_swill` or `nothing`
- bStats metrics (opt-out via `bstats: false` in config)
- GitHub Actions CI: build on push/PR, artifact upload, attach to release

### Fixed
- Ingredient PDC keys normalised to lowercase (was causing recipe-match failure)
- Off-hand interaction no longer double-fires
- Glass bottle returned to player when cauldron has no valid recipe
- Scheduler properly restarted on `/beercraft reload`
- Tick loop skips empty cauldrons (no unnecessary CPU usage)
- Potion effects applied deferred (1 tick after consumption)
- `/beercraft give` is now case-insensitive for recipe names
