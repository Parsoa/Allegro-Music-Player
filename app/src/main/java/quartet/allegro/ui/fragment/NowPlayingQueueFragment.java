package quartet.allegro.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import quartet.allegro.R;

import quartet.allegro.adapter.NowPlayingQueueAdapter;
import quartet.allegro.database.TrackData;

public class NowPlayingQueueFragment extends Fragment {

    private NowPlayingQueueAdapter adapter;
    private NowPlayingFragment parent;

    private DragSortListView dragSortListView;
    private DragSortController dragSortController;
    public int dragStartMode = DragSortController.ON_DOWN;
    public boolean removeEnabled = false;
    public int removeMode = DragSortController.FLING_REMOVE;
    public boolean sortEnabled = true;
    public boolean dragEnabled = true;

    public static NowPlayingQueueFragment newInstance(NowPlayingQueueAdapter adapter, NowPlayingFragment parent) {
        NowPlayingQueueFragment frag = new NowPlayingQueueFragment();
        frag.adapter = adapter;
        frag.parent = parent;
        return frag;
    }

    public NowPlayingQueueFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.now_playing_queue_fragment, container, false);

        if (savedInstanceState != null) {
        }

        dragSortListView = (DragSortListView) root.findViewById(R.id.now_playing_queue_list);
        dragSortController = buildController(dragSortListView);
        dragSortListView.setFloatViewManager(dragSortController);
        dragSortListView.setOnTouchListener(dragSortController);
        dragSortListView.setDragEnabled(dragEnabled);
        dragSortListView.setAdapter(adapter);

        dragSortListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NowPlayingQueueFragment.this.parent.playFromQueueRequested(position);
            }
        });

        return root;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        dragSortListView.setDropListener(onDrop);
        dragSortListView.setRemoveListener(onRemove);

    }

    public DragSortController buildController(DragSortListView dslv) {

        DragSortController controller = new DragSortController(dslv);
        controller.setDragHandleId(R.id.drag_handle);
        controller.setRemoveEnabled(removeEnabled);
        controller.setSortEnabled(sortEnabled);
        controller.setDragInitMode(dragStartMode);
        controller.setRemoveMode(removeMode);
        return controller;

    }

    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    if (from != to) {
                        TrackData item = adapter.getItem(from);
                        adapter.remove(item);
                        adapter.insert(item, to);
                        parent.updatePlaylistQueue(from, to);
                    }
                }
            };

    private DragSortListView.RemoveListener onRemove =
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    adapter.remove(adapter.getItem(which));
                }
            };

}
