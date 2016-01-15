package jp.tf_web.radiolink.ncmb.db.listener;

import com.nifty.cloud.mb.core.NCMBException;

import jp.tf_web.radiolink.ncmb.db.User;

/** ログイン処理のリスナー
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public interface LoginListener {
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
