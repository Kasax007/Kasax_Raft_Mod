package net.kasax.raft.platform;

import java.nio.file.Path;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

/** The part of Fabric's FabricLoader Beach Craft uses, on FML. */
public final class FabricLoader {
    private static final FabricLoader INSTANCE = new FabricLoader();

    private FabricLoader() {
    }

    public static FabricLoader getInstance() {
        return INSTANCE;
    }

    public boolean isModLoaded(String id) {
        ModList list = ModList.get();
        return list != null ? list.isLoaded(id) : FMLLoader.getLoadingModList().getModFileById(id) != null;
    }

    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
