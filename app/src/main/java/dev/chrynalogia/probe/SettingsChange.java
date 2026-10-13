package dev.chrynalogia.probe;
import java.util.Set;
record SettingsChange(boolean render,boolean menu,boolean session,boolean pictures) {
    record State(int color,float opacity,float thickness,boolean outline,boolean hidden,Set<String> selected,int rotation,int columns,int rows,boolean keepAspect,boolean allowStop,String runMode,boolean flipX,boolean flipY){}
    static SettingsChange classify(State a,State b){
        boolean pictures=a.thickness!=b.thickness||a.outline!=b.outline||a.rotation!=b.rotation||a.columns!=b.columns||a.rows!=b.rows||a.keepAspect!=b.keepAspect||!a.selected.equals(b.selected);
        boolean render=pictures||a.color!=b.color||a.opacity!=b.opacity||a.hidden!=b.hidden||a.flipX!=b.flipX||a.flipY!=b.flipY;
        return new SettingsChange(render,a.hidden!=b.hidden||a.allowStop!=b.allowStop||!a.runMode.equals(b.runMode),!a.runMode.equals(b.runMode),pictures);
    }
}
