package dev.chrynalogia.probe;
/** Only bridge holding the live accessibility service; late callbacks cannot revive it. */
final class CameraConnection {
    private static volatile CameraWatchService current;
    private static final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    static void attach(CameraWatchService service){current=service;MainActivity.refreshSessionState();}
    static void detach(CameraWatchService service){if(current==service){current=null;MainActivity.refreshSessionState();}}
    static boolean connected(){return current!=null;}
    static CameraWatchService service(){return current;}
    static void requestRefresh(){CameraWatchService target=current;if(target!=null)handler.post(()->{if(current==target)target.refreshSettings();});}
}
