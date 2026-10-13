package dev.chrynalogia.probe;
import java.util.List;

/** Supported choices, localized card labels and descriptions have one owner. */
final class GridCatalog {
    record Entry(String id,int nameRes,int descriptionRes){}
    private static final List<Entry> ENTRIES=List.of(
        new Entry("thirds",R.string.grid_thirds,R.string.desc_thirds),new Entry("cross",R.string.grid_cross,R.string.desc_cross),
        new Entry("golden_grid",R.string.grid_golden_grid,R.string.desc_golden_grid),new Entry("spiral",R.string.grid_spiral,R.string.desc_spiral),
        new Entry("x",R.string.grid_x,R.string.desc_x),new Entry("diagonal",R.string.grid_diagonal,R.string.desc_diagonal),
        new Entry("triangle",R.string.grid_triangle,R.string.desc_triangle),new Entry("rabatment",R.string.grid_rabatment,R.string.desc_rabatment),
        new Entry("dynamic",R.string.grid_dynamic,R.string.desc_dynamic),new Entry("custom",R.string.grid_custom,R.string.desc_custom));
    static List<Entry> entries(){return ENTRIES;}
    static Entry find(String id){for(Entry entry:ENTRIES)if(entry.id.equals(id))return entry;return null;}
    static boolean contains(String id){return find(id)!=null;}
}
