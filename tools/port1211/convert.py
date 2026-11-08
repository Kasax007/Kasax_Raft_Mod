"""Converts Beach Craft's resources from the Minecraft 26.3 formats (what tools/content/*/gen.py and the datagen
write) to Minecraft 1.21.1, in place, in src/main/{resources,generated,modules/*}.

Run from tools/:  python3 -m port1211.convert [--vanilla <dir with v26 data>]
Already converted files are recognised and left alone, so it can run again after a generator has rewritten files.
The vanilla 26.3 surface rules and conditions the mod refers to are kept in port1211/vanilla_material.json.
"""
import json
import os
import shutil
import sys

from . import advancement, density, feature, fixups, loot, models, nbt121, noise, recipe, worldgen

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.normpath(os.path.join(HERE, '..', '..', 'src', 'main'))


def roots():
    out = [os.path.join(SRC, 'resources'), os.path.join(SRC, 'generated')]
    mods = os.path.join(SRC, 'modules')
    out += [os.path.join(mods, m) for m in sorted(os.listdir(mods)) if os.path.isdir(os.path.join(mods, m))]
    return [r for r in out if os.path.isdir(r)]


def load(p):
    with open(p, encoding='utf-8') as f:
        return json.load(f)


def save(p, j):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8') as f:
        json.dump(j, f, indent=2, ensure_ascii=False)
        f.write('\n')


def files(root, sub):
    base = os.path.join(root, sub)
    for dp, dn, fn in os.walk(base):
        for f in sorted(fn):
            yield os.path.join(dp, f), os.path.relpath(os.path.join(dp, f), root)


def is_121_loot(j):
    s = json.dumps(j)
    return '"function"' in s or '"conditions"' in s or ('"modifier"' not in s and '"condition"' not in s and '"type": "minecraft:' not in s.split('"pools"', 1)[-1])


def convert_data(root, stats, extras):
    data = os.path.join(root, 'data')
    if not os.path.isdir(data):
        return
    for ns in sorted(os.listdir(data)):
        nsdir = os.path.join(data, ns)
        # recipes
        for p, rel in files(nsdir, 'recipe'):
            j = load(p)
            save(p, recipe.recipe(j)); stats['recipe'] += 1
        for p, rel in files(nsdir, 'loot_table'):
            j = load(p)
            if '"function"' in json.dumps(j) or '"conditions"' in json.dumps(j):
                continue
            save(p, loot.table(j)); stats['loot'] += 1
        for p, rel in files(nsdir, 'advancement'):
            save(p, advancement.advancement(load(p))); stats['advancement'] += 1
        wg = os.path.join(nsdir, 'worldgen')
        # noises and density functions
        for p, rel in files(wg, 'noise'):
            save(p, noise.noise(load(p))); stats['noise'] += 1
        for p, rel in files(wg, 'density_function'):
            j = load(p)
            if json.dumps(j).find('"argument') >= 0 and '"left"' not in json.dumps(j) and '"input"' not in json.dumps(j):
                continue
            extras.setdefault('density_raw', {})[ns + ':' + os.path.relpath(p, os.path.join(wg, 'density_function'))[:-5]] = j
            save(p, density.df(j)); stats['density'] += 1
        # configured features: 26.3 'feature' folder -> 'configured_feature'
        for p, rel in list(files(wg, 'feature')):
            j = load(p)
            target = os.path.join(wg, 'configured_feature', os.path.relpath(p, os.path.join(wg, 'feature')))
            save(target, feature.configured(j)); os.remove(p); stats['feature'] += 1
        shutil.rmtree(os.path.join(wg, 'feature'), ignore_errors=True)
        for p, rel in files(wg, 'placed_feature'):
            save(p, feature.placed(load(p))); stats['placed'] += 1
        for p, rel in files(wg, 'processor_list'):
            save(p, processor_list(load(p))); stats['processor'] += 1
        for p, rel in files(wg, 'biome'):
            j = load(p)
            if 'attributes' not in j and 'spawners' in j:
                continue
            ex = {}
            save(p, worldgen.biome(j, ex)); stats['biome'] += 1
            bid = ns + ':' + os.path.relpath(p, os.path.join(wg, 'biome'))[:-5]
            if ex.get('underwater_music'):
                extras.setdefault('underwater_music', []).append(bid)
            if 'water_fog_factor' in ex:
                extras.setdefault('water_fog', {})[bid] = ex['water_fog_factor']
        for p, rel in files(nsdir, 'dimension_type'):
            j = load(p)
            if 'attributes' in j or 'default_clock' in j:
                save(p, worldgen.dimension_type(j)); stats['dimension_type'] += 1
        # noise settings come last (they need the material rules and density functions of all roots)
        for p, rel in files(wg, 'noise_settings'):
            extras.setdefault('noise_settings', []).append((ns, p))
        for p, rel in files(wg, 'material_rule'):
            extras.setdefault('material_rules', {})[ns + ':' + os.path.relpath(p, os.path.join(wg, 'material_rule'))[:-5]] = (p, load(p))
        for p, rel in files(wg, 'material_condition'):
            extras.setdefault('material_rules', {})[ns + ':' + os.path.relpath(p, os.path.join(wg, 'material_condition'))[:-5]] = (p, load(p))
        # loot: the newer game's "filtered ... on_fail discard" (drop a map that found nothing) has no 1.21.1 form
        for p, rel in files(nsdir, 'loot_table'):
            j = load(p)
            f = fixups.loot(j)
            if f != j:
                save(p, f); stats['fixups'] = stats.get('fixups', 0) + 1
        for sub in ('recipe', 'advancement'):
            for p, rel in files(nsdir, sub):
                j = load(p)
                f = fixups.items(j)
                if f != j:
                    save(p, f); stats['fixups'] = stats.get('fixups', 0) + 1
        for p, rel in files(nsdir, 'tags'):
            j = load(p)
            f = fixups.tag(j)
            if f != j:
                save(p, f); stats['fixups'] = stats.get('fixups', 0) + 1
        for sub in ('configured_feature', 'placed_feature', 'processor_list', 'structure'):
            for p, rel in files(wg, sub):
                j = load(p)
                f = fixups.walk(j, sub)
                if f != j:
                    save(p, f); stats['fixups'] = stats.get('fixups', 0) + 1
        for p, rel in files(nsdir, 'structure'):
            if p.endswith('.nbt'):
                if nbt121.convert(p):
                    stats['nbt'] += 1


def processor_list(j):
    out = dict(j)
    procs = []
    for pr in j.get('processors', []):
        pr = dict(pr)
        if pr.get('processor_type') == 'minecraft:rule':
            rules = []
            for r in pr.get('rules', []):
                r = dict(r)
                r['output_state'] = feature.state(r['output_state'])
                rules.append(r)
            pr['rules'] = rules
        procs.append(pr)
    out['processors'] = procs
    return out


def finish_noise_settings(extras, stats):
    rules_raw = extras.get('material_rules', {})
    mod_rules = {k: v[1] for k, v in rules_raw.items()}
    dens = extras.get('density_raw', {})
    vanilla_veins = load(os.path.join(HERE, 'vanilla_veins.json'))
    for ns, p in extras.get('noise_settings', []):
        j = load(p)
        if 'noise_router' in j and 'surface_rule' in j:
            continue
        has_veins = 'ore_vein' in json.dumps(j.get('material_rule')) or any(
            'ore_vein' in s for s in [json.dumps(_expand_refs(j.get('material_rule'), mod_rules))])
        save(p, worldgen.noise_settings(j, mod_rules, {'density': dens, 'has_ore_veins': has_veins, 'vanilla_veins': vanilla_veins}))
        stats['noise_settings'] += 1
    for k, (p, j) in rules_raw.items():
        if os.path.exists(p):
            os.remove(p)
    for r in roots():
        for ns_dir in [os.path.join(r, 'data', n) for n in os.listdir(os.path.join(r, 'data'))] if os.path.isdir(os.path.join(r, 'data')) else []:
            for sub in ('material_rule', 'material_condition'):
                d = os.path.join(ns_dir, 'worldgen', sub)
                if os.path.isdir(d) and not any(fn for _, _, fn in os.walk(d)):
                    shutil.rmtree(d)


def _expand_refs(r, mod_rules):
    if isinstance(r, str):
        return worldgen.V26_MATERIAL.get(('material_rule', r)) or mod_rules.get(r) or r
    return r


def convert_assets(root, stats, extras):
    assets = os.path.join(root, 'assets')
    if not os.path.isdir(assets):
        return
    for ns in sorted(os.listdir(assets)):
        nsdir = os.path.join(assets, ns)
        for p, rel in files(nsdir, 'models'):
            if p.endswith('.json'):
                j = load(p)
                k = models.model(j)
                if k != j:
                    save(p, k); stats['model'] += 1
        for p, rel in files(nsdir, 'lang'):
            j = load(p)
            f = fixups.lang(j)
            if f != j:
                save(p, f); stats['fixups'] = stats.get('fixups', 0) + 1
        for p, rel in list(files(nsdir, 'items')):
            item = os.path.relpath(p, os.path.join(nsdir, 'items'))[:-5]
            extras.setdefault('items', []).append((root, ns, item, load(p)))
        for p, rel in list(files(nsdir, 'equipment')):
            extras.setdefault('equipment', []).append((root, ns, os.path.relpath(p, os.path.join(nsdir, 'equipment'))[:-5], load(p), p))


def all_item_models():
    out = {}
    for r in roots():
        base = os.path.join(r, 'assets')
        if not os.path.isdir(base):
            continue
        for ns in os.listdir(base):
            d = os.path.join(base, ns, 'models', 'item')
            for dp, dn, fn in os.walk(d):
                for f in fn:
                    out[ns + ':item/' + os.path.relpath(os.path.join(dp, f), d)[:-5]] = os.path.join(dp, f)
    return out


def finish_items(extras, stats):
    existing = all_item_models()
    special = {}
    for root, ns, item, j in extras.get('items', []):
        m = j['model']
        target = os.path.join(root, 'assets', ns, 'models', 'item', item + '.json')
        own = '%s:item/%s' % (ns, item)
        if m.get('type') == 'minecraft:model':
            ref = m['model']
            if ref == own and own in existing:
                pass
            elif own in existing:
                # a model of that name exists already but the item points elsewhere: point it there
                cur = load(existing[own])
                if cur.get('parent') != ref:
                    save(existing[own], {'parent': ref})
            else:
                save(target, {'parent': ref})
                existing[own] = target
            if 'tints' in m:
                special[ns + ':' + item] = {'tints': m['tints']}
        else:
            # select/condition/range models: overrides on the item model (1.21.1), predicates registered in Java
            base_model, overrides = models.item_overrides(m)
            for o in overrides:
                if o['model'] == own and own in existing:
                    # the case shows the item's own model file, which now becomes the overrides file: keep it as _gui
                    gui = own + '_gui'
                    gui_path = os.path.join(os.path.dirname(existing[own]), os.path.basename(existing[own])[:-5] + '_gui.json')
                    if not os.path.exists(gui_path):
                        shutil.copyfile(existing[own], gui_path)
                    o['model'] = gui
            if base_model == own and own in existing:
                # the item's own model is the base: keep it as _base, the item model becomes the overrides file
                base_path = os.path.join(os.path.dirname(existing[own]), os.path.basename(existing[own])[:-5] + '_base.json')
                cur = load(existing[own])
                if not os.path.exists(base_path) and 'overrides' not in cur:
                    save(base_path, cur)
                base_model = own + '_base'
            data = {'parent': base_model, 'overrides': overrides}
            if own in existing and existing[own] != target:
                os.remove(existing[own])
            save(target, data)
            special[ns + ':' + item] = {'definition': m}
        p = os.path.join(root, 'assets', ns, 'items', item + '.json')
        if os.path.exists(p):
            os.remove(p)
        stats['item'] += 1
    for root in roots():
        for ns in os.listdir(os.path.join(root, 'assets')) if os.path.isdir(os.path.join(root, 'assets')) else []:
            d = os.path.join(root, 'assets', ns, 'items')
            if os.path.isdir(d) and not os.listdir(d):
                os.rmdir(d)
    extras['special_items'] = special


def finish_equipment(extras, stats):
    for root, ns, name, j, p in extras.get('equipment', []):
        layers = j.get('layers', {})
        for kind, number in (('humanoid', 1), ('humanoid_leggings', 2)):
            for layer in layers.get(kind, []):
                tex = layer['texture']
                tns, tpath = tex.split(':', 1) if ':' in tex else ('minecraft', tex)
                src = None
                for r in roots():
                    cand = os.path.join(r, 'assets', tns, 'textures', 'entity', 'equipment', kind, tpath + '.png')
                    if os.path.exists(cand):
                        src = cand
                if src is None:
                    raise ValueError('no texture for equipment %s:%s %s' % (ns, name, tex))
                dst = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(src)))), 'models', 'armor',
                                   '%s_layer_%d.png' % (name, number))
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                shutil.copyfile(src, dst)
        os.remove(p)
        stats['equipment'] += 1


def main():
    vanilla_material = load(os.path.join(HERE, 'vanilla_material.json'))
    worldgen.V26_MATERIAL = {(k.split('|')[0], k.split('|')[1]): v for k, v in vanilla_material.items()}
    stats = {k: 0 for k in ('recipe', 'loot', 'advancement', 'noise', 'density', 'feature', 'placed', 'processor', 'biome',
                            'dimension_type', 'noise_settings', 'nbt', 'model', 'item', 'equipment')}
    extras = {}
    for r in roots():
        convert_data(r, stats, extras)
        convert_assets(r, stats, extras)
    finish_noise_settings(extras, stats)
    finish_items(extras, stats)
    from . import layers
    stats['render_layers'] = layers.build(roots(), os.path.join(SRC, 'resources', 'assets', 'raft', 'port1211', 'render_layers.json'))
    finish_equipment(extras, stats)
    # mixin configs (src/main/resources/*.mixins.json)
    for fn in sorted(os.listdir(os.path.join(SRC, 'resources'))):
        if fn.endswith('.mixins.json'):
            p = os.path.join(SRC, 'resources', fn)
            j = load(p)
            f = fixups.mixin_config(j)
            if f != j:
                save(p, f); stats['fixups'] = stats.get('fixups', 0) + 1
    # facts other parts of the mod need
    world = os.path.join(SRC, 'modules', 'world')
    # biomes converted in an earlier run are skipped: add to what is there instead of replacing it
    if extras.get('underwater_music'):
        p = os.path.join(world, 'data', 'minecraft', 'tags', 'worldgen', 'biome', 'plays_underwater_music.json')
        old = load(p).get('values', []) if os.path.exists(p) else []
        save(p, {'values': sorted(set(old) | set(extras['underwater_music']))})
    if extras.get('water_fog'):
        p = os.path.join(world, 'assets', 'raft', 'port1211', 'water_fog.json')
        old = load(p) if os.path.exists(p) else {}
        old.update(extras['water_fog'])
        save(p, dict(sorted(old.items())))
    print(json.dumps(stats))
    if extras.get('special_items'):
        print('items with special models (need Java item properties):', json.dumps(extras['special_items'])[:2000])


if __name__ == '__main__':
    main()
