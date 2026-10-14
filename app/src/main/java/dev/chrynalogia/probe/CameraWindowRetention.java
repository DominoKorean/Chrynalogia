package dev.chrynalogia.probe;

/** Missing accessibility roots may briefly interrupt the same verified camera window. */
final class CameraWindowRetention {
    static boolean hold(boolean unavailable,int focusedId,int cameraId,long now,long until){
        return unavailable&&focusedId>=0&&focusedId==cameraId&&now<until;
    }
}
