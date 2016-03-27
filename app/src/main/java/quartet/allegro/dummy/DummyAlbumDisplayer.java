package quartet.allegro.dummy;

import java.util.List;

import quartet.allegro.communication.AlbumDisplayer;
import quartet.allegro.database.CoverImageData;
import quartet.allegro.database.MusicInfoData;
import quartet.allegro.database.TrackData;

import static quartet.allegro.AllegroActivity.log;

/**
 * Created by akbar on 7/4/15.
 */
public class DummyAlbumDisplayer implements AlbumDisplayer {
    @Override
    public void setMusicInfo(MusicInfoData info) {
        log("YYYYY info >>", info.getText());
    }

    @Override
    public void setSongList(List<TrackData> songList) {
        log("YYYYY >>>> tracks");
        for (TrackData t:songList) {
            log("YYYYY ", t.getDisplayName());
        }
    }

    @Override
    public void setCoverPhotos(List<CoverImageData> covers) {
        log("YYYYY >>>> covers");
        for (CoverImageData data:covers) {
            log("YYYYY", data.getPath());
        }
    }
}
