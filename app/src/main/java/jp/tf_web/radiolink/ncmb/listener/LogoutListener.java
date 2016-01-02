package jp.tf_web.radiolink.ncmb.listener;

import com.nifty.cloud.mb.core.NCMBException;

import jp.tf_web.radiolink.ncmb.db.User;

/** ログアウト処理のリスナー
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public interface LogoutListener {
    /** ログアウト 成功
     *
     */
    void success();

    /** ログアウト 失敗
     *
     * @param e
     */
    void error(NCMBException e);
}
