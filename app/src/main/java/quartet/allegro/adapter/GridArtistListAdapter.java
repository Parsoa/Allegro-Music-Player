package quartet.allegro.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import quartet.allegro.AllegroActivity;
import quartet.allegro.communication.Support;
import quartet.allegro.database.AlbumData;
import quartet.allegro.misc.Persianize;
import quartet.allegro.R;
import quartet.allegro.UiThreadHolder;

import quartet.allegro.database.ArtistData;
import quartet.allegro.database.DataCenter;

public class GridArtistListAdapter extends BaseAdapter implements SectionIndexer {
    
    private final AllegroActivity activity;


    // ================== Fields ================================ //

    LayoutInflater inflater;
    UiThreadHolder uiThreadHolder;
    DataCenter dataCenter;

    String _track;
    String _album;

    Map<String, Integer> mapIndex;
    String[] sections;

    // ========================================================== //

    private void __init__() {

        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        _album = activity.getResources().getString(R.string.album);
        _track = activity.getResources().getString(R.string.track);

    }

    public GridArtistListAdapter(AllegroActivity activity) {

        this.uiThreadHolder = activity;
        this.activity = activity;
        this.dataCenter = activity.getDataCenter();

        __init__();
    }

    // ========================================================== //

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ArtistData artist = activity.getArtistList().get(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.artist_grid_item, null);
            Button buttonMore = (Button) convertView.findViewById(R.id.more_button);
            buttonMore.setTag(artist);
            buttonMore.setOnClickListener(new View.OnClickListener() {
                final ArtistData data = artist ;
                @Override
                public void onClick(View v) {
                    activity.showArtistOptionsPopUp(v , data);
                }
            });
        }

        String name = artist.getName();
        int albumCount = artist.getNumberOfAlbums();
        int trackCount = artist.getNumberOfTracks();
        String address = artist.getArtworkUri();

        ImageView artworkImageView = (ImageView) convertView.findViewById(R.id.artwork_container);
        TextView bigText = (TextView) convertView.findViewById(R.id.big_text);
        TextView smallText = (TextView) convertView.findViewById(R.id.small_text);

        String caption = Persianize.persianizeNumber(albumCount) + " " +
                _album + " و " +
                Persianize.persianizeNumber(trackCount) + " " + _track;

        smallText.setText(caption);
        bigText.setText(name);

        loadArtistPhoto(name, address, artworkImageView);

        return convertView;
    }

    // ========================================================== //

    void loadArtistPhoto(String name, String uriString, ImageView view) {

        int tmp = Math.abs(name.hashCode()) ;
        tmp = tmp % Support.ArtworkColors.length ;
        TextDrawable drawable = TextDrawable.builder()
                .buildRect(name.substring(0 , 1) , activity.getResources().getColor(Support.ArtworkColors[tmp]));

        view.setImageDrawable(drawable);

    }

    // ========================================================== //

    public void prepareIndexing(){

        mapIndex = new LinkedHashMap<String, Integer>();

        for (int i = 0; i < getCount() ; i++) {
            String name = ((ArtistData)getItem(i)).getName() ;
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
        return activity.getArtistList().size();
    }

    @Override
    public Object getItem(int position) {
        return activity.getArtistList().get(position);
    }

    @Override
    public long getItemId(int position) {
        return activity.getArtistList().get(position).getId();
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
        String tmp = ((ArtistData)getItem(position)).getName().substring(0 , 1) ;
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

    // ========================================================== //

}
