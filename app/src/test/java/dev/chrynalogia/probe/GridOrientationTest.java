package dev.chrynalogia.probe;

import org.junit.Test;
import static org.junit.Assert.*;

public class GridOrientationTest {
    @Test public void clockwiseDisplayTurnCounterRotatesCameraGuides() {
        var turn=GridOrientation.transform(400,800,0,1,false,false);
        assertEquals(800,turn.width(),0);assertEquals(400,turn.height(),0);
        float[] m=turn.values();
        // A camera-right point appears at the top after a clockwise display turn.
        assertEquals(200,m[0]*800+m[1]*200+m[2],0.001f);
        assertEquals(0,m[3]*800+m[4]*200+m[5],0.001f);
    }
    @Test public void allDisplayTurnsPreserveManualRotationAndPhysicalFlip() {
        for(boolean flipX:new boolean[]{false,true})for(boolean flipY:new boolean[]{false,true})
        for(int manual=0;manual<4;manual++)for(int display=0;display<4;display++) {
            float w=display%2==0?800:400,h=display%2==0?400:800;
            var turn=GridOrientation.transform(w,h,manual,display,flipX,flipY);
            float[] m=turn.values();
            float x=m[0]*(turn.width()/2+40)+m[1]*(turn.height()/2+20)+m[2]-w/2;
            float y=m[3]*(turn.width()/2+40)+m[4]*(turn.height()/2+20)+m[5]-h/2;
            float[] physical=switch(display){case 0->new float[]{x,y};case 1->new float[]{-y,x};case 2->new float[]{-x,-y};default->new float[]{y,-x};};
            float[][] expected={{40,20},{-20,40},{-40,-20},{20,-40}};
            assertEquals((flipX?-1:1)*expected[manual][0],physical[0],0.001f);
            assertEquals((flipY?-1:1)*expected[manual][1],physical[1],0.001f);
        }
    }
    @Test public void previewColorFollowsThemeWhileCameraUsesChosenColor() {
        assertEquals(0xff000000,GridOrientation.lineColor(true,false,0xffe65100));
        assertEquals(0xffffffff,GridOrientation.lineColor(true,true,0xff000000));
        assertEquals(0xffe65100,GridOrientation.lineColor(false,true,0xffe65100));
    }
}
