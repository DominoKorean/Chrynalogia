package dev.chrynalogia.probe;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class GridMetadataRollbackTest {
    @Test public void failedDurableRollbackRestoresMemoryAndReportsFailure() throws Exception {
        Map<String,Object> memory=new HashMap<>(Map.of("imports","changed","selected",Set.of("changed"),"color",7));
        Map<String,Object> pending=new HashMap<>();
        SharedPreferences.Editor editor=(SharedPreferences.Editor)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{SharedPreferences.Editor.class},(p,m,a)->{
            switch(m.getName()){
                case "putString", "putStringSet" -> {pending.put((String)a[0],a[1]);return p;}
                case "remove" -> {pending.put((String)a[0],null);return p;}
                case "commit" -> {pending.forEach((k,v)->{if(v==null)memory.remove(k);else memory.put(k,v);});pending.clear();return false;}
                default -> throw new AssertionError(m.getName());
            }
        });
        SharedPreferences prefs=(SharedPreferences)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{SharedPreferences.class},(p,m,a)->{if(m.getName().equals("edit"))return editor;throw new AssertionError(m.getName());});
        Class<?> writer=Class.forName("dev.chrynalogia.probe.GridFiles$PreferenceWriter");
        Object failure=Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{writer},(p,m,a)->((SharedPreferences.Editor)a[0]).commit());
        var restore=GridFiles.class.getDeclaredMethod("restore",SharedPreferences.class,Map.class,writer);restore.setAccessible(true);
        try{restore.invoke(null,prefs,Map.of("selected",Set.of("thirds")),failure);fail("Disk failure lost");}
        catch(java.lang.reflect.InvocationTargetException error){assertTrue(error.getCause() instanceof java.io.IOException);}
        assertEquals(Set.of("thirds"),memory.get("selected"));assertFalse(memory.containsKey("imports"));assertEquals(7,memory.get("color"));
    }
}
