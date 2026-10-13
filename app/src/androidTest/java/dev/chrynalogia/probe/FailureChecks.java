package dev.chrynalogia.probe;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.Map;

/** Real preferences change memory before an injected writer reports failure. */
final class FailureChecks {
    static int files(Context c) throws Exception {
        Class<?> writer=Class.forName("dev.chrynalogia.probe.GridFiles$PreferenceWriter");
        int[] attempts={0};
        Object failsOnce=Proxy.newProxyInstance(writer.getClassLoader(),new Class<?>[]{writer},(p,m,a)->{
            boolean saved=((SharedPreferences.Editor)a[0]).commit();return ++attempts[0]>1&&saved;
        });
        var register=GridFiles.class.getDeclaredMethod("register",Context.class,String.class,String.class,File.class,GridFiles.FileOps.class,writer);register.setAccessible(true);
        var remove=GridFiles.class.getDeclaredMethod("remove",Context.class,String.class,GridFiles.FileOps.class,writer);remove.setAccessible(true);
        GridFiles.FileOps ops=new GridFiles.FileOps(){public boolean move(File a,File b){return a.renameTo(b);}public boolean delete(File f){return f.delete();}};
        SettingsStore.update(c,s->{s.imports=new org.json.JSONArray();s.selected=java.util.Set.of("thirds");});
        Map<String,?> before=c.getSharedPreferences("probe",0).getAll();File temp=File.createTempFile("failure-",".svg",c.getCacheDir());Files.write(temp.toPath(),"<svg/>".getBytes());
        try{register.invoke(null,c,"failed.svg",".svg",temp,ops,failsOnce);throw new AssertionError("Commit failure ignored");}
        catch(java.lang.reflect.InvocationTargetException error){SettingsChecks.require(error.getCause() instanceof GridImportException,"Wrong import commit error");}
        SettingsChecks.require(before.equals(c.getSharedPreferences("probe",0).getAll()),"Import commit failure left mutated in-memory metadata");
        temp=File.createTempFile("failure-",".svg",c.getCacheDir());Files.write(temp.toPath(),"<svg/>".getBytes());String id=GridFiles.register(c,"saved.svg",".svg",temp);
        try{
            before=c.getSharedPreferences("probe",0).getAll();attempts[0]=0;
            try{remove.invoke(null,c,id,ops,failsOnce);throw new AssertionError("Removal commit failure ignored");}
            catch(java.lang.reflect.InvocationTargetException error){SettingsChecks.require(error.getCause() instanceof GridImportException,"Wrong removal commit error");}
            SettingsChecks.require(before.equals(c.getSharedPreferences("probe",0).getAll())&&new File(c.getFilesDir(),"grids/"+id+".svg").exists(),"Removal commit failure did not restore memory and file");
        }finally{GridFiles.remove(c,id);}
        return 4;
    }
    static int restore(Context c) throws Exception {
        Class<?> type=Class.forName("dev.chrynalogia.probe.DebugState");var snapshot=type.getDeclaredMethod("snapshot",Context.class);snapshot.setAccessible(true);
        org.json.JSONObject original=(org.json.JSONObject)snapshot.invoke(null,c);original.put("running",false);
        Class<?> remover=Class.forName("dev.chrynalogia.probe.DebugState$FileRemover");int[] calls={0};
        Object failFirst=Proxy.newProxyInstance(remover.getClassLoader(),new Class<?>[]{remover},(p,m,a)->{calls[0]++;if(calls[0]==1)return false;return ((File)a[0]).delete();});
        var restore=type.getDeclaredMethod("restore",Context.class,org.json.JSONObject.class,remover);restore.setAccessible(true);
        File folder=new File(c.getFilesDir(),"grids");folder.mkdirs();File one=new File(folder,"file_"+java.util.UUID.randomUUID().toString().replace("-","")+".svg"),two=new File(folder,"file_"+java.util.UUID.randomUUID().toString().replace("-","")+".svg");
        Map<String,?> prefs=c.getSharedPreferences("probe",0).getAll();
        try{
            Files.write(one.toPath(),"<svg/>".getBytes());Files.write(two.toPath(),"<svg/>".getBytes());c.getSharedPreferences("probe",0).edit().putString("restoreFixture","temporary").commit();
            try{restore.invoke(null,c,original,failFirst);throw new AssertionError("Restoration failure ignored");}
            catch(java.lang.reflect.InvocationTargetException error){SettingsChecks.require(error.getCause() instanceof Exception,"Wrong restoration failure");}
            SettingsChecks.require(calls[0]==2&&(!one.exists()||!two.exists()),"Restoration skipped remaining files after first failure");
            SettingsChecks.require(prefs.equals(c.getSharedPreferences("probe",0).getAll()),"File failure skipped preference restoration");
        }finally{one.delete();two.delete();}
        return 3;
    }
}
