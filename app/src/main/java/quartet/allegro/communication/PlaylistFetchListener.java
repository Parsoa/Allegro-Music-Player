package quartet.allegro.communication;

import java.util.List;

import quartet.allegro.database.PlayListData;

/**
 * Created by akbar on 7/4/15.
 */
public interface PlaylistFetchListener {
    void onPlaylistsFetched(List<PlayListData> playLists);
}
