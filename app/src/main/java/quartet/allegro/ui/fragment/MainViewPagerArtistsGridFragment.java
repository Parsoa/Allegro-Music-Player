package quartet.allegro.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;


import quartet.allegro.adapter.GridArtistListAdapter;
import quartet.allegro.R;

public class MainViewPagerArtistsGridFragment extends Fragment {

    public GridView gridView ;
    private int position ;

    GridArtistListAdapter adapter ;
    AdapterView.OnItemClickListener listener ;

    public static MainViewPagerArtistsGridFragment newInstance(int pose , GridArtistListAdapter adapter ,
                                                               AdapterView.OnItemClickListener listener){
        MainViewPagerArtistsGridFragment frag = new MainViewPagerArtistsGridFragment() ;
        frag.position = pose ;
        frag.adapter = adapter ;
        frag.listener = listener ;
        return frag ;
    }

    public MainViewPagerArtistsGridFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity) ;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_artist_grid , container , false) ;
        gridView = (GridView)root.findViewById(R.id.main_artist_grid) ;
        gridView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);

        setAdapter();

        return root ;
    }

    public void setAdapter() {
        AbsListView artistsListView = (AbsListView) gridView ;
        ((AdapterView<ListAdapter>) artistsListView).setAdapter(adapter);
        artistsListView.setOnItemClickListener(listener);
    }

}
