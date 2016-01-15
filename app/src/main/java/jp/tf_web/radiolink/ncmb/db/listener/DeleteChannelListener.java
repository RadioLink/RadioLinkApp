package jp.tf_web.radiolink.ncmb.db.listener;

import com.nifty.cloud.mb.core.NCMBException;

/** チャンネル削除
 * Created by furukawanobuyuki on 2016/01/02.
 */
public interface DeleteChannelListener {
    /**  成功
     *
     */
    void success();

    /** 失敗
     *
     * @param e
     */
    void error(NCMBException e);
}
