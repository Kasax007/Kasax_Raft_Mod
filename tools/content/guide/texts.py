"""Texts of the Seafarer's Almanac (lang keys almanac.raft.<chapter>.title / .text).

Markup inside a text (the screen reads it, see AlmanacBook.java):
  paragraphs are separated by a newline; '# ' starts a heading, '* ' a bullet point, '---' forces a new page.
Every fact here was checked against the code, lang, recipes and loot of the modules (Beach Craft 3.0 tree).
Keep pages short and friendly; numbers are fine, boss details only in the Leviathan chapter (behind its warning).
"""

# Chapter order = contents order. (id, title)
CHAPTERS = [
    ('welcome', 'Welcome Aboard'),
    ('portal', 'The Portal'),
    ('survival', 'Raft Survival'),
    ('building', 'Building'),
    ('technology', 'Technology'),
    ('sailing', 'Sailing'),
    ('diving', 'Diving'),
    ('biomes', "The Sea's Biomes"),
    ('places', 'Islands & Wrecks'),
    ('creatures', 'Creatures'),
    ('story', 'The Story'),
    ('leviathan', 'The Leviathan'),
    ('titanium', 'Titanium & Tools'),
    ('tips', 'Tips'),
]

T = {}

T['welcome'] = """\
This almanac belongs to every castaway of the Endless Sea. It is bound in oiled leather, so a little sea water will not hurt it.
# How to read it
Click a chapter on the contents page to open it. Turn the pages with the arrows at the bottom, the arrow keys or the mouse wheel. The red ribbon takes you back to the contents.
Hold the mouse over a picture to see its name. The almanac opens where you stopped reading.
# Your first days
* Build a driftwood portal in the overworld and step through.
* Fish debris out of the water and make your raft bigger.
* Purify water, grill fish and plant crops.
* Read the Expedition Journal: it always knows where to go next.
Lost your almanac? Craft a new one from a book and kelp or driftwood."""

T['portal'] = """\
The way to the sea starts in the overworld.
# Ancient treasure
Ancient Broken Rings hide in shipwreck treasure, buried treasure and ocean ruins, and sometimes they bite on your fishing line. Smelt one into Ancient Energy. Shipwreck treasure also holds Ancient Crystals.
# The Energy Staff
Ancient Energy on top, an Ancient Crystal in the middle and a stick at the bottom. It lights portals 64 times.
# The frame
Short, dark driftwood stumps stand on sandy beaches. Four driftwood logs around one Ancient Energy make 4 Driftwood Portal Frames.
Build the frame like a nether portal, with at least 2 by 3 empty blocks inside, and light it with the staff.
# Arrival
You land on a driftwood platform high above the waves, with a portal back home. On a new platform a castaway's chest is waiting for you. Open it!
# Without a portal
The Raft Dimension Teleporter (5 titanium ingots and 2 Ancient Energy) takes you to the sea and back home. It has 64 uses."""

T['survival'] = """\
Out here you need a floor under your feet, clean water and a warm meal. Almost everything comes from the sea.
# Debris
Now and then the current carries a plank, palm leaves, plastic or scrap metal past you, from west to east. Most materials come from the islands and from the chests of the places and their guards. Rarely a Message in a Bottle or a Supply Barrel floats by. Use them to open them.
# Plastic Hook
Hold use to pull floating items towards you, up to 12 blocks away. Nothing solid may be in the way: it does not pull through walls, floors or glass.
# Item Catcher
Build it into a raft of your own: it only catches while the raft sails. It picks up the debris it sails through and now and then nets a piece out of the sea, more the faster you sail. Standing still, with the anchor down or on land it catches nothing. It holds up to nine stacks of its catch. Hoppers can take the catch out.
With an Item Catcher Net inside, it also fishes up Unrefined Ocean Garbage while you sail, faster at speed. A net lasts 32 catches. Smelt the garbage into a Trash Cube and use the cube for 1 to 3 surprises.
# Foundations
Seven planks with rope in the top and bottom middle make 4 Raft Foundations. Rope comes from 2 palm leaves or 3 string. A foundation and 2 scrap metal make a Reinforced Foundation.
# Sharks!
Bruce the shark circles rafts that people stand on. Every 15 to 45 seconds he bites an edge foundation, and after 4 bites it breaks. Reinforced Foundations are never bitten. Hit him and he swims away for a while.
On a server, /gamerule raft:shark_bites false stops the biting.
# Water
The Water Purifier turns water bottles into Fresh Water; a water bucket gives three. It burns planks, logs, coal or charcoal.
# Food
The Simple Grill cooks two foods at once, 50% faster than a campfire. Use it with fuel first, then with raw food.
Pick coconuts from palm leaves; they grow back after about 20 minutes. Coconut Milk gives Regeneration, a Pina Colada gives Speed.
# Farming
Crop Plots are farmland that is always watered and never trampled. Crops grow about 50% faster on them.
Plant a Pineapple Crown on farmland or a crop plot to grow pineapples.
Seagulls peck at crops and set them back one stage, unless a Scarecrow stands within 16 blocks. A scarecrow also stops monsters from spawning within 24 blocks and keeps farmland within 8 blocks from being trampled."""

T['building'] = """\
Beach Craft has a whole harbour of new blocks. Here are the highlights; the Building and Decoration tabs show them all.
# Wood
Weathered Planks: 4 driftwood or palm planks around bone meal. Teak Planks: dark oak and palm planks. Both come with stairs, slabs, fences, gates, a door and a hatch.
# Stone
Coral Stone (stone and coral blocks) comes polished and as plain or mossy bricks. Tidestone (stone and prismarine shards) also comes cracked, chiseled and as pillars, and Glowing Tidestone shines with light 10. Most stone works in the stonecutter.
# Sea Glass
Four Sea Glass Shards make Aqua Sea Glass. Add blue or green dye for the other colours. There are panes too.
# Steel and city
Riveted and Rusty Hull, Corrugated Metal, Steel Beams, Catwalks, Railings, Portholes, Bulkhead Doors and Metal Hatches (both open by hand), pipes with valves, Shipping Containers, Cracked Concrete and Asphalt with lane lines.
# Lights
Paper Lanterns (light 14), Ship Lanterns and Jelly Lamps (15), String Lights and Neon Tubes (12), Tiki Torches, a Lighthouse Lens and Buoys that float on the water.
# Furniture
Chairs, Cushions and the Deck Chair: right-click to sit. The Hammock lets you rest by day and sleep by night, and it sets your respawn point.
Treasure Chests and Wooden Crates hold 27 stacks.
# On deck
The Ship Cannon fires Cannonballs (an iron ingot and gunpowder make 4): 8 damage and no broken blocks. Ring the Ship Bell, raise a Jolly Roger and stack Gold Coins into piles (9 coins each)."""

T['technology'] = """\
The sea has its own small tech line. It runs on FE (Team Reborn Energy), so machines of other tech mods can connect.
# Power
* Makeshift Solar Panel: 10 FE per tick in the sun, by day only.
* Wind Turbine: 20 FE per tick. It needs free air in the 3 by 3 area in front of its rotor and a spot above y 90.
* Makeshift Battery: stores 1,000,000 FE.
* Copper Cable carries 128 FE per tick, Titanium Cable 512.
# Machines
* Quarry: digs an 11 by 11 hole down to the bottom of the world for 1,000 FE per block. A chest on top collects the drops.
* Chunk Destroyer: mines its whole chunk, 100,000 FE per column with blocks in it. Empty columns are free.
* Brick Furnace: 1.5 times faster. Titanium Furnace: 32 times faster, and every result is doubled.
* Air Compressor: fills Oxygen Tanks and charges the Sea Scooter (see Diving).
Batteries, generators and machines keep their energy when you break them. The item shows how much, and the machine has it back when you place it.
# Expedition tech
Control Panels, Server Racks and Satellite Dishes blink and hum on your walls. A Circuit Board (copper, a slime ball and plastic) is in most machine recipes.
Research Terminals and Expedition Radios from the old expedition still work. Listen to them!"""

T['sailing'] = """\
# Small boats
The Palm Boat and the Driftwood Raft are made like vanilla boats, with a chest version too. In a boat, the Paddle gives you a push forward.
# The Expedition Raft
Six Raft Foundations, 2 rope and a paddle. A big 3 by 3 raft with four seats and 27 storage slots. Sneak and use it to open the storage. The first one to board sits at the helm and steers.
It never sinks, never tips over, and sharks cannot hurt it. Friends standing on the deck ride along.
To pick it up, hit it 3 times within 2 seconds, or once while sneaking. It drops as an item and keeps its upgrades; the cargo drops next to it.
# Upgrades
Use an upgrade item on the raft. Each one shows on the raft.
* Raft Sail: 50% faster sailing ahead.
* Raft Engine: more than twice as fast. It burns furnace fuel from the fuel slot in the raft menu while you drive.
* Raft Anchor: drop it with the lever in the raft menu, and the raft stays where it is.
* Raft Cargo Crate: 27 more storage slots.
* Raft Lanterns: lantern posts and rope rails.
A bare Expedition Raft is a little slower than a boat. With sail and engine it is more than twice as fast as a boat."""

T['diving'] = """\
# The gear
* Diving Helmet: see much farther under water. With a tank you breathe from the tank.
* Helmet Lamp: craft the Diving Helmet with an Angler Lure. Under water you can then see in the dark deep sea and the trench.
* Oxygen Tank: 2 minutes of air. The Titanium Oxygen Tank holds 6 minutes and protects like an iron chestplate.
* Wetsuit: mine three times faster under water, and jellyfish cannot sting you.
* Flippers: swim much faster, but walk 25% slower on land.
Wear all four for Dolphin's Grace while swimming.
# Air
A tank only works together with the helmet. The bar on the tank and a gauge on the screen show how much air is left.
The tank refills when your head is above the water, or in a bubble column: a normal tank is full again in 3 seconds. Bubble Vents and soul sand make bubble columns.
# Air Compressor
Put a tank in its slot and it fills it with FE from a neighbour: a cable, battery, solar panel or wind turbine. No power? Sneak and use it with an empty hand, or press the crank in its menu. Each turn adds 5 seconds of air.
It also charges the Sea Scooter.
# Harpoon Gun
Shoots Harpoons from your inventory: 8 damage under water, 4 in the air, one shot per second. Pick the harpoons up again after the hunt. Iron, a stick and string make 4 harpoons; an Urchin Spine, a stick and string make 2.
# Sea Scooter
Hold use under water and it pulls you where you look. Its battery holds 3,000 FE, about 75 seconds of riding."""

T['biomes'] = """\
The Endless Sea is an archipelago: open water, islands, reefs and deep trenches.
* Endless Sea: deep blue open water. Sharks, whales and jellyfish.
* Kelp Forest: tall kelp and glow kelp. Sea otters float on top.
* Coral Shallows: warm reefs with anemones, giant clams, starfish and urchins. Manta rays glide here.
* Glowing Lagoon: rare and dim, full of glowing life.
* Drowned Shelf: a flat, murky floor covered in concrete rubble.
* Abyssal Trench: the deepest and darkest water. Tube worms, bubble vents and anglerfish.
* Wreck Graveyard: foggy water full of wrecks and drowned corsairs.
* Frost Floes: icebergs, ice floes and penguins.
* Palm Isle: sandy beaches with palms, hibiscus, plumeria, morning glory, aloe, seashells, sea glass and crabs.
* Jungle Isle: jungle, bamboo, orchids, heliconias, birds of paradise, pineapples and parrots.
* Mangrove Isle: low, muddy islands with mangroves, spider lilies and frogs.
* Volcanic Isle: black sand, ash, basalt and lava, guarded by ember crabs.
# Gifts of the sea
Giant Clams open and close. Use an open clam to take its pearl; a new one grows in time.
Sea Urchins hurt to touch and drop spines. Wild Pineapple Bushes are ready when the fruit is golden.
Black Sand smelts into tinted glass. Island flowers make dyes (Hibiscus and Heliconia red, Bird of Paradise orange, Orchid magenta, Morning Glory purple, Plumeria and Spider Lily white, Aloe green) and look nice in flower pots. Seashells become bone meal or Shell Sand."""

T['places'] = """\
Away from the story you find many smaller places, with loot, logs and a bit of danger.
* Castaway Camps: tiny camps on palm islands, with SOS letters in the sand.
* Abandoned Rafts: lonely rafts floating on the sea, with a chest on board.
* Beach Shacks: little beach bars with deck chairs and tiki torches.
* Buoy Fields: channel buoys and a floating supply crate.
* Sunken Ruins: small Tidewarden ruins on the reefs.
* Fishing Boat Wrecks: small wrecks with nets and crates.
* Cargo Ship Wrecks: a rusty freighter broken in two, with containers everywhere.
* Sunken Planes: crashed planes on the sea floor.
* Rusted Rigs: giant old platforms on steel legs, with terminals and a radio.
* Iceberg Outposts: research huts on the ice of the frost floes.
* Galleon Wrecks: old pirate ships full of drowned corsairs. Their chests sometimes hold Saltjaw's Treasure Chart.
* Pirate Cove: Saltjaw's hidden hideout in an island. Follow the chart!
Use a Receiver to find the nearest floating driftwood: it shows the direction and the distance."""

T['creatures'] = """\
The sea is full of life. Most creatures are peaceful; a few bite back.
Turn the page to meet them, one page each. Their spawn eggs are in the Creatures tab."""

# One page per creature: (id, title, text). The id is the entity id (raft:<id>).
CREATURES = [
    ('shark', 'Bruce the Shark', """\
Bruce hunts anyone who swims in the raft ocean, with 13 hearts of grumpy muscle. Climb onto your raft and you are safe, but your raft is not: he circles it and bites through its edge foundations. Reinforced Foundations are safe.
He drops Raw Shark Meat and Shark Teeth. Cook the meat for a good meal; 3 planks on 3 teeth make a Shark Jaw Trophy."""),
    ('hammerhead_shark', 'Hammerhead Shark', """\
A bigger, tougher shark of the deep open sea, with 20 hearts. Like Bruce it hunts swimmers and bites rafts, but it hits harder and lives deeper.
It drops Shark Teeth and Raw Shark Meat."""),
    ('whale', 'Whale', """\
A huge, gentle giant. It cruises a few blocks below the surface, comes up every minute or two to breathe and spout, and sings now and then.
It is peaceful and drops nothing: it is a friend."""),
    ('manta_ray', 'Manta Ray', """\
Glides through the warm shallows in slow, wide loops and sometimes leaps out of the water with a splash. Peaceful.
It may drop a piece of leather."""),
    ('jellyfish', 'Jellyfish', """\
Drifts with the current and pulses its bell. Moon jellies are white, crimson ones red, and glow jellies light up the dark lagoons.
Touching one stings: Poison for 3 seconds. A Wetsuit protects you.
It drops Jelly Blobs (2 make a slime ball). Glow jellies drop Glow Jelly for glow ink sacs and Jelly Lamps."""),
    ('anglerfish', 'Anglerfish', """\
Hides in the dark of the Abyssal Trench with its glowing lure. It creeps closer, then darts forward to bite.
It drops an Angler Lure and sometimes cod. Four prismarine shards around a lure make a sea lantern."""),
    ('moray_eel', 'Moray Eel', """\
Hides in holes of reefs and ruins with only its head out. Swim within 4 blocks and it lunges at you, then pulls back into its hole.
It drops Eel Fillets. Cooked, they fill 3 drumsticks."""),
    ('crab', 'Crab', """\
Scuttles sideways along the beach. Sprint close and it digs itself into the sand; hit it and it pinches back. Red, blue and sand-coloured crabs breed with kelp.
It drops Crab Claws, and sometimes a seashell. A Cooked Crab Claw fills 3 drumsticks."""),
    ('ember_crab', 'Ember Crab', """\
A crab of the volcanic isles, with a basalt shell and glowing cracks. Keep your distance: up close it attacks and sets you on fire. Fire and lava do not bother it.
It drops Crab Claws and warm Ember Shells."""),
    ('seagull', 'Seagull', """\
Flies in flocks over islands and rafts and rests on the water. It pecks at crops unless a Scarecrow stands within 16 blocks, and it snatches food that lies on the ground!
It drops feathers."""),
    ('penguin', 'Penguin', """\
Waddles over the frost floes, slides on its belly and dives for fish. Feed penguins cod and they have chicks: grey and fluffy.
It drops feathers."""),
    ('sea_otter', 'Sea Otter', """\
Floats on its back in the kelp forests and cracks shells on its belly with a stone.
Feed it a seashell and it follows you for a while. Now and then it brings you a pearl or a seashell from its dives."""),
    ('drowned_corsair', 'Drowned Corsair', """\
A cursed undead pirate of Saltjaw's crew. It swims like a drowned, does not burn in the sun, fights with a cutlass and even climbs onto rafts. It haunts wreck graveyards, galleon wrecks and the pirate cove.
It drops gold coins, bones and sometimes its Cutlass."""),
]

T['story'] = """\
Long ago an expedition came to the Endless Sea. Then the sea rose. Their logs and letters are still out there, and they show you the way.
# The Expedition Journal
It waits in the castaway chest, or craft one from a book, a compass and paper. Use it to read your goal and a hint. It follows your progress by itself.
# Charts
Story chests hold a chart to the next place. Lost it? Sneak and use the journal at sea to draw a fresh chart to your goal, once every 5 minutes.
# Clues
Read the books in the chests, the Research Terminals and the Expedition Radios. Messages in Bottles drift on the sea with letters from the survivors.
# Precious things
Some story items are too important to lose: when dropped, they never despawn.
The story leads you deep below the waves. Build a good raft, and wear a Diving Helmet and a tank before you dive deep."""

T['leviathan.warning'] = """\
Spoiler warning!
This chapter tells you everything about the final fight. It opens by itself once the Leviathan is defeated.
Turn the page only if you want to know it now."""

T['leviathan'] = """\
A colossal sea serpent with 600 health and thick scales. It sleeps in the pool of the Abyssal Sanctum. Use the Tidewarden Key on the Leviathan Altar at the pool's edge to wake it, and fight on the stone ring.
# How it fights
* Circling deep in the pool, it cannot be hurt.
* It rises and spits brine bolts: 5 damage and Slowness.
* It leaps across the ring and slams down: 8 damage around it. Then it lies stranded for 4 seconds and takes 50% more damage. Strike now!
* Below half health it roars, pushes you back and calls Tidelings, and it stirs a whirlpool that pulls you to the pool.
* Below a quarter it is faster and chains its attacks.
# Rewards
* Heart of the Tide: place it, and every swimmer within 32 blocks gets Conduit Power and Dolphin's Grace.
* Leviathan Scales, a Leviathan Trophy and 500 experience.
* Tidebreaker: 4 scales, a titanium ingot and a trident. 9 damage, +50% against sea creatures. Hold use in water or rain to surge ahead.
To fight again, bring the key and an Ancient Crystal to the altar."""

T['leviathan.tidelings'] = """\
Small fish-like minions of the Leviathan with 5 hearts. They bite for 3 damage and hop after you on land for 10 seconds before they flop about helplessly.
One is easy. Several at once are dangerous: keep moving!"""

T['titanium'] = """\
# Titanium
Titanium Ore is only found in the raft dimension, below y 20 under the sea floor. Mine it with a diamond pickaxe and smelt the raw titanium. Nine nuggets make an ingot, nine ingots a block.
# Titanium Armour
A full set gives Water Breathing, Dolphin's Grace and Conduit Power.
# Finders
* Metal Detector: use it on the ground to learn at which height the first titanium ore below lies.
* Receiver: direction and distance to the nearest floating driftwood.
# Weapons
* Cutlass: iron and teak planks. 6 damage, and faster than an iron sword.
* Harpoon Gun: see Diving.
# Handy things
* Paddle: a push forward in a boat.
* Plastic Hook: pulls in debris.
* Energy Staff and Raft Dimension Teleporter: see The Portal.
* Expedition Journal: see The Story."""

T['tips'] = """\
* Put Reinforced Foundations on the edges of your raft to keep sharks from biting.
* Nights over the Endless Sea are dark, and so are deep water, caves and rooms. Bring lights.
* Glow kelp (light 8), glowing anemones (10) and tube worms (6) light the deep.
* Rest in a Hammock on your raft: it sets your respawn point.
* A Scarecrow keeps seagulls away from your crops.
* Short of air? Swim up, or find a bubble column.
* A Wetsuit keeps jellyfish stings away.
* Feed seashells to a Sea Otter for pearls.
* Lost? Sneak and use your Expedition Journal at sea for a chart.
* A Supply Barrel drifting by is always worth the swim."""

UI = {
    'item.raft.seafarers_almanac': "Seafarer's Almanac",
    'tooltip.raft.seafarers_almanac': 'Everything about the Endless Sea',
    'tooltip.raft.seafarers_almanac.1': 'Use: open the guide',
    'gui.raft.almanac.title': "Seafarer's Almanac",
    'gui.raft.almanac.subtitle': 'A guide to the Endless Sea',
    'gui.raft.almanac.contents': 'Contents',
    'gui.raft.almanac.hint': 'Click a chapter to open it.',
    'gui.raft.almanac.previous': 'Previous page',
    'gui.raft.almanac.next': 'Next page',
    'gui.raft.almanac.blank': 'Notes',
    'message.raft.almanac.given': "A Seafarer's Almanac is in your bag. Read it to learn about the Endless Sea!",
}


def lang():
    d = dict(UI)
    for cid, title in CHAPTERS:
        d[f'almanac.raft.{cid}.title'] = title
        d[f'almanac.raft.{cid}.text'] = T[cid]
    d['almanac.raft.leviathan.warning.title'] = 'The Leviathan'
    d['almanac.raft.leviathan.warning.text'] = T['leviathan.warning']
    d['almanac.raft.leviathan.tidelings.title'] = 'Tidelings'
    d['almanac.raft.leviathan.tidelings.text'] = T['leviathan.tidelings']
    for cid, title, text in CREATURES:
        d[f'almanac.raft.creatures.{cid}.title'] = title
        d[f'almanac.raft.creatures.{cid}.text'] = text
    return d
