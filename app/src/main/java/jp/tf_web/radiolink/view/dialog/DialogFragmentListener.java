package jp.tf_web.radiolink.view.dialog;

import android.os.Bundle;

/** ダイアログのリスナー
 *
 * Created by furukawanobuyuki on 2016/01/15.
 */
public interface DialogFragmentListener {

    /** OKボタンクリック時
     *
     * @param params
     */
    void ok(Bundle params);

    /** キャンセルクリック
     *
     * @param params
     */
    void cancel(Bundle params);
}
