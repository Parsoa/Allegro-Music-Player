package quartet.allegro.ui.fragment;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.flaviofaria.kenburnsview.Transition;
import com.github.florent37.materialviewpager.MaterialViewPager;

import java.util.List;
import quartet.allegro.AllegroActivity;

import quartet.allegro.R;
import quartet.allegro.bone.AlbumProvider;
import quartet.allegro.communication.AlbumDisplayer;
import quartet.allegro.database.AlbumData;
import quartet.allegro.database.CoverImageData;
import quartet.allegro.database.MusicInfoData;
import quartet.allegro.database.TrackData;

public class AlbumViewPagerFragment extends Fragment implements AlbumDisplayer {

    private AllegroActivity activity;
    private KenBurnsView header ;

    private final static String DEBUG_TAG = "AlbumViewPager" ;

    private boolean albumInfoAvailable = false ;
    private boolean songListAvailable = false ;
    private boolean coverImageDataReady = false ;

    private LoadingFragment infoLoadingFragment ;

    private AlbumProvider albumProvider ;
    private AlbumSongsRecyclerFragment songsRecyclerFragment ;
    private MaterialViewPager viewPager ;

    private List<TrackData> trackData ;
    private List<CoverImageData> coverImageData ;
    private AlbumData currentAlbumData ;

    public int animCounter ;
    private int currentCover ;

    public static AlbumViewPagerFragment newInstance(AlbumData albumData , AllegroActivity activity){
        AlbumViewPagerFragment frag = new AlbumViewPagerFragment() ;
        frag.currentAlbumData = albumData ;
        frag.activity = activity ;
        return frag ;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.view_pager_fragment_album , container, false);

        // ====================================================================================== \\

        songsRecyclerFragment = AlbumSongsRecyclerFragment.newInstance() ;

        infoLoadingFragment = LoadingFragment.newInstance() ;

        // ====================================================================================== \\

        viewPager = (MaterialViewPager) root.findViewById(R.id.album_info_view_pager) ;

        header = (KenBurnsView) viewPager.getHeaderBackgroundContainer().findViewById(R.id.header_ken_burns_view) ;

        header.setTransitionListener(new KenBurnsView.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {

            }

            @Override
            public void onTransitionEnd(Transition transition) {
            }
        });

        viewPager.getPagerTitleStrip().setTextColor(getResources().getColor(R.color.colorAccent));
        viewPager.getViewPager().setAdapter(new FragmentStatePagerAdapter(activity.getSupportFragmentManager()) {

            String[] TITLES = {"درباره", "آهنگ ها"};

            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        Log.e(DEBUG_TAG, "Created info fragment");
                        return infoLoadingFragment;
                    case 1:
                        return songsRecyclerFragment;
                    default:
                        return null;
                }
            }

            @Override
            public int getCount() {
                return TITLES.length;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return TITLES[position];
            }
        });

        viewPager.getViewPager().setOffscreenPageLimit(viewPager.getViewPager().getAdapter().getCount());
        viewPager.getPagerTitleStrip().setViewPager(viewPager.getViewPager());
        viewPager.getViewPager().setCurrentItem(0);

        requestLoadData();
        return root;
    }

    public void requestLoadData() {
        albumProvider = new AlbumProvider(activity , currentAlbumData , this) ;
        albumProvider.start() ;
    }

    // ========================================================================================== \\

    @Override
    public void setMusicInfo(MusicInfoData info) {
        this.albumInfoAvailable = true ;
    }

    @Override
    public void setSongList(List<TrackData> songList) {
        this.trackData = songList ;
        this.songListAvailable = true ;
        songsRecyclerFragment.loadData(this.trackData);
    }

    @Override
    public void setCoverPhotos(List<CoverImageData> covers) {
        this.coverImageData = covers ;
        if(covers.size() > 0){
            coverImageDataReady = true;
            animCounter = 0;
            currentCover = 0;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setHeaderImage();
                }
            });
        } else {
            coverImageDataReady = false ;
            header.setImageResource(R.drawable.place_holder_cover);
        }
    }

    // ========================================================================================== \\

    private void setHeaderImage(){
        if(coverImageData.size() > 0) {
            Drawable d = Drawable.createFromPath(coverImageData.get(currentCover).getPath());
            if (d != null) {
                header.setImageDrawable(d);
            } else {
                coverImageData.remove(currentCover);
                //header.setImageResource(R.drawable.place_holder_cover);
                setHeaderImage();
            }
        } else {
            header.setImageResource(R.drawable.place_holder_cover);
            coverImageDataReady = false ;
        }
    }
}