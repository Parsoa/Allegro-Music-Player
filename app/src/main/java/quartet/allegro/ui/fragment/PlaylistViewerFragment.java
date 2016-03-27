package quartet.allegro.ui.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonFloat;
import quartet.allegro.R;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.List;

import quartet.allegro.AllegroActivity;
import quartet.allegro.database.PlayListData;
import quartet.allegro.database.Track;
import quartet.allegro.database.TrackData;
import quartet.allegro.misc.Persianize;

import static quartet.allegro.AllegroActivity.log;

/**
 * Created by akbar on 6/26/15.
 */
public class PlaylistViewerFragment extends Fragment {

    private static final int FLOAT_BUTTON_MARGIN = 50;
    private static final int SAVING_PROGRESSBAR_MARGIN = 24;

    public int dragStartMode = DragSortController.ON_DOWN;
    public boolean removeEnabled = false;
    public int removeMode = DragSortController.FLING_REMOVE;
    public boolean sortEnabled = true;
    public boolean dragEnabled = true;

    AllegroActivity activity;

    DragSortListView dragList;
    ProgressBar progressBar;
    ButtonFloat saveButton;
    ProgressBar savingProgressBar;
    RelativeLayout animateCanvas;

    PlayListData currentPlayList;

    List<TrackData> currentTrackList;
    List<TrackData> lastSavedTrackList;
    DragListAdapter adapter;

    public static PlaylistViewerFragment newInstance(AllegroActivity activity, PlayListData initialPlaylist){

        PlaylistViewerFragment fragment = new PlaylistViewerFragment();
        fragment.activity = activity;
        fragment.currentPlayList = initialPlaylist;

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_playlist_viewer, container, false);

        dragList = (DragSortListView) v.findViewById(R.id.drag_sort_list);
        progressBar = (ProgressBar) v.findViewById(R.id.progress_bar);

        dragList.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        adapter = new DragListAdapter();
        dragList.setAdapter(adapter);
        dragList.setDropListener(onDrop);
        dragList.setOnItemClickListener(listItemClickListener);
        setController(dragList);

        savingProgressBar = (ProgressBar) v.findViewById(R.id.progressBar_saving);
        saveButton = (ButtonFloat) v.findViewById(R.id.button_float_save);
        animateCanvas = (RelativeLayout) v.findViewById(R.id.animation_area);

        saveButton.setVisibility(View.INVISIBLE);
        savingProgressBar.setVisibility(View.INVISIBLE);

        saveButton.setOnClickListener(floatButtonClick);

        return v;
    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadPlaylist(currentPlayList);
    }

    public void loadPlaylist(PlayListData playListData){

        dragList.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        currentPlayList = playListData;
        new FetchPlaylistSongs().execute(playListData.getSugarId());

    }

    // =============================== Event Handlers =========================================== //

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {

            log("XXXXXX drag from", from, "to", to);

            TrackData src = currentTrackList.get(from);
            currentTrackList.remove(from);
            currentTrackList.add(to, src);

            adapter.notifyDataSetChanged();

            if (from != to)
                showSaveButton();

        }
    };

    View.OnClickListener floatButtonClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (!_locked) {
                lock();
                new SaveTask().execute();
            }
        }
    };

    AdapterView.OnItemClickListener listItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            activity.getServiceConnection().loadAndPlay(currentTrackList, position);
        }
    };

    class SaveTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            log("XXXXX save task");

            for (TrackData td: toDelete) {
                activity.getDataCenter().removeTrackFromPlaylist(td, currentPlayList);
            }

            ArrayList<TrackData> newTrackList = new ArrayList<>();

            for (TrackData td: lastSavedTrackList) {
                if (!toDelete.contains(td))
                    newTrackList.add(td);
            }

            lastSavedTrackList = newTrackList;

            toDelete.clear();

            return activity.getDataCenter().updatePlaylistOrder(currentPlayList, currentTrackList, lastSavedTrackList);
        }

        @Override
        protected void onPostExecute(Boolean resultOK) {
            if (resultOK) {
                lastSavedTrackList = new ArrayList<>(currentTrackList);
            } else {
                currentTrackList = new ArrayList<>(lastSavedTrackList);
                adapter.notifyDataSetChanged();
            }

            unlock();
        }
    }

    // ============================== Delete UI ================================================= //

    private List<TrackData> toDelete = new ArrayList<>();

    private void deleteTemp(TrackData trackData) {
        currentTrackList.remove(trackData);
        adapter.notifyDataSetChanged();
        toDelete.add(trackData);
    }


    // ============================== Save UI =================================================== //

    private boolean _saveButtonOn;

    private void showSaveButton(){

        if (_saveButtonOn)
            return;

        _saveButtonOn = true;

        int width = animateCanvas.getWidth();
        int height = animateCanvas.getHeight();

        saveButton.setX(width - saveButton.getWidth() - FLOAT_BUTTON_MARGIN);
        saveButton.setY(height + saveButton.getHeight());

        int targetY = height - saveButton.getHeight() - FLOAT_BUTTON_MARGIN;
        saveButton.setVisibility(View.VISIBLE);
        saveButton.animate().translationY(targetY).setDuration(500).start();
    }

    private void hideSaveButton(){

        if (!_saveButtonOn)
            return;

        _saveButtonOn = false;

        int targetY = animateCanvas.getHeight() + saveButton.getHeight() + FLOAT_BUTTON_MARGIN;
        saveButton.animate().translationY(targetY).setDuration(350).start();
    }

    private boolean _savingAnimationOn = false;

    private void showSavingProgressbar(){

        if (_savingAnimationOn)
            return;

        _savingAnimationOn = true;

        savingProgressBar.setX(SAVING_PROGRESSBAR_MARGIN);
        savingProgressBar.setY(-savingProgressBar.getHeight());

        savingProgressBar.setVisibility(View.VISIBLE);
        savingProgressBar.animate().translationY(SAVING_PROGRESSBAR_MARGIN).setDuration(250).start();
    }

    private void hideSavingProgressbar(){

        if (!_savingAnimationOn)
            return;

        _savingAnimationOn = false;

        savingProgressBar.animate().translationY(-savingProgressBar.getHeight()).setDuration(500).start();
    }

    private boolean _locked = false;

    private void lock(){
        _locked = true;
        dragList.setDragEnabled(false);
        hideSaveButton();
        showSavingProgressbar();
    }

    private void unlock(){
        dragList.setDragEnabled(true);
        hideSavingProgressbar();
        _locked = false;
    }


    // ========================================================================================== //

    private View.OnClickListener moreButtonClickListener
            = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            PopupMenu popup = new PopupMenu(activity, v);
            popup.getMenuInflater()
                    .inflate(R.menu.menu_playlist_viewer, popup.getMenu());

            final TrackData t = (TrackData) v.getTag();

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {

                    int id = item.getItemId();

                    if (id == R.id.menu_item_delete) {
                        deleteTemp(t);
                        showSaveButton();
                    }

                    return true;
                }
            });

            popup.show(); //showing popup menu
        }
    };


    // ========================================================================================== //

    public void setController(DragSortListView dslv) {

        DragSortController controller = new DragSortController(dslv);
        controller.setDragHandleId(R.id.drag_handle);
        controller.setRemoveEnabled(removeEnabled);
        controller.setSortEnabled(sortEnabled);
        controller.setDragInitMode(dragStartMode);
        controller.setRemoveMode(removeMode);

        dslv.setDragEnabled(true);
        dslv.setFloatViewManager(controller);
        dslv.setOnTouchListener(controller);

    }


    class DragListAdapter extends BaseAdapter {


        public DragListAdapter() {
            currentTrackList = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return currentTrackList.size();
        }

        @Override
        public Object getItem(int position) {
            return currentTrackList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return currentTrackList.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = activity.getLayoutInflater().inflate(R.layout.item_fragment_playlists, parent, false);
                Button moreButton = (Button) convertView.findViewById(R.id.more_button);
                moreButton.setOnClickListener(moreButtonClickListener);
            }

            TrackData track = currentTrackList.get(position);

            String name = track.getDisplayName();
            int duration = (int) (track.getDuration() / 1000);
            String artistName = track.getArtist().getName();
            String albumName = track.getAlbum().getTitle();

            TextView bigText = (TextView) convertView.findViewById(R.id.big_text);
            TextView smallText = (TextView) convertView.findViewById(R.id.small_text);

            Button moreButton = (Button) convertView.findViewById(R.id.more_button);
            moreButton.setTag(track);

            int hours = duration / 3600;
            int minutes = (duration % 3600) / 60;
            int seconds = duration % 60;

            StringBuilder sb = new StringBuilder();

            if (hours != 0) {
                sb.append(Persianize.persianizeNumber(hours));
                sb.append(":");
            }

            sb.append(Persianize.persianizeNumber(minutes));
            sb.append(":");

            sb.append(Persianize.persianizeNumber(seconds));
            sb.append(" | ");

            sb.append(artistName);
            sb.append(" - ");
            sb.append(albumName);

            smallText.setText(sb.toString());
            bigText.setText(name);

            return convertView;
        }
    }

    class FetchPlaylistSongs extends AsyncTask<Long, Void, List<TrackData>> {

        @Override
        protected List<TrackData> doInBackground(Long... params) {

            if (params.length != 1) {
                throw new IllegalArgumentException();
            } else if (params[0] == null) {
                throw new IllegalArgumentException();
            }

            long sugarId = params[0];
            return activity.getDataCenter().fetchPlaylistItems(sugarId);

        }

        @Override
        protected void onPostExecute(final List<TrackData> trackDatas) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    currentTrackList = trackDatas;
                    lastSavedTrackList = new ArrayList<TrackData>(currentTrackList);

                    adapter.notifyDataSetChanged();

                    log("XXXXXXXXXXX found playlist items:", trackDatas.size());
                    dragList.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);

                    String title = getResources().getString(R.string.play_list);
                    title += " " + currentPlayList.getName();
                    activity.setPageTitle(title);

                }
            });
        }
    };

    // ========= Getters and Setters

    public PlayListData getCurrentPlayList() {
        return currentPlayList;
    }
}
