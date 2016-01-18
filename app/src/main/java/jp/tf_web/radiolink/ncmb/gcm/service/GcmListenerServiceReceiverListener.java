package jp.tf_web.radiolink.ncmb.gcm.service;

import android.content.Context;
import android.content.Intent;

/** GCMからのメッセージを受信 リスナー
 *
 * Created by furukawanobuyuki on 2016/01/19.
 */
public interface GcmListenerServiceReceiverListener {
    /** チャンネル更新 通知を受信
     *
     */
    void onUpdateChannel();
}
