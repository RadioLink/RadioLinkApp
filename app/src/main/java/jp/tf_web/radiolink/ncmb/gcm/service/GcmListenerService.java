package jp.tf_web.radiolink.ncmb.gcm.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.nifty.cloud.mb.core.NCMBGcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import jp.tf_web.radiolink.ncmb.gcm.GcmUtil;

/** プッシュ通知を受け取るサービス
 *
 * Created by furukawanobuyuki on 2016/01/19.
 */
public class GcmListenerService extends NCMBGcmListenerService {
    private static final String TAG = "GcmListenerService";

    public static final String ACTION_NAME = "ACTION_GCM_ON_RECEIVE";

    //プッシュに設定するアクション
    public static final String CMD_CHANNEL_UPDATE = "CMD_CHANNEL_UPDATE";

    public static final String KEY_NAME_CMD = "cmd";

    /** ブロードキャストレシーバーに通知
     *
     * @param context
     * @param cmd
     * @param params
     */
    private static void sendOnReceive(final Context context,final String cmd,final Map<String,Object> params){
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_NAME);
        broadcastIntent.putExtra(KEY_NAME_CMD, cmd);
        if(params != null){
            //TODO: パラメータが必要なアクションができたらココに実装
            for(Map.Entry<String,Object> obj:params.entrySet()){
                broadcastIntent.putExtra(obj.getKey(), obj.getValue().toString());
            }
        }
        context.sendBroadcast(broadcastIntent);
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG,"onMessageReceived from:"+from);
        //ペイロードデータの取得
        String cmd = data.getString(GcmUtil.KEY_ACTION);
        Log.d(TAG,"action:"+cmd);
        Map<String,Object> params = new HashMap<>();
        if (data.containsKey(GcmUtil.KEY_JSON_DATA)) {
            Log.d(TAG, "data " + GcmUtil.KEY_JSON_DATA);
            try {
                JSONObject json = new JSONObject(data.getString(GcmUtil.KEY_JSON_DATA));
                Iterator iter = json.keys();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    String value = json.getString(key);
                    //Log.d(TAG, key+":"+value);
                    params.put(key,value);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else{
            //未設定だった場合
            Log.d(TAG, GcmUtil.KEY_JSON_DATA+" is null");
        }

        //必要データを ブロードキャストして Activity に伝える
        sendOnReceive(getApplicationContext(), cmd, params);

        //デフォルトの通知を実行する場合はsuper.onMessageReceivedを実行する
        //super.onMessageReceived(from, data);
    }
}
