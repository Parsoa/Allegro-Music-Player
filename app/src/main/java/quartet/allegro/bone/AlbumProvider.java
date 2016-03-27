package quartet.allegro.bone;

import android.app.Activity;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import quartet.allegro.AllegroActivity;
import quartet.allegro.communication.AlbumDisplayer;
import quartet.allegro.database.AlbumData;
import quartet.allegro.database.CoverImageData;
import quartet.allegro.database.MusicInfoData;
import quartet.allegro.database.TrackData;

/**
 * Created by akbar on 7/2/15.
 */
public class AlbumProvider {

    // ========================================================================================== //

    private Executor executor;

    private AllegroActivity activity;
    private AlbumData albumData;
    private AlbumDisplayer albumDisplayer;

    public AlbumProvider(AllegroActivity activity, AlbumData albumData, AlbumDisplayer albumDisplayer) {

        if (albumDisplayer == null) {
            throw new IllegalArgumentException("album displayer passed to constructor is null");
        }

        this.activity = activity;
        this.albumData = albumData;
        this.albumDisplayer = albumDisplayer;

    }

    public void start(){
        executor = Executors.newSingleThreadExecutor();
        executor.execute(fetchDataRunnable);
    }

    private Runnable fetchDataRunnable = new Runnable() {
        @Override
        public void run() {

            List<CoverImageData> covers = activity.getDataCenter().fetchAlbumCoverImages(albumData);
            albumDisplayer.setCoverPhotos(covers);

            List<TrackData> tracks = activity.getDataCenter().fetchAlbumSongs(albumData);
            albumDisplayer.setSongList(tracks);

            MusicInfoData info = activity.getDataCenter().fetchAlbumInfo(albumData);
            if (info == null) {
                // TODO initiate fetch from net
            } else {
                albumDisplayer.setMusicInfo(info);
            }

        }
    };

}
