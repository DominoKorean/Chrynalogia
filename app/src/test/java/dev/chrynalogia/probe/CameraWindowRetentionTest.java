package dev.chrynalogia.probe;
import org.junit.Test;
import static org.junit.Assert.*;
public class CameraWindowRetentionTest {
    @Test public void retainsOnlyTheVerifiedWindowWhileItsRootIsMissing(){
        assertTrue(CameraWindowRetention.hold(true,17,17,1200,1500));
        assertFalse(CameraWindowRetention.hold(false,17,17,1200,1500));
        assertFalse(CameraWindowRetention.hold(true,18,17,1200,1500));
        assertFalse(CameraWindowRetention.hold(true,-1,-1,1200,1500));
    }
    @Test public void missingRootExpiresInsteadOfKeepingStaleCameraControls(){
        assertFalse(CameraWindowRetention.hold(true,17,17,1500,1500));
        assertFalse(CameraWindowRetention.hold(true,17,17,1800,1500));
    }
}
