# Beach Craft – sound and music assessment

Branch `feature/sounds-music`, module `audio` (`net.kasax.raft.content.audio`, resources `src/main/modules/audio/`,
generator `tools/content/audio/`). This page lists every sound the mod makes: what it used before (vanilla placeholder,
nothing, or own), whether a sound of its own is needed, and what is done now.

**How the new sounds are made.** Every sound effect is synthesised by `tools/content/audio` (numpy/scipy: oscillators,
noise, glottal-pulse voices with formants, bubble and splash models, modal synthesis for wood/metal/stone, convolution
reverb, compression and loudness mastering), exported as mono Ogg Vorbis 44.1 kHz (the sea loop in stereo). No recordings
are used. The music is by Kevin MacLeod (incompetech.com, CC BY 4.0, credits in `CREDITS_MUSIC.txt` in the jar and in
`docs/curseforge/description.html`). Loudness is set per sound (LUFS) against the vanilla sounds of the same kind.

Legend for "needed": **custom** = a vanilla stand-in that does not fit (or silence where a sound is expected), a sound
of its own is needed; **ok** = the vanilla sound fits (often on purpose), nothing to do; **–** = silent on purpose.

## Creatures

| Thing | Before | Needed | Done |
| --- | --- | --- | --- |
| Shark / hammerhead: ambient | dolphin whistles (pitched down) – sharks do not whistle | custom | `entity.shark.ambient`: a slow tail sweep and the low pressure of a big body, under water |
| Shark / hammerhead: hurt, death | dolphin hurt/death | custom | `entity.shark.hurt` / `.death`: thrashing water, bubbles, a knock |
| Shark / hammerhead: bite | evoker fangs | custom | `entity.shark.bite`: jaw snap, crunch, water swoosh |
| Shark bites the raft / breaks a plank | zombie on wooden door + evoker fangs / zombie breaks door | custom | `entity.shark.gnaw` (teeth in planks, splinters, splash), `entity.shark.raft_break` (splintering crack, pieces in the water) |
| Shark swim | dolphin swim (a splash) | ok | unchanged |
| Whale: song, spout | own (synthesised) | – | kept |
| Whale: hurt, death | dolphin hurt/death at pitch 0.4 | custom | `entity.whale.hurt` (groan), `entity.whale.death` (long falling moan, bubbles) |
| Manta ray: hurt, death | guardian hurt/death | custom | `entity.manta_ray.hurt` / `.death`: wing flap through water, a soft squeak, bubbles |
| Jellyfish: sting | sweet berry bush prickle | custom | `entity.jellyfish.sting`: electric crackle of nematocysts, squish |
| Jellyfish: hurt, death | small slime | custom | `entity.jellyfish.hurt` / `.death`: gelatinous squelch and bubbles |
| Anglerfish: ambient | none | custom | `entity.anglerfish.ambient`: the lure's eerie hum (quiet, 12 blocks) |
| Anglerfish: bite, hurt, death | evoker fangs, pufferfish | custom | `entity.anglerfish.bite` (snap and gulp), `.hurt` / `.death` (croaky gurgle) |
| Moray eel: lunge bite | evoker fangs | custom | `entity.moray_eel.bite`: water hiss and a quick snap |
| Moray eel: hurt, death, flop | salmon | ok | unchanged (fish sounds fit an eel) |
| Crab: ambient | silverfish steps | custom | `entity.crab.ambient` now: claw clicks and froth bubbles |
| Crab: pinch | armadillo scute | custom | `entity.crab.pinch` now: claw snip |
| Crab: hurt, death, step, burrow | turtle (pitched), silverfish, sand | ok | unchanged |
| Ember crab: ambient | fizz + silverfish | custom | `entity.ember_crab.ambient` now: glowing-shell sizzle, pops, clicks |
| Ember crab: hurt, death | turtle | ok | unchanged |
| Seagull: calls, hurt, death, steal | parrot idle/hurt/death (pitched down) | custom | `entity.seagull.ambient` / `.hurt` / `.death` / `.steal` now: synthesised gull calls (long call, laughing calls, mews, squawks) |
| Seagull: flap, peck, step | parrot | ok | unchanged |
| Sea otter | axolotl, fox, turtle egg crack, pop | ok | unchanged (fits) |
| Penguin | donkey (pitched up: the "jackass penguin" bray), frog steps, snow | ok | unchanged (fits on purpose) |
| Drowned corsair | drowned + zombie (pitched down) | ok | unchanged (a drowned pirate) |
| Tideling: ambient, hurt, death | drowned (water), fish, pufferfish | custom | `entity.tideling.*` now: small bubbly gurgling voice |
| Tideling: flop, bite | fish flop, hoglin | ok | unchanged |
| **Leviathan**: ambient | elder guardian + ender dragon growls | custom | deep growling rumbles and a whale-like moan, bubbles |
| Leviathan: roar | warden roar | custom | huge wet roar: growling throat, breath, rasp, sub rumble, a throat full of sea water, room of the dome |
| Leviathan: hurt, death | elder guardian | custom | pained roars; death = falling roar that breaks into gurgles and a last moan |
| Leviathan: charge, spit | warden sonic charge, llama spit/glow squid | custom | water sucked in with a rising churn and glow; a throaty heave and a wet burst |
| Leviathan: emerge, dive | heavy splash | custom | eruption of water with a falling curtain; a heavy plunge with the gulp of displaced water |
| Leviathan: slam, flop | mace smash, guardian flop | custom | stone-shaking boom with a wet slap; heavy body slapping stone |
| Leviathan: whirlpool, bite, deflect | bubble column whirlpool, zoglin attack, shield block | custom | turning water with gurgles; giant jaw snap; glassy-metal clang on scales |
| Leviathan: burst (death) | beacon deactivate + dragon end | custom | rising swell, radiant blast, chord and bells, long shimmer |
| Brine bolt hit | splash | custom | splash with a salty hiss |

## Blocks

| Thing | Before | Needed | Done |
| --- | --- | --- | --- |
| Water purifier (lit) | furnace crackle | custom | crackle kept for the fire, plus `block.water_purifier.boil`: water boiling in the pot |
| Simple grill | campfire crackle, fire charge (fuel), item frame (food) | custom (cooking) | `block.grill.sizzle` while food lies on the hot grill; the rest kept |
| Raft furnace | furnace crackle | ok | unchanged |
| Air compressor: crank, fill, insert tank | grindstone, fizz, iron trapdoor | custom (crank, fill) | `block.air_compressor.crank` (ratchet and piston chuff), `.fill` (rising air hiss); trapdoor clank kept |
| Wind turbine | none | custom | `block.wind_turbine.spin`: seamless rotor loop (blades sweeping past the mast, wind, gearbox hum), louder and higher the faster the rotor turns; client sound per running turbine |
| Battery, solar panel, cables | none | – | silent on purpose (no moving parts) |
| Item collector | item pickup | ok | unchanged |
| Ship cannon | generic explosion | custom | `block.ship_cannon.fire`: crack, heavy boom, ringing barrel, echo over the water |
| Cannonball impact | generic explosion | ok | unchanged (it explodes) |
| Expedition radio | own static | – | kept |
| Research terminal | stone button + beacon select | custom | `block.research_terminal.use`: relay click and a short beep melody |
| Control panel | stone button | custom | `block.control_panel.beep`: click and one or two beeps |
| Server rack, pipe valve | beacon hum, grindstone | ok | unchanged |
| Tidewarden altar: insert sigil | end portal frame + amethyst chime | custom | `block.tidewarden_altar.insert`: crystal chime with a swell of water-light (pitch rises with every sigil); the frame click is kept |
| Tidewarden altar: key forged | beacon select + enchanting table + amethyst | custom (story moment) | `block.tidewarden_altar.forge`: rising voices, a radiant strike, bells and a long glow |
| Tidewarden altar: all four sigils, status | beacon activate + conduit, amethyst resonate | ok | unchanged |
| Tide Seal: open | conduit activate + beacon activate | custom (story moment) | `block.tide_seal.open`: ancient stone grinding apart, a deep chord of the Tidewardens, rushing water |
| Tide Seal: locked | amethyst resonate | custom | `block.tide_seal.locked`: dull knock on stone and a sealed hum |
| Leviathan altar: summon | elder guardian curse + warden emerge + conduit | custom (story moment) | `block.leviathan_altar.summon`: two horn blasts from the deep, the pool heaving, a distant roar |
| Leviathan altar: inspect, missing crystal | conduit ambient, beacon deactivate | ok | unchanged |
| Heart of the Tide | conduit ambient/activate | ok | unchanged (it is a conduit of the deep) |
| Hammock | none | custom | `block.hammock.creak`: ropes and wood creaking when you lie down or rest |
| Ship bell, wind chime, brazier, tiki torch, giant clam, bubble vent, chests, barrels | bell, amethyst chimes, campfire, shulker, bubble column, chest/barrel | ok | unchanged (they fit) |
| Portal | portal ambient + own teleport sound | – | kept |

## Items

| Thing | Before | Needed | Done |
| --- | --- | --- | --- |
| Harpoon gun: fire, reload, empty | crossbow (pitched), crossbow loading end, dispenser fail | custom (fire, reload) | `item.harpoon_gun.fire` (trigger clack, pneumatic burst, whoosh, twang), `.reload` (slide, latch, ratchet, air); dispenser click kept for "empty" |
| Harpoon hit | trident pierce / ground impact | ok | unchanged |
| Sea scooter | beehive buzz (pitched down) | custom | `item.sea_scooter.run`: electric motor whine, propeller churn, bubbles |
| Diving gear equip | turtle armor + iron | ok | unchanged |
| Oxygen tank: low, refill | note block bit, bubble column | custom | `item.oxygen_tank.low` (two-tone regulator alarm), `.refill` (air rush and valve) |
| Plastic hook | none | custom | `item.plastic_hook.reel`: line whirring, reel clicks, things dragged through the water |
| Paddle | boat paddle | ok | unchanged |
| Seafarer's Almanac, Expedition Journal, message in a bottle, supply barrel | book page, cartography table, bottle, barrel, wood break | ok | unchanged (they fit) |
| Tidebreaker dash | trident riptide | ok | unchanged |
| Energy staff, teleporter, trash cube, metal detector | lodestone, sniffer egg, none | ok | unchanged |

## Ambience and story

| Thing | Before | Needed | Done |
| --- | --- | --- | --- |
| The open sea around the raft | nothing (only vanilla underwater ambience when diving) | custom | `ambient.raft.sea`: 24 s seamless stereo loop of waves lapping at the raft and wind; a client handler fades it with the amount of open water at sea level around the player, the height above it and the sky light, off with the head under water |
| Seagulls in the distance | nothing | custom | `ambient.raft.gulls`: far gull calls now and then over open water by day |
| Mood in the dark (deep water, wrecks, caves) | vanilla cave sounds | custom | `ambient.raft.deep` as the dimension's mood sound: a far whale, a hull groaning, a deep rumble, debris settling |
| Story moments | vanilla mixes (see blocks) | custom | key forged, seal opened, Leviathan summoned (see blocks); the journal, letters and radio keep their sounds |

## Music

| Place | Before | Done |
| --- | --- | --- |
| Raft dimension, open sea by day (`raft_ocean_biome`, `coral_shallows`, `kelp_forest`, dimension default) | vanilla overworld tracks (cherry grove, flower forest, lush caves) | `music.raft.sea_day`: "Morning", "Clear Waters"; between pieces 2–6 minutes (vanilla 10–20) |
| Open sea at night | same as day | `music.raft.sea_night`: "With the Sea", "Decline" (the client swaps the day theme for the night theme between dusk and dawn) |
| Port Solace (the drowned city) | biome music (deep dark) | `music.raft.port_solace`: "Shores of Avalon" |
| Saltmere (stilt village) | biome music | `music.raft.saltmere`: "Port Horizon" |
| Abyssal Trench (biome, also under water) | deep dark | `music.raft.abyss`: "Shadowlands 1 - Horizon" |
| Abyssal Sanctum and Coral Temple (the Tidewarden temples) | biome music | `music.raft.sanctum`: "Night Vigil" |
| Helix Research Station | biome music | `music.raft.helix`: "Static Motion" |
| Old Lighthouse | biome music | `music.raft.lighthouse`: "On the Shore" |
| Leviathan fight | none | `music.raft.leviathan`: "Black Vortex", starts at once while the Leviathan's boss bar is shown, loops, fades out after the fight |
| Other biomes (lagoon, shelf, graveyard, floes, islands), under water | vanilla tracks | unchanged |

How it works: the sea and trench themes are data (`minecraft:audio/background_music` of the biomes and of the
dimension type). Places are structures, not biomes: the server checks once a second whether a player is inside (or up
to 16 blocks above) Port Solace, Saltmere, Helix Station, the Old Lighthouse, the sanctum or the Coral Temple and
tells the client (`raft:audio_place`).
The client's `MusicDirector` (mixin on `Minecraft.getSituationalMusic`) plays, in this order: the Leviathan's theme
while its boss bar is shown, the place theme, otherwise the attribute music with the night swap. The "Now Playing" toast
shows "Kevin MacLeod - <title>".

All music together: 17.0 MB (11 pieces, Ogg Vorbis q3; the wide pieces stereo, the quiet and drone pieces mono), the
sound effects 2.7 MB.
