package quartet.allegro;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import quartet.allegro.adapter.ClassicAlbumListAdapter;
import quartet.allegro.adapter.GridArtistListAdapter;
import quartet.allegro.adapter.PlaylistsAdapter;
import quartet.allegro.adapter.SongsListAdapter2;
import quartet.allegro.async.AlbumNotifyListener;
import quartet.allegro.async.ArtistNotifyListener;
import quartet.allegro.async.TrackNotifyListener;
import quartet.allegro.bone.EargasmService;
import quartet.allegro.bone.MusicPlayerUI;
import quartet.allegro.communication.PlaylistFetchListener;
import quartet.allegro.communication.ScanCycleListener;
import quartet.allegro.communication.Support;
import quartet.allegro.database.Album;
import quartet.allegro.database.AlbumData;
import quartet.allegro.database.Artist;
import quartet.allegro.database.ArtistData;
import quartet.allegro.database.DataCenter;

import quartet.allegro.database.PlayListData;
import quartet.allegro.ui.fragment.AddToPlaylistDialog;
import quartet.allegro.ui.fragment.AlbumViewPagerFragment;
import quartet.allegro.ui.fragment.ArtistViewPagerFragment;

import quartet.allegro.database.TrackData;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import chrisrenke.drawerarrowdrawable.DrawerArrowDrawable;
import quartet.allegro.communication.SongDisplayer;
import quartet.allegro.ui.fragment.CircularTimePickerFragment;
import quartet.allegro.ui.fragment.MainViewPagerFragment;
import quartet.allegro.ui.fragment.NowPlayingFragment;
import quartet.allegro.ui.widget.WidgetBroadcastReceiver;
import quartet.allegro.ui.fragment.PlaylistViewerFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class AllegroActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener,
        DataCenterOwner, UiThreadHolder,
        TrackNotifyListener, AlbumNotifyListener, ArtistNotifyListener
        ,ScanCycleListener {

    // ========= MACROS ===================================== //

    public static boolean alreadyStarted = false ;

    private static final int TAG_SONG_POSITION = 0;
    private static final String SHARED_PREFERENCES = "ACTIVITY_SHARED_PREFERENCES";
    private static final String LOG_TAG = "GOOSHASM_PROTOTYPE";

    private static final String DEBUG_TAG = "ALLEGRO_MAIN" ;
    private static final String PREF_NAME = "ALLEGRO_PREFS" ;
    private static final String APP_STATE = "ALLEGRO_ACTIVITY_RUNNING" ;
    private static final String WIDGET_STATE = "ALLEGRO_WIDGET_RECEIVER" ;

    private static final String DIALOG_ADD_TO_PLAYLIST = "DIALOG_ADD_TO_PLAYLIST" ;

    public final static String ALBUM_INFO_STATE = "ALBUM_INFO" ;
    public final static String PLAYLIST_STATE = "PLAYLIST_STATE" ;
    public final static String ARTIST_INFO_STATE = "ARTIST_INFO" ;
    public final static String NOW_PLAYING_STATE = "NOW_PLAYING" ;
    public final static String MAIN_VIEWPAGER_STATE = "MAIN_VIEWPAGER" ;

    SharedPreferences sharedpreferences;

    public static void log(Object... s){
        StringBuilder sb = new StringBuilder();
        for (Object ss:s){
            sb.append(ss);
            sb.append(" ");
        }
        //Log.d(LOG_TAG, sb.toString());
    }

    // ================== Fields ============================ //

    private boolean flipped;
    private float offset;

    private DataCenter dataCenter;
    private Handler handler;

    private boolean nowPlayingOpen = false ;

    boolean cachedDataAvailable = false;

    boolean artistsAttached = false;
    boolean albumsAttached = false;
    boolean songsAttached = false;

    boolean startedPlaying = false ;

    SongsListAdapter2 songsListAdapter;
    ClassicAlbumListAdapter albumsListAdapter;
    GridArtistListAdapter artistsListAdapter;
    PlaylistsAdapter playlistsAdapter;

    SongsListAdapter2 newSongAdapter;

    private DrawerLayout drawer;
    private Button playPauseButton;
    private TextView playPauseBigText;
    private TextView playPauseSmallText;
    private ImageView playPauseArtwork;
    private TextView pageTitleTextView;
    private LinearLayout playPauseHandle;
    private LinearLayout playPauseContent;

    private MainViewPagerFragment viewPagerFragment ;
    private NowPlayingFragment nowPlayingFragment ;
    private AlbumViewPagerFragment albumInfoFragment ;
    private PlaylistViewerFragment playlistViewerFragment ;

    private Stack<String> stateStack = new Stack<>();
    private ArtistData lastVisitedArtist ;
    private AlbumData lastVisitedAlbumData;

    private android.support.v4.app.Fragment artistInfoFragment;
    private android.support.v4.app.Fragment contentFragment;

    private ImageButton searchButton ;
    private ImageButton prefButton ;
    private ImageButton timerButton ;

    Handler mHandler = new Handler();

    private static enum State {
        PLAYING, PAUSED
    }

    private State state;

    // ========================================================================================= \\

    private TrackData popUpTrackData ;
    private AlbumData popUpAlbumData ;
    private ArtistData popUpArtistData ;

    private TrackData[] trackDataArray ;
    private AlbumData[] albumDataArray ;
    private ArtistData[] artistDataArray ;

    private ArrayList<TrackData> trackDatas;
    private ArrayList<ArtistData> artistDatas;
    private ArrayList<AlbumData> albumDatas;

    private String _previousPageTitle_ = null;

    public enum RepeatState {
        CURRENT, QUEUE, NONE
    }

    // ========================================================================================== //

    public AllegroActivity(){

        super();
        this.handler = new Handler();

    }

    // ================================= New Service Handling =================================== //

    public class MyServiceConnection2 implements ServiceConnection, MusicPlayerUI {

        private EargasmService service;
        public boolean connected = false ;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            this.service = ((EargasmService.EargasmBinder)service).getService();
            this.service.registerCallbackListener(this);

            if (nowPlayingFragment != null) {
                registerSongDisplayer(nowPlayingFragment);
            }

            this.service.requestBroadcast();

            if (this.service.queueAvailable()) {
                this.service.requestUpdateDisplayers();
            }

            connected = true ;
            this.service.notifyActivityIn();
            Log.e(DEBUG_TAG, "Connected") ;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            this.service.unregisterCallbackListener(this);
            this.service = null ;
            Log.e(DEBUG_TAG, "Disconnected") ;
        }

        public void setQueueToAlbum(AlbumData album){
            service.setQueueToAlbum(album);
        }

        public void setQueueToArtist(ArtistData artist){
            service.setQueueToArtist(artist);
        }

        public void addAlbumToQueue(AlbumData album){
            service.addAlbumToQueue(album);
        }

        public void addArtistToQueue(ArtistData artist){
            service.addArtistToQueue(artist);
        }

        public void addArtistToPlayList(ArtistData artist){
            service.addArtistToPlayList(artist);
        }

        public void addAlbumToPlayList(AlbumData album) {
            service.addAlbumToPlayList(album);
        }

        public void setNextTrackQueue(TrackData trackData){
            service.setNextTrackInQueue(trackData);
        }

        public void addSongToQueue(TrackData trackData){
            service.addSongToQueue(trackData);
        }

        public TrackData requestCurrentTrack(){
            return service.nowPlayingTrackData() ;
        }

        public void playPlayer(){
            service.requestPlay();
        }

        public void pausePlayer(){
            service.requestPause();
        }

        public boolean isAvailable(){
            return service != null;
        }

        public void requestBroadcast(){
            service.requestBroadcast();
        }

        public void requestUpdateDisplayers(){
            service.requestUpdateDisplayers();
        }

        public void loadAndPlay(List<TrackData> t, int initialPosition){
            service.requestPlay(t, initialPosition);
        }

        public void updateQueue(List<TrackData> newOrder, int newPositionOfCurrent){
            service.updateQueue(newOrder, newPositionOfCurrent);
        }

        public void requestNext(){
            service.requestNext();
        }

        public void requestPrevious(){
            service.requestPrevious();
        }

        public void requestToggleState(){
            service.iteratePlayState();
        }

        public void setRepeat(RepeatState state){
            service.setRepeat(state);
        }

        public void requestSeek(int progress){
            service.requestSeek(progress);
        }

        public void gotoTrack(int trackNum) {
            service.gotoTrack(trackNum);
        }

        public float getCurrentProgress(){
            return service.getCurrentProgress();
        }

        public void registerSongDisplayer(SongDisplayer displayer){
            if(isAvailable()) {
                service.registerSongDisplayer(displayer);
            }
        }

        public boolean unregisterSongDisplayer(SongDisplayer displayer){
            return service.unregisterSongDisplayer(displayer);
        }

        public boolean queueAvailable(){
            return service.queueAvailable();
        }

        public boolean isVirgin(){
            return service.isVirgin();
        }

        @Override
        public void updateUI(final EargasmService.EargasmState state, final float progress, final TrackData track) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (track != null) {

                        if (serviceConnection.isVirgin()) {
                            _hideHandle();
                        } else {
                            if(!startedPlaying) {
                                _showHandle();
                                startedPlaying = true;
                            }
                        }

                        playPauseBigText.setText(track.getDisplayName());
                        playPauseSmallText.setText(track.getArtist().getName() + " | " + track.getAlbum().getTitle());

                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {

                                Drawable d = Drawable.createFromPath(track.getAlbum().getAlbumArtPath());
                                if (d == null)
                                    d = new BitmapDrawable(((GooshasmApp)getApplication()).getPlaceHolder());

                                final Drawable img = d;

                                postOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        playPauseArtwork.setImageDrawable(img);
                                    }
                                });

                                return null;
                            }
                        }.execute();

                    } else {

                        // TODO temprary fix, find a proper way to fix this
                        // NOTE:
                        // this gets called a little bit early and due to
                        // an annoying bug, it breaks "hide handle" until
                        // a show handle happens which cannot be afforded
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        _hideHandle();
                                    }
                                });
                            }
                        }, 500);

                        playPauseBigText.setText("");
                        playPauseSmallText.setText("");
                        playPauseArtwork.setImageDrawable(null);

                    }

                    switch (state) {
                        case PLAYING:
                            _switchUiToPlaying();
                            break;

                        case PAUSED:
                        case STOPPED:
                        case ERROR:
                            _switchUiToPaused();
                            break;
                    }
                }
            });
        }
    }

    private MyServiceConnection2 serviceConnection;

    public MyServiceConnection2 getServiceConnection() {
        return serviceConnection;
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, EargasmService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        ((GooshasmApp)getApplication()).notifyActivityIn();

        Log.e(DEBUG_TAG, "Started") ;
    }

    @Override
    protected void onStop() {

        ((GooshasmApp)getApplication()).notifyActivityOut();

        if (serviceConnection != null && serviceConnection.isAvailable())
            serviceConnection.service.notifyActivityOut();

        Intent intent2 = new Intent(this, EargasmService.class);
        startService(intent2);

        serviceConnection.unregisterSongDisplayer(nowPlayingFragment) ;
        unbindService(serviceConnection);

        Log.e(DEBUG_TAG, "Stopped") ;
        super.onStop();
    }

    // ============================= Page Construction ========================================== //

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        trackDatas = new ArrayList<>();
        albumDatas = new ArrayList<>();
        artistDatas = new ArrayList<>();

        this.serviceConnection = new MyServiceConnection2();

        // =============================================== //

        super.onCreate(savedInstanceState);

        dataCenter = new DataCenter(this);
        setContentView(R.layout.activity_main);

        viewPagerFragment = MainViewPagerFragment.newInstance();
        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction() ;
        fragmentTransaction.replace(R.id.main_layout_frame_layout, viewPagerFragment) ;
        fragmentTransaction.disallowAddToBackStack();
        fragmentTransaction.commit() ;

        contentFragment = viewPagerFragment;
        stateStack.push(MAIN_VIEWPAGER_STATE);

        // ============ The Navigation Drawer ============ //

        this.drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ImageView drawerImageView = (ImageView) findViewById(R.id.drawer_indicator);
        Resources resources = getResources();

        final DrawerArrowDrawable drawerArrowDrawable = new DrawerArrowDrawable(resources);
        drawerArrowDrawable.setStrokeColor(resources.getColor(R.color.colorAccent));
        drawerImageView.setImageDrawable(drawerArrowDrawable);

        drawer.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);

                offset = slideOffset;

                if (slideOffset >= .995) {
                    flipped = true;
                    drawerArrowDrawable.setFlip(flipped);
                } else if (slideOffset <= .005) {
                    flipped = false;
                    drawerArrowDrawable.setFlip(flipped);
                }

                drawerArrowDrawable.setParameter(offset);
            }
        });

        drawerImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AllegroActivity.this.drawer.isDrawerVisible(Gravity.RIGHT)) {
                    AllegroActivity.this.drawer.closeDrawer(Gravity.RIGHT);
                } else {
                    AllegroActivity.this.drawer.openDrawer(Gravity.RIGHT);
                }
            }
        });

        // =============================================== //

        cachedDataAvailable = dataCenter.cachedDataAvailable();
        setAdapters();

        if (!cachedDataAvailable) {
            scanLibrary();
        }

        // =============================================== //

        LinearLayout handle = (LinearLayout) findViewById(R.id.play_pause_handle);

        playPauseHandle = (LinearLayout) findViewById(R.id.slide_up_thingy);
        playPauseContent = (LinearLayout) findViewById(R.id.slide_up_thingy_content);

        playPauseButton = (Button) handle.findViewById(R.id.play_pause_button);
        playPauseBigText = (TextView) handle.findViewById(R.id.big_text);
        playPauseSmallText = (TextView) handle.findViewById(R.id.small_text);
        playPauseArtwork = (ImageView) handle.findViewById(R.id.artwork_container);

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePlayButtonClick();
            }
        });

        // =============================================== //

        pageTitleTextView = (TextView) findViewById(R.id.page_title_tv);
        pageTitleTextView.setTypeface(((GooshasmApp)getApplication()).getIranSans());
        setPageTitle(getResources().getString(R.string.app_title));

        // =============================================== //

        SlidingUpPanelLayout panelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        panelLayout.setPanelSlideListener(panelSlideListener);

        // =============================================== //

        Support.createInstance(this);

        if(nowPlayingFragment != null){
            fragmentTransaction = getSupportFragmentManager().beginTransaction();
            nowPlayingFragment = NowPlayingFragment.newInstance();
            fragmentTransaction.remove(nowPlayingFragment) ;
            fragmentTransaction.commit();
        }

        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        nowPlayingFragment = NowPlayingFragment.newInstance();
        fragmentTransaction.add(R.id.now_playing_fragment, nowPlayingFragment);
        fragmentTransaction.commit();

        // =============================================== //

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        prefButton = (ImageButton)toolbar.findViewById(R.id.action_bar_more_button) ;
        prefButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionsPopUp(prefButton);
            }
        });

        searchButton = (ImageButton)toolbar.findViewById(R.id.action_bar_search_button) ;
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        timerButton = (ImageButton)toolbar.findViewById(R.id.action_bar_timer_button) ;
        timerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CircularTimePickerFragment ctf = CircularTimePickerFragment.newInstance() ;
                ctf.show(getSupportFragmentManager() , "TAG");
            }
        });

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setHomeButtonEnabled(false);
        } else {
            Log.e(DEBUG_TAG, "ActionBar not found, This can cause unexpected behavior");
        }

        toolbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        // =============================================== //

        storeActivityState(true);
        Log.e(DEBUG_TAG, "Created") ;

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(serviceConnection.isAvailable()){
            if (serviceConnection.isVirgin()) {
                _hideHandle();
            }
        }

        if (serviceConnection.isAvailable()){
            serviceConnection.requestBroadcast();
            if (serviceConnection.queueAvailable()) {
                serviceConnection.requestUpdateDisplayers();
            }
        }

        viewPagerFragment.resetPosition();
        Log.e(DEBUG_TAG, "Resumed") ;

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(DEBUG_TAG, "Paused") ;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        storeActivityState(false);
        //ArtworkCache.getInstance(this).dispose();
        Log.e(DEBUG_TAG, "Destroyed");
    }

    // ====================================================== //

    private void initDrawerContent(){

        playlistsAdapter = new PlaylistsAdapter(this);
        ListView playlistListView = (ListView) findViewById(R.id.listview_playlists);

        if (cachedDataAvailable) {
            dataCenter.registerPlaylistListener(playlistsAdapter);
            dataCenter.iteratePlaylists();
        }

        playlistListView.setAdapter(playlistsAdapter);
        playlistListView.setOnItemClickListener(playlistClickListener);

    }

    private void setAdapters(){

        GridArtistListAdapter artistAdapter = new GridArtistListAdapter(AllegroActivity.this);
        ClassicAlbumListAdapter albumsAdapter = new ClassicAlbumListAdapter(AllegroActivity.this);
        SongsListAdapter2 songsAdapter = new SongsListAdapter2(AllegroActivity.this);

        if (cachedDataAvailable && !artistsAttached) {
            dataCenter.registerArtistListener(this);
            dataCenter.iterateArtists();
            artistsAttached = true;
        }

        if (cachedDataAvailable && !albumsAttached) {
            dataCenter.registerAlbumListener(this);
            dataCenter.iterateAlbums();
            albumsAttached = true;
        }

        if (cachedDataAvailable && !songsAttached) {
            dataCenter.registerTrackListener(this);
            dataCenter.iterateSongs();
            songsAttached = true;
        }

        viewPagerFragment.presetAdaptersAndListeners(artistAdapter , artistsItemClickListener
                , albumsAdapter , albumsItemClickListener
                , songsAdapter , songsItemClickListener);

        songsListAdapter = songsAdapter;
        albumsListAdapter = albumsAdapter;
        artistsListAdapter = artistAdapter;

        initDrawerContent();
    }

    // ============= List Item Click Callbacks ============== //

    AdapterView.OnItemClickListener playlistClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            PlayListData data = (PlayListData) view.getTag();
            log("XXXXXXX clicked playlist:", data.getName());
            showPlaylistFragment(data);
        }
    };

    AdapterView.OnItemClickListener artistsItemClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ArtistData artistData = (ArtistData) artistsListAdapter.getItem(position) ;
            showArtistInfoFragment(artistData);
        }
    };

    AdapterView.OnItemClickListener albumsItemClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Log.e("XXXXX", "XXXXX click!");

            /*
            log("YYYYYYYY initiate album data fetch for", albumDatas.get(position).getTitle());
            new AlbumProvider(AllegroActivity.this, albumDatas.get(position), new DummyAlbumDisplayer()).start();
            */

            AlbumData albumData = (AlbumData) albumsListAdapter.getItem(position) ;
            showAlbumInfoFragment(albumData);

        }
    };

    AdapterView.OnItemClickListener songsItemClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TrackData t = (TrackData) songsListAdapter.getItem(position);
            serviceConnection.loadAndPlay(songsListAdapter.getAllSongsList(), position);
        }
    };


    public void songClicked(TrackData data) {
        int i = 0;
        for (TrackData t:songsListAdapter.getAllSongsList()) {
            if (t.equals(data)){
                serviceConnection.loadAndPlay(songsListAdapter.getAllSongsList(), i);
                break;
            }
            i++;
        }
    }

    public void showAddToPlaylistDialog(TrackData trackData) {
        AddToPlaylistDialog dialog = AddToPlaylistDialog.create(this, trackData);
        dialog.show(getSupportFragmentManager(), DIALOG_ADD_TO_PLAYLIST);
    }

    private boolean scanRunning = false;

    private void scanLibrary(){

        scanRunning = true;

        dataCenter.registerAlbumListener(this);
        dataCenter.registerArtistListener(this);
        dataCenter.registerTrackListener(this);
        dataCenter.registerPlaylistListener(playlistsAdapter);
        dataCenter.populateDataSets(this);
    }

    @Override
    public void onScanFinished() {
        scanRunning = false;
    }

    public boolean isScanRunning() {
        return scanRunning;
    }

    public boolean isCachedDataAvailable() {
        return cachedDataAvailable;
    }

    // =============================== End page construction===================================== //

    // ============================== Activity Callbacks========================================= //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu) ;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void showOptionsPopUp(View v){
        if(nowPlayingOpen) {
            PopupMenu popup = new PopupMenu(this , v) ;
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.now_playing_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(this);
            popup.show();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){

            case R.id.action_open_album :
                showAlbumInfoFragment(serviceConnection.requestCurrentTrack().getAlbum());
                _hideNowPlaying();
                break ;
            case R.id.action_open_artist :
                showArtistInfoFragment(serviceConnection.requestCurrentTrack().getArtist());
                _hideNowPlaying();
                break ;

            case R.id.action_song_play_next :
                serviceConnection.setNextTrackQueue(popUpTrackData);
                break ;
            case R.id.action_song_add_queue :
                serviceConnection.addSongToQueue(popUpTrackData);
                break ;
            case R.id.action_song_add_play_list :
                break ;

            case R.id.action_album_play :
                serviceConnection.setQueueToAlbum(popUpAlbumData);
                break ;
            case R.id.action_album_add_queue :
                serviceConnection.addAlbumToQueue(popUpAlbumData);
                break ;
            case R.id.action_album_add_play_list :
                serviceConnection.addAlbumToPlayList(popUpAlbumData);
                break ;

            case R.id.action_artist_play :
                serviceConnection.setQueueToArtist(popUpArtistData);
                break ;
            case R.id.action_artist_add_queue :
                serviceConnection.addArtistToQueue(popUpArtistData);
                break ;
            case R.id.action_artist_add_play_list :
                serviceConnection.addArtistToPlayList(popUpArtistData);


        }
        return false;
    }


    public void showSongOptionsPopUp(View v , TrackData data){
        this.popUpTrackData = data ;
        PopupMenu popup = new PopupMenu(this , v) ;
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.song_context_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    public void showAlbumOptionsPopUp(View v , AlbumData data){
        this.popUpAlbumData = data ;
        PopupMenu popup = new PopupMenu(this , v) ;
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.album_context_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    public void showArtistOptionsPopUp(View v , ArtistData data){
        this.popUpArtistData = data ;
        PopupMenu popup = new PopupMenu(this , v) ;
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.artist_context_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    // ============================= UI Management ============================================== //

    private void _hideNowPlaying(){
        SlidingUpPanelLayout panelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        panelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    private void _switchUiToPlaying(){
        state = State.PLAYING;
        playPauseButton.setBackgroundResource(R.drawable.ic_pause_white_36dp);
    }

    private void _switchUiToPaused(){
        state = State.PAUSED;
        playPauseButton.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp);
    }

    private void _hideHandle() {

        SlidingUpPanelLayout panelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        panelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

    }

    private void _showHandle(){

        SlidingUpPanelLayout panelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        if (panelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
            panelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }

    }

    private void handlePlayButtonClick() {
        if (state == State.PAUSED) {
            _switchUiToPlaying();
            serviceConnection.playPlayer();
        } else {
            _switchUiToPaused();
            serviceConnection.pausePlayer();
        }
    }

    private SlidingUpPanelLayout.PanelSlideListener panelSlideListener = new SlidingUpPanelLayout.PanelSlideListener() {

        @Override
        public void onPanelSlide(View view, float v) {

        }

        @Override
        public void onPanelCollapsed(View view) {
            if (getPageTitle().equals(getResources().getString(R.string.now_playing)))
                setPageTitle(getPreviousPageTitle());
            nowPlayingOpen = false ;
            if(stateStack.peek().equals(NOW_PLAYING_STATE)){
                stateStack.pop() ;
            }
        }

        @Override
        public void onPanelExpanded(View view) {
            setPageTitle(getResources().getString(R.string.now_playing));
            stateStack.push(NOW_PLAYING_STATE) ;
            nowPlayingOpen = true ;
        }

        @Override
        public void onPanelAnchored(View view) {

        }

        @Override
        public void onPanelHidden(View view) {
            setPageTitle(getResources().getString(R.string.app_title));
            nowPlayingOpen = false ;
        }
    };

    // ============================= End UI Management ========================================== //

    // ================================ Misc ==================================================== //

    @Override
    public DataCenter getDataCenter() {
        return dataCenter;
    }

    @Override
    public void postOnUiThread(Runnable r) {
        handler.post(r);
    }

    // ========================================================================================== //

    public void showArtistInfoFragment(ArtistData artistData) {

        if(stateStack.peek().equals(NOW_PLAYING_STATE)){
            removeOldFragments() ;
            stateStack.clear() ;
            stateStack.push(MAIN_VIEWPAGER_STATE) ;
        }

        Log.e(DEBUG_TAG , "Adding artist") ;
        artistInfoFragment = ArtistViewPagerFragment.newInstance(artistData, this) ;
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.push_down_enter, R.anim.push_down_exit) ;
        fragmentTransaction.add(R.id.main_layout_frame_layout, artistInfoFragment) ;
        fragmentTransaction.disallowAddToBackStack() ;
        fragmentTransaction.commit() ;

        stateStack.push(ARTIST_INFO_STATE) ;
        lastVisitedArtist = artistData ;
        contentFragment = artistInfoFragment ;

    }

    public void showAlbumInfoFragment(AlbumData albumData) {

        if(stateStack.peek().equals(NOW_PLAYING_STATE)){
            removeOldFragments();
            stateStack.clear() ;
            stateStack.push(MAIN_VIEWPAGER_STATE) ;
        }

        Log.e(DEBUG_TAG , "Adding album") ;

        albumInfoFragment = AlbumViewPagerFragment.newInstance(albumData , this) ;
        FragmentTransaction fragmentTransaction ;

        if(stateStack.peek().equals(ARTIST_INFO_STATE)) {
            fragmentTransaction = getSupportFragmentManager().beginTransaction() ;
            fragmentTransaction.setCustomAnimations(R.anim.fade_out_exit, R.anim.fade_out_exit) ;
            fragmentTransaction.remove(artistInfoFragment) ;
            fragmentTransaction.disallowAddToBackStack() ;
            fragmentTransaction.commit() ;
            stateStack.pop() ;
        }

        fragmentTransaction = getSupportFragmentManager().beginTransaction() ;
        fragmentTransaction.setCustomAnimations(R.anim.push_down_enter, R.anim.push_down_exit) ;
        fragmentTransaction.add(R.id.main_layout_frame_layout, albumInfoFragment);
        fragmentTransaction.disallowAddToBackStack() ;
        fragmentTransaction.commit() ;

        stateStack.push(ALBUM_INFO_STATE) ;
        lastVisitedAlbumData = albumData ;
        contentFragment = albumInfoFragment ;

    }

    public void removeOldFragments(){
        if(stateStack.contains(ALBUM_INFO_STATE)){
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction() ;
            fragmentTransaction.setCustomAnimations(R.anim.fade_out_exit, R.anim.fade_out_exit) ;
            fragmentTransaction.remove(albumInfoFragment) ;
            fragmentTransaction.disallowAddToBackStack() ;
            fragmentTransaction.commit() ;
        }
        if(stateStack.contains(ARTIST_INFO_STATE)){
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction() ;
            fragmentTransaction.setCustomAnimations(R.anim.fade_out_exit, R.anim.fade_out_exit) ;
            fragmentTransaction.remove(artistInfoFragment) ;
            fragmentTransaction.disallowAddToBackStack() ;
            fragmentTransaction.commit() ;
        }
    }

    public void showPlaylistFragment(PlayListData playListData) {

        drawer.closeDrawers();

        if (stateStack.peek().equals(PLAYLIST_STATE) && playlistViewerFragment.getCurrentPlayList().equals(playListData)) {
            drawer.closeDrawers();
            return;
        }

        playlistViewerFragment = PlaylistViewerFragment.newInstance(this, playListData) ;
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction() ;
        fragmentTransaction.replace(R.id.main_layout_frame_layout, playlistViewerFragment) ;
        fragmentTransaction.disallowAddToBackStack() ;
        fragmentTransaction.commit() ;
        stateStack.push(PLAYLIST_STATE) ;
        contentFragment = playlistViewerFragment ;

    }

    public void setPageTitle(String title){
        _previousPageTitle_ = getPageTitle();
        pageTitleTextView.setText(title);
    }

    public String getPreviousPageTitle(){
        if (_previousPageTitle_ == null) {
            _previousPageTitle_ = getResources().getString(R.string.app_title);
        }
        return _previousPageTitle_;
    }

    public String getPageTitle() {
        return pageTitleTextView.getText().toString();
    }

    public void serviceConnectionPlay(){
        serviceConnection.requestToggleState();
    }

    public void serviceConnectionPause(){
        serviceConnection.requestToggleState();
    }

    public void serviceConnectionNext(){
        serviceConnection.requestNext();
    }

    public void serviceConnectionPrev() {
        serviceConnection.requestPrevious();
    }

    public void serviceConnectionRepeat(RepeatState state){
        serviceConnection.setRepeat(state);
    }

    public void serviceConnectionRequestQueueUpdate(int newPose , List<TrackData> data) {
        serviceConnection.updateQueue(data, newPose);
    }

    public void serviceConnectionRequestSeek(int progress){
        serviceConnection.requestSeek(progress);
    }

    public void requestServiceConnectionLoadAndPlay(int position , List<TrackData> data){
        serviceConnection.loadAndPlay(data, position);
    }

    public void serviceConnectionRequestTrack(int track){
        serviceConnection.gotoTrack(track);
    }

    public void serviceConnectionRequestSetTimer(int minutes){
        //TODO : implement
    }

    @Override
    public void onAlbumFound(final Album a) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                int i = 0;
                for (AlbumData x:albumDatas) {
                    if (x.getTitle().compareTo(a.title) > 0) {
                        break;
                    }
                    i++;
                }

                albumDatas.add(i, new AlbumData(a));
                albumsListAdapter.notifyDataSetChanged();
                albumsListAdapter.prepareIndexing();
            }
        });
    }

    @Override
    public void onArtistFound(final Artist a) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                int i = 0;
                for (ArtistData x:artistDatas) {
                    if (x.getName().compareTo(a.name) > 0) {
                        break;
                    }
                    i++;
                }

                artistDatas.add(i, new ArtistData(a));
                artistsListAdapter.notifyDataSetChanged();
                artistsListAdapter.prepareIndexing();
            }
        });
    }

    @Override
    public void onTrackFound(final TrackData t) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                int i = 0;
                for (TrackData x:trackDatas) {
                    if (x.getDisplayName().compareTo(t.getDisplayName()) > 0) {
                        break;
                    }
                    i++;
                }

                trackDatas.add(i, t);
                songsListAdapter.notifyDataSetChanged();
                songsListAdapter.prepareIndexing();
            }
        });
    }

    public List<TrackData> getSongList(){
        return trackDatas;
    }

    public List<ArtistData> getArtistList(){
        return artistDatas;
    }

    public List<AlbumData> getAlbumsList(){
        return albumDatas;
    }


    // ========================================================================== //

    public void notifyArtworkInvalid(AlbumData album) {
        dataCenter.notifyArtworkInvalid(album);
    }

    // ========================================================================== //

    @Override
    public void onBackPressed() {

        /*if (!drawer.isDrawerOpen(Gravity.RIGHT)){
            FragmentManager fm = getSupportFragmentManager();
            if(fm.getBackStackEntryCount() != 0) {
                fm.popBackStack();
                stateStack.pop();
                if (stateStack.peek().equals(MAIN_VIEWPAGER_STATE)){
                    setPageTitle(getResources().getString(R.string.app_title));
                }
            } else {
                super.onBackPressed();
            }
        } else {
            drawer.closeDrawers();
        }*/

        String top = stateStack.peek() ;
        if(top.equals(MAIN_VIEWPAGER_STATE)){
            // handle app exit
        } else {
            if(top.equals(NOW_PLAYING_STATE)){
                _hideNowPlaying();
                setTitle(getResources().getString(R.string.app_name));
                stateStack.pop() ;
            } else if(top.equals(ARTIST_INFO_STATE)){

                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction() ;
                fragmentTransaction.setCustomAnimations(R.anim.fade_in_enter, R.anim.fade_out_exit) ;
                fragmentTransaction.remove(artistInfoFragment) ;
                fragmentTransaction.disallowAddToBackStack() ;
                fragmentTransaction.commit() ;
                stateStack.clear();
                stateStack.push(MAIN_VIEWPAGER_STATE) ;

            } else if(top.equals(ALBUM_INFO_STATE)){
                stateStack.pop() ;
                String tmp = stateStack.peek() ;

                Log.e(DEBUG_TAG, "Removing Album") ;
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction() ;
                fragmentTransaction.setCustomAnimations(R.anim.fade_in_enter, R.anim.fade_out_exit) ;
                fragmentTransaction.remove(albumInfoFragment) ;
                fragmentTransaction.disallowAddToBackStack() ;
                fragmentTransaction.commit() ;

                if(tmp.equals(ARTIST_INFO_STATE)){
                    Log.e(DEBUG_TAG, "Adding Artist") ;
                    artistInfoFragment = ArtistViewPagerFragment.newInstance(lastVisitedArtist , this) ;
                    fragmentTransaction = getSupportFragmentManager().beginTransaction() ;
                    fragmentTransaction.setCustomAnimations(R.anim.fade_in_enter, R.anim.fade_in_enter) ;
                    fragmentTransaction.add(R.id.main_layout_frame_layout, artistInfoFragment);
                    fragmentTransaction.disallowAddToBackStack() ;
                    fragmentTransaction.commit() ;
                } else {
                    stateStack.clear();
                    stateStack.push(MAIN_VIEWPAGER_STATE) ;
                }
            }
        }
    }

    private void storeActivityState(boolean state){
        sharedpreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean(APP_STATE, state) ;
        editor.commit() ;
    }

    // ========================================================================== //

    private List<PlayListData> cachedPlaylistList = null;

    /* public void invalidatePlaylistList() {
        cachedPlaylistList = null;
    } */

    public void introduceNewPlaylist(PlayListData data) {
        cachedPlaylistList = null;
        playlistsAdapter.onPlaylistFound(data);
    }

    public List<PlayListData> getCachedPlaylistList() {
        return cachedPlaylistList;
    }

    public void fetchPlaylistList(PlaylistFetchListener callback) {
        new FetchPlaylistsTask(callback).execute();
    }

    private class FetchPlaylistsTask extends AsyncTask<Void, Void, List<PlayListData>> {

        PlaylistFetchListener callback = null;

        // TODO @Important if first scan, you should wait until playlists are scanned

        public FetchPlaylistsTask(PlaylistFetchListener callback) {
            this.callback = callback;
        }

        @Override
        protected List<PlayListData> doInBackground(Void... params) {
            return dataCenter.fetchPlaylists();
        }

        @Override
        protected void onPostExecute(List<PlayListData> playListDatas) {
            cachedPlaylistList = playListDatas;
            if (callback != null) {
                callback.onPlaylistsFetched(playListDatas);
            }
        }
    }

    // ========================================================================== //


}

