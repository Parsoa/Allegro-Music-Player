package quartet.allegro.async;

import quartet.allegro.database.Artist;

/**
 * Created by akbar on 5/25/15.
 */
public interface ArtistNotifyListener extends NotifyListener {
    public void onArtistFound(Artist a);
}
