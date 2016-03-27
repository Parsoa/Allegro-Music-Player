package quartet.allegro.ui.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.gc.materialdesign.views.ButtonFloat;

import quartet.allegro.AllegroActivity;
import quartet.allegro.R ;
import quartet.allegro.misc.Persianize;

public class CircularTimePickerFragment extends DialogFragment {

    private CircularSeekBar circularSeekBar ;
    private TextView time ;
    private int currentProgress = 0 ;
    private ButtonFloat buttonFloat ;

    private static final String DEBUT_TAG = "TIME_DIALOG" ;

    public static CircularTimePickerFragment newInstance(){
        return new CircularTimePickerFragment() ;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ((AllegroActivity)getActivity()).serviceConnectionRequestSetTimer(currentProgress);
        super.onCancel(dialog);
    }

    @Override
    public void onResume() {
        super.onResume();

        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
        float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 340 , getResources().getDisplayMetrics());

        Window window = getDialog().getWindow();
        window.setLayout((int)width , (int)height);
        window.setGravity(Gravity.CENTER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);

        View root = inflater.inflate(R.layout.circular_time_picker_fragment, container,
                false);
        circularSeekBar = (CircularSeekBar)root.findViewById(R.id.circular_time_picker) ;
        circularSeekBar.setProgress(0);
        circularSeekBar.setMax(120);

        time = (TextView)root.findViewById(R.id.time_text_view) ;

        buttonFloat = (ButtonFloat)root.findViewById(R.id.done_button_float) ;

        buttonFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AllegroActivity) getActivity()).serviceConnectionRequestSetTimer(currentProgress);
                getDialog().dismiss();
            }
        });

        circularSeekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
                time.setText(Persianize.persianizeNumber(progress) + "دقیقه");
                currentProgress = progress;
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });

        return root ;
    }

}
