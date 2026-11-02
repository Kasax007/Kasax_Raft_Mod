"""Writers for Minecraft 26.3 resource/data JSON, into src/main/modules/<module>/.

Every function takes the module name first. Ids without namespace are 'raft:'. Lang and tags are written as
fragments and merged at build time (build.gradle mergeResources), so several modules may add to the same tag.
Formats follow the files the game itself uses (see src/main/generated and the vanilla extraction in paths.VANILLA).
"""
import json
import os
from collections import OrderedDict

from .paths import out, NS

_lang = {}   # module -> dict
_tags = {}   # (module, registry, tag) -> list


def rid(x):
    return x if ':' in x else NS + ':' + x


def write_json(module, rel, data):
    p = out(module, *rel.split('/'))
    with open(p, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')
    return p


# ----------------------------------------------------------------------------------------------- models
def block_model(module, name, parent, textures=None, **extra):
    """assets/raft/models/block/<name>.json. parent e.g. 'minecraft:block/cube_all'."""
    d = OrderedDict(parent=parent)
    if textures:
        d['textures'] = {k: rid(v) for k, v in textures.items()}
    d.update(extra)
    return write_json(module, f'assets/{NS}/models/block/{name}.json', d)


def item_model(module, name, parent='minecraft:item/generated', textures=None, **extra):
    d = OrderedDict(parent=parent)
    if textures:
        d['textures'] = {k: rid(v) for k, v in textures.items()}
    d.update(extra)
    return write_json(module, f'assets/{NS}/models/item/{name}.json', d)


def item_def(module, name, model=None, tints=None, extra=None):
    """assets/raft/items/<name>.json, the 26.3 item model definition. model defaults to raft:item/<name>."""
    m = OrderedDict(type='minecraft:model', model=rid(model or f'item/{name}'))
    if tints:
        m['tints'] = tints
    d = OrderedDict(model=m)
    if extra:
        d.update(extra)
    return write_json(module, f'assets/{NS}/items/{name}.json', d)


def flat_item(module, name, texture=None):
    """Item drawn from one 16x16 sprite: textures/item/<name>.png."""
    item_model(module, name, textures={'layer0': texture or f'item/{name}'})
    item_def(module, name)


def handheld_item(module, name, texture=None):
    item_model(module, name, parent='minecraft:item/handheld', textures={'layer0': texture or f'item/{name}'})
    item_def(module, name)


def block_item(module, name, model=None):
    """Item that shows the block model (default raft:block/<name>)."""
    item_def(module, name, model=model or f'block/{name}')


def spawn_egg(module, name, texture=None):
    """<name> e.g. 'crab_spawn_egg'. 26.3 spawn eggs are plain flat items with their own sprite."""
    flat_item(module, name, texture)


# ----------------------------------------------------------------------------------------------- blockstates
def blockstate(module, name, variants=None, multipart=None):
    d = OrderedDict()
    if variants is not None:
        d['variants'] = variants
    if multipart is not None:
        d['multipart'] = multipart
    return write_json(module, f'assets/{NS}/blockstates/{name}.json', d)


def simple_block(module, name, texture=None, render_type=None):
    """Full cube with one texture + blockstate + item."""
    block_model(module, name, 'minecraft:block/cube_all', {'all': texture or f'block/{name}'})
    blockstate(module, name, {'': {'model': rid(f'block/{name}')}})
    block_item(module, name)


def column_block(module, name, side=None, end=None, axis=True):
    """Pillar/log-like block. axis=True writes axis=x|y|z variants."""
    block_model(module, name, 'minecraft:block/cube_column',
                {'side': side or f'block/{name}', 'end': end or f'block/{name}_top'})
    if axis:
        block_model(module, name + '_horizontal', 'minecraft:block/cube_column_horizontal',
                    {'side': side or f'block/{name}', 'end': end or f'block/{name}_top'})
        m, h = rid(f'block/{name}'), rid(f'block/{name}_horizontal')
        blockstate(module, name, {'axis=y': {'model': m}, 'axis=z': {'model': h, 'x': 90},
                                  'axis=x': {'model': h, 'x': 90, 'y': 90}})
    else:
        blockstate(module, name, {'': {'model': rid(f'block/{name}')}})
    block_item(module, name)


def horizontal_variants(model, base_rot=0, extra=''):
    """facing=north|east|south|west variants of one model whose front faces north."""
    v = OrderedDict()
    for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        key = f'facing={facing}' + (',' + extra if extra else '')
        e = {'model': rid(model)}
        r = (rot + base_rot) % 360
        if r:
            e['y'] = r
        v[key] = e
    return v


def from_vanilla(module, name, vanilla, textures, item='auto'):
    """Copy a vanilla block family member (blockstate + all its models) and swap the textures.

    vanilla: e.g. 'oak_stairs', 'oak_door', 'oak_trapdoor', 'oak_fence_gate', 'oak_button', 'oak_pressure_plate',
    'lantern', 'chain', 'ladder', 'iron_bars', 'sea_pickle', 'glow_lichen' ... textures: maps the texture keys of the
    vanilla models ('texture', 'bottom', 'top', 'side', 'particle', ...) to new ids; missing keys fall back to
    textures['*'] if given. item: 'auto' copies the vanilla item definition (renamed), None skips it.
    """
    import json as _json
    from .paths import VANILLA
    vbase = os.path.join(VANILLA, 'assets', 'minecraft')
    bs = _json.load(open(os.path.join(vbase, 'blockstates', vanilla + '.json')))
    pref = 'minecraft:block/' + vanilla
    seen = set()

    def remap(model_ref):
        if model_ref.startswith(pref):
            new = rid('block/' + name + model_ref[len(pref):])
            seen.add(model_ref)
            return new
        return model_ref

    def walk(o):
        if isinstance(o, dict):
            return {k: (remap(v) if k == 'model' and isinstance(v, str) else walk(v)) for k, v in o.items()}
        if isinstance(o, list):
            return [walk(x) for x in o]
        return o

    write_json(module, f'assets/{NS}/blockstates/{name}.json', walk(bs))
    item_models = set()
    if item == 'auto':
        ip = os.path.join(vbase, 'items', vanilla + '.json')
        if os.path.isfile(ip):
            idef = _json.load(open(ip))
            txt = _json.dumps(idef)
            for kind in ('block', 'item'):
                vp = f'minecraft:{kind}/{vanilla}'
                if vp in txt:
                    import re as _re
                    for m in set(_re.findall(_re.escape(vp) + r'[a-z0-9_]*', txt)):
                        if kind == 'block':
                            seen.add(m)
                        else:
                            item_models.add(m)
                    txt = txt.replace(vp, rid(f'{kind}/{name}'))
            write_json(module, f'assets/{NS}/items/{name}.json', _json.loads(txt))

    def copy_model(kind, ref):
        mp = os.path.join(vbase, 'models', kind, ref.split('/', 1)[1] + '.json')
        md = _json.load(open(mp))
        if 'textures' in md:
            nt = {}
            for k, v in md['textures'].items():
                if k in textures:
                    nt[k] = rid(textures[k])
                elif '*' in textures and not v.startswith('#'):
                    nt[k] = rid(textures['*'])
                else:
                    nt[k] = v
            md['textures'] = nt
        suffix = ref.split('/', 1)[1][len(vanilla):]
        write_json(module, f'assets/{NS}/models/{kind}/{name}{suffix}.json', md)

    for ref in sorted(seen):
        copy_model('block', ref)
    for ref in sorted(item_models):
        copy_model('item', ref)


def stairs(module, name, texture):
    from_vanilla(module, name, 'oak_stairs', {'*': texture})


def door(module, name, bottom_tex, top_tex, item_tex=None):
    """Door; the item sprite is textures/item/<name>.png unless item_tex is given."""
    from_vanilla(module, name, 'oak_door', {'bottom': bottom_tex, 'top': top_tex}, item=None)
    flat_item(module, name, item_tex)


def trapdoor(module, name, texture):
    from_vanilla(module, name, 'oak_trapdoor', {'*': texture})


def fence_gate(module, name, texture):
    from_vanilla(module, name, 'oak_fence_gate', {'*': texture})


def button(module, name, texture):
    from_vanilla(module, name, 'oak_button', {'*': texture})


def pressure_plate(module, name, texture):
    from_vanilla(module, name, 'oak_pressure_plate', {'*': texture})


def slab(module, name, texture, full_block_model):
    t = {'bottom': texture, 'top': texture, 'side': texture}
    block_model(module, name, 'minecraft:block/slab', t)
    block_model(module, name + '_top', 'minecraft:block/slab_top', t)
    blockstate(module, name, {'type=bottom': {'model': rid(f'block/{name}')},
                              'type=top': {'model': rid(f'block/{name}_top')},
                              'type=double': {'model': rid(full_block_model)}})
    block_item(module, name)


def wall(module, name, texture):
    block_model(module, name + '_post', 'minecraft:block/template_wall_post', {'wall': texture})
    block_model(module, name + '_side', 'minecraft:block/template_wall_side', {'wall': texture})
    block_model(module, name + '_side_tall', 'minecraft:block/template_wall_side_tall', {'wall': texture})
    block_model(module, name + '_inventory', 'minecraft:block/wall_inventory', {'wall': texture})
    mp = [{'when': {'up': 'true'}, 'apply': {'model': rid(f'block/{name}_post')}}]
    for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for h, suf in (('low', '_side'), ('tall', '_side_tall')):
            a = {'model': rid(f'block/{name}{suf}'), 'uvlock': True}
            if y:
                a['y'] = y
            mp.append({'when': {d: h}, 'apply': a})
    blockstate(module, name, multipart=mp)
    item_def(module, name, f'block/{name}_inventory')


def fence(module, name, texture):
    block_model(module, name + '_post', 'minecraft:block/fence_post', {'texture': texture})
    block_model(module, name + '_side', 'minecraft:block/fence_side', {'texture': texture})
    block_model(module, name + '_inventory', 'minecraft:block/fence_inventory', {'texture': texture})
    mp = [{'apply': {'model': rid(f'block/{name}_post')}}]
    for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        a = {'model': rid(f'block/{name}_side'), 'uvlock': True}
        if y:
            a['y'] = y
        mp.append({'when': {d: 'true'}, 'apply': a})
    blockstate(module, name, multipart=mp)
    item_def(module, name, f'block/{name}_inventory')


def pane(module, name, pane_texture, edge_texture):
    """Glass-pane/iron-bars style block (IronBarsBlock): north/east/south/west booleans."""
    t = {'pane': pane_texture, 'edge': edge_texture}
    for suf, parent in (('_post', 'template_glass_pane_post'), ('_side', 'template_glass_pane_side'),
                        ('_side_alt', 'template_glass_pane_side_alt'), ('_noside', 'template_glass_pane_noside'),
                        ('_noside_alt', 'template_glass_pane_noside_alt')):
        block_model(module, name + suf, 'minecraft:block/' + parent, t)
    m = lambda s: rid(f'block/{name}{s}')
    mp = [{'apply': {'model': m('_post')}},
          {'when': {'north': 'true'}, 'apply': {'model': m('_side')}},
          {'when': {'east': 'true'}, 'apply': {'model': m('_side'), 'y': 90}},
          {'when': {'south': 'true'}, 'apply': {'model': m('_side_alt')}},
          {'when': {'west': 'true'}, 'apply': {'model': m('_side_alt'), 'y': 90}},
          {'when': {'north': 'false'}, 'apply': {'model': m('_noside')}},
          {'when': {'east': 'false'}, 'apply': {'model': m('_noside_alt')}},
          {'when': {'south': 'false'}, 'apply': {'model': m('_noside_alt'), 'y': 90}},
          {'when': {'west': 'false'}, 'apply': {'model': m('_noside'), 'y': 270}}]
    blockstate(module, name, multipart=mp)
    item_model(module, name, textures={'layer0': pane_texture})
    item_def(module, name)


def cross_plant(module, name, texture=None, item_texture=None, tinted=False):
    """Cross-shaped plant (flowers, grass, saplings): model block/cross, flat item."""
    block_model(module, name, 'minecraft:block/tinted_cross' if tinted else 'minecraft:block/cross',
                {'cross': texture or f'block/{name}'})
    blockstate(module, name, {'': {'model': rid(f'block/{name}')}})
    flat_item(module, name, item_texture or texture or f'block/{name}')


def potted(module, name, plant_texture):
    """potted_<plant> block: model flower_pot_cross. Register a FlowerPotBlock without item."""
    block_model(module, name, 'minecraft:block/flower_pot_cross', {'plant': plant_texture})
    blockstate(module, name, {'': {'model': rid(f'block/{name}')}})


# ----------------------------------------------------------------------------------------------- data
def recipe_shaped(module, name, pattern, key, result, count=1, category='misc'):
    return write_json(module, f'data/{NS}/recipe/{name}.json', OrderedDict(
        type='minecraft:crafting_shaped', category=category,
        key={k: (v if isinstance(v, (list, dict)) or v.startswith('#') else rid(v)) for k, v in key.items()},
        pattern=pattern, result=OrderedDict(count=count, id=rid(result))))


def recipe_shapeless(module, name, ingredients, result, count=1, category='misc'):
    ing = [(i if isinstance(i, (list, dict)) or i.startswith('#') else rid(i)) for i in ingredients]
    return write_json(module, f'data/{NS}/recipe/{name}.json', OrderedDict(
        type='minecraft:crafting_shapeless', category=category, ingredients=ing,
        result=OrderedDict(count=count, id=rid(result))))


def recipe_cooking(module, name, ingredient, result, xp=0.1, time=200, kind='smelting', category='misc'):
    """kind: smelting | blasting | smoking | campfire_cooking"""
    return write_json(module, f'data/{NS}/recipe/{name}.json', OrderedDict(
        type='minecraft:' + kind, category=category, cookingtime=time, experience=xp,
        ingredient=ingredient if ingredient.startswith('#') else rid(ingredient), result={'id': rid(result)}))


def recipe_stonecutting(module, name, ingredient, result, count=1):
    return write_json(module, f'data/{NS}/recipe/{name}.json', OrderedDict(
        type='minecraft:stonecutting', ingredient=rid(ingredient), result=OrderedDict(count=count, id=rid(result))))


def loot_self(module, block, item=None):
    """Block drops itself (or item) when it survives the explosion."""
    return loot_block(module, block, [{'type': 'minecraft:item', 'name': rid(item or block)}],
                      conditions=[{'condition': 'minecraft:survives_explosion'}])


def loot_block(module, block, entries, conditions=None, functions=None):
    pool = OrderedDict(bonus_rolls=0.0)
    if conditions:
        pool['conditions'] = conditions
    pool['entries'] = entries
    if functions:
        pool['functions'] = functions
    pool['rolls'] = 1.0
    return write_json(module, f'data/{NS}/loot_table/blocks/{block}.json', OrderedDict(
        type='minecraft:block', pools=[pool], random_sequence=f'{NS}:blocks/{block}'))


def loot_slab(module, block):
    return write_json(module, f'data/{NS}/loot_table/blocks/{block}.json', {
        'type': 'minecraft:block', 'pools': [{'bonus_rolls': 0.0, 'entries': [{
            'type': 'minecraft:item', 'name': rid(block), 'functions': [
                {'function': 'minecraft:set_count', 'add': False, 'count': 2.0,
                 'conditions': [{'condition': 'minecraft:block_state_property', 'block': rid(block),
                                 'properties': {'type': 'double'}}]},
                {'function': 'minecraft:explosion_decay'}]}], 'rolls': 1.0}],
        'random_sequence': f'{NS}:blocks/{block}'})


def loot_door(module, block):
    """Only the lower half drops the door."""
    return loot_block(module, block, [{'type': 'minecraft:item', 'name': rid(block), 'conditions': [
        {'condition': 'minecraft:block_state_property', 'block': rid(block), 'properties': {'half': 'lower'}}]}],
                      conditions=[{'condition': 'minecraft:survives_explosion'}])


def loot_table(module, path, table):
    """Any loot table, e.g. path='chests/old_lighthouse'."""
    return write_json(module, f'data/{NS}/loot_table/{path}.json', table)


def tag(module, registry, tag_id, values):
    """Add values to a tag fragment. registry: block | item | entity_type | worldgen/biome | worldgen/structure ...
    tag_id: 'minecraft:mineable/axe' or 'raft:has_structure/x'. Call flush(module) at the end."""
    key = (module, registry, rid(tag_id))
    _tags.setdefault(key, [])
    for v in values:
        v = v if isinstance(v, dict) or v.startswith('#') else rid(v)
        if v not in _tags[key]:
            _tags[key].append(v)


def lang(module, entries):
    """Add en_us lang entries. Call flush(module) at the end."""
    _lang.setdefault(module, OrderedDict()).update(entries)


def flush(module):
    """Write collected tags and lang of a module (overwrites the module's previous fragments)."""
    for (m, registry, tag_id), values in _tags.items():
        if m != module:
            continue
        ns, path = tag_id.split(':', 1)
        write_json(module, f'data/{ns}/tags/{registry}/{path}.json', {'values': values})
    if module in _lang:
        p = out(module, 'assets', NS, 'lang', 'en_us.json')
        old = {}
        data = OrderedDict(sorted({**old, **_lang[module]}.items()))
        with open(p, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')


def sounds(module, entries):
    """assets/raft/sounds.json fragment: {'event_name': {'sounds': ['raft:x'], 'subtitle': 'subtitles.raft.x'}}"""
    return write_json(module, f'assets/{NS}/sounds.json', entries)
