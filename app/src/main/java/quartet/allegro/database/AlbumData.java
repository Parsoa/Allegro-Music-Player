package quartet.allegro.database;

/**
 * Created by Parsoa on 6/9/15.
 */
public class AlbumData {

    private Album correspondingAlbum;

    public AlbumData(Album correspondingAlbum) {
        this.correspondingAlbum = correspondingAlbum;
    }

    public String getTitle() {
        return correspondingAlbum.title;
    }

    public String getAlbumArtPath() {
        return correspondingAlbum.albumArtPath;
    }

    public int getYear() {
        return correspondingAlbum.year;
    }

    public int getNumberOfSongs() {
        return correspondingAlbum.numberOfSongs;
    }

    public ArtistData getArtist() {
        return new ArtistData(correspondingAlbum.artist);
    }

    public Long getId(){
        return correspondingAlbum.getId();
    }

    @Override
    public boolean equals(Object o) {
        try {
            return  correspondingAlbum == ((AlbumData)o).correspondingAlbum;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return super.toString() + ":" + correspondingAlbum.toString();
    }
}
