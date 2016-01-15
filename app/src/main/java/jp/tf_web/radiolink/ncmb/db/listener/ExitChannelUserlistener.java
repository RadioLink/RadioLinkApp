package jp.tf_web.radiolink.ncmb.db.listener;

import com.nifty.cloud.mb.core.NCMBException;

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
