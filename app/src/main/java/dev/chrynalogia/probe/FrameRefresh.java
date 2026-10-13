package dev.chrynalogia.probe;
import java.util.function.IntConsumer;

/** One delivery per frame; cancellation also invalidates a callback already dispatched. */
final class FrameRefresh {
    interface Scheduler {void post(Runnable callback);void remove(Runnable callback);}
    private final Scheduler scheduler;private final IntConsumer consumer;
    private Runnable queued;private int mask;private long generation;
    FrameRefresh(Scheduler scheduler,IntConsumer consumer){this.scheduler=scheduler;this.consumer=consumer;}
    void request(int value){mask|=value;if(queued!=null)return;long version=++generation;
        queued=()->{if(version!=generation)return;queued=null;int latest=mask;mask=0;consumer.accept(latest);};scheduler.post(queued);
    }
    void cancel(){generation++;if(queued!=null)scheduler.remove(queued);queued=null;mask=0;}
}
