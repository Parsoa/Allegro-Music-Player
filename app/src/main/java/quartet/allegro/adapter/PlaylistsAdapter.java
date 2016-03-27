package quartet.allegro.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import quartet.allegro.AllegroActivity;

import quartet.allegro.R;

import quartet.allegro.async.PlaylistNotifyListener;
import quartet.allegro.database.PlayListData;

import java.util.ArrayList;

/**
 * Created by akbar on 6/19/15.
 */
public class PlaylistsAdapter extends BaseAdapter implements PlaylistNotifyListener{

    AllegroActivity activity;

    ArrayList<PlayListData> playListDatas;

    public PlaylistsAdapter(AllegroActivity activity) {
        this.activity = activity;
        this.playListDatas = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return playListDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return playListDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return playListDatas.get(position).getSugarId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null){
            convertView = activity.getLayoutInflater().inflate(R.layout.drawer_playlists_item, parent, false);
        }

        PlayListData data = playListDatas.get(position);
        TextView nameTextView = (TextView) convertView.findViewById(R.id.textview_name);
        nameTextView.setText(data.getName());

        convertView.setTag(data);

        return convertView;
    }

    @Override
    public void onPlaylistFound(final PlayListData pl) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                int idx = 0;
                for (PlayListData playListData:playListDatas){
                    if (pl.getName().compareTo(playListData.getName())>0)
                        break;
                    idx++;
                }

                playListDatas.add(idx, pl);
                notifyDataSetChanged();
            }
        });
    }
}
