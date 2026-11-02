package net.kasax.raft.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.impl.client.gametest.context.ClientGameTestContextImpl;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Test helper only. The game test snapshots the default options early; key mappings whose key had no
 * name at that moment (or that came later) end up as null and the option reset crashes in
 * InputConstants.getKey(null). Fill those gaps with the current value.
 */
public class KeyFix implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(KeyFix::fill);
        ScreenEvents.BEFORE_INIT.register((mc, screen, w, h) -> fill(mc));
    }

    @SuppressWarnings("unchecked")
    static void fill(Minecraft mc) {
        try {
            Field f = ClientGameTestContextImpl.class.getDeclaredField("DEFAULT_GAME_OPTIONS");
            f.setAccessible(true);
            Map<String, Object> map = (Map<String, Object>) f.get(null);
            int fixed = 0;
            for (KeyMapping km : mc.options.keyMappings) {
                String k = "key_" + km.getName();
                if (map.get(k) == null) {
                    String v = km.saveString();
                    map.put(k, v != null ? v : "key.keyboard.unknown");
                    fixed++;
                }
            }
            if (fixed > 0) BcScript.LOG.info("[bctest] filled {} missing default key options", fixed);
        } catch (Exception e) {
            BcScript.LOG.warn("[bctest] key fix failed: {}", e.toString());
        }
    }
}
