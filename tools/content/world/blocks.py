"""Natural blocks and items of the raft dimension (docs/design/beachcraft3.md 4.3): textures, block models,
blockstates, item definitions, loot tables, recipes, tags and lang. The Java side is content/world/WorldBlocks.java.
"""
from collections import OrderedDict

from bclib import assets, extrude, tex
from content.world import art as A

M = 'world'


def R(x):
    return assets.rid(x)


def save(im, rel):
    tex.save(im, M, rel)


def save_anim(frames, rel, frametime=6, interpolate=True):
    tex.save_animated(frames, M, rel, frametime=frametime, interpolate=interpolate)


def model(name, data):
    assets.write_json(M, f'assets/raft/models/block/{name}.json', data)


def el(frm, to, faces, shade=None):
    e = OrderedDict([('from', list(frm)), ('to', list(to)), ('faces', faces)])
    if shade is not None:
        e['shade'] = shade
    return e


def face(tex_key, uv, **extra):
    f = OrderedDict(uv=list(uv), texture=tex_key)
    f.update(extra)
    return f


# --------------------------------------------------------------------------------------------- loot helpers
# 26.3 loot format: "condition" (one condition object or a named predicate), "modifier" (function or list),
# conditions are {"type": ...}; block states via minecraft:match_block (see the vanilla loot tables).
SURVIVES = {'type': 'minecraft:survives_explosion'}
SHEARS = 'minecraft:tool/can_shear'
SILK = 'minecraft:tool/can_silk_touch'
DECAY = {'type': 'minecraft:explosion_decay'}


def state_is(block, **props):
    return OrderedDict(type='minecraft:match_block', blocks=R(block),
                       state={k: str(v).lower() if isinstance(v, bool) else str(v) for k, v in props.items()})


def all_of(*terms):
    return OrderedDict(type='minecraft:all_of', terms=list(terms))


def any_of(*terms):
    return OrderedDict(type='minecraft:any_of', terms=list(terms))


def item_entry(item, condition=None, modifier=None):
    e = OrderedDict(type='minecraft:item')
    if condition is not None:
        e['condition'] = condition
    if modifier is not None:
        e['modifier'] = modifier
    e['name'] = R(item)
    return e


def pool(entries, condition=None, modifier=None):
    p = OrderedDict()
    if condition is not None:
        p['condition'] = condition
    p['entries'] = entries
    if modifier is not None:
        p['modifier'] = modifier
    p['rolls'] = 1
    return p


def table(block, pools, modifier=None):
    t = OrderedDict(type='minecraft:block')
    if modifier is not None:
        t['modifier'] = modifier
    t['pools'] = pools
    t['random_sequence'] = f'raft:blocks/{block}'
    assets.loot_table(M, f'blocks/{block}', t)


def drop_self(block, item=None):
    table(block, [pool([item_entry(item or block)], condition=SURVIVES)])


def shears_only(block, item=None, extra_cond=None):
    cond = SHEARS if extra_cond is None else all_of(extra_cond, SHEARS)
    table(block, [pool([item_entry(item or block)], condition=cond)])


def count_by_state(block, item, prop, maxn):
    mods = [OrderedDict(type='minecraft:set_count', condition=state_is(block, **{prop: n}), count=n)
            for n in range(2, maxn + 1)]
    mods.append(DECAY)
    table(block, [pool([item_entry(item, modifier=mods)])])


# --------------------------------------------------------------------------------------------- blocks
def ground():
    save(A.shell_sand(), 'block/shell_sand')
    save(A.black_sand(), 'block/black_sand')
    save(A.volcanic_ash(), 'block/volcanic_ash')
    save(A.pumice(), 'block/pumice')
    for b in ('shell_sand', 'black_sand', 'volcanic_ash', 'pumice'):
        assets.simple_block(M, b)
        drop_self(b)
    side, top, bottom = A.bubble_vent()
    save(side, 'block/bubble_vent_side')
    save_anim(top, 'block/bubble_vent_top', frametime=10)
    save(bottom, 'block/bubble_vent_bottom')
    assets.block_model(M, 'bubble_vent', 'minecraft:block/cube_bottom_top',
                       {'side': 'block/bubble_vent_side', 'top': 'block/bubble_vent_top',
                        'bottom': 'block/bubble_vent_bottom'})
    assets.blockstate(M, 'bubble_vent', {'': {'model': R('block/bubble_vent')}})
    assets.block_item(M, 'bubble_vent')
    drop_self('bubble_vent')
    # recipes
    assets.recipe_cooking(M, 'tinted_glass_from_black_sand', 'black_sand', 'minecraft:tinted_glass', xp=0.1,
                          category='blocks')
    assets.recipe_shapeless(M, 'shell_sand', ['minecraft:sand'] * 4 + ['seashell'], 'shell_sand', count=4,
                            category='building')
    assets.recipe_shaped(M, 'bubble_vent', [' B ', 'BSB', ' B '], {'B': 'minecraft:basalt', 'S': 'minecraft:soul_sand'},
                         'bubble_vent', category='building')
    assets.tag(M, 'block', 'minecraft:mineable/shovel', ['shell_sand', 'black_sand', 'volcanic_ash'])
    assets.tag(M, 'block', 'minecraft:mineable/pickaxe', ['pumice', 'bubble_vent'])
    assets.tag(M, 'block', 'minecraft:sand', ['shell_sand', 'black_sand'])
    assets.tag(M, 'item', 'minecraft:sand', ['shell_sand', 'black_sand'])
    assets.tag(M, 'item', 'minecraft:smelts_to_glass', ['shell_sand'])
    assets.tag(M, 'block', 'minecraft:enables_bubble_column_push_up', ['bubble_vent'])
    assets.tag(M, 'block', 'minecraft:rabbits_spawnable_on', ['shell_sand'])
    assets.tag(M, 'block', 'minecraft:enderman_holdable', ['shell_sand', 'black_sand', 'volcanic_ash'])


NEW_FLOWERS = [('plumeria', 'white_dye'), ('beach_morning_glory', 'purple_dye'), ('tropical_orchid', 'magenta_dye'),
               ('spider_lily', 'white_dye')]
NEW_POTS = ['potted_plumeria', 'potted_tropical_orchid', 'potted_aloe_vera']


def island_plants():
    # simple cross plants
    save(A.beach_grass(), 'block/beach_grass')
    save(A.tropical_fern(), 'block/tropical_fern')
    save(A.hibiscus(), 'block/hibiscus')
    for b in ('beach_grass', 'tropical_fern', 'hibiscus'):
        assets.cross_plant(M, b)
    shears_only('beach_grass')
    shears_only('tropical_fern')
    drop_self('hibiscus')
    assets.recipe_shapeless(M, 'red_dye_from_hibiscus', ['hibiscus'], 'minecraft:red_dye', category='misc')
    # island flowers of 3.0.1
    for b, dye in NEW_FLOWERS + [('aloe_vera', 'green_dye')]:
        save(getattr(A, b)(), f'block/{b}')
        assets.cross_plant(M, b)
        drop_self(b)
        assets.recipe_shapeless(M, f'{dye}_from_{b}', [b], f'minecraft:{dye}', category='misc')

    # two tall plants
    for b, (lo, up) in (('sea_oats', A.sea_oats()), ('bird_of_paradise', A.bird_of_paradise()),
                        ('heliconia', A.heliconia())):
        save(lo, f'block/{b}_bottom')
        save(up, f'block/{b}_top')
        assets.block_model(M, f'{b}_bottom', 'minecraft:block/cross', {'cross': f'block/{b}_bottom'})
        assets.block_model(M, f'{b}_top', 'minecraft:block/cross', {'cross': f'block/{b}_top'})
        assets.blockstate(M, b, {'half=lower': {'model': R(f'block/{b}_bottom')},
                                 'half=upper': {'model': R(f'block/{b}_top')}})
        assets.flat_item(M, b, f'block/{b}_top')
    shears_only('sea_oats', extra_cond=state_is('sea_oats', half='lower'))
    table('bird_of_paradise', [pool([item_entry('bird_of_paradise')],
                                    condition=all_of(SURVIVES, state_is('bird_of_paradise', half='lower')))])
    assets.recipe_shapeless(M, 'orange_dye_from_bird_of_paradise', ['bird_of_paradise'], 'minecraft:orange_dye', count=2)
    table('heliconia', [pool([item_entry('heliconia')], condition=all_of(SURVIVES, state_is('heliconia', half='lower')))])
    assets.recipe_shapeless(M, 'red_dye_from_heliconia', ['heliconia'], 'minecraft:red_dye', count=2)

    # pineapple bush (ages 0..3) and pineapple crop (ages 0..7)
    v = OrderedDict()
    for age in range(4):
        save(A.pineapple_bush(age), f'block/pineapple_bush_stage{age}')
        assets.block_model(M, f'pineapple_bush_stage{age}', 'minecraft:block/cross',
                           {'cross': f'block/pineapple_bush_stage{age}'})
        v[f'age={age}'] = {'model': R(f'block/pineapple_bush_stage{age}')}
    assets.blockstate(M, 'pineapple_bush', v)
    assets.flat_item(M, 'pineapple_bush', 'block/pineapple_bush_stage3')
    table('pineapple_bush', [
        pool([item_entry('pineapple_bush')], condition=SURVIVES),
        pool([item_entry('pineapple')], condition=state_is('pineapple_bush', age=3),
             modifier=[OrderedDict(type='minecraft:set_count', count=OrderedDict(type='minecraft:uniform', max=2, min=1)),
                       OrderedDict(type='minecraft:apply_bonus', enchantment='minecraft:fortune',
                                   formula='minecraft:uniform_bonus_count', parameters={'bonusMultiplier': 1})])],
          modifier=DECAY)
    v = OrderedDict()
    for age in range(8):
        save(A.pineapple_crop(age), f'block/pineapple_crop_stage{age}')
        assets.block_model(M, f'pineapple_crop_stage{age}', 'minecraft:block/crop',
                           {'crop': f'block/pineapple_crop_stage{age}'})
        v[f'age={age}'] = {'model': R(f'block/pineapple_crop_stage{age}')}
    assets.blockstate(M, 'pineapple_crop', v)
    ripe = state_is('pineapple_crop', age=7)
    table('pineapple_crop', [
        pool([OrderedDict(type='minecraft:alternatives', children=[
            item_entry('pineapple', condition=ripe), item_entry('pineapple_crown')])]),
        pool([item_entry('pineapple_crown', modifier=OrderedDict(
            type='minecraft:apply_bonus', enchantment='minecraft:fortune', formula='minecraft:binomial_with_bonus_count',
            parameters={'extra': 0, 'probability': 0.35}))], condition=ripe)], modifier=DECAY)

    # flower pots
    for pot, plant in (('potted_hibiscus', 'block/hibiscus'), ('potted_tropical_fern', 'block/tropical_fern'),
                       ('potted_palm_sapling', 'block/palm_sapling')) + tuple((p, f'block/{p[7:]}') for p in NEW_POTS):
        assets.potted(M, pot, R(plant))
        item = pot[len('potted_'):]
        table(pot, [pool([item_entry('minecraft:flower_pot')], condition=SURVIVES),
                    pool([item_entry(item)], condition=SURVIVES)])

    # items
    save(A.pineapple_item(), 'item/pineapple')
    save(A.pineapple_crown_item(), 'item/pineapple_crown')
    assets.flat_item(M, 'pineapple')
    assets.flat_item(M, 'pineapple_crown')
    assets.recipe_shapeless(M, 'pineapple_crown', ['pineapple'], 'pineapple_crown', category='misc')

    plants = ['beach_grass', 'sea_oats', 'tropical_fern']
    small = ['hibiscus'] + [b for b, _ in NEW_FLOWERS]
    flowers = small + ['bird_of_paradise', 'heliconia', 'aloe_vera']
    assets.tag(M, 'block', 'minecraft:replaceable', plants)
    assets.tag(M, 'block', 'minecraft:replaceable_by_trees', plants + ['bird_of_paradise', 'heliconia'])
    assets.tag(M, 'block', 'minecraft:replaceable_by_mushrooms', plants + ['bird_of_paradise', 'heliconia'])
    assets.tag(M, 'block', 'minecraft:sword_efficient', plants + flowers + ['pineapple_bush'])
    assets.tag(M, 'block', 'minecraft:washed_away_by_fluids', plants + flowers + ['pineapple_bush', 'pineapple_crop'])
    assets.tag(M, 'block', 'minecraft:small_flowers', small)
    assets.tag(M, 'block', 'minecraft:flowers', ['bird_of_paradise', 'heliconia'])
    assets.tag(M, 'block', 'minecraft:bee_attractive', flowers)
    assets.tag(M, 'block', 'minecraft:flower_pots', ['potted_hibiscus', 'potted_tropical_fern', 'potted_palm_sapling']
               + NEW_POTS)
    assets.tag(M, 'block', 'minecraft:crops', ['pineapple_crop'])
    assets.tag(M, 'block', 'minecraft:maintains_farmland', ['pineapple_crop'])
    assets.tag(M, 'block', 'minecraft:bee_growables', ['pineapple_crop', 'pineapple_bush'])
    assets.tag(M, 'item', 'minecraft:small_flowers', small)
    assets.tag(M, 'item', 'minecraft:flowers', ['bird_of_paradise'])
    assets.tag(M, 'item', 'minecraft:bee_food', flowers)
    assets.tag(M, 'item', 'minecraft:villager_plantable_seeds', ['pineapple_crown'])


def sea_life():
    # glow kelp (animated)
    save_anim(A.glow_kelp(False), 'block/glow_kelp', frametime=5)
    save_anim(A.glow_kelp(True), 'block/glow_kelp_plant', frametime=5)
    for b in ('glow_kelp', 'glow_kelp_plant'):
        assets.block_model(M, b, 'minecraft:block/cross', {'cross': f'block/{b}'})
        assets.blockstate(M, b, {'': {'model': R(f'block/{b}')}})
        drop_self(b, 'glow_kelp')
    save(A.glow_kelp_item(), 'item/glow_kelp')
    assets.flat_item(M, 'glow_kelp')

    # anemones and tube worms (cross models, shears)
    save(A.anemone('#c8283a', '#ffb0a0'), 'block/sea_anemone_red')
    save(A.anemone('#7a3ac8', '#f0b0ff'), 'block/sea_anemone_purple')
    save_anim(A.anemone_glow_frames(), 'block/sea_anemone_glow', frametime=8)
    save(A.tube_worms(), 'block/tube_worms')
    for b in ('sea_anemone_red', 'sea_anemone_purple', 'sea_anemone_glow', 'tube_worms'):
        assets.cross_plant(M, b)
        shears_only(b)

    # giant clam: bottom valve + lid (closed / open), pearl
    shell, lip, mantle, pearl = A.giant_clam()
    save(shell, 'block/giant_clam_shell')
    save(lip, 'block/giant_clam_lip')
    save(mantle, 'block/giant_clam_mantle')
    save(pearl, 'block/giant_clam_pearl')
    tx = {'shell': R('block/giant_clam_shell'), 'lip': R('block/giant_clam_lip'),
          'mantle': R('block/giant_clam_mantle'), 'pearl': R('block/giant_clam_pearl'),
          'particle': R('block/giant_clam_shell')}

    def box_faces(top, sides, bottom, h0, h1):
        """faces of a 14x14 valve box between heights h0..h1 (lip rows taken from the matching texture band)"""
        return OrderedDict([
            ('down', face(bottom, (1, 1, 15, 15), cullface='down') if h0 == 0 else face(bottom, (1, 1, 15, 15))),
            ('up', face(top, (1, 1, 15, 15))),
            ('north', face(sides, (1, 16 - h1, 15, 16 - h0))), ('south', face(sides, (1, 16 - h1, 15, 16 - h0))),
            ('west', face(sides, (1, 16 - h1, 15, 16 - h0))), ('east', face(sides, (1, 16 - h1, 15, 16 - h0)))])

    bottom = el((1, 0, 1), (15, 4, 15), box_faces('#mantle', '#lip', '#shell', 0, 4))
    closed_lid = el((1, 4, 1), (15, 7, 15), OrderedDict([
        ('up', face('#shell', (1, 1, 15, 15))), ('down', face('#shell', (1, 1, 15, 15))),
        ('north', face('#lip', (1, 8, 15, 11))), ('south', face('#lip', (1, 8, 15, 11))),
        ('west', face('#lip', (1, 8, 15, 11))), ('east', face('#lip', (1, 8, 15, 11)))]))
    # open lid: a valve standing up at the back (hinge south), its inside (mantle) facing the opening (north)
    open_lid = el((1, 4, 11), (15, 13, 14), OrderedDict([
        ('north', face('#mantle', (1, 3, 15, 12))), ('south', face('#shell', (1, 3, 15, 12))),
        ('up', face('#lip', (1, 8, 15, 11))), ('down', face('#lip', (1, 8, 15, 11))),
        ('west', face('#lip', (2, 8, 5, 16))), ('east', face('#lip', (2, 8, 5, 16)))]))
    pearl_el = el((7, 4, 5), (9, 6, 7), OrderedDict(
        (d, face('#pearl', (0, 0, 2, 2))) for d in ('north', 'south', 'east', 'west', 'up', 'down')))
    model('giant_clam', OrderedDict(parent='minecraft:block/block', textures=tx, elements=[bottom, closed_lid]))
    model('giant_clam_open', OrderedDict(parent='minecraft:block/block', textures=tx, elements=[bottom, open_lid]))
    model('giant_clam_open_pearl', OrderedDict(parent='minecraft:block/block', textures=tx,
                                               elements=[bottom, open_lid, pearl_el]))
    v = OrderedDict()
    for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for op in (False, True):
            for pe in (False, True):
                mdl = 'giant_clam' if not op else 'giant_clam_open_pearl' if pe else 'giant_clam_open'
                e = {'model': R('block/' + mdl)}
                if rot:
                    e['y'] = rot
                v[f'facing={facing},open={str(op).lower()},pearl={str(pe).lower()}'] = e
    assets.blockstate(M, 'giant_clam', v)
    assets.block_item(M, 'giant_clam', 'block/giant_clam_open_pearl')
    drop_self('giant_clam')

    # starfish: flat on the floor, wall or ceiling (button-style rotations)
    for colour, base in (('orange', '#f08a2a'), ('purple', '#9a4ad8'), ('red', '#d8283a')):
        b = f'starfish_{colour}'
        save(A.starfish(base), f'block/{b}')
        model(b, OrderedDict(parent='minecraft:block/block', ambientocclusion=False,
                             textures={'star': R(f'block/{b}'), 'particle': R(f'block/{b}')},
                             elements=[el((0, 0, 0), (16, 0.5, 16), OrderedDict([
                                 ('up', face('#star', (0, 0, 16, 16))),
                                 ('down', face('#star', (0, 16, 16, 0), cullface='down'))]))]))
        v = OrderedDict()
        rots = {('floor', 'north'): {}, ('floor', 'east'): {'y': 90}, ('floor', 'south'): {'y': 180},
                ('floor', 'west'): {'y': 270},
                ('wall', 'north'): {'x': 90}, ('wall', 'east'): {'x': 90, 'y': 90},
                ('wall', 'south'): {'x': 90, 'y': 180}, ('wall', 'west'): {'x': 90, 'y': 270},
                ('ceiling', 'north'): {'x': 180, 'y': 180}, ('ceiling', 'east'): {'x': 180, 'y': 270},
                ('ceiling', 'south'): {'x': 180}, ('ceiling', 'west'): {'x': 180, 'y': 90}}
        for (fc, fa), r in rots.items():
            e = {'model': R(f'block/{b}')}
            e.update(r)
            v[f'face={fc},facing={fa}'] = e
        assets.blockstate(M, b, v)
        assets.flat_item(M, b, f'block/{b}')
        drop_self(b)

    # sea urchin: spiny ball (core + crossed spine planes)
    core, spines = A.sea_urchin()
    save(core, 'block/sea_urchin')
    save(spines, 'block/sea_urchin_spines')
    cross_planes = []
    for frm, to, axis_faces in (((0.8, 0, 8), (15.2, 8.1, 8), ('north', 'south')),
                                ((8, 0, 0.8), (8, 8.1, 15.2), ('west', 'east'))):
        f = OrderedDict((d, face('#spines', (0, 7, 16, 16) if d in ('north', 'west') else (16, 7, 0, 16)))
                        for d in axis_faces)
        e = el(frm, to, f, shade=False)
        e['rotation'] = OrderedDict(angle=45, axis='y', origin=[8, 8, 8],
                                    rescale=True)
        cross_planes.append(e)
    model('sea_urchin', OrderedDict(parent='minecraft:block/block', ambientocclusion=False,
                                    textures={'core': R('block/sea_urchin'), 'spines': R('block/sea_urchin_spines'),
                                              'particle': R('block/sea_urchin')},
                                    elements=[el((4, 0, 4), (12, 6, 12), OrderedDict([
                                        ('down', face('#core', (4, 4, 12, 12), cullface='down')),
                                        ('up', face('#core', (4, 4, 12, 12))),
                                        ('north', face('#core', (4, 10, 12, 16))), ('south', face('#core', (4, 10, 12, 16))),
                                        ('west', face('#core', (4, 10, 12, 16))), ('east', face('#core', (4, 10, 12, 16)))])),
                                              el((5, 6, 5), (11, 7, 11), OrderedDict([
                                                  ('up', face('#core', (5, 5, 11, 11))),
                                                  ('north', face('#core', (5, 9, 11, 10))), ('south', face('#core', (5, 9, 11, 10))),
                                                  ('west', face('#core', (5, 9, 11, 10))), ('east', face('#core', (5, 9, 11, 10)))]))]
                                    + cross_planes))
    assets.blockstate(M, 'sea_urchin', {'': {'model': R('block/sea_urchin')}})
    assets.block_item(M, 'sea_urchin')
    table('sea_urchin', [pool([item_entry('urchin_spine', modifier=[
        OrderedDict(type='minecraft:set_count', count=OrderedDict(type='minecraft:uniform', max=2, min=1)), DECAY])])])
    save(A.urchin_spine_item(), 'item/urchin_spine')
    assets.flat_item(M, 'urchin_spine')

    # barnacles: multiface layer like glow lichen
    save(A.barnacles(), 'block/barnacles')
    assets.from_vanilla(M, 'barnacles', 'glow_lichen', {'*': 'block/barnacles'}, item=None)
    assets.flat_item(M, 'barnacles', 'block/barnacles')
    mods = [OrderedDict(type='minecraft:set_count', add=True, condition=state_is('barnacles', **{d: True}), count=1)
            for d in ('down', 'up', 'north', 'south', 'west', 'east')]
    mods += [OrderedDict(type='minecraft:set_count', add=True, count=-1), DECAY]
    table('barnacles', [pool([item_entry('barnacles', condition=any_of(SHEARS, SILK), modifier=mods)])])

    # seashells (1-4) and sea glass (1-3) lying on the ground
    save(A.seashell_atlas(), 'block/seashells')
    save(A.sea_glass_atlas(), 'block/sea_glass_scatter')
    shells = [((0, 0, 4, 4), (6, 0, 8), 4, 4), ((4, 0, 8, 4), (10, 0, 3), 4, 4), ((8, 0, 12, 5), (3, 0, 3), 4, 5),
              ((12, 0, 15, 4), (11, 0, 11), 3, 4)]
    # each shell is its top-view sprite extruded row by row, so the sides follow the shell's outline; the sides show
    # the shell's side colour (texture row 6)
    for n in range(1, 5):
        els = []
        for (uv, (x, _, z), w, d) in shells[:n]:
            h = 1.5 if w == 4 else 1
            els += extrude.flat('raft:block/seashells', '#shell', uv, at=(x, 0, z), height=h, side_row=6)
        model(f'seashells_{n}', OrderedDict(parent='minecraft:block/block', ambientocclusion=False,
                                            textures={'shell': R('block/seashells'), 'particle': R('block/seashells')},
                                            elements=els))
    assets.blockstate(M, 'seashells', OrderedDict((f'shells={n}', {'model': R(f'block/seashells_{n}')})
                                                  for n in range(1, 5)))
    pebbles = [((0, 0, 4, 4), (6, 0, 7)), ((5, 0, 9, 4), (2, 0, 2)), ((10, 0, 14, 4), (10, 0, 11))]
    for n in range(1, 4):
        els = []
        for k, (uv, (x, _, z)) in enumerate(pebbles[:n]):
            side_uv = (uv[0], 5, uv[2], 6)
            els.append(el((x, 0, z), (x + 3, 1, z + 3), OrderedDict([
                ('up', face('#glass', uv)), ('down', face('#glass', uv, cullface='down')),
                ('north', face('#glass', side_uv)), ('south', face('#glass', side_uv)),
                ('west', face('#glass', side_uv)), ('east', face('#glass', side_uv))])))
        model(f'sea_glass_scatter_{n}', OrderedDict(parent='minecraft:block/block', ambientocclusion=False,
                                                    textures={'glass': R('block/sea_glass_scatter'),
                                                              'particle': R('block/sea_glass_scatter')},
                                                    elements=els))
    assets.blockstate(M, 'sea_glass_scatter', OrderedDict((f'pebbles={n}', {'model': R(f'block/sea_glass_scatter_{n}')})
                                                          for n in range(1, 4)))
    count_by_state('seashells', 'seashell', 'shells', 4)
    count_by_state('sea_glass_scatter', 'sea_glass_shard', 'pebbles', 3)
    save(A.seashell_item(), 'item/seashell')
    save(A.sea_glass_item(), 'item/sea_glass_shard')
    save(A.pearl_item(), 'item/pearl')
    for i in ('seashell', 'sea_glass_shard', 'pearl'):
        assets.flat_item(M, i)
    assets.recipe_shapeless(M, 'bone_meal_from_seashell', ['seashell'], 'minecraft:bone_meal', category='misc')

    assets.tag(M, 'block', 'minecraft:mineable/pickaxe', ['giant_clam', 'barnacles'])
    assets.tag(M, 'block', 'minecraft:washed_away_by_fluids', ['glow_kelp', 'glow_kelp_plant'])
    assets.tag(M, 'block', 'minecraft:replaceable_by_trees', ['barnacles'])
    assets.tag(M, 'block', 'minecraft:inside_step_sound_blocks', ['barnacles', 'seashells', 'sea_glass_scatter'])
    assets.tag(M, 'block', 'minecraft:dangerous_for_teleportation', ['sea_urchin'])
    assets.tag(M, 'block', 'minecraft:happy_ghast_avoids', ['sea_urchin'])


# --------------------------------------------------------------------------------------------- lang
NAMES = {
    'block.raft.shell_sand': 'Shell Sand', 'block.raft.black_sand': 'Black Sand',
    'block.raft.volcanic_ash': 'Volcanic Ash', 'block.raft.pumice': 'Pumice', 'block.raft.bubble_vent': 'Bubble Vent',
    'block.raft.beach_grass': 'Beach Grass', 'block.raft.sea_oats': 'Sea Oats', 'block.raft.hibiscus': 'Hibiscus',
    'block.raft.bird_of_paradise': 'Bird of Paradise', 'block.raft.tropical_fern': 'Tropical Fern',
    'block.raft.pineapple_bush': 'Pineapple Bush', 'block.raft.pineapple_crop': 'Pineapples',
    'block.raft.plumeria': 'Plumeria', 'block.raft.beach_morning_glory': 'Beach Morning Glory',
    'block.raft.tropical_orchid': 'Tropical Orchid', 'block.raft.spider_lily': 'Spider Lily',
    'block.raft.heliconia': 'Heliconia', 'block.raft.aloe_vera': 'Aloe Vera',
    'block.raft.potted_plumeria': 'Potted Plumeria', 'block.raft.potted_tropical_orchid': 'Potted Tropical Orchid',
    'block.raft.potted_aloe_vera': 'Potted Aloe Vera',
    'block.raft.potted_hibiscus': 'Potted Hibiscus', 'block.raft.potted_tropical_fern': 'Potted Tropical Fern',
    'block.raft.potted_palm_sapling': 'Potted Palm Sapling', 'block.raft.glow_kelp': 'Glow Kelp',
    'block.raft.glow_kelp_plant': 'Glow Kelp Plant', 'block.raft.sea_anemone_red': 'Red Sea Anemone',
    'block.raft.sea_anemone_purple': 'Purple Sea Anemone', 'block.raft.sea_anemone_glow': 'Glowing Sea Anemone',
    'block.raft.giant_clam': 'Giant Clam', 'block.raft.starfish_orange': 'Orange Starfish',
    'block.raft.starfish_purple': 'Purple Starfish', 'block.raft.starfish_red': 'Red Starfish',
    'block.raft.sea_urchin': 'Sea Urchin', 'block.raft.barnacles': 'Barnacles', 'block.raft.tube_worms': 'Tube Worms',
    'block.raft.seashells': 'Seashells', 'block.raft.sea_glass_scatter': 'Sea Glass',
    'item.raft.pineapple': 'Pineapple', 'item.raft.pineapple_crown': 'Pineapple Crown', 'item.raft.pearl': 'Pearl',
    'item.raft.seashell': 'Seashell', 'item.raft.sea_glass_shard': 'Sea Glass Shard',
    'item.raft.urchin_spine': 'Urchin Spine',
    'itemgroup.raft.nature': 'Beach Craft: Nature',
    'tooltip.raft.bubble_vent': 'Makes the water above it bubble upwards. Divers catch their breath in the bubbles.',
    'tooltip.raft.pineapple_bush': 'Grows wild pineapples. Pick them when they are golden.',
    'tooltip.raft.pineapple_crown': 'Plant it on farmland or a crop plot to grow pineapples.',
    'tooltip.raft.giant_clam': 'Opens and closes under water. Some hide a pearl.',
    'tooltip.raft.giant_clam.2': 'Use an open clam to take its pearl. A new one grows in time.',
    'tooltip.raft.sea_urchin': 'Prickly! It hurts to touch.',
    'tooltip.raft.pearl': 'A shiny pearl from a giant clam.',
    'tooltip.raft.urchin_spine': 'Sharp and light. Good for hooks and harpoon tips.',
}


def write(module):
    assert module == M
    ground()
    island_plants()
    sea_life()
    assets.lang(M, NAMES)
