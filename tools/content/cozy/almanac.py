"""Seafarer's Almanac pages of Beach Craft 3.3 (chapters Cozy Living, Island Kitchen, Gear & Gadgets).

Same markup as tools/content/guide/texts.py ('# ' heading, '* ' bullet, '---' new page). The chapters and their icons
are listed in AlmanacBook.java; each module writes its own pages into its lang file. Numbers checked against the code
of the cozy, food and gear modules (CozyComfort, FoodContent, PotRecipes, FishTrapBlockEntity, GearContent).
"""

PAGES = {
    'cozy': {
        'almanac.raft.cozy.title': 'Cozy Living',
        'almanac.raft.cozy.text': """\
Life on the Endless Sea is more than staying alive. Make your raft and your island home cozy, and it pays you back.
# Rest
Sit down on any chair, bench, stool, lounger or cushion, or lie in a hammock or bed. After 3 seconds you feel Rested.
Rested heals half a heart every 4 seconds and makes you hungry 25% slower.
# Comfort
Rested lasts 2 minutes plus 1 minute per comfort point. Resting gives 1 point, and every kind of cozy thing within 6 blocks gives 1 more:
* seats, tables, lights and fires
* rugs and towels, plants and flowers
* shade: parasols and beach umbrellas
* music: steel drum, conch, wind chime, ship bell, jukebox
* storage, wall decor and a view of the water: aquarium, blinds, lattice, sea glass
With everything around you, you stay Rested for 14 minutes. Keep sitting and it is topped up every 30 seconds.""",
        'almanac.raft.cozy.furniture.title': 'Furniture',
        'almanac.raft.cozy.furniture.text': """\
# Seats
Palm and Teak Benches join into one long bench when you place them side by side. The Bamboo Bar Stool, the Sun Lounger and the Rattan Armchair are seats too. Right-click to sit down.
# Tables and plates
Put a Serving Plate on a Teak or Bamboo Table and right-click it with food: it holds up to 4 portions (sneak to put down more at once).
Right-click with an empty hand to eat a portion when you are hungry; its effects work as usual. Sneak-click with an empty hand to take the food back.
# Storage
Palm, Teak and Weathered Shelves show up to three items. Right-click swaps the item in your hand with the spot you point at; with redstone power a shelf swaps a whole row with your hotbar.
The Wicker Basket holds 9 stacks and keeps them when you pick it up, like a shulker box.""",
        'almanac.raft.cozy.lights.title': 'Lights, Music and Decor',
        'almanac.raft.cozy.lights.text': """\
# Lights with a switch
Right-click to switch them on and off: Hurricane Lantern (light 14, stands or hangs), Sea Glass Lamp (14), Driftwood Sconce (12, on a wall) and Shell Candle (10).
# Shade and music
The Thatch Parasol is two blocks tall and gives shade. The Steel Drum plays the next note of an island tune with every right-click or redstone pulse. The Conch Shell blows a horn that carries far over the water.
# Decor
Beach Towels, Rope Rug, Potted Monstera, Coconut Planter, Flower Box, Coral Vase, Shell Garland, Fishing Net, Surfboards (two blocks tall, on a wall), Fish Trophy, Dock Bollard and Bamboo Blinds (right-click to roll them up).
# Building
Shell Tiles and Pumice Bricks with stairs, slabs and walls (also in the stonecutter), and the Driftwood Lattice for windows.""",
    },
    'food': {
        'almanac.raft.kitchen.title': 'Island Kitchen',
        'almanac.raft.kitchen.text': """\
# Island crops
Plant them on farmland or a crop plot. They have four stages and grow about as fast as wheat.
* Sweet Potato: plant the potato itself; bake it for a filling meal.
* Chili Pepper: from chili seeds. A raw chili gives 10 seconds of Fire Resistance; one chili makes 2 seeds.
* Banana Plant: from a banana shoot; you get bananas and the shoot back.
* Rice: plant rice and cook it into Cooked Rice.
* Sea Grapes: plant them on sand or gravel under water. When ripe, right-click to pick 2 or 3 bunches; they grow back.
---
# Finding seeds
Wild sweet potatoes, chilis, bananas and rice grow on the Palm, Jungle, Mangrove and Volcanic Isles. Break them for food and seeds.
Sea grapes grow in the Coral Shallows and the Glowing Lagoon.
Seeds also float by in flotsam barrels, hide in supply barrels and island chests, and in overworld shipwrecks.""",
        'almanac.raft.kitchen.pot.title': 'Cooking Pot and Fish Trap',
        'almanac.raft.kitchen.pot.text': """\
# The Cooking Pot
Set it on a campfire, fire, brazier, simple grill, magma block or lava. Right-click it with up to 4 ingredients; when they make a dish it cooks in 10 seconds. Serve it with bowls. Sneak-click with an empty hand to take the ingredients back.
* Island Curry, 3 bowls: cooked rice, chili pepper, sweet potato, coconut
* Seafood Chowder, 3 bowls: a fish, crab claw or eel fillet, sweet potato, kelp, sea grapes
* Sweet Potato Soup, 2 bowls: 2 sweet potatoes, chili pepper
---
# The Fish Trap
Put it in water with open water around it. Every 1 to 2 minutes it catches cod, salmon, tropical fish, crab claws, shells, kelp or sea grapes, now and then a pufferfish or a nautilus shell.
Fish Bait or a Golden Lure inside makes it twice as fast; it uses one per catch. Hoppers can empty it.""",
        'almanac.raft.kitchen.dishes.title': 'Dishes and Treats',
        'almanac.raft.kitchen.dishes.text': """\
Hunger in drumsticks; a full bar is 10.
* Banana 2, Cooked Rice 2, Sea Grapes 1 (quick)
* Baked Sweet Potato 3
* Sushi Roll 1.5, quick: 3 from cooked rice, raw fish and dried kelp
* Banana Bread 3.5: 2 bananas, 3 wheat, sugar
* Fruit Salad 4 and Regeneration: banana, pineapple, melon slice, bowl
* Chili Crab 4.5 and 90 s Fire Resistance: cooked crab claw, chili, bowl
* Island Curry 5 and Regeneration
* Seafood Chowder 5 and 60 s Water Breathing
* Sweet Potato Soup 4
* Coconut Ice Cream 3: coconut, snowball, sugar, bowl
* Tropical Smoothie 3 and 60 s Haste: banana, pineapple, sugar, fresh water""",
    },
    'gear': {
        'almanac.raft.gear.title': 'Gear & Gadgets',
        'almanac.raft.gear.text': """\
# Weapons
* Shark Tooth Spear: a stick and a shark tooth. Jab or charge like any spear; as strong as an iron spear, with 320 uses.
* Driftwood Boomerang: right-click to throw. It flies about 18 blocks, hits for 5, then comes back to you, even through walls. On the way it picks up to 3 stacks of floating items. 200 throws.
* Machete: 5.5 damage and quick swings. It cuts leaves, plants, bamboo, vines and crops in a blink. 280 uses.""",
        'almanac.raft.gear.armour.title': 'Armour',
        'almanac.raft.gear.armour.text': """\
# Shell Armour
Made from seashells: 13 armour points in all (between chainmail and iron) and a little toughness. Wear all four pieces for Resistance I while in water.
# Explorer's Outfit
Hat, Jacket, Trousers and Boots made from sail canvas: 7 armour points, like leather. The full outfit lets you walk 10% faster and gives Luck I for better fishing and loot.""",
        'almanac.raft.gear.tools.title': 'Tools',
        'almanac.raft.gear.tools.text': """\
# Wreck Compass
Right-click and the needle turns to the nearest shipwreck within about 1000 blocks: galleons, cargo ships, fishing boats, sunken planes and overworld shipwrecks. It also tells you how far it is. Once every 5 seconds.
# Captain's Spyglass
Zooms twice as far as a spyglass and shows the distance to what you look at.
# Bait
Fish Bait: 4 from kelp and rotten flesh, a spider eye, a crab claw or a cod. Keep it in your bag while fishing: bites come faster, like Lure I. One per cast.
Golden Lure: bait and 3 gold nuggets. Faster bites and better treasure, like Luck of the Sea II.""",
    },
}
