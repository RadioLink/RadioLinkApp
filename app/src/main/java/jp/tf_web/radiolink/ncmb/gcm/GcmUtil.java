package jp.tf_web.radiolink.ncmb.gcm;

import com.nifty.cloud.mb.core.DoneCallback;
import com.nifty.cloud.mb.core.FindCallback;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBInstallation;
import com.nifty.cloud.mb.core.NCMBQuery;

import java.util.List;

import jp.tf_web.radiolink.Config;

/** GCM 関連の処理を実装するクラス
 *
 * Created by furukawanobuyuki on 2016/01/14.
 */
public class GcmUtil {
    private static String TAG = "GcmUtil";

    private static GcmUtil ourInstance = new GcmUtil();

    public static GcmUtil getInstance() {
        return ourInstance;
    }

    private GcmUtil() {
    }

    /** GCM デバイスIDを取得してサーバに登録
     *
     * @param listener
     */
    public void registration(final GcmUtilRegistrationListener listener){
        //端末情報を扱うNCMBInstallationのインスタンスを作成
        final NCMBInstallation installation = NCMBInstallation.getCurrentInstallation();

        //GCMからRegistrationIdを取得
        installation.getRegistrationIdInBackground(Config.GOOGLE_CLOUD_MESSAGING_PROJECT_NUMBER, new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e == null) {
                    //端末情報をデータストアに登録
                    installation.saveInBackground(new DoneCallback() {
                        @Override
                        public void done(NCMBException saveErr) {

                            //registration IDの重複エラーをチェックする
                            if (saveErr != null) {
                                //保存失敗
                                listener.error(saveErr);
                                return;
                            }

                            //端末情報登録に成功
                            listener.success();
                        }
                    });
                }
                else {
                    //RegistrationId取得時のエラー処理
                    listener.error(e);
                }
            }
        });
    }
}
