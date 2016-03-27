package quartet.allegro.database;

/**
 * Created by akbar on 7/1/15.
 */
public class MusicInfoData {

    private MusicInfo correspondingMusicInfo;

    public MusicInfoData(MusicInfo correspondingMusicInfo) {
        this.correspondingMusicInfo = correspondingMusicInfo;
    }

    public int getType() {
        return correspondingMusicInfo.type;
    }

    public String getText() {
        return correspondingMusicInfo.text;
    }

    public Artist getArtist() {
        return correspondingMusicInfo.artist;
    }

    public Album getAlbum() {
        return correspondingMusicInfo.album;
    }
}
