package jp.tf_web.radiolink.bluetooth;

import android.content.Context;
import android.content.Intent;

/** MEDIA_BUTTON を押された事を受け取るリスナー
 *
 * Created by furukawanobuyuki on 2015/12/28.
 */
public interface MediaButtonReceiverListener {
    /** MEDIA_BUTTON ボタンが押された事を通知
     *
     */
    void onClickMediaButton();
}