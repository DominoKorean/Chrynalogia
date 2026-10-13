package dev.chrynalogia.probe;
import android.content.Context;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;

/** File ownership and current metadata are committed together under the settings lock. */
final class GridFiles {
    interface FileOps {boolean move(File from,File to);boolean delete(File file);}
    private static final FileOps FILES=new FileOps(){public boolean move(File a,File b){return a.renameTo(b);}public boolean delete(File f){return f.delete();}};
    static String register(Context c,String name,String extension,File prepared) throws GridImportException {return register(c,name,extension,prepared,FILES);}
    static String register(Context c,String name,String extension,File prepared,FileOps ops) throws GridImportException {
        boolean owned=false;File destination=null;
        try {
            owned=prepared.getCanonicalFile().getParentFile().equals(c.getCacheDir().getCanonicalFile());
            if(!owned||!(extension.equals(".svg")||extension.equals(".png")))throw new GridImportException(R.string.file_type_error);
            synchronized(SettingsStore.LOCK){
                GridSettings state=new GridSettings(c);
                if(state.imports.length()>=12)throw new GridImportException(R.string.file_count_error);
                File folder=new File(c.getFilesDir(),"grids");if(!folder.exists()&&!folder.mkdirs())throw new GridImportException(R.string.storage_error);
                String id="file_"+UUID.randomUUID().toString().replace("-","");destination=new File(folder,id+extension);
                if(!ops.move(prepared,destination))throw new GridImportException(R.string.image_save_error);
                state.imports.put(new ImportedGrid(id,name==null?c.getString(R.string.custom_grid):name,extension).json());
                if(!state.multi)state.selected.clear();state.selected.add(id);
                if(!SettingsStore.saveChanges(state,true))throw new GridImportException(R.string.storage_error);
                ProbeLog.event(c,"grid_imported","id",id,"format",extension);return id;
            }
        }catch(GridImportException error){if(destination!=null)ops.delete(destination);throw error;}
        catch(Exception error){if(destination!=null)ops.delete(destination);throw new GridImportException(R.string.image_save_error,error);}
        finally{if(owned&&prepared.exists())ops.delete(prepared);}
    }
    static void remove(Context c,String id) throws GridImportException {remove(c,id,FILES);}
    static void remove(Context c,String id,FileOps ops) throws GridImportException {
        synchronized(SettingsStore.LOCK){
            GridSettings state=new GridSettings(c);JSONArray remaining=new JSONArray();ImportedGrid target=null;
            for(int i=0;i<state.imports.length();i++){ImportedGrid item=ImportedGrid.parse(state.imports.optJSONObject(i));if(item==null)continue;if(item.id().equals(id))target=item;else remaining.put(state.imports.optJSONObject(i));}
            if(target==null)return;
            File file=new File(c.getFilesDir(),"grids/"+target.id()+target.extension());File trash=new File(c.getCacheDir(),"removed_"+UUID.randomUUID());
            boolean moved=file.exists();
            if(moved&&!ops.move(file,trash))throw new GridImportException(R.string.grid_delete_error);
            JSONArray original=state.imports;Set<String> selected=new LinkedHashSet<>(state.selected);
            state.imports=remaining;state.selected.remove(id);
            if(!SettingsStore.saveChanges(state,true)){if(moved)ops.move(trash,file);throw new GridImportException(R.string.storage_error);}
            if(moved&&!ops.delete(trash)){
                boolean restored=ops.move(trash,file);state.imports=original;state.selected=selected;
                boolean metadata=SettingsStore.saveChanges(state,true);
                ProbeLog.event(c,"grid_delete_rollback","fileRestored",restored,"metadataRestored",metadata);
                throw new GridImportException(R.string.grid_delete_error);
            }
        }
    }
}
