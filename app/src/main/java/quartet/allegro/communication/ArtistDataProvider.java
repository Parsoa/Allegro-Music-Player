package quartet.allegro.communication;

import quartet.allegro.database.ArtistData;

/**
 * Created by akbar on 7/1/15.
 */
public interface ArtistDataProvider {
    void registerDisplayer(ArtistData artist, ArtistDisplayer artistDisplayer);
}
