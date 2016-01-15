package jp.tf_web.radiolink.ncmb.db.listener;

import com.nifty.cloud.mb.core.NCMBException;

import jp.tf_web.radiolink.ncmb.db.Channel;

/** チャンネルアイコン設定
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public interface SetChannelIconImageListener {
    /**  成功
     *
     */
    void success(Channel channel);

    /** 失敗
     *
     * @param e
     */
    void error(NCMBException e);
}
