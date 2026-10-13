package dev.chrynalogia.probe;
import java.lang.reflect.Proxy;
import org.junit.Test;
import static org.junit.Assert.*;
public class RestoreStepsTest {
    @Test public void laterRestorationRunsAndBothFailuresSurvive() throws Exception {
        Class<?> type=Class.forName("dev.chrynalogia.probe.RestoreSteps"),step=Class.forName("dev.chrynalogia.probe.RestoreSteps$Step");
        int[] calls={0};Object array=java.lang.reflect.Array.newInstance(step,3);
        for(int i=0;i<3;i++){final int index=i;java.lang.reflect.Array.set(array,i,Proxy.newProxyInstance(step.getClassLoader(),new Class<?>[]{step},(p,m,a)->{calls[0]++;if(index<2)throw new java.io.IOException("failure "+index);return null;}));}
        var run=type.getDeclaredMethod("run",array.getClass());run.setAccessible(true);
        try{run.invoke(null,array);fail("Failures lost");}catch(java.lang.reflect.InvocationTargetException error){assertEquals("failure 0",error.getCause().getMessage());assertEquals("failure 1",error.getCause().getSuppressed()[0].getMessage());}
        assertEquals(3,calls[0]);
    }
}
