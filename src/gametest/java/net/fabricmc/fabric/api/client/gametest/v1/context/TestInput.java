package net.fabricmc.fabric.api.client.gametest.v1.context;

import com.mojang.blaze3d.platform.InputConstants;
import net.kasax.raft.gametest.mixin.MouseHandlerInvoker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;

/**
 * Keys and mouse of the test (see FabricClientGameTest). In the world a key mapping is pressed like the keyboard
 * does it (held + one click); with a screen open the key or mouse event goes through the game's own input handlers,
 * so screens get real key presses and clicks.
 */
public final class TestInput {
    private final ClientGameTestContext ctx;
    private double cursorX;
    private double cursorY;

    TestInput(ClientGameTestContext ctx) {
        this.ctx = ctx;
    }

    private static InputConstants.Key keyOf(Minecraft mc, Function<Options, KeyMapping> mapping) {
        return InputConstants.getKey(mapping.apply(mc.options).saveString());
    }

    private static void key(Minecraft mc, InputConstants.Key key, boolean down) {
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE && mc.screen != null) {
            ((MouseHandlerInvoker) mc.mouseHandler).bc$onPress(window, key.getValue(), down ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE, 0);
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            // in the world: the key mapping alone (the mouse handler would grab the mouse and could count the click twice)
            KeyMapping.set(key, down);
            if (down) {
                KeyMapping.click(key);
            }
        } else if (mc.screen != null) {
            mc.keyboardHandler.keyPress(window, key.getValue(), 0, down ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE, 0);
        } else {
            KeyMapping.set(key, down);
            if (down) {
                KeyMapping.click(key);
            }
        }
    }

    public void holdKey(Function<Options, KeyMapping> mapping) {
        this.ctx.runOnClient(mc -> key(mc, keyOf(mc, mapping), true));
    }

    public void releaseKey(Function<Options, KeyMapping> mapping) {
        this.ctx.runOnClient(mc -> key(mc, keyOf(mc, mapping), false));
    }

    public void pressKey(Function<Options, KeyMapping> mapping) {
        this.holdKey(mapping);
        this.ctx.waitTick();
        this.releaseKey(mapping);
    }

    /** A key by its GLFW key code (arrows in screens). */
    public void pressKey(int keyCode) {
        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        this.ctx.runOnClient(mc -> key(mc, key, true));
        this.ctx.waitTick();
        this.ctx.runOnClient(mc -> key(mc, key, false));
    }

    public void holdKeyFor(Function<Options, KeyMapping> mapping, int ticks) {
        this.holdKey(mapping);
        this.ctx.waitTicks(ticks);
        this.releaseKey(mapping);
    }

    /** A raw key by its GLFW key code held for some ticks (keys of other mods, e.g. JEI's R and U). */
    public void holdKeyFor(int keyCode, int ticks) {
        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        this.ctx.runOnClient(mc -> key(mc, key, true));
        this.ctx.waitTicks(ticks);
        this.ctx.runOnClient(mc -> key(mc, key, false));
    }

    /** Types text into the focused text box of the open screen (character events like the keyboard sends them). */
    public void typeChars(String text) {
        this.ctx.runOnClient(mc -> {
            long window = mc.getWindow().getWindow();
            for (char c : text.toCharArray()) {
                ((net.kasax.raft.gametest.mixin.KeyboardHandlerInvoker) mc.keyboardHandler).bc$charTyped(window, c, 0);
            }
        });
        this.ctx.waitTick();
    }

    public void setCursorPos(double x, double y) {
        this.cursorX = x;
        this.cursorY = y;
        this.ctx.runOnClient(mc -> ((MouseHandlerInvoker) mc.mouseHandler).bc$onMove(mc.getWindow().getWindow(), x, y));
    }

    public void pressMouse(int button) {
        this.ctx.runOnClient(mc -> ((MouseHandlerInvoker) mc.mouseHandler).bc$onPress(mc.getWindow().getWindow(), button, GLFW.GLFW_PRESS, 0));
        this.ctx.waitTick();
        this.ctx.runOnClient(mc -> ((MouseHandlerInvoker) mc.mouseHandler).bc$onPress(mc.getWindow().getWindow(), button, GLFW.GLFW_RELEASE, 0));
    }

    public void scroll(double amount) {
        this.ctx.runOnClient(mc -> ((MouseHandlerInvoker) mc.mouseHandler).bc$onScroll(mc.getWindow().getWindow(), 0.0, amount));
    }

    public void resizeWindow(int width, int height) {
        this.ctx.runOnClient(mc -> mc.getWindow().setWindowed(width, height));
        this.ctx.waitTicks(5);
    }
}
