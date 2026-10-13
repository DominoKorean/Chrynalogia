package dev.chrynalogia.probe;
import android.view.Choreographer;
import java.util.HashMap;
import java.util.Map;

final class AndroidFrameScheduler implements FrameRefresh.Scheduler {
    private final Map<Runnable,Choreographer.FrameCallback> pending=new HashMap<>();
    public void post(Runnable work){Choreographer.FrameCallback callback=time->{pending.remove(work);work.run();};pending.put(work,callback);Choreographer.getInstance().postFrameCallback(callback);}
    public void remove(Runnable work){Choreographer.FrameCallback callback=pending.remove(work);if(callback!=null)Choreographer.getInstance().removeFrameCallback(callback);}
}
