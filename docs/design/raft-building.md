# Raft Building – Design (module `vessel`)

Players build their own raft from blocks and sail the whole build as one vessel, like in survival-on-a-raft games.
Everything on the raft keeps working at sea: chests, furnaces, crafting, crops, redstone; players build and break on
it while it sails. The fixed Expedition Raft (module `voyage`) stays as it is. Everything new lives in the module
**`vessel`** (`net.kasax.raft.content.vessel`, resources `src/main/modules/vessel/`, generator `tools/content/vessel/`).

## 1. Player view

1. **Foundations on the water.** `raft_foundation` and `reinforced_foundation` can be placed straight onto the
   water surface (look at open water and use the item): the block replaces the top water block, its top is level
   with the sea. More foundations go against the sides of the first ones, so the raft grows block by block.
2. **Build on it.** Anything that stands on the foundations sails along: floors, walls, roof, chests, furnaces,
   farms, redstone, decorations. Rule: a block belongs to the raft when it is connected to the foundations and stands
   in a column above a foundation (holes inside the hull count as hull). Nothing below the lowest foundation layer,
   nothing outside the hull outline (keep roofs inside the edge).
3. **Helm** (`raft:helm`): a ship's wheel with a small seat, placed on the raft facing the bow.
   * Use it on an anchored raft: **set sail** (the raft becomes a vessel, the player sits at the helm).
   * At the helm: `W`/`S` sail forward/back, `A`/`D` turn, `Shift` leaves the helm, **using the wheel drops anchor**.
   * Not at the helm: using the helm takes it; **sneak + use drops anchor**.
   * Dropping anchor: the raft slows down, turns to the nearest compass direction, slides onto the block grid and
     becomes blocks again (chests keep their contents, furnaces their smelting, crops their growth, ...).
   * Nobody at the helm: the raft slows down and stays where it is. It never drops anchor by itself (not when the
     helmsman leaves, logs out, or the world is reloaded).
4. **At sea everything works.** Open chests, barrels, furnaces, crafting tables, anvils; move items; farm (crops
   grow, bone meal, hoe, harvest, replant); flip levers and buttons (redstone runs); place and break blocks on the
   raft (survival mining speed as on land, drops land on the deck). Furnaces smelt and crops grow while sailing.
5. **Propulsion** (blocks on board): `raft:oar` (+1 power, set on the deck edge), `raft:sail` (+3, stackable:
   each sail block counts, stacked sails share one mast), `raft:raft_motor` (+6 while it has fuel; use furnace
   fuel on it, also while sailing). Without any of these the helmsman can only paddle slowly (power 0.5).
   Top speed = 0.45 · P / (P + blocks/8) blocks per tick (≈ 2–6 blocks/s), heavier rafts accelerate and turn slower.
6. **Limits:** to set sail at most 1024 blocks, 32 × 32 blocks of hull, 24 blocks high; at sea a raft may grow to
   2048 blocks within its plot (8 blocks around the hull, 32 high). The raft must float (at least half of its hull
   bottom over water) and only sails where there is water under it; land, ice, piers, other blocks and other
   sailing rafts stop it. Portals, bedrock and similar blocks cannot go to sea.

## 2. How it works

* **Shipyard** (`VesselShipyard`). Each sailing raft owns a plot: a box of 48 × 32 × 48 blocks in the same
  dimension, far out (x, z from 28 000 000 on, one plot every 64 blocks, high above the terrain: top 8 blocks below
  the build limit). Taken plots are saved per dimension (`SavedData raft:vessel_shipyard`). A raft's blocks are real
  blocks in its plot, so vanilla does all the work: block entity ticks, random ticks (crops), scheduled ticks and
  redstone. A custom chunk ticket (`raft:vessel_plot`, level 31 = entity ticking, not persistent) keeps the plot's
  chunks loaded and ticking exactly while the raft entity is loaded (as for any farm, nothing ticks far from players).
* **Set sail (server).** `VesselAssembler` flood-fills the hull from the foundation under the helm, then the
  structure inside the hull outline. It takes a free plot, waits until its chunks are loaded (the first use of a plot
  generates its chunks: a moment of "Setting sail..."), then moves the blocks into the plot in the raft's own frame
  (helm facing south) with their block entity data (`saveWithId` / `loadWithComponents`), updates their neighbours
  there and removes them from the world without drops or side effects (cells below the waterline get water back).
  Item frames and paintings hanging on the raft are stowed (NBT) and hung back when anchoring.
* **Vessel entity** `raft:vessel` (`VesselEntity`): holds a copy of the plot's blocks (`VesselStructure`: block map,
  collision boxes, hull outline, propulsion stats) and its plot number. It is **server-authoritative**: the server
  reads the helmsman's input (`ServerPlayer.getLastClientInput()`), simulates speed/yaw rate, checks every move
  against terrain (exterior blocks only, rotated boxes) and water under the hull. Clients get the blocks in one
  payload when they start tracking the vessel and interpolate position + precise yaw (synced float).
* **Plot ↔ raft.** A mixin on `ServerLevel.sendBlockUpdated` reports block changes in a plot to its raft, which
  updates its copy once per tick and sends the changes to the clients that track it (`VesselBlocksPayload`); a full
  comparison every 10 s catches anything else. Entities spawned in a plot (drops of broken blocks, items from
  dispensers, mobs from spawn eggs), sounds and level events (break particles, door and lever sounds) are moved to
  where that spot is in the world, on the raft (mixins on `addFreshEntity`, `playSeededSound`, `levelEvent`).
* **Collision** for entities: a mixin on `EntityGetter.getEntityCollisions` adds the vessel's block boxes (rotated,
  as axis-aligned boxes) so players, mobs, items and boats stand on and bump into the vessel like into blocks.
  Players are only tested against the vessel on their own client (the server copy is a few ticks ahead, same rule as
  the Expedition Raft); server mixins keep players on a vessel from the "floating too long" kick and from the
  sneak-edge check. Blocks placed or broken at sea change the collision at once (the copy follows the plot).
* **Carrying.** Each tick the vessel moves everything that rests on it or is in the air above its deck (up to
  3 blocks) by its own translation and rotation: the server carries mobs and items, each client its own player
  (with the render interpolation corrected, so it is smooth whichever entity ticks first).
* **Using blocks at sea.** A client mixin on `Minecraft.pick` ray-casts the vessel in its local frame (exact block
  shapes) and targets it like an entity. The use key sends `VesselUsePayload` (block, hit point and face in the raft's
  frame); the server runs vanilla `ServerPlayerGameMode.useItemOn` on the plot block, with the player turned into the
  raft's frame for the call (placed stairs, doors, furnaces point the right way). Menus stay open: a mixin on
  `Player.isWithinBlockInteractionRange` measures reach to where the plot block is in the world, and chests count
  their openers among all players (`ContainerOpenersCounter`). If the block and the item do nothing, the server says
  so (`VesselUseResultPayload`) and the client uses the item in the air (eat, drink, throw, scoop water). Breaking:
  the client counts the mining progress like vanilla (tool, enchantments, effects, on ground) and sends
  `VesselBreakPayload` start/finish; the server checks the time it took and runs `destroyBlock` in the plot.
* **Drop anchor.** Only on request (helm, or NBT `DropAnchor:1b`): brake, pick the nearest grid pose (yaw multiple
  of 90°, block-aligned; nearby alternatives if blocked), glide into it, move the blocks from the plot into the world
  (no shape updates while placing, then neighbour updates), load block entity data, empty the plot and free it. The
  entity is still drawn for a few ticks so clients never see a gap. If no pose is free (e.g. jammed against a pier)
  the raft says so and stays at sea.
* **Saving.** The plot's blocks are saved with their chunks. The vessel saves its plot number, a copy of its blocks
  (palette + packed positions, so it can be stood on before the plot is loaded again), stowed entities, speed and
  autopilot. It never leaves with a logging-out player (`hasExactlyOnePlayerPassenger` = false), cannot use portals
  and ignores damage, pistons and explosions. `/kill` drops anchor on the spot (or breaks every block of the raft
  with normal drops at the raft if there is no room).
* **Autopilot** (map makers, tests): `/data merge entity @e[type=raft:vessel,limit=1]
  {Autopilot:{Forward:1b,Turn:-1b,Ticks:200}}` sails as if a helmsman held these keys (Forward/Turn -1..1)
  for that many ticks, also without anyone at the helm; `{DropAnchor:1b}` drops anchor.

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
* Things that need the block in the client's world wait until anchored: beds (sleeping), editing sign text. Block
  entity renderers are not drawn at sea (sign text, banner patterns, chest lid animation); chests, furnaces and lit
  lamps are drawn with their block models.
* Open water cannot be poured onto a sailing raft (it would run off the deck); water buckets go into waterloggable
  blocks. Pressure plates, tripwires and other blocks triggered by entities do not see entities on the deck.
* Item frames and paintings are stowed while sailing (not usable); blocks pushed or flowing out of the plot leave the
  raft.
* Mobs on board use world pathfinding and are held still while the raft moves; leads tied to fences on board break
  when setting sail.
* The first time a plot is used its chunks are generated (a few seconds on a slow server). A plot is loaded and ticks
  only while its raft is loaded.

## 5. Tests

`tools/bctest/run.sh tools/content/vessel/tests/raft_1.txt <out>` builds a raft with a chest, a furnace with raw iron
and coal, a moist wheat field, a lever next to a redstone lamp and a chicken in a walled pool of the raft dimension,
sets sail at the helm, leaves the helm, flips the lever (the lamp in the plot lights), places and breaks a block,
opens the chest and keeps it open while the raft sails, takes the diamonds out, checks that the raft holds position
without a helmsman, carries a player standing at a corner through a turn (`corner.py <out>/gradle.log 11.5,0,12.5`
prints the player's position in the raft frame), checks that the wheat grew and the furnace smelted in the plot and
keeps the world; `raft_2.txt` reopens it with the raft at sea, checks the plot, drops anchor and checks the blocks in
the world.
