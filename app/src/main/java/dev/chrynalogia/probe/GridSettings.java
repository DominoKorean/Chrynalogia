package dev.chrynalogia.probe;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.provider.Settings;
import org.json.JSONArray;
import java.util.LinkedHashSet;
import java.util.Set;

/** One preference store for the launcher, camera controls and notification. */
final class GridSettings {
    final SharedPreferences prefs;
    boolean multi, hidden, flipX, flipY, keepAspect, outline, allowStop, onboarding;
    int rotation, columns, rows, color, customColor;
    boolean customColorSelected;
    float opacity, thickness;
    String runMode;
    Set<String> selected;
    JSONArray imports;

    GridSettings(Context context) {
        prefs = context.getSharedPreferences("probe", Context.MODE_PRIVATE);
        reload();
    }
    void reload() {
        multi = prefs.getBoolean("multi", false);
        hidden = prefs.getBoolean("hidden", false);
        flipX = prefs.getBoolean("flipX", false); flipY = prefs.getBoolean("flipY", false);
        keepAspect = prefs.getBoolean("keepAspect", false);
        outline = prefs.getBoolean("outline", false);
        allowStop = prefs.getBoolean("allowStop", false);
        onboarding = prefs.getBoolean("onboarding", false);
        rotation = prefs.getInt("rotation", 0);
        columns = prefs.getInt("columns", 3); rows = prefs.getInt("rows", 3);
        color = prefs.getInt("color", Color.WHITE);
        customColor = prefs.getInt("customColor", color);
        customColorSelected = prefs.getBoolean("customColorSelected",color!=Color.WHITE&&color!=Color.BLACK&&color!=0xffffcb0c);
        opacity = prefs.getFloat("opacity", 0.9f); thickness = prefs.getFloat("thickness", 1.2f);
        runMode = prefs.getString("runMode", "always");
        selected = new LinkedHashSet<>(prefs.getStringSet("selected", Set.of("golden_grid")));
        if(!prefs.getBoolean("retiredGoldenRatio",false)) {
            if(selected.remove("golden_ratio"))selected.add("golden_grid");
            prefs.edit().putStringSet("selected",new LinkedHashSet<>(selected)).putBoolean("retiredGoldenRatio",true).apply();
        }
        try { imports = new JSONArray(prefs.getString("imports", "[]")); }
        catch (Exception error) { imports = new JSONArray(); }
    }
    void save() {
        prefs.edit().putBoolean("multi", multi).putBoolean("hidden", hidden)
            .putBoolean("flipX", flipX).putBoolean("flipY", flipY).putBoolean("keepAspect", keepAspect)
            .putBoolean("outline", outline).putBoolean("allowStop", allowStop).putBoolean("onboarding", onboarding)
            .putInt("rotation", rotation).putInt("columns", columns).putInt("rows", rows).putInt("color", color)
            .putInt("customColor",customColor).putBoolean("customColorSelected",customColorSelected)
            .putFloat("opacity", opacity).putFloat("thickness", thickness).putString("runMode", runMode)
            .putStringSet("selected", new LinkedHashSet<>(selected)).putString("imports", imports.toString()).apply();
    }
    void select(String id, boolean enabled) {
        if (!multi && enabled) selected.clear();
        if (enabled) selected.add(id); else selected.remove(id);
        save();
    }
    boolean active(Context context) {
        if (!onboarding || prefs.getBoolean("paused", false)) return false;
        return !runMode.equals("manual") || (requested(context) && ManualSessionService.running);
    }
    boolean requested(Context context) {return onboarding && !prefs.getBoolean("paused",false) && prefs.getInt("sessionBoot",-1)==boot(context);}
    static int boot(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -2);
    }
    void activate(Context context) {
        prefs.edit().putBoolean("paused", false).putInt("sessionBoot", boot(context)).apply();
    }
    void stop() { prefs.edit().putBoolean("paused", true).putInt("sessionBoot", -1).apply(); }
}
