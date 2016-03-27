package quartet.allegro.ui.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import quartet.allegro.bone.EargasmService;

public class WidgetBroadcastReceiver extends BroadcastReceiver {

    public static EargasmService serviceInstance;

    private static final String DEBUT_TAG = "Widget Receiver" ;

    public WidgetBroadcastReceiver(){
        //Log.e(DEBUT_TAG, "Registered") ;
    }

    @Override
    public IBinder peekService(Context myContext, Intent service) {
        return super.peekService(myContext, service);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String request = intent.getStringExtra("quartet.allegro.widget.PLAYER_ACTION") ;

        Log.e(DEBUT_TAG , "Received " + request) ;

        if(serviceInstance != null) {
            switch (request) {

                case "UPDATE" :
                    serviceInstance.initialAppWidgetInterfaceUpdate();
                    break ;

                case "SHUFFLE" :
                    break ;

                case "PREV" :
                    serviceInstance.requestPrevious();
                    break ;

                case "PLAY" :
                    Log.e(DEBUT_TAG , "1") ;
                    serviceInstance.iteratePlayState();
                    break ;

                case "NEXT" :
                    serviceInstance.requestNext();
                    break ;

                case "REPEAT" :
                    serviceInstance.iterateRepeatState();
                    break ;
            }
        } else {
            Log.e(DEBUT_TAG , "Service is NULL") ;
        }
    }
}
