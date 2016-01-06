package jp.tf_web.radiolink.qrcode;

import com.google.zxing.integration.android.IntentResult;

/** QRコードリスナー
 *
 * Created by furukawanobuyuki on 2016/01/07.
 */
public interface ScanQRCodeResultListener {
    /** スキャン成功
     *
     * @param scanResult
     */
    void success(IntentResult scanResult);
}
