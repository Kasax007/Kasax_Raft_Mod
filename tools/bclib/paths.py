import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
MODULES = os.path.join(ROOT, 'src', 'main', 'modules')
RESOURCES = os.path.join(ROOT, 'src', 'main', 'resources')
GENERATED = os.path.join(ROOT, 'src', 'main', 'generated')
# Vanilla 26.3 assets/data extracted from the client jar (textures, models, worldgen JSON, structures).
VANILLA = os.environ.get('BC_VANILLA', '/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad/vanilla')
NS = 'raft'


def module_dir(module):
    return os.path.join(MODULES, module)


def out(module, *parts):
    """Path inside a module resource folder, parent dirs created."""
    p = os.path.join(MODULES, module, *parts)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def find_texture(ident):
    """'minecraft:block/stone' or 'raft:block/x' -> png path (modules, resources, vanilla) or None."""
    ns, path = ident.split(':', 1) if ':' in ident else ('minecraft', ident)
    cands = []
    if ns == NS:
        if os.path.isdir(MODULES):
            for m in sorted(os.listdir(MODULES)):
                cands.append(os.path.join(MODULES, m, 'assets', ns, 'textures', path + '.png'))
        cands.append(os.path.join(RESOURCES, 'assets', ns, 'textures', path + '.png'))
    else:
        cands.append(os.path.join(VANILLA, 'assets', ns, 'textures', path + '.png'))
    for c in cands:
        if os.path.isfile(c):
            return c
    return None
