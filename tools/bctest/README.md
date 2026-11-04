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
| `swait <ticks>` | wait until the server has run that many ticks (a busy server falls behind the client) |
| `settle [s]` | wait until the client has loaded the level (after a dimension change) and the chunks around the player are downloaded and drawn |
| `shot <name>` | screenshot |
| `size <w> <h>` | window size (default 1280×720) |
| `hud on/off`, `third on/off`, `gui <n>`, `rd <n>` | HUD, third person camera, GUI scale, render distance |
| `use`, `attack`, `sneak`, `hold <use/attack/forward/jump/sneak> <ticks>` | input |
| `sneakuse` | hold sneak and press use |
| `esc` | close the open screen |
| `slot <1-9>` | select a hotbar slot |
| `tab <id> [rows]` | open the creative inventory on a tab (`raft:bc_decor`, `raft:raft`, ...), scrolled down by rows, and log its items (`TAB` line) |
| `count [dim]` | log loaded entities per type (`COUNT` line in latest.log) |
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

`settle` logs how long it took (`settle took N s`). Chunk generation on the test machine is slow (3–5 min for a new
area at render distance 8): use `rd 5` and reuse areas within one script. `BC_JFR=/path/x.jfr` records a profile.

Errors from the mod (exceptions, missing models/textures) are in `latest.log`; `run.sh` prints the important lines.
