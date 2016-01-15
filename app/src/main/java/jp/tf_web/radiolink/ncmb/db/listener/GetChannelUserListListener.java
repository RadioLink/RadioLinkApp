package jp.tf_web.radiolink.ncmb.db.listener;

import com.nifty.cloud.mb.core.NCMBException;

import jp.tf_web.radiolink.ncmb.db.Channel;

/**
 * Created by furukawanobuyuki on 2016/01/03.
 */
public interface GetChannelUserListListener {
    /**  成功
     *
     * @param channel
     */
    void success(final Channel channel);

    /** 失敗
     *
     * @param e
     */
    void error(NCMBException e);
}
