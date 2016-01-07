package jp.tf_web.radiolink;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/** URLスキームで起動される 非表示 Activity
 *
 * Created by furukawanobuyuki on 2016/01/08.
 */
public class SchemeActivity extends Activity {
    private static String TAG = "SchemeActivity";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        //TODO:起動時パラメータを取得

        String action = getIntent().getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                Log.d(TAG, "uri "+uri.toString());
                //TODO: アクションをブロードキャストしてメイン画面に通知する
            }
        }

        finish();
    }
}
