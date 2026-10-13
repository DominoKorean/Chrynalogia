package dev.chrynalogia.probe;
import org.junit.Test;
import java.util.ArrayList;
import java.util.function.IntConsumer;
import static org.junit.Assert.*;

public class FrameRefreshTest {
    @Test public void rapidRequestsDeliverOneCombinedFrameAndCancelDropsLateWork() throws Exception {
        Class<?> type=Class.forName("dev.chrynalogia.probe.FrameRefresh"), scheduler=Class.forName("dev.chrynalogia.probe.FrameRefresh$Scheduler");
        ArrayList<Runnable> queue=new ArrayList<>();int[] delivered={0,0};
        Object scheduling=java.lang.reflect.Proxy.newProxyInstance(scheduler.getClassLoader(),new Class<?>[]{scheduler},(object,method,args)->{if(method.getName().equals("post"))queue.add((Runnable)args[0]);else if(method.getName().equals("remove"))queue.remove(args[0]);return null;});
        var constructor=type.getDeclaredConstructor(scheduler,IntConsumer.class);constructor.setAccessible(true);
        Object refresh=constructor.newInstance(scheduling,(IntConsumer)mask->{delivered[0]++;delivered[1]=mask;});
        var request=type.getDeclaredMethod("request",int.class);var cancel=type.getDeclaredMethod("cancel");request.setAccessible(true);cancel.setAccessible(true);
        for(int i=0;i<100;i++)request.invoke(refresh,i%2==0?1:2);
        assertEquals(1,queue.size());assertEquals(0,delivered[0]);queue.remove(0).run();
        assertEquals(1,delivered[0]);assertEquals(3,delivered[1]);
        request.invoke(refresh,4);Runnable stale=queue.get(0);cancel.invoke(refresh);stale.run();
        assertEquals(1,delivered[0]);assertTrue(queue.isEmpty());
    }
}
