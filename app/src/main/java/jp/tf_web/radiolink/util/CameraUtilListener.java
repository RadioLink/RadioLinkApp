package jp.tf_web.radiolink.util;

import android.graphics.Bitmap;

/** カメラ,ライブラリからの写真取得
 *
 * Created by furukawanobuyuki on 2016/01/13.
 */
public interface CameraUtilListener {
    /** 写真取得に成功した時
     *
     * @param bitmap
     */
    void onImage(Bitmap bitmap);
}
