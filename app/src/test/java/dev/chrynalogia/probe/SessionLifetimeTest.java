package dev.chrynalogia.probe;
import org.junit.Test;
import static org.junit.Assert.*;
public class SessionLifetimeTest {
    @Test public void oldServiceCannotEndNewRequestOrClearNewOwner() throws Exception {
        Class<?> type=Class.forName("dev.chrynalogia.probe.SessionLifetime");var ctor=type.getDeclaredConstructor();ctor.setAccessible(true);Object state=ctor.newInstance();
        var request=type.getDeclaredMethod("request");var ready=type.getDeclaredMethod("ready",Object.class,long.class);var end=type.getDeclaredMethod("ended",Object.class,long.class);var active=type.getDeclaredMethod("ready");
        for(var method:new java.lang.reflect.Method[]{request,ready,end,active})method.setAccessible(true);
        Object old=new Object(),current=new Object();long first=(Long)request.invoke(state);ready.invoke(state,old,first);
        long latest=(Long)request.invoke(state);assertFalse((Boolean)active.invoke(state));ready.invoke(state,current,latest);
        assertFalse((Boolean)end.invoke(state,old,first));assertTrue((Boolean)active.invoke(state));
        ready.invoke(state,old,latest);ready.invoke(state,current,latest);
        assertFalse((Boolean)end.invoke(state,old,latest));assertTrue((Boolean)active.invoke(state));
        assertTrue((Boolean)end.invoke(state,current,latest));assertFalse((Boolean)active.invoke(state));
    }
}
