package quartet.allegro.database;

import com.orm.SugarRecord;

/**
 * Created by akbar on 5/24/15.
 */
public class Album extends SugarRecord<Album> {

    public String title;
    public String albumArtPath;
    public int year;
    public int numberOfSongs;
    public Artist artist;
}
