package quartet.allegro.communication;
import quartet.allegro.database.TrackData;

import java.util.List;

/**
 * Created by akbar on 6/3/15.
 */
public interface SongDisplayer {

    // loads a new list of tracks and starts at given index
    public void loadQueue(List<TrackData> trackList, int initialPosition);

    // whilst playing the queue, changes the order of songs and also
    // gives the position of currently playing song in new queue order
    public void updateQueue(List<TrackData> trackList, int newPosition);

    // asks interface to pause
    public void pause();

    // asks interface to play
    public void play();

    // asks interface to go to next song
    public void next();

    // asks interface to go to previous song
    public void previous();

    // asks interface to go to given track num
    public void goToTrack(int index);

    // asks interface to show new progress (0.0 to 1.0)
    public void seekToProgress(long progress);

}
