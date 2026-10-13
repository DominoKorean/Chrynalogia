package dev.chrynalogia.probe;

import org.junit.Test;
import static org.junit.Assert.*;

public class GridGeometryTest {
    @Test public void triangleAuxiliariesArePerpendicularOnTallAndWidePreviews() {
        for(float[] dimensions:new float[][]{{1080,1440},{1920,1080},{720,720}}) {
            float w=dimensions[0],h=dimensions[1];
            var lines=GridGeometry.lines("triangle",w,h,3,3);
            for(int i=1;i<lines.size();i++) {
                var s=lines.get(i);
                assertEquals(0,(s.x2()-s.x1())*w+(s.y2()-s.y1())*h,1f);
                assertEquals(s.x2()/w,s.y2()/h,0.00001f);
            }
        }
    }
    @Test public void allStraightGridsStayInsideThePreviewAcrossAspectRatios() {
        for(String id:GridGeometry.IDS) for(float[] d:new float[][]{{720,1600},{1440,1080},{600,600}}) {
            for(var s:GridGeometry.lines(id,d[0],d[1],16,1)) {
                assertTrue(s.x1()>=0&&s.x1()<=d[0]);assertTrue(s.x2()>=0&&s.x2()<=d[0]);
                assertTrue(s.y1()>=0&&s.y1()<=d[1]);assertTrue(s.y2()>=0&&s.y2()<=d[1]);
            }
        }
    }
    @Test public void diagonalMethodUsesFortyFiveDegreesInsteadOfCornerDiagonals() {
        for(var s:GridGeometry.lines("diagonal",1080,1920,3,3))assertEquals(Math.abs(s.x2()-s.x1()),Math.abs(s.y2()-s.y1()),0.001f);
    }
    @Test public void customGridSupportsOneUndividedAxis() {
        assertEquals(0,GridGeometry.lines("custom",720,1280,1,1).size());
        assertEquals(15,GridGeometry.lines("custom",720,1280,16,1).size());
    }
}
