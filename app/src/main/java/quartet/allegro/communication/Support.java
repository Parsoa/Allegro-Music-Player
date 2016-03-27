package quartet.allegro.communication;

import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import quartet.allegro.R;

public class Support {

    private Context context ;
    public static Support instance = null ;

    public static int[] ArtworkColors = new int[] {
            R.color.material_yellow_500 ,
            R.color.material_red_500 ,
            R.color.material_pink_500 ,
            R.color.material_purple_500 ,
            R.color.material_deep_purple_500 ,
            R.color.material_blue_500 ,
            R.color.material_indigo_500 ,
            R.color.material_cyan_500 ,
            R.color.material_teal_500 ,
            R.color.material_green_500 ,
            R.color.material_light_green_500 ,
            R.color.material_lime_500 ,
            R.color.material_orange_A400 ,
            R.color.material_pink_A200 ,
            R.color.material_purple_A200 ,
            R.color.material_deep_purple_A200 ,
            R.color.material_blue_A200 ,
            R.color.material_indigo_A200 ,
            R.color.material_cyan_A200 ,
            R.color.material_teal_A200 ,
            R.color.material_green_A200 ,
            R.color.material_light_green_A400 ,
            R.color.material_yellow_A200
    } ;

    public static void createInstance(Context c) {
        Support.instance = new Support(c) ;
    }

    public static Support getInstance() {
        return Support.instance ;
    }

    private Support(Context c) {
        this.context = c ;
        this.init();
    }

    public void init() {

        this.FadeInAnimation = AnimationUtils.loadAnimation(context , R.anim.fade_in_enter) ;
        this.FadeOutAnimation = AnimationUtils.loadAnimation(context , R.anim.fade_out_exit) ;
        this.FadeOutInAnimation = AnimationUtils.loadAnimation(context , R.anim.fade_out_in) ;

        /*this.AlbumCoverAnimationSet = (AnimatorSet)AnimatorInflater.loadAnimator(context, R.animator.cover_animation) ;
        this.AlbumCoverRightInAnimationSet = (AnimatorSet) AnimatorInflater.loadAnimator(context , R.animator.artwork_in_right) ;
        this.AlbumCoverRightOutAnimationSet = (AnimatorSet) AnimatorInflater.loadAnimator(context , R.animator.artwork_out_right) ;
        this.AlbumCoverLeftInAnimationSet = (AnimatorSet) AnimatorInflater.loadAnimator(context , R.animator.artwork_in_left) ;
        this.AlbumCoverLeftOutAnimationSet = (AnimatorSet) AnimatorInflater.loadAnimator(context , R.animator.artwork_out_left) ;*/

        this.AlbumCoverRightInAnimation = AnimationUtils.loadAnimation(context , R.anim.artwork_in_left) ;
        this.AlbumCoverRightOutAnimation = AnimationUtils.loadAnimation(context , R.anim.artwork_out_left) ;
        this.AlbumCoverLeftInAnimation = AnimationUtils.loadAnimation(context , R.anim.artwork_in_right) ;
        this.AlbumCoverLeftOutAnimation = AnimationUtils.loadAnimation(context , R.anim.artwork_out_right) ;
        this.AlbumCoverAnimation = AnimationUtils.loadAnimation(context , R.anim.cover_animation) ;

    }

    public Animation FadeInAnimation ;
    public Animation FadeOutAnimation ;
    public Animation AlbumCoverAnimation ;

    public Animation AlbumCoverRightInAnimation ;
    public Animation AlbumCoverRightOutAnimation ;
    public Animation AlbumCoverLeftInAnimation ;
    public Animation AlbumCoverLeftOutAnimation ;

    public Animation FadeOutInAnimation ;

    public static final String widgetShuffleKey = "Widget.SHUFFLE" ;
    public static final String widgetRepeatKey = "Widget.REPEAT" ;
    public static final String widgetPlayKey = "Widget.PLAY" ;
    public static final String widgetRewindKey = "Widget.REWIND" ;
    public static final String widgetForwardKey = "Widget.FORWARD" ;

    public AdapterMode adapterMode ;

    public enum AdapterMode {

        ARTIST_SONG_LIST ,
        ARTIST_ALBUM_LIST ,
        ALBUM_SONG_LIST ,
        ALBUM_INFO ,
        ARTIST_INFO

    }

}
