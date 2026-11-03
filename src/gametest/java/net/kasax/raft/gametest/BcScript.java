package net.kasax.raft.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.Difficulty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Scripted in-game test for Beach Craft. Reads the script file given by -Dbc.script (one command per line) and writes
 * screenshots to -Dbc.out. Commands (see tools/bctest/README.md):
 * <pre>
 * world [seed]            create a creative world with cheats (needed before server commands)
 * cmd &lt;command&gt;          run a server command as the server (use @p for the player)
 * pcmd &lt;command&gt;         run a command as the player, at the player's position
 * wait &lt;ticks&gt;           wait game ticks
 * settle [seconds]        wait until all chunk sections around the camera are drawn (default max 40 s)
 * shot &lt;name&gt;            screenshot to OUT/&lt;name&gt;.png
 * size &lt;w&gt; &lt;h&gt;          window size (default 1280 720)
 * hud on|off, third on|off, gui &lt;scale&gt;, rd &lt;chunks&gt;
 * use | attack | sneak    press the use / attack / sneak key once
 * sneakuse                hold sneak and press use (sneak-use an item or block)
 * esc                     close the open screen
 * slot &lt;1-9&gt;              select a hotbar slot
 * count [dimension]       log the loaded entities per type (COUNT line in latest.log)
 * hold &lt;use|attack|forward|jump|sneak&gt; &lt;ticks&gt;
 * log &lt;text&gt;
 * </pre>
 */
public class BcScript implements FabricClientGameTest {
    public static final Logger LOG = LoggerFactory.getLogger("bctest");

    @Override
    public void runTest(ClientGameTestContext ctx) {
        Path out = Path.of(System.getProperty("bc.out", "bctest-out")).toAbsolutePath();
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
            mc.resizeGui();
        });
        TestSingleplayerContext sp = null;
        int n = 0;
        try {
            for (String raw : lines) {
                n++;
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\s+", 2);
                String cmd = p[0];
                String arg = p.length > 1 ? p[1] : "";
                LOG.info("[bctest] {}: {}", n, line);
                switch (cmd) {
                    case "world" -> {
                        String seed = arg.isEmpty() ? "beachcraft" : arg;
                        sp = ctx.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
                            s.setSeed(seed);
                            s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                            s.setAllowCommands(true);
                            s.setDifficulty(Difficulty.NORMAL);
                        }).create();
                        sp.getServer().runCommand("gamerule advance_time false");
                        sp.getServer().runCommand("gamerule advance_weather false");
                        sp.getServer().runCommand("time set 6000");
                        sp.getServer().runCommand("weather clear");
                        settle(ctx, 40);
                    }
                    case "cmd" -> need(sp).getServer().runCommand(arg);
                    case "pcmd" -> need(sp).getServer().runCommand("execute as @p at @p run " + arg);
                    case "wait" -> ctx.waitTicks(Integer.parseInt(arg));
                    case "settle" -> {
                        long t0 = System.currentTimeMillis();
                        if (sp != null) {
                            ctx.waitTicks(20);
                            try {
                                sp.getConnection().waitForChunksRender();
                            } catch (Throwable t) {
                                LOG.warn("[bctest] chunks not ready in time: {}", t.toString());
                            }
                        }
                        settle(ctx, arg.isEmpty() ? 40 : Integer.parseInt(arg));
                        LOG.info("[bctest] settle took {} s", (System.currentTimeMillis() - t0) / 1000);
                    }
                    case "shot" -> ctx.takeScreenshot(TestScreenshotOptions.of(arg).disableCounterPrefix().withDestinationDir(out));
                    case "size" -> {
                        String[] wh = arg.split("\\s+");
                        ctx.getInput().resizeWindow(Integer.parseInt(wh[0]), Integer.parseInt(wh[1]));
                    }
                    case "hud" -> ctx.runOnClient(mc -> { if (mc.gui.hud.isHidden() != arg.equals("off")) mc.gui.hud.toggle(); });
                    case "third" -> ctx.runOnClient(mc -> mc.options.setCameraType(arg.equals("on") ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON));
                    case "gui" -> ctx.runOnClient(mc -> {
                        mc.options.guiScale().set(Integer.parseInt(arg));
                        mc.resizeGui();
                    });
                    case "rd" -> ctx.runOnClient(mc -> mc.options.renderDistance().set(Integer.parseInt(arg)));
                    case "use" -> ctx.getInput().pressKey(o -> o.keyUse);
                    case "attack" -> ctx.getInput().pressKey(o -> o.keyAttack);
                    case "sneak" -> ctx.getInput().pressKey(o -> o.keyShift);
                    case "sneakuse" -> {
                        ctx.getInput().holdKey(o -> o.keyShift);
                        ctx.waitTicks(3);
                        ctx.getInput().pressKey(o -> o.keyUse);
                        ctx.waitTicks(3);
                        ctx.getInput().releaseKey(o -> o.keyShift);
                    }
                    case "esc" -> ctx.setScreen(() -> null);
                    case "slot" -> {
                        int i = Integer.parseInt(arg) - 1;
                        ctx.getInput().pressKey(o -> o.keyHotbarSlots[i]);
                    }
                    case "hold" -> {
                        String[] a = arg.split("\\s+");
                        int ticks = Integer.parseInt(a[1]);
                        switch (a[0]) {
                            case "use" -> ctx.getInput().holdKeyFor(o -> o.keyUse, ticks);
                            case "attack" -> ctx.getInput().holdKeyFor(o -> o.keyAttack, ticks);
                            case "forward" -> ctx.getInput().holdKeyFor(o -> o.keyUp, ticks);
                            case "jump" -> ctx.getInput().holdKeyFor(o -> o.keyJump, ticks);
                            case "sneak" -> ctx.getInput().holdKeyFor(o -> o.keyShift, ticks);
                            default -> LOG.warn("[bctest] unknown hold key {}", a[0]);
                        }
                    }
                    case "log" -> LOG.info("[bctest] LOG {}", arg);
                    case "count" -> {
                        // count <dimension>: entities per type loaded in that dimension (written to the log)
                        String dim = arg.isEmpty() ? "raft:raftdim" : arg;
                        String res = need(sp).getServer().computeOnServer(server -> {
                            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                                    net.minecraft.resources.Identifier.parse(dim));
                            var level = server.getLevel(key);
                            if (level == null) return "no level " + dim;
                            java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
                            for (var e : level.getAllEntities()) {
                                counts.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(), 1, Integer::sum);
                            }
                            return counts.toString();
                        });
                        LOG.info("[bctest] COUNT {} {}", dim, res);
                    }
                    default -> LOG.warn("[bctest] unknown command '{}' in line {}", cmd, n);
                }
            }
        } finally {
            if (sp != null) sp.close();
        }
        LOG.info("[bctest] script done");
    }

    private static TestSingleplayerContext need(TestSingleplayerContext sp) {
        if (sp == null) throw new IllegalStateException("'world' must come before server commands");
        return sp;
    }

    private static void settle(ClientGameTestContext ctx, int maxSeconds) {
        ctx.waitTicks(10);
        for (int i = 0; i < maxSeconds * 2; i++) {
            if (ctx.computeOnClient(mc -> mc.levelRenderer.hasRenderedAllSections())) break;
            ctx.waitTicks(10);
        }
        ctx.waitTicks(10);
    }
}
