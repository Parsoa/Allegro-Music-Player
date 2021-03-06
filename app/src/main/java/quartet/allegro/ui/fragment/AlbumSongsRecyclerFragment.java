package quartet.allegro.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.github.florent37.materialviewpager.adapter.RecyclerViewMaterialAdapter;
import quartet.allegro.R;
import quartet.allegro.adapter.AlbumArtistSongsRecyclerAdapter;
import quartet.allegro.database.TrackData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Parsoa on 6/14/15.
 */
public class AlbumSongsRecyclerFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;

    private ProgressBarCircularIndeterminate progressBarCircularIndeterminate ;

    private boolean postponedInitialization = false ;

    private List<TrackData> trackData = new ArrayList<>();

    private Handler handler ;

    public static AlbumSongsRecyclerFragment newInstance() {
        return new AlbumSongsRecyclerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.layout_recycler_album_songs, container, false);

        recyclerView = (RecyclerView) root.findViewById(R.id.album_songs_recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        progressBarCircularIndeterminate = (ProgressBarCircularIndeterminate)root.findViewById(R.id.loading_spinner) ;

        return root ;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.e("Albums" , "ViewCreated") ;

        if(postponedInitialization){
            postponedInitialization = false ;
            adapter = new RecyclerViewMaterialAdapter(new AlbumArtistSongsRecyclerAdapter(trackData , getActivity()));
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(View.VISIBLE);
            progressBarCircularIndeterminate.setVisibility(View.GONE);
        }

        MaterialViewPagerHelper.registerRecyclerView(getActivity(), recyclerView, null);
    }

    public void loadData(List<TrackData> data){
        this.trackData = data ;

        if(recyclerView == null) {
            postponedInitialization = true ;
        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("albums songs" , "received") ;
                    recyclerView.setVisibility(View.VISIBLE);
                    progressBarCircularIndeterminate.setVisibility(View.GONE);
                    adapter = new RecyclerViewMaterialAdapter(new AlbumArtistSongsRecyclerAdapter(trackData , getActivity()));
                    recyclerView.setAdapter(adapter);
                }
            });
        }
    }
}