package jp.tf_web.radiolink.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import jp.tf_web.radiolink.HomeActivity;

/** MEDIA_BUTTON のイベントを受け取る
 *
 * Created by furukawanobuyuki on 2015/12/28.
 */
public class BluetoothControlReceiver extends BroadcastReceiver {
    private static String TAG = "BluetoothControlReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive()");
        String action = intent.getAction();
        if(action.equals(Intent.ACTION_MEDIA_BUTTON)){
            Log.d(TAG, " action:"+action);
            //Activityに通知するためにブロードキャスト等をする
            MediaButtonReceiver.onClickMediaButton(context.getApplicationContext());
      }
    }
}
