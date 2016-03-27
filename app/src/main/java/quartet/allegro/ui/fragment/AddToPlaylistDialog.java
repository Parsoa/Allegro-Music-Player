package quartet.allegro.ui.fragment;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonFloat;

import java.util.ArrayList;
import java.util.List;

import quartet.allegro.AllegroActivity;
import quartet.allegro.R;
import quartet.allegro.communication.PlaylistFetchListener;
import quartet.allegro.database.AlbumData;
import quartet.allegro.database.ArtistData;
import quartet.allegro.database.PlayListData;
import quartet.allegro.database.TrackData;
import quartet.allegro.ui.YobsViewPager;

public class AddToPlaylistDialog extends DialogFragment implements PlaylistFetchListener {


    public static AddToPlaylistDialog create(AllegroActivity activity, TrackData trackData) {
        AddToPlaylistDialog res = new AddToPlaylistDialog();
        res.track = trackData;
        res.activity = activity;
        res.type = DialogType.TRACK;
        return res;
    }

    public static AddToPlaylistDialog create(AllegroActivity activity, ArtistData artistData) {
        AddToPlaylistDialog res = new AddToPlaylistDialog();
        res.artist = artistData;
        res.activity = activity;
        res.type = DialogType.ARTIST;
        return res;
    }

    public static AddToPlaylistDialog create(AllegroActivity activity, AlbumData albumData) {
        AddToPlaylistDialog res = new AddToPlaylistDialog();
        res.album = albumData;
        res.activity = activity;
        res.type = DialogType.ALBUM;
        return res;
    }

    private enum DialogType {
        TRACK, ARTIST, ALBUM
    }

    private DialogType type;
    private TrackData track;
    private ArtistData artist;
    private AlbumData album;

    private YobsViewPager viewPager;
    private ListView playlistsListView;
    private AllegroActivity activity;
    private View rootView;
    private ButtonFloat buttonFloatNew;
    private ButtonFloat buttonFloatDone;
    private ProgressBar loadingPlaylists;
    private EditText nameField;
    private TextView errorTextView;
    private ProgressBar loadingNew;

    private Handler handler = new Handler();
    private List<PlayListData> playListDatas;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_dialog_addtoplaylist, container, false);

        this.getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(true);

        viewPager = (YobsViewPager) rootView.findViewById(R.id.viewPager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(0, true);

        nameField = (EditText) rootView.findViewById(R.id.edittext_playlist_name);
        errorTextView = (TextView) rootView.findViewById(R.id.error_textview);

        playlistsListView = (ListView) rootView.findViewById(R.id.listview_playlists);
        loadingPlaylists = (ProgressBar) rootView.findViewById(R.id.loading_spinner);
        buttonFloatDone = (ButtonFloat) rootView.findViewById(R.id.buttonFloatDone);
        loadingNew = (ProgressBar) rootView.findViewById(R.id.loading_new_playlist);
        buttonFloatNew = (ButtonFloat) rootView.findViewById(R.id.buttonFloatAdd);

        playlistsListView = (ListView) rootView.findViewById(R.id.listview_playlists);
        playListDatas = activity.getCachedPlaylistList();

        if (playListDatas != null) {
            loadingPlaylists.setVisibility(View.GONE);
            onPlaylistsFetched(playListDatas);
        }

        activity.fetchPlaylistList(this);

        buttonFloatNew.setOnClickListener(addButtonListener);
        buttonFloatDone.setOnClickListener(doneButtonListener);

        playlistsListView.setOnItemClickListener(itemClickListener);

        nameField.addTextChangedListener(textWatcher);

        return rootView;
    }

    @Override
    public void onPlaylistsFetched(List<PlayListData> playLists) {

        this.playListDatas = playLists;
        ArrayList<String> playlistNames = new ArrayList<>();
        for (PlayListData p:playLists) {
            playlistNames.add(p.getName());
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                R.layout.item_simple_list, playlistNames);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingPlaylists.setVisibility(View.GONE);
                playlistsListView.setAdapter(adapter);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        // TODO might get messed up in the smaller gooshies

        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
        float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400 , getResources().getDisplayMetrics());

        Window window = getDialog().getWindow();
        window.setLayout((int) width, (int) height);
        window.setGravity(Gravity.CENTER);
    }

    private View.OnClickListener addButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            goToNewPlaylist();
        }
    };

    private View.OnClickListener doneButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String text = nameField.getText().toString();
            if (text.isEmpty()) {
                inputError(1);
            } else {
                requestAddPlaylist(text);
            }
        }
    };

    private AdapterView.OnItemClickListener itemClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            PlayListData data = playListDatas.get(position);
            new AddSongsToPListTask(data).execute();
            AddToPlaylistDialog.this.dismiss();
        }
    };

    // ========================= Adapter

    private PagerAdapter pagerAdapter = new PagerAdapter() {

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            int resid = 0;

            switch (position) {
                case 0:
                    resid = R.id.page1;
                    break;
                case 1:
                    resid = R.id.page2;
                    break;
            }

            return container.findViewById(resid);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

    };

    // ========================= Database Interface

    private void requestAddPlaylist(String name) {

        if (activity.isCachedDataAvailable() && activity.isScanRunning()) {
            String message = activity.getResources().getString(R.string.toast_error_scan_running);
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            goToList();
            return;
        }

        lockAndShowLoadingSpinner();
        new AddPlaylistTask().execute(name);
    }

    private void processAddedPlaylist(PlayListData data) {

        int i=0;
        for (PlayListData p:playListDatas) {
            if (data.getName().compareTo(p.getName()) >= 0) {
                playListDatas.add(i, data);
                break;
            }
            i++;
        }

        ArrayList<String> playlistNames = new ArrayList<>();
        for (PlayListData p:playListDatas) {
            playlistNames.add(p.getName());
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                R.layout.item_simple_list, playlistNames);

        playlistsListView.setAdapter(adapter);
        activity.introduceNewPlaylist(data);
    }


    class AddSongsToPListTask extends AsyncTask<Void, Void, Void> {

        private PlayListData playlist;
        private List<TrackData> tracks;

        public AddSongsToPListTask(PlayListData playlist) {
            this.playlist = playlist;
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (type == DialogType.TRACK) {
                this.tracks = new ArrayList<>();
                tracks.add(track);
            } else if (type == DialogType.ALBUM) {
                tracks = activity.getDataCenter().fetchAlbumSongs(album);
            } else if (type == DialogType.ARTIST) {
                tracks = activity.getDataCenter().fetchArtistSongs(artist);
            }

            activity.getDataCenter().addToPlaylist(tracks, playlist);
            return null;
        }

    }

    class AddPlaylistTask extends AsyncTask<String, Void, PlayListData> {

        @Override
        protected PlayListData doInBackground(String... params) {
            return activity.getDataCenter().addPlaylist(params[0]);
        }

        @Override
        protected void onPostExecute(final PlayListData playListData) {

            if (playListData == null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        inputError(0);
                        unlockAndClearLoadingSpinner();
                    }
                });
            } else {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        unlockAndClearLoadingSpinner();
                        processAddedPlaylist(playListData);
                        goToList();
                    }
                });
            }

        }
    }

    // ========================= UI methods

    private void goToNewPlaylist() {
        buttonFloatNew.hide();
        viewPager.setCurrentItem(1);
        unlockAndClearLoadingSpinner();
        nameField.setText("");
    }

    private void goToList() {
        viewPager.setCurrentItem(0);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                buttonFloatNew.show();
            }
        }, 1000);
    }

    boolean errorOnScreen = false;

    private void inputError(int code) {

        errorOnScreen = true;
        errorTextView.setVisibility(View.VISIBLE);

        if (code == 0)
            errorTextView.setText(activity.getResources().getString(R.string.error_playlist_already_exists));
        else
            errorTextView.setText(activity.getResources().getString(R.string.error_playlist_name_null));

        Animation shake = AnimationUtils.loadAnimation(activity, R.anim.shake);
        nameField.startAnimation(shake);
    }

    private void clearError() {
        errorTextView.setVisibility(View.INVISIBLE);
        errorOnScreen = false;
    }

    private void lockAndShowLoadingSpinner() {
        _hideKeyboard();
        loadingNew.setVisibility(View.VISIBLE);
        nameField.setEnabled(false);
        buttonFloatDone.setEnabled(false);
    }

    private void unlockAndClearLoadingSpinner() {
        loadingNew.setVisibility(View.GONE);
        nameField.setEnabled(true);
        buttonFloatDone.setEnabled(true);
    }

    private void _hideKeyboard() {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (errorOnScreen) {
                clearError();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

}
