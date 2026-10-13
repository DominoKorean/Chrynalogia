package dev.chrynalogia.probe;

import android.Manifest;
import android.app.LocaleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.LocaleList;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** DUMP-protected test backup. Fixed app-internal path; never accepts an external path. */
final class DebugState {
    private static File backup(Context c) { return new File(c.getFilesDir(), "test-state.json"); }
    interface FileRemover {boolean delete(File file);}
    static JSONObject snapshot(Context c) throws Exception {
        JSONObject values=new JSONObject();
        for (Map.Entry<String,?> entry:c.getSharedPreferences("probe",0).getAll().entrySet()) {
            Object value=entry.getValue();String type=value instanceof Set?"set":value.getClass().getSimpleName();
            values.put(entry.getKey(),new JSONObject().put("type",type).put("value",value instanceof Set?new JSONArray((Set<?>)value):value instanceof Float?value.toString():value));
        }
        JSONArray files=new JSONArray();File[] inventory=new File(c.getFilesDir(),"grids").listFiles();
        if(inventory!=null)for(File file:inventory)files.put(file.getName());
        JSONObject state=new JSONObject().put("boot",GridSettings.boot(c)).put("prefs",values).put("files",files)
            .put("language",AppAppearance.language(c)).put("running",SessionController.ready());
        return state;
    }
    static void save(Context c) throws Exception {
        JSONObject state=snapshot(c);
        android.util.AtomicFile output=new android.util.AtomicFile(backup(c));
        java.io.FileOutputStream stream=output.startWrite();
        try {stream.write(state.toString().getBytes(StandardCharsets.UTF_8));output.finishWrite(stream);}
        catch(Exception error){output.failWrite(stream);throw error;}
    }
    static JSONObject read(Context c) throws Exception {
        JSONObject state=new JSONObject(new String(Files.readAllBytes(backup(c).toPath()),StandardCharsets.UTF_8));
        if(state.getInt("boot")!=GridSettings.boot(c))throw new java.io.IOException("Test backup belongs to a different boot");
        return state;
    }
    static void restore(Context c,JSONObject state) throws Exception {restore(c,state,File::delete);}
    static void restore(Context c,JSONObject state,FileRemover remover) throws Exception {
        Set<String> initial=new HashSet<>();JSONArray files=state.getJSONArray("files");
        for(int i=0;i<files.length();i++)initial.add(files.getString(i));
        java.util.ArrayList<RestoreSteps.Step> steps=new java.util.ArrayList<>();
        steps.add(()->SessionController.clearForeground(c));
        File[] inventory=new File(c.getFilesDir(),"grids").listFiles();
        if(inventory!=null)for(File file:inventory)if(!initial.contains(file.getName())&&file.getName().matches("file_[0-9a-f]{32}\\.(svg|png)"))
            steps.add(()->{if(!remover.delete(file))throw new java.io.IOException("Cannot remove test file: "+file.getName());});
        steps.add(()->restorePreferences(c,state.getJSONObject("prefs")));
        steps.add(()->c.getSystemService(LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags(state.getString("language"))));
        steps.add(()->{if(state.getBoolean("running")) {
            if(c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)throw new java.io.IOException("Notification permission missing for original session");
            c.startForegroundService(new Intent(c,ManualSessionService.class).putExtra(SessionController.GENERATION,SessionController.generation()));
        }});
        steps.add(CameraConnection::requestRefresh);
        RestoreSteps.run(steps.toArray(RestoreSteps.Step[]::new));
    }
    private static void restorePreferences(Context c,JSONObject values) throws Exception {
        SharedPreferences.Editor edit=c.getSharedPreferences("probe",0).edit().clear();java.util.Iterator<String> keys=values.keys();
        while(keys.hasNext()) {String key=keys.next();JSONObject item=values.getJSONObject(key);switch(item.getString("type")) {
            case "Boolean"->edit.putBoolean(key,item.getBoolean("value"));case "Integer"->edit.putInt(key,item.getInt("value"));
            case "Long"->edit.putLong(key,item.getLong("value"));case "Float"->edit.putFloat(key,(float)item.getDouble("value"));
            case "String"->edit.putString(key,item.getString("value"));case "set"->{JSONArray a=item.getJSONArray("value");Set<String> s=new HashSet<>();for(int i=0;i<a.length();i++)s.add(a.getString(i));edit.putStringSet(key,s);}
            default->throw new java.io.IOException("Unknown stored preference type");
        }}
        if(!edit.commit())throw new java.io.IOException("Could not restore preferences");
    }
}
