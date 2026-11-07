package net.kasax.raft.content.guide.client;

import net.kasax.raft.content.guide.item.AlmanacItem;
import net.minecraft.client.Minecraft;

/** Client-only init of the "guide" module (the almanac screen). */
public final class GuideClient {
    private GuideClient() {
    }

    public static void init() {
        AlmanacItem.openScreen = stack -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new AlmanacScreen(stack));
        };
    }
}
