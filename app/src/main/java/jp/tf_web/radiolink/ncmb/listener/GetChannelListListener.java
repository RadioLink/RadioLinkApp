package jp.tf_web.radiolink.ncmb.listener;

import com.nifty.cloud.mb.core.NCMBException;

import java.util.List;

import jp.tf_web.radiolink.ncmb.db.Channel;

/** チャンネル一覧 取得
 * Created by furukawanobuyuki on 2016/01/02.
 */
public interface GetChannelListListener {
    /**  成功
     *
     * @param channels
     */
    void success(List<Channel> channels);

    /** 失敗
     *
     * @param e
     */
    void error(NCMBException e);
}
