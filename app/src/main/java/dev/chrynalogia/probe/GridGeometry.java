package dev.chrynalogia.probe;

import java.util.ArrayList;
import java.util.List;

/** Geometry in preview pixels; diagonals use actual aspect ratio, not normalized squares. */
final class GridGeometry {
    static final String[] IDS = {"golden_ratio", "thirds", "cross", "golden_grid", "spiral", "x", "diagonal", "triangle", "rabatment", "dynamic", "custom"};
    static final String[] NAMES = {"황금비", "3분할법", "중앙 십자선", "황금비 격자", "황금 나선", "대각선 X자", "대각선법", "황금 삼각형", "라바트망", "동적 대칭", "커스텀 직각 격자"};
    static final String[] DESCRIPTIONS = {
        "61.8% 분할선으로 피사체의 중심을 배치합니다.", "화면을 가로·세로 세 부분으로 나눕니다.",
        "화면 중앙에서 정렬과 대칭을 확인합니다.", "38.2%와 61.8% 지점에 황금비 격자를 표시합니다.",
        "황금 직사각형 안에서 점차 좁아지는 나선입니다.", "양쪽 모서리를 연결해 중심으로 시선을 모읍니다.",
        "각 모서리에서 45° 선을 그어 구도를 잡습니다.", "대각선과 그에 수직인 두 선으로 화면을 나눕니다.",
        "짧은 변 길이의 정사각형을 양쪽에 겹쳐 배치합니다.", "대각선·수직 대각선·중앙선을 함께 표시합니다.",
        "가로와 세로의 칸 수를 각각 지정합니다."};
    record Segment(float x1, float y1, float x2, float y2) {}
    static List<Segment> lines(String id, float w, float h, int columns, int rows) {
        List<Segment> result = new ArrayList<>();
        switch (id) {
            case "golden_ratio" -> grid(result, w, h, new float[]{0.618034f});
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
