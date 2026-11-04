package net.kasax.raft.gametest.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

/**
 * The dev client logs in as a random "PlayerNNN" on every start, so the default skin (picked from the UUID) changed
 * from run to run. For footage that is cut together the player must look the same: always log in as "Castaway" with a
 * fixed UUID (default skin: Steve, wide arms).
 */
@Mixin(Minecraft.class)
public class UserMixin {
    @Unique
    private static final UUID BCTEST_ID = UUID.fromString("6513270e-269e-3d37-b2a7-4de452e6b438");
    @Unique
    private static final User BCTEST_USER = new User("Castaway", BCTEST_ID, "", Optional.empty(), Optional.empty());

    @Inject(method = "getUser", at = @At("HEAD"), cancellable = true)
    private void bctest$user(CallbackInfoReturnable<User> cir) {
        cir.setReturnValue(BCTEST_USER);
    }

    @Inject(method = "getGameProfile", at = @At("HEAD"), cancellable = true)
    private void bctest$profile(CallbackInfoReturnable<GameProfile> cir) {
        cir.setReturnValue(new GameProfile(BCTEST_ID, "Castaway"));
    }
}
