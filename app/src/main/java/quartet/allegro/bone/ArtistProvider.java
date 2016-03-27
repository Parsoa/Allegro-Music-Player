package quartet.allegro.bone;

import android.app.Activity;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import quartet.allegro.AllegroActivity;
import quartet.allegro.R;
import quartet.allegro.communication.ArtistDisplayer;
import quartet.allegro.database.AlbumData;
import quartet.allegro.database.ArtistData;
import quartet.allegro.database.CoverImageData;
import quartet.allegro.database.MusicInfo;
import quartet.allegro.database.MusicInfoData;
import quartet.allegro.database.TrackData;

/**
 * Created by akbar on 7/2/15.
 */
public class ArtistProvider {

    // ========================================================================================== //

    private Executor executor;

    private AllegroActivity activity;
    private ArtistData artistData;
    private ArtistDisplayer artistDisplayer;

    public ArtistProvider(AllegroActivity activity, ArtistData artistData, ArtistDisplayer artistDisplayer) {

        if (artistDisplayer == null) {
            throw new IllegalArgumentException("artist displayer passed to constructor is null");
        }

        this.activity = activity;
        this.artistData = artistData;
        this.artistDisplayer = artistDisplayer;
    }

    public void start(){
        executor = Executors.newSingleThreadExecutor();
        executor.execute(fetchDataRunnable);
    }

    private Runnable fetchDataRunnable = new Runnable() {
        @Override
        public void run() {

            List<CoverImageData> covers = activity.getDataCenter().fetchArtistCoverImages(artistData);
            artistDisplayer.setCoverPhotos(covers);

            List<AlbumData> albums = activity.getDataCenter().fetchArtistAlbums(artistData);
            artistDisplayer.setAlbumList(albums);

            List<TrackData> tracks = activity.getDataCenter().fetchArtistSongs(artistData);
            artistDisplayer.setSongList(tracks);

            MusicInfoData info = activity.getDataCenter().fetchArtistInfo(artistData);
            if (info == null) {
                // TODO initiate fetch from net
                MusicInfo dummy = new MusicInfo();
                dummy.text = activity.getResources().getString(R.string.default_artist_info);
                info = new MusicInfoData(dummy);
                artistDisplayer.setMusicInfo(info);
            } else {
                artistDisplayer.setMusicInfo(info);
            }

        }
    };

}
