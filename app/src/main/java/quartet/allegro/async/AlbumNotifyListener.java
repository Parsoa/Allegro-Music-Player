package quartet.allegro.async;

import quartet.allegro.database.Album;

/**
 * Created by akbar on 5/25/15.
 */
public interface AlbumNotifyListener extends NotifyListener {
    public void onAlbumFound(Album a);
}
