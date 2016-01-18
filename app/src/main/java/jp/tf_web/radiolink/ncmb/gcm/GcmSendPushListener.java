package jp.tf_web.radiolink.ncmb.gcm;

import com.nifty.cloud.mb.core.NCMBException;

/** プッシュ送信結果のリスナー
 *
 * Created by furukawanobuyuki on 2016/01/18.
 */
public interface GcmSendPushListener {
    /** 成功
     *
     */
    void success();

    /** 失敗
     *
     * @param e
     */
    void error(NCMBException e);
}
