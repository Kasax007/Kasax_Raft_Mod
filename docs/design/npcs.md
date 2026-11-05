# People of the Sea – NPC design (module `npc`)

Beach Craft 3 tells its story through logs, letters and empty places. That works, but the sea felt deserted: nobody
answers, nobody trades, and the bottle letters name survivors you never meet. A few people who stayed behind make the
places warmer, give the player someone to ask when lost, and tie the side places to the story. The rule: **NPCs help,
the story never depends on them** – every story item still comes from its chest, so a missing or removed NPC cannot
block anything.

## Who lives where

| Person | Place (template) | Role in the story | Hands over (first talk) | Trades |
| --- | --- | --- | --- | --- |
| Keeper Tobias Fenn | `old_lighthouse` (cottage, both variants) | Ch. 2 guide: points to Saltmere and Nana Kalea, later asks about her | spyglass | buys cod/salmon; candles, lanterns, glowstone, spyglass |
| Nalu, Kalea's grandson | `saltmere_village/elder` (veranda) | Ch. 3 guide: letters in the house, Coral Sigil in the shrine, then Helix | Potion of Water Breathing (8:00) | Saltmere crafts for pearls (lanterns, rugs, chimes, shell lamp, a spare diving helmet); seashells to pearls |
| Old Tamo | `saltmere_village/docks` (pier) | Village life; fisherman, grandpa of Mia (bottle letter) | 3 cooked cod | buys fish, crab claws, eel; fishing rod (Luck of the Sea), sea pickles, glow kelp, palm boat, nautilus shell |
| Dr. Imara Voss | `ashen_forge/hall` (storeroom) | Ch. 6–8: the scientist who cracked the seal studies the tablets ("the forge lights are back on"); Ember vault, then the Coral Temple altar | Potion of Fire Resistance (8:00), again after 5 min if lost, until the Ember Sigil is taken | – |
| Teo | `beach_shack/tiki_bar` (behind the counter) | Side: waits for Ana (bottle letter "Dearest Ana"), from Port Solace | piña colada | drinks and island food; buys pineapples, coconuts, rum |
| Kai | `castaway_camp/tent` (at the fire) | Side: the SOS castaway (bottle letter "SOS"), wants biscuits | – | barters cookies for flotsam: bottle letters, rope, plastic, scrap, supply barrels; buys a net |
| Dr. Elin Lund | `iceberg_outpost` (hut) | Side: polar researcher (bottle letter, radio line) | "tea" (honey bottle) | buys jelly, angler lures, shark teeth, leviathan scales; harpoons, wetsuit, flippers, oxygen tank |
| Patience Okafor | `rusted_rig` (control room) | Side: last engineer of Rig 7 (bottle letter "Note from Rig 7") | circuit board | tech parts: scrap, redstone, cables, circuit boards, titanium, battery; buys copper, iron, plastic |

Only the tiki-bar beach shacks and the tent camps are inhabited; the other variants stay abandoned, so frequent side
places do not repeat the same person too often. Deep places (Helix, Port Solace, temple, sanctum) stay empty: their
silence is part of the story.

## What they say

Right-click opens a conversation panel (portrait, name, title, typed text) with four or five choices:
*Tell me about this place* (who they are), *What should I do next?* (a hint for the player's current story step,
from the `raft:story/*` advancements, same rules as the Expedition Journal), *Heard any rumours?* (one of three lines
pointing to other places and people), *Let's trade* (vanilla trade screen) and *Goodbye*. The greeting changes with the
story: Fenn before and after Saltmere and after Kalea's sigil, Nalu before/after the Coral Sigil and after Helix, Voss
before the Ember Sigil, after it (temple and altar) and after the key, Teo once Port Solace was found; everyone has a
line for the calm sea after the Leviathan. Hitting a person only makes them protest.

## Behaviour

- Home = where the template puts them (or a spawn egg); they wander in a small radius, return when lost (teleport
  home after 5 s in water, far away, high above home or on top of a railing), never despawn, cannot be hurt (except
  by creative players and `/kill`), are not pushed, leashed, carried off by boats or portals, or targeted by
  monsters. They walk on floors only: the top of a fence, railing or wall is no floor for their path finding (vanilla
  lets mobs step from a table onto a railing and walk along it). A place of interest is worked from a floor cell next
  to it (a chair or a table is not stood on); one up high (lantern, chime) from the floor below it.
- Idle per person: Fenn walks between desk, radio and stove and now and then watches the sea through his spyglass;
  Tamo fishes from the pier (casts, splashes, bites, sometimes a fish); Voss walks between anvil and storage and reads
  her notes; Kai sits at the camp fire and gets up now and then; Teo, Lund, Okafor and Nalu work at the blocks of
  their place (barrels, grill, terminal, control panels, lanterns). They stop and face whoever talks to them.
- Currency: pirate gold coins and pearls (both found at sea). Trades restock every 10 minutes; uses are saved.
- Per player (attachment `raft:npc_met`, kept on death): who they met and when a gift was handed over, so gifts come
  once. Advancements `raft:adventure/new_friend` and `raft:adventure/every_soul` (meet all eight).

## Technical notes

- One entity type per person (`raft:keeper_fenn` …), class `NpcEntity` (PathfinderMob + Merchant), MISC category,
  player-shaped model (vanilla player layers, slim arms for Voss, Lund, Okafor) with skins from
  `tools/content/npc/characters.py`. Spawn eggs in the Creatures tab. Creative sneak-use on a person moves their home.
- Placement: template entities. `tools/content/npc/place.py` patches the saved templates of the structure modules
  (only their entity list; spots are relative to a known anchor block, checked when the script runs). Every newly
  generated structure gets its person exactly once (an entity is placed by the chunk that contains it).
- Older worlds: `NpcRetrofit` looks at every structure start that loads, works out the person's spot from the
  saved pieces (template + position + rotation) and, once the chunks around the spot have their entities loaded,
  spawns the person if nobody of that kind is within 24 blocks. Each spot is then marked in a chunk attachment
  (`raft:npc_spots_done`), so it happens once: structures from 3.0 worlds get their people when the player comes
  near, new structures (which already have them) are only marked, and a person removed later stays removed.
- Multiplayer: conversations are per player (payloads `raft:npc_dialog` / `raft:npc_action`), only one player trades
  with a person at a time, the server checks distance on every action.
