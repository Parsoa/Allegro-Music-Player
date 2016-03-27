package quartet.allegro.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.github.florent37.materialviewpager.adapter.RecyclerViewMaterialAdapter;
import quartet.allegro.R;
import quartet.allegro.adapter.ArtistAlbumsRecyclerAdapter;
import quartet.allegro.database.AlbumData;

import java.util.ArrayList;
import java.util.List;

public class ArtistAlbumsRecyclerFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private ProgressBarCircularIndeterminate progressBarCircularIndeterminate ;

    private boolean postponedInitialization = false ;
    private ArtistViewPagerFragment parent ;

    private Handler handler  ;

    private List<AlbumData> albumData = new ArrayList<>();

    public static ArtistAlbumsRecyclerFragment newInstance(ArtistViewPagerFragment parent) {
        ArtistAlbumsRecyclerFragment frag = new ArtistAlbumsRecyclerFragment() ;
        frag.parent = parent ;
        return frag ;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_recycler_artist_albums, container, false);

        recyclerView = (RecyclerView) root.findViewById(R.id.artist_albums_recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        progressBarCircularIndeterminate = (ProgressBarCircularIndeterminate)root.findViewById(R.id.loading_spinner) ;

        return root ;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState) ;

        if(postponedInitialization){
            postponedInitialization = false ;
            adapter = new RecyclerViewMaterialAdapter(new ArtistAlbumsRecyclerAdapter(albumData , getActivity() , parent));
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(View.VISIBLE);
            progressBarCircularIndeterminate.setVisibility(View.GONE);
        }

        MaterialViewPagerHelper.registerRecyclerView(getActivity(), recyclerView, null);
    }

    public void loadData(List<AlbumData> l){
        this.albumData = l ;
        if(recyclerView == null) {
            postponedInitialization = true;
        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recyclerView.setVisibility(View.VISIBLE);
                    progressBarCircularIndeterminate.setVisibility(View.GONE);
                    adapter = new RecyclerViewMaterialAdapter(new ArtistAlbumsRecyclerAdapter(albumData , getActivity() , parent));
                    recyclerView.setAdapter(adapter);
                }
            });
        }
    }
}

