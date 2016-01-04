package jp.tf_web.radiolink.ncmb.db;

import android.util.Log;

import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBFile;
import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** チャンネル
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public class Channel implements NCMBObjectInterface {
    private static String TAG = "Channel";

    //オブジェクト名
    public static String OBJ_NAME = "Channel";

    //キー名 ユーザー
    public static String KEY_USER = "user";

    //キー名 チャンネルコード
    public static String KEY_CHANNEL_CODE = "channelCode";

    //キー名 チャンネルユーザー一覧
    public static String KEY_CHANNEL_CHANNEL_USER_LIST = "channelUserList";

    private NCMBObject obj;

    //チャンネル作成ユーザー
    private User user;

    //チャンネルコード
    private String channelCode;

    //チャンネルアイコン
    private NCMBFile icon;

    //チャンネルユーザー一覧
    private List<ChannelUser> channelUserList;

    /** コンストラクタ
     *
     * @param channelCode チャンネルコード
     */
    public Channel(final User user,final String channelCode){
        this.user = user;
        this.channelCode = channelCode;
    }

    /** コンストラクタ
     *
     * @param src
     */
    public Channel(final JSONObject src){
        try {
            String objectId = src.getString("objectId");
            obj = new NCMBObject(OBJ_NAME);
            obj.setObjectId(objectId);
            obj.fetch();

            user = new User(obj.getJSONObject(KEY_USER));
            channelCode = obj.getString(KEY_CHANNEL_CODE);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NCMBException e) {
            e.printStackTrace();
        }
    }

    /** コンストラクタ
     *
     * @param src
     */
    public Channel(final NCMBObject src){
        obj = new NCMBObject(OBJ_NAME);
        obj.setObjectId(src.getObjectId());
        try {
            obj.fetch();
        } catch (NCMBException e) {
            e.printStackTrace();
        }
        user = new User(src.getJSONObject(KEY_USER));
        channelCode = src.getString(KEY_CHANNEL_CODE);
    }

    /** コンストラクタ
     *
     * @param src
     */
    public Channel(final Channel src){
        try {
            obj = src.toNCMBObject();
            obj.setObjectId(src.toNCMBObject().getObjectId());
            obj.fetch();
        } catch (NCMBException e) {
            e.printStackTrace();
            Log.e(TAG, "error " + e);
        }
        user = new User(src.user);
        channelCode = src.getChannelCode();
    }

    /** ユーザー取得
     *
     * @return ユーザー
     */
    public User getUser(){
        return this.user;
    }

    /** チャンネルコード取得
     *
     * @return チャンネルコード
     */
    public String getChannelCode(){
        return this.channelCode;
    }

    /** チャンネルにアイコンを設定
     *
     * @param icon
     */
    public void setIcon(NCMBFile icon){
        this.icon = icon;
    }

    /** チャンネルにアイコンを取得
     *
     * @return
     */
    public byte[] getIcon(){
        if(this.icon == null){
            return null;
        }
        return this.icon.getFileData();
    }

    /** チャンネルユーザーを追加
     *
     * @param channelUser
     */
    public void addChannelUser(final ChannelUser channelUser){
        if(this.channelUserList == null){
            this.channelUserList = new ArrayList<ChannelUser>();
        }
        String objectId = channelUser.getUser().toNCMBObject().getObjectId();
        //自分がチャンネルに登録済みの場合更新する
        boolean flg = true;
        for(ChannelUser cu:this.channelUserList){
            String cuObjectId = cu.getUser().toNCMBObject().getObjectId();
            if(cuObjectId.equals(objectId)){
                //更新
                cu.copy(channelUser);
                flg = false;
                break;
            }
        }
        if(flg == true) {
            //新規追加
            this.channelUserList.add(channelUser);
        }
    }

    public void deleteChannelUser(final ChannelUser channelUser){
        if(this.channelUserList == null){
            return;
        }
        this.channelUserList.remove(channelUser);
    }

    /** チャンネルユーザーを設定
     *
     * @param channelUserList
     */
    public void setChannelUserList(List<ChannelUser> channelUserList){
        this.channelUserList = channelUserList;
    }

    /** チャンネルユーザー一覧を取得
     *
     * @return
     */
    public List<ChannelUser> getChannelUserList(){
        return this.channelUserList;
    }

    /** 保存するために NCMBObject に変換
     *
     * @return
     */
    @Override
    public NCMBObject toNCMBObject(){
        if(obj == null) obj = new NCMBObject(OBJ_NAME);

        obj.put(KEY_USER, (NCMBUser) user.toNCMBObject());
        obj.put(KEY_CHANNEL_CODE, channelCode);
        return obj;
    }
}
