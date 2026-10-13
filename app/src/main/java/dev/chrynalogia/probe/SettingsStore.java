package dev.chrynalogia.probe;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

/** Shared lock for edits. Stale readers only write fields they actually changed. */
final class SettingsStore {
    static final Object LOCK=new Object();
    static void initialize(Context c){synchronized(LOCK){
        SharedPreferences p=c.getSharedPreferences("probe",0);
        if(Boolean.TRUE.equals(p.getAll().get("retiredGoldenRatio")))return;
        GridSettings state=new GridSettings(c);
        if(state.selected.remove("golden_ratio"))state.selected.add("golden_grid");
        p.edit().putStringSet("selected",new LinkedHashSet<>(state.selected)).putBoolean("retiredGoldenRatio",true).apply();
    }}
    static GridSettings update(Context c,Consumer<GridSettings> mutation){synchronized(LOCK){GridSettings s=new GridSettings(c);mutation.accept(s);saveChanges(s);return s;}}
    static void saveChanges(GridSettings state){saveChanges(state,false);}
    static boolean saveChanges(GridSettings state,boolean synchronous){synchronized(LOCK){
        Map<String,Object> current=state.values();SharedPreferences.Editor edit=state.prefs.edit();boolean changed=false;
        for(var item:current.entrySet())if(!Objects.equals(item.getValue(),state.original.get(item.getKey()))){put(edit,item.getKey(),item.getValue());changed=true;}
        if(changed){if(synchronous){if(!edit.commit())return false;}else edit.apply();}state.original=current;return true;
    }}
    static void put(SharedPreferences.Editor edit,String key,Object value){
        if(value instanceof Boolean b)edit.putBoolean(key,b);else if(value instanceof Integer i)edit.putInt(key,i);
        else if(value instanceof Long n)edit.putLong(key,n);else if(value instanceof Float f)edit.putFloat(key,f);else if(value instanceof String s)edit.putString(key,s);
        else if(value instanceof Set<?> values){Set<String> strings=new LinkedHashSet<>();for(Object item:values)strings.add((String)item);edit.putStringSet(key,strings);}
        else throw new IllegalArgumentException("Unsupported preference "+key);
    }
}
