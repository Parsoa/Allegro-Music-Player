package quartet.allegro.ui.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import quartet.allegro.R;

public class LyricsFlipFragment extends Fragment {

    private NowPlayingFragment parentFragment ;

    private View.OnTouchListener listener ;
    private GestureDetectorCompat gestureDetector ;
    private TextView lyricsTextView ;
    private FrameLayout lyricsMainLayout ;

    public LyricsFlipFragment() {

    }

    public static LyricsFlipFragment newInstance(NowPlayingFragment p){

        LyricsFlipFragment frag = new LyricsFlipFragment() ;
        frag.parentFragment = p ;
        return frag ;

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        gestureDetector = new GestureDetectorCompat(activity , new FlipGestureDetector()) ;
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


        final View root = inflater.inflate(R.layout.lyrics_flip_fragment, container, false);

        if (savedInstanceState != null) {

        }

        lyricsTextView = (TextView)root.findViewById(R.id.lyrics_text_view) ;
        lyricsTextView.setOnTouchListener(listener);
        lyricsTextView.setText(R.string.lipsum);
        lyricsTextView.setTypeface(Typeface.createFromAsset(getActivity().getAssets() , "Niloofar.ttf"));

        lyricsMainLayout = (FrameLayout)root.findViewById(R.id.lyrics_fragment_frame_layout) ;
        lyricsMainLayout.setOnTouchListener(listener);

        return root;

    }

    class FlipGestureDetector extends GestureDetector.SimpleOnGestureListener {

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
            return true ;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true ;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            parentFragment.flipCards() ;
            return true ;
        }
    }
}
