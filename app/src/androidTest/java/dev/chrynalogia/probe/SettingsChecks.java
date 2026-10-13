package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.Color;
import java.util.Set;

/** Real SharedPreferences regressions, restored by the instrumentation runner. */
final class SettingsChecks {
    static int run(Context c) {
        c.getSharedPreferences("probe",0).edit().clear().putBoolean("retiredGoldenRatio",true).commit();
        GridSettings a=new GridSettings(c), b=new GridSettings(c);
        b.rotation=1;b.save();a.color=Color.BLACK;a.save();
        require(new GridSettings(c).rotation==1,"Stale style snapshot overwrote rotation");
        require(new GridSettings(c).color==Color.BLACK,"Latest color was lost");
        a=new GridSettings(c);b=new GridSettings(c);
        b.selected=Set.of("thirds");b.save();a.opacity=0.4f;a.save();
        require(new GridSettings(c).selected.equals(Set.of("thirds")),"Style snapshot overwrote selection");
        var before=c.getSharedPreferences("probe",0).getAll();new GridSettings(c).reload();
        require(before.equals(c.getSharedPreferences("probe",0).getAll()),"Reading settings wrote preferences");
        c.getSharedPreferences("probe",0).edit().putInt("columns",99).putInt("rows",0).putFloat("opacity",Float.NaN).putFloat("thickness",Float.NaN).putInt("rotation",-1).putString("imports","[null,\"bad\",{}]").commit();
        GridSettings invalid=new GridSettings(c);
        require(invalid.columns==16&&invalid.rows==1,"Grid count validation");
        require(invalid.opacity==0.9f&&invalid.thickness==1.2f,"Finite style validation");
        require(invalid.rotation==3&&invalid.imports.length()==0,"Rotation and metadata validation");
        c.getSharedPreferences("probe",0).edit().putString("columns","broken").putString("runMode","unknown").commit();
        require(new GridSettings(c).columns==3&&new GridSettings(c).runMode.equals("always"),"Wrong preference types must recover");
        return 8;
    }
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
