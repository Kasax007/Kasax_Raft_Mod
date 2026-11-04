"""The words of the people of the sea (lang keys npc.raft.*, entity names, almanac pages, advancements).

Rules: short lines (the conversation panel shows five lines at a time, ~45 characters each; longer texts get a
second page), friendly, a little melancholic, hopeful. Every person points somewhere: the next story step, a side
place, another person. Characters and places are those of docs/design/beachcraft3.md, section 3.
Greeting buckets must match NpcCharacter.java (stages) and NpcDialogue.java (first, first_late, regift).
"""

# id -> (name, title)
PEOPLE = {
    'keeper_fenn': ('Keeper Tobias Fenn', 'Keeper of the Old Light'),
    'nalu': ('Nalu', 'Grandson of Nana Kalea'),
    'old_tamo': ('Old Tamo', 'Fisherman of Saltmere'),
    'dr_voss': ('Dr. Imara Voss', 'Chief Scientist of the Expedition'),
    'teo': ('Teo', 'Barkeeper of the Beach Bar'),
    'kai': ('Kai', 'Castaway'),
    'dr_lund': ('Dr. Elin Lund', 'Polar Researcher'),
    'okafor': ('Patience Okafor', 'Engineer of Rig Seven'),
}

# id -> {key: text}; keys: greet.<bucket>, place, rumor.1..3, hurt
LINES = {
    'keeper_fenn': {
        'greet.first': "Well now. The portal spat out another one, did it? Welcome to the Endless Sea, castaway. "
                       "I'm Tobias Fenn, and this light is mine to keep.\n"
                       "Here, take my old spyglass. Out here you want to see the fins before they see you.",
        'greet.at': "You'll want Saltmere next, the stilt village where the reef glows. Ask for Nana Kalea. "
                    "My logbook and a chart are in the desk by the window. Take them, I know the way by heart.",
        'greet.visited': "So you've seen Saltmere. Empty, I suppose. Kalea and her people sailed for higher rocks. "
                         "Did you read her letters? That woman never wrote a word without a reason.",
        'greet.after': "Back again? Good. Every time a sail shows up on the horizon I light the lamp a little "
                       "brighter. Kalea trusted you with her sigil, I hear. Seventy years, and that woman never "
                       "trusted anybody.",
        'greet.done': "The sea's gone quiet as a millpond. Forty years I've kept this light and never once saw it "
                      "like this. Thank you, keeper. The lamp burns for you now.",
        'place': "This light has burned every night for a hundred years. When the oil runs low I burn driftwood. "
                 "After the flood a ferry brought forty souls to this rock. They moved on to the high islands. "
                 "I stayed. Somebody has to keep the light.",
        'rumor.1': "A lad called Kai built a camp on a palm isle and spelled SOS in stones. Never had the heart to "
                   "tell him nobody's coming. Except you, maybe.",
        'rumor.2': "There's an old Meridian rig out on the open sea, standing on four steel legs. Its radio still "
                   "crackles at night. Sometimes it sounds like singing.",
        'rumor.3': "Pirate gold? Hah. Saltjaw Brannigan lost the Grinning Gull on the reef of wrecks. They say his "
                   "crew still guards his treasure in a cove. They say a lot of things.",
        'hurt': "Easy now! These old bones have weathered worse storms than you.",
    },
    'nalu': {
        'greet.first': "A stranger with salt in their hair and a chart in their hand. Grandmother said you would "
                       "come. I'm Nalu. She left her letters for you inside the house.\n"
                       "Take this too. Grandmother brews it from pufferfish and moonlight, for the deep water "
                       "you will swim through.",
        'greet.at': "The Coral Sigil rests in our shrine, behind the statue of the Tidewarden. Grandmother wants it "
                    "to go home. Read her letters first. She explains it better than I ever could.",
        'greet.after': "The sigil is in good hands. Now dive to Helix, Doctor Voss's station on the floor below the "
                       "glowing lagoon. Wear Grandmother's helmet, and come back to tell me what you find.",
        'greet.later': "The reef sings louder at night since you passed through. The old songs say the tide knows "
                       "the ones who help it. I think it knows you.",
        'greet.done': "The sea is calm, and Grandmother is coming home. She says the songs have a new verse now. "
                      "It is about you.",
        'place': "Saltmere stands on stilts because the sea gives and the sea takes. When the water rose we "
                 "sailed for higher rocks. I came back to keep the shrine lamps lit. And to trade a little: pearls "
                 "for the things our people made.",
        'rumor.1': "Old Tamo is down at the docks. Bring him fish and he pays in coin. Bring him gossip and he "
                   "talks your ears off.",
        'rumor.2': "Far to the north the sea freezes into floating islands. A doctor of the Expedition lives out "
                   "there with the penguins. She buys strange things from the sea.",
        'rumor.3': "Some nights a light glows on the fire island where the old forge stands. Nobody should be in "
                   "there. Grandmother says the old ones made the sigils in that fire.",
        'hurt': "Hey! I'm on your side, you know.",
    },
    'old_tamo': {
        'greet.first': "Ahoy there! Mind the loose plank. I'm Tamo. Sixty years I've fished these waters and never "
                       "seen the sea this hungry.\n"
                       "Here, a bit of supper. And bring me your catch: I pay in good pirate gold.",
        'greet.default': "Fish are biting today. Or something is biting them. Either way, I'm buying!",
        'greet.done': "Calm water, full nets! You did that? Then the first fish of the season is yours, friend. "
                      "Well. For a fair price.",
        'place': "My granddaughter Mia writes letters to the sea and asks for my old boat back. Ha! The sea can "
                 "keep it. I built a new one, see? She floats. Mostly.",
        'rumor.1': "Watch out for the shark with the notched fin. Bruce, they call him. He bites rafts. Hit him on "
                   "the nose and he thinks twice.",
        'rumor.2': "Down on the drowned shelf there's a whole city under the water, Port Solace. I sold fish at "
                   "its harbour every Sunday. Now the fish live in the shops.",
        'rumor.3': "A fellow called Teo runs a beach bar on the palm isles. Pours a fine pina colada and waits for "
                   "his sweetheart, every single sunset.",
        'hurt': "Oi! Save your fists for the sharks.",
    },
    'dr_voss': {
        'greet.first': "Stay back, I... oh. You're not crew. You're the one from Kalea's songs. I'm Imara Voss. "
                       "Yes, that Voss. I cracked the Tide Seal.\n"
                       "The Ember Sigil lies in the vault beyond the river of fire. Drink this before you cross. "
                       "The lava does not forgive a careless step.",
        'greet.first_late': "Stay back, I... oh. You're not crew. You're the one from Kalea's songs. I'm Imara "
                            "Voss. Yes, that Voss. I cracked the Tide Seal. And you are cleaning up after me. "
                            "Thank you.",
        'greet.regift': "You lost the potion? The lava takes everything it touches. Here, another one. Please be "
                        "careful this time. I can't lose anybody else.",
        'greet.at': "The Ember Sigil waits in the vault beyond the river of fire. Drink the potion before you "
                    "cross, and don't look down.",
        'greet.after': "Four sigils: Coral, Pearl, Storm and Ember. Take them to the Coral Temple on the bright "
                       "reef floor and set them into its altar. The key will rise from their light.",
        'greet.key': "You forged the key. The Sanctum waits in the trench, and the Sleeper waits inside. Whatever "
                     "happens down there: thank you for finishing what I broke.",
        'greet.done': "It's over. The seal is quiet and the sea sleeps. I don't deserve to feel this relieved. "
                      "But I do. Thank you.",
        'place': "The Tidewardens forged their sigils here, in the heat of the mountain. Their tablets explain "
                 "everything I got wrong. I came to learn how to undo it. The forge lights are mine, if you were "
                 "wondering.",
        'rumor.1': "Port Solace... I went to warn them and came too late. Mayor Crane stayed in his tower to the "
                   "end and kept the Storm Sigil above the water line. A clever man. A proud man.",
        'rumor.2': "Captain Marlow leaves notes for everyone who comes through the portals. She told me not to "
                   "drill. Everybody should listen to her. I wish I had.",
        'rumor.3': "If you see the Leviathan's back break the water, row. It isn't angry. It is just very, very "
                   "big.",
        'hurt': "I suppose I deserve that. Please don't do it again.",
    },
    'teo': {
        'greet.first': "Welcome to the last bar on the Endless Sea! I'm Teo. The view is free, the music is the "
                       "waves, and the first drink is on the house.",
        'greet.default': "Pull up a chair! Coconut milk, pina colada, fresh water for the serious ones. Grilled "
                         "claws if you're hungry.",
        'greet.solace': "You've been to Port Solace? Did you see a bakery on Harbour Street, blue shutters? No. "
                        "Of course not. It's under thirty metres of water. Never mind me.",
        'greet.done': "Calm seas! You know what that means? Boats. Boats mean guests. And maybe one day a boat "
                      "with Ana on it.",
        'place': "Before the flood I mixed drinks in Port Solace. Now I mix them here, on a beach, for crabs and "
                 "seagulls. The crabs tip better. Bring me pineapples, coconuts or a good bottle of rum and I "
                 "pay in coin.",
        'rumor.1': "Ana was on the evacuation ferry to Saltmere. Or the one to the lighthouse isle. Nobody kept "
                   "lists that night. So I wait here, where every boat can see my lanterns.",
        'rumor.2': "Seagulls steal anything that isn't nailed down. Put a scarecrow near your crops, trust me.",
        'rumor.3': "A pirate came by once, soaking wet and grinning. He paid in gold coins as cold as ice and said "
                   "his captain's treasure lies in a cove where the sea goes in and nothing comes out.",
        'hurt': "Whoa! No bar fights. House rules.",
    },
    'kai': {
        'greet.first': "A person! A real person! Hi! I'm Kai! Did you see my SOS? Did you bring biscuits? Please "
                       "say you brought biscuits.",
        'greet.default': "Still here! Still waiting for the rescue ship. Any day now. Got any biscuits?",
        'greet.done': "The sea's so calm I can see the fish laughing at me. Hey, can I come with you next time? "
                      "Just kidding. Unless...?",
        'place': "Day forty-something on this island. A seagull stole my last biscuit. I named him Captain "
                 "Crumbs. We're not on speaking terms. Everything that washes up here, I trade for biscuits.",
        'rumor.1': "Bottles wash up here all the time, with letters inside. Sad ones, funny ones. I trade them "
                   "for biscuits. Everybody needs biscuits.",
        'rumor.2': "There are abandoned rafts drifting out there. Some still have crops on them. And chests! "
                   "Don't ask me how I know.",
        'rumor.3': "If you see a light on a rocky island at night, that's the old lighthouse. The keeper waves "
                   "when he sees you. Well. He waved at me once.",
        'hurt': "Ow! Rude! I'm already stranded, you know.",
    },
    'dr_lund': {
        'greet.first': "Visitors! Close the hatch, you're letting the cold in. Elin Lund, Meridian Expedition, "
                       "weather and wildlife.\n"
                       "Here, have some tea. Honey helps against the cold.",
        'greet.default': "The kettle is always on. And if you bring me specimens from the sea, I pay well. "
                         "Science doesn't stop for a little flood.",
        'greet.done': "The currents have changed: warmer, calmer. The penguins are confused and a little "
                      "offended. So am I. Thank you, though.",
        'place': "This outpost measures the sea: temperature, salt, currents. Since the seal cracked every chart "
                 "I draw looks wrong. Something huge breathes in the trench, and the whole ocean breathes with "
                 "it.",
        'rumor.1': "The penguins here are very angry little birds. Don't let them see your fish unless you plan "
                   "to share.",
        'rumor.2': "Jellyfish, angler lures, shark teeth: bring them to me and I pay in coin. I am writing the "
                   "first proper guide to the creatures of the Endless Sea.",
        'rumor.3': "Whales sing under the ice at night, long low songs. Sometimes something answers from very far "
                   "below. That one I don't record.",
        'hurt': "Ow! I have frostbite in three toes, I don't need bruises too.",
    },
    'okafor': {
        'greet.first': "Hello up there! You climbed all the way up? Nice. Patience Okafor, chief engineer of Rig "
                       "Seven and the only crew left.\n"
                       "Here, a spare circuit board. Everybody needs one eventually.",
        'greet.default': "The generators hum, the radio crackles and the crane still turns. Need parts? My store "
                         "room is full of them. I buy good metal, too.",
        'greet.done': "The hum is gone. Do you hear that? Nothing! I've never heard nothing before. It's lovely.",
        'place': "Rig Seven drilled for crystal on orders from Port Solace. The deeper we went, the louder the hum "
                 "got. Then the sea rose and the crew left on the last boat. I stayed. Somebody has to keep the "
                 "radio on.",
        'rumor.1': "The radio picks up strange things at night. Static that sounds like singing. My log says "
                   "don't answer it. So I don't. Mostly.",
        'rumor.2': "Port Solace ran its whole grid on crystal power, and the sigils made the crystals sing. "
                   "Brilliant engineering. Terrible idea.",
        'rumor.3': "A plane went down over the kelp forest, Flight Twelve. The crew made it, the cargo didn't: "
                   "medicine, tools, tinned peaches. Somebody should go and look.",
        'hurt': "Hey! I'm holding a live wire here!",
    },
}

# the shared answers to "What should I do next?" (one per story chapter, NpcStory.CHAPTERS)
HINTS = {
    'lighthouse': "Your journal knows the way. Follow its chart to the old light on the rocky isle. They say "
                  "Keeper Fenn still lives there.",
    'saltmere': "Sail for Saltmere, the stilt village where the reef glows at night. The keeper's chart shows the "
                "way.",
    'coral_sigil': "The Coral Sigil sleeps in Saltmere's shrine, behind the statue of the Tidewarden.",
    'helix': "Dive to Helix Station on the sea floor below the glowing lagoon. Take a diving helmet and "
             "something to breathe.",
    'pearl_sigil': "Doctor Voss locked the Pearl Sigil in the lab vault of Helix Station. Her terminals tell you "
                   "more.",
    'port_solace': "Port Solace sleeps on the drowned shelf. Look for broken towers breaking the waves.",
    'storm_sigil': "Climb City Hall, the tallest tower of Port Solace. The Storm Sigil waits in the vault near the "
                   "top.",
    'ashen_forge': "Find the Ashen Forge on the fire island. The Tidewarden tablets were taken there.",
    'ember_sigil': "The Ember Sigil lies beyond the river of fire, in the vault behind the forge. Don't cross "
                   "without protection from the heat.",
    'coral_temple': "Seek the Coral Temple on the bright reef floor. Its altar remembers the four lights.",
    'tidewarden_key': "Set all four sigils into the Tidewarden altar of the Coral Temple. The key will rise from "
                      "their light.",
    'abyssal_sanctum': "Dive into the trench and show the Tide Seal your key. The Abyssal Sanctum lies beyond.",
    'leviathan': "Wake the Leviathan at the altar by the pool and face it on the ring of stone. The whole sea is "
                 "counting on you.",
    'done': "The sea is calm now. Sail wherever you like, keeper of the tides.",
}

UI = {
    'npc.raft.ask.place': 'Tell me about this place.',
    'npc.raft.ask.next': 'What should I do next?',
    'npc.raft.ask.rumor': 'Heard any rumours?',
    'npc.raft.ask.trade': "Let's trade.",
    'npc.raft.ask.bye': 'Goodbye.',
    'npc.raft.received': 'You received: %s',
    'npc.raft.says': '<%s> %s',
    'npc.raft.busy': '%s is busy with another customer.',
    'npc.raft.home_set': "%s will stay here now.",
}

ADVANCEMENTS = {
    'new_friend': ('A Friendly Face', 'Talk to one of the people who still live on the Endless Sea'),
    'every_soul': ('Every Soul Aboard', 'Meet all eight people of the Endless Sea'),
}

# Almanac: an introduction (a text section of the "Islands & Wrecks" chapter) and one portrait page per person
ALMANAC_INTRO = """\
Not everyone left when the sea rose. A few brave or stubborn souls stayed in the old places, and they are glad of every visitor.
Right-click a person to talk. What they say changes as your story goes on, and the story's guides hand you something useful when you first meet them.
# Trading
Some of them trade. The coin of the sea is pirate gold from corsairs, wrecks and treasure, and pearls from giant clams. Many of them also buy what you catch or find.
# They stay
The people of the sea never leave their homes and nothing out here can hurt them. Be kind anyway."""

ALMANAC = {
    'keeper_fenn': "Keeper Tobias Fenn has kept the Old Lighthouse burning for forty years, flood or no flood. He "
                   "knows the way to Saltmere and gives every newcomer his old spyglass.\n"
                   "# Trades\nHe buys cod and salmon and sells candles, lanterns, glowstone and spyglasses.",
    'nalu': "Nalu, the grandson of Nana Kalea, came back to Saltmere to keep the shrine lamps lit. He knows "
            "where the Coral Sigil rests and gives castaways a potion for the deep water ahead.\n"
            "# Trades\nPearls for the crafts of Saltmere: lanterns, rugs, wind chimes and shell lamps. He turns "
            "seashells into pearls, too.",
    'old_tamo': "Old Tamo fishes from the docks of Saltmere. He never misses a bite, never misses a chance to "
                "talk and always pays for a good catch.\n"
                "# Trades\nHe buys fish, crab claws and eel and sells fishing rods, sea pickles, glow kelp, boats "
                "and the odd nautilus shell.",
    'dr_voss': "Dr. Imara Voss, chief scientist of the Meridian Expedition, studies the Tidewarden tablets in the "
               "Ashen Forge. She knows how to cross the river of fire, and what to do with the sigils.",
    'teo': "Teo runs the beach bar on the palm isles and waits for his sweetheart Ana, who was lost on the night "
           "of the flood.\n"
           "# Trades\nDrinks and island food. He buys pineapples, coconuts and good rum.",
    'kai': "Kai is a cheerful castaway who lives in a tent camp on a palm isle and waits for a rescue ship.\n"
           "# Trades\nEverything that washes up on his beach, for cookies. He would also love a fishing net.",
    'dr_lund': "Dr. Elin Lund studies the weather and wildlife of the Endless Sea from her outpost on the ice "
               "floes of the far north.\n"
               "# Trades\nShe buys jelly, angler lures, shark teeth and leviathan scales and sells diving gear "
               "and harpoons.",
    'okafor': "Patience Okafor is the last engineer of Rig Seven, the rusted giant on the open sea. She keeps its "
              "generators humming and its radio on.\n"
              "# Trades\nCircuit boards, cables, scrap, redstone, titanium and batteries. She buys copper, iron "
              "and plastic.",
}


def lang():
    out = {}
    for pid, (name, title) in PEOPLE.items():
        out[f'entity.raft.{pid}'] = name
        out[f'item.raft.{pid}_spawn_egg'] = f'{name} Spawn Egg'
        out[f'npc.raft.{pid}.title'] = title
        for k, v in LINES[pid].items():
            out[f'npc.raft.{pid}.{k}'] = v
        out[f'almanac.raft.people.{pid}.title'] = name
        out[f'almanac.raft.people.{pid}.text'] = ALMANAC[pid]
    for k, v in HINTS.items():
        out[f'npc.raft.hint.{k}'] = v
    out.update(UI)
    for k, (t, d) in ADVANCEMENTS.items():
        out[f'advancements.raft.adventure.{k}.title'] = t
        out[f'advancements.raft.adventure.{k}.description'] = d
    out['almanac.raft.people.title'] = 'People of the Sea'
    out['almanac.raft.people.text'] = ALMANAC_INTRO
    return out


# greeting buckets every character must have (see NpcCharacter.java)
REQUIRED = {
    'keeper_fenn': ('first', 'at', 'visited', 'after', 'done'),
    'nalu': ('first', 'at', 'after', 'later', 'done'),
    'old_tamo': ('first', 'default', 'done'),
    'dr_voss': ('first', 'first_late', 'regift', 'at', 'after', 'key', 'done'),
    'teo': ('first', 'default', 'solace', 'done'),
    'kai': ('first', 'default', 'done'),
    'dr_lund': ('first', 'default', 'done'),
    'okafor': ('first', 'default', 'done'),
}


def check():
    bad = []
    for pid, buckets in REQUIRED.items():
        for b in buckets:
            if f'greet.{b}' not in LINES[pid]:
                bad.append(f'{pid}: greet.{b}')
        for k in ('place', 'rumor.1', 'rumor.2', 'rumor.3', 'hurt'):
            if k not in LINES[pid]:
                bad.append(f'{pid}: {k}')
    return bad
