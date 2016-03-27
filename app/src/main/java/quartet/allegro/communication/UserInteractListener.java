package quartet.allegro.communication;

import quartet.allegro.database.Track;

import java.util.List;


/**
 * Created by akbar on 6/3/15.
 */
public interface UserInteractListener {

    // asks the media player to pause
    public void pause();

    // asks the media player to continue playing
    public void play();

    // asks the media player to play given item
    public void playItem(int position);

    // asks media player to seek to progress (0.0 to 1.0)
    public void seekToProgress(float progress);

    // notifies shuffle is on or off
    public void shuffleChanged(boolean on);

    // notifies repeat is on or off
    public void repeatChanged(boolean on);

    // asks the media player to play next
    public void next();

    // asks the media player to play next
    public void previous();

    // notifies the order of elements changed and also index of current
    // track in new queue
    public void notifyOrderChange(List<Track> trackList, int position);

}
