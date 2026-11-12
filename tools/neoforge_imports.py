"""Point Fabric API / Team Reborn Energy references at the NeoForge loader layer (net.kasax.raft.platform).
Usage: python3 tools/neoforge_imports.py src  (rewrites main/java and gametest/java, skips the layer itself and the local
client game test shim under net/fabricmc/)."""
import os, re, sys
root = sys.argv[1]
pat = re.compile(r'net\.fabricmc\.fabric\.api\.((?:[a-z0-9_]+\.)+)([A-Z]\w*)')
tr = re.compile(r'team\.reborn\.energy\.api\.(?:base\.)?([A-Z]\w*)')
changed = 0
for sub in ('main/java', 'gametest/java'):
    for dp, dn, fn in os.walk(os.path.join(root, sub)):
        for f in fn:
            if not f.endswith('.java'):
                continue
            p = os.path.join(dp, f)
            if '/platform/' in p or '/net/fabricmc/' in p or '/datagen/' in p or f == 'RaftDataGenerator.java':
                continue
            s = open(p).read(); o = s
            def rep(m):
                pkg, cls = m.group(1), m.group(2)
                if pkg.startswith('client.gametest.'):
                    return m.group(0)
                if pkg.startswith('client.') or pkg.startswith('blockrenderlayer.'):
                    return 'net.kasax.raft.platform.client.' + cls
                if pkg.startswith('transfer.') or pkg.startswith('lookup.'):
                    return 'net.kasax.raft.platform.transfer.' + cls
                return 'net.kasax.raft.platform.' + cls
            s = pat.sub(rep, s)
            s = tr.sub(lambda m: 'net.kasax.raft.platform.energy.' + m.group(1), s)
            s = s.replace('net.fabricmc.loader.api.FabricLoader', 'net.kasax.raft.platform.FabricLoader')
            s = re.sub(r'^import net\.fabricmc\.api\.(Environment|EnvType);\n', '', s, flags=re.M)
            s = re.sub(r'^[ \t]*@Environment\(EnvType\.CLIENT\)[ \t]*\n', '', s, flags=re.M)
            seen, out = set(), []
            for l in s.split('\n'):
                if l.startswith('import ') and l in seen:
                    continue
                if l.startswith('import '):
                    seen.add(l)
                out.append(l)
            s = '\n'.join(out)
            if s != o:
                open(p, 'w').write(s); changed += 1
print('changed', changed)
