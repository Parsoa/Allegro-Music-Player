package quartet.allegro.async;

import quartet.allegro.database.TrackData;

/**
 * Created by akbar on 5/25/15.
 */
public interface TrackNotifyListener extends NotifyListener {
    public void onTrackFound(TrackData t);
}
