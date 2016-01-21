package jp.tf_web.radiolink.qrcode;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;

import jp.tf_web.radiolink.QRCodeActivity;

/**QRCode 関係の処理
 *
 * Created by furukawanobuyuki on 2016/01/07.
 */
public class QRCodeUtil {

    /** QRCodeを作成する
     *
     * @param contents
     * @param size
     * @return
     * @throws WriterException
     */
    public static Bitmap createQRCodeByZxing(String contents,int size) throws WriterException {
        //QRコードをエンコードするクラス
        QRCodeWriter writer = new QRCodeWriter();

        //異なる型の値を入れるためgenericは使えない
        Hashtable encodeHint = new Hashtable();

        encodeHint.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        encodeHint.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

        //QRコードのbitmap画像を作成
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.argb(255, 255, 255, 255));

        BitMatrix qrCodeData = writer.encode(contents, BarcodeFormat.QR_CODE, size, size, encodeHint);
        for (int x = 0; x < qrCodeData.getWidth(); x++) {
            for (int y = 0; y < qrCodeData.getHeight(); y++) {
                if (qrCodeData.get(x, y) == true) {
                    //0はBlack
                    bitmap.setPixel(x, y, Color.argb(255, 0, 0, 0));
                } else {
                    //-1はWhite
                    bitmap.setPixel(x, y, Color.argb(255, 255, 255, 255));
                }
            }
        }

        return bitmap;
    }

    /** QRコードキャプチャーの為の画面を開く
     *
     * @param activity
     */
    public static void scanQRCode(Activity activity){
        IntentIntegrator integrator = new IntentIntegrator(activity);
        integrator.setCaptureActivity(QRCodeActivity.class);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    /** QRコードキャプチャーの為の画面を開く
     *
     * @param fragment
     */
    public static void scanQRCode(Fragment fragment){
        IntentIntegrator integrator = new FragmentIntentIntegrator(fragment);
        integrator.setCaptureActivity(QRCodeActivity.class);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    /** QRコードキャプチャ画面からの結果受け取り
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @param listener
     * @return
     */
    public static boolean onActivityResult(int requestCode, int resultCode, Intent data,ScanQRCodeResultListener listener) {
        boolean flg = false;
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            listener.success(scanResult);
            flg = true;
        }
        return flg;
    }
}
