package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.provider.OpenableColumns;
import com.caverock.androidsvg.SVG;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class GridImporter {
    static final int MAX_BYTES=4*1024*1024;
    static String importUri(Context context,Uri uri) throws Exception {
        String name=context.getString(R.string.custom_grid);
        try(android.database.Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)) {
            if(cursor!=null && cursor.moveToFirst()) name=cursor.getString(0);
        }
        byte[] bytes;
        try(InputStream in=context.getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            if(in==null) throw new GridImportException(R.string.file_open_error);
            byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1) {if(out.size()+n>MAX_BYTES)throw new GridImportException(R.string.file_size_error);out.write(buffer,0,n);}
            bytes=out.toByteArray();
        }
        return importBytes(context,name,bytes);
    }
    static String importBytes(Context context,String name,byte[] bytes) throws Exception {
        if(bytes.length>MAX_BYTES)throw new GridImportException(R.string.file_size_error);
        GridSettings settings=new GridSettings(context);
        if(settings.imports.length()>=12)throw new GridImportException(R.string.file_count_error);
        File folder=new File(context.getFilesDir(),"grids");if(!folder.exists()&&!folder.mkdirs())throw new GridImportException(R.string.storage_error);
        String extension;Bitmap bitmap=null;
        String prefix=new String(bytes,0,Math.min(bytes.length,2048),StandardCharsets.UTF_8);
        if(name.toLowerCase(java.util.Locale.ROOT).endsWith(".svg") || prefix.contains("<svg")) {
            String xml=new String(bytes,StandardCharsets.UTF_8);
            if(xml.startsWith("\uFEFF"))xml=xml.substring(1);
            if(xml.contains("<!ENTITY") || xml.contains("<!DOCTYPE"))throw new GridImportException(R.string.svg_entities_error);
            SVG.setInternalEntitiesEnabled(false);
            SVG svg;try{svg=SVG.getFromString(xml);}catch(com.caverock.androidsvg.SVGParseException error){throw new GridImportException(R.string.svg_parse_error,error);}
            RectF box=svg.getDocumentViewBox();
            if(box==null) {
                float w=svg.getDocumentWidth(),h=svg.getDocumentHeight();
                if(w<=0||h<=0||!Float.isFinite(w)||!Float.isFinite(h))throw new GridImportException(R.string.svg_dimensions_error);
                svg.setDocumentViewBox(0,0,w,h);
                try{xml=SvgLines.normalize(xml,w,h);}catch(Exception error){throw new GridImportException(R.string.svg_parse_error,error);}
            } else if(box.width()<=0||box.height()<=0||!Float.isFinite(box.width())||!Float.isFinite(box.height()))throw new GridImportException(R.string.svg_viewbox_error);
            else try{xml=SvgLines.normalize(xml,0,0);}catch(Exception error){throw new GridImportException(R.string.svg_parse_error,error);}
            bytes=xml.getBytes(StandardCharsets.UTF_8);
            if(bytes.length>MAX_BYTES)throw new GridImportException(R.string.svg_size_error);
            extension=".svg";
        } else {
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            if(options.outWidth<=0||options.outHeight<=0)throw new GridImportException(R.string.file_type_error);
            options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1024)options.inSampleSize*=2;
            options.inJustDecodeBounds=false;options.inMutable=true;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
            bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)throw new GridImportException(R.string.image_read_error);
            int w=bitmap.getWidth(),h=bitmap.getHeight();int[] pixels=new int[w*h];bitmap.getPixels(pixels,0,w,0,0,w,h);
            boolean transparent=false;for(int p:pixels)if(Color.alpha(p)<250){transparent=true;break;}
            int[] corners={pixels[0],pixels[w-1],pixels[(h-1)*w],pixels[w*h-1]};
            int background=0;for(int p:corners)background+=(Color.red(p)+Color.green(p)+Color.blue(p))/3;
            boolean lightBackground=background/4>=128;
            for(int i=0;i<pixels.length;i++) {
                int p=pixels[i],alpha=Color.alpha(p);
                if(!transparent) {int luminance=(Color.red(p)*299+Color.green(p)*587+Color.blue(p)*114)/1000;alpha=lightBackground?255-luminance:luminance;}
                pixels[i]=Color.argb(alpha,255,255,255);
            }
            bitmap.setPixels(pixels,0,w,0,0,w,h);extension=".png";
        }
        File destination=File.createTempFile("grid-import-",extension,context.getCacheDir());
        try(FileOutputStream out=new FileOutputStream(destination)) {
            if(bitmap!=null) {if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new GridImportException(R.string.image_save_error);}
            else out.write(bytes);
        } catch(Exception e) {destination.delete();throw e;}
        finally {if(bitmap!=null)bitmap.recycle();}
        return GridFiles.register(context,name,extension,destination);
    }
}
