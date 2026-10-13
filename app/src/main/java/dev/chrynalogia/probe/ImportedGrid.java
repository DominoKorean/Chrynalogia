package dev.chrynalogia.probe;
import org.json.JSONObject;

/** Only generated internal names can become file paths. */
record ImportedGrid(String id,String name,String extension) {
    static ImportedGrid parse(JSONObject data){
        if(data==null)return null;String id=data.optString("id"),ext=data.optString("extension");
        if(!id.matches("file_[0-9a-f]{32}")||!(ext.equals(".svg")||ext.equals(".png")))return null;
        return new ImportedGrid(id,data.optString("name",id),ext);
    }
    JSONObject json() throws org.json.JSONException {return new JSONObject().put("id",id).put("name",name).put("extension",extension);}
}
