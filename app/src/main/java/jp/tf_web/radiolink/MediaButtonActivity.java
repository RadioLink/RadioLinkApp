package jp.tf_web.radiolink;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import jp.tf_web.radiolink.bluetooth.MediaButtonReceiver;

/**
 * Created by furukawanobuyuki on 2016/01/07.
 */
public class MediaButtonActivity extends Activity {
    private static String TAG = "MediaButtonActivity";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
    }

    /** アプリを再起動した等
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent()");
        //ヘッドセットが有効だった場合はボタンを押されたとして通知
        MediaButtonReceiver.onClickMediaButton(getApplicationContext());
    }
}
