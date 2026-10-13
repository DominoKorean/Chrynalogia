package dev.chrynalogia.probe;
import org.junit.Test;
import static org.junit.Assert.*;

public class GridCatalogTest {
    @Test public void supportedGridCardsHaveUniqueIdsAndKeepGoldenGridAndSpiral() throws Exception {
        Class<?> catalog=Class.forName("dev.chrynalogia.probe.GridCatalog");var method=catalog.getDeclaredMethod("entries");method.setAccessible(true);
        java.util.List<?> cards=(java.util.List<?>)method.invoke(null);java.util.Set<String> ids=new java.util.HashSet<>();
        for(Object card:cards){var id=card.getClass().getDeclaredMethod("id");id.setAccessible(true);assertTrue(ids.add((String)id.invoke(card)));}
        assertEquals(10,ids.size());assertTrue(ids.contains("golden_grid"));assertTrue(ids.contains("spiral"));assertTrue(ids.contains("custom"));assertFalse(ids.contains("golden_ratio"));
    }
}
