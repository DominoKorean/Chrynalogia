package dev.chrynalogia.probe;
import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.*;

public class SettingsChangeTest {
    private Object state(int color,boolean hidden,float thickness) throws Exception {
        Class<?> type=Class.forName("dev.chrynalogia.probe.SettingsChange$State");var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        return ctor.newInstance(color,0.9f,thickness,false,hidden,Set.of("thirds"),0,3,3,false,false,"always");
    }
    private boolean flag(Object before,Object after,String name) throws Exception {
        Class<?> type=Class.forName("dev.chrynalogia.probe.SettingsChange");var classify=type.getDeclaredMethod("classify",before.getClass(),after.getClass());classify.setAccessible(true);
        var accessor=type.getDeclaredMethod(name);accessor.setAccessible(true);return (Boolean)accessor.invoke(classify.invoke(null,before,after));
    }
    @Test public void linePaintUpdatesDoNotRebuildCameraMenuOrReconcileSession() throws Exception {
        Object before=state(-1,false,1.2f),after=state(0xff000000,false,1.2f);
        assertTrue(flag(before,after,"render"));assertFalse(flag(before,after,"menu"));assertFalse(flag(before,after,"session"));assertFalse(flag(before,after,"pictures"));
    }
    @Test public void geometryInvalidatesPicturesAndHiddenStateUpdatesMenu() throws Exception {
        assertTrue(flag(state(-1,false,1.2f),state(-1,false,2.4f),"pictures"));
        assertTrue(flag(state(-1,false,1.2f),state(-1,true,1.2f),"menu"));
    }
}
