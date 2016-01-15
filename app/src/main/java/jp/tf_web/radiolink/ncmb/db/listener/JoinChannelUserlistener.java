package jp.tf_web.radiolink.ncmb.db.listener;

import com.nifty.cloud.mb.core.NCMBException;

import jp.tf_web.radiolink.ncmb.db.Channel;

/** チャンネルに ユーザーを追加する処理のリスナー
 *
 * Created by furukawanobuyuki on 2016/01/05.
 */
public interface JoinChannelUserlistener {
    /** 成功
     *
     * @param channel
     */
    void success(Channel channel);

    /** エラー
     *
     * @param e
     */
    void error(NCMBException e);
}
