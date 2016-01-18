package jp.tf_web.radiolink.ncmb.gcm;

import com.nifty.cloud.mb.core.NCMBException;

import jp.tf_web.radiolink.ncmb.db.User;

/**
 * Created by furukawanobuyuki on 2016/01/14.
 */
public interface GcmUtilRegistrationListener {
    /** 成功
     *
     */
    void success(final User user);

    /** 失敗
     *
     * @param saveErr
     */
    void error(NCMBException saveErr);
}
