package jp.tf_web.radiolink.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/** 円 ImageView
 *
 * Created by furukawanobuyuki on 2016/01/21.
 */
public class CircleImageView extends ImageView {
    private static String TAG = "CircleImageView";

    private Path pathCircle = new Path();

    public CircleImageView(Context context) {
        super(context);
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        Log.d(TAG, "onSizeChanged w:" + w + " h:" + h + " oldw:" + oldw + " oldh:" + oldh);
        // 丸いパス
        pathCircle.addCircle(w / 2, h / 2, w / 2, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //パスに沿って切り取り
        canvas.clipPath(pathCircle);
        super.onDraw(canvas);
    }
}
