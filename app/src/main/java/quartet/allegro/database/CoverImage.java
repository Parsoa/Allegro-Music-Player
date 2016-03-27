package quartet.allegro.database;

import com.orm.SugarRecord;

/**
 * Created by akbar on 6/17/15.
 */
public class CoverImage extends SugarRecord<CoverImage> {

    static final int TYPE_ALBUM_COVER = 0;
    static final int TYPE_ARTIST = 1;

    String path;
    int imageType;
    Artist artist;
    Album album;


}
