package net.fabricmc.fabric.api.client.gametest.v1.screenshot;

import java.nio.file.Path;

/** Screenshot name and folder (see FabricClientGameTest). */
public final class TestScreenshotOptions {
    final String name;
    boolean counterPrefix = true;
    Path dir;
    float deltaTicks = 1.0F;

    private TestScreenshotOptions(String name) {
        this.name = name;
    }

    public static TestScreenshotOptions of(String name) {
        return new TestScreenshotOptions(name);
    }

    public TestScreenshotOptions disableCounterPrefix() {
        this.counterPrefix = false;
        return this;
    }

    public TestScreenshotOptions withDestinationDir(Path dir) {
        this.dir = dir;
        return this;
    }

    int width = -1;
    int height = -1;

    /** Scale the picture to this size. */
    public TestScreenshotOptions withSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public TestScreenshotOptions withDeltaTicks(float delta) {
        this.deltaTicks = delta;
        return this;
    }

    public String name() {
        return this.name;
    }

    public boolean counterPrefix() {
        return this.counterPrefix;
    }

    public Path dir() {
        return this.dir;
    }

    public float deltaTicks() {
        return this.deltaTicks;
    }
}
