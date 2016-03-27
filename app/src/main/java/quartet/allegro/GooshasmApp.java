package quartet.allegro;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;

import quartet.allegro.R;
import com.orm.SugarApp;

import quartet.allegro.ui.FontsOverride;

/**
 * Created by akbar on 5/28/15.
 */
public class GooshasmApp extends SugarApp {

    private boolean activityIn;

    private Typeface iranSansTypeface;
    private Bitmap placeHolderBitmap;

    private void loadIranSans(){
        Typeface font = Typeface.createFromAsset(getAssets(), "irsans.ttf");
        this.iranSansTypeface = font;
    }

    public Bitmap getPlaceHolder(){
        return placeHolderBitmap;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        AllegroActivity.log("{{{{{{{{{{{{{{{ Application onCreate }}}}}}}}}}}}}}}}}}");
        FontsOverride.setDefaultFont(this, "DEFAULT", "irsans.ttf");
        FontsOverride.setDefaultFont(this, "MONOSPACE", "irsans.ttf");
        FontsOverride.setDefaultFont(this, "SERIF", "irsans.ttf");
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "irsans.ttf");

        placeHolderBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.place_holder_cover);

        loadIranSans();
    }

    public Typeface getIranSans(){
        return iranSansTypeface;
    }

    protected void notifyActivityIn(){
        activityIn = true;
    }

    protected void notifyActivityOut(){
        activityIn = false;
    }

    public boolean isActivityIn(){
        return activityIn;
    }


}
