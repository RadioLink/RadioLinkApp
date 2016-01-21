package jp.tf_web.radiolink.net.connectivity;

/** ネットワーク接続状態の監視リスナー
 *
 * Created by furukawanobuyuki on 2016/01/21.
 */
public interface ConnectivityReceiverListener {

    /** 接続状態の通知
     *
     * @param type
     * @param connected
     */
    void onConnectedChanged(int type,boolean connected);
}
