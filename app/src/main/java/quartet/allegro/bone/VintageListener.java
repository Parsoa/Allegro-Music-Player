package quartet.allegro.bone;

import quartet.allegro.PlayerState;

import quartet.allegro.database.Track;

/**
 * Created by akbar on 6/1/15.
 */
public interface VintageListener {
   public void updateUI(PlayerState state, float progress, Track track);
}
