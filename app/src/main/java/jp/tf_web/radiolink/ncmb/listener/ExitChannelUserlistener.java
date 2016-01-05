package jp.tf_web.radiolink.ncmb.listener;

import com.nifty.cloud.mb.core.NCMBException;

import jp.tf_web.radiolink.ncmb.db.Channel;

/** チャンネルから 抜ける
 *
 * Created by furukawanobuyuki on 2016/01/06.
 */
public interface ExitChannelUserlistener {
    /** 成功
     *
     */
    void success();

    /** エラー
     *
     * @param e
     */
    void error(NCMBException e);
}
