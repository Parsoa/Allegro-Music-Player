package quartet.allegro.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import quartet.allegro.R ;

/**
 * Created by Parsoa on 7/2/15.
 */
public class LoadingFragment extends Fragment {

    public static LoadingFragment newInstance(){
        return new LoadingFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.loading_fragment_layout , container , false) ;
        return root ;

    }

}
