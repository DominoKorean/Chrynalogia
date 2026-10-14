package dev.chrynalogia.probe;

import org.junit.Test;
import static org.junit.Assert.*;

public class GridGeometryTest {
    @Test public void spiralFitsCenteredGoldenRectangleWithReferenceSquareOrder() {
        var spiral=GridGeometry.spiral(2000,1000);
        assertEquals(190.983f,spiral.frame().left(),0.002f);
        assertEquals(1809.017f,spiral.frame().right(),0.002f);
        float[][] expected={{809.017f,0,1809.017f,1000},{190.983f,381.966f,809.017f,1000},
            {190.983f,0,572.949f,381.966f},{572.949f,0,809.017f,236.068f}};
        for(int i=0;i<4;i++) {
            var square=spiral.squares().get(i);
            assertArrayEquals(expected[i],new float[]{square.left(),square.top(),square.right(),square.bottom()},0.002f);
        }
        assertEquals(4+spiral.squares().size(),spiral.partitions().size());
    }
    @Test public void spiralQuarterCirclesJoinFromUpperRightTowardTheCenter() {
        var spiral=GridGeometry.spiral(2000,1000);
        assertFalse("quarter-circle spiral is present",spiral.arcs().isEmpty());
        assertEquals(1809.017f,spiral.arcs().get(0).startX(),0.002f);
        assertEquals(0,spiral.arcs().get(0).startY(),0.002f);
        for(int i=0;i<spiral.arcs().size();i++) {
            var arc=spiral.arcs().get(i);var square=spiral.squares().get(i);
            assertEquals(square.right()-square.left(),arc.radius(),0.002f);
            assertEquals(90,arc.sweepDegrees(),0);
            if(i>0) {
                var previous=spiral.arcs().get(i-1);
                assertEquals(previous.endX(),arc.startX(),0.003f);
                assertEquals(previous.endY(),arc.startY(),0.003f);
            }
        }
    }
    @Test public void spiralPartitionsAreSquaresInsideUniformlyFittedPortraitAndWideFrames() {
        for(float[] size:new float[][]{{400,800},{1920,1080},{600,600}}) {
            var spiral=GridGeometry.spiral(size[0],size[1]);var frame=spiral.frame();
            assertEquals(1.618034f,(frame.right()-frame.left())/(frame.bottom()-frame.top()),0.00001f);
            for(var square:spiral.squares()) {
                assertEquals(square.right()-square.left(),square.bottom()-square.top(),0.02f);
                assertTrue(square.left()>=frame.left()&&square.right()<=frame.right());
                assertTrue(square.top()>=frame.top()&&square.bottom()<=frame.bottom());
            }
        }
    }
    @Test public void dynamicReciprocalsReachFrameEdgesInsteadOfStoppingAtDiagonal() {
        for(float[] dimensions:new float[][]{{1080,1440},{1920,1080},{720,720}}) {
            float w=dimensions[0],h=dimensions[1];
            var lines=GridGeometry.lines("dynamic",w,h,3,3);
            assertEquals(8,lines.size());
            for(int i=2;i<6;i++) {
                var s=lines.get(i);
                assertTrue("reciprocal endpoint must reach frame",s.x2()==0||s.x2()==w||s.y2()==0||s.y2()==h);
                float diagonalY=i<4?h:-h;
                assertEquals(0,(s.x2()-s.x1())*w+(s.y2()-s.y1())*diagonalY,1f);
            }
        }
    }
    @Test public void retiredSingleGoldenRatioIsUnavailableToConsumers() {
        assertFalse(java.util.Arrays.asList(GridGeometry.IDS).contains("golden_ratio"));
        assertTrue(GridGeometry.lines("golden_ratio",720,1280,3,3).isEmpty());
    }
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
