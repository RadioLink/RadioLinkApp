package jp.tf_web.radiolink.ncmb.db.listener;

import com.nifty.cloud.mb.core.NCMBException;

import jp.tf_web.radiolink.ncmb.db.Channel;

/** チャンネル画像を取得
 * Created by furukawanobuyuki on 2016/01/02.
 */
public interface GetChannelIconImageListener {
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
