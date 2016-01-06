package jp.tf_web.radiolink.ncmb.listener;

import com.nifty.cloud.mb.core.NCMBException;

import jp.tf_web.radiolink.ncmb.db.Channel;

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