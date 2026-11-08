## Beach Craft (Raft) 3.3.0 for Minecraft 1.21.1

**This is the Minecraft 1.21.1 version of Beach Craft 3.3.0** (Fabric Loader 0.16+, Fabric API 0.116.x for 1.21.1). It has the same content as Beach Craft 3.3.0 for Minecraft 26.3: the cozy furniture and Rested, the island kitchen and the new gear listed below, together with everything from 3.2.0. A few things work the 1.21.1 way because Minecraft 1.21.1 does not have them; they are listed under "Differences on Minecraft 1.21.1".

When you create or open a world, Minecraft 1.21.1 asks you to confirm "experimental settings": it does that for every mod that brings its own dimension. Choose Proceed (or "I know what I'm doing!") and play.

Worlds of the 26.3 version cannot be opened in 1.21.1 (Minecraft never loads worlds of a newer version). Worlds from 3.2.0 for 1.21.1 load as they are.

### Differences on Minecraft 1.21.1
- **Shelves**: Minecraft 1.21.1 has no shelves, so the Palm, Teak and Weathered Shelves are Beach Craft's own block with the same look. They show three items on the front: right-click with an item puts it on the spot you point at (swapping it with what stands there), right-click with an empty hand takes it. With redstone power, up to three shelves side by side join into a row and right-click swaps the whole row with your hotbar, like the shelves of newer Minecraft. Hoppers can fill and empty them and a comparator reads which spots are filled. They use the chiseled bookshelf sounds.
- **Shark Tooth Spear**: Minecraft 1.21.1 has no spears, so the Shark Tooth Spear is a weapon of its own with the same strength and the same 320 uses:
  - **Jab** (attack): 3 damage, about one stab a second, and it reaches 4.5 blocks (a sword reaches 3).
  - **Charge** (hold right-click): you raise the spear like a trident. After 0.6 seconds it is charged; run, fall or ride into a mob at speed and it is struck for 3 damage plus a bonus for your speed (knocked back when you are fast, knocked off its mount when you are very fast). Each mob is struck once per charge.
  - **Thrust** (let go of a charged spear): you lunge a step forward and hit the first mob within 4.5 blocks for 3 damage, up to 6 when you held the charge for 1.5 seconds, with knockback.
  - The tooltip and the Almanac describe it this way.
- **Hurricane Lantern and Driftwood Sconce**: crafted with iron nuggets instead of copper nuggets (Minecraft 1.21.1 has no copper nuggets).
- Behind the scenes some item models work the 1.21.1 way (the Wreck Compass needle, the Captain's Spyglass and the Shark Tooth Spear in the hand use 1.21.1 item properties); in the game they look and play the same.

### Fixes for Minecraft 1.21.1
- **Story chests**: the chests of the story places (castaway, Old Lighthouse, Saltmere, Helix Station, Port Solace, Ashen Forge, Coral Temple) and the galleon wreck were empty in 3.2.0 for 1.21.1: their loot tables did not load because of the map they give out. They are filled again, map included.
- **Crop Plot**: seeds can be planted on it again and it speeds up the crop like farmland, as in the 26.3 version.
- **Signs** in the Old Lighthouse and Saltmere show their texts again.
- **Cracked Concrete** can be crafted again (its recipe needed a concrete item tag that Minecraft 1.21.1 does not have).
- Composting chances are the same as in the 26.3 version (the pineapple fills the composter more often).

## Beach Craft 3.3.0

Make yourself at home on the Endless Sea. 3.3 brings cozy furniture and lights you can switch, a reason to decorate (resting in a cozy place makes you Rested), island crops and a whole kitchen of dishes, and new gear: a boomerang that fetches floating loot, a machete, a shark tooth spear, two armour sets, a compass that finds wrecks and a captain's spyglass.

### Cozy living
- **Rest and comfort**: sit on any chair, bench, stool, lounger or cushion, or lie in a hammock or bed. After 3 seconds you are **Rested**: you heal half a heart every 4 seconds and get hungry 25% slower.
- **Comfort decides how long**: 2 minutes plus 1 minute per comfort point. Resting gives 1 point, and every kind of cozy thing within 6 blocks adds one: seats, tables, lights, fires, rugs, plants, shade, music, storage, wall decor and a view of the water. With everything around you, Rested lasts 14 minutes. The old decorations count too, so the paper lanterns, string lights, hammocks and beach umbrellas you already built now pay off.
- **Seats**: Palm and Teak Benches (they join into one long bench side by side), Bamboo Bar Stool, Sun Lounger and Rattan Armchair.
- **Tables and plates**: Teak and Bamboo Tables. Put a **Serving Plate** on a table and serve up to 4 portions of one food on it, one per right-click. Right-click with an empty hand to eat one when you are hungry (with all its effects), sneak-click to take it back.
- **Storage**: Palm, Teak and Weathered **Shelves** show three items (on 1.21.1 Beach Craft's own shelf, see above). The **Wicker Basket** holds 9 stacks and keeps them when you pick it up, like a shulker box.
- **Lights with a switch**: Hurricane Lantern (stands or hangs, light 14), Sea Glass Lamp (14), Driftwood Sconce (12, on walls) and Shell Candle (10). Right-click to switch them on and off.
- **Music**: the **Steel Drum** plays the next note of an island tune with every right-click or redstone pulse, and the **Conch Shell** blows a horn that carries far over the water. Both have their own sounds.
- **Shade**: the two-block **Thatch Parasol**.
- **Decor**: Red, Blue and Yellow Beach Towels, Rope Rug, Potted Monstera, Coconut Planter, Driftwood Flower Box, Coral Vase, Shell Garland, Fishing Net, three Surfboards (two blocks tall, on a wall), Fish Trophy, Dock Bollard and **Bamboo Blinds** (right-click to roll them up).
- **Building blocks**: Shell Tiles and Pumice Bricks with stairs, slabs and walls (also in the stonecutter) and the Driftwood Lattice for windows.

### The island kitchen
- **Four new crops** for farmland and the crop plot, each with four stages: **Sweet Potatoes**, **Chili Peppers** (from chili seeds; a raw chili gives 10 seconds of Fire Resistance), **Bananas** (from a banana shoot) and **Rice**.
- **Sea Grapes** grow under water: plant them on sand or gravel in the sea. When ripe, right-click to pick 2 or 3 bunches; they grow back.
- **Where to find them**: wild sweet potatoes, chilis, bananas and rice grow on the Palm, Jungle, Mangrove and Volcanic Isles (in newly explored chunks), sea grapes in the Coral Shallows and the Glowing Lagoon. Seeds also drift by in flotsam barrels and wait in supply barrels, island chests and overworld shipwrecks.
- **Cooking Pot**: set it on a campfire, fire, brazier, simple grill, magma or lava, add up to 4 ingredients and it cooks in 10 seconds. Serve with bowls. Island Curry (cooked rice, chili, sweet potato, coconut; 3 bowls), Seafood Chowder (a fish, crab claw or eel fillet, sweet potato, kelp, sea grapes; 3 bowls) and Sweet Potato Soup (2 sweet potatoes, chili; 2 bowls).
- **Fish Trap**: put it in open water and every 1 to 2 minutes it catches cod, salmon, tropical fish, crab claws, shells, kelp or sea grapes, now and then a pufferfish or a nautilus shell. Bait inside makes it twice as fast. Hoppers can empty it.
- **Dishes, drinks and treats** (hunger in drumsticks): Banana 2, Baked Sweet Potato 3, Cooked Rice 2, Sea Grapes 1, Sushi Roll 1.5 (quick to eat), Banana Bread 3.5, Fruit Salad 4 with Regeneration, Chili Crab 4.5 with 90 s Fire Resistance, Island Curry 5 with Regeneration, Seafood Chowder 5 with 60 s Water Breathing, Sweet Potato Soup 4, Coconut Ice Cream 3 and the Tropical Smoothie 3 with 60 s Haste.

### Gear and gadgets
- **Shark Tooth Spear**: a spear with a shark tooth tip, with 320 uses: long jabs, a charge and a lunging thrust (see above).
- **Driftwood Boomerang**: throw it and it flies about 18 blocks, hits for 5 and comes back to you, even through walls. On the way it picks up to 3 stacks of floating items, so it fetches debris from the water. 200 throws.
- **Machete**: 5.5 damage with quick swings, and it cuts leaves, plants, bamboo, vines and crops in a blink. 280 uses.
- **Shell Armour** (from seashells): 13 armour points, between chainmail and iron, with a little toughness. The full set gives Resistance I while you are in water.
- **Explorer's Outfit** (hat, jacket, trousers and boots from sail canvas): 7 armour points like leather; the full outfit makes you 10% faster and gives Luck I for better fishing and loot.
- **Wreck Compass**: right-click and the needle turns to the nearest shipwreck within about 1000 blocks (galleons, cargo ships, fishing boats, sunken planes and overworld shipwrecks) and tells you how far it is.
- **Captain's Spyglass**: zooms twice as far as a spyglass and shows the distance to what you look at.
- **Fish Bait** and the **Golden Lure**: keep them with your fishing rod for faster bites (like Lure I); the golden lure also brings better treasure (like Luck of the Sea II). One per cast.

### Seafarer's Almanac
- Three new chapters with pictures: **Cozy Living** (comfort, furniture, lights and music), **Island Kitchen** (crops, the cooking pot, the fish trap and every dish) and **Gear & Gadgets** (weapons, armour and tools). The contents page now fits up to 17 chapters.

### Creative tabs
- Three new tabs: **Beach Craft: Cozy**, **Beach Craft: Island Kitchen** and **Beach Craft: Gear**.

### Fixes
- **Seafarer's Almanac**: the survival chapter now tells how debris and the Item Catcher work since 3.2 (the catcher only catches while a raft you built sails).

Worlds from 3.2 load as they are. The wild crops grow in newly explored chunks.
