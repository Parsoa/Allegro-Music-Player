package quartet.allegro.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.List;

import quartet.allegro.AllegroActivity;
import quartet.allegro.R;
import quartet.allegro.communication.Support;
import quartet.allegro.database.AlbumData;
import quartet.allegro.database.TrackData;

/**
 * Created by Parsoa on 7/2/15.
 */
public class AlbumArtistSongsRecyclerAdapter extends RecyclerView.Adapter<AlbumArtistSongsRecyclerAdapter.CardViewHolder> {

    private List<TrackData> trackData ;
    int count ;
    private Activity activity ;

    private static final int TYPE_SONG = 0 ;

    public class CardViewHolder extends RecyclerView.ViewHolder
            implements View
            .OnClickListener {
        int position ;
        TextView name ;
        TextView duration ;
        TextView album ;
        ImageButton imageButton ;

        public CardViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.song_card_name) ;
            album = (TextView) itemView.findViewById(R.id.song_card_album);
            duration = (TextView) itemView.findViewById(R.id.song_card_duration) ;
            imageButton = (ImageButton) itemView.findViewById(R.id.song_card_more_button) ;
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestSongPopUp(v , position - 1);
                }
            });
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            requestFragmentTransaction(getAdapterPosition() - 1);
        }
    }

    public void requestFragmentTransaction(int position){
        ((AllegroActivity)activity).requestServiceConnectionLoadAndPlay(position , trackData);
    }

    public void requestSongPopUp(View v , int position){
        ((AllegroActivity)activity).showSongOptionsPopUp(v , trackData.get(position));
    }

    public AlbumArtistSongsRecyclerAdapter(List<TrackData> data , Activity activity) {
        this.trackData = data ;
        this.activity = activity ;
        this.count = this.trackData.size() ;
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_SONG ;
    }

    @Override
    public int getItemCount() {
        return this.count ;
    }

    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_list_item_song, parent, false);

        return new CardViewHolder(view) {
        };
    }

    @Override
    public void onBindViewHolder(CardViewHolder holder, int position) {

        holder.name.setText(trackData.get(position).getDisplayName().toString()) ;
        holder.album.setText(trackData.get(position).getAlbum().getTitle().toString());
        holder.duration.setText(produceDurationString(trackData.get(position).getDuration()).toString());
        holder.position = position - 1 ;

    }

    public String produceDurationString(long time){
        int t = (int)(time / 1000) ;
        int minutes = t / 60 ;
        int seconds = t % 60 ;
        String tmp ;
        if(seconds < 10) {
            tmp = Integer.toString(minutes) + ":0" + Integer.toString(seconds);
        } else {
            tmp = Integer.toString(minutes) + ":" + Integer.toString(seconds);
        }
        return tmp ;
    }
}
