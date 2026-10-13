package dev.chrynalogia.probe;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.json.JSONArray;

/** File ownership and current metadata are committed together under the settings lock. */
final class GridFiles {
    interface FileOps {boolean move(File from,File to);boolean delete(File file);}
    interface PreferenceWriter {boolean commit(SharedPreferences.Editor editor);}
    private static final PreferenceWriter PREFERENCES=SharedPreferences.Editor::commit;
    private static final FileOps FILES=new FileOps(){public boolean move(File a,File b){return a.renameTo(b);}public boolean delete(File f){return f.delete();}};
    static String register(Context c,String name,String extension,File prepared) throws GridImportException {return register(c,name,extension,prepared,FILES);}
    static String register(Context c,String name,String extension,File prepared,FileOps ops) throws GridImportException {return register(c,name,extension,prepared,ops,PREFERENCES);}
    static String register(Context c,String name,String extension,File prepared,FileOps ops,PreferenceWriter writer) throws GridImportException {
        boolean owned=false;
        try {
            owned=prepared.getCanonicalFile().getParentFile().equals(c.getCacheDir().getCanonicalFile());
            if(!owned||!(extension.equals(".svg")||extension.equals(".png")))throw new GridImportException(R.string.file_type_error);
            synchronized(SettingsStore.LOCK){
                GridSettings state=new GridSettings(c);Map<String,?> original=state.prefs.getAll();
                if(state.imports.length()>=12)throw new GridImportException(R.string.file_count_error);
                File folder=new File(c.getFilesDir(),"grids");if(!folder.exists()&&!folder.mkdirs())throw new GridImportException(R.string.storage_error);
                String id="file_"+UUID.randomUUID().toString().replace("-","");File destination=new File(folder,id+extension);
                if(!ops.move(prepared,destination))throw new GridImportException(R.string.image_save_error);
                try {
                    state.imports.put(new ImportedGrid(id,name==null?c.getString(R.string.custom_grid):name,extension).json());
                    if(!state.multi)state.selected.clear();state.selected.add(id);
                    if(!write(state,writer))throw new GridImportException(R.string.storage_error);
                }catch(Exception error){
                    GridImportException failure=error instanceof GridImportException e?e:new GridImportException(R.string.storage_error,error);
                    rollback(c,failure,()->restore(state.prefs,original,writer),()->{if(destination.exists()&&!ops.delete(destination))throw new IOException("Cannot remove failed import");});
                    throw failure;
                }
                ProbeLog.event(c,"grid_imported","id",id,"format",extension);return id;
            }
        }catch(GridImportException error){throw error;}
        catch(Exception error){throw new GridImportException(R.string.image_save_error,error);}
        finally{if(owned&&prepared.exists()&&!ops.delete(prepared))ProbeLog.event(c,"grid_import_cleanup_failed","file",prepared.getName());}
    }
    static void remove(Context c,String id) throws GridImportException {remove(c,id,FILES);}
    static void remove(Context c,String id,FileOps ops) throws GridImportException {remove(c,id,ops,PREFERENCES);}
    static void remove(Context c,String id,FileOps ops,PreferenceWriter writer) throws GridImportException {
        synchronized(SettingsStore.LOCK){
            GridSettings state=new GridSettings(c);Map<String,?> original=state.prefs.getAll();JSONArray remaining=new JSONArray();ImportedGrid target=null;
            for(int i=0;i<state.imports.length();i++){ImportedGrid item=ImportedGrid.parse(state.imports.optJSONObject(i));if(item==null)continue;if(item.id().equals(id))target=item;else remaining.put(state.imports.optJSONObject(i));}
            if(target==null)return;
            File file=new File(c.getFilesDir(),"grids/"+target.id()+target.extension());File trash=new File(c.getCacheDir(),"removed_"+UUID.randomUUID());
            boolean moved=file.exists();
            if(moved&&!ops.move(file,trash))throw new GridImportException(R.string.grid_delete_error);
            state.imports=remaining;state.selected.remove(id);
            try {
                if(!write(state,writer))throw new GridImportException(R.string.storage_error);
                if(moved&&!ops.delete(trash))throw new GridImportException(R.string.grid_delete_error);
            }catch(Exception error){
                GridImportException failure=error instanceof GridImportException e?e:new GridImportException(R.string.storage_error,error);
                rollback(c,failure,()->{if(moved&&!ops.move(trash,file))throw new IOException("Cannot restore removed grid");},()->restore(state.prefs,original,writer));
                throw failure;
            }
        }
    }
    private static boolean write(GridSettings state,PreferenceWriter writer){return writer.commit(state.prefs.edit().putString("imports",state.imports.toString()).putStringSet("selected",new LinkedHashSet<>(state.selected)));}
    private static void restore(SharedPreferences prefs,Map<String,?> original,PreferenceWriter writer) throws IOException {
        SharedPreferences.Editor edit=prefs.edit();
        for(String key:new String[]{"imports","selected"}){Object value=original.get(key);if(value==null)edit.remove(key);else SettingsStore.put(edit,key,value);}
        // commit(false) still restores memory; retain the durability failure for diagnostics.
        if(!writer.commit(edit))throw new IOException("Grid metadata rollback was not persisted");
    }
    private static void rollback(Context c,GridImportException failure,RestoreSteps.Step... steps){
        try{RestoreSteps.run(steps);}catch(Exception error){failure.addSuppressed(error);ProbeLog.event(c,"grid_rollback_failed","error",error.toString());}
    }
}
