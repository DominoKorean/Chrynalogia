package dev.chrynalogia.probe;
import android.content.Context;
import android.graphics.Color;
import java.io.File;
import java.nio.file.Files;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

final class ImportChecks {
    static int run(Context c) throws Exception {
        // Reflection keeps the pre-implementation APK compilable: missing registration is RED.
        Class<?> files=Class.forName("dev.chrynalogia.probe.GridFiles");
        var register=files.getDeclaredMethod("register",Context.class,String.class,String.class,File.class);
        var remove=files.getDeclaredMethod("remove",Context.class,String.class);register.setAccessible(true);remove.setAccessible(true);
        File pending=File.createTempFile("import-check-",".svg",c.getCacheDir());String id=null;
        try {
            Files.write(pending.toPath(),"<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'><path d='M0 0 L10 10'/></svg>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            GridSettings state=new GridSettings(c);state.imports=new JSONArray();state.multi=true;state.selected=Set.of("thirds");state.color=Color.RED;state.rotation=2;state.save();
            id=(String)register.invoke(null,c,"test.svg",".svg",pending);
            state=new GridSettings(c);
            SettingsChecks.require(state.color==Color.RED&&state.rotation==2,"Register changed style");
            SettingsChecks.require(state.selected.contains("thirds")&&state.selected.contains(id),"Register ignored latest multi selection");
            SettingsChecks.require(new File(c.getFilesDir(),"grids/"+id+".svg").isFile(),"Registered file missing");
            try{GridFiles.remove(c,id,new GridFiles.FileOps(){public boolean move(File a,File b){return a.renameTo(b);}public boolean delete(File file){return false;}});throw new AssertionError("Deletion failure was ignored");}
            catch(GridImportException expected){SettingsChecks.require(expected.messageRes==R.string.grid_delete_error,"Wrong deletion error");}
            SettingsChecks.require(new GridSettings(c).selected.contains(id)&&new File(c.getFilesDir(),"grids/"+id+".svg").isFile(),"Failed delete did not roll back metadata and file");
            remove.invoke(null,c,id);SettingsChecks.require(new GridSettings(c).imports.length()==0,"Remove left metadata");
            SettingsChecks.require(!new File(c.getFilesDir(),"grids/"+id+".svg").exists(),"Remove left file");
            JSONArray full=new JSONArray();for(int i=0;i<12;i++)full.put(new JSONObject().put("id",String.format(java.util.Locale.ROOT,"file_%032x",i)).put("extension",".svg").put("name","fixture"));
            state=new GridSettings(c);state.imports=full;state.save();
            pending=File.createTempFile("import-check-",".svg",c.getCacheDir());Files.write(pending.toPath(),"<svg/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            try{register.invoke(null,c,"extra.svg",".svg",pending);throw new AssertionError("Accepted thirteenth file");}
            catch(java.lang.reflect.InvocationTargetException expected){SettingsChecks.require(expected.getCause() instanceof java.io.IOException,"Wrong count error");}
            SettingsChecks.require(!pending.exists(),"Failed registration left temporary file");
            JSONArray eleven=new JSONArray();for(int i=0;i<11;i++)eleven.put(full.get(i));state=new GridSettings(c);state.imports=eleven;state.save();
            java.util.concurrent.atomic.AtomicInteger success=new java.util.concurrent.atomic.AtomicInteger(),failure=new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicReference<String> winner=new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<Throwable> unexpected=new java.util.concurrent.atomic.AtomicReference<>();
            Runnable add=()->{try{File temp=File.createTempFile("import-check-",".svg",c.getCacheDir());Files.write(temp.toPath(),"<svg/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));winner.set(GridFiles.register(c,"race.svg",".svg",temp));success.incrementAndGet();}
                catch(GridImportException error){if(error.messageRes==R.string.file_count_error)failure.incrementAndGet();else unexpected.set(error);}catch(Exception error){unexpected.set(error);}};
            Thread one=new Thread(add),two=new Thread(add);one.start();two.start();one.join();two.join();
            try{SettingsChecks.require(unexpected.get()==null&&success.get()==1&&failure.get()==1,"Concurrent registrations did not enforce one remaining slot");SettingsChecks.require(new GridSettings(c).imports.length()==12,"Concurrent registration exceeded limit");}
            finally{if(winner.get()!=null)GridFiles.remove(c,winner.get());}
            return 11;
        }finally{pending.delete();if(id!=null)new File(c.getFilesDir(),"grids/"+id+".svg").delete();}
    }
}
