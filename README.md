# Crystal Spikes

A NeoForge 1.21.1 mod that adds a highly configurable, datapack-driven crystal spike
worldgen feature with geode-style block providers and a custom placement modifier for finding terrain undersides.

## Feature: `crystalspikes:crystal_spike`

Configured feature JSON (`data/<namespace>/worldgen/configured_feature/<name>.json`):

| Field | Type | Default | Description |
|---|---|---|---|
| `core` | BlockStateProvider | *required* | Spike interior blocks |
| `outer_layer` | BlockStateProvider | *required* | Spike surface blocks (any spike block exposed to air/water/lava) |
| `surface_decorator` | BlockStateProvider | *(none)* | Blocks attached to the spike surface (e.g. clusters, buds). `facing`/`waterlogged` are set automatically if the block has those properties |
| `base` | BlockStateProvider | calcite | Blocks that replace the anchor area where the spike erupts |
| `base_radius` | IntProvider (1–32) | *required* | Spike base radius; spike height scales with it |
| `crystal_direction` | `"floor"` / `"ceiling"` | *required* | `floor` = standing spike, `ceiling` = hanging spike |
| `anchor_tag` | Block tag | `minecraft:base_stone_overworld` | Blocks the spike can anchor to (and that `base` may replace) |

All BlockStateProvider fields accept any vanilla provider, e.g. `simple_state_provider`
or `weighted_state_provider` for mixed blocks within one spike.

### Example

```json
{
  "type": "crystalspikes:crystal_spike",
  "config": {
    "core": {
      "type": "minecraft:simple_state_provider",
      "state": { "Name": "minecraft:calcite" }
    },
    "outer_layer": {
      "type": "minecraft:weighted_state_provider",
      "entries": [
        { "weight": 4, "data": { "Name": "minecraft:amethyst_block" } },
        { "weight": 1, "data": { "Name": "minecraft:budding_amethyst" } }
      ]
    },
    "surface_decorator": {
      "type": "minecraft:weighted_state_provider",
      "entries": [
        { "weight": 3, "data": { "Name": "minecraft:amethyst_cluster" } },
        { "weight": 1, "data": { "Name": "minecraft:large_amethyst_bud" } }
      ]
    },
    "base": {
      "type": "minecraft:simple_state_provider",
      "state": { "Name": "minecraft:basalt" }
    },
    "base_radius": { "type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 4 },
    "crystal_direction": "floor"
  }
}
```

A complete working datapack (configured feature, placed feature, biome modifier, anchor
tag) is in [`example/`](example/).

## Placement modifier: `crystalspikes:under_island`

Finds anchor blocks on the **underside** of terrain (blocks in `anchor_tag` with
air/water/lava below them) — made for hanging spikes under floating islands, without
vanilla's 32-step `environment_scan` limit.

| Field | Type | Default | Description |
|---|---|---|---|
| `anchor_tag` | Block tag | `minecraft:base_stone_overworld` | Blocks that count as anchors |
| `min_y` / `max_y` | int | dimension bounds | Y range to scan |
| `selection` | `"random"` / `"lowest"` / `"highest"` | `"random"` | Which candidate in the column to use |

### Example

```json
{
  "feature": "example:my_spike",
  "placement": [
    { "type": "minecraft:rarity_filter", "chance": 8 },
    { "type": "minecraft:count", "count": { "type": "minecraft:uniform", "min_inclusive": 4, "max_inclusive": 8 } },
    { "type": "minecraft:in_square" },
    { "type": "crystalspikes:under_island", "anchor_tag": "example:end_stones", "min_y": 0, "max_y": 128 },
    { "type": "minecraft:biome" }
  ]
}
```

For surface placement, vanilla's `heightmap` + `environment_scan` (down) works fine —
see the example datapack.
