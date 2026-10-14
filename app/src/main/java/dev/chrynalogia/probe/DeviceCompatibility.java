package dev.chrynalogia.probe;

import android.content.Context;
import android.content.pm.FeatureInfo;

/** Uses One UI's advertised OS version features, not the hardware manufacturer. */
final class DeviceCompatibility {
    private static final String ONE_UI="com.samsung.android.oneui.version.";
    static boolean hasOneUi(Context context) {
        FeatureInfo[] features=context.getPackageManager().getSystemAvailableFeatures();
        if(features==null)return false;
        String[] names=new String[features.length];
        for(int i=0;i<features.length;i++)names[i]=features[i]==null?null:features[i].name;
        return hasOneUi(names);
    }
    static boolean hasOneUi(String[] features) {
        if(features==null)return false;
        for(String name:features)if(name!=null&&name.startsWith(ONE_UI)) {
            String version=name.substring(ONE_UI.length());
            if(version.matches("[1-9][0-9]*"))return true;
        }
        return false;
    }
}
