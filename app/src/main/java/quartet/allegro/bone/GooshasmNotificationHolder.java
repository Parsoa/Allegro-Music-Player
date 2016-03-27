package quartet.allegro.bone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import quartet.allegro.GooshasmApp;
import quartet.allegro.AllegroActivity;
import quartet.allegro.R;
import quartet.allegro.database.TrackData;

import static quartet.allegro.AllegroActivity.log;

/**
* Created by akbar on 6/1/15.
*/
public class GooshasmNotificationHolder implements MusicPlayerUI {

    private Notification notification;
    private NotificationManager manager;
    private EargasmService context;

    private EargasmService.EargasmController mediaController;

    private TrackData latestTrack;

    private GooshasmApp app;

    public GooshasmNotificationHolder(EargasmService context, TrackData currentTrack, EargasmService.EargasmState state) {

        this.context = context;
        this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        this.mediaController = ((EargasmService)context).mediaController;

        app = (GooshasmApp) (context.getApplication());

        notification = _buildNew(currentTrack, state);

    }

    // ===================== Determining text and icon colors =================================== //

    private static int COLORS_LIGHT = 0;
    private static int COLORS_DARK = 1;
    private static int COLORS_UNKNOWN = 2;

    private Integer colorsState = null;

    private Integer notification_text_color = null;
    private Integer notification_hint_color = null;

    private float notification_text_size = 11;
    private final String COLOR_SEARCH_RECURSE_TIP = "SOME_SAMPLE_TEXT";

    private boolean recurseGroup(ViewGroup gp)
    {
        final int count = gp.getChildCount();
        for (int i = 0; i < count; ++i)
        {
            if (gp.getChildAt(i) instanceof TextView)
            {
                final TextView text = (TextView) gp.getChildAt(i);
                final String szText = text.getText().toString();
                if (COLOR_SEARCH_RECURSE_TIP.equals(szText))
                {
                    notification_text_color = text.getTextColors().getDefaultColor();
                    notification_text_size = text.getTextSize();
                    DisplayMetrics metrics = new DisplayMetrics();
                    WindowManager systemWM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
                    systemWM.getDefaultDisplay().getMetrics(metrics);
                    notification_text_size /= metrics.scaledDensity;
                    log("XXXXXXXXXX found default notification settings XXXXXXXXXX");
                    return true;
                }
            }
            else if (gp.getChildAt(i) instanceof ViewGroup){
                if (recurseGroup((ViewGroup) gp.getChildAt(i)))
                    return true;
            }
        }
        log("XXXXXXXXXX default notification settings not found");
        return false;
    }

    private void extractColors()
    {
        if (colorsState != null)
            return;

        boolean error = false;

        try
        {
            Notification ntf = new NotificationCompat.Builder(context).setSubText("salam").build();
            ntf.setLatestEventInfo(context, COLOR_SEARCH_RECURSE_TIP, "Utest", null);
            LinearLayout group = new LinearLayout(context);
            ViewGroup event = (ViewGroup) ntf.contentView.apply(context, group);
            if (!recurseGroup(event)){
                error = true;
            } else {
                group.removeAllViews();
            }
        }
        catch (Exception e) {
            log("Colors unknown");
            error = true;

        }

        if (error) {
            log("XXXXXXX error again");
            colorsState = COLORS_UNKNOWN;
            notification_text_color = context.getResources().getColor(R.color.notification_default_text_dark);
            notification_text_color = context.getResources().getColor(R.color.notification_default_hint_dark);
            return;
        }

        float[] hsvColor = new float[3];
        Color.RGBToHSV(Color.red(notification_text_color),
                Color.green(notification_text_color),
                Color.blue(notification_text_color), hsvColor
        );

        if (hsvColor[2] < 0.3){
            colorsState = COLORS_DARK;
            notification_hint_color = context.getResources().getColor(R.color.notification_default_hint_dark);
//            hsvColor[2] = 0.9f;
//            notification_hint_color = Color.HSVToColor(hsvColor);
        } else {
            colorsState = COLORS_LIGHT;
            notification_hint_color = context.getResources().getColor(R.color.notification_default_hint_light);
//            hsvColor[2] = 0.1f;
//            notification_hint_color = Color.HSVToColor(hsvColor);
        }
    }


    // ========================================================================================== //

    private Notification _buildNew(final TrackData currentTrack, EargasmService.EargasmState state){

        extractColors();

        this.latestTrack = currentTrack;

        int apiLevel = Build.VERSION.SDK_INT;
//        boolean lightIcons = apiLevel <= Build.VERSION_CODES.KITKAT;
        boolean lightIcons = (colorsState == COLORS_LIGHT);

        // Using RemoteViews to bind custom layouts into Notification
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.notification);

        // Set Notification Title
        String title = context.getString(R.string.notification_title);

        // Open NotificationView Class on Notification Click
        Intent intent = new Intent(context, AllegroActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // creating a new notification
        Notification newNotification = new Notification(R.drawable.ic_prism_light,
                context.getString(R.string.notification_ticker), System.currentTimeMillis());

        // redirecting clicks on notification itself, to activity
        newNotification.contentIntent = pIntent;

        // the artwork bitmap
        Bitmap artworkBitmap = null;

        // close button

        remoteViews.setInt(R.id.close_button, "setBackgroundResource",
                lightIcons ? R.drawable.ic_close_white : R.drawable.ic_close_black);

        Intent closeIntent = new Intent(EargasmService.ACTION_MUSIC_CONTROL);
        closeIntent.putExtra(EargasmService.EXTRA_REQUEST_CODE, EargasmService.REQUEST_CODE_CLOSE);
        closeIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent closePendingIntent = PendingIntent.getBroadcast(context, EargasmService.REQUEST_CODE_CLOSE, closeIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.close_button, closePendingIntent);

        if (currentTrack == null) {

            remoteViews.setImageViewBitmap(R.id.artwork_container, app.getPlaceHolder());
            remoteViews.setTextViewText(R.id.small_text, "");
            remoteViews.setTextViewText(R.id.big_text, context.getString(R.string.notification_default_text));
            remoteViews.setViewVisibility(R.id.play_pause_button, View.INVISIBLE);
            remoteViews.setViewVisibility(R.id.small_text, View.GONE);

        } else {

            remoteViews.setViewVisibility(R.id.play_pause_button, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.small_text, View.VISIBLE);

            if (state == EargasmService.EargasmState.PLAYING){

                remoteViews.setInt(R.id.play_pause_button, "setBackgroundResource", lightIcons ? R.drawable.ic_pause_white_36dp : R.drawable.ic_pause_black_36dp);

                Intent pauseIntent = new Intent(EargasmService.ACTION_MUSIC_CONTROL);
                pauseIntent.putExtra(EargasmService.EXTRA_REQUEST_CODE, EargasmService.REQUEST_CODE_PAUSE);
                pauseIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                PendingIntent pi = PendingIntent.getBroadcast(context, EargasmService.REQUEST_CODE_PAUSE, pauseIntent, 0);
                remoteViews.setOnClickPendingIntent(R.id.play_pause_button, pi);

            } else  {

                remoteViews.setInt(R.id.play_pause_button, "setBackgroundResource", lightIcons ? R.drawable.ic_play_arrow_white_36dp : R.drawable.ic_play_arrow_black_36dp);

                Intent playIntent = new Intent(EargasmService.ACTION_MUSIC_CONTROL);
                playIntent.putExtra(EargasmService.EXTRA_REQUEST_CODE, EargasmService.REQUEST_CODE_PLAY);
                playIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                PendingIntent pi = PendingIntent.getBroadcast(context, EargasmService.REQUEST_CODE_PLAY, playIntent, 0);
                remoteViews.setOnClickPendingIntent(R.id.play_pause_button, pi);

            }

            // text views

            remoteViews.setTextViewText(R.id.big_text, currentTrack.getDisplayName());
            remoteViews.setTextViewText(R.id.small_text, currentTrack.getArtist().getName().trim() + " | " + currentTrack.getAlbum().getTitle().trim());

            artworkBitmap = BitmapFactory.decodeFile(currentTrack.getAlbum().getAlbumArtPath());

            if (artworkBitmap == null){
                artworkBitmap = app.getPlaceHolder();
            }

            remoteViews.setImageViewBitmap(R.id.artwork_container, artworkBitmap);

        }

        remoteViews.setTextColor(R.id.big_text, notification_text_color);
        remoteViews.setTextColor(R.id.small_text, notification_hint_color);

        if (colorsState == COLORS_UNKNOWN) {
            remoteViews.setInt(R.id.notif_root, "setBackgroundColor", android.R.color.white);
        }

        // ------------------------- large notification ------------------------- //

        // Using RemoteViews to bind custom layouts into Notification
        final RemoteViews remoteViewsLarge = new RemoteViews(context.getPackageName(),
                R.layout.notification_large);

        // close button

        remoteViewsLarge.setInt(R.id.close_button, "setBackgroundResource",
                lightIcons ? R.drawable.ic_close_white : R.drawable.ic_close_black);

        remoteViewsLarge.setOnClickPendingIntent(R.id.close_button, closePendingIntent);

        if (currentTrack == null) {

            remoteViewsLarge.setImageViewBitmap(R.id.artwork_container, app.getPlaceHolder());
            remoteViewsLarge.setTextViewText(R.id.small_text, "");
            remoteViewsLarge.setTextViewText(R.id.big_text, context.getString(R.string.notification_default_text));
            remoteViewsLarge.setViewVisibility(R.id.play_pause_button, View.INVISIBLE);
            remoteViewsLarge.setViewVisibility(R.id.next_button, View.INVISIBLE);
            remoteViewsLarge.setViewVisibility(R.id.previous_button, View.INVISIBLE);
            remoteViewsLarge.setViewVisibility(R.id.small_text, View.GONE);

        } else {

            remoteViewsLarge.setViewVisibility(R.id.play_pause_button, View.VISIBLE);
            remoteViewsLarge.setViewVisibility(R.id.next_button, View.VISIBLE);
            remoteViewsLarge.setViewVisibility(R.id.previous_button, View.VISIBLE);
            remoteViewsLarge.setViewVisibility(R.id.small_text, View.VISIBLE);

            // pause button

            if (state == EargasmService.EargasmState.PLAYING){

                remoteViewsLarge.setInt(R.id.play_pause_button, "setBackgroundResource",
                        lightIcons ? R.drawable.ic_pause_white_48dp : R.drawable.ic_pause_black_48dp);

                Intent pauseIntent = new Intent(EargasmService.ACTION_MUSIC_CONTROL);
                pauseIntent.putExtra(EargasmService.EXTRA_REQUEST_CODE, EargasmService.REQUEST_CODE_PAUSE);
                pauseIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                PendingIntent pi = PendingIntent.getBroadcast(context, EargasmService.REQUEST_CODE_PAUSE, pauseIntent, 0);
                remoteViewsLarge.setOnClickPendingIntent(R.id.play_pause_button, pi);

            } else  {

                remoteViewsLarge.setInt(R.id.play_pause_button, "setBackgroundResource",
                        lightIcons ? R.drawable.ic_play_arrow_white_48dp : R.drawable.ic_play_arrow_black_48dp);

                Intent playIntent = new Intent(EargasmService.ACTION_MUSIC_CONTROL);
                playIntent.putExtra(EargasmService.EXTRA_REQUEST_CODE, EargasmService.REQUEST_CODE_PLAY);
                playIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                PendingIntent pi = PendingIntent.getBroadcast(context, EargasmService.REQUEST_CODE_PLAY, playIntent, 0);
                remoteViewsLarge.setOnClickPendingIntent(R.id.play_pause_button, pi);

            }


            // next button

            remoteViewsLarge.setInt(R.id.next_button, "setBackgroundResource",
                    lightIcons ? R.drawable.ic_skip_next_white_48dp : R.drawable.ic_skip_next_black_48dp);

            Intent nextIntent = new Intent(EargasmService.ACTION_MUSIC_CONTROL);
            nextIntent.putExtra(EargasmService.EXTRA_REQUEST_CODE, EargasmService.REQUEST_CODE_NEXT);
            nextIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pi = PendingIntent.getBroadcast(context, EargasmService.REQUEST_CODE_NEXT, nextIntent, 0);
            remoteViewsLarge.setOnClickPendingIntent(R.id.next_button, pi);

            // previous button

            remoteViewsLarge.setInt(R.id.previous_button, "setBackgroundResource",
                    lightIcons ? R.drawable.ic_skip_previous_white_48dp : R.drawable.ic_skip_previous_black_48dp);

            Intent previousIntent = new Intent(EargasmService.ACTION_MUSIC_CONTROL);
            previousIntent.putExtra(EargasmService.EXTRA_REQUEST_CODE, EargasmService.REQUEST_CODE_PREVIOUS);
            previousIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pi2 = PendingIntent.getBroadcast(context, EargasmService.REQUEST_CODE_PREVIOUS, previousIntent, 0);
            remoteViewsLarge.setOnClickPendingIntent(R.id.previous_button, pi2);

            // setting texts

            remoteViewsLarge.setTextViewText(R.id.big_text, currentTrack.getDisplayName());
            remoteViewsLarge.setTextViewText(R.id.small_text,
                    currentTrack.getArtist().getName().trim() + " | " + currentTrack.getAlbum().getTitle().trim());

            // setting artwork

            artworkBitmap = BitmapFactory.decodeFile(currentTrack.getAlbum().getAlbumArtPath());

            if (artworkBitmap == null){
                artworkBitmap = app.getPlaceHolder();
            }

            remoteViewsLarge.setImageViewBitmap(R.id.artwork_container, artworkBitmap);

        }

        remoteViewsLarge.setTextColor(R.id.big_text, notification_text_color);
        remoteViewsLarge.setTextColor(R.id.small_text, notification_hint_color);

        if (colorsState == COLORS_UNKNOWN) {
            remoteViewsLarge.setInt(R.id.notif_root, "setBackgroundColor", android.R.color.white);
        }

        // ---------------------------------------------------------------------- //

        // setting two-state or one-state notification according to device's
        // API level
        if (apiLevel >= 16){
            newNotification.contentView = remoteViews;
            newNotification.bigContentView = remoteViewsLarge;
        } else {
            newNotification.contentView = remoteViewsLarge;
        }

        if (apiLevel >= 21) {
            newNotification.visibility = NotificationCompat.VISIBILITY_PUBLIC;
        }

        // ---------------------------------------------------------------------- //

        return newNotification;

    }

    private Notification emptyNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_prism_light);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return builder.build();
    }

    public Notification getNotification() {

        if (app.isActivityIn()) {
            return emptyNotification();
        }

        return notification;
    }

    private Executor notificationThreadPool = Executors.newSingleThreadExecutor();

    @Override
    public void updateUI(final EargasmService.EargasmState state, final float progress, final TrackData track) {

        if (!app.isActivityIn()) {
            // TODO do this in separate thread for artwork load might lock UI thread
            notificationThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    GooshasmNotificationHolder.this.notification = _buildNew(track, state);
                    manager.notify(EargasmService.NOTIFICATION_ID, GooshasmNotificationHolder.this.notification);
                }
            });
        }

    }

    public void pullNotification() {
        manager.cancel(EargasmService.NOTIFICATION_ID);
    }

    // ========================================================================================== //


}
