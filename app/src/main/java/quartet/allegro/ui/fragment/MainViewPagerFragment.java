package quartet.allegro.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.astuetz.PagerSlidingTabStrip;
import quartet.allegro.adapter.ClassicAlbumListAdapter;
import quartet.allegro.adapter.GridArtistListAdapter;
import quartet.allegro.R;
import quartet.allegro.adapter.SongsListAdapter2;
import quartet.allegro.database.DataCenter;

public class MainViewPagerFragment extends Fragment {

    private PagerAdapter pagerAdapter;
    private DataCenter dataCenter;
    private ListView songsListView ;

    private View songsView ;
    private View albumsView ;
    private View artistsView ;

    private View header ;

    private int actonbarHeight ;
    private int minHeaderHeight;
    private int headerHeight;
    private int minHeaderTranslation;
    private int initialHeaderY = -4000 ;

    private MainViewPagerAlbumListFragment albumListFragment ;
    private MainViewPagerArtistsGridFragment artistsGridFragment ;
    private MainViewPagerSongListFragment songListFragment ;

    private ClassicAlbumListAdapter albumListAdapter ;
    private SongsListAdapter2 songsListAdapter2 ;
    private GridArtistListAdapter artistListAdapter ;

    AdapterView.OnItemClickListener artistOnItemClickListener ;
    AdapterView.OnItemClickListener songsOnItemClickListener ;
    AdapterView.OnItemClickListener albumsOnItemClickListener ;

    int previousPage = 2;

    private ViewPager viewPager;

    public static MainViewPagerFragment newInstance() {

        MainViewPagerFragment frag = new MainViewPagerFragment() ;
        return frag ;

    }

    public MainViewPagerFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.main_view_pager_fragment, container, false);

        songListFragment = MainViewPagerSongListFragment.newInstance(0 , songsListAdapter2 , songsOnItemClickListener) ;
        albumListFragment = MainViewPagerAlbumListFragment.newInstance(1 , albumListAdapter, albumsOnItemClickListener) ;
        artistsGridFragment = MainViewPagerArtistsGridFragment.newInstance(2, artistListAdapter, artistOnItemClickListener) ;

        minHeaderHeight = getResources().getDimensionPixelSize(R.dimen.min_header_height);
        headerHeight = getResources().getDimensionPixelSize(R.dimen.header_height);
        minHeaderTranslation = -minHeaderHeight;


        // --------------------------------------------------------------------------- //

        viewPager = (ViewPager) root.findViewById(R.id.main_fragment_view_pager);
        pagerAdapter = new PagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        return root ;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) view.findViewById(R.id.lists_tabs_strip);
        tabs.setViewPager(viewPager);

        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                previousPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    // ==================================================================================================== \\

    public class PagerAdapter extends android.support.v4.app.FragmentPagerAdapter {

        private final String[] TITLES = { getResources().getString(R.string.tracks),
                getResources().getString(R.string.albums),
                getResources().getString(R.string.artists)
        } ;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            if(position == 0) {
                return songListFragment ;
            } else if(position == 1){
                return albumListFragment ;
            } else {
                return artistsGridFragment ;
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
    }


    public void presetAdaptersAndListeners(GridArtistListAdapter gridArtistListAdapter
            , AdapterView.OnItemClickListener artistOnItemClickListener
            , ClassicAlbumListAdapter albumListAdapter
            , AdapterView.OnItemClickListener albumOnItemClickListener
            , SongsListAdapter2 songsListAdapter2
            , AdapterView.OnItemClickListener songsOnItemClickListener) {

        this.artistListAdapter = gridArtistListAdapter ;
        this.artistOnItemClickListener = artistOnItemClickListener ;

        this.albumListAdapter = albumListAdapter ;
        this.albumsOnItemClickListener = albumOnItemClickListener ;

        this.songsListAdapter2 = songsListAdapter2 ;
        this.songsOnItemClickListener = songsOnItemClickListener ;

    }

    public void resetPosition(){
        viewPager.setCurrentItem(previousPage);
    }


}



