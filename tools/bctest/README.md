# bctest – scripted in-game test for Beach Craft

`tools/bctest/run.sh <script.txt> <outdir>` builds the mod, starts a Minecraft client (Xvfb, no sound) with the
test mod in `src/gametest`, runs the script and writes screenshots (`<outdir>/<name>.png`), `latest.log` and
`gradle.log`. It takes the build lock, so only one test or compile runs at a time (about 1–3 minutes).

Script commands (one per line, `#` comments):

| Command | Effect |
| --- | --- |
| `world [seed]` | new creative world with cheats, time frozen at noon, clear weather |
| `cmd <command>` | server command (e.g. `execute in raft:raftdim run tp @p 0 120 0 45 20`) |
| `pcmd <command>` | command as the player at the player (`place template raft:x ~ ~ ~`) |
| `wait <ticks>` | wait game ticks |
| `settle [s]` | wait until chunks around the player are downloaded and drawn |
| `shot <name>` | screenshot |
| `size <w> <h>` | window size (default 1280×720) |
| `hud on/off`, `third on/off`, `gui <n>`, `rd <n>` | HUD, third person camera, GUI scale, render distance |
| `use`, `attack`, `sneak`, `hold <use/attack/forward/jump/sneak> <ticks>` | input |
| `log <text>` | marker in the log |

Example:

```
world beachcraft
cmd execute in raft:raftdim run tp @p 0 130 0 -45 30
settle
cmd execute in raft:raftdim run place template raft:old_lighthouse 10 100 10
wait 20
hud off
shot lighthouse
```

Errors from the mod (exceptions, missing models/textures) are in `latest.log`; `run.sh` prints the important lines.
