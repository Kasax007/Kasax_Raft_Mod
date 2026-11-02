package net.kasax.raft.gametest.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Fabric's client gametest (fabric-client-gametest-api-v1 6.0.11) hands every screenshot to a consumer that saves it
 * but never closes the NativeImage: 8 MB of native memory per 1080x1920 frame, so a film run is OOM-killed after
 * about 600 frames. Close the image once the screenshot function (save or compare) has run; closing twice is safe.
 */
@Mixin(targets = "net.fabricmc.fabric.impl.client.gametest.context.ClientGameTestContextImpl", remap = false)
public class ScreenshotLeakMixin {
    @Inject(method = "lambda$doTakeScreenshot$3", at = @At("TAIL"), remap = false)
    private static void bctest$closeImage(CompletableFuture<?> future, Function<?, ?> function, NativeImage image, CallbackInfo ci) {
        image.close();
    }
}
