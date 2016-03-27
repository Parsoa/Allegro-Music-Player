package quartet.allegro.communication;

import java.util.List;

import quartet.allegro.database.CoverImageData;
import quartet.allegro.database.MusicInfoData;
import quartet.allegro.database.TrackData;

/**
 * Created by akbar on 7/1/15.
 */
public interface AlbumDisplayer {
    void setMusicInfo(MusicInfoData info);
    void setSongList(List<TrackData> songList);
    void setCoverPhotos(List<CoverImageData> covers);

}
