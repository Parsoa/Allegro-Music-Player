package quartet.allegro.ui.widget ;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import quartet.allegro.AllegroActivity;
import quartet.allegro.bone.EargasmService ;

import quartet.allegro.R ;

public class PlayerControlWidgetProvider extends AppWidgetProvider {

    private static final String APP_STATE = "ALLEGRO_ACTIVITY_RUNNING" ;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        final int N = appWidgetIds.length;

        for(int i = 0 ; i < N ; i++) {
            int appWidgetId = appWidgetIds[i];

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_small_layout);

            Intent updateWidgetIntent = new Intent("quartet.allegro.widget") ;
            updateWidgetIntent.putExtra("quartet.allegro.widget.PLAYER_ACTION" , "UPDATE") ;
            context.sendBroadcast(updateWidgetIntent);
            // ================================================================================ \\

            Intent shuffleToggleIntent = new Intent("quartet.allegro.widget") ;
            shuffleToggleIntent.putExtra("quartet.allegro.widget.PLAYER_ACTION" , "SHUFFLE") ;
            PendingIntent shufflePendingIntent = PendingIntent.getBroadcast(context , 0 , shuffleToggleIntent , PendingIntent.FLAG_UPDATE_CURRENT) ;
            remoteViews.setOnClickPendingIntent(R.id.widget_shuffle_button, shufflePendingIntent);

            // ================================================================================ \\

            Intent rewindIntent = new Intent("quartet.allegro.widget") ;
            rewindIntent.putExtra("quartet.allegro.widget.PLAYER_ACTION" , "PREV") ;
            PendingIntent rewindPendingIntent = PendingIntent.getBroadcast(context , 1 , rewindIntent , PendingIntent.FLAG_UPDATE_CURRENT) ;
            remoteViews.setOnClickPendingIntent(R.id.widget_prev_button, rewindPendingIntent);

            // ================================================================================ \\

            Intent playIntent = new Intent("quartet.allegro.widget") ;
            playIntent.putExtra("quartet.allegro.widget.PLAYER_ACTION" , "PLAY") ;
            PendingIntent playPendingIntent = PendingIntent.getBroadcast(context , 2 , playIntent , PendingIntent.FLAG_UPDATE_CURRENT) ;
            remoteViews.setOnClickPendingIntent(R.id.widget_play_button , playPendingIntent);

            // ================================================================================ \\

            Intent forwardIntent = new Intent("quartet.allegro.widget") ;
            forwardIntent.putExtra("quartet.allegro.widget.PLAYER_ACTION", "NEXT") ;
            PendingIntent forwardPendingIntent = PendingIntent.getBroadcast(context , 3 , forwardIntent , PendingIntent.FLAG_UPDATE_CURRENT) ;
            remoteViews.setOnClickPendingIntent(R.id.widget_next_button , forwardPendingIntent);

            // ================================================================================ \\

            Intent repeatIntent = new Intent("quartet.allegro.widget") ;
            repeatIntent.putExtra("quartet.allegro.widget.PLAYER_ACTION" , "REPEAT") ;
            PendingIntent repeatPendingIntent = PendingIntent.getBroadcast(context , 4 , repeatIntent , PendingIntent.FLAG_UPDATE_CURRENT) ;
            remoteViews.setOnClickPendingIntent(R.id.widget_repeat_button , repeatPendingIntent);

            // ================================================================================ \\

            Intent allegroStartIntent = new Intent(context , AllegroActivity.class) ;
            PendingIntent allegroPendingIntent = PendingIntent.getActivity(context , 5 , allegroStartIntent , PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_album_art_image_view , allegroPendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.widget_artist_name_text_view , allegroPendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.widget_track_name_text_view , allegroPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

        }
    }
}
