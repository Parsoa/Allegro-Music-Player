package quartet.allegro.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquareImageView extends ImageView {

    private boolean perWidth;

    private void __init__(AttributeSet attrs){
        this.perWidth = attrs.getAttributeBooleanValue("app", "perWidth", true);
    }

    public SquareImageView(Context context) {
        super(context);
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        __init__(attrs);
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        __init__(attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (perWidth){
            int width = getMeasuredWidth();
            setMeasuredDimension(width, width);
        } else {
            int height = getMeasuredHeight();
            setMeasuredDimension(height, height);
        }

    }

}
