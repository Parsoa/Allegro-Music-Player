package quartet.allegro.communication;

import quartet.allegro.database.AlbumData;

/**
 * Created by akbar on 7/1/15.
 */
public interface AlbumDataProvider {
    void registerDisplayer(AlbumData album, AlbumDisplayer displayer);
}
