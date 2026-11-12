# Beach Craft on NeoForge (26.3)

Branch `port/26.3-neoforge` is the Fabric branch `26.3` with a NeoForge loader layer. The mod code, the
mod id `raft`, every registry id, the resources (`src/main/resources`, `src/main/generated`, `src/main/modules`) and the
save data are the same as on Fabric.

## Build

- `sh ./gradlew build` (Java 25, ModDevGradle, NeoForge `neo_version` in `gradle.properties`). The jar holds the merged
  resources (task `mergeResources`, unchanged), `META-INF/neoforge.mods.toml` and `META-INF/accesstransformer.cfg`.
- The access transformer widens what Fabric API's access wideners widen for the Fabric build (spawn placements, menu
  screens, three `Blocks` helpers, the entity tracker for `PlayerLookup`).
- Fabric's data generators (`net.kasax.raft.datagen`, `RaftDataGenerator`) are left out of the build; their output in
  `src/main/generated` is committed and goes into the jar. Run them on the Fabric branch.

## The loader layer (`net.kasax.raft.platform`)

The classes have the names and methods of the Fabric API and Team Reborn Energy calls Beach Craft makes, implemented
with NeoForge. A merge from `26.3` mostly needs the imports pointed here:
`net.fabricmc.fabric.api.<pkg>.<Class>` becomes `net.kasax.raft.platform.<Class>` (client parts:
`net.kasax.raft.platform.client`, transfer and block API caches: `.transfer`), `team.reborn.energy.api(.base)` becomes
`net.kasax.raft.platform.energy`, `@Environment(EnvType.CLIENT)` is dropped (`python3 tools/neoforge_imports.py src` does it).

- `NeoPlatform`: the `@Mod` constructor (`Raft`) hands over; the mod's init runs in the last `RegisterEvent` (all
  registries are open, NeoForge's own entries exist), the client init right after it. What NeoForge wants in later
  events is recorded and handed over then: entity attributes, payloads, capabilities, layer definitions, render layers,
  creative tab entries, biome modifier codec.
- Events: Fabric callbacks on NeoForge events (server/level/client ticks, lifecycle, entity load/unload, chunk load,
  player join/leave/level change, use item/block, loot table load, axe stripping).
- Networking: `PayloadTypeRegistry`/`ServerPlayNetworking`/`ClientPlayNetworking` on NeoForge payloads (main thread).
- Menus with opening data: `ExtendedMenuType` is a NeoForge menu type with extra data; a mixin on
  `ServerPlayer.openMenu` sends the data like Fabric does.
- Energy: Team Reborn Energy's classes on NeoForge's transfer API: Beach Craft's transactions are NeoForge
  transactions and its snapshot participants NeoForge snapshot journals, so energy moved to or from other mods'
  `EnergyHandler`s is part of the same transaction. Beach Craft's storages are offered as NeoForge `EnergyHandler`s; the
  item charge stays in the component `team_reborn_energy:energy` (same as the Fabric build).
- Items: every Beach Craft container block entity has NeoForge's item capability (`ResourceHandler<ItemResource>`;
  Fabric's transfer API gives all containers one).
- Data maps instead of Fabric registries: fire (`data/neoforge/data_maps/block/flammable.json`) and axe stripping
  (`transformables.json`), exported from the code with `-Dbc.datamaps=<dir>` on a server with the test mod; a game test
  checks them. Point of interest types are registered without vanilla's state mapping (NeoForge maps the states).
- Biome changes: one NeoForge biome modifier (`data/raft/neoforge/biome_modifier/biome_modifications.json`) runs the
  recorded Fabric-style modifications in Fabric's order and, like Fabric, does not add a feature a biome already has.
- Attachments: NeoForge data attachments with the same ids and codecs (saved under `neoforge:attachments`); entities
  and chunks saved by the Fabric build (`fabric:attachments`) keep theirs (game test `attachments`).

## Differences to the Fabric build

- The fishing hook mixin leaves out its redirect: NeoForge already accepts any `FishingRodItem` (`canPerformAction`).
- Fabric's `ServerEntityEvents.ALLOW_LOAD` (the scarecrow keeping monsters away) runs on NeoForge's `FinalizeSpawnEvent`,
  which has the spawn reason for every newly spawned mob.
- Fabric's loot table source (`isBuiltin`) is not known on NeoForge: Beach Craft's chest pools are also added to its
  tables when a data pack replaces them.
- Recipes with `fabric:load_conditions` load unconditionally (all conditions name Beach Craft's own items).

## Tests

- NeoForge game tests (loader layer, registry based): `sh ./gradlew runGameTestServer`
  (`src/gametest/.../neo/BcGameTests.java`).
- Scripted client tests: `tools/bctest/run.sh <script> <out>` as before (`BC_DISPLAY` picks the Xvfb display); the
  harness in `src/gametest` is a NeoForge client mod with its own copy of the client game test API Beach Craft's
  scripts use (SDL input, screenshots read back asynchronously). `BC_JAR=<jar>` runs the harness against a built jar
  instead of the main classes; `BC_LOOTR=1` / `BC_JEI=1` add Lootr / JEI (`tools/bctest/lootr.txt`).
- Registry dump: start a server with `raft_gametest.jar` (task `gametestJar`) next to the release jar and
  `-Dbc.dump=<file>`; `-Dbc.audit=true` loads every mixin target.
