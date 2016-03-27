package quartet.allegro.database;

/**
 * Created by akbar on 6/17/15.
 */
public class CoverImageData {

    private CoverImage correspondingCoverImage;

    public CoverImageData(CoverImage correspondingCoverImage) {
        this.correspondingCoverImage = correspondingCoverImage;
    }

    public String getPath() {
        return correspondingCoverImage.path;
    }

    public ArtistData getArtist() {
        return new ArtistData(correspondingCoverImage.artist);
    }

    public AlbumData getAlbum() {
        return new AlbumData(correspondingCoverImage.album);
    }
}
