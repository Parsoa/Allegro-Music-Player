package quartet.allegro.dummy;

import java.util.List;

import quartet.allegro.communication.ArtistDisplayer;
import quartet.allegro.database.AlbumData;
import quartet.allegro.database.CoverImageData;
import quartet.allegro.database.MusicInfoData;
import quartet.allegro.database.TrackData;

import static quartet.allegro.AllegroActivity.log;

/**
 * Created by akbar on 7/2/15.
 */
public class DummyArtistDisplayer implements ArtistDisplayer {
    @Override
    public void setCoverPhotos(List<CoverImageData> covers) {
        for (CoverImageData coverImageData:covers) {
            log("YYYYY >", coverImageData.getPath());
        }
    }

    @Override
    public void setAlbumList(List<AlbumData> albumList) {
        for (AlbumData albumData: albumList) {
            log("YYYYY >", albumData.getTitle());
        }
    }

    @Override
    public void setMusicInfo(MusicInfoData info) {
        log("YYYYY >", info.getText());
    }

    @Override
    public void setSongList(List<TrackData> songList) {
        for (TrackData data:songList){
            log("YYYYY >", data.getDisplayName());
        }
    }
}
