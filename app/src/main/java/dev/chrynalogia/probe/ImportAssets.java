package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import com.caverock.androidsvg.SVG;
import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Shared, bounded preparation; SVGs are initialized before main-thread-only rendering. */
final class ImportAssets {
    record Result(Bitmap bitmap,SVG svg,Exception error,int bytes){}
    private static final Object LOCK=new Object();
    private static final LinkedHashMap<String,Result> CACHE=new LinkedHashMap<>(16,0.75f,true);
    private static final Map<String,CompletableFuture<Result>> PENDING=new HashMap<>();
    private static final int CACHE_BYTES=16*1024*1024;
    private static int cachedBytes;
    private static final AtomicInteger builds=new AtomicInteger();
    private static final Executor WORKERS=new ThreadPoolExecutor(2,2,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(24),task->{Thread thread=new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);task.run();},"grid-assets");thread.setDaemon(true);return thread;});
    private final Context context;private final Executor executor;private final Handler main=new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    ImportAssets(Context context){this(context,WORKERS);}
    ImportAssets(Context context,Executor executor){this.context=context.getApplicationContext();this.executor=executor;}
    static int pendingCount(){synchronized(LOCK){return PENDING.size();}}
    static int buildCount(){return builds.get();}
    void load(ImportedGrid grid,Consumer<Result> consumer){
        if(closed)return;
        File file=new File(context.getFilesDir(),"grids/"+grid.id()+grid.extension());
        String key=file.getAbsolutePath()+":"+file.lastModified()+":"+file.length();
        CompletableFuture<Result> work;boolean prepare=false;
        synchronized(LOCK){Result cached=CACHE.get(key);if(cached!=null)work=CompletableFuture.completedFuture(cached);
            else {work=PENDING.get(key);if(work==null){work=new CompletableFuture<>();PENDING.put(key,work);prepare=true;}}}
        CompletableFuture<Result> future=work;
        future.thenAccept(result->main.post(()->{if(!closed)consumer.accept(result);}));
        if(prepare){try{executor.execute(()->{
            Result result;
            try{
                builds.incrementAndGet();if(!file.isFile()||file.length()>GridImporter.MAX_BYTES)throw new java.io.IOException("Invalid prepared grid file");
                if(grid.extension().equals(".svg")){
                    SVG.setInternalEntitiesEnabled(false);SVG svg=SVG.getFromString(new String(Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8));svg.setDocumentWidth("100%");svg.setDocumentHeight("100%");
                    result=new Result(null,svg,null,(int)Math.min(Integer.MAX_VALUE,file.length()*3));
                }else{Bitmap bitmap=BitmapFactory.decodeFile(file.getPath());if(bitmap==null)throw new java.io.IOException("Cannot decode grid bitmap");result=new Result(bitmap,null,null,bitmap.getAllocationByteCount());}
            }catch(Exception error){result=new Result(null,null,error,0);}
            synchronized(LOCK){if(result.error==null&&file.exists()&&result.bytes<=CACHE_BYTES){CACHE.put(key,result);cachedBytes+=result.bytes;while(cachedBytes>CACHE_BYTES&&!CACHE.isEmpty()){var oldest=CACHE.entrySet().iterator();Result removed=oldest.next().getValue();oldest.remove();cachedBytes-=removed.bytes;}}}
            future.complete(result);synchronized(LOCK){PENDING.remove(key);}
        });}catch(RuntimeException error){future.complete(new Result(null,null,error,0));synchronized(LOCK){PENDING.remove(key);}}}
    }
    void close(){closed=true;}
}
