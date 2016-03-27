package quartet.allegro.ui.fragment;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v4.app.Fragment ;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup ;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import com.gc.materialdesign.views.ButtonFloat ;

import quartet.allegro.AllegroActivity;

import quartet.allegro.R;
import quartet.allegro.database.TrackData;
import quartet.allegro.bone.EargasmService;

import android.view.View.OnClickListener;
import quartet.allegro.bone.MusicPlayerUI;

import java.util.List;
import quartet.allegro.communication.SongDisplayer;
import quartet.allegro.communication.UserInteractListener;
import quartet.allegro.adapter.NowPlayingQueueAdapter;
import quartet.allegro.ui.SlidingFrameLayout;
import quartet.allegro.communication.Support;
import quartet.allegro.ui.widget.PlayerControlWidgetProvider;

import static quartet.allegro.AllegroActivity.log;


public class NowPlayingFragment extends Fragment implements SongDisplayer, MusicPlayerUI {

    private enum RepeatMode {
        REPEAT_DISABLED ,
        REPEAT_ONCE ,
        REPEAT_TWICE
    }

    private static int num_threads = 0 ;

    private Object lock = new Object() ;

    public boolean SHOWING_LYRICS = false ;
    public boolean SHOWING_QUEUE = false ;
    public boolean PLAYING = false ;
    public boolean SHUFFLING = false ;

    private boolean postponeInitialization = false ;

    private boolean userSeeking = false ;

    private static final int SWIPE_THRESHOLD = 100 ;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100 ;
    private static final String DEBUG_TAG = "NOW_PLAYING" ;

    private GestureDetectorCompat gestureDetector ;
    private View.OnTouchListener listener ;

    private boolean isPaused = false ;

    private ImageSwitcher pausePlayButton ;
    private ImageSwitcher shuffleButton ;
    private ImageSwitcher repeatButton ;
    private ImageButton forwardButton ;
    private ImageButton rewindButton ;

    private TextView songNameTextView ;
    private TextView albumNameTextView ;
    private TextView trackNumberTextView ;
    private TextView trackProgresTextView ;

    private FrameLayout lyricsAndSeekBarFrameLayout ;
    private SlidingFrameLayout nowPlayingMainLayout ;
    private ImageSwitcher albumArtSwitcher ;
    private FrameLayout queueFrameLayout ;

    private FragmentManager fragmentManager ;
    private NowPlayingQueueFragment nowPlayingQueueFragment ;

    private ButtonFloat queueButtonFloat ;

    private LyricsFlipFragment lyricsFlipFragment = null ;
    private SeekBarFlipFragment seekBarFlipFragment = null ;
    private NowPlayingQueueAdapter queueAdapter = null ;

    private List<TrackData> currentTrackDataList ;
    private UserInteractListener userInteractListener ;
    public int queuePosition ;

    private String totalTrackTime ;

    private int songSeconds ;
    private int currentProgress ;
    private Handler progressHandler = new Handler() ;
    private Thread progressThread ;

    private RepeatMode repeatMode = RepeatMode.REPEAT_DISABLED ;

    long prevSeekTime ;
    long currentSeekTime ;
    long sleepTime = 1000 ;

    boolean terminateSeekBarThread = false ;
    boolean seekBarThreadTerminated = true ;

    public static NowPlayingFragment newInstance() {

        NowPlayingFragment frag = new NowPlayingFragment() ;
        return frag ;

    }

    public static NowPlayingFragment newInstance(UserInteractListener uil){
        NowPlayingFragment frag = new NowPlayingFragment() ;
        frag.userInteractListener = uil ;
        return frag ;
    }

    public NowPlayingFragment() {
        Support.getInstance() ;
    }

    private AllegroActivity activity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = (AllegroActivity) activity;

        if(postponeInitialization){
            delayedLoadQueue();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        gestureDetector = new GestureDetectorCompat(activity , new QueueGestureDetector()) ;
        listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent) ;
            }
        } ;

        final View root = inflater.inflate(R.layout.now_playing_layout , container , false) ;
        nowPlayingMainLayout = (SlidingFrameLayout)root.findViewById(R.id.now_playing_sliding_frame_layout) ;

        ////////////////////////////////////////////////////////////////////////////////////////

        songNameTextView = (TextView)root.findViewById(R.id.now_playing_song_name) ;
        albumNameTextView = (TextView)root.findViewById(R.id.now_playing_artist_album_name);
        trackNumberTextView = (TextView)root.findViewById(R.id.track_number_text_view) ;
        trackProgresTextView = (TextView)root.findViewById(R.id.track_progress_text_view) ;

        ////////////////////////////////////////////////////////////////////////////////////////

        rewindButton = (ImageButton)root.findViewById(R.id.now_playing_prev_button);

        rewindButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AllegroActivity) activity).serviceConnectionPrev();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////

        forwardButton = (ImageButton)root.findViewById(R.id.now_playing_next_button);

        forwardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AllegroActivity)activity).serviceConnectionNext() ;
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////

        shuffleButton = (ImageSwitcher)root.findViewById(R.id.now_playing_shuffle_button) ;
        shuffleButton.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView img = new ImageView(activity.getApplicationContext());

                img.setScaleType(ImageView.ScaleType.FIT_CENTER);

                ImageSwitcher.LayoutParams params = new ImageSwitcher.LayoutParams(
                        ImageSwitcher.LayoutParams.MATCH_PARENT, ImageSwitcher.LayoutParams.MATCH_PARENT);
                img.setLayoutParams(params);


                return img;
            }
        });

        shuffleButton.setImageResource(R.drawable.ic_shuffle_black_48dp);
        shuffleButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                shuffleButton.setInAnimation(Support.getInstance().FadeInAnimation);
                shuffleButton.setOutAnimation(Support.getInstance().FadeOutAnimation);
                if(!SHUFFLING){
                    shuffleButton.setImageResource(R.drawable.ic_shuffle_white_48dp);
                    SHUFFLING = true ;
                    Toast.makeText(activity, "Shuffle Enabled",
                            Toast.LENGTH_LONG).show();
                } else {
                    shuffleButton.setImageResource(R.drawable.ic_shuffle_black_48dp);
                    SHUFFLING = true ;
                    Toast.makeText(activity, "Shuffle Disabled",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////

        repeatButton = (ImageSwitcher)root.findViewById(R.id.now_playing_repeat_button) ;
        repeatButton.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView img = new ImageView(activity.getApplicationContext());

                ImageSwitcher.LayoutParams params = new ImageSwitcher.LayoutParams(
                        ImageSwitcher.LayoutParams.MATCH_PARENT, ImageSwitcher.LayoutParams.MATCH_PARENT);
                img.setLayoutParams(params);

                return img;
            }
        });

        repeatButton.setImageResource(R.drawable.ic_repeat_one_white_48dp);
        repeatButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                repeatButton.setInAnimation(Support.getInstance().FadeInAnimation);
                repeatButton.setOutAnimation(Support.getInstance().FadeOutAnimation);

                if (repeatMode == RepeatMode.REPEAT_DISABLED){
                    repeatButton.setImageResource(R.drawable.ic_loop_white_48dp);
                    repeatMode = RepeatMode.REPEAT_ONCE ;
                    Toast.makeText(activity, "Repeating Once",
                            Toast.LENGTH_LONG).show();
                    ((AllegroActivity)activity).serviceConnectionRepeat(AllegroActivity.RepeatState.QUEUE);
                } else if (repeatMode ==  RepeatMode.REPEAT_ONCE){
                    repeatButton.setImageResource(R.drawable.ic_repeat_black_48dp);
                    repeatMode = RepeatMode.REPEAT_TWICE ;
                    Toast.makeText(activity, "Looping Current Track",
                            Toast.LENGTH_LONG).show();
                    ((AllegroActivity)activity).serviceConnectionRepeat(AllegroActivity.RepeatState.CURRENT);
                } else {
                    repeatButton.setImageResource(R.drawable.ic_repeat_one_white_48dp);
                    repeatMode = RepeatMode.REPEAT_DISABLED ;
                    Toast.makeText(activity, "Repeat Disabled",
                            Toast.LENGTH_LONG).show();
                    ((AllegroActivity)activity).serviceConnectionRepeat(AllegroActivity.RepeatState.NONE);
                }
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////

        pausePlayButton = (ImageSwitcher)root.findViewById(R.id.now_playing_play_button) ;
        pausePlayButton.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView img = new ImageView(activity.getApplicationContext());

                img.setScaleType(ImageView.ScaleType.FIT_CENTER);

                ImageSwitcher.LayoutParams params = new ImageSwitcher.LayoutParams(
                        ImageSwitcher.LayoutParams.MATCH_PARENT, ImageSwitcher.LayoutParams.MATCH_PARENT);

                img.setLayoutParams(params);

                return img;
            }
        });

        pausePlayButton.setImageResource(R.drawable.ic_pause_circle_outline_white_48dp);
        pausePlayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                pausePlayButton.setOutAnimation(Support.getInstance().FadeOutAnimation);
                pausePlayButton.setInAnimation(Support.getInstance().FadeInAnimation);

                if(PLAYING){
                    pausePlayButton.setImageResource(R.drawable.ic_play_circle_outline_white_48dp);
                    PLAYING = false ;
                    ((AllegroActivity)activity).serviceConnectionPause();
                } else {
                    pausePlayButton.setImageResource(R.drawable.ic_pause_circle_outline_white_48dp);
                    PLAYING = true ;
                    ((AllegroActivity)activity).serviceConnectionPlay();

                }
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        queueButtonFloat = (ButtonFloat)root.findViewById(R.id.play_queue_float_button) ;
        queueButtonFloat.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!SHOWING_QUEUE) {
                    showNowPlayingQueue() ;
                } else {
                    removeQueueFragment();
                }
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        lyricsAndSeekBarFrameLayout = (FrameLayout)root.findViewById(R.id.now_playing_album_art_frame_layout) ;
        lyricsAndSeekBarFrameLayout.setOnTouchListener(listener);

        this.seekBarFlipFragment = SeekBarFlipFragment.newInstance(this) ;
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction() ;
        //fragmentTransaction.setCustomAnimations(R.animator.fade_in_enter , R.animator.fade_out_exit) ;
        fragmentTransaction.add(R.id.now_playing_album_art_frame_layout, seekBarFlipFragment) ;
        fragmentTransaction.commit() ;

        SHOWING_LYRICS = false ;

        ////////////////////////////////////////////////////////////////////////////////////////////



        return root ;
    }

//    @Override
//    public void onViewCreated(View view, Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        AllegroActivity.MyServiceConnection2 sc = ((AllegroActivity) activity).getServiceConnection();
//        if (sc != null && sc.isAvailable() && sc.queueAvailable()) {
//            log("XXXXXXX requested update");
//            sc.requestUpdateDisplayers();
//        } else {
//            log("XXXXXXX request update failed "+sc.isAvailable()+" "+sc.queueAvailable());
//        }
//    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    void updateNowPlayingSong()  {


    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void flipCards() {

        if (SHOWING_LYRICS) {
            SHOWING_LYRICS = false ;
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fade_out_exit , R.anim.fade_out_exit)
                            //.setCustomAnimations(
                            //        R.anim.card_flip_right_in, R.anim.card_flip_right_out,
                            //        R.anim.card_flip_left_in, R.anim.card_flip_left_out)
                    .remove(lyricsFlipFragment)
                    .commit();
        }

        else {
            SHOWING_LYRICS = true ;

            if (this.lyricsFlipFragment == null) {
                this.lyricsFlipFragment = LyricsFlipFragment.newInstance(this);
            }

            activity
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fade_in_enter, R.anim.fade_out_exit)
                            //.setCustomAnimations(
                            //        R.anim.card_flip_right_in, R.anim.card_flip_right_out,
                            //        R.anim.card_flip_left_in, R.anim.card_flip_left_out)
                    .add(R.id.now_playing_album_art_frame_layout, this.lyricsFlipFragment)
                    .commit();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    class QueueGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false ;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                                float distanceY) {
            return false ;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true ;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            return true;
        }
    }

    /////// Handle Animations //////////////////////////////////////////////////////////////////////

    void AnimateFromPauseToPlay(boolean animate){
        if(animate) {
            pausePlayButton.setInAnimation(Support.getInstance().FadeInAnimation);
            pausePlayButton.setOutAnimation(Support.getInstance().FadeOutAnimation);
        }

        pausePlayButton.setImageResource(R.drawable.ic_play_circle_outline_white_48dp);
    }

    void AnimateFromPlayToPause(boolean animate){
        if(animate) {
            pausePlayButton.setInAnimation(Support.getInstance().FadeInAnimation);
            pausePlayButton.setOutAnimation(Support.getInstance().FadeOutAnimation);
        }

        pausePlayButton.setImageResource(R.drawable.ic_pause_circle_outline_white_48dp);
    }

    //////// NowPlaying Queue //////////////////////////////////////////////////////////////////////

    private void showNowPlayingQueue() {
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        if(nowPlayingQueueFragment == null) {
            nowPlayingQueueFragment = NowPlayingQueueFragment.newInstance(queueAdapter, this);
        }
        fragmentTransaction.setCustomAnimations(R.anim.fade_in_enter , R.anim.fade_out_exit) ;
        fragmentTransaction.add(R.id.now_playing_album_art_frame_layout, nowPlayingQueueFragment);
        fragmentTransaction.disallowAddToBackStack() ;
        fragmentTransaction.commit() ;
        SHOWING_QUEUE = true ;
    }

    public void removeQueueFragment() {
        if(SHOWING_QUEUE) {
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.anim.fade_in_enter , R.anim.fade_out_exit) ;
            fragmentTransaction.remove(nowPlayingQueueFragment);
            fragmentTransaction.disallowAddToBackStack();
            fragmentTransaction.commit();
            SHOWING_QUEUE = false ;
        }
    }

    public void playFromQueueRequested(int position) {
        ((AllegroActivity)getActivity()).serviceConnectionRequestTrack(position);
    }

    private void setNowPlayingData (int delta) {

        queuePosition += delta ;
        songNameTextView.setText(currentTrackDataList.get(queuePosition).getDisplayName());

        Log.e(DEBUG_TAG , currentTrackDataList.get(queuePosition).getDisplayName()) ;

        String tmp = currentTrackDataList.get(queuePosition).getArtist().getName() + " - " +
                currentTrackDataList.get(queuePosition).getAlbum().getTitle() ;

        Log.e(DEBUG_TAG, tmp) ;

        albumNameTextView.setText(tmp);

        tmp = Integer.toString(queuePosition+1) + "/" + Integer.toString(currentTrackDataList.size()) ;
        trackNumberTextView.setText(tmp);
        trackProgresTextView.setText(setTrackTiming(currentTrackDataList.get(queuePosition), true));

        if(delta == 0) {
            seekBarFlipFragment.setAlbumArt(currentTrackDataList.get(queuePosition).getAlbum().getAlbumArtPath());
        } else {
            if (!SHOWING_LYRICS && !SHOWING_QUEUE) {
                if (delta > 0) {
                    seekBarFlipFragment.albumArtSwipeRight(currentTrackDataList.get(queuePosition).getAlbum().getAlbumArtPath());
                } else {
                    seekBarFlipFragment.albumArtSwipeLeft(currentTrackDataList.get(queuePosition).getAlbum().getAlbumArtPath());
                }
            }
        }

        songSeconds = (int)currentTrackDataList.get(queuePosition).getDuration() / 1000 ;
        seekBarFlipFragment.resetSeekBar(songSeconds) ;

        synchronized (this) {
            while (!seekBarThreadTerminated) {
                terminateSeekBarThread = true;
            }
        }
        currentProgress = -1 ;
        startProgress();

        updateWidgetInterface();

    }

    private String setTrackTiming(TrackData track , boolean start){
        long total = track.getDuration() ;
        total /= 1000 ;
        long minutes = total / (60) ;
        long seconds = total % (60) ;
        String sec = "" ;
        String min = Long.toString(minutes) ;
        if(seconds < 10){
            sec += "0" ;
        }
        sec += Long.toString(seconds) ;
        String res = "00:00 / " + min + ":" + sec ;
        totalTrackTime = min + ":" + sec ;
        return res ;
    }

    public void updateTrackProgress(int time){
        int minutes = time / (60) ;
        int seconds = time % (60) ;
        String curTime = Integer.toString(minutes) ;
        if(seconds < 10){
            curTime += ( ":0" + Integer.toString(seconds)) ;
        } else {
            curTime += ( ":" + Integer.toString(seconds)) ;
        }
        trackProgresTextView.setText(curTime + "/" + totalTrackTime) ;
    }

    // ============================================================================================ \\

    public void delayedLoadQueue(){

        log("XXXXXXXXXXXXX delayedLoadQueue() called <<<<<<<<<<<< ");

        queueAdapter = new NowPlayingQueueAdapter(activity , currentTrackDataList) ;
        setNowPlayingData(0);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(activity);
        RemoteViews remoteViews = new RemoteViews(activity.getPackageName(), R.layout.widget_small_layout);
        ComponentName widget = new ComponentName(activity , PlayerControlWidgetProvider.class);
        remoteViews.setImageViewResource(R.id.widget_play_button, R.drawable.ic_pause_circle_filled_white_48dp);
        appWidgetManager.updateAppWidget(widget, remoteViews);

        postponeInitialization = false ;

    }

    @Override
    public void loadQueue(List<TrackData> trackList, int initialPosition) {
        currentTrackDataList = trackList ;

        if(postponeInitialization){
            postponeInitialization = false ;
        }

        if (activity != null) {

            log("XXXXXXX >>>>>>>>>>> in.");

            queueAdapter = new NowPlayingQueueAdapter(activity , trackList) ;
            queuePosition = initialPosition ;
            setNowPlayingData(0);

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(activity);
            RemoteViews remoteViews = new RemoteViews(activity.getPackageName(), R.layout.widget_small_layout);
            ComponentName widget = new ComponentName(activity , PlayerControlWidgetProvider.class);
            remoteViews.setImageViewResource(R.id.widget_play_button, R.drawable.ic_pause_circle_filled_white_48dp);
            appWidgetManager.updateAppWidget(widget, remoteViews);

        } else {

            currentTrackDataList = trackList ;
            queuePosition = initialPosition ;
            postponeInitialization = true ;

        }

    }

    @Override
    public void updateQueue(List<TrackData> trackList, int newPosition) {
        queueAdapter.updateDataSet(trackList);
        queueAdapter.notifyDataSetChanged() ;
    }

    @Override
    public void pause() {
        PLAYING = false ;
        AnimateFromPauseToPlay(true);

        /*AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(activity);
        RemoteViews remoteViews = new RemoteViews(activity.getPackageName(), R.layout.widget_small_layout);
        ComponentName widget = new ComponentName(activity , PlayerControlWidgetProvider.class);
        remoteViews.setImageViewResource(R.id.widget_play_button, R.drawable.ic_play_circle_filled_white_48dp);
        appWidgetManager.updateAppWidget(widget, remoteViews); */

    }

    @Override
    public void play() {
        PLAYING = true ;
        AnimateFromPlayToPause(true);

        /*AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(activity);
        RemoteViews remoteViews = new RemoteViews(activity.getPackageName(), R.layout.widget_small_layout);
        ComponentName widget = new ComponentName(activity , PlayerControlWidgetProvider.class);
        remoteViews.setImageViewResource(R.id.widget_play_button, R.drawable.ic_pause_circle_filled_white_48dp);
        appWidgetManager.updateAppWidget(widget, remoteViews); */
    }

    @Override
    public void next() {
        Log.e(DEBUG_TAG, "Next Called") ;
        setNowPlayingData(1);
    }

    @Override
    public void previous() {
        setNowPlayingData(-1);
    }

    @Override
    public void goToTrack(int index) {
        int delta = (index - queuePosition) ;
        setNowPlayingData(delta);
    }

    @Override
    public void seekToProgress(long progress) {
        int p = (int)(progress / 1000) ;
        currentProgress = p ;
    }

    //////////////////////////////////////////////////////////////////////////

    public void updateWidgetInterface() {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(activity);
        RemoteViews remoteViews = new RemoteViews(activity.getPackageName(), R.layout.widget_small_layout);
        ComponentName widget = new ComponentName(activity , PlayerControlWidgetProvider.class);

        String tmp = currentTrackDataList.get(queuePosition).getDisplayName() ;

        remoteViews.setTextViewText(R.id.widget_track_name_text_view , tmp) ;

        tmp = currentTrackDataList.get(queuePosition).getArtist().getName() + " - " +
                currentTrackDataList.get(queuePosition).getAlbum().getTitle() ;

        remoteViews.setTextViewText(R.id.widget_artist_name_text_view , tmp) ;

        String path = currentTrackDataList.get(queuePosition).getAlbum().getAlbumArtPath() ;
        if(path != null){
            Bitmap b = BitmapFactory.decodeFile(path) ;
            if(b != null){
                remoteViews.setImageViewBitmap(R.id.widget_album_art_image_view , b);
            } else {
                remoteViews.setImageViewResource(R.id.widget_album_art_image_view , R.drawable.place_holder_cover);
            }
        } else {
            remoteViews.setImageViewResource(R.id.widget_album_art_image_view , R.drawable.place_holder_cover);
        }

        appWidgetManager.updateAppWidget(widget, remoteViews);
    }

    @Override
    public void updateUI(EargasmService.EargasmState state, float progress, TrackData track) {
    }

    public void startProgress(){
        terminateSeekBarThread = false ;
        sleepTime = 1000 ;
        progressThread = new Thread(new SeekBarRunnable(num_threads++)) ;
        progressThread.start();
        seekBarThreadTerminated = false ;
    }

    private void pauseProgress(){

    }

    private void resumeProgress(){

    }

    class SeekBarRunnable implements Runnable {

        private int number ;

        public SeekBarRunnable(int count){
            number = count ;
        }

        @Override
        public void run() {
            while (!terminateSeekBarThread && currentProgress < songSeconds) {
                if (PLAYING) {
                    currentProgress++;
                    prevSeekTime = System.currentTimeMillis();
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        //Log.e(DEBUG_TAG, "Thread Exception");
                    }
                    currentSeekTime = System.currentTimeMillis();
                    sleepTime = 1000 - ((currentSeekTime - prevSeekTime) - 1000);
                    progressHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(!userSeeking) {
                                seekBarFlipFragment.getSeekbar().setProgress(currentProgress);
                            }
                            updateTrackProgress(currentProgress);
                            //Log.e(DEBUG_TAG, "Thread " + Integer.toString(number) + " set progress to" +
                                    //currentProgress) ;
                        }
                    });
                }
            }
            seekBarThreadTerminated = true;
            //Log.e(DEBUG_TAG, "Thread " + Integer.toString(number) + "terminated") ;
        }
    }

    public void updatePlaylistQueue(int from , int to){

        int newPosition = queuePosition ;

        if((from < queuePosition && to < queuePosition) ||
                (from > queuePosition && to > queuePosition)){
            newPosition = queuePosition ;
        }

        else if(from < queuePosition && to > queuePosition){
            newPosition = queuePosition - 1 ;
        }

        else if(from > queuePosition && to < queuePosition){
            newPosition = queuePosition + 1 ;
        }

        else if(from == queuePosition){
            newPosition = to ;
        }

        else if(to == queuePosition){
            newPosition = queuePosition + 1 ;
        }

        ((AllegroActivity)activity).serviceConnectionRequestQueueUpdate(newPosition , currentTrackDataList);

    }

    public void startUserSeek(){
        userSeeking = true ;
    }

    public void stopUserSeek(int p){
        userSeeking = false ;
        currentProgress = p ;
        ((AllegroActivity)getActivity()).serviceConnectionRequestSeek(currentProgress);
    }

}
