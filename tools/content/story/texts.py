"""The words of "Tides of the Deep" (docs/design/beachcraft3.md, section 3): books, letters, terminal logs, radio
lines and the Expedition Journal objectives. Everything ends up in the lang file, so it can be translated later.

Rules: short pages (a book page holds ~200 characters), friendly and a little melancholic, every place points to the
next one. Characters: Captain Rhea Marlow, keeper Tobias Fenn, Nana Kalea, Dr. Imara Voss, Mayor Aldous Crane,
"Saltjaw" Brannigan.
"""

# ------------------------------------------------------------------------------------------------- story books
# id -> (title (<= 32 chars), author, [pages])
BOOKS = {
    'marlow_note': ("Marlow's Note", 'Capt. Rhea Marlow', [
        "If you are reading this, the portal took you too. Welcome to the Endless Sea. I am Captain Rhea Marlow of "
        "the Meridian Expedition. Or what is left of it.",
        "Don't panic. Drink only clean water, keep your raft in good repair and watch the water for fins. The big "
        "shark out here has a name. We called him Bruce. He was never friendly.",
        "Once there was a city out here. Port Solace shone like a lantern on the water. Then the sea rose in a "
        "single night and swallowed it. Something woke in the deep.",
        "I left you my journal and a chart. Sail for the old light on the rocky isle. Tobias Fenn keeps it, if he "
        "still lives. He saw more than any of us.",
        "Read the journal when you are lost. It remembers where you have been and knows where to go next. Someone "
        "has to finish this. I am sorry it has to be you.\n\n- R.M.",
    ]),
    'fenn_logbook': ("Fenn's Logbook", 'Tobias Fenn', [
        "Day 1 of the high water.\nLamp lit at dusk as always. The sea stands two fathoms higher than yesterday and "
        "it is still climbing. No word from Port Solace.",
        "Day 3.\nThe city lights went out at midnight, all at once, like a candle pinched between two fingers. I kept "
        "the lamp burning till dawn. If anyone is out there, let them see it.",
        "Day 9.\nA raft from Saltmere passed below the rocks. Nana Kalea was at the oar, seventy if she is a day. She "
        "shouted that the old songs had warned them. I believe her now.",
        "Day 14.\nSomething passed under the island tonight. The whole rock hummed and the lens rang like a bell. "
        "I am not ashamed to say I hid in the oil store till sunrise.",
        "Day 21.\nWhoever finds this: go to Saltmere, the stilt village where the reef glows. Ask for Nana Kalea. "
        "She knows what the sea wants. I drew you a chart.",
        "My oil runs low. I will keep the light as long as I can. After that, the light keeps itself.\n\n"
        "- T. Fenn, Keeper",
    ]),
    'kalea_letters': ("Letters of Nana Kalea", 'Nana Kalea', [
        "To the stranger in my hut.\nSit, rest your feet. You came for answers, so here are the ones I have. They are "
        "older than me, and I am very old.",
        "Long ago the Tidewardens kept the sea calm. They sang to it with a crystal they called the Heart of the "
        "Tide. Our village kept their songs. And one of their sigils.",
        "The sigils are four: Coral, Pearl, Storm and Ember. Together they lock the Tide Seal, where the Leviathan "
        "sleeps. My grandmother said: never let them come together.",
        "Then the scientist came, Imara Voss, with clever eyes and a ship full of machines. She asked for our sigil. "
        "I said no. She was polite. She came back three times.",
        "Now the seal is cracked and the sea is rising. The Coral Sigil rests in our shrine, behind the statue of the "
        "Tidewarden. Take it. It is time it went home.",
        "Take my old diving helmet too. Voss built her station under the glowing lagoon. Helix, she called it. We "
        "sail for higher rocks tonight. The reef will light your way.",
    ]),
    'voss_journal': ("Voss's Journal", 'Dr. Imara Voss', [
        "Personal journal, I. Voss.\nThe others will read the terminals. These pages are for me. Or for whoever has "
        "to clean up after me.",
        "The sigils sing. That is not poetry: the Pearl Sigil hums at forty hertz, and beside it a crystal cell burns "
        "three times brighter. Crane wants all four. So do I.",
        "The sonar shows a dome of carved stone deep in the trench, with a door of light in it. The Tidewardens built "
        "a lock. A lock means something worth keeping.",
        "Marlow says a lock can also mean something worth keeping IN. I told her she listens to too many old songs. "
        "I wish I had listened to them too.",
        "We drilled for the Heart. The seal cracked. I felt it through the floor plates: a heartbeat, slow as the "
        "tides and far too big. Within the hour the sea began to rise.",
        "I locked the Pearl Sigil in the lab vault. Port Solace must be warned. If I do not come back: Mayor Crane "
        "holds the Storm Sigil. Follow the chart to the drowned city.",
    ]),
    'crane_broadcast': ("Crane's Last Broadcast", 'Mayor Aldous Crane', [
        "Transcript, Port Solace Radio, final broadcast.\n\nCitizens of Port Solace, this is your mayor. Please stay "
        "calm and listen carefully.",
        "The water is rising faster than the pumps can carry it. All boats to the harbour, families first. Take only "
        "what you can carry. The crystal grid will fail by midnight.",
        "I have sealed the Storm Sigil in the vault at the top of City Hall, high above the water line. Doctor Voss "
        "believes the sigils can stop this. I pray she is right.",
        "The Tidewarden tablets went to the Ashen Forge on the fire island for study. If there is a way to undo "
        "this, the old ones wrote it there, in the heat of the mountain.",
        "I was proud of this city. Too proud. We took power from something we did not understand. Forgive us.\n\n"
        "(static)\n...end of transmission.",
    ]),
    'forge_tablet': ('Tablet of the Forge', 'Meridian Expedition', [
        "Translation of the forge tablets.\nGaps in the stone are marked with dots. Read slowly: the Tidewardens "
        "wrote like the tide, in circles.",
        "In the fire of the mountain we forged four sigils from the Heart's light: Coral for the living reef, Pearl "
        "for patience, Storm for the wind, Ember for the deep fire.",
        "Four sigils lock the Seal. Four sigils, joined at the altar of coral, make the Key that opens it. Let the "
        "Key never be forged, unless the Sleeper ... wakes.",
        "The Ember we keep here, beyond the river of fire, where only the brave will walk. The altar waits in the "
        "temple of coral, on the bright reef floor.",
        "Translator's note: 'the Sleeper' is the Leviathan. The tablets say the Key can bind it again, or end it. "
        "They do not say which is harder.",
    ]),
    'tidewarden_prophecy': ('The Tidewarden Prophecy', 'The Tidewardens', [
        "When the Seal is broken and the sea climbs the mountains, one will come through the driftwood door, a "
        "stranger to the water, and the tide will know them.",
        "Coral, Pearl, Storm and Ember: four tides gathered in one hand. At the altar of coral they will burn "
        "together, and the Key will rise from their light.",
        "Go down into the dark where the trench drinks the sun. The Sanctum waits there, sealed in our stone. Show it "
        "the Key, and the Seal will part like water.",
        "There the Sleeper coils in its pool. Wake it at the altar by the water's edge. Face it on the ring of stone. "
        "When it falls, the Heart of the Tide returns.",
        "And the sea will lie down like a tired dog, and the keepers of the tides will be many again.",
    ]),
    'saltjaw_log': ("Saltjaw's Log", 'Saltjaw Brannigan', [
        "Log of the Grinning Gull.\nCaptain Brannigan, called Saltjaw on account of the jaw. Note to self: never "
        "write where the treasure is. Note to self: oops.",
        "Took a fat Expedition barge off the old rig today. Gold, crystals and a strange stone that hums. Crew wants "
        "to sell the stone. Captain wants to keep it. Captain wins.",
        "The old sailors say the humming stone is Tidewarden gold. Since we took it the crew don't sleep. Mulligan "
        "swears his own reflection grinned at him.",
        "The sea rose. Lost the Gull on the reef of wrecks. Swam to the cove with what I could carry. The rest of the "
        "crew... came back. Wet. Cold. Not quite right. Still loyal.",
        "My treasure sits in the cove under the cliff, where the sea goes in and nothing comes out. The lads guard "
        "it. They guard it from me too, now.",
        "If you're reading this, you got past them. Take the gold. Throw the humming stone back into the sea. And "
        "tell 'em Saltjaw was a gentleman. Mostly.",
    ]),
}

# ------------------------------------------------------------------------------------------------- bottle letters
# id -> (title, author, [pages]); one of them is rolled by raft:story/bottle_letters
LETTERS = {
    'letter_gerald': ('A Letter in a Bottle', 'A castaway', [
        "Day 40 on the raft. Caught a fish with a hook made of plastic. I named the fish Gerald. Then I ate Gerald. "
        "I miss Gerald.",
        "If found, please send a boat. Or another Gerald.",
    ]),
    'letter_mia': ('Dear Sea', 'Mia, 8 years', [
        "Dear sea,\nplease give back my grandpa's boat. You have lots of boats now. I counted eleven sunk ones from "
        "the dock of Saltmere.\n\nLove, Mia (8 years)",
    ]),
    'letter_okafor': ('Note from Rig 7', 'Eng. P. Okafor', [
        "To whoever finds this: the old Meridian rig on its steel legs, out on the open sea, still has power. The "
        "radio works. Nobody answers.",
        "There are good tools in the store room and a crane that still turns. Bring company. The nights out there "
        "are long. - Eng. P. Okafor",
    ]),
    'letter_reyes': ('Flight Twelve', 'Pilot J. Reyes', [
        "Flight Twelve went down over the kelp forest. We ditched in the sea and swam for it. Everyone made it. "
        "Everyone but the plane.",
        "The cargo hold had medicine, tools and a crate of tinned peaches. Maybe it still does. Watch out for the "
        "eels. - J. Reyes, pilot",
    ]),
    'letter_lund': ('From the Ice', 'Dr. E. Lund', [
        "It is cold on the white floes in the north. The penguins are better company than the Expedition ever was. "
        "Our hut has a stove, a porthole and a kettle.",
        "Visitors welcome. Bring your own mug. - Dr. E. Lund, Iceberg Outpost",
    ]),
    'letter_teo': ('Dearest Ana', 'Teo', [
        "Dearest Ana,\nthe water took Port Solace, but not me. I'm on the island with the little palm-roof beach "
        "bar. I wait at the water every sunset.",
        "Bring the good rum. Bring yourself, mostly. - Teo",
    ]),
    'letter_pirate': ('Arr!', 'A former pirate', [
        "ARR. To any scallywag who reads this: the big galleon on the floor of the wreck graveyard is still full of "
        "loot. Also full of dead men. Mind the dead men.",
    ]),
    'letter_marlow': ("Marlow's Second Note", 'Capt. Rhea Marlow', [
        "Note two. Saw the serpent's back break the water near the trench today. Longer than any ship I have ever "
        "sailed. It did not look at us. It did not need to.",
        "If the sigils can stop it, gather them. If not, row. - R.M.",
    ]),
    'letter_whale': ('Whale Song', 'A lonely sailor', [
        "Today a whale sang under my raft. It was so loud the planks hummed. I think it was saying hello. I sang "
        "back. It swam away. Rude.",
    ]),
    'letter_fenn': ('A Favour', 'T. Fenn', [
        "If you pass the old light on the rocky isle, do me a favour and look after the lamp. A light that goes out "
        "is a promise broken. - T.F.",
    ]),
    'letter_voss': ('An Apology', 'I. Voss', [
        "To every family of Port Solace:\nI am sorry. I was so sure. I will make this right, or the sea can have "
        "me.\n\n- I.V.",
    ]),
    'letter_memo': ('Memo 118', 'Office of the Mayor', [
        "Mayor's office, memo to all staff: the code of the City Hall vault is the mayor's birthday. Please stop "
        "writing it on sticky notes. Thank you.",
    ]),
    'letter_bruce': ('Warning!', 'Raft Club of Saltmere', [
        "WARNING: a shark with a notched fin bites rafts around here. His name is Bruce. If he circles you, hit him "
        "with something hard. He respects that.\n\n- Raft Club of Saltmere",
    ]),
    'letter_kai': ('SOS', 'Kai', [
        "We built a camp on a palm isle and spelled SOS in stones on the sand. A seagull stole my last biscuit. If "
        "you find this, bring biscuits. And a net. - Kai",
    ]),
}

# ------------------------------------------------------------------------------------------------- terminal logs
# LogId -> (title, text). Text: paragraphs separated by \n. The terminal screen pages it.
TERMINALS = {
    'helix_1': ('HELIX LOG 01 - ARRIVAL',
                "Dr. I. Voss, Helix Research Station.\n"
                "The domes are sealed and dry. Below us the reef glows blue at night: bioluminescence, and something "
                "more. Our crystal samples resonate with it.\n"
                "The elder of Saltmere refused to give us their sigil. We will find others. The Tidewardens made "
                "four, and the ruins on the shelf point the way."),
    'helix_2': ('HELIX LOG 02 - THE PEARL',
                "Recovered the Pearl Sigil from a collapsed ruin on the lagoon floor. It hums at forty hertz. Placed "
                "beside a crystal cell, the cell output tripled.\n"
                "Mayor Crane visited. He wants the whole Port Solace grid running on sigil resonance by spring. I "
                "told him we do not understand it yet. He told me to understand faster."),
    'helix_3': ('HELIX LOG 03 - THE TRENCH',
                "Sonar sweep of the abyssal trench: a dome of carved tidestone far below the shelf, with a door that "
                "reflects nothing. Our instruments call it a wall of light.\n"
                "The Tidewarden glyphs on the door repeat a single word. Captain Marlow translated it for me. The "
                "word is 'sleeping'."),
    'helix_4': ('HELIX LOG 04 - DRILL ORDER',
                "Drill order signed by Mayor Crane. Target: the crystal veins under the trench, the source the "
                "Tidewardens called the Heart of the Tide.\n"
                "Captain Marlow formally objects. Logged. Her words: 'You do not drill into a lock.' My reply: it is "
                "only stone.\n"
                "Note for later: I hope I was right."),
    'helix_5': ('HELIX LOG 05 - BREACH',
                "06:12 - drill head reached the vein.\n"
                "06:13 - resonance spike. Every sigil in the lab lit up at once.\n"
                "06:14 - the seal on the trench door cracked.\n"
                "Something moved below. The station tilted three degrees. The sonar shows a body, coiling. It does "
                "not end on the screen.\n"
                "Sea level rising. Four centimetres per minute."),
    'helix_6': ('HELIX LOG 06 - FINAL ENTRY',
                "Port Solace is gone. The water came over the sea wall at midnight.\n"
                "I have locked the Pearl Sigil in the lab vault. It must not be lost.\n"
                "To whoever reads this: the tablets at the Ashen Forge describe a key made from all four sigils. It "
                "can close what I opened. Find the sigils. Find the temple.\n"
                "I am going to the city to warn them. - Imara"),
    'solace_1': ('PORT SOLACE GRID - STATUS',
                 "Crystal grid output: 340 percent.\n"
                 "WARNING: resonance harmonics outside the safe range.\n"
                 "WARNING: tidal sensors report rising sea level.\n"
                 "Automatic message to the Power Authority: please confirm that the new sigil cells behave as "
                 "expected.\n"
                 "(No reply on record.)"),
    'solace_2': ('HARBOUR MASTER - EVACUATION',
                 "Evacuation list, harbour office.\n"
                 "Ferry one: Saltmere, 112 souls. Ferry two: the lighthouse isle, 40 souls. Fishing fleet: "
                 "scattered, heading for the high islands.\n"
                 "Remaining in the city: City Hall staff, the mayor and the night shift of the Power Authority.\n"
                 "Fair winds to them."),
    'solace_3': ('CITY HALL - MEMO 117',
                 "From the office of Mayor A. Crane.\n"
                 "The Storm Sigil is to be moved to the vault on the top floor of City Hall, above any flood line. "
                 "Access: mayor only.\n"
                 "The Tidewarden tablets are to be shipped to the research team at the Ashen Forge on the fire "
                 "island. Keep them dry. Keep them away from Doctor Voss."),
    'solace_4': ('EMERGENCY CHANNEL',
                 "Water on the twelfth floor. The lights are failing.\n"
                 "We can see it through the windows: something huge, circling the tower. It is not attacking. It is "
                 "waiting.\n"
                 "If the sigils could wake it, they can bind it. That is what the tablets say. Somebody, please, "
                 "finish this.\n"
                 "- Night shift, Power Authority"),
    'rig_1': ('RIG 7 - DRILLING LOG',
              "Meridian Rig 7, drilling log.\n"
              "Core sample 88 shows crystal veins running east, towards the trench. Hull vibration since we passed "
              "nine hundred metres. The crew calls it 'the hum'.\n"
              "Orders from Port Solace: keep drilling. Orders from my stomach: stop drilling. Following orders from "
              "Port Solace."),
    'rig_2': ('RIG 7 - RADIO ROOM',
              "Storm from the east, the third this week. Something big surfaced off the north leg: scales like slate, "
              "a back as long as the rig. It looked at us and went down again.\n"
              "We leave on the last boat. I left the radio on the emergency band. If you hear static that sounds "
              "like singing, that is the deep. Do not answer it."),
    'lab_misc_1': ('SAMPLE INVENTORY',
                   "Sample inventory, cold storage.\n"
                   "A-12: glowing kelp, alive.\n"
                   "A-13: urchin spines. Do not touch.\n"
                   "A-14: crystal shard, warm to the touch.\n"
                   "A-15: shark tooth, notched. The shark wants it back. Keep the hatch closed."),
    'lab_misc_2': ('MAINTENANCE TICKET 4031',
                   "Porthole seal in corridor C weeps salt water. Second time this month.\n"
                   "Resolution: replaced the gasket, tightened the bolts, said something rude to the ocean.\n"
                   "Status: holding. For now."),
    'lab_misc_3': ('STAFF NOTICE',
                   "Notice to all staff: the station coffee is brewed with filtered sea water. It tastes like sea "
                   "water. Complaints go to the Expedition office.\n"
                   "The Expedition office is under water.\n"
                   "Thank you for your understanding."),
}

# ------------------------------------------------------------------------------------------------- radio broadcasts
RADIO = [
    "...this is Port Solace harbour control... all vessels, keep clear of the drowned shelf... (static)",
    "...the light on the rocky isle still burns. Keeper Fenn, if you hear this: thank you...",
    "...Saltmere calling any boat. The reef glows tonight. The tide is high and the songs are old...",
    "...weather report: storm from the east, waves up to six metres... secure your rafts...",
    "...Helix to Port Solace, resonance unstable, repeat, resonance uns- (static)",
    "...anyone near the volcano? The forge lights are back on. Nobody should be in there...",
    "...arr, this be Saltjaw's crew... the gold stays in the cove, matey... (laughter) (static)",
    "...Rig Seven, emergency band. Something big off the north leg. Do not answer the singing...",
    "...Marlow to any survivor: gather the sigils. Coral, Pearl, Storm, Ember. Trust the journal...",
    "...whale song on channel nine... (long, low notes) ...beautiful...",
    "...Flight Twelve, mayday, mayday, going down over the kelp forest... (static)",
    "...Dr. Lund at the ice floe outpost. The tea is hot, the penguins are angry. Over...",
    "...City Hall to anyone: the Storm Sigil is in the tower vault, above the water line...",
    "...the temple on the reef floor... the altar remembers four lights... (static)",
    "...(singing from far below, too slow and too big to be a whale)...",
    "...(nothing but soft static and the sound of waves)...",
]

# ------------------------------------------------------------------------------------------------- journal chapters
# Order = the story order. (advancement path, title, objective, hint). The journal shows the first one not done.
CHAPTERS = [
    ('lighthouse', 'Keeper of the Light', 'Find the Old Lighthouse.',
     "Follow the map from the castaway chest to the old light on the rocky isle."),
    ('saltmere', 'Saltmere', 'Sail to Saltmere.',
     "Fenn's chart leads to the stilt village where the reef glows."),
    ('coral_sigil', 'The Coral Sigil', "Take the Coral Sigil from Saltmere's shrine.",
     "Nana Kalea hid it behind the statue of the Tidewarden."),
    ('helix', 'Deep Science', 'Dive to Helix Research Station.',
     "It lies on the sea floor below the glowing lagoon. Wear a diving helmet."),
    ('pearl_sigil', 'The Pearl Sigil', 'Open the lab vault of Helix Station.',
     "Voss locked the Pearl Sigil away. Read her terminals."),
    ('port_solace', 'The Drowned City', 'Find Port Solace.',
     "The city sleeps on the drowned shelf. Broken towers reach for the surface."),
    ('storm_sigil', 'The Storm Sigil', 'Climb the City Hall tower.',
     "Mayor Crane sealed the Storm Sigil in the vault near the top."),
    ('ashen_forge', 'Through Fire', 'Find the Ashen Forge.',
     "The Tidewarden tablets went to the forge on the volcanic isle."),
    ('ember_sigil', 'The Ember Sigil', 'Cross the river of fire.',
     "The Ember Sigil waits in the vault beyond the lava moat."),
    ('coral_temple', 'The Coral Temple', 'Find the Coral Temple.',
     "An overgrown Tidewarden temple rests on the bright reef floor."),
    ('tidewarden_key', 'The Tidewarden Key', 'Forge the Tidewarden Key.',
     "Place all four sigils in the altar of the Coral Temple."),
    ('abyssal_sanctum', 'The Abyssal Sanctum', 'Open the Abyssal Sanctum.',
     "Dive into the trench and show the Tide Seal your key."),
    ('leviathan', 'The Leviathan', 'Defeat the Leviathan.',
     "Use the key on the altar at the pool's edge. Fight on the stone ring."),
]
DONE = ('Calm Seas', 'The sea is calm.', 'Thank you, keeper of the tides.')

# map tag per chapter (the place the next step happens) and the map's name
MAP_OF = {
    'lighthouse': 'lighthouse', 'saltmere': 'saltmere', 'coral_sigil': 'saltmere', 'helix': 'helix',
    'pearl_sigil': 'helix', 'port_solace': 'port_solace', 'storm_sigil': 'port_solace',
    'ashen_forge': 'ashen_forge', 'ember_sigil': 'ashen_forge', 'coral_temple': 'coral_temple',
    'tidewarden_key': 'coral_temple', 'abyssal_sanctum': 'abyssal_sanctum', 'leviathan': 'abyssal_sanctum',
}
MAP_NAMES = {
    'lighthouse': 'Chart to the Old Lighthouse',
    'saltmere': 'Chart to Saltmere',
    'helix': 'Chart to Helix Station',
    'port_solace': 'Chart to Port Solace',
    'ashen_forge': 'Chart to the Ashen Forge',
    'coral_temple': 'Chart to the Coral Temple',
    'abyssal_sanctum': 'Chart to the Abyssal Sanctum',
    'pirate': "Saltjaw's Treasure Chart",
}
