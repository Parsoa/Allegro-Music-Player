package quartet.allegro.bone;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

import quartet.allegro.PlayerState;
import quartet.allegro.database.Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static quartet.allegro.AllegroActivity.log;

public class GooshasmService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener {

    public static String KEY_CALLBACK_LISTENER = "KEY_CALLBACK_LISTENER";

    public static String PREFERENCES_PREVIOUS_PROGRESS = "PREFERENCES_PREVIOUS_PROGRESS";
    public static String PREFERENCES_PREVIOUS_TRACK_ID = "PREFERENCES_PREVIOUS_TRACK_ID";
    public static String SHARED_PREFERENCES = "GOOSHASM_SERVICE";

    GooshashmBinder binder;
    MediaPlayer mediaPlayer;

    private ArrayList<VintageListener> callbacks;

    Track currentTrack;

    boolean playerLoaded = false;

    // ==================== media playback =========================== //


    public void registerCallbackListener(VintageListener callback){
        if (!callbacks.contains(callback))
            callbacks.add(callback);
    }

    public void unregisterCallbackListener(VintageListener callback){
        callbacks.remove(callback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void loadTrack(Track track){

        log("XXXXXX load track called for track:", track.displayName);

        this.currentTrack = track;
        String path = track.path;

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {

            relaxPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepareAsync();
            playerLoaded = true;

        } catch (IOException e) {

            e.printStackTrace();

            // reporting error to all UI elements
            broadcastError(0);
        }

    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        if (mp == null) {
            broadcastError(0);
            return;
        }

        mediaPlayer.start();
        broadcastPlay();

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        broadcastError(0);
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        broadcastFinished();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

        if (this.isPlaying()) {
            broadcastPlay();
        } else {
            broadcastPause();
        }
    }

    private float currentProgress(){
        if (mediaPlayer.getDuration() != 0)
            return (float)mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration();
        else
            return 0;
    }

    // ===================== Event broadcast ========================= //

    private void broadcastError(float progress){

        log("broadcasting error");

        for (VintageListener callback:callbacks) {
            callback.updateUI(PlayerState.ERROR, progress, currentTrack);
        }
    }

    private void broadcastPlay(){

        log("broadcasting play");

        for (VintageListener callback:callbacks)
            callback.updateUI(PlayerState.PLAYING, currentProgress(), currentTrack);
    }

    private void broadcastPause(){

        log("broadcasting pause");

        for (VintageListener callback:callbacks)
            callback.updateUI(PlayerState.PAUSED, currentProgress(), currentTrack);
    }

    private void broadcastFinished(){

        log("broadcasting finished");

        for (VintageListener callback:callbacks)
            callback.updateUI(PlayerState.FINISHED, 0, currentTrack);
    }

    private void relaxPlayer(){
        if (playerLoaded) {
            log("XXXXXXX relaxing player");
            mediaPlayer.stop();
            mediaPlayer.reset();
            playerLoaded = false;
        }
    }

    // =================== mandatory overrides ======================= //

    @Override
    public void onCreate() {
        super.onCreate();

        final MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        this.mediaPlayer = mediaPlayer;

        this.callbacks = new ArrayList<VintageListener>();

        SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);

        final long id = preferences.getLong(PREFERENCES_PREVIOUS_TRACK_ID, -1);

        if (id != -1) {

            final float seek = preferences.getFloat(PREFERENCES_PREVIOUS_PROGRESS, 0);

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {

                    List<Track> t = Track.find(Track.class, "audio_id=?", String.valueOf(id));
                    if (t.isEmpty()){
                        return null;
                    }

                    currentTrack = t.get(0);
                    String url = currentTrack.path;

                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                    try {

                        mediaPlayer.setDataSource(url);
                        playerLoaded = true;
                        mediaPlayer.prepare();
                        mediaPlayer.seekTo((int) (seek * mediaPlayer.getDuration()));
                        mediaPlayer.pause();
                        broadcastPause();

                    } catch (IOException e) {
                        e.printStackTrace();
                        broadcastError(0);
                    }

                    return null;
                }
            }.execute();

        }

    }




    @Override
    public void onDestroy() {
        super.onDestroy();

        log("XXXXXXX gooshasm destroy");

        SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);

        preferences.edit()
                .putLong(PREFERENCES_PREVIOUS_TRACK_ID, currentTrack.audioId)
                .putFloat(PREFERENCES_PREVIOUS_PROGRESS, currentProgress())
                .apply();

        disposePlayer();

    }

    public void disposePlayer(){
        mediaPlayer.release();
        mediaPlayer = null;
    }

    public GooshasmService(){
        binder = new GooshashmBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {

        return binder;

    }

    // =================== Juke Box interface ======================== //

    public void requestPause(){

        mediaPlayer.pause();
        broadcastPause();
    }

    public void requestPlay() {
        if (playerLoaded) {
            mediaPlayer.start();
        }
    }

    public void requestSeek(float progress){

        mediaPlayer.seekTo((int) (progress * mediaPlayer.getDuration()));

    }

    public void requestBroadcast(){

        if (currentTrack == null) {
            log("xxxx track null");
            return;
        }

        log("xxx broadcasting");

        if (isPlaying()) {
            broadcastPlay();
        } else {
            broadcastPause();
        }
    }

    public Track getCurrentTrack(){
        return this.currentTrack;
    }

    // =============================================================== //

    public class GooshashmBinder extends Binder {

        public GooshasmService getService(){
            return GooshasmService.this;
        }

    }

    public boolean isPlaying(){
        return mediaPlayer.isPlaying();
    }

}
