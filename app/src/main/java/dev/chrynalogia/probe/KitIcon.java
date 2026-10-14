package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.*;
import android.widget.ImageView;
import com.caverock.androidsvg.SVG;

/** Unmodified SVG exports from the user's One UI kit. */
@android.annotation.SuppressLint("ViewConstructor") // Built programmatically with an explicit kit SVG.
final class KitIcon extends ImageView {
    private static final android.util.LruCache<String,Bitmap> bitmaps=new android.util.LruCache<>(32);
    KitIcon(Context c,int resource) {
        super(c);setImageBitmap(bitmap(c,resource,48));setScaleType(ScaleType.FIT_CENTER);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    KitIcon(Context c,int resource,int color) {
        super(c);setImageDrawable(drawable(c,resource,color));
        setScaleType(ScaleType.FIT_CENTER);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    static android.graphics.drawable.Drawable drawable(Context c,int resource,int color) {
        android.graphics.drawable.BitmapDrawable d=new android.graphics.drawable.BitmapDrawable(c.getResources(),bitmap(c,resource));
        d.setColorFilter(new PorterDuffColorFilter(color,PorterDuff.Mode.SRC_IN));int size=Ui.dp(c,24);d.setBounds(0,0,size,size);return d;
    }
    static Bitmap bitmap(Context c,int resource) {
        return bitmap(c,resource,24);
    }
    /** Fit the original asset's visible glyph, so differing SVG whitespace cannot change icon size. */
    static android.graphics.drawable.Drawable fittedDrawable(Context c,int resource,int color,int glyphDp){
        Bitmap source=bitmap(c,resource,48);Rect ink=new Rect(source.getWidth(),source.getHeight(),0,0);
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if(Color.alpha(source.getPixel(x,y))>20){
            ink.left=Math.min(ink.left,x);ink.top=Math.min(ink.top,y);ink.right=Math.max(ink.right,x+1);ink.bottom=Math.max(ink.bottom,y+1);
        }
        int size=Ui.dp(c,24);Bitmap fitted=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
        fitted.setDensity(c.getResources().getDisplayMetrics().densityDpi);
        if(!ink.isEmpty()){
            float scale=Ui.dp(c,glyphDp)/(float)Math.max(ink.width(),ink.height());
            float w=ink.width()*scale,h=ink.height()*scale;
            new Canvas(fitted).drawBitmap(source,ink,new RectF((size-w)/2,(size-h)/2,(size+w)/2,(size+h)/2),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
        }
        android.graphics.drawable.BitmapDrawable d=new android.graphics.drawable.BitmapDrawable(c.getResources(),fitted);
        d.setColorFilter(new PorterDuffColorFilter(color,PorterDuff.Mode.SRC_IN));d.setBounds(0,0,size,size);return d;
    }
    private static Bitmap bitmap(Context c,int resource,int dpSize) {
        String key=resource+":"+c.getResources().getDisplayMetrics().densityDpi+":"+dpSize;
        synchronized(bitmaps){Bitmap cached=bitmaps.get(key);if(cached!=null)return cached;}
        try {
            SVG svg=SVG.getFromResource(c,resource);int size=Ui.dp(c,dpSize);
            svg.setDocumentWidth("100%");svg.setDocumentHeight("100%");
            Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);new Canvas(bitmap).drawPicture(svg.renderToPicture(size,size));
            synchronized(bitmaps){Bitmap existing=bitmaps.get(key);if(existing!=null)return existing;bitmaps.put(key,bitmap);}
            return bitmap;
        } catch(Exception e) {throw new IllegalArgumentException("Invalid kit asset "+resource,e);}
    }
}
