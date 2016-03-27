package quartet.allegro.async;

import quartet.allegro.database.Genre;

/**
 * Created by akbar on 5/25/15.
 */
public interface GenreNotifyListener extends NotifyListener{
    public void onGenreFound(Genre g);
}
