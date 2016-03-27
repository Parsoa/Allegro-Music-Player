package quartet.allegro.async;

import quartet.allegro.database.PlayListData;

/**
 * Created by akbar on 5/25/15.
 */
public interface PlaylistNotifyListener extends NotifyListener{
    public void onPlaylistFound(PlayListData pl);
}
