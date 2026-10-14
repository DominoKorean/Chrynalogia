package dev.chrynalogia.probe;

import org.junit.Test;
import static org.junit.Assert.*;

public class DeviceCompatibilityTest {
    @Test public void oneUiVersionsIdentifySamsungFirmware() {
        assertTrue(DeviceCompatibility.hasOneUi(new String[]{"android.hardware.camera", "com.samsung.android.oneui.version.80500"}));
        assertTrue(DeviceCompatibility.hasOneUi(new String[]{"com.samsung.android.oneui.version.80000"}));
    }
    @Test public void samsungHardwareOrCameraAloneDoesNotIdentifyOneUi() {
        assertFalse(DeviceCompatibility.hasOneUi(new String[]{"com.samsung.android.camerasdkservice", "com.samsung.feature.device_category_phone"}));
        assertFalse(DeviceCompatibility.hasOneUi(new String[]{"android.hardware.camera"}));
    }
    @Test public void missingOrUnrelatedFeaturesAreUnsupported() {
        assertFalse(DeviceCompatibility.hasOneUi((String[])null));
        assertFalse(DeviceCompatibility.hasOneUi(new String[]{null, "com.samsung.android.oneui.version.", "com.samsung.android.oneui.version.unknown"}));
    }
}
