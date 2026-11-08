package net.kasax.raft.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.impl.client.gametest.world.TestWorldSaveImpl;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Scripted in-game test for Beach Craft. Reads the script file given by -Dbc.script (one command per line) and writes
 * screenshots to -Dbc.out. Commands (see tools/bctest/README.md):
 * <pre>
 * world [seed]            create a creative world with cheats (needed before server commands)
 * open &lt;name&gt;             open a kept world (build/run/bctest/saves/&lt;name&gt;) instead of creating one
 * keep &lt;name&gt;             after the script, keep the world under saves/&lt;name&gt; (to open it in a later run)
 * deadline &lt;minutes&gt;      stop the script (and save the world) once it has run that long
 * cmd &lt;command&gt;          run a server command as the server (use @p for the player)
 * pcmd &lt;command&gt;         run a command as the player, at the player's position
 * wait &lt;ticks&gt; [render]  wait game ticks (the client does not draw them unless "render" is given)
 * swait &lt;ticks&gt;           wait until the server has run that many ticks (a busy server falls behind the client)
 * swaitfor &lt;ticks&gt; &lt;command&gt;  wait (server ticks, at most that many) until a server command succeeds, e.g.
 *                         "execute if entity @e[type=raft:vessel]"; logs how long it took (or that it timed out)
 * settle [seconds]        wait until the client has loaded the level and all chunk sections around the camera are
 *                         drawn (default max 40 s)
 * shot &lt;name&gt;            screenshot to OUT/&lt;name&gt;.png (.jpg while jpg is on)
 * still &lt;name&gt; [scale]   screenshot rendered larger than the window (default 1.25), for slow zooms in the edit
 * rec &lt;name&gt; &lt;frames&gt; &lt;fps&gt; [options]   record a clip: OUT/&lt;name&gt;/f00000.jpg ... at fps frames per second of game
 *                         time (smooth: frames between ticks use the partial tick), plus sounds.jsonl/shot.json (the
 *                         sounds the game played). Following lines "@&lt;frame&gt; &lt;command&gt;" run before that frame;
 *                         a line "end" closes the block. Camera options (the player is moved on the client, so the
 *                         motion is exact on every frame): to=x,y,z,yaw,pitch (absolute, ~ keeps a value),
 *                         by=dx,dy,dz,dyaw,dpitch, orbit=cx,cz,degrees, look=&lt;entity type&gt; or look=x,y,z,
 *                         chase=distance[,height], smooth=0..1, ease=0|1, yawoff=degrees and pitch=degrees (the
 *                         tracked creature then shows beside the player instead of behind them), draw (draw every tick)
 * jpg on|off              convert screenshots to JPEG in the background (default on, keeps the disk small)
 * size &lt;w&gt; &lt;h&gt;          window size (default 1280 720)
 * hud on|off, third on|off|front, gui &lt;scale&gt;, rd &lt;chunks&gt;, sim &lt;chunks&gt;, fov &lt;degrees&gt;, gamma &lt;0..1&gt;, fly on|off
 * pose x y z yaw pitch    put the camera (player) exactly there on the client
 * face x y z              turn the player to look at a point;  lookat &lt;entity type&gt;  at the nearest such entity
 * use | attack | sneak    press the use / attack / sneak key once
 * press &lt;key&gt; / release &lt;key&gt;   hold / let go a key (use, attack, forward, back, left, right, jump, sneak, sprint)
 * key &lt;inventory|drop|swap&gt;  press that key once
 * click &lt;x&gt; &lt;y&gt;          left click at window pixel x y;  cursor &lt;x&gt; &lt;y&gt;  move the mouse there
 * sneakuse                hold sneak and press use (sneak-use an item or block)
 * esc                     close the open screen
 * chat on|off             show or hide the chat;  arrow left|right|up|down  press an arrow key
 * slot &lt;1-9&gt;              select a hotbar slot
 * count [dimension]       log the loaded entities per type (COUNT line in latest.log)
 * near [radius]          log living entities per type (health of hostiles) and items around the player (NEAR)
 * track &lt;type&gt; [radius]  log game time and id:x,y,z,yaw,pitch of each such entity near the player (TRACK)
 * hold &lt;use|attack|forward|jump|sneak&gt; &lt;ticks&gt;
 * scout &lt;structure&gt; [x z] log the nearest structure of that id (raft dimension, from the player or x z): start,
 *                         bounding box, pieces and rotation (SCOUT)
 * openbe x y z           open the screen of the container at x y z for the player (loot is rolled like opening it)
 * openloot &lt;table&gt; [radius] [peek]  go to the nearest container whose loot table contains that text and open it
 *                         (sets {ox} {oy} {oz}; with peek only the variables)
 * tpbiome &lt;biome&gt; [dimension]   go to the nearest such biome (onto its surface)
 * chests [radius]         log containers (loot table) and block entities of the mod around the player (CHEST/BE)
 * find &lt;block&gt; [radius]  log where that block is around the player (FIND)
 * faceblock &lt;block&gt; [radius]   turn to the nearest such block
 * nearblock &lt;block&gt; &lt;radius&gt; &lt;dx&gt; &lt;dy&gt; &lt;dz&gt;   stand at that offset from the nearest such block, facing it
 * where                   log the player's position, rotation and biome (WHERE); sets {px} {py} {pz}
 * audit &lt;structure&gt; [x z] [label]   compare the nearest such structure with its templates block by block and
 *                         check its people (StructAudit): AUDIT line, OUT/audit/&lt;label&gt;.txt; sets the box like scout
 *                         and nx ny nz (first person found)
 * npcview &lt;entity type&gt;   free camera spot with a clear view of the face of the nearest such person to {cx} {cz}:
 *                         sets camx camy (feet) camz camyaw campitch
 * overview [0-3]          camera above the terrain beside the last audited box, looking at it: ovx ovy ovz ovyaw ovpitch
 * clouds on|off           show or hide the clouds
 * set &lt;name&gt; &lt;value&gt;     a script variable: {name} or {name+5} anywhere in later lines (scout sets x0 y0 z0 x1 y1 z1 and
 *                         the box center cx cy cz, find sets fx fy fz of the nearest hit, height &lt;x&gt; &lt;z&gt; sets hy)
 * freecam x y z yaw pitch | off   detached camera at that eye position (rec: cam=x,y,z,yaw,pitch with to/by/orbit/look)
 * respawn                 if the player is dead (a world saved like that): respawn and close the death screen
 * sounds &lt;name&gt; | off  log every sound the client starts to OUT/&lt;name&gt;/sounds.jsonl (like rec, without frames)
 * music [constant]       log the music the game would choose now (MUSIC); constant: no pauses between pieces
 * log &lt;text&gt;
 * </pre>
 */
public class BcScript implements FabricClientGameTest {
    public static final Logger LOG = LoggerFactory.getLogger("bctest");
    /** While true the client skips drawing its per-tick frames (screenshots still render): see NoRenderMixin. */
    public static volatile boolean skipFrames;
    /** While true the world stands still on server and client (HoldWorldMixin, HoldPlayerMixin): set while a recorded
     *  frame is read back, so a clip shows exactly fps frames per second of game time. */
    public static volatile boolean holdWorld;
    /** While above 0 the client draws a frame at most every throttleMs milliseconds (settle). */
    public static volatile long throttleMs;
    /** Detached camera (x, eye y, z, yaw, pitch) while not null: see FreeCamMixin. The player stays where it is. */
    public static volatile double[] freeCam;
    /** Script variables ({name} or {name+5} in a line): set by scout, find, height, where and set. */
    private int checksPassed, checksFailed;
    private final Map<String, Double> vars = new HashMap<>();

    private String subst(String line) {
        if (line.indexOf('{') < 0) return line;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{([a-z][a-z0-9]*)([+-][0-9.]+)?(:i)?\\}").matcher(line);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Double v = vars.get(m.group(1));
            if (v == null) {
                LOG.warn("[bctest] no variable {}", m.group(1));
                v = 0.0;
            }
            double d = v + (m.group(2) == null ? 0 : Double.parseDouble(m.group(2)));
            // {name:i}: a whole number (block coordinates for commands such as forceload)
            m.appendReplacement(sb, m.group(3) != null ? Long.toString((long) Math.floor(d)) : String.format(Locale.ROOT, "%.2f", d));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private ClientGameTestContext ctx;
    private TestSingleplayerContext sp;
    /** The box of the last audited structure (for overview). */
    private BoundingBox lastBox;

    /** near: list the health of hostile mobs only. */
    private static boolean e0(String type) {
        return !(type.contains("scavenger") || type.contains("guardian") || type.contains("corsair") || type.contains("drowned")
                || type.contains("blaze") || type.contains("crab") && type.contains("ember") || type.contains("stray")
                || type.contains("magma") || type.contains("player"));
    }

    private static double horiz(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return dx * dx + dz * dz;
    }
    private Path out;
    private String keep;
    private boolean jpg = true;
    private long started, deadline = Long.MAX_VALUE;
    private final ExecutorService saver = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "bctest-jpg");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    @Override
    public void runTest(ClientGameTestContext ctx) {
        this.ctx = ctx;
        started = System.currentTimeMillis();
        out = Path.of(System.getProperty("bc.out", "bctest-out")).toAbsolutePath();
        List<String> lines = new ArrayList<>();
        try {
            Files.createDirectories(out);
            String script = System.getProperty("bc.script");
            if (script != null) lines.addAll(Files.readAllLines(Path.of(script)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.options.renderDistance().set(8);
            mc.options.simulationDistance().set(8);
            mc.options.bobView().set(false);
            mc.options.showAutosaveIndicator().set(false);
            mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
            mc.options.tutorialStep = TutorialSteps.NONE;
            mc.options.fovEffectScale().set(0.0);
            mc.resizeDisplay();
        });
        try {
            for (int n = 0; n < lines.size(); n++) {
                String line = lines.get(n).strip();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("@") || line.equals("end")) continue;
                line = subst(line);
                if (System.currentTimeMillis() > deadline) {
                    LOG.warn("[bctest] deadline reached before line {}: stopping the script", n + 1);
                    break;
                }
                LOG.info("[bctest] {}: {}", n + 1, line);
                if (line.startsWith("rec ")) {
                    // the block of timed actions: "@<frame> <command>" lines, closed by "end"
                    Map<Integer, List<String>> at = new TreeMap<>();
                    int k = n + 1;
                    while (k < lines.size()) {
                        String l = lines.get(k).strip();
                        if (l.startsWith("@")) {
                            String[] p = l.substring(1).split("\\s+", 2);
                            at.computeIfAbsent(Integer.parseInt(p[0]), x -> new ArrayList<>()).add(p.length > 1 ? subst(p[1]) : "");
                            k++;
                        } else if (l.equals("end")) {
                            k++;
                            break;
                        } else {
                            break;
                        }
                    }
                    n = k - 1;
                    try {
                        rec(line.substring(4).strip(), at);
                    } catch (Exception e) {
                        LOG.warn("[bctest] rec failed: {}", e.toString(), e);
                    }
                    continue;
                }
                try {
                    exec(line);
                } catch (RuntimeException e) {
                    if (line.startsWith("world") || line.startsWith("open")) throw e;
                    LOG.warn("[bctest] line {} failed: {}", n + 1, e.toString());
                }
            }
        } finally {
            if (sp != null) {
                Path dir = sp.getWorldSave().getSaveDirectory();
                sp.close();
                if (keep != null) keepWorld(dir);
            }
            saver.shutdown();
            try {
                saver.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException ignored) {
            }
        }
        LOG.info("[bctest] script done after {} s", (System.currentTimeMillis() - started) / 1000);
    }

    // ------------------------------------------------------------------ commands

    private void exec(String line) {
        String[] p = line.split("\\s+", 2);
        String cmd = p[0];
        String arg = p.length > 1 ? p[1].strip() : "";
        switch (cmd) {
            case "world" -> {
                String seed = arg.isEmpty() ? "beachcraft" : arg;
                sp = ctx.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
                    s.setSeed(seed);
                    s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    s.setAllowCommands(true);
                    s.setDifficulty(Difficulty.NORMAL);
                }).create();
                sp.getServer().runCommand("gamerule doDaylightCycle false");
                sp.getServer().runCommand("gamerule doWeatherCycle false");
                sp.getServer().runCommand("time set 6000");
                sp.getServer().runCommand("weather clear");
                LOG.info("[bctest] WORLD {}", sp.getWorldSave().getSaveDirectory().getFileName());
                settle(40);
            }
            case "open" -> {
                Path dir = ctx.computeOnClient(mc -> mc.gameDirectory.toPath().resolve("saves").resolve(arg));
                if (!Files.isDirectory(dir)) throw new IllegalStateException("no saved world " + dir);
                sp = new TestWorldSaveImpl(ctx, dir).open();
                LOG.info("[bctest] WORLD {} opened", arg);
                settle(40);
            }
            case "keep" -> keep = arg;
            case "deadline" -> deadline = started + (long) (Double.parseDouble(arg) * 60_000);
            case "cmd" -> need().getServer().runCommand(arg);
            case "pcmd" -> need().getServer().runCommand("execute as @p at @p run " + arg);
            case "swait" -> {
                // swait <ticks>: wait for that many server ticks (client ticks run on even when the server lags)
                int n = Integer.parseInt(arg.split("\\s+")[0]);
                int target = need().getServer().computeOnServer(server -> server.getTickCount()) + n;
                skipFrames = true;
                try {
                    need().getServer().waitFor(server -> server.getTickCount() >= target, Math.max(600, n * 30));
                } finally {
                    skipFrames = false;
                }
            }
            case "swaitfor" -> {
                // swaitfor <ticks> <command>: wait until the command succeeds (result > 0), at most that many server ticks
                String[] a = arg.split("\\s+", 2);
                int max = Integer.parseInt(a[0]);
                skipFrames = true;
                try {
                    int took = need().getServer().waitFor(server -> {
                        try {
                            return server.getCommands().getDispatcher().execute(a[1], server.createCommandSourceStack().withSuppressedOutput()) > 0;
                        } catch (Exception e) {
                            return false;
                        }
                    }, max);
                    LOG.info("[bctest] SWAITFOR {} ticks: {}", took, a[1]);
                } catch (AssertionError e) {
                    LOG.warn("[bctest] SWAITFOR timed out after {} ticks: {}", max, a[1]);
                } finally {
                    skipFrames = false;
                }
            }
            case "wait" -> {
                // wait <ticks> [render]: the frames in between are not drawn unless "render" is given
                String[] a = arg.split("\\s+");
                skipFrames = !(a.length > 1 && a[1].equals("render"));
                try {
                    ctx.waitTicks(Integer.parseInt(a[0]));
                } finally {
                    skipFrames = false;
                }
            }
            case "settle" -> {
                long t0 = System.currentTimeMillis();
                // (no waitForChunksRender: it draws full frames while it waits, which starves the server with shaders)
                settle(arg.isEmpty() ? 40 : Integer.parseInt(arg));
                LOG.info("[bctest] settle took {} s", (System.currentTimeMillis() - t0) / 1000);
            }
            case "shotif" -> {
                // shotif <variable> <name>: the shot only when the variable is set (npcview found a camera spot)
                String[] a = arg.split("\\s+", 2);
                if (vars.containsKey(a[0])) save(ctx.takeScreenshot(TestScreenshotOptions.of(a[1]).disableCounterPrefix().withDestinationDir(out)), 0.95f);
                else LOG.warn("[bctest] shotif: no {}, no shot {}", a[0], a[1]);
            }
            case "shot" -> save(ctx.takeScreenshot(TestScreenshotOptions.of(arg).disableCounterPrefix().withDestinationDir(out)), 0.95f);
            case "still" -> {
                String[] a = arg.split("\\s+");
                double scale = a.length > 1 ? Double.parseDouble(a[1]) : 1.25;
                int[] wh = ctx.computeOnClient(mc -> new int[]{mc.getWindow().getWidth(), mc.getWindow().getHeight()});
                save(ctx.takeScreenshot(TestScreenshotOptions.of(a[0]).disableCounterPrefix().withDestinationDir(out)
                        .withSize((int) Math.round(wh[0] * scale), (int) Math.round(wh[1] * scale))), 0.95f);
            }
            case "jpg" -> jpg = !arg.equals("off");
            case "size" -> {
                String[] wh = arg.split("\\s+");
                ctx.getInput().resizeWindow(Integer.parseInt(wh[0]), Integer.parseInt(wh[1]));
            }
            case "hud" -> ctx.runOnClient(mc -> {
                mc.options.hideGui = arg.equals("off");
            });
            case "third" -> ctx.runOnClient(mc -> mc.options.setCameraType(arg.equals("on") ? CameraType.THIRD_PERSON_BACK
                    : arg.equals("front") ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON));
            case "gui" -> ctx.runOnClient(mc -> {
                mc.options.guiScale().set(Integer.parseInt(arg));
                mc.resizeDisplay();
            });
            case "rd" -> ctx.runOnClient(mc -> mc.options.renderDistance().set(Integer.parseInt(arg)));
            case "clouds" -> ctx.runOnClient(mc -> mc.options.cloudStatus().set(arg.equals("off") ? net.minecraft.client.CloudStatus.OFF : net.minecraft.client.CloudStatus.FANCY));
            case "sim" -> ctx.runOnClient(mc -> mc.options.simulationDistance().set(Integer.parseInt(arg)));
            case "fov" -> ctx.runOnClient(mc -> mc.options.fov().set(Integer.parseInt(arg)));
            case "gamma" -> ctx.runOnClient(mc -> mc.options.gamma().set(Double.parseDouble(arg)));
            case "fly" -> ctx.runOnClient(mc -> {
                mc.player.getAbilities().flying = arg.equals("on");
                mc.player.onUpdateAbilities();
            });
            case "pose" -> {
                double[] v = nums(arg);
                ctx.runOnClient(mc -> setPose(mc.player, v[0], v[1], v[2], (float) v[3], (float) v[4], true));
            }
            case "face" -> {
                double[] v = nums(arg);
                ctx.runOnClient(mc -> {
                    LocalPlayer pl = mc.player;
                    float[] r = aim(pl.getX(), pl.getEyeY(), pl.getZ(), new Vec3(v[0], v[1], v[2]));
                    setPose(pl, pl.getX(), pl.getY(), pl.getZ(), r[0], r[1], false);
                });
            }
            case "lookat" -> ctx.runOnClient(mc -> {
                LocalPlayer pl = mc.player;
                Entity e = nearest(mc, arg);
                if (e == null) {
                    LOG.warn("[bctest] no {} to look at", arg);
                    return;
                }
                float[] r = aim(pl.getX(), pl.getEyeY(), pl.getZ(), e.position().add(0, e.getBbHeight() * 0.5, 0));
                setPose(pl, pl.getX(), pl.getY(), pl.getZ(), r[0], r[1], false);
            });
            case "chat" -> ctx.runOnClient(mc -> {
                mc.options.chatVisibility().set(arg.equals("on") ? ChatVisiblity.FULL : ChatVisiblity.HIDDEN);
                // tell the server too: it does not send system messages (command feedback, mod messages) to a player
                // whose chat it believes hidden
                mc.options.broadcastOptions();
            });
            case "arrow" -> ctx.getInput().pressKey(switch (arg) {
                case "left" -> InputConstants.KEY_LEFT;
                case "up" -> InputConstants.KEY_UP;
                case "down" -> InputConstants.KEY_DOWN;
                default -> InputConstants.KEY_RIGHT;
            });
            case "use" -> ctx.getInput().pressKey(o -> o.keyUse);
            case "attack" -> ctx.getInput().pressKey(o -> o.keyAttack);
            case "sneak" -> ctx.getInput().pressKey(o -> o.keyShift);
            case "press" -> ctx.getInput().holdKey(key(arg));
            case "release" -> ctx.getInput().releaseKey(key(arg));
            case "key" -> ctx.getInput().pressKey(key(arg));
            case "click" -> {
                double[] v = nums(arg);
                ctx.getInput().setCursorPos(v[0], v[1]);
                ctx.waitTick();
                ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            }
            case "cursor" -> {
                double[] v = nums(arg);
                ctx.getInput().setCursorPos(v[0], v[1]);
            }
            case "scroll" -> ctx.getInput().scroll(Double.parseDouble(arg));
            case "sneakuse" -> {
                ctx.getInput().holdKey(o -> o.keyShift);
                ctx.waitTicks(3);
                ctx.getInput().pressKey(o -> o.keyUse);
                ctx.waitTicks(3);
                ctx.getInput().releaseKey(o -> o.keyShift);
            }
            case "esc" -> ctx.setScreen(() -> null);
            case "tab" -> {
                // tab <id> [rows]: open the creative inventory on that tab (scrolled down by rows) and log its items
                String[] ta = arg.split("\\s+");
                var tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(ResourceLocation.parse(ta[0]));
                ctx.runOnClient(mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                        mc.player, mc.player.connection.enabledFeatures(), false)));
                ctx.waitTicks(2);
                ctx.runOnClient(mc -> {
                    if (tab == null || !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen screen)) {
                        LOG.warn("[bctest] no creative tab {}", arg);
                        return;
                    }
                    // Fabric API pages the tabs on 1.21.1: a tab on another page is only selected through its page
                    if (!((net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen) screen).setSelectedItemGroup(tab)) {
                        ((net.kasax.raft.gametest.mixin.CreativeScreenInvoker) screen).bc$selectTab(tab);
                    }
                    if (ta.length > 1) screen.mouseScrolled(0, 0, 0, -Double.parseDouble(ta[1]));
                    StringBuilder ids = new StringBuilder();
                    for (var stack : tab.getDisplayItems()) {
                        ids.append(' ').append(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                    }
                    LOG.info("[bctest] TAB {} {}:{}", arg, tab.getDisplayItems().size(), ids);
                });
            }
            case "slot" -> {
                int i = Integer.parseInt(arg) - 1;
                ctx.getInput().pressKey(o -> o.keyHotbarSlots[i]);
            }
            case "hold" -> {
                String[] a = arg.split("\\s+");
                ctx.getInput().holdKeyFor(key(a[0]), Integer.parseInt(a[1]));
            }
            case "check" -> {
                // check <label> | <command>: run a server command as the player; logs CHECK PASS when it succeeds
                // (result > 0), CHECK FAIL otherwise. "checks" logs the totals.
                String[] a = arg.split("\\s*\\|\\s*", 2);
                String command = a.length > 1 ? a[1] : a[0];
                int result = need().getServer().computeOnServer(server -> {
                    try {
                        var pl = server.getPlayerList().getPlayers().getFirst();
                        return server.getCommands().getDispatcher().execute(command,
                                pl.createCommandSourceStack().withSuppressedOutput().withPermission(4));
                    } catch (Exception e) {
                        LOG.info("[bctest] CHECK error {}: {}", command, e.getMessage());
                        return 0;
                    }
                });
                if (result > 0) checksPassed++; else checksFailed++;
                LOG.info("[bctest] CHECK {} {} ({})", result > 0 ? "PASS" : "FAIL", a[0], result);
            }
            case "checks" -> LOG.info("[bctest] CHECKS passed {} failed {}", checksPassed, checksFailed);
            case "log" -> LOG.info("[bctest] LOG {}", arg);
            case "sounds" -> {
                // sounds <name> | off: log every sound the client starts to OUT/<name>/sounds.jsonl (no frames)
                if (arg.equals("off")) {
                    FilmSound.stop();
                } else {
                    long g0 = ctx.computeOnClient(mc -> mc.level == null ? 0L : mc.level.getGameTime());
                    FilmSound.start(out.resolve(arg), 20.0, g0);
                }
            }
            case "music" -> {
                // music [constant]: log the music the game would choose now (and the audio module's state);
                // "constant" plays music without pauses between pieces (to see it quickly in the sound log)
                if (arg.equals("constant")) {
                    ctx.runOnClient(mc -> {
                        // 1.21.1 has no music frequency option: start the situational music now instead
                        mc.getMusicManager().startPlaying(mc.getSituationalMusic());
                    });
                }
                String res = ctx.computeOnClient(mc -> {
                    var music = mc.getSituationalMusic();
                    return String.format(Locale.ROOT, "situational=%s place=%s fight=%s openSea=%.2f night=%s playing=%s",
                            music == null ? "none" : music.getEvent().unwrapKey().map(k -> k.location().toString()).orElse("?"),
                            net.kasax.raft.content.audio.client.MusicDirector.place(),
                            net.kasax.raft.content.audio.client.MusicDirector.inFight(),
                            net.kasax.raft.content.audio.client.AudioClient.openSea(),
                            mc.level != null && net.kasax.raft.content.audio.client.MusicDirector.isNight(mc.level),
                            mc.getMusicManager().isPlayingMusic(mc.getSituationalMusic()));
                });
                LOG.info("[bctest] MUSIC {}", res);
            }
            case "states" -> {
                // states <dim> x0 y0 z0 x1 y1 z1: log every non-air block state in the box (STATES lines)
                String[] a = arg.split("\\s+");
                int[] c = new int[6];
                for (int i = 0; i < 6; i++) c[i] = (int) Math.floor(Double.parseDouble(a[i + 1]));
                String res = need().getServer().computeOnServer(server -> {
                    var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(a[0])));
                    if (level == null) return "no level";
                    StringBuilder sb = new StringBuilder();
                    for (BlockPos bp : BlockPos.betweenClosed(Math.min(c[0], c[3]), Math.min(c[1], c[4]), Math.min(c[2], c[5]),
                            Math.max(c[0], c[3]), Math.max(c[1], c[4]), Math.max(c[2], c[5]))) {
                        var st = level.getBlockState(bp);
                        if (!st.isAir() && !st.is(net.minecraft.world.level.block.Blocks.WATER)) {
                            sb.append("\n  ").append(bp.toShortString()).append(' ').append(st);
                        }
                    }
                    return sb.toString();
                });
                LOG.info("[bctest] STATES {}{}", arg, res);
            }
            case "dumpmodels" -> {
                // dumpmodels <file>: every polygon of every raft: entity model layer in its default pose (model units,
                // after the part transforms) as JSON lines, for the z-fighting check (tools/bctest/zfight_entities.py)
                String name = arg.isEmpty() ? "models.jsonl" : arg;
                String res = ctx.computeOnClient(mc -> {
                    StringBuilder sb = new StringBuilder();
                    int layers = 0;
                    try {
                        var f = net.minecraft.client.model.geom.EntityModelSet.class.getDeclaredField("roots");
                        f.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        var roots = (Map<net.minecraft.client.model.geom.ModelLayerLocation, ?>) f.get(mc.getEntityModels());
                        for (var id : roots.keySet()) {
                            if (!id.getModel().getNamespace().equals("raft")) continue;
                            layers++;
                            var part = mc.getEntityModels().bakeLayer(id);
                            var ps = new com.mojang.blaze3d.vertex.PoseStack();
                            String layer = id.toString();
                            part.visit(ps, (pose, path, ci, cube) -> {
                                for (var poly : ((net.kasax.raft.compat.mixin.CubeAccess) cube).raft$polygons()) {
                                    sb.append("{\"layer\":\"").append(layer).append("\",\"part\":\"").append(path)
                                      .append("\",\"cube\":").append(ci).append(",\"v\":[");
                                    boolean first = true;
                                    for (var v : poly.vertices) {
                                        var wp = pose.pose().transformPosition(v.pos.x() / 16f, v.pos.y() / 16f, v.pos.z() / 16f, new org.joml.Vector3f()).mul(16f);
                                        if (!first) sb.append(',');
                                        first = false;
                                        sb.append(String.format(Locale.ROOT, "[%.4f,%.4f,%.4f,%.5f,%.5f]", wp.x, wp.y, wp.z, v.u, v.v));
                                    }
                                    sb.append("]}\n");
                                }
                            });
                        }
                        Files.writeString(out.resolve(name), sb.toString());
                    } catch (Exception e) {
                        return "error " + e;
                    }
                    return layers + " layers";
                });
                LOG.info("[bctest] DUMPMODELS {}", res);
            }
            case "count" -> {
                // count <dimension>: entities per type loaded in that dimension (written to the log)
                String dim = arg.isEmpty() ? "raft:raftdim" : arg;
                String res = need().getServer().computeOnServer(server -> {
                    var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dim)));
                    if (level == null) return "no level " + dim;
                    Map<String, Integer> counts = new TreeMap<>();
                    for (var e : level.getAllEntities()) {
                        counts.merge(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(), 1, Integer::sum);
                    }
                    return counts.toString();
                });
                LOG.info("[bctest] COUNT {} {}", dim, res);
            }
            case "aim" -> {
                // aim: what the crosshair targets now (AIM line): hit type, entity, block or raft block, distance
                String res = ctx.computeOnClient(mc -> {
                    var hr = mc.hitResult;
                    if (hr == null) return "none";
                    String s = hr.getType() + " at " + String.format(Locale.ROOT, "%.2f,%.2f,%.2f", hr.getLocation().x, hr.getLocation().y, hr.getLocation().z);
                    if (hr instanceof net.minecraft.world.phys.EntityHitResult eh) {
                        s += " entity " + BuiltInRegistries.ENTITY_TYPE.getKey(eh.getEntity().getType());
                        if (eh.getEntity() instanceof net.kasax.raft.content.vessel.VesselEntity v) {
                            s += " raftBlock " + net.kasax.raft.content.vessel.client.VesselPicker.targetOf(v);
                        }
                    } else if (hr instanceof net.minecraft.world.phys.BlockHitResult bh) {
                        s += " block " + bh.getBlockPos().toShortString() + " " + mc.level.getBlockState(bh.getBlockPos()).getBlock();
                    }
                    return s + " riding=" + (mc.player.getVehicle() != null) + " eye=" + String.format(Locale.ROOT, "%.2f,%.2f,%.2f",
                            mc.player.getEyePosition().x, mc.player.getEyePosition().y, mc.player.getEyePosition().z);
                });
                LOG.info("[bctest] AIM {}", res);
            }
            case "near" -> {
                // near [radius]: living entities per type around the player (health of each in brackets for monsters)
                // and the items lying there (NEAR line): guards of a structure, animals of an island, loot drops
                double r = arg.isEmpty() ? 48.0 : Double.parseDouble(arg.split(" ")[0]);
                String res = need().getServer().computeOnServer(server -> {
                    var pl = server.getPlayerList().getPlayers().getFirst();
                    var level = (ServerLevel) pl.level();
                    Map<String, List<String>> by = new TreeMap<>();
                    Map<String, Integer> items = new TreeMap<>();
                    for (var e : level.getEntities((Entity) null, pl.getBoundingBox().inflate(r), x -> x != pl)) {
                        if (e instanceof net.minecraft.world.entity.item.ItemEntity it) {
                            items.merge(BuiltInRegistries.ITEM.getKey(it.getItem().getItem()).toString(), it.getItem().getCount(), Integer::sum);
                        } else if (e instanceof net.minecraft.world.entity.LivingEntity le) {
                            String name = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString()
                                    + (e.hasCustomName() ? "(" + e.getCustomName().getString() + ")" : "");
                            by.computeIfAbsent(name, k -> new ArrayList<>()).add(String.format(Locale.ROOT, "%.0f/%.0f",
                                    le.getHealth(), le.getMaxHealth()));
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    by.forEach((k, v) -> sb.append(k).append('=').append(v.size())
                            .append(e0(k) ? "" : v.toString()).append(' '));
                    return sb + "items=" + items;
                });
                LOG.info("[bctest] NEAR {} {}", (int) r, res);
            }
            case "track" -> {
                // track <entity type> [radius]: game time, then id:x,y,z,yaw,pitch of each such entity near the player
                // (TRACK line; put it on every frame of a rec to measure how creatures move)
                String[] a = arg.split(" ");
                double r = a.length > 1 ? Double.parseDouble(a[1]) : 96.0;
                String res = need().getServer().computeOnServer(server -> {
                    var pl = server.getPlayerList().getPlayers().getFirst();
                    var level = (ServerLevel) pl.level();
                    var type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(a[0])).orElse(null);
                    if (type == null) return "no type " + a[0];
                    StringBuilder sb = new StringBuilder().append(level.getGameTime());
                    for (var e : level.getEntities(type, pl.getBoundingBox().inflate(r), x -> true)) {
                        sb.append(String.format(Locale.ROOT, " %d:%.2f,%.2f,%.2f,%.1f,%.1f", e.getId(), e.getX(), e.getY(), e.getZ(),
                                e.getYRot(), e.getXRot()));
                    }
                    return sb.toString();
                });
                LOG.info("[bctest] TRACK {} {}", a[0], res);
            }
            case "where" -> {
                String res = need().getServer().computeOnServer(server -> {
                    var pl = server.getPlayerList().getPlayers().getFirst();
                    var level = (ServerLevel) pl.level();
                    return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.1f %.1f %s", level.dimension().location(), pl.getX(), pl.getY(), pl.getZ(),
                            pl.getYRot(), pl.getXRot(), level.getBiome(pl.blockPosition()).getRegisteredName());
                });
                LOG.info("[bctest] WHERE {}", res);
                String[] w = res.split(" ");
                vars.put("px", Double.parseDouble(w[1]));
                vars.put("py", Double.parseDouble(w[2]));
                vars.put("pz", Double.parseDouble(w[3]));
            }
            case "openbe" -> {
                // openbe x y z: open the screen of that block (chest, barrel, crate ...) for the player, wherever they are
                double[] v = nums(arg);
                String res = need().getServer().computeOnServer(server -> {
                    var pl = server.getPlayerList().getPlayers().getFirst();
                    ServerLevel level = (ServerLevel) pl.level();
                    BlockPos bp = BlockPos.containing(v[0], v[1], v[2]);
                    var provider = level.getBlockState(bp).getMenuProvider(level, bp);
                    if (provider == null) return "no menu at " + bp.toShortString() + ": " + level.getBlockState(bp);
                    pl.openMenu(provider);
                    return "opened " + level.getBlockState(bp).getBlock();
                });
                LOG.info("[bctest] OPENBE {}", res);
            }
            case "openloot" -> {
                // openloot <loot table part> [radius]: go next to the nearest container whose loot table id contains
                // that text (unopened, still holding its table) and open it
                String[] a = arg.split("\\s+");
                int r = a.length > 1 ? Integer.parseInt(a[1]) : 64;
                BlockPos bp = need().getServer().computeOnServer(server -> {
                    var pl = server.getPlayerList().getPlayers().getFirst();
                    ServerLevel level = (ServerLevel) pl.level();
                    BlockPos c = pl.blockPosition(), best = null;
                    double bd = Double.MAX_VALUE;
                    int rc = (r >> 4) + 1;
                    for (int cx = (c.getX() >> 4) - rc; cx <= (c.getX() >> 4) + rc; cx++) {
                        for (int cz = (c.getZ() >> 4) - rc; cz <= (c.getZ() >> 4) + rc; cz++) {
                            LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
                            if (ch == null) continue;
                            for (BlockEntity be : ch.getBlockEntities().values()) {
                                if (be instanceof RandomizableContainer rc2 && rc2.getLootTable() != null
                                        && rc2.getLootTable().location().toString().contains(a[0])) {
                                    double d = be.getBlockPos().distSqr(c);
                                    if (d < bd) {
                                        bd = d;
                                        best = be.getBlockPos();
                                    }
                                }
                            }
                        }
                    }
                    return best;
                });
                if (bp == null) {
                    LOG.warn("[bctest] openloot: no container with {} within {}", a[0], r);
                    break;
                }
                vars.put("ox", bp.getX() + 0.5);
                vars.put("oy", (double) bp.getY());
                vars.put("oz", bp.getZ() + 0.5);
                if (a.length > 2 && a[2].equals("peek")) {
                    LOG.info("[bctest] PEEK {} at {}", a[0], bp.toShortString());
                    break;
                }
                double px = bp.getX() + 0.5, py = bp.getY() + 0.5, pz = bp.getZ() + 2.5;
                float[] rot = aim(px, py + 1.62, pz, Vec3.atCenterOf(bp));
                need().getServer().runCommand(String.format(Locale.ROOT, "execute as @p at @s run tp @s %.2f %.2f %.2f %.1f %.1f", px, py, pz, rot[0], rot[1]));
                ctx.waitTicks(3);
                ctx.runOnClient(mc -> setPose(mc.player, px, py, pz, rot[0], rot[1], true));
                String res = need().getServer().computeOnServer(server -> {
                    var pl = server.getPlayerList().getPlayers().getFirst();
                    ServerLevel level = (ServerLevel) pl.level();
                    var provider = level.getBlockState(bp).getMenuProvider(level, bp);
                    if (provider == null) return "no menu at " + bp.toShortString();
                    pl.openMenu(provider);
                    return "opened " + level.getBlockState(bp).getBlock() + " at " + bp.toShortString();
                });
                LOG.info("[bctest] OPENLOOT {} {}", a[0], res);
            }
            case "tpbiome" -> {
                // tpbiome <biome> [dimension]: to the nearest such biome, onto its surface
                String[] a = arg.split("\\s+");
                String dim = a.length > 1 ? a[1] : null;
                String res = need().getServer().computeOnServer(server -> {
                    var pl = server.getPlayerList().getPlayers().getFirst();
                    ServerLevel level = dim == null ? (ServerLevel) pl.level()
                            : server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dim)));
                    var key = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(a[0]));
                    var found = level.findClosestBiome3d(h -> h.is(key), pl.blockPosition(), 6400, 32, 64);
                    if (found == null) return null;
                    return level.dimension().location() + " " + found.getFirst().getX() + " " + found.getFirst().getZ();
                });
                if (res == null) {
                    LOG.warn("[bctest] tpbiome: no {}", a[0]);
                    break;
                }
                String[] f = res.split(" ");
                need().getServer().runCommand("execute in " + f[0] + " run tp @p " + f[1] + " 200 " + f[2]);
                ctx.waitTicks(40);
                need().getServer().runCommand("execute in " + f[0] + " positioned " + f[1] + " 0 " + f[2] + " positioned over motion_blocking_no_leaves run tp @p ~ ~ ~");
                LOG.info("[bctest] TPBIOME {} at {}", a[0], res);
            }
            case "scout" -> {
                // scout <structure> [x z]: from the player or from x z
                String[] a = arg.split("\\s+");
                BlockPos from = a.length >= 3 ? new BlockPos(Integer.parseInt(a[1]), 64, Integer.parseInt(a[2])) : null;
                String res = need().getServer().computeOnServer(server -> scout(server, a[0], from));
                LOG.info("[bctest] SCOUT {} {}", a[0], res);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "bbox (-?\\d+) (-?\\d+) (-?\\d+) \\.\\. (-?\\d+) (-?\\d+) (-?\\d+)").matcher(res);
                if (m.find()) {
                    String[] k = {"x0", "y0", "z0", "x1", "y1", "z1"};
                    for (int i = 0; i < 6; i++) vars.put(k[i], Double.parseDouble(m.group(i + 1)));
                    vars.put("cx", (vars.get("x0") + vars.get("x1") + 1) / 2);
                    vars.put("cy", (vars.get("y0") + vars.get("y1") + 1) / 2);
                    vars.put("cz", (vars.get("z0") + vars.get("z1") + 1) / 2);
                }
            }
            case "audit" -> {
                // audit <structure> [x z] [label]: compare the nearest such structure (Endless Sea, from x z or the
                // player) with its templates and check its people (StructAudit); sets the box like scout and nx ny nz
                String[] a = arg.split("\\s+");
                String label = a.length >= 4 ? a[3] : a[0].replace(':', '_');
                StructAudit.Result r = need().getServer().computeOnServer(server -> StructAudit.audit(server, a[0],
                        a.length >= 3 ? new BlockPos(Integer.parseInt(a[1]), 64, Integer.parseInt(a[2]))
                                : server.getPlayerList().getPlayers().getFirst().blockPosition(), out, label));
                LOG.info("[bctest] {}", r.summary());
                if (r.box() != null) {
                    lastBox = r.box();
                    BoundingBox b = r.box();
                    vars.put("x0", (double) b.minX());
                    vars.put("y0", (double) b.minY());
                    vars.put("z0", (double) b.minZ());
                    vars.put("x1", (double) b.maxX());
                    vars.put("y1", (double) b.maxY());
                    vars.put("z1", (double) b.maxZ());
                    vars.put("cx", (b.minX() + b.maxX() + 1) / 2.0);
                    vars.put("cy", (b.minY() + b.maxY() + 1) / 2.0);
                    vars.put("cz", (b.minZ() + b.maxZ() + 1) / 2.0);
                }
                if (!r.npcs().isEmpty()) {
                    vars.put("nx", r.npcs().getFirst()[0]);
                    vars.put("ny", r.npcs().getFirst()[1]);
                    vars.put("nz", r.npcs().getFirst()[2]);
                }
            }
            case "npcview" -> {
                // npcview <entity type>: a free camera spot with a clear view of the face of the nearest such person
                // to {cx} {cz} (else the player): sets camx camy (feet) camz camyaw campitch
                String type = arg.strip();
                double fx = vars.getOrDefault("cx", Double.NaN), fz = vars.getOrDefault("cz", Double.NaN);
                double[] cam = need().getServer().computeOnServer(server -> {
                    ServerLevel level = StructAudit.sea(server);
                    var et = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(type)).orElse(null);
                    if (et == null) return null;
                    Vec3 from = Double.isNaN(fx) ? server.getPlayerList().getPlayers().getFirst().position() : new Vec3(fx, 64, fz);
                    Entity best = null;
                    for (Entity e : level.getEntities(et, e -> true)) {
                        if (best == null || horiz(e.position(), from) < horiz(best.position(), from)) best = e;
                    }
                    if (best == null) return null;
                    double[] c = StructAudit.npcCamera(level, best);
                    LOG.info("[bctest] NPCVIEW {} at {} camera {}", type, best.blockPosition().toShortString(),
                            c == null ? "NONE (no free spot with a clear view)" : String.format(Locale.ROOT, "%.2f %.2f %.2f %.1f %.1f", c[0], c[1], c[2], c[3], c[4]));
                    return c;
                });
                for (String k : new String[]{"camx", "camy", "camz", "camyaw", "campitch"}) vars.remove(k);
                if (cam != null) {
                    vars.put("camx", cam[0]);
                    vars.put("camy", cam[1] - 1.62);
                    vars.put("camz", cam[2]);
                    vars.put("camyaw", cam[3]);
                    vars.put("campitch", cam[4]);
                }
            }
            case "overview" -> {
                // overview [0-3]: a camera above the terrain, out from the last audited box along a diagonal, looking
                // at its center: sets ovx ovy (feet) ovz ovyaw ovpitch
                if (lastBox == null) {
                    LOG.warn("[bctest] overview: no audited box");
                    break;
                }
                int dir = arg.isEmpty() ? 0 : Integer.parseInt(arg);
                BoundingBox b = lastBox;
                double[] c = need().getServer().computeOnServer(server -> StructAudit.overview(StructAudit.sea(server), b, dir));
                vars.put("ovx", c[0]);
                vars.put("ovy", c[1] - 1.62);
                vars.put("ovz", c[2]);
                vars.put("ovyaw", c[3]);
                vars.put("ovpitch", c[4]);
                LOG.info("[bctest] OVERVIEW {} {}", dir, String.format(Locale.ROOT, "%.1f %.1f %.1f %.1f %.1f", c[0], c[1], c[2], c[3], c[4]));
            }
            case "chests" -> {
                int r = arg.isEmpty() ? 64 : Integer.parseInt(arg);
                for (String s : need().getServer().computeOnServer(server -> chests(server, r))) LOG.info("[bctest] {}", s);
            }
            case "faceblock", "nearblock" -> {
                // faceblock <block> [radius]: turn to the nearest such block
                // nearblock <block> <radius> <dx> <dy> <dz>: stand at that offset from the nearest such block, facing it
                String[] a = arg.split("\\s+");
                int r = a.length > 1 ? Integer.parseInt(a[1]) : 64;
                BlockPos hit = need().getServer().computeOnServer(server -> nearestBlock(server, a[0], r));
                if (hit == null) {
                    LOG.warn("[bctest] {}: no {} within {}", cmd, a[0], r);
                    break;
                }
                Vec3 c = Vec3.atCenterOf(hit);
                if (cmd.equals("nearblock")) {
                    double[] d = new double[]{Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4])};
                    double px = c.x + d[0], py = hit.getY() + d[1], pz = c.z + d[2];
                    float[] rot = aim(px, py + 1.62, pz, c);
                    need().getServer().runCommand(String.format(Locale.ROOT, "execute as @p at @s run tp @s %.2f %.2f %.2f %.1f %.1f", px, py, pz, rot[0], rot[1]));
                    ctx.waitTicks(2);
                    ctx.runOnClient(mc -> setPose(mc.player, px, py, pz, rot[0], rot[1], true));
                } else {
                    ctx.runOnClient(mc -> {
                        LocalPlayer pl = mc.player;
                        float[] rot = aim(pl.getX(), pl.getEyeY(), pl.getZ(), c);
                        setPose(pl, pl.getX(), pl.getY(), pl.getZ(), rot[0], rot[1], false);
                    });
                }
                LOG.info("[bctest] {} {} at {}", cmd.toUpperCase(Locale.ROOT), a[0], hit.toShortString());
            }
            case "find" -> {
                String[] a = arg.split("\\s+");
                int r = a.length > 1 ? Integer.parseInt(a[1]) : 48;
                LOG.info("[bctest] FIND {} {}", a[0], need().getServer().computeOnServer(server -> find(server, a[0], r)));
                double[] f = need().getServer().computeOnServer(server -> {
                    var pl = server.getPlayerList().getPlayers().getFirst();
                    BlockPos best = null;
                    for (BlockPos h : scan(server, a[0], r)) {
                        if (best == null || h.distSqr(pl.blockPosition()) < best.distSqr(pl.blockPosition())) best = h;
                    }
                    return best == null ? null : new double[]{best.getX() + 0.5, best.getY(), best.getZ() + 0.5};
                });
                if (f != null) {
                    vars.put("fx", f[0]);
                    vars.put("fy", f[1]);
                    vars.put("fz", f[2]);
                }
            }
            case "respawn" -> {
                // a world saved with a dead player opens on the death screen: respawn and close it
                boolean dead = ctx.computeOnClient(mc -> mc.player != null && mc.player.isDeadOrDying());
                if (dead) {
                    ctx.runOnClient(mc -> mc.player.respawn());
                    ctx.waitTicks(40);
                    ctx.setScreen(() -> null);
                    LOG.info("[bctest] RESPAWNED");
                }
            }
            case "set" -> {
                String[] a = arg.split("\\s+");
                vars.put(a[0], Double.parseDouble(a[1]));
            }
            case "height" -> {
                // height <x> <z>: hy = the y above the highest solid block there (water and leaves ignored)
                double[] v = nums(arg);
                vars.put("hy", (double) need().getServer().computeOnServer(server -> {
                    var pl = server.getPlayerList().getPlayers().getFirst();
                    ServerLevel lv = (ServerLevel) pl.level();
                    lv.getChunk((int) Math.floor(v[0]) >> 4, (int) Math.floor(v[1]) >> 4);   // generate it first (else: min height)
                    return lv.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,
                            (int) Math.floor(v[0]), (int) Math.floor(v[1]));
                }));
                LOG.info("[bctest] HEIGHT {} -> {}", arg, vars.get("hy"));
            }
            case "freecam" -> {
                // freecam x y z yaw pitch | off: a detached camera (eye position) for shots; the player stays visible
                if (arg.equals("off")) {
                    freeCam = null;
                } else {
                    freeCam = nums(arg);
                }
            }
            default -> LOG.warn("[bctest] unknown command '{}'", cmd);
        }
    }

    private TestSingleplayerContext need() {
        if (sp == null) throw new IllegalStateException("'world' or 'open' must come before server commands");
        return sp;
    }

    private static double[] nums(String arg) {
        String[] a = arg.trim().split("[\\s,]+");
        double[] v = new double[a.length];
        for (int i = 0; i < a.length; i++) v[i] = Double.parseDouble(a[i]);
        return v;
    }

    private static Function<Options, KeyMapping> key(String name) {
        return switch (name) {
            case "use" -> o -> o.keyUse;
            case "attack" -> o -> o.keyAttack;
            case "forward" -> o -> o.keyUp;
            case "back" -> o -> o.keyDown;
            case "left" -> o -> o.keyLeft;
            case "right" -> o -> o.keyRight;
            case "jump" -> o -> o.keyJump;
            case "sneak" -> o -> o.keyShift;
            case "sprint" -> o -> o.keySprint;
            case "inventory" -> o -> o.keyInventory;
            case "drop" -> o -> o.keyDrop;
            case "swap" -> o -> o.keySwapOffhand;
            default -> throw new IllegalArgumentException("unknown key " + name);
        };
    }

    /**
     * Waits (at most maxSeconds) until every chunk within the render distance (minus one) is on the client and all
     * sections are drawn, so no half-loaded terrain shows up in a shot.
     */
    private void settle(int maxSeconds) {
        long t0 = System.currentTimeMillis(), end = t0 + maxSeconds * 1000L;
        // 1) the server generates the chunks around the player first: no client frames in the way (with shaders a
        //    frame takes seconds and starves the server thread)
        skipFrames = true;
        try {
            while (System.currentTimeMillis() < end && sp != null && !serverChunksReady()) ctx.waitTicks(20);
        } finally {
            skipFrames = false;
        }
        long t1 = System.currentTimeMillis();
        // 2) the client meshes and draws them; meanwhile a frame at most every 1.5 s
        throttleMs = 1500;
        try {
            ctx.waitTicks(10);
            while (System.currentTimeMillis() < end) {
                if (ctx.computeOnClient(mc -> {
                    if (mc.level == null || mc.player == null) return true;
                    // after a dimension change the player only moves (and sends moves) once the client has loaded
                    // the level: it needs drawn frames for that, else it gives up after 30 s
                    if (mc.getConnection() == null || mc.player == null) return false;
                    int need = Math.max(2, mc.options.getEffectiveRenderDistance() - 1);
                    int cx = mc.player.chunkPosition().x, cz = mc.player.chunkPosition().z;
                    for (int dx = -need; dx <= need; dx++) {
                        for (int dz = -need; dz <= need; dz++) {
                            if (dx * dx + dz * dz > need * need) continue;
                            if (!mc.level.getChunkSource().hasChunk(cx + dx, cz + dz)) return false;
                        }
                    }
                    return mc.levelRenderer.hasRenderedAllSections();
                })) break;
                ctx.waitTicks(10);
            }
        } finally {
            throttleMs = 0;
        }
        ctx.waitTicks(10);
        LOG.info("[bctest] settle took {} s (server {} s)", (System.currentTimeMillis() - t0) / 1000, (t1 - t0) / 1000);
    }

    /** true when every chunk within the render distance (minus one) of the player is fully loaded on the server. */
    private boolean serverChunksReady() {
        int rd = ctx.computeOnClient(mc -> mc.options.getEffectiveRenderDistance());
        return need().getServer().computeOnServer(server -> {
            var pl = server.getPlayerList().getPlayers().getFirst();
            var level = (ServerLevel) pl.level();
            int need = Math.max(2, rd - 1);
            int cx = pl.chunkPosition().x, cz = pl.chunkPosition().z;
            for (int dx = -need; dx <= need; dx++) {
                for (int dz = -need; dz <= need; dz++) {
                    if (dx * dx + dz * dz > need * need) continue;
                    if (!level.getChunkSource().hasChunk(cx + dx, cz + dz)) return false;
                }
            }
            return true;
        });
    }

    /** Renames the world folder of this run to saves/&lt;keep&gt; (unless it is that folder already). */
    private void keepWorld(Path dir) {
        Path target = dir.resolveSibling(keep);
        if (dir.getFileName().toString().equals(keep)) return;
        try {
            if (Files.exists(target)) {
                LOG.warn("[bctest] keep: {} exists, the world stays in {}", target.getFileName(), dir.getFileName());
                return;
            }
            Files.move(dir, target);
            LOG.info("[bctest] KEPT world as {}", keep);
        } catch (Exception e) {
            LOG.warn("[bctest] keep failed: {}", e.toString());
        }
    }

    // ------------------------------------------------------------------ frames

    /** Converts a screenshot to JPEG in the background (and deletes the PNG) while the next frame renders. */
    private void save(Path png, float quality) {
        if (!jpg || png == null) return;
        saver.submit(() -> {
            try {
                BufferedImage src = ImageIO.read(png.toFile());
                int w = src.getWidth(), h = src.getHeight();
                BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                int[] row = new int[w];
                for (int y = 0; y < h; y++) {
                    src.getRGB(0, y, w, 1, row, 0, w);
                    rgb.setRGB(0, y, w, 1, row, 0, w);
                }
                String name = png.getFileName().toString();
                Path dst = png.resolveSibling(name.substring(0, name.length() - 4) + ".jpg");
                ImageWriter wr = ImageIO.getImageWritersByFormatName("jpeg").next();
                ImageWriteParam prm = wr.getDefaultWriteParam();
                prm.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                prm.setCompressionQuality(quality);
                try (ImageOutputStream os = ImageIO.createImageOutputStream(dst.toFile())) {
                    wr.setOutput(os);
                    wr.write(null, new IIOImage(rgb, null, null), prm);
                } finally {
                    wr.dispose();
                }
                Files.delete(png);
            } catch (Exception e) {
                LOG.warn("[bctest] jpeg {} failed: {}", png.getFileName(), e.toString());
            }
        });
    }

    /**
     * rec &lt;name&gt; &lt;frames&gt; &lt;fps&gt; [options]: frame i shows game time i/fps s after the start (ticks run between the
     * frames, the partial tick of each frame keeps the motion smooth). The camera path is applied on the client right
     * before every frame, so it is exact even though a frame takes a second or more to render.
     */
    private void rec(String arg, Map<Integer, List<String>> at) throws Exception {
        String[] a = arg.split("\\s+");
        String name = a[0];
        int frames = Integer.parseInt(a[1]);
        double fps = Double.parseDouble(a[2]);
        Map<String, String> opt = new HashMap<>();
        for (int i = 3; i < a.length; i++) {
            int eq = a[i].indexOf('=');
            if (eq > 0) opt.put(a[i].substring(0, eq), a[i].substring(eq + 1));
        }
        Path dir = out.resolve(name);
        Files.createDirectories(dir);
        Cam cam = ctx.computeOnClient(mc -> Cam.of(mc, opt));
        double tpf = 20.0 / fps;
        long g0 = ctx.computeOnClient(mc -> mc.level == null ? 0L : mc.level.getGameTime());
        FilmSound.start(dir, fps, g0);
        long w0 = System.nanoTime();
        try {
            for (int i = 0; i < frames; i++) {
                double t = i * tpf;
                int whole = (int) Math.floor(t);
                // ticks the game has to run before this frame (actions like "use" run a tick themselves)
                skipFrames = !opt.containsKey("draw");
                try {
                    while (ctx.computeOnClient(mc -> mc.level == null ? 0L : mc.level.getGameTime()) - g0 < whole) ctx.waitTick();
                } finally {
                    skipFrames = false;
                }
                List<String> acts = at.get(i);
                if (acts != null) {
                    for (String c : acts) {
                        try {
                            exec(c);
                        } catch (RuntimeException e) {
                            LOG.warn("[bctest] rec {} action '{}' failed: {}", name, c, e.toString());
                        }
                    }
                }
                float delta = (float) (t - whole);
                if (cam != null) {
                    final int fi = i;
                    ctx.runOnClient(mc -> cam.apply(mc, fi, frames, delta));
                }
                // the framework renders the frame at once, then runs ticks until the GPU has copied it out (two per
                // frame): hold the world meanwhile, else every frame cost two game ticks and a 30 fps clip ran 3x fast
                holdWorld = true;
                Path shot;
                try {
                    shot = ctx.takeScreenshot(TestScreenshotOptions.of(String.format(Locale.ROOT, "f%05d", i)).withDeltaTicks(delta)
                            .disableCounterPrefix().withDestinationDir(dir));
                } finally {
                    holdWorld = false;
                }
                save(shot, 0.93f);
                if (i % 30 == 29) LOG.info("[bctest] rec {}: {}/{} frames, {} ms per frame", name, i + 1, frames, (System.nanoTime() - w0) / 1_000_000 / (i + 1));
            }
        } finally {
            FilmSound.stop();
            if (cam != null && cam.free) freeCam = null;
        }
        LOG.info("[bctest] REC {} {} frames at {} fps, {} ms per frame", name, frames, fps, (System.nanoTime() - w0) / 1_000_000 / Math.max(1, frames));
    }

    /** Puts the client player at a pose, with the "old" values equal so the frame shows exactly that pose. */
    static void setPose(LocalPlayer p, double x, double y, double z, float yaw, float pitch, boolean move) {
        if (move) {
            p.setPos(x, y, z);
            p.setDeltaMovement(Vec3.ZERO);
            p.xo = p.xOld = x;
            p.yo = p.yOld = y;
            p.zo = p.zOld = z;
        }
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.yRotO = yaw;
        p.xRotO = pitch;
        p.setYHeadRot(yaw);
        p.yHeadRotO = yaw;
        p.setYBodyRot(yaw);
        p.yBodyRotO = yaw;
        p.yBob = p.yBobO = yaw;
        p.xBob = p.xBobO = pitch;
    }

    /** yaw, pitch to look from (x, eyeY, z) at a point. */
    static float[] aim(double x, double eyeY, double z, Vec3 t) {
        double dx = t.x - x, dy = t.y - eyeY, dz = t.z - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new float[]{yaw, pitch};
    }

    static Entity nearest(Minecraft mc, String type) {
        Entity best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || !BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(type)) continue;
            double d = e.distanceToSqr(mc.player);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    /** A camera path for one clip (all values are of the client player; the camera follows the player). */
    static final class Cam {
        double x0, y0, z0, x1, y1, z1;
        float yaw0, pitch0, yaw1, pitch1;
        boolean ease = true, move;
        Double ocx, ocz;
        double odeg;
        Entity lookEnt;
        Vec3 lookAt;
        double chase = -1, chaseDy, smooth = 0.2, pitchOff, yawOff;
        double cx, cy, cz;
        float cyaw, cpitch;
        boolean first = true, free;

        static Cam of(Minecraft mc, Map<String, String> opt) {
            if (!(opt.containsKey("to") || opt.containsKey("by") || opt.containsKey("orbit") || opt.containsKey("look")
                    || opt.containsKey("cam"))) return null;
            LocalPlayer p = mc.player;
            Cam c = new Cam();
            c.x0 = c.x1 = p.getX();
            c.y0 = c.y1 = p.getY();
            c.z0 = c.z1 = p.getZ();
            c.yaw0 = c.yaw1 = p.getYRot();
            c.pitch0 = c.pitch1 = p.getXRot();
            if (opt.containsKey("cam")) {
                // cam=x,y,z,yaw,pitch: a detached camera starting at that eye position; the player stays visible
                double[] v = nums(opt.get("cam"));
                c.free = true;
                c.x0 = c.x1 = v[0];
                c.y0 = c.y1 = v[1] - p.getEyeHeight();
                c.z0 = c.z1 = v[2];
                c.yaw0 = c.yaw1 = (float) v[3];
                c.pitch0 = c.pitch1 = (float) v[4];
            }
            if (opt.containsKey("to")) {
                String[] v = opt.get("to").split(",");
                if (v.length > 0 && !v[0].equals("~")) c.x1 = Double.parseDouble(v[0]);
                if (v.length > 1 && !v[1].equals("~")) c.y1 = Double.parseDouble(v[1]);
                if (v.length > 2 && !v[2].equals("~")) c.z1 = Double.parseDouble(v[2]);
                if (v.length > 3 && !v[3].equals("~")) c.yaw1 = Float.parseFloat(v[3]);
                if (v.length > 4 && !v[4].equals("~")) c.pitch1 = Float.parseFloat(v[4]);
            }
            if (opt.containsKey("by")) {
                double[] v = nums(opt.get("by"));
                if (v.length > 0) c.x1 += v[0];
                if (v.length > 1) c.y1 += v[1];
                if (v.length > 2) c.z1 += v[2];
                if (v.length > 3) c.yaw1 += (float) v[3];
                if (v.length > 4) c.pitch1 += (float) v[4];
            }
            if (opt.containsKey("orbit")) {
                double[] v = nums(opt.get("orbit"));
                c.ocx = v[0];
                c.ocz = v[1];
                c.odeg = v[2];
            }
            if (opt.containsKey("look")) {
                String l = opt.get("look");
                if (l.contains(":")) {
                    c.lookEnt = nearest(mc, l);
                    if (c.lookEnt == null) LOG.warn("[bctest] rec: no {} to look at", l);
                } else {
                    double[] v = nums(l);
                    c.lookAt = new Vec3(v[0], v[1], v[2]);
                }
            }
            if (opt.containsKey("chase")) {
                double[] v = nums(opt.get("chase"));
                c.chase = v[0];
                c.chaseDy = v.length > 1 ? v[1] : 0;
            }
            if (opt.containsKey("smooth")) c.smooth = Double.parseDouble(opt.get("smooth"));
            if (opt.containsKey("pitch")) c.pitchOff = Double.parseDouble(opt.get("pitch"));
            if (opt.containsKey("yawoff")) c.yawOff = Double.parseDouble(opt.get("yawoff"));
            if (opt.containsKey("ease")) c.ease = !opt.get("ease").equals("0");
            c.move = c.x0 != c.x1 || c.y0 != c.y1 || c.z0 != c.z1 || c.ocx != null || c.chase > 0;
            if (c.move && !c.free && !p.isSpectator() && p.getAbilities().mayfly) {
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
            }
            return c;
        }

        void apply(Minecraft mc, int i, int n, float delta) {
            LocalPlayer p = mc.player;
            double f = n <= 1 ? 1 : i / (double) (n - 1);
            double e = ease ? f * f * (3 - 2 * f) : f;
            double x = x0 + (x1 - x0) * e, y = y0 + (y1 - y0) * e, z = z0 + (z1 - z0) * e;
            float yaw = (float) (yaw0 + (yaw1 - yaw0) * e), pitch = (float) (pitch0 + (pitch1 - pitch0) * e);
            if (ocx != null) {
                double ang = Math.toRadians(odeg * e), dx = x - ocx, dz = z - ocz;
                x = ocx + dx * Math.cos(ang) - dz * Math.sin(ang);
                z = ocz + dx * Math.sin(ang) + dz * Math.cos(ang);
                yaw += (float) (odeg * e);
            }
            if (lookEnt != null || lookAt != null) {
                Vec3 t = lookEnt != null ? lookEnt.getPosition(delta).add(0, lookEnt.getBbHeight() * 0.5, 0) : lookAt;
                if (chase > 0) {
                    if (first) {
                        cx = free ? x0 : p.getX();
                        cy = free ? y0 : p.getY();
                        cz = free ? z0 : p.getZ();
                    }
                    double dx = cx - t.x, dz = cz - t.z, d = Math.sqrt(dx * dx + dz * dz);
                    if (d < 1e-3) {
                        dx = 1;
                        d = 1;
                    }
                    double wx = t.x + dx / d * chase, wy = t.y + chaseDy - p.getEyeHeight(), wz = t.z + dz / d * chase;
                    double k = first ? 1 : smooth;
                    cx += (wx - cx) * k;
                    cy += (wy - cy) * k;
                    cz += (wz - cz) * k;
                    x = cx;
                    y = cy;
                    z = cz;
                }
                float[] r = aim(x, y + p.getEyeHeight(), z, t);
                if (first) {
                    cyaw = r[0];
                    cpitch = r[1];
                } else {
                    cyaw += (float) (Mth.wrapDegrees(r[0] - cyaw) * smooth);
                    cpitch += (float) ((r[1] - cpitch) * smooth);
                }
                yaw = (float) (cyaw + yawOff);
                pitch = (float) Mth.clamp(cpitch + pitchOff, -90, 90);
            }
            first = false;
            if (free) {
                freeCam = new double[]{x, y + p.getEyeHeight(), z, yaw, pitch};
            } else {
                setPose(p, x, y, z, yaw, pitch, move);
            }
        }
    }

    // ------------------------------------------------------------------ scouting (server side, results go to the log)

    private static String scout(net.minecraft.server.MinecraftServer server, String id, BlockPos from) {
        var pl = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("raft:raftdim")));
        if (level == null) level = (ServerLevel) pl.level();
        var reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var holder = reg.get(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(id))).orElse(null);
        if (holder == null) return "unknown structure";
        var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), from != null ? from : pl.blockPosition(), 100, false);
        if (found == null && level != pl.level()) {
            // not a structure of the raft dimension (e.g. the driftwood stumps): look in the player's dimension
            level = (ServerLevel) pl.level();
            found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), from != null ? from : pl.blockPosition(), 100, false);
        }
        if (found == null) return "none nearby";
        BlockPos bp = found.getFirst();
        StructureStart st = level.getChunk(bp.getX() >> 4, bp.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(holder.value());
        if (st == null || !st.isValid()) return "at " + bp.toShortString() + " (no start)";
        StringBuilder sb = new StringBuilder("at ").append(bp.toShortString()).append(" bbox ").append(box(st.getBoundingBox()));
        for (StructurePiece pc : st.getPieces()) {
            sb.append(" | ");
            if (pc instanceof PoolElementStructurePiece pe) sb.append(pe.getElement()).append(' ').append(pe.getRotation()).append(' ');
            sb.append(box(pc.getBoundingBox()));
        }
        return sb.toString();
    }

    private static String box(BoundingBox b) {
        return b.minX() + " " + b.minY() + " " + b.minZ() + " .. " + b.maxX() + " " + b.maxY() + " " + b.maxZ();
    }

    private static List<String> chests(net.minecraft.server.MinecraftServer server, int r) {
        var pl = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = (ServerLevel) pl.level();
        BlockPos c = pl.blockPosition();
        List<String> res = new ArrayList<>();
        int rc = (r >> 4) + 1;
        for (int cx = (c.getX() >> 4) - rc; cx <= (c.getX() >> 4) + rc; cx++) {
            for (int cz = (c.getZ() >> 4) - rc; cz <= (c.getZ() >> 4) + rc; cz++) {
                LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
                if (ch == null) continue;
                for (BlockEntity be : ch.getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (Math.abs(p.getX() - c.getX()) > r || Math.abs(p.getZ() - c.getZ()) > r) continue;
                    String block = BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).toString();
                    if (be instanceof RandomizableContainer rc2) {
                        var lt = rc2.getLootTable();
                        res.add("CHEST " + p.getX() + " " + p.getY() + " " + p.getZ() + " " + block + " " + (lt == null ? "-" : lt.location()));
                    } else if (block.startsWith("raft:")) {
                        res.add("BE " + p.getX() + " " + p.getY() + " " + p.getZ() + " " + block + " " + be.getBlockState());
                    }
                }
            }
        }
        if (res.isEmpty()) res.add("CHEST none within " + r);
        return res;
    }

    private static BlockPos nearestBlock(net.minecraft.server.MinecraftServer server, String id, int r) {
        var pl = server.getPlayerList().getPlayers().getFirst();
        List<BlockPos> hits = scan(server, id, r);
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos h : hits) {
            double d = h.distSqr(pl.blockPosition());
            if (d < bd) {
                bd = d;
                best = h;
            }
        }
        return best;
    }

    private static List<BlockPos> scan(net.minecraft.server.MinecraftServer server, String id, int r) {
        var pl = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = (ServerLevel) pl.level();
        Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        BlockPos c = pl.blockPosition();
        List<BlockPos> hits = new ArrayList<>();
        int rc = (r >> 4) + 1;
        for (int cx = (c.getX() >> 4) - rc; cx <= (c.getX() >> 4) + rc; cx++) {
            for (int cz = (c.getZ() >> 4) - rc; cz <= (c.getZ() >> 4) + rc; cz++) {
                LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
                if (ch == null) continue;
                LevelChunkSection[] secs = ch.getSections();
                for (int si = 0; si < secs.length; si++) {
                    LevelChunkSection s = secs[si];
                    if (s == null || s.hasOnlyAir() || !s.maybeHas(st -> st.is(b))) continue;
                    int y0 = level.getSectionYFromSectionIndex(si) << 4;
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                if (s.getBlockState(x, y, z).is(b)) hits.add(new BlockPos((cx << 4) + x, y0 + y, (cz << 4) + z));
                            }
                        }
                    }
                }
            }
        }
        return hits;
    }

    private static String find(net.minecraft.server.MinecraftServer server, String id, int r) {
        List<BlockPos> hits = scan(server, id, r);
        if (hits.isEmpty()) return "none within " + r;
        int minX = Integer.MAX_VALUE, minY = minX, minZ = minX, maxX = Integer.MIN_VALUE, maxY = maxX, maxZ = maxX;
        for (BlockPos h : hits) {
            minX = Math.min(minX, h.getX());
            minY = Math.min(minY, h.getY());
            minZ = Math.min(minZ, h.getZ());
            maxX = Math.max(maxX, h.getX());
            maxY = Math.max(maxY, h.getY());
            maxZ = Math.max(maxZ, h.getZ());
        }
        StringBuilder sb = new StringBuilder(hits.size() + " in " + minX + " " + minY + " " + minZ + " .. " + maxX + " " + maxY + " " + maxZ + ":");
        for (int i = 0; i < Math.min(24, hits.size()); i++) sb.append(' ').append(hits.get(i).toShortString().replace(" ", ""));
        return sb.toString();
    }
}
