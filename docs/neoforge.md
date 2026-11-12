# Beach Craft on NeoForge (1.21.1)

Branch `port/1.21.1-neoforge` is the Fabric branch `port/1.21.1` with a NeoForge loader layer. The mod code, the
mod id `raft`, every registry id, the resources (`src/main/resources`, `src/main/generated`, `src/main/modules`) and the
save data are the same as on Fabric.

## Build

- `sh ./gradlew build` (Java 21, ModDevGradle, NeoForge `neo_version` in `gradle.properties`). The jar holds the merged
  resources (task `mergeResources`, unchanged), `META-INF/neoforge.mods.toml` and `META-INF/accesstransformer.cfg`.
- The access transformer widens what Fabric API's access wideners widen for the Fabric build (spawn placements, menu
  screens, two `Blocks` helpers, the model part faces, the entity tracker for `PlayerLookup`).

## The loader layer (`net.kasax.raft.platform`)

The classes have the names and methods of the Fabric API and Team Reborn Energy calls Beach Craft makes, implemented
with NeoForge. A merge from `port/1.21.1` mostly needs the imports pointed here:
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
- Menus with opening data: `ExtendedScreenHandlerType` is a NeoForge menu type with extra data; a mixin on
  `ServerPlayer.openMenu` sends the data like Fabric does.
- Energy: Team Reborn Energy's classes with a small Fabric-style transaction system (`transfer.Transaction`,
  `SnapshotParticipant`). Beach Craft's storages are offered to other mods as NeoForge energy (`IEnergyStorage`), other
  mods' NeoForge energy blocks join Beach Craft's transactions (amounts simulated, moved on the outer commit). The item
  charge stays in the component `team_reborn_energy:energy` (same as the Fabric build).
- Items: every Beach Craft container block entity has NeoForge's item handler capability (Fabric's transfer API gives
  all containers one).
- Data maps instead of Fabric registries: `data/neoforge/data_maps/item/compostables.json` and `furnace_fuels.json`
  (exported from the code with `-Dbc.datamaps=<dir>` on a server with the test mod; a game test checks them).
- Biome changes: one NeoForge biome modifier (`data/raft/neoforge/biome_modifier/biome_modifications.json`) runs the
  recorded Fabric-style modifications in Fabric's order and, like Fabric, does not add a feature a biome already has.
- Attachments: NeoForge data attachments with the same ids and codecs (saved under `neoforge:attachments`).

## Differences to the Fabric build

- Mixins that NeoForge makes unnecessary are left out: the fishing hook and its renderer accept any `FishingRodItem`
  (NeoForge asks `canPerformAction`), crops grow faster on the crop plot because it is a `FarmBlock` (NeoForge counts
  every farm block and its moisture).
- Fabric's loot table source (`isBuiltin`) is not known on NeoForge: Beach Craft's chest pools are also added to its
  tables when a data pack replaces them.
- Recipes with `fabric:load_conditions` load unconditionally (all conditions name Beach Craft's own items).

## Tests

- NeoForge game tests (loader layer): `sh ./gradlew runGameTestServer` (`src/gametest/.../neo/BcGameTests.java`).
- Scripted client tests: `tools/bctest/run.sh <script> <out>` as before (`BC_DISPLAY` picks the Xvfb display); the
  harness in `src/gametest` is a NeoForge client mod.
- Registry dump: start a server with `raft_gametest.jar` (task `gametestJar`) next to the release jar and
  `-Dbc.dump=<file>`; `-Dbc.audit=true` loads every mixin target.
