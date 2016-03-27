package quartet.allegro.database;

import com.orm.SugarRecord;

/**
 * Created by akbar on 7/1/15.
 */
public class MusicInfo extends SugarRecord<MusicInfo> {

    public static final int TYPE_ARTIST = 0;
    public static final int TYPE_ALBUM = 1;

    public int type;
    public String text;
    public Artist artist;
    public Album album;
}
