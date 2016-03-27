package quartet.allegro.database;

import com.orm.SugarRecord;

/**
 * Created by akbar on 6/18/15.
 */
public class SongPlayListMap extends SugarRecord<SongPlayListMap>{
    int playOrder;
    long audioId;
    PlayList playList;
}
