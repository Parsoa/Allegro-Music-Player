package quartet.allegro.database;

import com.orm.SugarRecord;

/**
 * Created by akbar on 6/18/15.
 */
public class PlayList extends SugarRecord<PlayList>{
    long playlistId;
    String name;
    int count;
}
