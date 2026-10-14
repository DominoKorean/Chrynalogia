package dev.chrynalogia.probe;
import org.junit.Test;
import static org.junit.Assert.*;
public class CameraCoordinatesTest {
    @Test public void flipActionsFollowBothPhysicalLandscapeDirections() {
        assertTrue(CameraCoordinates.horizontalFlipChangesX(0));
        assertFalse(CameraCoordinates.horizontalFlipChangesX(1));
        assertTrue(CameraCoordinates.horizontalFlipChangesX(2));
        assertFalse(CameraCoordinates.horizontalFlipChangesX(3));
    }
    private final CameraMenuPlacement.Box portrait=new CameraMenuPlacement.Box(0,0,1080,2340);
    private final CameraMenuPlacement.Box landscape=new CameraMenuPlacement.Box(0,0,2340,1080);
    private final CameraMenuPlacement.Box opener=new CameraMenuPlacement.Box(39,81,165,176);
    private final CameraMenuPlacement.Box menu=new CameraMenuPlacement.Box(39,197,165,827);
    @Test public void projectsTheSamePhysicalOpenerIntoBothLandscapeDirections(){
        assertEquals(new CameraMenuPlacement.Box(81,915,176,1041),CameraCoordinates.rotate(opener,portrait,1));
        assertEquals(new CameraMenuPlacement.Box(2164,39,2259,165),CameraCoordinates.rotate(opener,portrait,3));
    }
    @Test public void projectsMenuTogetherWithItsOpenerInsteadOfChoosingTheShutterStrip(){
        assertEquals(new CameraMenuPlacement.Box(197,915,827,1041),CameraCoordinates.rotate(menu,portrait,1));
        assertEquals(new CameraMenuPlacement.Box(1513,39,2143,165),CameraCoordinates.rotate(menu,portrait,3));
    }
    @Test public void returnsToTheOriginalPortraitAnchorWithoutAccumulatingRotation(){
        assertEquals(opener,CameraCoordinates.rotate(new CameraMenuPlacement.Box(81,915,176,1041),landscape,3));
        assertEquals(opener,CameraCoordinates.rotate(new CameraMenuPlacement.Box(2164,39,2259,165),landscape,1));
    }
    @Test public void normalizesNativeButtonChromeToItsOriginalTopRightLocation(){
        assertEquals(new CameraMenuPlacement.Box(679,81,1041,176),CameraCoordinates.rotate(new CameraMenuPlacement.Box(81,39,176,401),landscape,3));
    }
    @Test public void compensatesOppositeSensorAndDisplayConventions(){
        assertEquals(0,CameraCoordinates.iconAngle(1,3));
        assertEquals(0,CameraCoordinates.iconAngle(3,1));
        assertEquals(0,CameraCoordinates.iconAngle(2,2));
        assertEquals(-90,CameraCoordinates.iconAngle(0,1));
        assertEquals(-270,CameraCoordinates.iconAngle(0,3));
    }
}
