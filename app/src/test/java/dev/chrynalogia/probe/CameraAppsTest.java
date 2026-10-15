package dev.chrynalogia.probe;
import org.junit.Test;
import static org.junit.Assert.*;

public class CameraAppsTest {
    @Test public void acceptsOnlyTheTwoSamsungCameraPackages(){
        assertTrue(CameraApps.supports("com.sec.android.app.camera"));
        assertTrue(CameraApps.supports("com.samsung.android.app.galaxyraw"));
        assertTrue(CameraApps.supports(new StringBuilder("com.samsung.android.app.galaxyraw")));
        assertFalse(CameraApps.supports(null));assertFalse(CameraApps.supports(""));
        assertFalse(CameraApps.supports("com.samsung.android.app.galaxyraw.settings"));
        assertFalse(CameraApps.supports("com.example.camera"));
    }
    @Test public void preservesTheCameraThatOpenedTheFilePicker(){
        assertEquals(CameraApps.EXPERT_RAW,CameraApps.returnTarget(CameraApps.EXPERT_RAW));
        assertEquals(CameraApps.SAMSUNG_CAMERA,CameraApps.returnTarget(CameraApps.SAMSUNG_CAMERA));
    }
    @Test public void oldOrUntrustedPickerTargetsFallBackToSamsungCamera(){
        assertEquals(CameraApps.SAMSUNG_CAMERA,CameraApps.returnTarget(null));
        assertEquals(CameraApps.SAMSUNG_CAMERA,CameraApps.returnTarget("com.example.app"));
    }
}
