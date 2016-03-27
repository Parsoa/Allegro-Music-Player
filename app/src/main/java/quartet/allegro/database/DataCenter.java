package quartet.allegro.database;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import quartet.allegro.AllegroActivity;
import quartet.allegro.async.AlbumNotifyListener;
import quartet.allegro.async.ArtistNotifyListener;
import quartet.allegro.async.GenreNotifyListener;
import quartet.allegro.async.PlaylistNotifyListener;
import quartet.allegro.async.TrackNotifyListener;
import quartet.allegro.communication.ScanCycleListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static quartet.allegro.AllegroActivity.log;

/**
 * Created by akbar on 5/24/15.
 */
public class DataCenter {

    Context context;

    private HashMap<String, Artist> artistInstances;
    private HashMap<String, Album> albumInstances;

    ArrayList<TrackNotifyListener> trackNotifyListeners;
    ArrayList<AlbumNotifyListener> albumNotifyListeners;
    ArrayList<ArtistNotifyListener> artistNotifyListeners;
    ArrayList<GenreNotifyListener> genreNotifyListeners;
    ArrayList<PlaylistNotifyListener> playlistNotifyListeners;

    // ====== MACROS

    public static final String KEY_SCANNED_LIBRARY = "KEY_SCANNED_LIBRARY";

    // ====== table projections

    private final String[] projectionArtist = new String[]{MediaStore.Audio.ArtistColumns.ARTIST,
            MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS,
            MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS
    };

    private final String[] projectionAlbums = new String[]{
            MediaStore.Audio.AlbumColumns.ALBUM,
            MediaStore.Audio.AlbumColumns.ALBUM_ART,
            MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS,
            MediaStore.Audio.AlbumColumns.FIRST_YEAR,
            MediaStore.Audio.AlbumColumns.ARTIST
    };

    private final String[] projectionGenres = new String[] {
            MediaStore.Audio.GenresColumns.NAME,
            MediaStore.Audio.Genres._ID
    };

    // ======

    private void __init__() {
        trackNotifyListeners = new ArrayList<TrackNotifyListener>();
        albumNotifyListeners = new ArrayList<AlbumNotifyListener>();
        artistNotifyListeners = new ArrayList<ArtistNotifyListener>();
        genreNotifyListeners = new ArrayList<GenreNotifyListener>();
        playlistNotifyListeners = new ArrayList<>();
    }

    public DataCenter(Context context) {
        __init__();
        this.context = context;
    }

    public void registerTrackListener(TrackNotifyListener t){

        if (t == null)
            throw new IllegalArgumentException();

        if (!trackNotifyListeners.contains(t))
            trackNotifyListeners.add(t);
    }

    public void registerAlbumListener(AlbumNotifyListener t){

        if (t == null)
            throw new IllegalArgumentException();

        if (!albumNotifyListeners.contains(t))
            albumNotifyListeners.add(t);
    }

    public void registerArtistListener(ArtistNotifyListener t){

        if (t == null)
            throw new IllegalArgumentException();

        if (!artistNotifyListeners.contains(t))
            artistNotifyListeners.add(t);
    }

    public void registerGenreListener(GenreNotifyListener t){

        if (t == null)
            throw new IllegalArgumentException();

        if (!genreNotifyListeners.contains(t))
            genreNotifyListeners.add(t);
    }

    public void registerPlaylistListener(PlaylistNotifyListener t){

        if (t == null)
            throw new IllegalArgumentException();

        if (!playlistNotifyListeners.contains(t))
            playlistNotifyListeners.add(t);
    }

    // ======= SQL DB management

    public void clearDataSets() throws DataFetchException {

        // dropping all previous song meta data
        Track.deleteAll(Track.class);
        Album.deleteAll(Album.class);
        Artist.deleteAll(Artist.class);
        Genre.deleteAll(Genre.class);
    }

    private Artist getArtist(String artistName, ContentResolver cr, boolean save){

        if (artistInstances.containsKey(artistName)) {

            return artistInstances.get(artistName);

        } else {

            // querying media store for artist info
            Cursor cursorArtist = cr.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    projectionArtist, MediaStore.Audio.Artists.ARTIST+"=?",
                    new String[]{artistName}, null);

            int tracks;
            int albums;

            if (cursorArtist.moveToFirst()){
                albums = cursorArtist.getInt(1);
                tracks = cursorArtist.getInt(2);
            } else {
                albums = -1;
                tracks = -1;
            }

            cursorArtist.close();

            Artist artist = new Artist();
            artist.numberOfAlbums = albums;
            artist.numberOfTracks = tracks;
            artist.name = artistName;

            if (save) {
                artist.save();
            }


            for (ArtistNotifyListener anl:artistNotifyListeners){
                anl.onArtistFound(artist);
            }

            artistInstances.put(artistName, artist);

            return artist;
        }
    }


    private Album getAlbum(String albumName, Artist artist, ContentResolver cr, boolean save){

        if (albumInstances.containsKey(albumName)) {

            return albumInstances.get(albumName);

        } else {

            // querying media store for album info
            Cursor cursorAlbums = cr.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    projectionAlbums, MediaStore.Audio.Albums.ALBUM+"=?",
                    new String[]{albumName}, null);

            String albumArtUri;
            int numberOfSongs;
            int year;
            String artistName;

            if (cursorAlbums.moveToFirst()){

                albumArtUri = cursorAlbums.getString(1);
                numberOfSongs = cursorAlbums.getInt(2);
                year = cursorAlbums.getInt(3);
                artistName = cursorAlbums.getString(4);

            } else {

                albumArtUri = null;
                numberOfSongs = -1;
                year = -1;
                artistName = null;
            }

            cursorAlbums.close();

            Album album = new Album();

            album.title = albumName;
            album.albumArtPath = albumArtUri;
            album.year = year;
            album.numberOfSongs = numberOfSongs;

            // TODO check if querying for previous photos is wise

            // storing path to current album's album art in a separate table
            CoverImage coverImage = new CoverImage();

            coverImage.path = albumArtUri;
            coverImage.album = album;
            coverImage.artist = artist;
            coverImage.imageType = CoverImage.TYPE_ALBUM_COVER;

            coverImage.save();


            if (artistName != null) {
                album.artist = getArtist(artistName, cr, true);
            }

            if (save){
                album.save();
            }

            for (AlbumNotifyListener afl:albumNotifyListeners){
                afl.onAlbumFound(album);
            }

            albumInstances.put(albumName, album);

            return album;
        }
    }


    private void makeToast(final String str){

        if (context == null)
            return ;

        ((AllegroActivity)context).postOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(DataCenter.this.context, str,
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    log("XXXXXX unable to make toast");
                }
            }
        });
    }

    private void _fetchLocalPlayLists(){

        ContentResolver cr = context.getContentResolver();

        // querying for all play lists
        String[] projection_playlists = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME
        };

        String[] projection_songs = {
                MediaStore.Audio.Playlists.Members._ID,
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.PLAY_ORDER,
                MediaStore.Audio.Playlists.Members.TITLE
        };

        Cursor cursor = cr.query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                projection_playlists, null, null, null);

        // iterating through play lists
        if (cursor.moveToFirst()) {
            do {
                long playlist_id = cursor.getLong(0);
                String playlist_name = cursor.getString(1);

                // creating playlist object
                PlayList playList = new PlayList();
                playList.name = playlist_name;
                playList.playlistId = playlist_id;
                playList.count = 0;
                playList.save();

                // querying for current playlist songs
                Cursor songCursor = cr.query(MediaStore.Audio.Playlists.Members.getContentUri("external", playlist_id),
                        projection_songs, MediaStore.Audio.Media.IS_MUSIC + "!=0", null, null
                );

                log("XXXXXXX finding songs of playlist", playList.name);

                if (songCursor.moveToFirst()){
                    do {
                        playList.count ++;

                        // using an intermediate table to handle many to many linkage
                        // between songs and play lists

                        long audio_id = songCursor.getLong(1);
                        int playOrder = songCursor.getInt(2);
                        String displayName = songCursor.getString(3);

                        SongPlayListMap songPlayListMap = new SongPlayListMap();
                        songPlayListMap.audioId = audio_id;
                        songPlayListMap.playList = playList;
                        songPlayListMap.playOrder = playOrder;
                        songPlayListMap.save();

                        log("HHHHHHHH playlist", playlist_name, "->", audio_id, playOrder, displayName);

                    } while (songCursor.moveToNext());
                }

                // saving the playlist (duh)
                playList.save();

                // notify the listeners new playlist is found
                for (PlaylistNotifyListener pnl:playlistNotifyListeners){
                    pnl.onPlaylistFound(new PlayListData(playList));
                }

                songCursor.close();

            } while (cursor.moveToNext());
        }

    }

    public void populateDataSets(final ScanCycleListener callback) {

        Executor exec = Executors.newSingleThreadExecutor();

        exec.execute(new Runnable() {

            @Override
            public void run() {
                long __d_tstart = System.currentTimeMillis();

                artistInstances = new HashMap<String, Artist>();
                albumInstances = new HashMap<String, Album>();

                // ====== getting in touch with content resolver

                ContentResolver cr = context.getContentResolver();

                if (cr == null) {

                    makeToast("خطا در اسکن کردن آهنگ ها");

                    return;

                } else {

                    makeToast("در حال اسکن کردن آهنگ ها...");

                }

                String[] projectionSongs = new String[]{
                        MediaStore.Audio.AudioColumns.IS_MUSIC,
                        MediaStore.Audio.AudioColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ALBUM,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AudioColumns.DURATION,
                        MediaStore.Audio.AudioColumns._ID,
                        MediaStore.Audio.AudioColumns.DATA,
                        MediaStore.Audio.AudioColumns.TRACK

                };

                String[] projectionGenreMembers = new String[]{
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE
                };


                // ====== querying for genres

                HashMap<Long, Genre> genres = new HashMap<Long, Genre>();

                Cursor c = cr.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, projectionGenres, null, null, null);

                if (c != null && c.moveToFirst()) {
                    do {

                        String name = c.getString(0);
                        long id = c.getLong(1);

                        log("found genre", name, id);

                        Genre genre = new Genre();
                        genre.title = name;
                        genre.save();

                        for (GenreNotifyListener gnl : genreNotifyListeners) {
                            gnl.onGenreFound(genre);
                        }

                        genres.put(id, genre);

                    } while (c.moveToNext());
                }

                // ====== getting all songs from each genre

                HashMap<Long, Genre> trackGenreMap = new HashMap<Long, Genre>();

                for (Long genreID : genres.keySet()) {

                    Genre g = genres.get(genreID);

                    Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external", genreID);
                    Cursor cur = cr.query(uri, projectionGenreMembers, null, null, null);

                    if (cur.moveToFirst()) {
                        do {

                            String name = cur.getString(1);
                            long id = cur.getLong(0);

                            trackGenreMap.put(id, g);

                        } while (cur.moveToNext());
                    }
                }

                log("Genre process took", System.currentTimeMillis() - __d_tstart, "milliseconds");

                long __d_total_db_time = 0;

                // ====== iterating through the tracks

                c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionSongs, null, null, null);

                if (c != null && c.moveToFirst()) {
                    do {

                        // check if track is music
                        int isMusic = c.getInt(0);
                        if (isMusic == 0)
                            continue;

                        // fetching track info
                        String name = c.getString(1);
                        String albumName = c.getString(2);
                        String artistName = c.getString(3);
                        long duration = c.getLong(4);
                        long songId = c.getLong(5);
                        String path = c.getString(6);
                        int trackNum = c.getInt(7);

                        // making null album and artist labeled "Unknown"
                        if (albumName == null || albumName.isEmpty()) {
                            albumName = "Unknown Album";
                        }

                        if (artistName == null || artistName.isEmpty()) {
                            artistName = "Unknown Artist";
                        }

                        // finding artist instance, creating one if none
                        Artist artist = getArtist(artistName, cr, true);
                        Album album = getAlbum(albumName, artist, cr, true);
                        Genre genre = genres.get(songId);

                        Track track = new Track();
                        track.displayName = name;
                        track.artist = artist;
                        track.album = album;
                        track.duration = duration;
                        track.audioId = songId;
                        track.genre = genre;
                        track.path = path;
                        track.indexInAlbum = trackNum;

                        // storing track meta data
                        long __d_st = System.currentTimeMillis();
                        track.save();
                        __d_total_db_time += System.currentTimeMillis() - __d_st;

                        for (TrackNotifyListener tfl : trackNotifyListeners) {
                            tfl.onTrackFound(new TrackData(track));
                        }

                    } while (c.moveToNext());
                }

                if (c != null)
                    c.close();

                Info.deleteAll(Info.class, "key=?", KEY_SCANNED_LIBRARY);

                Info info = new Info();
                info.key = KEY_SCANNED_LIBRARY;
                info.value = "true";
                info.save();

                makeToast("گوشاسم آهنگ های شما را اسکن کرد");

                log("XXXXXX The entire process took", System.currentTimeMillis() - __d_tstart, "milliseconds");
                log("XXXXXX", __d_total_db_time, "of which being insert time");


                // getting play lists
                _fetchLocalPlayLists();

                if (callback != null) {
                    callback.onScanFinished();
                }

            }
        });


    }

    // ======= Async Interface for Querying database

    Executor getDatabaseExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    public void iteratePlaylists(){

        Executor dbThread = getDatabaseExecutor();

        dbThread.execute(new Runnable() {
            @Override
            public void run() {

                Iterator<PlayList> a = PlayList.findAsIterator(PlayList.class, null, null, null, "name", null);

                while (a.hasNext()) {
                    for (PlaylistNotifyListener nl: playlistNotifyListeners)
                        nl.onPlaylistFound(new PlayListData(a.next()));
                }
            }
        });

    }

    public void iterateAlbums(){

        Executor dbThread = getDatabaseExecutor();

        dbThread.execute(new Runnable() {
            @Override
            public void run() {

                Iterator<Album> a = Album.findAsIterator(Album.class, null, null, null, "title", null);

                while (a.hasNext()) {
                    for (AlbumNotifyListener nl: albumNotifyListeners)
                        nl.onAlbumFound(a.next());
                }
            }
        });

    }

    public void iterateArtists(){

        Executor dbThread = getDatabaseExecutor();

        dbThread.execute(new Runnable() {
            @Override
            public void run() {

                Iterator<Artist> a = Artist.findAsIterator(Artist.class, null, null, null, "name", null);

                while (a.hasNext()) {
                    for (ArtistNotifyListener nl: artistNotifyListeners)
                        nl.onArtistFound(a.next());
                }

            }
        });
    }

    public void iterateSongs() {

        Executor dbThread = getDatabaseExecutor();

        dbThread.execute(new Runnable() {
            @Override
            public void run() {

                Iterator<Track> a = Track.findAsIterator(Track.class, null, null, null, "display_name", null);

                while (a.hasNext()) {
                    for (TrackNotifyListener nl: trackNotifyListeners)
                        nl.onTrackFound(new TrackData(a.next()));
                }

            }
        });
    }

    public List<AlbumData> fetchArtistAlbums(ArtistData artist)  {

        // querying
        List<Album> albums = Album.find(Album.class, "artist=?", new String[]{String.valueOf(artist.getId())}, null, "title", null);

        // making un-editable list
        ArrayList<AlbumData> res = new ArrayList<>();
        for (Album album:albums) {
            res.add(new AlbumData(album));
        }

        return res;
    }

    public List<TrackData> fetchArtistSongs(ArtistData artist) {

        // querying
        List<Track> tracks = Track.find(Track.class, "artist=?", new String[]{String.valueOf(artist.getId())}, null, null, null);

        Collections.sort(tracks, new Comparator<Track>() {
            @Override
            public int compare(Track lhs, Track rhs) {
                int res1 = lhs.album.title.compareTo(rhs.album.title);
                if (res1 == 0) {
                    return Integer.valueOf(lhs.indexInAlbum).compareTo(rhs.indexInAlbum);
                }
                return res1;
            }
        });

        // making un-editable list
        ArrayList<TrackData> res = new ArrayList<>(tracks.size());
        for (Track track:tracks) {
            res.add(new TrackData(track));
        }

        return res;

    }

    public MusicInfoData fetchArtistInfo(ArtistData artist) {

        // querying
        List<MusicInfo> query = MusicInfo.find(MusicInfo.class, "artist=?", new String[]{String.valueOf(artist.getId())}, null, null, "1");

        if (!query.isEmpty())
             return  new MusicInfoData(query.get(0));

        return null;

    }

    public List<CoverImageData> fetchArtistCoverImages(ArtistData artist) {

        // querying
        List<CoverImage> images = CoverImage.find(CoverImage.class, "artist=?", new String[]{String.valueOf(artist.getId())}, null, null, null);

        ArrayList<CoverImageData> coverImageDatas = new ArrayList<>();
        for (CoverImage image:images) {
            coverImageDatas.add(new CoverImageData(image));
        }

        return coverImageDatas;
    }

    public List<TrackData> fetchAlbumSongs(AlbumData album)  {

        // querying
        List<Track> tracks = Track.find(Track.class, "album=?", new String[]{String.valueOf(album.getId())}, null, "index_in_album", null);

        // making un-editable list
        ArrayList<TrackData> res = new ArrayList<>(tracks.size());
        for (Track t:tracks) {
            res.add(new TrackData(t));
        }

        return res;
    }

    public MusicInfoData fetchAlbumInfo(AlbumData album) {

        // querying
        List<MusicInfo> query = MusicInfo.find(MusicInfo.class, "album=?", new String[]{String.valueOf(album.getId())}, null, null, "1");

        if (!query.isEmpty())
            return new MusicInfoData(query.get(0));

        return null;

    }

    public List<CoverImageData> fetchAlbumCoverImages(AlbumData albums) {

        // querying
        List<CoverImage> images = CoverImage.find(CoverImage.class, "album=?", new String[]{String.valueOf(albums.getId())}, null, null, null);

        ArrayList<CoverImageData> coverImageDatas = new ArrayList<>();
        for (CoverImage image:images) {
            coverImageDatas.add(new CoverImageData(image));
        }

        return coverImageDatas;
    }

    public List<TrackData> fetchPlaylistItems(long playlistSugarId) {

        log("XXXXXX getting playlists");

        List<SongPlayListMap> songMappers = SongPlayListMap.find(SongPlayListMap.class,
                "play_list = ?", new String[]{String.valueOf(playlistSugarId)}, null, "play_order", null);

        ArrayList<TrackData> res = new ArrayList<>();

        // TODO vaghti ye track inja peida mishe, manteghie bara dafe baad save beshe ya aslan az avval Track find beshe

        for (SongPlayListMap mapper:songMappers) {
            String ids = String.valueOf(mapper.audioId);
            List<Track> tracks = Track.find(Track.class, "audio_id=?", new String[]{ids}, null, null, "1"); // TODO might be empty, residigi shavad
            res.add(new TrackData(tracks.get(0)));
        }

        return res;
    }

    public List<PlayListData> fetchPlaylists() {

        List<PlayList> query = PlayList.find(PlayList.class, null, null, null, "name", null);

        ArrayList<PlayListData> res = new ArrayList<>();

        for (PlayList p:query) {
            res.add(new PlayListData(p));
        }

        return res;

    }

    // ======= playlists interface

    public PlayListData addPlaylist(String name) {

        if (name == null) {
            throw new IllegalArgumentException("playlist name can not be null");
        }

        Iterator<PlayList> c = PlayList.findAsIterator(PlayList.class, null, null);
        boolean failure = false;
        while (c.hasNext()) {
            PlayList p = c.next();
            if (p.name != null && p.name.equals(name)) {
                failure = true;
                break;
            }
        }

        if (failure) {
            return null;
        }

        PlayList playList = new PlayList();
        playList.name = name;
        playList.count = 0;
        playList.playlistId = -1; // TODO sync with device playlists and fix use real id here
        playList.save();

        return new PlayListData(playList);
    }

    public void addToPlaylist(List<TrackData> tracks, PlayListData playlist) {

        PlayList pl = playlist.associatedPlatlist;

        for (TrackData track:tracks) {
            SongPlayListMap map = new SongPlayListMap();
            map.audioId = track.getAudioId();
            map.playOrder = pl.count;
            map.playList = pl;

            pl.count ++;
            map.save();
        }

        pl.save();
    }

    // ======= interface methods for altering database

    public boolean updatePlaylistOrder(PlayListData playListData, final List<TrackData> newOrder,
                                       final List<TrackData> pastOrder) {

        log("XXXXXX update playlist ");

        // TODO put a try-catch later for errors, in case of error, try correction and return false

        List<SongPlayListMap> songMappers = SongPlayListMap.find(SongPlayListMap.class,
                "play_list = ?", new String[]{String.valueOf(playListData.getSugarId())}, null, null, null);

        for (SongPlayListMap mapper:songMappers) {

            boolean found = false;

            for (int i=0; i<newOrder.size(); i++) {
                if (newOrder.get(i).getAudioId() == mapper.audioId) {
                    found = true;
                    mapper.playOrder = i;
                    break;
                }
            }

            if (!found){
                Log.e("ALLEGRO_DATABASE", "XXXXX ERROR: in playlist"+" "+ playListData.getName()
                        +" "+ "song"+" "+mapper.audioId+" "+ "not found in database");
            } else {
                log("XXXXXXX updating order of track", mapper.audioId, "to", mapper.playOrder);
            }

            mapper.save();
        }

        // TODO also apply device-wide?

        return true;

    }

    public void removeTrackFromPlaylist(TrackData track, PlayListData playList) {

        SongPlayListMap.deleteAll(SongPlayListMap.class, "audio_id=? and play_list=?",
                String.valueOf(track.getAudioId()), String.valueOf(playList.getSugarId()));

    }

    // ======= info

    public boolean cachedDataAvailable(){
        List<Info> res = Info.find(Info.class, "key=?", KEY_SCANNED_LIBRARY);
        for (Info info:res) {
            if (!info.value.equals("false"))
                return true;
        }
        return false;
    }

    public void notifyArtworkInvalid(AlbumData album) {
        List<CoverImage> coverImages = CoverImage.find(CoverImage.class, "album=?", album.getId().toString());
        for (CoverImage coverImage:coverImages){
            coverImage.delete();
        }
    }
}
