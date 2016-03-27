package quartet.allegro.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;


import quartet.allegro.adapter.ClassicAlbumListAdapter;
import quartet.allegro.R;

import quartet.allegro.communication.Support;


public class MainViewPagerAlbumListFragment extends Fragment {

    public ListView listView ;
    private int position ;

    ClassicAlbumListAdapter adapter ;
    AdapterView.OnItemClickListener listener ;

    public static MainViewPagerAlbumListFragment newInstance(int pose , ClassicAlbumListAdapter adapter
            , AdapterView.OnItemClickListener listener){
        MainViewPagerAlbumListFragment frag = new MainViewPagerAlbumListFragment() ;
        frag.position = pose ;
        frag.adapter = adapter ;
        frag.listener = listener ;
        return frag ;
    }

    public MainViewPagerAlbumListFragment() {

        Support.getInstance() ;

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_albums_list , container , false) ;
        listView = (ListView)root.findViewById(R.id.main_albums_list) ;

        listView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
        listView.setFastScrollEnabled(true);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(listener);

        return root ;
    }

    public void setAdapter() {
        //AbsListView albumsListView = (AbsListView) listView ;
        //((AdapterView<ListAdapter>) albumsListView).setAdapter(adapter);
        //albumsListView.setOnItemClickListener(listener);
    }

    // ========================================================================================== \\
}
