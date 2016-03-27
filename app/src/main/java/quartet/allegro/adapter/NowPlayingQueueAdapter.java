package quartet.allegro.adapter;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import quartet.allegro.R;
import quartet.allegro.database.TrackData;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NowPlayingQueueAdapter extends ArrayAdapter {

    private static final int NUM_THREADS = 10  ;
    private final Context context ;
    private static final int MAX_STRING_LENGTH = 29 ;
    private List<TrackData> trackData ;

    public int capacity = 0 ;
    ExecutorService executorService ;
    private Handler handler ;
    private LayoutInflater inflater ;

    class QueueViewHolder {

        TextView songText ;
        TextView albumText ;
        ImageView image ;

    }

    public NowPlayingQueueAdapter(Context context, List<TrackData> values) {

        super(context, R.layout.now_playing_queue_fragment, values) ;
        this.context = context ;
        this.trackData = values ;
        this.capacity = values.size() ;
        inflater = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);

        executorService = Executors.newFixedThreadPool(NUM_THREADS) ;
        handler = new Handler() ;

    }

    @Override
    public TrackData getItem(int position) {
        if(trackData != null) {
            return trackData.get(position) ;
        } else {
            return null ;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null) {

            convertView = inflater.inflate(R.layout.now_playing_queue_list_item, parent, false);

            QueueViewHolder holder = new QueueViewHolder() ;

            holder.songText = (TextView) convertView.findViewById(R.id.queue_list_item_song_text);
            String tmp = trackData.get(position).getDisplayName();
            holder.songText.setText(tmp);

            holder.albumText = (TextView) convertView.findViewById(R.id.queue_list_item_album_text);
            tmp = trackData.get(position).getAlbum().getTitle();
            holder.albumText.setText(tmp);

            convertView.setTag(holder);

        /*image = (ImageView)root.findViewById(R.id.queue_list_item_album_cover) ;

        tmp = trackData.get(position).getAlbum().getAlbumArtPath() ;
        if(tmp != null){
            //new Drawableloader(image).execute(tmp) ;
            executorService.subm it(new ImageViewInitializer(tmp , image)) ;
        }*/

        } else {

            QueueViewHolder holder = (QueueViewHolder)convertView.getTag() ;

            String tmp = trackData.get(position).getDisplayName();
            holder.songText.setText(tmp);

            tmp = trackData.get(position).getAlbum().getTitle();
            holder.albumText.setText(tmp);

        }

        return convertView ;
    }

    public String cutString(String tmp){
        if(tmp != null) {
            if (tmp.length() > MAX_STRING_LENGTH) {
                tmp = tmp.substring(0, MAX_STRING_LENGTH - 3);
                tmp += "..." ;
            }
        }

        return tmp ;
    }

    public void updateDataSet(List<TrackData> data){
        this.trackData = data ;
    }

    @Override
    public void insert(Object d, int index) {
        trackData.add(index , (TrackData)d);
    }

    @Override
    public void remove(Object object) {
        trackData.remove(trackData.indexOf((TrackData)object)) ;
    }

    @Override
    public int getCount() {
        return trackData.size() ;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    /*class Drawableloader extends AsyncTask<String, Void, Drawable> {
        private final WeakReference<ImageView> imageViewReference;

        public Drawableloader(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Drawable doInBackground(String... params) {
            return Drawable.createFromPath(params[0]) ;
        }

        @Override
        protected void onPostExecute(Drawable d) {
            if (imageViewReference != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    if(d != null) {
                        imageView.setImageDrawable(d);
                    }
                }
            }
        }
    }*/

    class ImageViewInitializer implements  Runnable {

        String path ;
        ImageView image ;
        public ImageViewInitializer(String path , ImageView img){
            this.path = path ;
            this.image = img ;
        }

        public void run() {
            Drawable d = Drawable.createFromPath(path) ;
            if(d != null) {
                DrawableLoader dl = new DrawableLoader(d , image) ;
                handler.post(dl) ;
            }
        }

    }

    class DrawableLoader implements Runnable {
        Drawable drawable ;
        ImageView image ;

        public DrawableLoader(Drawable d , ImageView img){
            drawable = d ;
            image = img ;
        }

        public void run() {
            image.setImageDrawable(drawable);
        }
    }

}