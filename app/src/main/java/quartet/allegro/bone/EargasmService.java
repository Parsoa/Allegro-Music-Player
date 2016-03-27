package quartet.allegro.bone;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.MediaController;
import android.widget.RemoteViews;

import quartet.allegro.AllegroActivity;
import quartet.allegro.R;
import quartet.allegro.database.AlbumData;
import quartet.allegro.database.ArtistData;
import quartet.allegro.database.TrackData;
import quartet.allegro.database.Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import quartet.allegro.communication.SongDisplayer;
import quartet.allegro.ui.widget.PlayerControlWidgetProvider;
import quartet.allegro.ui.widget.WidgetBroadcastReceiver;

import static quartet.allegro.AllegroActivity.log ;

public class EargasmService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener {

    public static String KEY_CALLBACK_LISTENER = "KEY_CALLBACK_LISTENER";

    public static String PREFERENCES_PREVIOUS_PROGRESS = "PREFERENCES_PREVIOUS_PROGRESS";
    public static String PREFERENCES_PREVIOUS_TRACK_ID = "PREFERENCES_PREVIOUS_TRACK_ID";
    public static String SHARED_PREFERENCES = "GOOSHASM_SERVICE";

    public static String ACTION_MUSIC_CONTROL = "quartet.allegro.bone.EargasmService";
    public static String EXTRA_REQUEST_CODE = "EXTRA_REQUEST_CODE" ;

    private static final String PREF_NAME = "ALLEGRO_PREFS" ;
    private static final String WIDGET_STATE = "ALLEGRO_WIDGET_RECEIVER" ;

    public static final int REQUEST_CODE_PAUSE = 1001 ;
    public static final int REQUEST_CODE_PLAY = 1002 ;
    public static final int REQUEST_CODE_NEXT = 1003 ;
    public static final int REQUEST_CODE_PREVIOUS = 1004 ;
    public static final int REQUEST_CODE_CLOSE = 1005 ;

    private static final String DEBUG_TAG = "EARGASM SERVICE" ;

    // ==================================================================== //

    protected static final int NOTIFICATION_ID = 2049100547;

    EargasmBinder binder;

    ArrayList<MusicPlayerUI> callbackListeners;

    MediaPlayer mediaPlayer;

    TrackData currentTrack;

    EargasmState state;

    Executor serviceThread;

    GooshasmNotificationHolder notificationHolder;

    WidgetBroadcastReceiver widgetBroadcastReceiver ;
    SharedPreferences sharedpreferences;

    private boolean isVirgin = true ;

    // ==================================================================== //

    private void __init__(){

        binder = new EargasmBinder();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        this.callbackListeners = new ArrayList<MusicPlayerUI>();
        state = EargasmState.STOPPED;
        serviceThread = Executors.newSingleThreadExecutor();

        registerWidgetReceiver();
    }

    public EargasmService() {
    }

    // ===================== Simple Callbacks ============================= //

    public void registerCallbackListener(MusicPlayerUI listener){
        if (!callbackListeners.contains(listener)) {
            callbackListeners.add(listener);
        }
    }

    public boolean unregisterCallbackListener(MusicPlayerUI listener){
        return callbackListeners.remove(listener);
    }

    // ==================== Full Size Player Callbacks ==================== //

    enum EventCode {
        PAUSE, PLAY, NEXT, PREVIOUS, NEW_QUEUE, UPDATED_QUEUE, SEEK
    }

    ArrayList<SongDisplayer> songDisplayers = new ArrayList<>();

    private void broadcastToDisplayers(EventCode eventCode) {
        for (SongDisplayer sd: songDisplayers) {
            switch (eventCode) {
                case PLAY:
                    sd.play();
                    break;
                case PAUSE:
                    sd.pause();
                    break;
                case NEXT:
                    sd.next();
                    break;
                case PREVIOUS:
                    sd.previous();
                    break;
                case UPDATED_QUEUE:
                    sd.updateQueue(currentQueue, currentPosition);
                    break;
                case NEW_QUEUE:
                    sd.loadQueue(currentQueue, currentPosition);
                    break;
                case SEEK:
                    sd.seekToProgress(currentProgress());
                    break;
            }
        }

        initialAppWidgetInterfaceUpdate();
    }

    public boolean queueAvailable(){
        return currentQueue != null;
    }

    public void requestUpdateDisplayers() {
        // TODO make it safe
        broadcastToDisplayers(EventCode.NEW_QUEUE);
        if (mediaPlayer.isPlaying()) {
            broadcastToDisplayers(EventCode.PLAY);
        } else {
            broadcastToDisplayers(EventCode.PAUSE);
        }
        broadcastToDisplayers(EventCode.SEEK);
    }

    public void registerSongDisplayer(SongDisplayer displayer){
        if (!songDisplayers.contains(displayer)){
            songDisplayers.add(displayer);
        }
    }

    public boolean unregisterSongDisplayer(SongDisplayer displayer){
        return songDisplayers.remove(displayer);
    }


    // ========================== Queue Control =========================== //

    List<TrackData> currentQueue;
    int currentPosition;
    AllegroActivity.RepeatState repeat = AllegroActivity.RepeatState.NONE;

    private void loadNewQueue(List<TrackData> newQueue, int initialPosition, boolean play) {

        if (initialPosition >= newQueue.size()){
            throw new IllegalArgumentException("Load queue initial position not in queue");
        }

        this.currentQueue = newQueue;
        this.currentPosition = initialPosition;

        if (shuffled) {
            shuffleSongs();
        }

        broadcastToDisplayers(EventCode.NEW_QUEUE);

        loadTrack(currentQueue.get(currentPosition), play);
    }

    public void gotoTrack(int trackNum){
        currentPosition = trackNum;
        broadcastToDisplayers(EventCode.NEW_QUEUE); // TODO maybe it should be update queue, huh?
        loadTrack(currentQueue.get(trackNum), true);
    }

    private void _playNext(){

        broadcastToDisplayers(EventCode.NEXT);

        if (currentPosition == currentQueue.size() - 1){
            currentPosition = 0;
            loadTrack(currentQueue.get(currentPosition), repeat == AllegroActivity.RepeatState.QUEUE);
        } else {
            currentPosition ++;
            loadTrack(currentQueue.get(currentPosition), true);
        }
    }

    private void _playPrevious(){

        broadcastToDisplayers(EventCode.PREVIOUS);

        if (currentPosition == 0){
            currentPosition = 0;
            loadTrack(currentQueue.get(currentPosition), true);
        } else {
            currentPosition --;
            loadTrack(currentQueue.get(currentPosition), true);
        }
    }

    // ==================================================================== //


    private void loadTrack(final TrackData track, boolean play){

        isVirgin = false;

        resetPlayer();
        this.currentTrack = track;
        this.currentTrack.playNext = play;

        try {
            mediaPlayer.setDataSource(track.getPath());
            mediaPlayer.prepareAsync();
            initialAppWidgetInterfaceUpdate();
        } catch (IOException e) {
            e.printStackTrace();
            state = EargasmState.ERROR;
            broadcastState();
        }

    }

    private void replayTrack(){
        loadTrack(currentTrack, true);
    }

    private void resetPlayer(){

        mediaPlayer.reset();

    }

    private void disposePlayer(){

        mediaPlayer.release();
        mediaPlayer = null;

    }

    private long currentProgress(){

        int duration = mediaPlayer.getDuration();
        if (duration == 0)
            return 0;
        return mediaPlayer.getCurrentPosition();

    }

    // ==================================================================== //

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        state = EargasmState.ERROR;
        broadcastState();

        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (currentQueue != null && !currentQueue.isEmpty() && (currentTrack.getDuration()==0 || mp.getCurrentPosition()!=0)){

            log("YYYY current position", mp.getCurrentPosition(), "duration:", currentTrack.getDuration());
            log("YYYYY onComplete with current track = ", currentTrack.getDisplayName());

            if (repeat == AllegroActivity.RepeatState.CURRENT) {
                mediaPlayer.seekTo(0);
            } else {
                _playNext();
            }

            broadcastState();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        if (currentTrack.playNext){
            mediaPlayer.start();
            state = EargasmState.PLAYING;
            broadcastToDisplayers(EventCode.PLAY);
            broadcastState();
        }

        log("XXXXX prepared track while currentTrack =", currentTrack.getDisplayName());
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        broadcastToDisplayers(EventCode.SEEK);
    }


    // ==================================================================== //

    private void broadcastState() {

        log("XXXX request broadcast");

        for (MusicPlayerUI ui:callbackListeners){
            log("XXXXX broadcasting state for a", ui.getClass().getName());
            ui.updateUI(state, getCurrentProgress(), currentTrack);
        }

        // TODO mohkam kari ye broadcast be displayer ha ham bokonim

    }

    // ==================================================================== //

    public void requestPlay(){
        if (state != EargasmState.PLAYING) {
            mediaPlayer.start();
            state = EargasmState.PLAYING;
            broadcastState();
            broadcastToDisplayers(EventCode.PLAY);
        }
    }

    public void requestPlay(List<TrackData> t, int initialPosition){
        if (t.size() == 0){
            requestPlay();
        } else {
            loadNewQueue(t, initialPosition, true);
        }
    }

    public void requestNext(){
        _playNext();
    }

    public void requestPrevious(){
        if (mediaPlayer.getCurrentPosition() > 2000){
            replayTrack();
            broadcastToDisplayers(EventCode.SEEK);
        } else {
            _playPrevious();
        }
    }

    public void requestReplayCurrentTrack(){
        mediaPlayer.seekTo(0);
    }

    public void updateQueue(List<TrackData> newOrder, int newPositionOfCurrent){
        currentQueue = newOrder;
        currentPosition = newPositionOfCurrent;

        broadcastToDisplayers(EventCode.UPDATED_QUEUE);
    }

    public void requestPause(){
        mediaPlayer.pause();
        state = EargasmState.PAUSED;
        broadcastState();
        broadcastToDisplayers(EventCode.PAUSE);
    }

    public void requestSeek(int progress){
        mediaPlayer.seekTo(progress * 1000);
    }

    public void requestBroadcast(){
        broadcastState();
    }

    public TrackData getCurrentTrack(){
        return currentTrack;
    }

    public float getCurrentProgress(){
        return currentProgress();
    }

    // ========================================================================================== \\

    public void notifyActivityIn(){
        stopForeground(true);
    }

    public void notifyActivityOut(){
        requestBroadcast();
    }

    // ========================================================================================== \\

    public boolean isVirgin(){
        return isVirgin;
    }

    public void setRepeat(AllegroActivity.RepeatState state){
        this.repeat = state;
    }

    public void iterateRepeatState(){
        if(this.repeat == AllegroActivity.RepeatState.NONE){
            this.repeat = AllegroActivity.RepeatState.QUEUE ;
        } else if(this.repeat == AllegroActivity.RepeatState.QUEUE){
            this.repeat = AllegroActivity.RepeatState.CURRENT ;
        } else if(this.repeat == AllegroActivity.RepeatState.CURRENT){
            this.repeat = AllegroActivity.RepeatState.NONE ;
        }
    }

    public void iteratePlayState(){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_small_layout);
        ComponentName widget = new ComponentName(getApplicationContext() , PlayerControlWidgetProvider.class);

        if(state == EargasmState.PLAYING){
            requestPause();
            Log.e(DEBUG_TAG, "2 - Play") ;
            remoteViews.setImageViewResource(R.id.widget_play_button, R.drawable.ic_play_circle_filled_white_48dp);
            appWidgetManager.updateAppWidget(widget, remoteViews);
        } else if(state == EargasmState.PAUSED) {
            requestPlay();
            Log.e(DEBUG_TAG, "2 - Pause") ;
            remoteViews.setImageViewResource(R.id.widget_play_button, R.drawable.ic_pause_circle_filled_white_48dp);
            appWidgetManager.updateAppWidget(widget, remoteViews);
        }
    }

    public void initialAppWidgetInterfaceUpdate(){
        Log.e(DEBUG_TAG, "AppWidget updated") ;

        if(!isVirgin()) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
            RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_small_layout);
            ComponentName widget = new ComponentName(getApplication() , PlayerControlWidgetProvider.class);

            String tmp = currentTrack.getDisplayName() ;

            remoteViews.setTextViewText(R.id.widget_track_name_text_view , tmp) ;

            tmp = currentTrack.getArtist().getName() + " - " +
                    currentTrack.getAlbum().getTitle() ;

            remoteViews.setTextViewText(R.id.widget_artist_name_text_view, tmp) ;

            String path = currentTrack.getAlbum().getAlbumArtPath() ;
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

            if (state == EargasmState.PLAYING) {
                remoteViews.setImageViewResource(R.id.widget_play_button, R.drawable.ic_pause_circle_filled_white_48dp);
            } else {
                remoteViews.setImageViewResource(R.id.widget_play_button, R.drawable.ic_play_circle_filled_white_48dp);
            }

            appWidgetManager.updateAppWidget(widget, remoteViews);
        }
    }

    public void revertWidgetInterface() {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_small_layout);
        ComponentName widget = new ComponentName(getApplication() , PlayerControlWidgetProvider.class);

        remoteViews.setTextViewText(R.id.widget_track_name_text_view , getResources().getString(R.string.widget_begin)) ;

        remoteViews.setTextViewText(R.id.widget_artist_name_text_view, "") ;

        remoteViews.setImageViewResource(R.id.widget_album_art_image_view , R.drawable.place_holder_cover);

        remoteViews.setImageViewResource(R.id.widget_play_button, R.drawable.ic_play_circle_filled_white_48dp);

        appWidgetManager.updateAppWidget(widget, remoteViews);
    }

    // =================== Shuffle Interface ============================== //

    private List<TrackData> preShuffle;
    private boolean shuffled = false;

    private void shuffleSongs() {

        int currentPos = currentPosition;
        TrackData current = currentQueue.get(currentPos);

        List<TrackData> shuffled = new ArrayList<>(currentQueue);
        shuffled.remove(currentPos);

        Random dice = new Random(System.currentTimeMillis());

        for (int i=shuffled.size()-1; i>=0; i--) {
            int rand = dice.nextInt(i);
            TrackData tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(rand));
            shuffled.set(rand, shuffled.get(i));
        }

        shuffled.add(currentPos, current);

        this.preShuffle = currentQueue;
        this.shuffled = true;
        this.currentQueue = shuffled;

    }

    public void requestShuffle(boolean shuffle) {

        if (shuffle == this.shuffled)
            return;

        shuffled = shuffle;

        if (!shuffle && preShuffle != null) {

            int position = 0;

            for (int i=0; i<preShuffle.size(); i++) {
                if (preShuffle.get(i).equals(currentTrack)) {
                    position = i;
                    break;
                }
            }

            currentPosition = position;
            currentQueue = preShuffle;
            shuffled = false;
            preShuffle = null;
        }

        if (shuffle) {
            shuffleSongs();
        }

        broadcastToDisplayers(EventCode.UPDATED_QUEUE);

    }

    // =================== Create & Destroy =============================== //

    @Override
    public void onCreate() {
        super.onCreate();

        __init__();
        MusicControlReceiver.serviceInstance = this ;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposePlayer();

        unregisterWidgetReceiver();
        Log.e(DEBUG_TAG , "Destroyed") ;

    }

    // ==================================================================== //

    @Override
    public IBinder onBind(Intent intent) {
        log("XXXXXXXX service bind XXXXXXXXXXXX");
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        notificationHolder = new GooshasmNotificationHolder(this, currentTrack, state);
        registerCallbackListener(notificationHolder);
        startForeground(NOTIFICATION_ID, notificationHolder.getNotification());

        return START_STICKY;
    }

    // ==================================================================== //

    public enum EargasmState {
        PLAYING, PAUSED, STOPPED, ERROR
    }

    // ==================================================================== //

    public class EargasmBinder extends Binder {

        public EargasmService getService(){
            return EargasmService.this;
        }
    }

    private void saveCurrentTrack(){

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);

                preferences.edit()
                        .putLong(PREFERENCES_PREVIOUS_TRACK_ID, currentTrack.getAudioId())
                        .putFloat(PREFERENCES_PREVIOUS_PROGRESS, currentProgress())
                        .apply();

                disposePlayer();

                return null;
            }
        }.execute();
    }

    private void loadCurrentTrack() {

        SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);

        final long id = preferences.getLong(PREFERENCES_PREVIOUS_TRACK_ID, -1);

        if (id != -1) {

            final float seek = preferences.getFloat(PREFERENCES_PREVIOUS_PROGRESS, 0);

            // TODO @Important the whole thing should be moved to datacenter

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {

                    List<Track> t = Track.find(Track.class, "audio_id=?", String.valueOf(id));
                    if (t.isEmpty()) {
                        return null;
                    }

                    currentTrack = new TrackData(t.get(0));
                    String url = currentTrack.getPath();

                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                    try {

                        mediaPlayer.setDataSource(url);
                        mediaPlayer.prepare();
                        mediaPlayer.seekTo((int) (seek * mediaPlayer.getDuration()));
                        mediaPlayer.pause();
                        state = EargasmState.PAUSED;
                        broadcastState();
                        broadcastToDisplayers(EventCode.PAUSE);

                    } catch (IOException e) {
                        e.printStackTrace();
                        state = EargasmState.ERROR;
                        broadcastState();
                    }

                    return null;
                }
            }.execute();
        }
    }

    public TrackData nowPlayingTrackData(){
        return currentTrack ;
    }

    // =========================== Widget ================================= \\

    /*private void storeWidgetReceiverState(boolean state){
        sharedpreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean(WIDGET_STATE, state) ;
        editor.commit() ;
    }*/

    /*private boolean getWidgetReceiverState(){
        //sharedpreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        //return sharedpreferences.getBoolean(WIDGET_STATE , false);
        return true ;
    }*/

    public void registerWidgetReceiver(){
        WidgetBroadcastReceiver.serviceInstance = this ;
        widgetBroadcastReceiver = new WidgetBroadcastReceiver();
        IntentFilter filter = new IntentFilter("quartet.allegro.widget");
        registerReceiver(widgetBroadcastReceiver, filter);
        if(!isVirgin()) {
            initialAppWidgetInterfaceUpdate();
        } else {
            revertWidgetInterface();
        }
        Log.e(DEBUG_TAG, "Widget Registered") ;
    }

    public void unregisterWidgetReceiver(){
        revertWidgetInterface();
        unregisterReceiver(widgetBroadcastReceiver);
        widgetBroadcastReceiver = null ;
        Log.e(DEBUG_TAG, "Widget Unregistered") ;
    }

    // ==================================================================== //

    public static class MusicControlReceiver extends BroadcastReceiver {

        public static EargasmService serviceInstance;

        public MusicControlReceiver(){

        }

        @Override
        public IBinder peekService(Context myContext, Intent service) {
            return super.peekService(myContext, service);
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            // TODO check for null service

            int request = intent.getExtras().getInt(EXTRA_REQUEST_CODE);

            switch (request) {

                case REQUEST_CODE_CLOSE:
                    log("XXXXXX close");
                    serviceInstance.stopForeground(true); /* TODO might need a better approach */
                    serviceInstance.stopSelf();
                    break;

                case REQUEST_CODE_PAUSE:
                    serviceInstance.requestPause();
                    break;

                case REQUEST_CODE_PLAY:
                    serviceInstance.requestPlay();
                    break;

                case REQUEST_CODE_NEXT:
                    serviceInstance.requestNext();
                    break;

                case REQUEST_CODE_PREVIOUS:
                    if (serviceInstance.mediaPlayer.getCurrentPosition() < 3000)
                        serviceInstance.requestPrevious();
                    else
                        serviceInstance.requestReplayCurrentTrack();
                    break;
            }

        }
    }

    // =========================================================================================== \\

    //TODO : show a toast when done , request it from activity

    public void setQueueToAlbum(AlbumData albumData){

    }

    public void setQueueToArtist(ArtistData artistData){

    }

    public void addAlbumToQueue(AlbumData albumData){

    }

    public void addArtistToQueue(ArtistData artistData){

    }

    public void addArtistToPlayList(ArtistData artistData){

    }

    public void addAlbumToPlayList(AlbumData albumData){

    }

    public void setNextTrackInQueue(TrackData trackData){

    }

    public void addSongToQueue(TrackData trackData){

    }

    // =========================================================================================== \\

    class EargasmController implements MediaController.MediaPlayerControl {

        @Override
        public void start() {
            mediaPlayer.start();
        }

        @Override
        public void pause() {
            mediaPlayer.pause();
        }

        @Override
        public int getDuration() {
            return mediaPlayer.getDuration();
        }

        @Override
        public int getCurrentPosition() {
            return mediaPlayer.getCurrentPosition();
        }

        @Override
        public void seekTo(int pos) {
            mediaPlayer.seekTo(pos);
        }

        @Override
        public boolean isPlaying() {
            return mediaPlayer.isPlaying();
        }

        @Override
        public int getBufferPercentage() {
            return 100;
        }

        @Override
        public boolean canPause() {
            return true;
        }

        @Override
        public boolean canSeekBackward() {
            return true;
        }

        @Override
        public boolean canSeekForward() {
            return true;
        }

        @Override
        public int getAudioSessionId() {
            return mediaPlayer.getAudioSessionId();
        }
    }

    protected EargasmController mediaController;


}
