"""Pairs WHERE lines with the following vessel Pos/Rotation outputs and prints the player's position in the raft frame."""
import math, re, sys
log = open(sys.argv[1], encoding='utf-8', errors='replace').read().splitlines()
pivot = (3.5, 0.0, 4.5) if len(sys.argv) < 3 else tuple(map(float, sys.argv[2].split(',')))
player = None; pos = None
for line in log:
    m = re.search(r'\[bctest\] WHERE (\S+) (-?[\d.]+) (-?[\d.]+) (-?[\d.]+)', line)
    if m:
        player = tuple(float(m.group(i)) for i in (2, 3, 4)); pos = None
        continue
    m = re.search(r'has the following entity data: \[(-?[\d.]+)d, (-?[\d.]+)d, (-?[\d.]+)d\]', line)
    if m:
        pos = tuple(float(m.group(i)) for i in (1, 2, 3)); continue
    m = re.search(r'has the following entity data: \[(-?[\d.]+)f, (-?[\d.]+)f\]', line)
    if m and player and pos:
        yaw = float(m.group(1)); c, s = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        dx, dz = player[0] - pos[0], player[2] - pos[2]
        lx = pivot[0] + dx * c + dz * s; lz = pivot[2] + dz * c - dx * s
        print(f'player {player} vessel {pos} yaw {yaw:.1f} -> local x {lx:.2f} y {player[1]-pos[1]+pivot[1]:.2f} z {lz:.2f}')
        player = None
