package dev.chrynalogia.probe;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

final class ProbeLog {
    static final String TAG = "CameraGridProbe";
    private static final long MAX_BYTES = 512 * 1024;
    static synchronized void event(Context context, String name, Object... values) {
        try {
            JSONObject json = new JSONObject().put("time", Instant.now().toString()).put("event", name);
            for (int i = 0; i + 1 < values.length; i += 2) json.put(String.valueOf(values[i]), values[i + 1]);
            String line = json.toString();
            Log.i(TAG, line);
            File file = new File(context.getFilesDir(), "probe-events.jsonl");
            if (file.length() > MAX_BYTES) {
                File old = new File(context.getFilesDir(), "probe-events.previous.jsonl");
                if (old.exists() && !old.delete()) Log.w(TAG, "Could not rotate old events");
                if (!file.renameTo(old)) Log.w(TAG, "Could not rotate events");
            }
            try (FileOutputStream stream = new FileOutputStream(file, true)) {
                stream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception error) { Log.e(TAG, "Diagnostic log failure", error); }
    }
    static synchronized void write(Context context, String name, String content) {
        try (FileOutputStream stream = context.openFileOutput(name, Context.MODE_PRIVATE)) {
            stream.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) { Log.e(TAG, "Diagnostic write failure", error); }
    }
}
