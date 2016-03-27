package quartet.allegro.ui.fragment;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.devadvance.circularseekbar.CircularSeekBar;

import quartet.allegro.AllegroActivity;
import quartet.allegro.R;

import quartet.allegro.communication.Support;

public class SeekBarFlipFragment extends Fragment {

    private static final int SWIPE_THRESHOLD = 100 ;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100 ;

    private static final String DEBUG_TAG = "Seekbar" ;

    public NowPlayingFragment parentFragment ;
    private ImageSwitcher albumArtSwitcher ;
    private CircularSeekBar seekBar ;

    private int songSeconds ;

    private View.OnTouchListener listener ;
    private GestureDetectorCompat gestureDetector ;

    public SeekBarFlipFragment(){}

    public static SeekBarFlipFragment newInstance(NowPlayingFragment p){
        SeekBarFlipFragment frag = new SeekBarFlipFragment() ;
        frag.parentFragment = p ;
        return frag ;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        gestureDetector = new GestureDetectorCompat(activity , new LyricsGestureDetector()) ;
        listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent) ;
            }
        } ;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View root = inflater.inflate(R.layout.seek_bar_flip_fragment, container, false);

        if (savedInstanceState != null) {

        }

        albumArtSwitcher = (ImageSwitcher)root.findViewById(R.id.now_playing_album_art) ;
        albumArtSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView img = new ImageView(getActivity());

                ImageSwitcher.LayoutParams params = new ImageSwitcher.LayoutParams(
                        ImageSwitcher.LayoutParams.MATCH_PARENT, ImageSwitcher.LayoutParams.MATCH_PARENT);

                img.setScaleType(ImageView.ScaleType.CENTER_CROP);

                img.setLayoutParams(params);
                return img;
            }
        });

        seekBar = (CircularSeekBar)root.findViewById(R.id.circular_seek_bar) ;

        seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
                parentFragment.stopUserSeek(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {
                parentFragment.startUserSeek();
            }
        });

        albumArtSwitcher.setImageResource(R.drawable.place_holder_cover);

        /*albumArtSwitcher.startAnimation(Support.getInstance().AlbumCoverAnimation);
        Support.getInstance().AlbumCoverAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Support.getInstance().AlbumCoverAnimation.setAnimationListener(this);
                albumArtSwitcher.startAnimation(Support.getInstance().AlbumCoverAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        }); */

        albumArtSwitcher.setOnTouchListener(listener);

        return root;
    }

    void albumArtSwipeRight(String path) {

        Drawable dr = Drawable.createFromPath(path) ;
        if(dr != null) {
            albumArtSwitcher.setInAnimation(Support.getInstance().AlbumCoverRightInAnimation);
            albumArtSwitcher.setOutAnimation(Support.getInstance().AlbumCoverRightOutAnimation);
            albumArtSwitcher.setImageDrawable(dr);
        } else {
            albumArtSwitcher.setInAnimation(Support.getInstance().AlbumCoverRightInAnimation);
            albumArtSwitcher.setOutAnimation(Support.getInstance().AlbumCoverRightOutAnimation);
            albumArtSwitcher.setImageResource(R.drawable.place_holder_cover);
        }

    }

    void albumArtSwipeLeft(String path) {

        Drawable dr = Drawable.createFromPath(path) ;
        if(dr != null) {
            albumArtSwitcher.setInAnimation(Support.getInstance().AlbumCoverLeftInAnimation);
            albumArtSwitcher.setOutAnimation(Support.getInstance().AlbumCoverLeftOutAnimation);
            albumArtSwitcher.setImageDrawable(dr);
        } else {
            albumArtSwitcher.setInAnimation(Support.getInstance().AlbumCoverLeftInAnimation);
            albumArtSwitcher.setOutAnimation(Support.getInstance().AlbumCoverLeftOutAnimation);
            albumArtSwitcher.setImageResource(R.drawable.place_holder_cover);
        }

    }

    public void setAlbumArt(String path){
        Drawable dr = Drawable.createFromPath(path) ;
        if(dr != null) {
            albumArtSwitcher.setImageDrawable(dr);
        } else {
            albumArtSwitcher.setImageResource(R.drawable.place_holder_cover);
        }
    }

    class LyricsGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();

            boolean result = false ;

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        ((AllegroActivity) getActivity()).serviceConnectionPrev();
                    } else {
                        ((AllegroActivity) getActivity()).serviceConnectionNext();
                    }
                }
                result = true;
            }


            return result ;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                                float distanceY) {
            Log.d(DEBUG_TAG, "onScroll: " + e1.toString() + e2.toString());
            return true ;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
            return true ;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            parentFragment.flipCards() ;
            return true ;
        }
    }

    public void resetSeekBar(int length){
        songSeconds = length ;
        seekBar.setMax(songSeconds) ;
        seekBar.setProgress(0) ;
    }

    public CircularSeekBar getSeekbar(){
        return seekBar ;
    }

}
