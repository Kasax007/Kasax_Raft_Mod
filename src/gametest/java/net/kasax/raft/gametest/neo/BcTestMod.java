package net.kasax.raft.gametest.neo;

import java.nio.file.Path;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev-only test mod (common side): NeoForge game tests ({@link BcGameTests}), and two checks driven by system
 * properties when a server has started: {@code -Dbc.audit=true} loads every mixin target (all mixins must apply),
 * {@code -Dbc.dump=<file>} writes {@link RegistryDump}.
 */
@Mod("raft_gametest")
public class BcTestMod {
    public static final Logger LOG = LoggerFactory.getLogger("bctest");

    public BcTestMod(IEventBus modBus) {
        BcGameTests.register(modBus);
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, e -> {
            if (Boolean.getBoolean("bc.audit")) {
                LOG.info("[bctest] mixin audit starts");
                org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
                LOG.info("[bctest] mixin audit done");
            }
            String maps = System.getProperty("bc.datamaps");
            if (maps != null) {
                try {
                    DataMapExport.write(Path.of(maps), e.getServer().registryAccess());
                    LOG.info("[bctest] data maps written to {}", maps);
                } catch (Exception ex) {
                    LOG.error("[bctest] data map export failed", ex);
                }
            }
            String dump = System.getProperty("bc.dump");
            if (dump != null) {
                try {
                    RegistryDump.dump(e.getServer(), Path.of(dump));
                    LOG.info("[bctest] registry dump written to {}", dump);
                } catch (Exception ex) {
                    LOG.error("[bctest] registry dump failed", ex);
                }
            }
        });
    }
}
