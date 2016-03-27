package quartet.allegro.database;

/**
 * Created by Parsoa on 6/9/15.
 */
public class ArtistData {

    private Artist correspondingArtist;

    public ArtistData(Artist correspondingArtist) {
        this.correspondingArtist = correspondingArtist;
    }

    public String getName() {
        return correspondingArtist.name;
    }

    public int getNumberOfTracks() {
        return correspondingArtist.numberOfTracks;
    }

    public int getNumberOfAlbums() {
        return correspondingArtist.numberOfAlbums;
    }

    public long getId(){
        return correspondingArtist.getId();
    }

    public String getArtworkUri() {
        return correspondingArtist.artworkUri;
    }
}
