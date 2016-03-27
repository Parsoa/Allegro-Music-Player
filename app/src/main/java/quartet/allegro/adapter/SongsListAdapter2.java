package quartet.allegro.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.SectionIndexer;
import android.widget.TextView;

import quartet.allegro.AllegroActivity;
import quartet.allegro.database.AlbumData;
import quartet.allegro.database.Track;
import quartet.allegro.misc.Persianize;
import quartet.allegro.R;
import quartet.allegro.UiThreadHolder;

import quartet.allegro.database.DataCenter;
import quartet.allegro.database.TrackData;
import quartet.allegro.ui.menu.MenuItemCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static quartet.allegro.AllegroActivity.log;

/**
 * Created by akbar on 5/31/15.
 */
public class SongsListAdapter2 extends BaseAdapter implements SectionIndexer {

    private static final String DEBUG_TAG = "SONGS_ADAPTER" ;

    private final AllegroActivity activity;
    private LayoutInflater inflater;
    private DataCenter dataCenter;
    UiThreadHolder uiThreadHolder;

    private View.OnClickListener moreButtonListener;
    private List<String> ctxMenuItems;

    Map<String, Integer> mapIndex;
    String[] sections;

    // ====================================================================== //

    public SongsListAdapter2(AllegroActivity activity){

        this.activity = activity;
        this.uiThreadHolder = (UiThreadHolder) activity;

        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.dataCenter = ((AllegroActivity)activity).getDataCenter();

        String[] ctxMenuArr = activity.getResources().getStringArray(R.array.songs_context_menu);
        this.ctxMenuItems = Arrays.asList(ctxMenuArr);

        moreButtonListener = moreButtonDefaultListener;
    }

    public void setMoreButtonListener(View.OnClickListener moreButtonListener) {
        this.moreButtonListener = moreButtonListener;
    }

    // ====================================================================== //

    public void prepareIndexing(){

        mapIndex = new LinkedHashMap<String, Integer>();

        for (int i = 0; i < getCount() ; i++) {
            String name = ((TrackData)getItem(i)).getDisplayName() ;
            String ch = name.substring(0, 1);
            ch = ch.toUpperCase(Locale.US);
            if (!mapIndex.containsKey(ch)) {
                mapIndex.put(ch, i);
            }
        }

        Set<String> sectionLetters = mapIndex.keySet();
        ArrayList<String> sectionList = new ArrayList<String>(sectionLetters) ;

        sections = new String[sectionList.size()] ;
        sectionList.toArray(sections);
    }

    @Override
    public int getCount() {
        return activity.getSongList().size();
    }

    @Override
    public Object getItem(int position) {
        return activity.getSongList().get(position);
    }

    @Override
    public long getItemId(int position) {
        return activity.getSongList().get(position).getId();
    }

    @Override
    public Object[] getSections() {
        return sections ;
    }

    @Override
    public int getPositionForSection(int section) {
        return mapIndex.get(sections[section]) ;
    }

    @Override
    public int getSectionForPosition(int position) {
        String tmp = ((TrackData)getItem(position)).getDisplayName().substring(0 , 1) ;
        Iterator<String> it = mapIndex.keySet().iterator() ;
        int i = 0 ;
        while(it.hasNext()){
            if(it.next().equals(tmp)){
                break ;
            }
            i ++ ;
        }
        return i ;
    }

    // ====================================================================== //

    class ViewHolder {

        Button moreButton;
        TextView bigText;
        TextView smallText;

    }

    // ====================================================================== //

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final TrackData track = activity.getSongList().get(position);

        if (convertView == null) {

            convertView = inflater.inflate(R.layout.song_list_item_main, null);
            ViewHolder holder = new ViewHolder();

            holder.moreButton = (Button) convertView.findViewById(R.id.more_button);
            holder.bigText = (TextView) convertView.findViewById(R.id.big_text);
            holder.smallText = (TextView) convertView.findViewById(R.id.small_text);

            //holder.moreButton.setOnClickListener(moreButtonListener);

            holder.moreButton.setOnClickListener(new View.OnClickListener() {
                final TrackData data = track ;
                @Override
                public void onClick(View v) {
                    activity.showSongOptionsPopUp(v,  track);
                }
            });

            convertView.setTag(holder);
        }

        String name = track.getDisplayName();
        int duration = (int) (track.getDuration() / 1000);
        String artistName = track.getArtist().getName();
        String albumName = track.getAlbum().getTitle();

        ViewHolder holder = (ViewHolder) convertView.getTag();

        holder.moreButton.setTag(track);

        int hours = duration / 3600;
        int minutes = (duration % 3600) / 60;
        int seconds = duration % 60;

        StringBuilder sb = new StringBuilder();

        String M ;
        String S ;
        String H ;

        if (hours != 0) {
            H = Persianize.persianizeNumber(hours);
            H += ":" ;
            if(minutes < 10){
                M = Persianize.persianizeNumber(0) +
                        Persianize.persianizeNumber(minutes) ;
            } else {
                M = Persianize.persianizeNumber(minutes) ;
            }
            M += ":" ;

            if(seconds < 10) {
              S = Persianize.persianizeNumber(0) +
                      Persianize.persianizeNumber(seconds) ;
            } else {
                S = Persianize.persianizeNumber(seconds) ;
            }

        } else {
            H = "" ;
            M = Persianize.persianizeNumber(minutes) ;
            M += ":" ;
            if(seconds < 10) {
                S = Persianize.persianizeNumber(0) +
                        Persianize.persianizeNumber(seconds) ;
            } else {
                S = Persianize.persianizeNumber(seconds) ;
            }

        }

        sb.append(H) ;
        sb.append(M) ;
        sb.append(S) ;
        sb.append(" | ");

        sb.append(artistName);
        sb.append(" - ");
        sb.append(albumName);

        holder.smallText.setText(sb.toString());
        holder.bigText.setText(name);

        return convertView;

    }

    // ====================================================================== //

    TrackData latestTrackClicked = null;

    View.OnClickListener moreButtonDefaultListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            latestTrackClicked = (TrackData) v.getTag();
            log("XXXXX item click x,y ", v.getX(), v.getY());
            //activity.showContextMenu(ctxMenuItems, itemCallback);
        }
    };

    private MenuItemCallback itemCallback = new MenuItemCallback() {
        @Override
        public void onItemSelected(int position) {
            if (position == 0) {
                activity.songClicked(latestTrackClicked);
            } else if (position == 1) {
                activity.showAddToPlaylistDialog(latestTrackClicked);
            }
        }
    };

    // ====================================================================== //

    public List<TrackData> getAllSongsList(){
        return activity.getSongList();
    }

    // ====================================================================== //

}
