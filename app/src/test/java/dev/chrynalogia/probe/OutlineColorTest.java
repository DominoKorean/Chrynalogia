package dev.chrynalogia.probe;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class OutlineColorTest {
    @Test public void choosesContrastingEdgeForLightAndDarkLines() {
        assertEquals(0xff000000,OutlineColor.forLine(0xffffffff));
        assertEquals(0xffffffff,OutlineColor.forLine(0xff000000));
        assertEquals(0xff000000,OutlineColor.forLine(0xffffd25a));
        assertEquals(0xffffffff,OutlineColor.forLine(0xff172554));
    }
    @Test public void opacityDoesNotChangeEdgeChoice() {
        assertEquals(OutlineColor.forLine(0xffffffff),OutlineColor.forLine(0x10ffffff));
    }
}
