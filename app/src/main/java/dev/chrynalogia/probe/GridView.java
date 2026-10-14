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
    private final Matrix gridTurn=new Matrix();
    private final Path rabatmentPath=new Path();
    private final Map<String,Bitmap> bitmaps=new HashMap<>();
    private final Map<String,SVG> svgs=new HashMap<>();
    private final Map<GridRenderKey,Picture> pictures=new HashMap<>();
    private final Map<String,Path> paths=new HashMap<>();
    private final Map<String,Long> loading=new HashMap<>();
    private Set<String> wanted=Set.of();
    private long loadGeneration;
    private ImportAssets assets;
    private GridSettings settings;
    private final String previewId;
    private boolean firstFrame=true;
    private int displayQuarterTurn;
    GridView(Context context) {this(context,null);}
    GridView(Context context,String previewId) {
        super(context); this.previewId=previewId;
        line.setFilterBitmap(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setBackgroundColor(Color.TRANSPARENT);
        applySettings();
    }
    void setDisplayQuarterTurn(int surfaceRotation) {
        int latest=previewId==null?Math.floorMod(surfaceRotation,4):0;
        if(displayQuarterTurn==latest)return;
        displayQuarterTurn=latest;invalidate();
    }
    void applySettings() {
        GridSettings latest=new GridSettings(getContext());
        if(settings==null||SettingsChange.classify(settings.changeState(),latest.changeState()).pictures()){pictures.clear();paths.clear();}
        settings=latest;
        wanted=previewId==null?Set.copyOf(settings.selected):Set.of(previewId);
        bitmaps.keySet().retainAll(wanted);svgs.keySet().retainAll(wanted);loading.keySet().retainAll(wanted);
        pictures.keySet().removeIf(key->!wanted.contains(key.id().replace("-outline","")));
        if(assets==null)assets=new ImportAssets(getContext());
        for(int i=0;i<settings.imports.length();i++){
            ImportedGrid file=ImportedGrid.parse(settings.imports.optJSONObject(i));
            if(file==null||!wanted.contains(file.id())||bitmaps.containsKey(file.id())||svgs.containsKey(file.id())||loading.containsKey(file.id()))continue;
            long version=++loadGeneration;loading.put(file.id(),version);
            assets.load(file,result->{
                if(!java.util.Objects.equals(loading.get(file.id()),version)||!wanted.contains(file.id()))return;
                if(result.error()!=null){ProbeLog.event(getContext(),"import_prepare_error","id",file.id(),"error",result.error().toString());return;}
                loading.remove(file.id());if(result.bitmap()!=null)bitmaps.put(file.id(),result.bitmap());if(result.svg()!=null)svgs.put(file.id(),result.svg());invalidate();
            });
        }
        invalidate();
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(assets==null)applySettings();}
    @Override protected void onDetachedFromWindow(){if(assets!=null)assets.close();assets=null;loading.clear();bitmaps.clear();svgs.clear();pictures.clear();paths.clear();super.onDetachedFromWindow();}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) { pictures.clear();paths.clear(); }
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
        GridOrientation.Transform turn=GridOrientation.transform(w,h,rotation,displayQuarterTurn,settings.flipX,settings.flipY);
        float dw=turn.width(),dh=turn.height();gridTurn.setValues(turn.values());
        int lineColor=GridOrientation.lineColor(previewId!=null,AppAppearance.dark(getContext()),settings.color);
        float density=getResources().getDisplayMetrics().density;
        float stroke=settings.thickness*density;
        Set<String> ids=previewId==null?settings.selected:Set.of(previewId);
        for(String id:ids) {
            canvas.save();
            if(id.startsWith("file_")) {canvas.concat(gridTurn);drawImport(canvas,id,dw,dh,stroke,lineColor);canvas.restore();continue;}
            Path path;
            if(id.equals("rabatment")) {
                // Rebuilding on swapped axes cancels this grid's turn. Fit the path, keeping stroke width constant.
                float pw=displayQuarterTurn%2==0?w:h,ph=displayQuarterTurn%2==0?h:w;
                canvas.translate(w/2,h/2);canvas.rotate(-displayQuarterTurn*90);canvas.scale(settings.flipX?-1:1,settings.flipY?-1:1);
                rabatmentPath.set(path(id,pw,ph));path=rabatmentPath;rabatmentTurn.setTranslate(-pw/2,-ph/2);rabatmentTurn.postRotate(rotation*90);
                if(rotation%2!=0)rabatmentTurn.postScale(pw/ph,ph/pw);path.transform(rabatmentTurn);
            } else {
                // Preserve the spiral's proportions, 45-degree guides and imported-grid behavior.
                canvas.concat(gridTurn);path=path(id,dw,dh);
            }
            line.setStyle(Paint.Style.STROKE); line.setStrokeCap(Paint.Cap.BUTT);
            line.setColor(OutlineColor.forLine(lineColor)); line.setAlpha(Math.round(settings.opacity*255));
            if(settings.outline) {line.setStrokeWidth(stroke+1.3f*density);canvas.drawPath(path,line);}
            line.setColor(lineColor); line.setAlpha(Math.round(settings.opacity*255));line.setStrokeWidth(stroke);
            canvas.drawPath(path,line);
            canvas.restore();
        }
        canvas.restore();
        if(firstFrame && previewId==null) {
            firstFrame=false; ProbeLog.event(getContext(),"grid_drawn","size",getWidth()+"x"+getHeight());
        }
    }
    private Path path(String id,float w,float h) {
        String key=id+":"+w+":"+h+":"+settings.columns+":"+settings.rows;
        Path cached=paths.get(key);if(cached!=null)return cached;
        Path path=new Path();
        if(id.equals("spiral")) {
            GridGeometry.Spiral spiral=GridGeometry.spiral(w,h);
            for(GridGeometry.Segment s:spiral.partitions()) {path.moveTo(s.x1(),s.y1());path.lineTo(s.x2(),s.y2());}
            boolean first=true;
            for(GridGeometry.Arc arc:spiral.arcs()) {
                RectF circle=new RectF(arc.centerX()-arc.radius(),arc.centerY()-arc.radius(),arc.centerX()+arc.radius(),arc.centerY()+arc.radius());
                path.arcTo(circle,arc.startDegrees(),arc.sweepDegrees(),first);first=false;
            }
        } else for(GridGeometry.Segment s:GridGeometry.lines(id,w,h,settings.columns,settings.rows)) {
            path.moveTo(s.x1(),s.y1());path.lineTo(s.x2(),s.y2());
        }
        paths.put(key,path);return path;
    }
    private void drawImport(Canvas canvas,String id,float w,float h,float stroke,int lineColor) {
        JSONObject data=null;
        for(int i=0;i<settings.imports.length();i++) if(id.equals(settings.imports.optJSONObject(i).optString("id"))) {data=settings.imports.optJSONObject(i);break;}
        if(data==null) return;
        if(data.optString("extension").equals(".svg")?!svgs.containsKey(id):!bitmaps.containsKey(id))return;
        GridRenderKey key=new GridRenderKey(id,(int)w,(int)h,settings.rotation,settings.columns,settings.rows,stroke,settings.keepAspect,settings.outline);
        GridRenderKey edgeKey=new GridRenderKey(id+"-outline",(int)w,(int)h,settings.rotation,settings.columns,settings.rows,stroke,settings.keepAspect,settings.outline);
        try {
            line.setStyle(Paint.Style.FILL);line.setColor(lineColor);line.setAlpha(Math.round(settings.opacity*255));
            line.setColorFilter(new PorterDuffColorFilter(lineColor,PorterDuff.Mode.SRC_IN));
            if(data.optString("extension").equals(".svg")) {
                SVG svg=svgs.get(id);
                Picture picture=pictures.get(key);
                if(picture==null) {
                    RectF box=svg.getDocumentViewBox();
                    float basis=box==null?Math.max(w,h):Math.max(box.width(),box.height());
                    float svgWidth=stroke*basis/Math.max(w,h);
                    RenderOptions options=RenderOptions.create().viewPort(0,0,w,h)
                        .preserveAspectRatio(settings.keepAspect?PreserveAspectRatio.LETTERBOX:PreserveAspectRatio.STRETCH)
                        .css("* { stroke: white !important; stroke-width: "+svgWidth+" !important; fill: none !important; }");
                    picture=new Picture(); Canvas recording=picture.beginRecording((int)w,(int)h);
                    svg.renderToCanvas(recording,options);picture.endRecording();pictures.put(key,picture);
                }
                if(settings.outline) {
                    Picture border=pictures.get(edgeKey);
                    if(border==null) {
                        RectF box=svg.getDocumentViewBox();float basis=box==null?Math.max(w,h):Math.max(box.width(),box.height());
                        RenderOptions options=RenderOptions.create().viewPort(0,0,w,h)
                            .preserveAspectRatio(settings.keepAspect?PreserveAspectRatio.LETTERBOX:PreserveAspectRatio.STRETCH)
                            .css("* { stroke: white !important; stroke-width: "+((stroke+1.3f*getResources().getDisplayMetrics().density)*basis/Math.max(w,h))+" !important; fill: none !important; }");
                        border=new Picture();Canvas recording=border.beginRecording((int)w,(int)h);svg.renderToCanvas(recording,options);border.endRecording();pictures.put(edgeKey,border);
                    }
                    line.setColorFilter(new PorterDuffColorFilter(OutlineColor.forLine(lineColor),PorterDuff.Mode.SRC_IN));
                    int layer=canvas.saveLayer(0,0,w,h,line);canvas.drawPicture(border);canvas.restoreToCount(layer);
                    line.setColorFilter(new PorterDuffColorFilter(lineColor,PorterDuff.Mode.SRC_IN));
                }
                int layer=canvas.saveLayer(0,0,w,h,line);canvas.drawPicture(picture);canvas.restoreToCount(layer);
            } else {
                Bitmap bitmap=bitmaps.get(id);
                RectF destination=new RectF(0,0,w,h);
                if(settings.keepAspect) {
                    float scale=Math.min(w/bitmap.getWidth(),h/bitmap.getHeight());
                    float bw=bitmap.getWidth()*scale,bh=bitmap.getHeight()*scale;
                    destination.set((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2);
                }
                if(settings.outline) {
                    line.setColorFilter(new PorterDuffColorFilter(OutlineColor.forLine(lineColor),PorterDuff.Mode.SRC_IN));
                    float edge=0.65f*getResources().getDisplayMetrics().density;
                    for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)if(x!=0||y!=0) {
                        RectF border=new RectF(destination);border.offset(x*edge,y*edge);canvas.drawBitmap(bitmap,null,border,line);
                    }
                    line.setColorFilter(new PorterDuffColorFilter(lineColor,PorterDuff.Mode.SRC_IN));
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
