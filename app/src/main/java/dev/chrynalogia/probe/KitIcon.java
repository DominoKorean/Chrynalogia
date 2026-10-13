package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.*;
import android.widget.ImageView;
import com.caverock.androidsvg.SVG;

/** Unmodified SVG exports from the user's One UI kit. */
@android.annotation.SuppressLint("ViewConstructor") // Built programmatically with an explicit kit SVG.
final class KitIcon extends ImageView {
    private static final android.util.LruCache<String,Bitmap> bitmaps=new android.util.LruCache<>(32);
    KitIcon(Context c,int resource,int color) {
        super(c);setImageDrawable(drawable(c,resource,color));
        setScaleType(ScaleType.FIT_CENTER);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    static android.graphics.drawable.Drawable drawable(Context c,int resource,int color) {
        android.graphics.drawable.BitmapDrawable d=new android.graphics.drawable.BitmapDrawable(c.getResources(),bitmap(c,resource));
        d.setColorFilter(new PorterDuffColorFilter(color,PorterDuff.Mode.SRC_IN));int size=Ui.dp(c,24);d.setBounds(0,0,size,size);return d;
    }
    static Bitmap bitmap(Context c,int resource) {
        String key=resource+":"+c.getResources().getDisplayMetrics().densityDpi;
        synchronized(bitmaps){Bitmap cached=bitmaps.get(key);if(cached!=null)return cached;}
        try {
            SVG svg=SVG.getFromResource(c,resource);int size=Ui.dp(c,24);
            svg.setDocumentWidth("100%");svg.setDocumentHeight("100%");
            Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);new Canvas(bitmap).drawPicture(svg.renderToPicture(size,size));
            synchronized(bitmaps){Bitmap existing=bitmaps.get(key);if(existing!=null)return existing;bitmaps.put(key,bitmap);}
            return bitmap;
        } catch(Exception e) {throw new IllegalArgumentException("Invalid kit asset "+resource,e);}
    }
}
