package dev.chrynalogia.probe;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.provider.Settings;
import org.json.JSONArray;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/** One preference store for the launcher, camera controls and notification. */
final class GridSettings {
    final SharedPreferences prefs;
    private final Context context;
    Map<String,Object> original;
    boolean multi, hidden, flipX, flipY, keepAspect, outline, allowStop, onboarding;
    int rotation, columns, rows, color, customColor;
    boolean customColorSelected;
    float opacity, thickness;
    String runMode;
    Set<String> selected;
    JSONArray imports;

    GridSettings(Context context) {
        this.context=context.getApplicationContext();
        prefs = context.getSharedPreferences("probe", Context.MODE_PRIVATE);
        reload();
    }
    private static boolean bool(Map<String,?> v,String k,boolean d){return v.get(k) instanceof Boolean b?b:d;}
    private static int integer(Map<String,?> v,String k,int d){return v.get(k) instanceof Integer n?n:d;}
    private static String string(Map<String,?> v,String k,String d){return v.get(k) instanceof String t?t:d;}
    private static float number(Map<String,?> v,String k,float d){return v.get(k) instanceof Float n&&Float.isFinite(n)?n:d;}
    void reload() {
        Map<String,?> v=prefs.getAll();
        multi=bool(v,"multi",false);hidden=bool(v,"hidden",false);flipX=bool(v,"flipX",false);flipY=bool(v,"flipY",false);
        keepAspect=bool(v,"keepAspect",false);outline=bool(v,"outline",false);allowStop=bool(v,"allowStop",false);onboarding=bool(v,"onboarding",false);
        rotation=Math.floorMod(integer(v,"rotation",0),4);columns=Math.max(1,Math.min(16,integer(v,"columns",3)));rows=Math.max(1,Math.min(16,integer(v,"rows",3)));
        color=integer(v,"color",Color.WHITE);customColor=integer(v,"customColor",color);
        customColorSelected=bool(v,"customColorSelected",color!=Color.WHITE&&color!=Color.BLACK&&color!=0xffffcb0c);
        opacity=Math.max(0,Math.min(1,number(v,"opacity",0.9f)));thickness=Math.max(0.3f,Math.min(6,number(v,"thickness",1.2f)));
        runMode="manual".equals(string(v,"runMode","always"))?"manual":"always";
        imports=new JSONArray();Set<String> known=new LinkedHashSet<>();
        try {JSONArray raw=new JSONArray(string(v,"imports","[]"));for(int i=0;i<raw.length();i++){ImportedGrid file=ImportedGrid.parse(raw.optJSONObject(i));if(file!=null&&known.add(file.id()))imports.put(file.json());}}
        catch(Exception error){imports=new JSONArray();}
        selected=new LinkedHashSet<>();Object raw=v.get("selected");
        if(raw instanceof Set<?> ids){for(Object value:ids)if(value instanceof String id&&(known.contains(id)||java.util.Arrays.asList(GridGeometry.IDS).contains(id)))selected.add(id);}
        else selected.add("golden_grid");
        original=values();
    }
    Map<String,Object> values(){
        Map<String,Object> v=new HashMap<>();
        v.put("multi",multi);v.put("hidden",hidden);v.put("flipX",flipX);v.put("flipY",flipY);v.put("keepAspect",keepAspect);
        v.put("outline",outline);v.put("allowStop",allowStop);v.put("onboarding",onboarding);v.put("customColorSelected",customColorSelected);
        v.put("rotation",rotation);v.put("columns",columns);v.put("rows",rows);v.put("color",color);v.put("customColor",customColor);
        v.put("opacity",opacity);v.put("thickness",thickness);v.put("runMode",runMode);
        v.put("selected",new LinkedHashSet<>(selected));v.put("imports",imports.toString());return v;
    }
    void save(){SettingsStore.saveChanges(this);}
    void select(String id,boolean enabled){SettingsStore.update(context,state->{if(!state.multi&&enabled)state.selected.clear();if(enabled)state.selected.add(id);else state.selected.remove(id);});reload();}
    boolean active(Context context) {
        if (!onboarding || bool(prefs.getAll(),"paused",false)) return false;
        return !runMode.equals("manual") || (requested(context) && ManualSessionService.running);
    }
    boolean requested(Context context) {return onboarding && !bool(prefs.getAll(),"paused",false) && boot(context)>=0&&integer(prefs.getAll(),"sessionBoot",-1)==boot(context);}
    static int boot(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -2);
    }
    void activate(Context context) {
        prefs.edit().putBoolean("paused", false).putInt("sessionBoot", boot(context)).apply();
    }
    void stop() { prefs.edit().putBoolean("paused", true).putInt("sessionBoot", -1).apply(); }
}
