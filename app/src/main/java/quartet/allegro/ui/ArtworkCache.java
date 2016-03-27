package quartet.allegro.ui;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import quartet.allegro.GooshasmApp;
import quartet.allegro.database.AlbumData;

import static quartet.allegro.AllegroActivity.log;

/**
 * Created by akbar on 6/30/15.
 */
public class ArtworkCache {

    private static ArtworkCache _instance;

    public static ArtworkCache getInstance(Activity activity){
        if (_instance == null) {
            _instance = new ArtworkCache(activity);
        }
        return _instance;
    }

    Drawable placeHolderDrawable;

    Activity activity;
    ArtworkLoadThread loadThread;
    boolean threadStarted;

    private final ConcurrentHashMap<AlbumData, CacheItem> artworkCache;
    private int cacheSize = 0;

    public ArtworkCache(Activity activity) {
        this.activity = activity;
        this.threadStarted = false;
        loadThread = new ArtworkLoadThread();
        artworkCache = new ConcurrentHashMap<>();
        placeHolderDrawable = new BitmapDrawable(activity.getResources(), ((GooshasmApp)activity.getApplication()).getPlaceHolder());
    }

    public void startThread(){
        if (!threadStarted){
            threadStarted = true;
            loadThread.start();
        }
    }

    public boolean isThreadStarted() {
        return threadStarted;
    }

    public void loadAlbumArt(final AlbumData album, final ImageView imageView) {

        imageView.setTag(album);
        CacheItem cached = artworkCache.get(album);

        // load image from cache if available
        if (cached != null) {

            cached.accessTime = System.currentTimeMillis();
            if (cached.available && cached.drawable != null) {
                imageView.setImageDrawable(cached.drawable);
            } else {
                imageView.setImageDrawable(placeHolderDrawable);
            }

            return;
        }

        imageView.setImageDrawable(placeHolderDrawable);
        LoadTask task = new LoadTask(album, System.currentTimeMillis(), imageView);
        loadThread.enqueueTask(task);

    }

    private void notifyArtworkLoaded(final AlbumData album, final CacheItem cacheItem, final ImageView imageView) {
        if (imageView.getTag() == album) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (cacheItem.drawable != null) {
                        cacheItem.accessTime = System.currentTimeMillis();
                        imageView.setImageDrawable(cacheItem.drawable);
                    }
                    else
                        imageView.setImageDrawable(placeHolderDrawable);
                }
            });
        }
    }

    private static class CacheItem {
        Drawable drawable = null;
        boolean available = false;
        long accessTime = 0;
        int size;

        public CacheItem(Drawable drawable, boolean available, long accessTime, int size) {
            this.drawable = drawable;
            this.available = available;
            this.accessTime = accessTime;
            this.size = size;
        }
    }

    private static class LoadTask implements Comparable {

        AlbumData albumData;
        Long accessTime;
        ImageView imageView;

        public LoadTask(AlbumData albumData, long accessTime, ImageView imageView) {
            this.albumData = albumData;
            this.accessTime = accessTime;
            this.imageView = imageView;
        }


        @Override
        public int compareTo(Object another) {
            return -(accessTime.compareTo(((LoadTask) another).accessTime));
        }
    }

    private class ArtworkLoadThread extends Thread {

        boolean haltSignal = false;
        int cacheLimit;
        final Queue<LoadTask> loadQueue = new PriorityQueue<>(); // TODO maybe linked blocking queue?

        private void enqueueTask(LoadTask task){
            synchronized (loadQueue) {
                loadQueue.add(task);
                loadQueue.notify();
            }
        }

        @Override
        public void run() {

            long allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
            cacheLimit = (int) ((Runtime.getRuntime().maxMemory() - allocatedMemory) * 0.5);
//            cacheLimit = (int) ((Runtime.getRuntime().freeMemory()));

            while (!haltSignal) {

                try {

                    LoadTask task;

                    synchronized (loadQueue) {

                        if (loadQueue.isEmpty()) {
                            loadQueue.wait();
                        }

                        task = loadQueue.poll();
                    }

                    if (task == null)
                        continue;

                    if (haltSignal) {
                        break;
                    }

                    String uriString = task.albumData.getAlbumArtPath();

                    boolean success = false;
                    Drawable drawable = null;
                    int fileSize = 0;

                    try {

                        if (uriString == null)
                            throw new FileNotFoundException();

                        RandomAccessFile f = new RandomAccessFile(uriString, "r");
                        FileChannel inChannel = f.getChannel();
                        fileSize = (int) inChannel.size();

                        InputStream is = Channels.newInputStream(inChannel);

                        drawable = new BitmapDrawable(is);
                        success = true;

                    } catch (FileNotFoundException e) {
                        log("XXXXXX read file error: "+e.getMessage());
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    CacheItem cacheItem = new CacheItem(drawable, success, task.accessTime, fileSize);

                    if (cacheSize < cacheLimit) {
                        cacheSize += fileSize;
                        artworkCache.put(task.albumData, cacheItem);
                    }

                    notifyArtworkLoaded(task.albumData, cacheItem, task.imageView);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.gc();

                if (cacheSize >= cacheLimit) {


                    log("xxxxxx cachesize: ", cacheSize, "- cachelimit:", cacheLimit);

                    // TODO might need a better data structure

                    AlbumData[] albumDatas = new AlbumData[artworkCache.size()];
                    artworkCache.keySet().toArray(albumDatas);

                    Arrays.sort(albumDatas, new Comparator<AlbumData>() {
                        @Override
                        public int compare(AlbumData lhs, AlbumData rhs) {
                            return artworkCache.get(lhs).accessTime < artworkCache.get(rhs).accessTime ? -1 : +1;
                        }
                    });

                    int idx = 0;
                    while (cacheSize > cacheLimit) {


                        if (idx >= albumDatas.length)
                            break;

                        log("XXXXX removing item from cache with access", artworkCache.get(albumDatas[idx]).accessTime);

                        cacheSize -= artworkCache.get(albumDatas[idx]).size;
                        artworkCache.remove(albumDatas[idx]);

                        idx ++;
                    }

                }

            }
        }

        public void sendHaltSignal(){
            haltSignal = true;
            synchronized (loadQueue){
                loadQueue.notifyAll();
            }
        }

    }

    public Drawable getCachedArtwork(AlbumData data) {
        return artworkCache.get(data) == null ? null : artworkCache.get(data).drawable;
    }

    public void dispose(){

        loadThread.sendHaltSignal();

        while (true) {
            log("XXXXX trying to join image load thread");
            try {
                loadThread.join();
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                break;
            }
        }

        log("XXXX final join thread successful");

    }

}
