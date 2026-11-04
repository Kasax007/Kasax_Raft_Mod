# Raft Building – Design (module `vessel`)

Players build their own raft from blocks and sail the whole build as one vessel, like in survival-on-a-raft games.
The fixed Expedition Raft (module `voyage`) stays as it is. Everything new lives in the module **`vessel`**
(`net.kasax.raft.content.vessel`, resources `src/main/modules/vessel/`, generator `tools/content/vessel/`).

## 1. Player view

1. **Foundations on the water.** `raft_foundation` and `reinforced_foundation` can be placed straight onto the
   water surface (look at open water and use the item): the block replaces the top water block, its top is level
   with the sea. More foundations go against the sides of the first ones, so the raft grows block by block.
2. **Build on it.** Anything that stands on the foundations sails along: floors, walls, roof, chests, stations,
   decorations. Rule: a block belongs to the raft when it is connected to the foundations and stands in a column
   above a foundation (holes inside the hull count as hull). Nothing below the lowest foundation layer, nothing
   outside the hull outline (keep roofs inside the edge).
3. **Helm** (`raft:helm`): a ship's wheel with a small seat, placed on the raft facing the bow. Use it to set sail:
   the raft turns into a vessel and the player sits at the helm.
   * `W`/`S` sail forward/back, `A`/`D` turn; leave the helm with `Shift` (or use the wheel again).
   * When nobody is at the helm, the raft slows down, turns to the nearest compass direction, slides onto the block
     grid and **drops anchor**: it becomes blocks again (chests keep their contents, signs their text, ...).
4. **Propulsion** (blocks on board): `raft:oar` (+1 power, set on the deck edge), `raft:sail` (+3, stackable:
   each sail block counts, stacked sails share one mast), `raft:raft_motor` (+6 while it has fuel; use furnace
   fuel on it, also while sailing). Without any of these the helmsman can only paddle slowly (power 0.5).
   Top speed = 0.45 · P / (P + blocks/8) blocks per tick (≈ 2–6 blocks/s), heavier rafts accelerate and turn slower.
5. **Limits:** at most 1024 blocks, 32 × 32 blocks of hull, 24 blocks high. The raft must float (at least half
   of its hull bottom over water) and only sails where there is water under it; land, ice, piers and other
   blocks stop it. Portals, bedrock and similar blocks cannot go to sea.

## 2. How it works

* **Assembly (server).** `VesselAssembler` flood-fills the hull from the foundation under the helm, then the
  structure inside the hull outline. Blocks are stored in a canonical frame (helm faces south), block entities as
  NBT (`saveWithId`). Blocks are removed without drops or side effects (`UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS`,
  `UPDATE_SUPPRESS_DROPS`, no shape updates); cells below the waterline get water back. Item frames and paintings
  hanging on the raft are stowed (NBT) and hung back when docking.
* **Vessel entity** `raft:vessel` (`VesselEntity`): holds a `VesselStructure` (block map, collision boxes,
  hull outline, propulsion stats). It is **server-authoritative**: the server reads the helmsman's input
  (`ServerPlayer.getLastClientInput()`), simulates speed/yaw rate, checks every move against terrain (exterior
  blocks only, rotated boxes) and water under the hull. Clients get the blocks in one payload when they start
  tracking the vessel and interpolate position + precise yaw (synced float).
* **Collision** for entities: a mixin on `EntityGetter.getEntityCollisions` adds the vessel's block boxes
  (rotated, as axis-aligned boxes) so players, mobs, items and boats stand on and bump into the vessel like into
  blocks. Players are only tested against the vessel on their own client (the server copy is a few ticks ahead,
  same rule as the Expedition Raft); a server mixin keeps players on a vessel from the "floating too long" kick.
* **Carrying.** Each tick the vessel moves everything that rests on it or is in the air above its deck (up to
  3 blocks) by its own translation and rotation: the server carries mobs and items, each client its own player
  (with the render interpolation corrected, so it is smooth whichever entity ticks first).
* **Docking.** Without helmsman: brake, pick the nearest grid pose (yaw multiple of 90°, block-aligned; nearby
  alternatives if blocked), glide into it, place the blocks (no shape updates while placing, then neighbour
  updates), load block entity data, and remove the entity a few ticks later (so clients never see a gap).
  If no pose is free (e.g. jammed against a pier) the raft says so and retries; someone can take the helm.
* **Interaction while sailing.** A client mixin on `Minecraft.pick` ray-casts the vessel in its local frame
  (exact block shapes) and targets it like an entity; the use key sends `VesselUsePayload` with the local block:
  helm = take/leave the helm, motor + fuel = refuel. Other blocks: "drop anchor to use it". Blocks of a sailing
  raft cannot be broken or built on.
* **Saving.** The vessel saves its blocks (palette + packed positions), block entity NBT, stowed entities, speed
  and fuel. A vessel loaded without helmsman docks. It never leaves with a logging-out player
  (`hasExactlyOnePlayerPassenger` = false), cannot use portals and ignores damage, pistons and explosions.
  `/kill` drops anchor on the spot (or drops everything as items if there is no room).
* **Autopilot** (map makers, tests): `/data merge entity @e[type=raft:vessel,limit=1]
  {Autopilot:{Forward:1b,Turn:-1b,Ticks:200}}` sails as if a helmsman held these keys (Forward/Turn -1..1)
  for that many ticks, also without anyone at the helm; then the raft drops anchor as usual.

## 3. Content

| Id | What | Recipe |
| --- | --- | --- |
| `helm` | helm: wheel on a post with a seat, `facing` = bow | ship_wheel over stick over 3 planks |
| `sail` | mast + canvas, `facing`, `top`/`bottom` (stacked sails share a mast) | rope, 2 sail canvas, 2 sticks |
| `oar` | oar on a rowlock, `facing` = outboard | paddle + rope + stick |
| `raft_motor` | outboard motor, `facing` = stern, block entity with fuel | scrap metal, circuit board, furnace, titanium |
| `vessel` (entity) | the sailing raft | – |

Advancement `raft:adventure/shipwright` ("Shipwright": set sail on a raft you built; criterion
`started_riding` with vehicle type `raft:vessel`), parent `adventure/set_sail`. Almanac: section
`sailing/shipwright` (bookmark `{almanac:"sailing/shipwright"}`). Creative tab: VOYAGE.

## 4. Limits (known)

* While turned by a non-right angle, block boxes are approximated by axis-aligned boxes (slightly wider), so stairs
  on a raft that is not aligned may need a jump. Exact when aligned.
* Blocks on a sailing raft do not tick (furnaces, crops wait until anchored); sign text and banner patterns are not
  drawn while sailing; chests can be opened once anchored.
* Mobs on board use world pathfinding and may walk off; leads tied to fences on board break when setting sail.
