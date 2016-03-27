package quartet.allegro.bone;

import quartet.allegro.database.TrackData;

/**
 * Created by akbar on 5/28/15.
 */
public interface MusicPlayerUI {
    public void updateUI(EargasmService.EargasmState state, float progress, TrackData track);
//    public void updateUI(PlayerState state, float progress, Track track);
}
