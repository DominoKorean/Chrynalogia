package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.RenderOptions;
import com.caverock.androidsvg.PreserveAspectRatio;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

final class GridView extends View {
    private final Paint line=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix rabatmentTurn=new Matrix();
    private final Map<String,Bitmap> bitmaps=new HashMap<>();
    private final Map<String,SVG> svgs=new HashMap<>();
    private final Map<String,Picture> pictures=new HashMap<>();
    private GridSettings settings;
    private final String previewId;
    private boolean firstFrame=true;
    GridView(Context context) {this(context,null);}
    GridView(Context context,String previewId) {
        super(context); this.previewId=previewId;
        line.setFilterBitmap(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setBackgroundColor(Color.TRANSPARENT);
        applySettings();
    }
    void applySettings() {
        settings=new GridSettings(getContext()); pictures.clear();
        Set<String> known=new java.util.HashSet<>();
        for(int i=0;i<settings.imports.length();i++) known.add(settings.imports.optJSONObject(i).optString("id"));
        bitmaps.keySet().retainAll(known); svgs.keySet().retainAll(known);
        invalidate();
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) { pictures.clear(); }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        if(previewId!=null && MeasureSpec.getMode(widthSpec)==MeasureSpec.EXACTLY) {
            int w=MeasureSpec.getSize(widthSpec);setMeasuredDimension(w,Math.round(w*148f/218));
        } else super.onMeasure(widthSpec,heightSpec);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(settings.hidden && previewId==null) return;
        float w=getWidth(), h=getHeight();
        if(w<=0 || h<=0) return;
        canvas.save(); canvas.clipRect(0,0,w,h);
        int rotation=previewId==null||settings.selected.contains(previewId)?settings.rotation:0;
        float dw=rotation%2==0?w:h, dh=rotation%2==0?h:w;
        float density=getResources().getDisplayMetrics().density;
        float stroke=settings.thickness*density;
        Set<String> ids=previewId==null?settings.selected:Set.of(previewId);
        for(String id:ids) {
            canvas.save();canvas.translate(w/2,h/2);canvas.scale(settings.flipX?-1:1,settings.flipY?-1:1);
            if(id.startsWith("file_")) {canvas.rotate(rotation*90);canvas.translate(-dw/2,-dh/2);drawImport(canvas,id,dw,dh,stroke);canvas.restore();continue;}
            Path path;
            if(id.equals("rabatment")) {
                // Rebuilding on swapped axes cancels this grid's turn. Fit the path, keeping stroke width constant.
                path=path(id,w,h);rabatmentTurn.setTranslate(-w/2,-h/2);rabatmentTurn.postRotate(rotation*90);
                if(rotation%2!=0)rabatmentTurn.postScale(w/h,h/w);path.transform(rabatmentTurn);
            } else {
                // Preserve the spiral's proportions, 45-degree guides and imported-grid behavior.
                canvas.rotate(rotation*90);canvas.translate(-dw/2,-dh/2);path=path(id,dw,dh);
            }
            line.setStyle(Paint.Style.STROKE); line.setStrokeCap(Paint.Cap.BUTT);
            line.setColor(OutlineColor.forLine(settings.color)); line.setAlpha(Math.round(settings.opacity*255));
            if(settings.outline) {line.setStrokeWidth(stroke+1.3f*density);canvas.drawPath(path,line);}
            line.setColor(settings.color); line.setAlpha(Math.round(settings.opacity*255));line.setStrokeWidth(stroke);
            canvas.drawPath(path,line);
            canvas.restore();
        }
        canvas.restore();
        if(firstFrame && previewId==null) {
            firstFrame=false; ProbeLog.event(getContext(),"grid_drawn","size",getWidth()+"x"+getHeight());
        }
    }
    private Path path(String id,float w,float h) {
        Path path=new Path();
        if(id.equals("spiral")) {
            float rw=Math.min(w,h*1.618034f), rh=rw/1.618034f;
            float ox=(w-rw)/2, oy=(h-rh)/2;
            double k=Math.log(1.61803398875)/(Math.PI/2);
            double minX=Double.MAX_VALUE,minY=minX,maxX=-minX,maxY=-minX;
            for(int i=0;i<=720;i++) {
                double angle=i*Math.PI/180, r=Math.exp(k*angle);
                double x=r*Math.cos(angle),y=r*Math.sin(angle);
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
            }
            double scale=Math.min(rw/(maxX-minX),rh/(maxY-minY));
            float px=ox+(rw-(float)((maxX-minX)*scale))/2, py=oy+(rh-(float)((maxY-minY)*scale))/2;
            for(int i=0;i<=720;i++) {
                double a=i*Math.PI/180,r=Math.exp(k*a);
                float x=px+(float)((r*Math.cos(a)-minX)*scale),y=py+(float)((r*Math.sin(a)-minY)*scale);
                if(i==0) path.moveTo(x,y);else path.lineTo(x,y);
            }
        } else for(GridGeometry.Segment s:GridGeometry.lines(id,w,h,settings.columns,settings.rows)) {
            path.moveTo(s.x1(),s.y1());path.lineTo(s.x2(),s.y2());
        }
        return path;
    }
    private void drawImport(Canvas canvas,String id,float w,float h,float stroke) {
        JSONObject data=null;
        for(int i=0;i<settings.imports.length();i++) if(id.equals(settings.imports.optJSONObject(i).optString("id"))) {data=settings.imports.optJSONObject(i);break;}
        if(data==null) return;
        File file=new File(getContext().getFilesDir(),"grids/"+id+data.optString("extension"));
        try {
            line.setStyle(Paint.Style.FILL);line.setColor(settings.color);line.setAlpha(Math.round(settings.opacity*255));
            line.setColorFilter(new PorterDuffColorFilter(settings.color,PorterDuff.Mode.SRC_IN));
            if(data.optString("extension").equals(".svg")) {
                SVG svg=svgs.get(id);
                if(svg==null) {SVG.setInternalEntitiesEnabled(false);try(java.io.FileInputStream input=new java.io.FileInputStream(file)) {svg=SVG.getFromInputStream(input);}svg.setDocumentWidth("100%");svg.setDocumentHeight("100%");svgs.put(id,svg);}
                Picture picture=pictures.get(id);
                if(picture==null) {
                    RectF box=svg.getDocumentViewBox();
                    float basis=box==null?Math.max(w,h):Math.max(box.width(),box.height());
                    float svgWidth=stroke*basis/Math.max(w,h);
                    RenderOptions options=RenderOptions.create().viewPort(0,0,w,h)
                        .preserveAspectRatio(settings.keepAspect?PreserveAspectRatio.LETTERBOX:PreserveAspectRatio.STRETCH)
                        .css("* { stroke: white !important; stroke-width: "+svgWidth+" !important; fill: none !important; }");
                    picture=new Picture(); Canvas recording=picture.beginRecording((int)w,(int)h);
                    svg.renderToCanvas(recording,options);picture.endRecording();pictures.put(id,picture);
                }
                if(settings.outline) {
                    Picture border=pictures.get(id+"-outline");
                    if(border==null) {
                        RectF box=svg.getDocumentViewBox();float basis=box==null?Math.max(w,h):Math.max(box.width(),box.height());
                        RenderOptions options=RenderOptions.create().viewPort(0,0,w,h)
                            .preserveAspectRatio(settings.keepAspect?PreserveAspectRatio.LETTERBOX:PreserveAspectRatio.STRETCH)
                            .css("* { stroke: white !important; stroke-width: "+((stroke+1.3f*getResources().getDisplayMetrics().density)*basis/Math.max(w,h))+" !important; fill: none !important; }");
                        border=new Picture();Canvas recording=border.beginRecording((int)w,(int)h);svg.renderToCanvas(recording,options);border.endRecording();pictures.put(id+"-outline",border);
                    }
                    line.setColorFilter(new PorterDuffColorFilter(OutlineColor.forLine(settings.color),PorterDuff.Mode.SRC_IN));
                    int layer=canvas.saveLayer(0,0,w,h,line);canvas.drawPicture(border);canvas.restoreToCount(layer);
                    line.setColorFilter(new PorterDuffColorFilter(settings.color,PorterDuff.Mode.SRC_IN));
                }
                int layer=canvas.saveLayer(0,0,w,h,line);canvas.drawPicture(picture);canvas.restoreToCount(layer);
            } else {
                Bitmap bitmap=bitmaps.get(id);
                if(bitmap==null) {bitmap=BitmapFactory.decodeFile(file.getPath());if(bitmap==null)return;bitmaps.put(id,bitmap);}
                RectF destination=new RectF(0,0,w,h);
                if(settings.keepAspect) {
                    float scale=Math.min(w/bitmap.getWidth(),h/bitmap.getHeight());
                    float bw=bitmap.getWidth()*scale,bh=bitmap.getHeight()*scale;
                    destination.set((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2);
                }
                if(settings.outline) {
                    line.setColorFilter(new PorterDuffColorFilter(OutlineColor.forLine(settings.color),PorterDuff.Mode.SRC_IN));
                    float edge=0.65f*getResources().getDisplayMetrics().density;
                    for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)if(x!=0||y!=0) {
                        RectF border=new RectF(destination);border.offset(x*edge,y*edge);canvas.drawBitmap(bitmap,null,border,line);
                    }
                    line.setColorFilter(new PorterDuffColorFilter(settings.color,PorterDuff.Mode.SRC_IN));
                }
                canvas.drawBitmap(bitmap,null,destination,line);
            }
            line.setColorFilter(null);
        } catch(Exception error) {
            line.setColorFilter(null);
            ProbeLog.event(getContext(),"import_draw_error","id",id,"error",error.toString());
        }
    }
}
