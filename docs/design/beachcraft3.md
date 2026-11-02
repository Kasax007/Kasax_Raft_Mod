# Beach Craft 3.0 – Design and Build Specification

This is the single source of truth for Beach Craft 3.0 ("Tides of the Deep"), the big update of the Raft mod
(mod id `raft`, CurseForge "Beach Craft", Minecraft 26.3, Fabric). Everybody who builds a part of it follows this file.
**IDs, block states and loot table names in this file are a contract**: other modules (especially the structure
builders) use them without seeing your code. If you must deviate, write the change into section 14 ("Deviations")
of this file with a one-line reason.

Goal: turn the raft dimension ("The Endless Sea") into a living ocean world of AAA quality: archipelago terrain with
many biomes, reefs and trenches, dozens of structures above and below the water, a story told through logs, journals
and places, many decorative blocks, creatures everywhere, a raft you can really sail, diving gear to explore sunken
cities, and a final boss that can only be reached after the story is done. It is inspired by survival-on-a-raft
games, but everything (places, names, lore, creatures) is our own.

---------------------------------------------------------------------------------------------------------------------

## 1. How the code base works (read first)

### 1.1 Facts

- Minecraft **26.3** with official Mojang names (unobfuscated), Fabric Loader 0.19.5+, **Fabric API 0.165.0+26.3**,
  Java 25. Energy: Team Reborn Energy 5 (bundled), see `block/cable`, `MakeshiftBatteryBlockEntity`.
- Existing code (`net.kasax.raft.*` outside `content`) is the 2.0 mod: driftwood/palm wood, raft foundation, portal,
  shark "Bruce", titanium, machines, furnaces, item catcher, water purifier, grill, crop plot, scarecrow, debris.
  Read `docs/curseforge/description.html` and `src/main/resources/assets/raft/lang/en_us.json` for the full list.
- Sea level of the raft dimension is **100**: water fills y ≤ 99. The arrival platform is built at y ≈ 209.
- Reference material (read-only):
  - Minecraft 26.3 sources: `$SP/ref/mcsrc/net/minecraft/...` (grep them for exact names and signatures!).
  - Vanilla assets/data JSON + structure NBT + textures: `$SP/vanilla/` (`assets/minecraft/...`, `data/minecraft/...`).
  - Fabric API classes: `$SP/ref/fapi.sh <ClassName>` prints the API (javap).
  - `SP=/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad`
- 26.3 specifics that bite: blocks/items need `properties.setId(key)` before construction (use `BcReg`); item models
  are `assets/raft/items/<id>.json` definitions + models; armor uses `assets/raft/equipment/<material>.json`;
  spawn eggs use `new Item.Properties().spawnEgg(TYPE)` with `SpawnEggItem::new`; furnace fuel is the item's
  `cookingFuel(...)` property; no `BlockRenderLayerMap` (render layers come from the textures: translucent pixels ->
  translucent layer); `Identifier` (not ResourceLocation); entity renderers use render states
  (`extractRenderState`). Look at the existing shark (`entity/SharkEntity`, `client/render/Shark*`) for a working
  26.3 entity + model + renderer.

### 1.2 Modules and ownership

Every content area is a **module**. A module owns exactly these places and must not edit anything else:

| Module | Java package | Resources | Python generators |
| --- | --- | --- | --- |
| `world` | `net.kasax.raft.content.world` | `src/main/modules/world/` | `tools/content/world/` |
| `building` | `...content.building` | `src/main/modules/building/` | `tools/content/building/` |
| `decor` | `...content.decor` | `src/main/modules/decor/` | `tools/content/decor/` |
| `shore` | `...content.shore` | `src/main/modules/shore/` | `tools/content/shore/` |
| `marine` | `...content.marine` | `src/main/modules/marine/` | `tools/content/marine/` |
| `voyage` | `...content.voyage` | `src/main/modules/voyage/` | `tools/content/voyage/` |
| `diving` | `...content.diving` | `src/main/modules/diving/` | `tools/content/diving/` |
| `story` | `...content.story` | `src/main/modules/story/` | `tools/content/story/` |
| `boss` | `...content.boss` | `src/main/modules/boss/` | `tools/content/boss/` |
| `structures_surface` | (data only) | `src/main/modules/structures_surface/` | `tools/content/structures_surface/` |
| `structures_deep` | (data only) | `src/main/modules/structures_deep/` | `tools/content/structures_deep/` |
| `structures_city` | (data only) | `src/main/modules/structures_city/` | `tools/content/structures_city/` |
| `structures_side` | (data only) | `src/main/modules/structures_side/` | `tools/content/structures_side/` |

Extra files owned by single modules (listed again in their sections):
- `world`: `src/main/resources/data/raft/dimension/raftdim.json`, `.../worldgen/noise_settings/raftdim.json`,
  `.../worldgen/material_rule/raftdim.json`, `world/biome/ModBiomes.java`, `RaftDataGenerator.java`,
  `datagen/ModWorldGenerator.java`, `datagen/ModBiomeTagProvider.java`, `src/main/generated/data/raft/worldgen/biome/*`.
- `marine`: `entity/SharkEntity.java`, `entity/ModEntities.java`, `client/render/Shark*.java`, `textures/entity/shark.png`.
- `story`: `world/OceanDebris.java`, `world/portal/RaftPortalForcer.java`.
- Polishing existing textures (`src/main/resources/assets/raft/textures/**`) is a separate later task (module `polish`).

Each module has (already created, fill them in):
- `XxxModule implements ContentModule` with `init()` (common) and `initClient()` which calls `client/XxxClient.init()`.
  Put **all client-only code** (renderers, models, screens, HUD, key bindings) in `content/<module>/client/` and only
  reach it from `XxxClient.init()`. Common classes must never import `net.minecraft.client.*`.
- A mixin config `src/main/resources/raft-<module>.mixins.json` (package `net.kasax.raft.content.<module>.mixin`,
  lists `mixins` and `client`). It is already registered in `fabric.mod.json`. Prefer events/APIs over mixins.
  No access wideners (use `@Accessor`/`@Invoker` mixins if needed).
- `BcReg` registration helpers (`block`, `blockWithItem`, `blockItem`, `item`, `entity`, `blockEntity`, `sound`) and
  `BcTabs` creative tabs (`BUILDING, DECOR, NATURE, VOYAGE, CREATURES, STORY`; `BcTabs.add(tab, item)` in display order,
  `BcTabs.icon(tab, () -> stack)`). Lang keys of the tabs (`itemgroup.raft.building` ...) are written by `building`
  (BUILDING), `decor` (DECOR), `world` (NATURE), `voyage` (VOYAGE; `diving` adds its items to VOYAGE too),
  `marine` (CREATURES), `story` (STORY).
- Cross-module references in **Java**: avoid them. Look things up by id at runtime
  (`BuiltInRegistries.BLOCK.getValue(Raft.id("tide_seal"))`, `BuiltInRegistries.ENTITY_TYPE.getValue(Raft.id("leviathan"))`).
  Cross-module references in **JSON** (recipes, loot, structures, biome spawns) by id string are fine and expected.

### 1.3 Resources: generated by Python, merged by Gradle

- All JSON and textures of a module are produced by **re-runnable Python scripts** in `tools/content/<module>/`
  (entry point `gen.py`) using `tools/bclib` (`assets.py` for 26.3 JSON, `tex.py` for pixel art, `nbt.py` for
  structure templates, `preview.py` for structure previews). Run them with `cd tools && python3 -m content.<module>.gen`
  or `python3 tools/content/<module>/gen.py` (add `sys.path.insert(0, <tools dir>)`). They write into
  `src/main/modules/<module>/{assets,data}/...`. Do not use Fabric datagen for new content.
- Gradle (`mergeResources`) merges `src/main/resources`, `src/main/generated` and every module folder. Several modules
  may write the same **lang file** (`assets/raft/lang/en_us.json`), **`assets/raft/sounds.json`** and the same
  **tag** (e.g. `data/minecraft/tags/block/mineable/axe.json`): they are merged (same lang key twice with different
  text = build error). Any other file that exists twice is a build error, so stay inside your names.
- Standard data every block/item needs: blockstate, block model, item definition (`assets/raft/items/<id>.json`),
  textures, loot table (`data/raft/loot_table/blocks/<id>.json`), mining tags (`minecraft:mineable/*`,
  `minecraft:needs_*_tool`), recipe(s) (`data/raft/recipe/<name>.json`), lang (`block.raft.<id>` / `item.raft.<id>`),
  creative tab entry. Entities: `entity.raft.<id>`, spawn egg `item.raft.<id>_spawn_egg`, loot
  `data/raft/loot_table/entities/<id>.json`. Biomes: `biome.raft.<id>`. Tooltips: `tooltip.raft.<id>[.n]`.
- Only English (`en_us`). Texts: short, clear, friendly; no developer talk.

### 1.4 Compiling and testing (shared machine: 4 CPUs, max 2 JVMs!)

- **Compile check:** `$SP/ref/jc.sh` (whole tree) or `$SP/ref/jc.sh path/A.java path/B.java` (these files plus what
  they reference). It holds a lock, so it may wait. Other modules are being written at the same time: errors in
  files you do not own are not yours – ignore them, but keep **your** files free of errors. Always write complete,
  syntactically valid files (a syntax error blocks everyone's compile).
- **In-game test:** `tools/bctest/run.sh <script> <outdir>` (see `tools/bctest/README.md`): builds the mod, starts a
  client, runs your script (teleport, `/place template`, `/summon`, `/give`, screenshots), and prints errors from
  `latest.log`. It needs the whole tree to compile; if someone else's half-written file breaks the build, wait a few
  minutes and retry. Look at your screenshots with the Read tool. One run takes 1–3 minutes; do not spam it.
- **Never** run `./gradlew` directly, never start other JVMs, never `pkill -f`. Keep the disk tidy (outputs into
  `$SP/work/<module>/`).
- Do not commit; the lead integrates and commits.

---------------------------------------------------------------------------------------------------------------------

## 2. Art direction ("AAA" in Minecraft terms)

- **16×16** block and item textures in the vanilla style. Quality comes from good pixel art, not resolution: one
  hue-shifted ramp of 4–6 shades per material (`tex.ramp`), light from the top-left, clean silhouettes, readable at a
  glance, no pure black, no random single-pixel noise, no gradients across the whole texture. Blocks tile seamlessly.
- Items: 16×16 sprites with a 1-px darker outline (`tex.outline`), centred, using ~70 % of the canvas.
- Entities: textures in vanilla scale (1 texel = 1/16 block), 64×64 (small/medium), 128×128 (large), 256×256 boss.
  Models built from boxes in Java `LayerDefinition`s, animated in `setupAnim` (swim cycles, fins, claws, wings, jaws).
- Animated textures (`tex.save_animated`) for glowing things (jelly lamps, glow kelp, neon, aquarium, buoys).
- **Always look at your work**: `tex.sheet([...])` next to vanilla neighbours, `tex.tile_preview(...)`, and in-game
  screenshots via bctest. Redo anything that looks flat, noisy or out of style.
- Palette themes: Tidewarden relics = teal/turquoise stone with glowing cyan runes and gold inlays; Meridian
  Expedition = painted steel, brass, white/red stripes, porthole glass, 1950s research look; pirates/corsairs =
  dark teak, red cloth, gold coins, rope; islands = warm sand, palm greens, hibiscus reds; deep sea = navy, black,
  bioluminescent cyan/violet; volcano = basalt grey, ash, ember orange.

---------------------------------------------------------------------------------------------------------------------

## 3. Story: "Tides of the Deep"

### 3.1 Lore

Long ago the **Tidewardens**, a sea people, kept the ocean calm with the **Heart of the Tide**, a crystal of
Ancient Energy (the `ancient_crystal`/`ancient_broken_ring` items of the 2.0 mod are their relics). Deep in the
abyss they bound **the Leviathan**, a colossal sea serpent, behind the **Tide Seal**, locked with four **Tide Sigils**
(Coral, Pearl, Storm, Ember).

Centuries later the **Meridian Expedition** found the relics, built driftwood portals (that is how the player
arrives) and founded **Port Solace**, a city powered by crystal energy. Their chief scientist **Dr. Imara Voss**
studied the sigils at **Helix Research Station** and drilled into the abyss. The seal cracked, the Leviathan woke,
the sea rose and swallowed Port Solace. Survivors scattered across islands and rafts.

The player arrives as a castaway. Logs, letters and terminals of the survivors lead them from place to place:
gather the four sigils, forge the **Tidewarden Key**, open the **Abyssal Sanctum** and defeat the Leviathan to calm
the sea.

Characters (authors of logs): Captain **Rhea Marlow** (expedition captain, leaves notes for whoever comes through
the portal), **Tobias Fenn** (lighthouse keeper), **Nana Kalea** (elder of the stilt village Saltmere),
**Dr. Imara Voss** (scientist, guilt-ridden), **Mayor Aldous Crane** (Port Solace), **"Saltjaw" Brannigan** (pirate
captain, side story about his treasure). Tone: adventurous, a little melancholic, hopeful; short entries (a book page
holds ~200 characters; 2–6 pages per log).

### 3.2 Chapters

| # | Chapter | Place (structure) | What the player finds | Leads to |
| --- | --- | --- | --- | --- |
| 1 | The Castaway | arrival platform (portal) | Castaway chest: Marlow's first note, **Expedition Journal**, map to the lighthouse, basic kit | Old Lighthouse |
| 2 | Keeper of the Light | `old_lighthouse` | Fenn's logbook, map to Saltmere, lantern oil/supplies | Saltmere |
| 3 | Saltmere | `saltmere_village` | Nana Kalea's letters, **Diving Helmet**, **Coral Sigil** (village shrine), map to Helix | Helix Station |
| 4 | Deep Science | `helix_station` | Voss's research logs (terminals), **Pearl Sigil** (lab vault), oxygen tank, map to Port Solace | Port Solace |
| 5 | The Drowned City | `port_solace` | Mayor Crane's last broadcast, **Storm Sigil** (City Hall vault, top of the central tower), map to the forge | Ashen Forge |
| 6 | Through Fire | `ashen_forge` | Tidewarden tablets, **Ember Sigil**, map to the Coral Temple | Coral Temple |
| 7 | The Tidewarden Key | `coral_temple` | Tidewarden altar: insert the 4 sigils -> **Tidewarden Key**; map to the sanctum | Abyssal Sanctum |
| 8 | The Leviathan | `abyssal_sanctum` | Use the key on the **Tide Seal**, enter the dome, summon and defeat **the Leviathan** | Epilogue |
| – | Side: Saltjaw's Gold | `pirate_cove`, `wreck_galleon` | Brannigan's logs, pirate treasure, cursed corsairs | – |

**Gating (hard requirement):** the boss can only be fought after the story. The sanctum shell and seal are
unbreakable and explosion-proof; the seal opens only for the Tidewarden Key; the key can only be forged at a
Tidewarden altar from all four sigils; each sigil exists only in its story structure's vault loot. The Leviathan
spawns only from the `leviathan_altar` inside the sanctum (no natural spawn, no spawn egg in survival).

**No dead ends:** every story chest contains an exploration map to the next place; additionally the Expedition
Journal shows the current objective and, on sneak-use, gives a fresh map to the next place (cooldown 5 min).

---------------------------------------------------------------------------------------------------------------------

## 4. World (module `world`)

### 4.1 Terrain

Replace the overworld-copy terrain of `raft:raftdim` (big stone mountains) by an **archipelago**: mostly open sea,
islands of 30–200 blocks, shallows around islands, deep basins, flat drowned shelves and dark trenches.
Implement it with custom density functions (`data/raft/worldgen/density_function/raftdim/*.json`, own noises in
`data/raft/worldgen/noise/*.json`) and a new `noise_settings/raftdim.json` (keep sea level 100, min_y -64,
height 384). Target heights (top of the sea floor / land):

| Zone | Floor / land height | Share of area |
| --- | --- | --- |
| Abyssal trench | y 0–15, steep walls | ~10 % |
| Deep open sea | y 35–60 | ~35 % |
| Drowned shelf | flat y 44–50 (city ground) | ~10 % |
| Kelp / mid sea | y 55–75 | ~15 % |
| Shallows (reef/lagoon) | y 82–96 | ~12 % around islands |
| Islands | beach y 100–103, inland 104–135 | ~15 % |
| Volcanic peaks | up to y 170–190, crater with lava | a few % (part of volcanic isles) |

Keep vanilla-style caves light (no huge cave systems under the sea; a few underwater caves are nice). No floating
stone overhangs above water except arches/sea stacks by design. Titanium ore keeps generating (existing placed feature).

### 4.2 Biomes

`dimension/raftdim.json` uses a `minecraft:multi_noise` biome source whose parameters follow the terrain (island
biomes only on land, shallows around islands, trench where the floor is deepest). Biome patches are large (ocean
biomes 400–1500 blocks). Each biome is a JSON file in `data/raft/worldgen/biome/` (remove the datagen biome:
delete `generated/.../biome/raft_ocean_biome.json`, drop the BIOME line from `RaftDataGenerator`/`ModWorldGenerator`).
Keep the id `raft:raft_ocean_biome` (old worlds). All biomes: no rain-darkness, sky `#6cbaea`-ish, custom water
colour, water fog colour, fog colour, music (vanilla tracks), ambient particles where noted.

| Biome id | Name | Where | Water / fog | Features (natural blocks see 4.3) | Spawns (weight) |
| --- | --- | --- | --- | --- | --- |
| `raft_ocean_biome` | Endless Sea | deep open sea | deep blue | kelp, seagrass, rare tube coral, bubble vents | `shark` 3, `whale` 1, `jellyfish` 4, `hammerhead_shark` 1, vanilla cod/squid/dolphin |
| `kelp_forest` | Kelp Forest | mid sea | green-blue | very tall kelp, glow kelp patches, seagrass, barnacles on rocks | `sea_otter` 4 (surface), `jellyfish` 2, `moray_eel` 1, cod, salmon |
| `coral_shallows` | Coral Shallows | shallows (warm) | turquoise, clear | vanilla coral reefs + anemones, giant clams, starfish, sea urchins, sea glass, sea pickles | `manta_ray` 3, `moray_eel` 2, turtle, tropical fish, pufferfish |
| `glowing_lagoon` | Glowing Lagoon | shallows (rare) | teal, slightly dark | glow kelp, glowing anemones, tube worms, bubble vents; ambient glow particles | `jellyfish` 6 (glow variant), `manta_ray` 1, glow squid |
| `drowned_shelf` | Drowned Shelf | flat mid-depth | murky grey-green | concrete rubble (cracked_concrete, rusty bars), seagrass, barnacles; no coral | `moray_eel` 2, `drowned_corsair` 1, drowned, cod |
| `abyssal_trench` | Abyssal Trench | deepest | near-black navy, short fog | tube worms, bubble vents, basalt, glowing anemones (sparse), deep sand/gravel | `anglerfish` 4, `jellyfish` 2, glow squid |
| `wreck_graveyard` | Wreck Graveyard | deep sea (rare) | grey-blue, foggy surface | debris, kelp, barnacles; many wrecks (structures) | `drowned_corsair` 3, `shark` 3, drowned |
| `frost_floes` | Frost Floes | open sea (cold) | cold cyan | icebergs (vanilla iceberg features), packed/blue ice floes, snow on floes | `penguin` 6 (on ice), polar bear 1, salmon, `whale` 1 |
| `palm_isle` | Palm Isle | islands | – | sand beaches with shell_sand, palms (existing palm feature), beach grass, sea oats, hibiscus, bird of paradise, seashells, sea glass, pineapple bushes, driftwood logs | `crab` 8, `seagull` 6 |
| `jungle_isle` | Jungle Isle | islands (warm, wet) | – | jungle trees, bamboo, ferns, tropical ferns, hibiscus, pineapple, melons, waterfalls (springs), cocoa | parrot, ocelot, `seagull` 2, `crab` 3 |
| `mangrove_isle` | Mangrove Isle | low wet islands | swampy green water | vanilla mangrove trees/roots, mud, lily pads, tropical ferns | frog, `crab` 4, slime (rare) |
| `volcanic_isle` | Volcanic Isle | islands (rare) | warm | black_sand beaches, basalt/blackstone/pumice/volcanic_ash surface, basalt columns, magma, lava pools, crater lava lake, ash particles, no trees | `ember_crab` 5, `seagull` 1 |

Biome tags written by `world` (structures use them): `raft:is_island` (palm, jungle, mangrove, volcanic),
`raft:is_shallow` (coral_shallows, glowing_lagoon), `raft:is_deep` (raft_ocean_biome, kelp_forest, wreck_graveyard,
abyssal_trench, drowned_shelf, frost_floes), `raft:is_raft_dimension` (all 12). Also add all island biomes to
`minecraft:is_beach`-like vanilla tags only where vanilla logic needs it (e.g. turtles need `#minecraft:...`), and the
ocean ones to `minecraft:is_ocean` and `minecraft:is_deep_ocean` (deep ones) so vanilla fish/drowned behave.

### 4.3 Natural blocks and items (module `world`)

| Id | Kind / behaviour | Block state properties | Drops / notes |
| --- | --- | --- | --- |
| `shell_sand` | falling block, sand with shells | – | itself; shovel |
| `black_sand` | falling block, volcanic sand | – | itself; smelts to tinted glass |
| `volcanic_ash` | falling block, soft grey ash | – | itself; shovel |
| `pumice` | light porous stone | – | itself; pickaxe |
| `beach_grass` | short plant on sand (like short grass, no tint) | – | shears: itself; else nothing |
| `sea_oats` | 2-tall plant on sand | `half=lower/upper` | shears: itself |
| `hibiscus` | flower (red) | – | itself; red dye recipe |
| `bird_of_paradise` | 2-tall flower | `half=lower/upper` | itself; orange dye |
| `tropical_fern` | fern-like plant, jungle/mangrove | – | shears |
| `pineapple_bush` | wild bush with fruit (berry-bush like) | `age=0..3` | `pineapple` when age 3 (right-click harvest), regrows |
| `pineapple_crop` | crop (on farmland and crop plot) | `age=0..7` | `pineapple` + `pineapple_crown` at age 7 |
| `potted_hibiscus`, `potted_tropical_fern`, `potted_palm_sapling` | flower pots | – | pot + plant |
| `glow_kelp`, `glow_kelp_plant` | like vanilla kelp, emits light 8 | `age=0..25` (top block) | `glow_kelp` item; place on sea floor |
| `sea_anemone_red`, `sea_anemone_purple`, `sea_anemone_glow` | underwater plant, always waterlogged | `waterlogged` | itself (shears); `glow` gives light 10 |
| `giant_clam` | underwater block, opens/closes | `facing`, `open`, `pearl`, `waterlogged` | right-click open clam with pearl -> `pearl`, regrows in ~10 min; mined with pickaxe -> itself |
| `starfish_orange`, `starfish_purple`, `starfish_red` | flat decoration on floor or wall (like a button shape) | `face=floor/wall/ceiling`, `facing`, `waterlogged` | itself |
| `sea_urchin` | spiky ball, hurts on touch (1 dmg) | `waterlogged` | `urchin_spine` ×1–2 |
| `barnacles` | multiface layer (like glow lichen) on stone/wood/metal | `north/east/south/west/up/down`, `waterlogged` | shears or silk touch |
| `tube_worms` | trench plant clusters, light 6 | `waterlogged` | shears |
| `bubble_vent` | full block (basalt-like) that makes an upward bubble column above it (like soul sand) and bubble particles | – | itself; pickaxe. Divers refill air in its column |
| `seashells` | 1–4 shells lying on sand (like sea pickles) | `shells=1..4`, `waterlogged` | `seashell` × shells |
| `sea_glass_scatter` | 1–3 glass pebbles on sand | `pebbles=1..3`, `waterlogged` | `sea_glass_shard` × pebbles |

Items: `pineapple` (food 4, sat 0.6), `pineapple_crown` (plants `pineapple_crop`), `pearl` (crafting/trade),
`seashell`, `sea_glass_shard`, `urchin_spine` (crafting: harpoon tips, fish hooks), `glow_kelp` (block item of
`glow_kelp`). Recipes: hibiscus -> red dye, bird_of_paradise -> orange dye, pineapple_crown from pineapple
(shapeless 1 -> 1 crown + nothing; crown also drops from crops).

Features: placed features for all of the above in the biomes of 4.2 (patch features, coral reef with vanilla coral
blocks/fans plus our anemones/clams/starfish/urchins, glow kelp columns, tall kelp, barnacle scatter on stone, tube
worm fields, bubble vent clusters, seashell + sea glass scatter on beaches, beach grass/sea oats/flower patches,
pineapple bushes, fallen driftwood logs on beaches (use existing `raft:driftwood_log`), boulders of stone/pumice,
basalt columns, lava pools, ash layers, icebergs). Existing palm tree feature stays (`raft:palm_placed`).

---------------------------------------------------------------------------------------------------------------------

## 5. Building blocks (module `building`)

All with recipes (stonecutter where it makes sense), loot, tags, lang, creative tab BUILDING (tech blocks may go to
DECOR). The Expedition tech blocks (control panel, server rack, satellite dish, wind turbine) belong to `building` too. "set" = block + stairs +
slab (+ wall for stone, + fence/fence gate for wood) using `assets.stairs/slab/wall/fence/fence_gate` (vanilla block
classes: `StairBlock`, `SlabBlock`, `WallBlock`, `FenceBlock`, `FenceGateBlock` with the right `WoodType`).

| Id(s) | Description | Properties | Recipe idea |
| --- | --- | --- | --- |
| `weathered_planks` + `_stairs`, `_slab`, `_fence`, `_fence_gate` | grey sun-bleached planks (docks, shacks, wrecks) | standard | 4 driftwood/palm planks + 1 bone meal -> 4 |
| `teak_planks` + `_stairs`, `_slab`, `_fence`, `_fence_gate`, `teak_door`, `teak_trapdoor` | dark rich ship wood (pirate ships, captain's cabins) | standard | dark oak planks + palm planks |
| `aqua_sea_glass`, `green_sea_glass`, `blue_sea_glass` + `*_sea_glass_pane` | frosted translucent glass | panes: north/east/south/west/waterlogged | 4 `sea_glass_shard` -> 1 |
| `coral_stone`, `polished_coral_stone`, `coral_stone_bricks`, `mossy_coral_stone_bricks` + stairs/slab/wall each for `coral_stone_bricks` | pink-beige limestone with coral fossils (reef ruins, villages) | standard | coral blocks + stone / stonecutter |
| `tidestone`, `tidestone_bricks`, `cracked_tidestone_bricks`, `mossy_tidestone_bricks`, `chiseled_tidestone`, `tidestone_pillar` (axis), `glowing_tidestone` (light 10, cyan runes, animated), + `tidestone_brick_stairs`, `tidestone_brick_slab`, `tidestone_brick_wall` | teal ancient stone of the Tidewardens (temples, sanctum, ruins) | pillar: `axis` | prismarine shard + stone; glowing: + glowstone dust |
| `cracked_concrete`, `mossy_concrete`, `rebar_concrete` | drowned-city concrete (grey; rebar sticks out) | – | concrete + gravel; rebar: + iron nuggets |
| `asphalt`, `asphalt_line` (white lane line), `asphalt_slab` | city streets | `asphalt_line`: `axis=x/z` | gravel + coal + sand |
| `cracked_glass`, `cracked_glass_pane` | broken office windows | pane props | glass + gravel |
| `riveted_hull`, `rusty_hull` | painted steel ship hull plates (`riveted_hull` dark navy with rivets, `rusty_hull` corroded) | – | iron + scrap metal |
| `corrugated_metal` + `_stairs`, `_slab` | wavy galvanized sheet (shacks, rigs) | standard | iron nuggets + scrap metal |
| `rusted_plate` | rusty steel floor plate | – | scrap metal |
| `steel_beam` | I-beam | `axis` | iron ingots |
| `catwalk` | open steel grating slab (see-through) | `type` (slab), `waterlogged` | iron bars + scrap metal |
| `steel_railing` | thin railing (pane-like) | north/east/south/west/waterlogged | iron nuggets |
| `porthole` | full block, steel frame with round glass centre (translucent) | – | glass + iron |
| `bulkhead_door` | steel door with porthole (iron-door rules: hands can open it here – like a wooden door) | door props | iron + porthole |
| `metal_hatch` | steel trapdoor (opens by hand) | trapdoor props | iron |
| `rusty_pipe` | pipe segment | `axis`, `waterlogged` | scrap metal |
| `rusty_pipe_valve` | pipe with red wheel valve | `axis`, `waterlogged` | pipe + iron |
| `shipping_container_red`, `shipping_container_blue`, `shipping_container_green` | full block, ribbed container wall (side) | `axis=x/z` | iron + dye |
| `control_panel` | Expedition console with blinking lights (animated), decor (light 4) | `facing`, `waterlogged` | scrap metal + circuit board / redstone |
| `server_rack` | humming computer rack (decor) (light 3) | `facing` | scrap metal + circuit board / redstone |
| `satellite_dish` | dish antenna (decor) | `facing` | scrap metal + circuit board / redstone |
| `wind_turbine` | **functional**: generates 20 FE/t when the 3×3 area around its blades is free and it is above y 90; animated blades (block entity renderer); pushes energy into adjacent energy storages like the solar panel | `facing` | scrap metal + circuit board / redstone |

---------------------------------------------------------------------------------------------------------------------

## 6. Decoration (module `decor`)

Creative tab DECOR. Waterloggable where it makes sense (anything that can sit underwater in a structure). Lights:
emit the given light level. Sittable blocks use one shared invisible **seat entity** `raft:seat` (decor module,
no renderer output, removed when the player dismounts or the block breaks; `SeatEntity.sit(level, pos, player, yOffset)`).

| Id(s) | Description | Properties | Light |
| --- | --- | --- | --- |
| `string_lights_warm`, `string_lights_colorful` | fairy-light garland hanging along a wall (like a ladder-thin decoration), connects visually | `facing`, `waterlogged` | 12 |
| `bunting_red_white`, `bunting_rainbow` | pennant flag garland on a wall | `facing`, `waterlogged` | – |
| `paper_lantern_<color>` ×9 (white, red, orange, yellow, lime, cyan, blue, purple, pink) | round paper lantern, standing or hanging | `hanging`, `waterlogged` | 14 |
| `ship_lantern` | brass and glass lantern | `hanging`, `waterlogged` | 15 |
| `tiki_torch` | 2-tall bamboo torch with flame particles | `half=lower/upper`, `lit` | 14 (upper, lit) |
| `jelly_lamp` | jar of glowing jelly (animated) | `hanging`, `waterlogged` | 15 |
| `brazier` | iron fire bowl (hurts like campfire, can be lit/extinguished) | `lit`, `waterlogged` | 15 when lit |
| `buoy_red`, `buoy_green` | floating channel buoy (sits in water), blinking top (animated) | `waterlogged` | 10 |
| `lighthouse_lens` | big Fresnel lens block (translucent, glowing) | – | 15 |
| `neon_tube_cyan`, `neon_tube_pink` | thin glowing tube | `axis`, `waterlogged` | 12 |
| `driftwood_lamp` | standing lamp of driftwood and canvas | `waterlogged` | 13 |
| `seashell_lamp` | lamp made of shells | `waterlogged` | 12 |
| `ship_cannon` | deck cannon; right-click with a `cannonball` fires it (projectile flies ~40 blocks, 8 damage, no block damage) | `facing`, `waterlogged` | – |
| `ship_anchor` | big anchor wall/floor decoration | `facing`, `waterlogged` | – |
| `treasure_chest` | iron-banded chest, 27 slots, lid opens (open state) | `facing`, `open`, `waterlogged` | – |
| `gold_coin_pile` | gold coins, stackable layers like snow | `layers=1..8`, `waterlogged` | – |
| `jolly_roger` | pirate flag on a wall | `facing`, `waterlogged` | – |
| `wanted_poster` | "WANTED: Saltjaw" poster on a wall | `facing` | – |
| `rum_bottles` | 1–3 bottles | `bottles=1..3`, `facing`, `waterlogged` | – |
| `rope_railing` | fence-like rope railing | `north/east/south/west`, `waterlogged` | – |
| `cargo_net` | climbable rope net on a wall (like a ladder) | `facing`, `waterlogged` | – |
| `powder_keg` | barrel of gunpowder (decor; explodes weakly if set on fire? no – decor only) | `waterlogged` | – |
| `wooden_crate` | storage crate, 27 slots | `facing`, `open` | – |
| `ship_bell` | brass bell on a wall bracket; rings on use | `facing`, `waterlogged` | – |
| `chart_table` | table with a sea chart on top | `facing` | – |
| `spyglass_stand` | spyglass on a tripod | `facing`, `waterlogged` | – |
| `figurehead` | carved mermaid figurehead (wall decor) | `facing`, `waterlogged` | – |
| `life_ring` | red-white life ring on a wall | `facing`, `waterlogged` | – |
| `hammock` | 2 blocks long; right-click: rest (sit/lie) and set your respawn point | `facing`, `part=head/foot` | – |
| `deck_chair` | striped deck chair, sittable | `facing` | – |
| `beach_umbrella_red`, `beach_umbrella_blue`, `beach_umbrella_yellow` | 2-tall parasol | `half=lower/upper` | – |
| `cushion_white`, `cushion_red`, `cushion_blue`, `cushion_yellow`, `cushion_green` | floor cushion, sittable | – | – |
| `woven_rug` | thin rug (carpet-like) | – | – |
| `driftwood_table`, `palm_table` | tables | `waterlogged` | – |
| `driftwood_chair`, `palm_chair` | chairs, sittable | `facing`, `waterlogged` | – |
| `driftwood_shelf` | wall shelf with jars | `facing` | – |
| `wind_chime` | hanging shell chime, chimes now and then | `waterlogged` | – |
| `aquarium` | glass tank with fish (animated), translucent | – | 6 |
| `hanging_planter` | hanging basket with flowers | – | – |

Items: `cannonball` (16 per stack; recipe iron + gunpowder), `gold_coin` (9 = 1 pile layer? no: `gold_coin_pile` is
crafted from 9 `gold_coin`; coins come from loot), seat entity `raft:seat`.

---------------------------------------------------------------------------------------------------------------------

## 7. Creatures

All creatures: entity type, attributes, AI goals, spawn placement + natural spawn via biome JSON (4.2), Java model
(`LayerDefinition`) with animation, renderer, texture, spawn egg (creative tab CREATURES), loot table, sounds (vanilla
sound events are fine, custom ones welcome), lang. Water creatures use `MobCategory.WATER_CREATURE` or
`WATER_AMBIENT`; land animals `CREATURE`; hostiles `MONSTER` (beware: the raft dimension is always bright – override
spawn rules like the shark does). They must not despawn too eagerly (whales/mantas persist while near players).

### 7.1 Module `shore` (land/surface creatures and the corsairs)

| Id | Size (w×h) | HP | Behaviour | Drops |
| --- | --- | --- | --- | --- |
| `crab` | 0.6×0.4 | 8 | walks sideways on beaches, hides in sand when a player sprints close, pinches (2 dmg) when hit (neutral), 3 colour variants (red, blue, sand) | `crab_claw` 1–2 (food raw 2; `cooked_crab_claw` 6), `seashell` rare |
| `ember_crab` | 0.8×0.5 | 14 | volcanic, hostile when close, fire immune, sets target on fire (2 s) | `crab_claw`, `ember_shell` (crafting ingredient, e.g. for a fire-resistant lantern recipe of `decor`) |
| `seagull` | 0.5×0.6 | 6 | flies in flocks over islands and rafts, lands on blocks, **pecks crops on crop plots/farmland** (resets growth one stage) unless a `raft:scarecrow` is within 16 blocks, snatches food items lying on the ground | feather 0–2 |
| `penguin` | 0.5×0.9 | 10 | waddles on ice floes, slides on belly, dives for fish, babies, breedable with cod | feather |
| `sea_otter` | 0.7×0.4 | 10 | floats on its back at the surface in kelp forests, cracks shells with a stone, dives; tameable-like: feeding it a `seashell` makes it follow you and occasionally bring `pearl`/`seashell` (no full taming needed) | – |
| `drowned_corsair` | humanoid | 24 | cursed undead pirate: swims like a drowned, does not burn in sunlight, fights with a `cutlass` (sword); spawns in wreck graveyard, pirate cove, galleon wrecks | `gold_coin` 0–3, bones, rare `cutlass` |

Items: `crab_claw`, `cooked_crab_claw` (smelt/smoke/campfire), `ember_shell`, `cutlass` (sword: 6 attack damage,
faster than an iron sword, iron + teak planks). The corsair can reuse the vanilla zombie/drowned humanoid model with
its own texture (and a tricorn hat layer).

### 7.2 Module `marine` (sea)

| Id | Size | HP | Behaviour | Drops |
| --- | --- | --- | --- | --- |
| `shark` (existing "Bruce") | – | – | **improve**: better model/texture and swim animation; **bites raft foundations**: when a player stands on a raft of `raft:raft_foundation` near water, a shark may circle and bite edge foundations (break one block after a few bites); `raft:reinforced_foundation` resists. Game rule `raftSharkBites` (default true) | existing |
| `hammerhead_shark` | 2.0×1.0 | 40 | stronger shark of deep water; same rules as shark (hunts swimmers only) | `shark_tooth` 1–3, `shark_meat` |
| `whale` | 6.0×3.0 | 120 | huge gentle whale, slow cruising at depth 3–15, surfaces and spouts (particles + sound), sings (note-like sounds), passive; persists | nothing (it is a friend) |
| `manta_ray` | 2.2×0.5 | 20 | glides in shallows in slow loops, flaps wings; passive | `leather` 0–1 |
| `jellyfish` | 0.6×0.8 | 6 | drifts and pulses, variants `moon` (white), `crimson` (red), `glow` (cyan, glows: render full bright); touching it stings (poison 3 s) unless wearing `wetsuit` | `jelly_blob` (moon/crimson) or `glow_jelly` (glow) |
| `anglerfish` | 0.9×0.7 | 18 | hostile in the trench, glowing lure (full-bright part), lurks then darts, bites 5 | `angler_lure` (light source ingredient), cod |
| `moray_eel` | 1.2×0.4 | 16 | hides in holes of reefs and ruins, lunges at players within 4 blocks, bites 4 | `eel_fillet` (food raw 2, cooked 6) |

Items: `jelly_blob`, `glow_jelly`, `angler_lure`, `eel_fillet`, `cooked_eel_fillet`, spawn eggs.

---------------------------------------------------------------------------------------------------------------------

## 8. Voyage (module `voyage`: 8.1) and diving (module `diving`: 8.2)

### 8.1 Boats

- `palm_boat`, `palm_chest_boat`: vanilla boat/chest boat with palm texture (`Boat`/`ChestBoat` entity types,
  `BoatRenderer` with own model layers + textures, items placing them).
- `driftwood_raft`, `driftwood_chest_raft`: raft-style (like the bamboo raft) with driftwood texture.
- **`expedition_raft`** – the signature vehicle. A large raft entity (3×3 deck look, hitbox ~3.0×0.6) that sails well:
  - Based on vanilla boat physics (`AbstractBoat`), driven by the passenger in the helm seat; **4 seats**.
  - **Built-in storage**: 27 slots, +27 with a second crate upgrade (`raft_crate` item) – opened by sneak-use on the
    raft (like chest boats). Contents drop when the raft is broken.
  - **Upgrades** (use the item on the raft; each shows on the model): `raft_sail` (mast + sail, speed ×1.5 when moving
    forward), `raft_engine` (outboard motor, speed ×2.2, burns furnace fuel from a fuel slot shown in the raft menu),
    `raft_anchor` (toggle with use: stops all drift and keeps the raft in place), `raft_crate` (second storage row set),
    `raft_lantern` (lantern posts, visual).
  - **Stable and leak-free**: it never sinks, does not tip, does not throw riders off when a wave or rain hits it, does
    not break on collisions, sharks cannot damage it; water is not drawn inside the hull (water mask like vanilla
    boats); only a player can break it (3 hits within 2 s, or sneak-attack), and it drops as item with its upgrades
    kept (stored in the item's components) plus the cargo.
  - Players **standing on the deck** (not seated) move with the raft.
  - Recipe: 6 `raft_foundation` + 2 `rope` + 1 `paddle` (shaped).

### 8.2 Diving gear

| Id | Slot | Effect |
| --- | --- | --- |
| `diving_helmet` | head | brass helmet with porthole. Underwater: clearer, farther vision (water fog pushed out, client); with a tank: breathing from the tank |
| `oxygen_tank` | chest | stores 2 400 ticks of air (2 min), shown as bar on the item and as a HUD gauge while underwater; refills at the surface (head out of water, 40 ticks of air per tick), in bubble columns, or in the `air_compressor` |
| `titanium_oxygen_tank` | chest | 7 200 ticks (6 min), armor like iron chestplate |
| `wetsuit` | legs | underwater mining speed ×3 (attribute `submerged_mining_speed`), immune to jellyfish stings |
| `flippers` | feet | faster swimming (attribute `water_movement_efficiency` +0.6), slower walking on land |
| `air_compressor` | block | fills tanks put in its slot (GUI), runs on FE (any adjacent energy source) or hand crank (use with empty hand: +100 air) |
| `harpoon_gun` | hand | shoots `harpoon` projectiles (8 damage underwater, 4 in air, retrievable), reload 1 s |
| `sea_scooter` | hand | hold use underwater: pulls you forward fast; 3 000 FE battery charged in the air compressor (or by battery blocks) |

Full set bonus (helmet + tank + wetsuit + flippers): Dolphin's Grace while swimming. Advancements: see 9.6.

---------------------------------------------------------------------------------------------------------------------

## 9. Story items, blocks and data (module `story`)

### 9.1 Items

| Id | Description |
| --- | --- |
| `expedition_journal` | Use: shows the current story objective and a short hint in a book-like screen (or chat), derived from the player's `raft:story/*` advancements; sneak-use: gives an exploration map to the next place (cooldown 5 min, shown on the item). Given by the castaway chest and craftable (book + compass + paper) |
| `message_in_a_bottle` | drifts in the sea (OceanDebris, rare); use: becomes a random lore letter (`raft:story/bottle_letters`) |
| `supply_barrel` | drifts in the sea (OceanDebris, rare); use: opens and drops loot `raft:gameplay/supply_barrel` (Raft-style barrels) |
| `coral_sigil`, `pearl_sigil`, `storm_sigil`, `ember_sigil` | Tide Sigils, rare/epic, glowing item models, not stackable, fire- and explosion-proof item entities |
| `tidewarden_key` | made at the `tidewarden_altar` from the 4 sigils; opens the Tide Seal and summons the Leviathan at its altar; not consumed |

### 9.2 Blocks

| Id | Description | Properties |
| --- | --- | --- |
| `tide_seal` | **unbreakable** (hardness -1, blast resistance 3 600 000) shimmering teal barrier with gold runes; using the `tidewarden_key` on it opens all connected `tide_seal` blocks (flood fill ≤ 256) permanently: `open=true` = no collision, no occlusion, faint particles | `open` |
| `sanctum_tidestone` | unbreakable variant of `tidestone_bricks` (looks the same, slightly darker) for the sanctum shell | – |
| `sanctum_glass` | unbreakable dark-teal glass (translucent) for the sanctum dome | – |
| `tidewarden_altar` | Tidewarden altar (temple): insert sigils one by one (use with sigil; shows them as glowing gems), with all 4 inside it forges the `tidewarden_key` (particles, sound) and keeps working (more keys for friends need 4 new sigils) | `facing`, `coral`, `pearl`, `storm`, `ember` (booleans) |
| `leviathan_altar` | sanctum altar at the edge of the boss pool; use with the key: summons the Leviathan (once per fight; not while one is alive). **Contract:** it stands on the arena floor at the pool's rim and its `facing` points to the pool centre, which is **9 blocks** away horizontally; the boss spawns at that centre 4 blocks below the altar | `facing`, `active` |
| `research_terminal` | Expedition computer; block entity holds `LogId` (string, set by structure NBT `{"id":"raft:research_terminal","LogId":"helix_1"}`); use: shows that log (title + text from lang `terminal.raft.<LogId>.title` / `.text`) in a screen or chat; screen glows (animated) | `facing`, `waterlogged` |
| `expedition_radio` | radio set; use: plays a short crackle and shows a random broadcast line (lang `radio.raft.<n>`, 12+ lines that hint at places and lore) | `facing`, `waterlogged` |
| `sigil_pedestal` | decorative stone pedestal (sanctum/temples) | `waterlogged` |

Terminal log ids (story owns the texts): `helix_1` … `helix_6` (Voss), `solace_1` … `solace_4` (city office/tower
terminals), `rig_1`, `rig_2` (rusted rig), `lab_misc_1` … `lab_misc_3` (generic). Structure builders place
terminals with these ids.

### 9.3 Loot tables (story owns them; structure builders only reference them)

Story chests (each contains the listed book/map/items plus a little themed loot):

| Loot table | Contents |
| --- | --- |
| `raft:chests/story/castaway` | Marlow's note #1 (book), `expedition_journal`, exploration map -> `raft:on_lighthouse_maps`, bread/fresh water/rope/paddle |
| `raft:chests/story/lighthouse` | Fenn's logbook (book), map -> `raft:on_saltmere_maps`, lantern supplies, rope |
| `raft:chests/story/saltmere_elder` | Nana Kalea's letters (book), `diving_helmet`, map -> `raft:on_helix_maps` |
| `raft:chests/story/saltmere_shrine` | `coral_sigil` (exactly 1), shells, pearls |
| `raft:chests/story/helix_lab` | Voss's journal (book), `oxygen_tank`, map -> `raft:on_port_solace_maps` |
| `raft:chests/story/helix_vault` | `pearl_sigil` (exactly 1), circuit boards |
| `raft:chests/story/port_solace_hall` | Mayor Crane's last broadcast (book), map -> `raft:on_ashen_forge_maps` |
| `raft:chests/story/port_solace_vault` | `storm_sigil` (exactly 1), gold, titanium |
| `raft:chests/story/ashen_forge` | Tidewarden tablet translation (book), map -> `raft:on_coral_temple_maps` |
| `raft:chests/story/ashen_vault` | `ember_sigil` (exactly 1) |
| `raft:chests/story/coral_temple` | Tidewarden prophecy (book), map -> `raft:on_abyssal_sanctum_maps`, prismarine, glowing ink |
| `raft:chests/story/pirate_captain` | Saltjaw's log (book), gold coins, `cutlass`, treasure |

Other chests (story writes them too, themed vanilla + mod loot; random lore letters at low chance):
`raft:chests/castaway_camp`, `raft:chests/abandoned_raft`, `raft:chests/beach_shack`, `raft:chests/saltmere_house`,
`raft:chests/saltmere_market`, `raft:chests/helix_storage`, `raft:chests/port_solace_office`,
`raft:chests/port_solace_apartment`, `raft:chests/port_solace_shop`, `raft:chests/ashen_storage`,
`raft:chests/coral_temple_offering`, `raft:chests/sanctum_reward`, `raft:chests/wreck_galleon`,
`raft:chests/wreck_cargo`, `raft:chests/wreck_fishing`, `raft:chests/pirate_treasure`, `raft:chests/rusted_rig`,
`raft:chests/sunken_plane`, `raft:chests/sunken_ruins`, `raft:chests/iceberg_outpost`, `raft:chests/lighthouse_storage`.
Gameplay: `raft:gameplay/supply_barrel`, `raft:story/bottle_letters` (one random letter book).

Books are `minecraft:written_book` made with loot functions (`minecraft:set_written_book_pages`, `set_book_cover`,
`set_custom_data` with `{raft_log:"<id>"}`), titles like "Fenn's Logbook". Maps use `minecraft:exploration_map` with
`destination` = the map tag, `decoration` (e.g. `minecraft:target_x`), `zoom` 2, `search_radius` 120,
`skip_existing_chunks` false, plus `minecraft:set_name`.

Structure tags (story writes them, `data/raft/tags/worldgen/structure/`): `on_lighthouse_maps` = [old_lighthouse],
`on_saltmere_maps` = [saltmere_village], `on_helix_maps` = [helix_station], `on_port_solace_maps` = [port_solace],
`on_ashen_forge_maps` = [ashen_forge], `on_coral_temple_maps` = [coral_temple], `on_abyssal_sanctum_maps` =
[abyssal_sanctum], `on_pirate_maps` = [pirate_cove]. Keep the existing `receiver_targets` tag.

### 9.4 Castaway chest and debris

- `RaftPortalForcer`: when it builds a new arrival platform, also place a chest with loot `raft:chests/story/castaway`
  on the platform (once per platform).
- `OceanDebris`: add `message_in_a_bottle` (rare) and `supply_barrel` (rare) to the drifting debris.

### 9.5 Expedition Journal objectives

Objectives follow the advancements: no `story/lighthouse` -> "Find the Old Lighthouse. Follow the map from the
castaway chest."; ... -> after `story/leviathan`: "The sea is calm. Thank you, keeper of the tides." Each objective
has a title, one line of hint and optionally the next map tag.

### 9.6 Advancements (story writes all, `data/raft/advancement/story/*.json` and `.../adventure/*.json`)

Story: `story/root` (enter `raft:raftdim`; background texture e.g. `minecraft:block/prismarine_bricks`),
`story/castaway` (get the journal), `story/lighthouse`, `story/saltmere`, `story/coral_sigil`, `story/helix`,
`story/pearl_sigil`, `story/port_solace`, `story/storm_sigil`, `story/ashen_forge`, `story/ember_sigil`,
`story/coral_temple`, `story/tidewarden_key`, `story/abyssal_sanctum`, `story/leviathan` (challenge frame, reward XP).
Structure ones use `minecraft:location` with `structures` predicates. Adventure: `adventure/set_sail` (ride the
expedition raft), `adventure/full_upgrade` (raft with sail+engine+anchor – a custom trigger is fine, or skip),
`adventure/deep_breath` (wear helmet + tank), `adventure/shark_bait`, `adventure/whale_watcher` (be near a whale –
skip if hard), `adventure/pirate_gold` (get a gold coin), `adventure/crab_dinner` (eat a cooked crab claw),
`adventure/rusted_giant` (visit the rig), `adventure/ghost_ship` (visit the galleon wreck).

---------------------------------------------------------------------------------------------------------------------

## 10. The Leviathan (module `boss`)

- `leviathan`: colossal sea serpent boss. Hitbox ~3.0×2.5 at the head/front; the long body (head + 6 segments +
  tail fin, ~14 blocks) is visual only and follows the head's path. 600 HP, armor 10, knockback immune, boss bar
  (blue, notched), immune to drowning/fire/fall, never despawns, does not leave the sanctum area (home = spawn point,
  leash 30 blocks).
- Arena (built by `structures_deep`): air-filled dome radius ~22, central pool radius 7, ≥ 10 deep, floor ring around
  it. The boss lives in the pool and attacks the floor ring.
- Phases and attacks (telegraphed with particles/sounds, fair to dodge):
  - `CIRCLE`: swims in the pool, invulnerable while fully submerged, 2–4 s.
  - `RISE_SPIT`: rises with the head above the water and spits 3–5 `brine_bolt` projectiles at players (5 damage +
    slowness 2 s). Vulnerable.
  - `LUNGE`: leaps out across the floor towards a player in an arc, slams down (8 damage in 4 blocks, knockback),
    lies **stranded for 4 s** (takes ×1.5 damage), then slithers back into the pool.
  - `TIDAL_ROAR` (below 50 % HP): roar that pushes players back and summons 3–4 `tideling` minions from the pool.
  - `WHIRLPOOL` (below 50 % HP): pulls players within 12 blocks toward the pool for 3 s.
  - Below 25 %: faster, attacks chain.
- `tideling`: small fish-like minion (0.7×0.7, 10 HP, bites 3, swims and hops on land for 10 s before it flops).
- `brine_bolt`: projectile entity (renders as a glowing teal blob).
- Defeat: death animation (sinks, light burst), drops `heart_of_the_tide` (1), `leviathan_scale` (8–12),
  `leviathan_trophy` (block item), 500 XP; triggers `story/leviathan`. The `leviathan_altar` can summon it again with
  the key + 1 `ancient_crystal` (so friends can fight it too).
- Items/blocks: `leviathan_scale` (crafting), `tidebreaker` (legendary trident-like spear/sword: 9 damage, +50 %
  damage to water mobs, Riptide-like dash when used in water/rain; recipe: 4 scales + titanium + trident),
  `heart_of_the_tide` (**block**: place it to get a conduit-like aura – Conduit Power + Dolphin's Grace for players in
  water within 32 blocks; glowing animated model), `leviathan_trophy` (head trophy decoration, `facing`).

---------------------------------------------------------------------------------------------------------------------

## 11. Structures

General rules for the structure builders:
- Templates are built in Python (`bclib.nbt.Structure`), saved to `data/raft/structure/<path>.nbt`. Use only block ids
  from vanilla 26.3 or from this spec (section 4–10) with the listed properties. Check with `preview.unknown_blocks`.
- Every structure: `data/raft/worldgen/structure/<id>.json` (type `minecraft:jigsaw` even for single pieces),
  `structure_set/<id>.json`, `template_pool/<id>/...json`, biome tag `data/raft/tags/worldgen/biome/has_structure/<id>.json`
  (reference the biome tags/ids of section 4.2), processors if needed (`processor_list/<id>.json`: decay with
  `minecraft:rule` + `random_block_match`, mossy/cracked variants, waterlogging handled by the game).
- Underwater: rooms that should be dry must be **fully enclosed** (glass/walls, doors as airlocks) and the template must
  contain `air` inside; rooms that should be flooded contain `water`. Anything left out keeps the terrain.
  Start with `"project_start_to_heightmap": "OCEAN_FLOOR_WG"` for things on the sea floor, `WORLD_SURFACE_WG` on
  islands, or a fixed `start_height` (absolute 99/100) for things floating on the surface.
- Chests: `Structure.chest(x, y, z, 'raft:chests/...', facing=...)`. Use `minecraft:barrel`, `raft:wooden_crate`,
  `raft:treasure_chest` too. Mobs: structure `spawn_overrides` (monster category) and/or a few placed entities with
  `PersistenceRequired`.
- Size limits: one template ≤ 48×48×48 is easiest; bigger builds as jigsaw pieces (max distance 128, depth ≤ 7).
- Look at every build: `preview.iso(...)`, `preview.slices(...)`, then in-game with bctest (`place structure raft:<id>`
  or `place template raft:<path>`), from outside and inside, at the surface and under water.
- Quality bar: varied silhouettes, depth (insets, trims, beams), decay that tells a story (collapsed floors, seaweed,
  barnacles, rust, broken glass), lighting, props from section 6, a reason to go inside (loot, logs, puzzles).

### 11.1 Story structures

| Id | Builder (`structures_*`) | Biomes (`has_structure`) | Spacing / separation (chunks) | Placement | Description and must-haves |
| --- | --- | --- | --- | --- | --- |
| `old_lighthouse` | surface | palm_isle, jungle_isle | 22 / 8 | island surface, `beard_thin` | Tall white-red striped lighthouse (≥ 28 high) on a rocky base, spiral stairs, lantern room with `lighthouse_lens`, keeper's cottage with bed/desk, small dock with buoys. Chests: `story/lighthouse` (keeper's desk), `lighthouse_storage` |
| `saltmere_village` | surface | coral_shallows, glowing_lagoon | 28 / 10 | jigsaw, stilts from sea floor to decks at y 101–104 | Abandoned stilt fishing village: central plaza deck with well/market stalls, 6–10 huts (fisher, net-maker, cookhouse, elder's hut, shrine, houses), rope-bridge walkways, docks with moored `palm_boat`-style decor, bunting, paper lanterns, string lights. Chests: `story/saltmere_elder` (elder hut), `story/saltmere_shrine` (shrine, behind the Tidewarden statue), `saltmere_house` ×n, `saltmere_market` |
| `helix_station` | deep | coral_shallows, glowing_lagoon, kelp_forest | 28 / 10 | sea floor | Expedition research base: 3–5 glass domes (`sea_glass`, `porthole`, `riveted_hull`) linked by tunnels, entrance airlock (two `bulkhead_door`s) and a moon pool, labs with `research_terminal` (`helix_1`..`helix_6`), aquarium tanks, bunks, generator room, partly flooded wing with moray eels. Chests: `story/helix_lab`, `story/helix_vault` (locked-looking vault room), `helix_storage` |
| `port_solace` | city | drowned_shelf | 36 / 14 | jigsaw on the shelf floor (y ≈ 45) | **The drowned city**: grid of asphalt streets with lane lines, street lamps, wrecked cars (built from blocks), plaza with a statue and fountain, 6–12 skyscrapers 30–60 high (some broken tops reaching above the sea surface), apartments/offices/shops with `port_solace_*` chests and terminals (`solace_1`..`solace_4`), central **City Hall tower** with an air-filled vault room near the top (`story/port_solace_vault`) and the mayor's office (`story/port_solace_hall`). Concrete variants, cracked glass, rust, kelp, barnacles. Drowned/corsairs/eels as spawn overrides |
| `ashen_forge` | surface | volcanic_isle | 30 / 12 | built into a volcano slope, `beard_box` | Tidewarden forge temple of tidestone and basalt: entrance stair, hall with lava channels, giant anvil/forge, glowing runes, ember crabs. Chests: `story/ashen_forge`, `story/ashen_vault` (vault behind a lava moat bridge), `ashen_storage` |
| `coral_temple` | deep | coral_shallows, glowing_lagoon | 32 / 12 | sea floor | Overgrown Tidewarden temple (tidestone, coral blocks, glowing tidestone): courtyard, colonnades, statues, inner sanctum with the `tidewarden_altar` (facing the entrance) and four `sigil_pedestal`s. Chests: `story/coral_temple`, `coral_temple_offering` ×2. Moray eels |
| `abyssal_sanctum` | deep | abyssal_trench, raft_ocean_biome | 40 / 16 | sea floor (deep) | Boss arena: entrance tunnel sealed by a 5×5 `tide_seal` door, all shell blocks `sanctum_tidestone`/`sanctum_glass` (unbreakable), inside an **air** dome (radius ~22, height ~18) with floor ring, pillars, `glowing_tidestone` light, central water pool (radius 7, ≥ 10 deep), `leviathan_altar` per the contract in 9.2, four `sigil_pedestal`s, reward room (`sanctum_reward` chest) behind the arena. No natural mob spawns inside |

### 11.2 Side structures (`structures_side`)

| Id | Biomes | Spacing / sep. | Description |
| --- | --- | --- | --- |
| `wreck_galleon` | wreck_graveyard, raft_ocean_biome | 26 / 9 | big pirate galleon (40 long) lying tilted on the floor, broken masts, teak + `jolly_roger`, cannons, captain's cabin and cargo hold (both `wreck_galleon` chests), corsairs |
| `wreck_cargo_ship` | wreck_graveyard, raft_ocean_biome, kelp_forest | 24 / 8 | rusty steel freighter (`rusty_hull`, `riveted_hull`) broken in two, spilled `shipping_container_*`s, bridge with `control_panel`; chests `wreck_cargo` |
| `wreck_fishing_boat` | all deep + shallows | 12 / 4 | small fishing boat wrecks (3 variants), nets, crates; `wreck_fishing` |
| `pirate_cove` | jungle_isle, palm_isle | 34 / 12 | hidden cave hideout in an island: sea entrance, dock with a small moored ship, treasure room with `gold_coin_pile`s, `treasure_chest` (`pirate_treasure`), captain's quarters (`story/pirate_captain`), torches/lanterns, `wanted_poster`, corsairs |
| `rusted_rig` | raft_ocean_biome, frost_floes | 30 / 10 | abandoned oil/research platform: steel legs from the floor to a deck at y 108–112, cranes, containers, helipad, living quarters, radio mast with `expedition_radio`, terminals `rig_1`/`rig_2`, `wind_turbine`; chests `rusted_rig` |
| `sunken_plane` | kelp_forest, raft_ocean_biome, coral_shallows | 28 / 10 | crashed seaplane/airliner on the floor (white fuselage of concrete/quartz, wings, broken tail), cockpit, cargo; `sunken_plane` |
| `sunken_ruins` | coral_shallows, kelp_forest, raft_ocean_biome, glowing_lagoon | 10 / 3 | small Tidewarden ruins, 4+ variants (columns, arches, broken statue head, stairs to nowhere, small shrine), tidestone + coral; `sunken_ruins` |
| `castaway_camp` | palm_isle | 16 / 6 | tiny camp: thatch shelter, campfire, SOS letters of stone on sand, crates; 3 variants; `castaway_camp` |
| `abandoned_raft` | raft_ocean_biome, kelp_forest, coral_shallows | 12 / 4 | floating raft of `raft_foundation` on the surface (deck at y 99) with sail, chest, crop plot, maybe a skeleton; 3 variants; `abandoned_raft` |
| `iceberg_outpost` | frost_floes | 24 / 8 | research hut on an ice floe (corrugated metal, porthole, stove), `iceberg_outpost` |
| `beach_shack` | palm_isle, jungle_isle | 18 / 6 | beach bar/shack with deck chairs, umbrellas, tiki torches, string lights; `beach_shack` |
| `buoy_field` | coral_shallows, raft_ocean_biome | 14 / 5 | 3–6 buoys and a chained floating crate (navigation markers) |

Salts: use 314159xx with a distinct number per structure set.

---------------------------------------------------------------------------------------------------------------------

## 12. Sounds

Vanilla sound events are fine everywhere. Custom ones (ogg, mono, 44.1 kHz) only if they sound good (e.g.
synthesised whale song); put them in `assets/raft/sounds/<module>/...` and the module's `sounds.json` fragment.

## 13. Testing checklist (lead, at integration)

Build + datagen-free resources load without errors (`latest.log`: no missing model/texture/loot/tag warnings);
`/locate structure` and `/place structure` for every structure; screenshots of every structure, biome, creature,
block group (creative tabs), boss fight from start to death; story walk-through with the journal; raft sailing
with all upgrades, storage, breaking/dropping; diving with each tank, refills; seagull vs. scarecrow; shark bites;
server start (dedicated) without client classes loading.

## 14. Deviations

(Builders: note any change to IDs/states/contracts here, one line each, with your module name.)
- building: wood family ids follow the vanilla naming (no `_planks` in the derived ids): `weathered_stairs`, `weathered_slab`, `weathered_fence`, `weathered_fence_gate`, `teak_stairs`, `teak_slab`, `teak_fence`, `teak_fence_gate` (+ `teak_door`, `teak_trapdoor`); coral bricks: `coral_stone_brick_stairs`, `coral_stone_brick_slab`, `coral_stone_brick_wall`.
- building: `steel_beam` is a real I-beam shape (not a full cube) and has `waterlogged` besides `axis` (so it looks right under water; the game waterlogs it when a template is placed in water).

## 15. Guide book (module `guide`, built after the other modules are integrated)

`raft:seafarers_almanac` – the in-game guide of Beach Craft, in the spirit of the Tamagotchi mod's Tamacon
(`/home/user/tamagotchi/src/main/java/net/kasax/tamagotchi/client/screen/TamaconScreen.java`): a custom screen styled
as a weathered, waterproof leather logbook with a contents page, chapters with item icons, creature portraits
(rendered entities), paged text from lang keys `almanac.raft.<chapter>.title/.text`. Chapters: Welcome, The Portal,
Survival on the Raft (water, food, debris, hooks, nets), Building (foundations, sets, decor highlights), Technology
(energy, machines, wind turbine), Sailing (boats, expedition raft and upgrades), Diving (gear, air, compressor, harpoon,
scooter), The Sea's Biomes, Islands and Wrecks (side places), Creatures (one page per creature), The Story (spoiler-free:
how the journal and maps guide you), The Leviathan (only after the boss is defeated or with a warning), Titanium & Tools,
Tips. Craftable early (book + kelp or + driftwood), given in the castaway chest and when a player first enters the raft
dimension.
