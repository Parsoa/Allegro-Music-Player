package quartet.allegro.database;

/**
 * Created by akbar on 6/19/15.
 */
public class PlayListData {

    PlayList associatedPlatlist;

    public PlayListData(PlayList associatedPlatlist) {
        this.associatedPlatlist = associatedPlatlist;
    }

    public long getSugarId(){
        return associatedPlatlist.getId();
    }

    public long getAndroidId() {
        return associatedPlatlist.playlistId;
    }

    public String getName() {
        return associatedPlatlist.name;
    }

    public int getCount() {
        return associatedPlatlist.count;
    }
}
