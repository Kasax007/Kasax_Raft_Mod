package net.fabricmc.fabric.api.client.gametest.v1.world;

import java.nio.file.Path;

/** A world folder of the test (see FabricClientGameTest). */
public interface TestWorldSave {
    Path getSaveDirectory();
}
