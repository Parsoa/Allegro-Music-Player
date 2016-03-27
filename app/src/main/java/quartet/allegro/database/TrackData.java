package quartet.allegro.database;

/**
 * Created by Parsoa on 6/9/15.
 */
public class TrackData {

    private Track correspondingTrack;

    public boolean playNext;

    public TrackData(Track correspondingTrack) {
        this.correspondingTrack = correspondingTrack;
        this.playNext = false;
    }

    public long getId(){
        return correspondingTrack.getId();
    }

    public String getDisplayName() {
        return correspondingTrack.displayName;
    }

    public long getDuration() {
        return correspondingTrack.duration;
    }

    public ArtistData getArtist() {
        return new ArtistData(correspondingTrack.artist);
    }

    public AlbumData getAlbum() {
        return new AlbumData(correspondingTrack.album);
    }

    public Genre getGenre() {
        return correspondingTrack.genre;
    }

    public long getAudioId() {
        return correspondingTrack.audioId;
    }

    public String getPath() {
        return correspondingTrack.path;
    }
}
