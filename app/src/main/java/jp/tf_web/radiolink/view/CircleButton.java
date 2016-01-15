package jp.tf_web.radiolink.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageButton;

/** 丸い イメージボタン
 *
 * Created by furukawanobuyuki on 2016/01/13.
 */
public class CircleButton extends ImageButton {
    private static String TAG = "CircleButton";

    private Path pathCircle = new Path();

    public CircleButton(Context context) {
        super(context);
    }

    public CircleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        Log.d(TAG, "onSizeChanged w:"+w+" h:"+h+" oldw:"+oldw+" oldh:"+oldh);
        // 丸いパス
        pathCircle.addCircle(w/2, h/2, w/2, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //パスに沿って切り取り
        canvas.clipPath(pathCircle);
        super.onDraw(canvas);
    }
}
