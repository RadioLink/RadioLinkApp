package jp.tf_web.radiolink.ncmb.listener;

import com.nifty.cloud.mb.core.NCMBException;

import jp.tf_web.radiolink.ncmb.db.Channel;

/** チャンネル作成
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public interface CreateChannelListener {
    /**  成功
     *
     * @param channel
     */
    void success(Channel channel);

    /** 失敗
     *
     * @param e
     */
    void error(NCMBException e);
}
