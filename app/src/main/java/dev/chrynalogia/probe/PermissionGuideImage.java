package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Frames an actual One UI screenshot; crop and emphasis are rendered by the view. */
@android.annotation.SuppressLint("ViewConstructor")
final class PermissionGuideImage extends View {
    private final Bitmap screenshot;
    private final Rect crop;
    private final RectF highlight;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF dest=new RectF(),emphasis=new RectF();
    private final Path clip=new Path();
    PermissionGuideImage(Context c,String file,Rect crop,RectF highlight,String description) {
        super(c);
        boolean korean="ko".equals(c.getResources().getConfiguration().getLocales().get(0).getLanguage());
        String folder="permission_guide/"+(korean?"":"en/");
        Rect selectedCrop=new Rect(crop);RectF selectedHighlight=new RectF(highlight);
        if(!korean)try(InputStream in=c.getAssets().open(folder+"regions.json")){
            JSONObject region=new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getJSONObject(file);
            JSONArray bounds=region.getJSONArray("crop"),target=region.getJSONArray("target");
            if(bounds.length()!=4||target.length()!=4)throw new IllegalStateException("Invalid guide region: "+file);
            selectedCrop.set(bounds.getInt(0),bounds.getInt(1),bounds.getInt(2),bounds.getInt(3));
            selectedHighlight.set(target.getInt(0),target.getInt(1),target.getInt(2),target.getInt(3));
        }catch(java.io.IOException|org.json.JSONException e){throw new IllegalStateException("Missing guide region: "+file,e);}
        this.crop=selectedCrop;this.highlight=selectedHighlight;
        try(InputStream in=c.getAssets().open(folder+file+".png")) {
            screenshot=BitmapFactory.decodeStream(in);
            if(screenshot==null)throw new IllegalStateException("Invalid guide screenshot: "+file);
        } catch(java.io.IOException e){throw new IllegalStateException("Missing guide screenshot: "+file,e);}
        if(this.crop.isEmpty()||!new Rect(0,0,screenshot.getWidth(),screenshot.getHeight()).contains(this.crop)
                ||this.highlight.isEmpty()||!new RectF(this.crop).contains(this.highlight))
            throw new IllegalStateException("Guide region outside screenshot: "+file);
        setContentDescription(description);
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int width=MeasureSpec.getSize(widthSpec);
        int height=Math.min(Ui.dp(getContext(),280),Math.round(width*crop.height()/(float)crop.width()));
        setMeasuredDimension(width,resolveSize(Math.max(Ui.dp(getContext(),64),height),heightSpec));
    }
    @Override protected void onDraw(Canvas canvas) {
        float scale=Math.min(getWidth()/(float)crop.width(),getHeight()/(float)crop.height());
        float w=crop.width()*scale,h=crop.height()*scale,left=(getWidth()-w)/2,top=(getHeight()-h)/2;
        dest.set(left,top,left+w,top+h);
        clip.rewind();clip.addRoundRect(dest,Ui.dp(getContext(),16),Ui.dp(getContext(),16),Path.Direction.CW);
        int save=canvas.save();canvas.clipPath(clip);canvas.drawBitmap(screenshot,crop,dest,paint);
        paint.setColor(Ui.ACCENT);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),3));
        emphasis.set(left+(highlight.left-crop.left)*scale,top+(highlight.top-crop.top)*scale,
                left+(highlight.right-crop.left)*scale,top+(highlight.bottom-crop.top)*scale);
        canvas.drawRoundRect(emphasis,Ui.dp(getContext(),9),Ui.dp(getContext(),9),paint);
        paint.setStyle(Paint.Style.FILL);canvas.restoreToCount(save);
    }
}
