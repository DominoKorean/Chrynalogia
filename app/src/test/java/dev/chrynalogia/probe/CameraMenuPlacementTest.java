package dev.chrynalogia.probe;
import org.junit.Test;
import static org.junit.Assert.*;
public class CameraMenuPlacementTest {
    private CameraMenuPlacement.Box portrait=new CameraMenuPlacement.Box(0,63,1080,2205);
    private CameraMenuPlacement.Box preview=new CameraMenuPlacement.Box(0,180,1080,1620);
    @Test public void mirrorsNativeInsetAndKeepsTheOpenerAlignedWithItsMenu(){
        var p=CameraMenuPlacement.place(portrait,preview,39,81,126,95,21,6,false);
        assertEquals(39,p.opener().left());assertEquals(p.opener().left(),p.menu().left());assertEquals(p.opener().width(),p.menu().width());
    }
    @Test public void physicallyLandscapeLockedWindowUsesTopChrome(){
        var p=CameraMenuPlacement.place(portrait,preview,39,81,126,95,21,6,true);
        assertTrue(p.horizontal());assertTrue(p.menu().bottom()<=preview.top());assertTrue(p.menu().right()<=portrait.right());assertEquals(39,p.opener().left());
    }
    @Test public void logicalLandscapeUsesSideChrome(){
        var s=new CameraMenuPlacement.Box(0,0,2340,1017);var f=new CameraMenuPlacement.Box(200,0,1640,1080);
        var p=CameraMenuPlacement.place(s,f,39,0,126,95,21,6,true);
        assertFalse(p.horizontal());assertTrue(p.menu().right()<=f.left());assertTrue(p.menu().bottom()<=s.bottom());
    }
    @Test public void horizontalMenuDoesNotCoverNativeQuickButtons(){
        var p=CameraMenuPlacement.place(portrait,preview,39,81,126,95,21,6,true,640);
        assertTrue(p.menu().right()<=640);assertTrue(p.cellWidth()>=60);
    }
    @Test public void narrowWindowKeepsSixActionsInsideItsChrome(){
        var p=CameraMenuPlacement.place(new CameraMenuPlacement.Box(0,24,360,760),new CameraMenuPlacement.Box(0,64,360,544),16,28,48,36,8,6,true);
        assertTrue(p.menu().right()<=360);assertTrue(p.menu().bottom()<=64);assertTrue(p.cellWidth()>=36);
    }
}
