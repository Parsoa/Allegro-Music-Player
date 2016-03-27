package quartet.allegro.database;

import com.orm.SugarRecord;

public class Track extends SugarRecord<Track> {

    public String displayName;

    public long duration;

    public Artist artist;
    public Album album;
    public Genre genre;

    public long audioId;
    public int indexInAlbum;

    public String path;

}
