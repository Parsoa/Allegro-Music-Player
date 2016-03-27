package quartet.allegro.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.mobsandgeeks.ui.TypefaceTextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import quartet.allegro.AllegroActivity;
import quartet.allegro.misc.Persianize;
import quartet.allegro.R;
import quartet.allegro.UiThreadHolder;

import quartet.allegro.database.AlbumData;
import quartet.allegro.database.DataCenter;
import quartet.allegro.ui.ArtworkCache;

public class ClassicAlbumListAdapter extends BaseAdapter implements SectionIndexer {

    private static final String DEBUG_TAG = "ALBUM_ADAPTER" ;

    private ListView list;
    DataCenter dataCenter;
    Context ctx;
    LayoutInflater inflater;
    UiThreadHolder uiThreadHolder;

    String _track;
    AllegroActivity activity;

    private int firstVisibleItem;
    private int lastVisibleItem;

    Map<String, Integer> mapIndex;
    String[] sections;

    private ArtworkCache artworkCache;

    // =================================================================== //

    private void __init__(){

        inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        _track = ctx.getResources().getString(R.string.track);

        artworkCache = ArtworkCache.getInstance(activity);
        artworkCache.startThread();
    }

    public ClassicAlbumListAdapter(AllegroActivity activity) {

        this.uiThreadHolder = activity;
        this.ctx = activity;
        this.activity = activity;
        this.dataCenter = activity.getDataCenter();

        __init__();

    }

    public void prepareIndexing(){

        mapIndex = new LinkedHashMap<String, Integer>();

        for (int i = 0; i < getCount() ; i++) {
            String name = ((AlbumData)getItem(i)).getTitle() ;
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
    public Object[] getSections() {
        return sections ;
    }

    @Override
    public int getPositionForSection(int section) {
        return mapIndex.get(sections[section]) ;
    }

    @Override
    public int getSectionForPosition(int position) {
        String tmp = ((AlbumData)getItem(position)).getTitle().substring(0 , 1) ;
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

    class ViewHolder {
        ImageView artworkContainer;
        TypefaceTextView smallTextLeft ;
        TypefaceTextView smallTextRight ;
        TextView bigText;
    }

    @Override
    public int getCount() {
        return activity.getAlbumsList().size();
    }

    @Override
    public Object getItem(int position) {
        return activity.getAlbumsList().get(position);
    }

    @Override
    public long getItemId(int position) {
        return activity.getAlbumsList().get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final AlbumData album = activity.getAlbumsList().get(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.classic_list_item, null);
            Button moreButton = (Button) convertView.findViewById(R.id.more_button);
            moreButton.setTag(album);
            moreButton.setOnClickListener(new View.OnClickListener() {
                final AlbumData data = album ;
                @Override
                public void onClick(View v) {
                    activity.showAlbumOptionsPopUp(v , data);
                }
            });

            ViewHolder holder = new ViewHolder();
            holder.artworkContainer = (ImageView) convertView.findViewById(R.id.artwork_container);
            holder.bigText = (TextView) convertView.findViewById(R.id.big_text);
            holder.smallTextLeft = (TypefaceTextView) convertView.findViewById(R.id.small_text_left);
            holder.smallTextRight = (TypefaceTextView) convertView.findViewById(R.id.small_text_right);
            convertView.setTag(holder);
        }

        String text = album.getTitle();
        String caption = album.getArtist().getName() ;

        ViewHolder holder = (ViewHolder) convertView.getTag();

        holder.bigText.setText(text);
        holder.smallTextLeft.setText(caption);

        caption = Persianize.persianizeNumber(album.getNumberOfSongs()) + " آهنگ" ;
        holder.smallTextRight.setText(caption);

        artworkCache.loadAlbumArt(album, holder.artworkContainer);

        return convertView;
    }

    // ======================= artwork load ======================= //

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

}
