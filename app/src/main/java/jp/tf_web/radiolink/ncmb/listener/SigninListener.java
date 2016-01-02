package jp.tf_web.radiolink.ncmb.listener;

import com.nifty.cloud.mb.core.NCMBException;
import jp.tf_web.radiolink.ncmb.db.User;

/** 新規ユーザー登録のリスナー
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public interface SigninListener {
    /** 成功
     *
     * @param user
     */
    void success(User user);

    /** 失敗
     *
     * @param e
     */
    void error(NCMBException e);
}