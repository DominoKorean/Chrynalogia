package dev.chrynalogia.probe;

/** Camera coordinates stay physical; persisted rotation and flips remain user actions. */
final class GridOrientation {
    record Transform(float width,float height,float[] values) {}
    static Transform transform(float w,float h,int manual,int display,boolean flipX,boolean flipY) {
        int rotation=Math.floorMod(manual-display,4);
        float dw=rotation%2==0?w:h,dh=rotation%2==0?h:w;
        int[] cos={1,0,-1,0},sin={0,1,0,-1};
        int m=Math.floorMod(manual,4),d=Math.floorMod(display,4);
        float cm=cos[m],sm=sin[m],cd=cos[d],sd=sin[d],fx=flipX?-1:1,fy=flipY?-1:1;
        // R(-display) * physical flips * R(manual): flips must not move to UI axes.
        float xx=cd*fx*cm+sd*fy*sm,xy=-cd*fx*sm+sd*fy*cm;
        float yx=-sd*fx*cm+cd*fy*sm,yy=sd*fx*sm+cd*fy*cm;
        return new Transform(dw,dh,new float[]{xx,xy,w/2-xx*dw/2-xy*dh/2,
            yx,yy,h/2-yx*dw/2-yy*dh/2,0,0,1});
    }
    static int lineColor(boolean preview,boolean dark,int chosen) {return preview?(dark?0xffffffff:0xff000000):chosen;}
}
