package dev.chrynalogia.probe;

import java.util.ArrayList;
import java.util.List;

/** Geometry in preview pixels; diagonals use actual aspect ratio, not normalized squares. */
final class GridGeometry {
    static final String[] IDS=GridCatalog.entries().stream().map(GridCatalog.Entry::id).toArray(String[]::new);
    record Segment(float x1, float y1, float x2, float y2) {}
    static List<Segment> lines(String id, float w, float h, int columns, int rows) {
        List<Segment> result = new ArrayList<>();
        switch (id) {
            case "golden_grid" -> grid(result, w, h, new float[]{0.381966f, 0.618034f});
            case "thirds" -> grid(result, w, h, new float[]{1f/3, 2f/3});
            case "cross" -> grid(result, w, h, new float[]{0.5f});
            case "custom" -> {
                for (int i=1; i<columns; i++) add(result,w*i/columns,0,w*i/columns,h);
                for (int i=1; i<rows; i++) add(result,0,h*i/rows,w,h*i/rows);
            }
            case "x" -> diagonals(result,w,h);
            case "diagonal" -> {
                float d=Math.min(w,h);
                add(result,0,0,d,d); add(result,w,0,w-d,d);
                add(result,0,h,d,h-d); add(result,w,h,w-d,h-d);
            }
            case "triangle" -> triangle(result,w,h,false);
            case "rabatment" -> {
                float d=Math.min(w,h);
                if(w>=h) { add(result,d,0,d,h); add(result,w-d,0,w-d,h); }
                else { add(result,0,d,w,d); add(result,0,h-d,w,h-d); }
            }
            case "dynamic" -> { diagonals(result,w,h); triangle(result,w,h,true); grid(result,w,h,new float[]{0.5f}); }
            default -> { }
        }
        return result;
    }
    private static void add(List<Segment> list,float a,float b,float c,float d) {list.add(new Segment(a,b,c,d));}
    private static void grid(List<Segment> list,float w,float h,float[] fractions) {
        for(float f:fractions) {add(list,w*f,0,w*f,h); add(list,0,h*f,w,h*f);}
    }
    private static void diagonals(List<Segment> list,float w,float h) {add(list,0,0,w,h);add(list,w,0,0,h);}
    private static void triangle(List<Segment> list,float w,float h,boolean omitDiagonal) {
        if(!omitDiagonal) add(list,0,0,w,h);
        float t=w*w/(w*w+h*h);
        add(list,w,0,w*t,h*t); add(list,0,h,w*(1-t),h*(1-t));
        if(omitDiagonal) {add(list,0,0,w*(1-t),h*t);add(list,w,h,w*t,h*(1-t));}
    }
}
