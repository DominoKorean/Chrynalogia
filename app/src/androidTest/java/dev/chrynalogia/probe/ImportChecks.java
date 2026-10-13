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
            Files.writeString(pending.toPath(),"<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'><path d='M0 0 L10 10'/></svg>");
            GridSettings state=new GridSettings(c);state.imports=new JSONArray();state.multi=true;state.selected=Set.of("thirds");state.color=Color.RED;state.rotation=2;state.save();
            id=(String)register.invoke(null,c,"test.svg",".svg",pending);
            state=new GridSettings(c);
            SettingsChecks.require(state.color==Color.RED&&state.rotation==2,"Register changed style");
            SettingsChecks.require(state.selected.contains("thirds")&&state.selected.contains(id),"Register ignored latest multi selection");
            SettingsChecks.require(new File(c.getFilesDir(),"grids/"+id+".svg").isFile(),"Registered file missing");
            remove.invoke(null,c,id);SettingsChecks.require(new GridSettings(c).imports.length()==0,"Remove left metadata");
            SettingsChecks.require(!new File(c.getFilesDir(),"grids/"+id+".svg").exists(),"Remove left file");
            JSONArray full=new JSONArray();for(int i=0;i<12;i++)full.put(new JSONObject().put("id",String.format(java.util.Locale.ROOT,"file_%032x",i)).put("extension",".svg").put("name","fixture"));
            state=new GridSettings(c);state.imports=full;state.save();
            pending=File.createTempFile("import-check-",".svg",c.getCacheDir());Files.writeString(pending.toPath(),"<svg/>");
            try{register.invoke(null,c,"extra.svg",".svg",pending);throw new AssertionError("Accepted thirteenth file");}
            catch(java.lang.reflect.InvocationTargetException expected){SettingsChecks.require(expected.getCause() instanceof java.io.IOException,"Wrong count error");}
            SettingsChecks.require(!pending.exists(),"Failed registration left temporary file");
            return 7;
        }finally{pending.delete();if(id!=null)new File(c.getFilesDir(),"grids/"+id+".svg").delete();}
    }
}
