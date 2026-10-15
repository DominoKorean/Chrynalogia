package dev.chrynalogia.probe;

/** Explicit camera allowlist shared by foreground detection, launchers and picker return. */
final class CameraApps {
    static final String SAMSUNG_CAMERA="com.sec.android.app.camera";
    static final String EXPERT_RAW="com.samsung.android.app.galaxyraw";
    static boolean supports(CharSequence packageName){
        return packageName!=null&&(SAMSUNG_CAMERA.contentEquals(packageName)||EXPERT_RAW.contentEquals(packageName));
    }
    static String returnTarget(String requested){return supports(requested)?requested:SAMSUNG_CAMERA;}
}
