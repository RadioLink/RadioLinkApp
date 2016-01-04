package jp.tf_web.radiolink.ncmb.listener;

import com.nifty.cloud.mb.core.NCMBException;

import java.util.List;

import jp.tf_web.radiolink.ncmb.db.Channel;
import jp.tf_web.radiolink.ncmb.db.ChannelUser;

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
