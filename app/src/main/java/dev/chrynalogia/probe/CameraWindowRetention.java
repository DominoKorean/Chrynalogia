package dev.chrynalogia.probe;

/** Missing accessibility roots may briefly interrupt the same verified camera window. */
final class CameraWindowRetention {
    static long rotationDeadline(boolean hasFrame,int previousTurn,int currentTurn,long now,long deadline){
        return hasFrame&&previousTurn!=currentTurn?Math.max(deadline,now+750):deadline;
    }
    static boolean hold(boolean unavailable,int focusedId,int cameraId,long now,long until){
        return unavailable&&focusedId>=0&&focusedId==cameraId&&now<until;
    }
}
