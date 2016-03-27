package quartet.allegro.ui.fragment;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.flaviofaria.kenburnsview.Transition;
import com.github.florent37.materialviewpager.MaterialViewPager;

import java.util.List;

import quartet.allegro.AllegroActivity;

import quartet.allegro.R;
import quartet.allegro.bone.ArtistProvider;
import quartet.allegro.communication.ArtistDisplayer;
import quartet.allegro.database.AlbumData;
import quartet.allegro.database.ArtistData;
import quartet.allegro.database.CoverImageData;
import quartet.allegro.database.MusicInfoData;
import quartet.allegro.database.TrackData;


public class ArtistViewPagerFragment extends Fragment implements ArtistDisplayer {

    private AllegroActivity activity;
    private KenBurnsView header ;

    private boolean bandInfoAvailable = false ;
    private boolean albumInfoAvailable = false ;
    private boolean songInfoAvailable = false ;
    private boolean coverImageDataReady = false ;

    public int animCounter = 1 ;
    private int currentCover ;

    private ArtistSongsRecyclerFragment songsRecyclerFragment ;
    private ArtistAlbumsRecyclerFragment albumsRecyclerFragment ;
    MaterialViewPager materialViewPager;

    private LoadingFragment bandInfoLoadingFragment = LoadingFragment.newInstance() ;

    private List<AlbumData> albumData ;
    private List<TrackData> trackData ;
    private List<CoverImageData> coverImageData ;

    private ArtistProvider artistProvider ;
    private ArtistData currentArtistData ;

    public static ArtistViewPagerFragment newInstance(ArtistData data , AllegroActivity activity){
        ArtistViewPagerFragment frag = new ArtistViewPagerFragment() ;
        frag.activity = activity ;
        frag.currentArtistData = data ;
        return frag ;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.view_pager_fragment_artist, container, false);

        // ====================================================================================== \\

        songsRecyclerFragment = ArtistSongsRecyclerFragment.newInstance() ;

        // ====================================================================================== \\

        albumsRecyclerFragment = ArtistAlbumsRecyclerFragment.newInstance(this) ;

        // ====================================================================================== \\

        materialViewPager = (MaterialViewPager) root.findViewById(R.id.artist_info_view_pager) ;

        header = (KenBurnsView) materialViewPager.getHeaderBackgroundContainer().findViewById(R.id.header_ken_burns_view) ;

        header.setTransitionListener(new KenBurnsView.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {

            }
            @Override
            public void onTransitionEnd(Transition transition) {
                if(coverImageDataReady){
                    animCounter++ ;
                    if(animCounter % 4 == 0){
                        animCounter = 0 ;
                        currentCover++ ;
                        currentCover = currentCover % coverImageData.size() ;
                        setHeaderImage();
                    }
                }
            }
        });

        materialViewPager.getPagerTitleStrip().setTextColor(getResources().getColor(R.color.colorAccent));

        materialViewPager.getViewPager().setAdapter(new FragmentStatePagerAdapter(activity.getSupportFragmentManager()) {

            String[] TITLES = {"درباره" , "آلبوم ها" , "آهنگ ها"} ;

            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return bandInfoLoadingFragment ;
                    case 1 :
                        return albumsRecyclerFragment ;
                    case 2 :
                        return songsRecyclerFragment ;
                    default:
                        return null ;
                }
            }

            @Override
            public int getCount() {
                return TITLES.length ;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return TITLES[position] ;
            }
        });

        materialViewPager.getViewPager().setOffscreenPageLimit(materialViewPager.getViewPager().getAdapter().getCount());
        materialViewPager.getPagerTitleStrip().setViewPager(materialViewPager.getViewPager());
        materialViewPager.getViewPager().setCurrentItem(1);

        requestLoadData();
        return root;

    }

    public void requestLoadData() {
        artistProvider = new ArtistProvider(activity , currentArtistData , this) ;
        artistProvider.start() ;
    }

    @Override
    public void setCoverPhotos(List<CoverImageData> covers) {
        this.coverImageData = covers ;
        if(covers.size() > 0) {
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

    private void setHeaderImage(){
        if(coverImageData.size() > 0) {
            Drawable d = Drawable.createFromPath(coverImageData.get(currentCover).getPath());
            if (d != null) {
                header.setImageDrawable(d);
            } else {
                coverImageData.remove(currentCover);
                //header.setImageResource(R.drawable.place_holder_cover);
                currentCover = 0 ;
                setHeaderImage();
            }
        } else {
            header.setImageResource(R.drawable.place_holder_cover);
            coverImageDataReady = false ;
        }
    }

    @Override
    public void setAlbumList(List<AlbumData> albumList) {
        albumInfoAvailable = true ;
        this.albumData = albumList ;
        albumsRecyclerFragment.loadData(this.albumData);
    }

    @Override
    public void setMusicInfo(MusicInfoData info) {
        bandInfoAvailable = true ;
    }

    @Override
    public void setSongList(List<TrackData> songList) {
        songInfoAvailable = true ;
        this.trackData = songList ;
        songsRecyclerFragment.loadData(this.trackData);
    }

    public void showAlbumInfoFragment(AlbumData data){
        AlbumViewPagerFragment albumViewPagerFragment = AlbumViewPagerFragment.newInstance(data
                , activity) ;
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction() ;
        fragmentTransaction.setCustomAnimations(R.anim.push_down_enter, R.anim.push_down_exit) ;
        fragmentTransaction.add(R.id.artist_view_pager_fragment_linear_layout , albumViewPagerFragment);
        fragmentTransaction.disallowAddToBackStack() ;
        fragmentTransaction.commit() ;
    }
}
