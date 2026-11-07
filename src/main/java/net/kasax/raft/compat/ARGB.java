package net.kasax.raft.compat;

/** 1.21.1 port helper: the newer game's ARGB colour helpers. */
public final class ARGB {
    private ARGB() {
    }

    public static int alpha(int c) { return c >>> 24; }
    public static int red(int c) { return c >> 16 & 255; }
    public static int green(int c) { return c >> 8 & 255; }
    public static int blue(int c) { return c & 255; }

    public static int color(int a, int r, int g, int b) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
    }

    public static int color(int r, int g, int b) {
        return color(255, r, g, b);
    }

    public static int color(int alpha, int rgb) {
        return (alpha & 255) << 24 | rgb & 0xFFFFFF;
    }

    public static int as8BitChannel(float v) {
        return (int) Math.floor(v * 255.0F);
    }

    public static int colorFromFloat(float a, float r, float g, float b) {
        return color(as8BitChannel(a), as8BitChannel(r), as8BitChannel(g), as8BitChannel(b));
    }

    public static int white(float alpha) {
        return color(as8BitChannel(alpha), 0xFFFFFF);
    }

    public static int white(int alpha) {
        return color(alpha, 0xFFFFFF);
    }

    public static int black(int alpha) {
        return color(alpha, 0);
    }

    public static int black(float alpha) {
        return color(as8BitChannel(alpha), 0);
    }

    public static int opaque(int c) {
        return c | 0xFF000000;
    }

    public static int multiply(int a, int b) {
        return color(alpha(a) * alpha(b) / 255, red(a) * red(b) / 255, green(a) * green(b) / 255, blue(a) * blue(b) / 255);
    }

    public static int lerp(float t, int a, int b) {
        return color((int) (alpha(a) + (alpha(b) - alpha(a)) * t), (int) (red(a) + (red(b) - red(a)) * t),
                (int) (green(a) + (green(b) - green(a)) * t), (int) (blue(a) + (blue(b) - blue(a)) * t));
    }
}
