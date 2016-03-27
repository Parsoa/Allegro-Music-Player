package quartet.allegro.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import quartet.allegro.AllegroActivity;
import quartet.allegro.R;
import quartet.allegro.database.AlbumData;
import quartet.allegro.ui.ArtworkCache;
import quartet.allegro.ui.fragment.ArtistViewPagerFragment;

public class ArtistAlbumsRecyclerAdapter extends RecyclerView.Adapter<ArtistAlbumsRecyclerAdapter.CardViewHolder> {

    private static final int TYPE_ALBUM = 0 ;
    private static final int TYPE_LOADING = 1 ;
    private List<AlbumData> albumData ;
    int count ;
    public Activity context ;
    private ArtistViewPagerFragment parent ;

    public class CardViewHolder extends RecyclerView.ViewHolder
            implements View
            .OnClickListener {

        int position ;
        TextView name ;
        TextView year ;
        TextView numSongs ;
        ImageView albumArt ;
        ImageButton moreButton ;

        public CardViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.album_card_name);
            year = (TextView) itemView.findViewById(R.id.album_card_year);
            numSongs = (TextView) itemView.findViewById(R.id.album_card_num_songs) ;
            albumArt = (ImageView) itemView.findViewById(R.id.album_card_art_image) ;
            moreButton = (ImageButton) itemView.findViewById(R.id.album_card_more_button) ;

            moreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestPopUpMenu(v , position - 1);
                }
            });
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            requestAlbumFragment(getAdapterPosition() - 1);
        }
    }

    public void requestPopUpMenu(View v , int position){
        ((AllegroActivity)context).showAlbumOptionsPopUp(v , albumData.get(position));
    }

    public void requestAlbumFragment(int position){
        ((AllegroActivity)context).showAlbumInfoFragment(albumData.get(position));
        //parent.showAlbumInfoFragment(albumData.get(position));
    }

    public ArtistAlbumsRecyclerAdapter(List<AlbumData> data , Activity context , ArtistViewPagerFragment parent) {
        this.albumData = data ;
        this.context = context ;
        this.parent = parent ;
        this.count = this.albumData.size() ;
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_ALBUM ;
    }

    @Override
    public int getItemCount() {
        return this.count ;
    }

    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_list_item_album, parent, false);

        return new CardViewHolder(view) {
        };
    }

    @Override
    public void onBindViewHolder(CardViewHolder holder, int position) {

        holder.name.setText(albumData.get(position).getTitle().toString());
        holder.year.setText(Integer.toString(albumData.get(position).getYear()));
        if(albumData.get(position).getNumberOfSongs() > 1) {
            holder.numSongs.setText(Integer.toString(albumData.get(position).getNumberOfSongs()) + " Songs");
        } else {
            holder.numSongs.setText(Integer.toString(albumData.get(position).getNumberOfSongs()) + " Song");
        }
        holder.position = position ;

        ArtworkCache.getInstance(context).loadAlbumArt(albumData.get(position) ,
                holder.albumArt);

    }
}

