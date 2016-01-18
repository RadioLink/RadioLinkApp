package jp.tf_web.radiolink.ncmb.gcm;

import android.content.Context;
import android.util.Log;

import com.nifty.cloud.mb.core.DoneCallback;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBInstallation;
import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBPush;
import com.nifty.cloud.mb.core.NCMBQuery;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import jp.tf_web.radiolink.Config;
import jp.tf_web.radiolink.R;
import jp.tf_web.radiolink.ncmb.db.Channel;
import jp.tf_web.radiolink.ncmb.db.ChannelUser;
import jp.tf_web.radiolink.ncmb.db.NCMBUtil;
import jp.tf_web.radiolink.ncmb.db.User;
import jp.tf_web.radiolink.ncmb.db.listener.GetChannelUserListListener;
import jp.tf_web.radiolink.ncmb.gcm.service.GcmListenerService;

/** GCM 関連の処理を実装するクラス
 *
 * Created by furukawanobuyuki on 2016/01/14.
 */
public class GcmUtil {
    private static final String TAG = "GcmUtil";

    //ユーザー情報
    private static final String KEY_USER = "user";

    //アクション
    public static final String KEY_ACTION = "action";

    //任意JSONパラメータ
    public static final String KEY_JSON_DATA = "com.nifty.Data";

    private static GcmUtil ourInstance = new GcmUtil();

    public static GcmUtil getInstance() {
        return ourInstance;
    }

    private GcmUtil() {
    }

    /** GCM デバイスIDを取得してサーバに登録
     *
     * @param user
     * @param listener
     */
    public void registration(final User user,final GcmUtilRegistrationListener listener){
        //端末情報を扱うNCMBInstallationのインスタンスを作成
        final NCMBInstallation installation = NCMBInstallation.getCurrentInstallation();

        //GCMからRegistrationIdを取得
        installation.getRegistrationIdInBackground(Config.GOOGLE_CLOUD_MESSAGING_PROJECT_NUMBER, new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e == null) {
                    //端末情報をデータストアに登録
                    installation.put(KEY_USER, user.toNCMBObject());
                    //端末情報をデータストアに登録
                    installation.saveInBackground(new DoneCallback() {
                        @Override
                        public void done(NCMBException saveErr) {

                            //TODO: registration IDの重複エラーをチェックする
                            if (saveErr != null) {
                                //保存失敗
                                listener.error(saveErr);
                                return;
                            }

                            //端末情報登録に成功
                            listener.success(user);
                        }
                    });
                } else {
                    //RegistrationId取得時のエラー処理
                    listener.error(e);
                }
            }
        });
    }


    /** プッシュ通知を送る
     *
     * @param users 対象ユーザーリスト
     * @param action
     * @param title
     * @param message
     * @param listener
     * @throws JSONException
     */
    public void sendPush(final List<User> users,final String action,final String title,final String message,final GcmSendPushListener listener) throws JSONException {
        Log.d(TAG, "sendPush");

        if(users.size() == 0){
            //以下の処理をしない
            Log.d(TAG, "users size 0");
            listener.success();
            return;
        }

        //配信対象を検索するクエリー OR検索
        List<NCMBQuery<NCMBInstallation>> subQueryList = new ArrayList<>();
        for(NCMBObject u:toNCMBUserList(users)) {
            NCMBQuery<NCMBInstallation> subQuery = new NCMBQuery<>("installation");
            subQuery.whereEqualTo(KEY_USER+".objectId", u.getObjectId());
            subQueryList.add(subQuery);
        }

        NCMBQuery query = new NCMBQuery<>("installation");
        query.or(subQueryList);
        query.setIncludeKey(KEY_USER);

        NCMBPush push = new NCMBPush();
        push.setSearchCondition(query);
        push.setAction(action);
        push.setTitle(title);
        push.setMessage(message);
        push.setTarget(new JSONArray("[android]"));
        push.setDialog(true);
        //push.setUserSettingValue(new JSONObject("{\"key1\":\"value1\"}"));
        push.sendInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e != null) {
                    // エラー処理
                    listener.error(e);
                } else {
                    // プッシュ通知登録後の操作
                    listener.success();
                }
            }
        });
    }

    /** ユーザー一覧を NCMBUser 一覧に変換
     *
     * @param users
     * @return
     */
    private List<NCMBObject> toNCMBUserList(final List<User> users){
        List<NCMBObject> result = new ArrayList<NCMBObject>();
        for(User u:users){
            result.add(u.toNCMBObject());
        }
        return result;
    }

    /** チャンネルアップデート通知
     *
     * @param channel
     * @param listener
     */
    public void channelUpdateSendPush(final Context context,final Channel channel,final GcmSendPushListener listener){

        try {

            final NCMBUtil ncmbUtil = NCMBUtil.getInstance();
            //カレントユーザーを取得
            final User currentUser = ncmbUtil.getCurrentUser();

            //チャンネルユーザーを取得する
            ncmbUtil.getChannelUserList(channel, new GetChannelUserListListener() {
                @Override
                public void success(Channel channel) {

                    //カレントユーザー以外の チャンネルユーザー一覧を作る
                    List<User> users = new ArrayList<User>();
                    for(ChannelUser cu: channel.getChannelUserList()){
                        if((currentUser.getObjectId().equals(cu.getUser().getObjectId())) == false){
                            users.add( cu.getUser() );
                        }
                    }

                    Log.d(TAG,"users size:"+users.size());
                    if(users.size() == 0) {
                        //通知対象が無かった場合 成功扱いとしておく
                        listener.success();
                        return;
                    }

                    //チャンネルアップデートをプッシュ通知する
                    try {
                        String title = context.getString(R.string.gcm_title_channel_update);
                        String message = context.getString(R.string.gcm_message_channel_update);
                        sendPush(users, GcmListenerService.CMD_CHANNEL_UPDATE, title, message, listener);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        listener.error(new NCMBException(e));
                    }
                }

                @Override
                public void error(NCMBException e) {
                    listener.error(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
