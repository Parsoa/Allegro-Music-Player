package quartet.allegro.database;

import com.orm.SugarRecord;

/**
 * Created by akbar on 5/24/15.
 */
public class Artist extends SugarRecord<Artist> {

    public String name;
    public int numberOfTracks;
    public int numberOfAlbums;

    public String artworkUri;

}
