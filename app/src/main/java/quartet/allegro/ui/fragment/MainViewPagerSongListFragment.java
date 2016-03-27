package quartet.allegro.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import quartet.allegro.R;
import quartet.allegro.adapter.SongsListAdapter2;

import static quartet.allegro.AllegroActivity.log;


public class MainViewPagerSongListFragment extends Fragment {

    public ListView listView ;
    private int position ;

    SongsListAdapter2 adapter ;
    AdapterView.OnItemClickListener listener ;

    public static MainViewPagerSongListFragment newInstance(int pose , SongsListAdapter2 adapter ,
                                                            AdapterView.OnItemClickListener listener){
        MainViewPagerSongListFragment frag = new MainViewPagerSongListFragment() ;
        frag.position = pose ;
        frag.adapter = adapter ;
        frag.listener = listener ;
        return frag ;
    }

    public MainViewPagerSongListFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity) ;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        log("XXXXXX created song fragment XXXXXXX");

        View root = inflater.inflate(R.layout.fragment_song_list , container , false) ;
        listView = (ListView)root.findViewById(R.id.main_song_list) ;
        listView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(listener);

        return root ;
    }


}
