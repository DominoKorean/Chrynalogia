package dev.chrynalogia.probe;

/** Chooses the black or white edge with the highest contrast to the line. */
final class OutlineColor {
    private OutlineColor() {}
    static int forLine(int color) {
        double luminance = 0.2126 * channel((color >> 16) & 255)
            + 0.7152 * channel((color >> 8) & 255) + 0.0722 * channel(color & 255);
        return luminance > 0.1791287847 ? 0xff000000 : 0xffffffff;
    }
    private static double channel(int component) {
        double value = component / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
