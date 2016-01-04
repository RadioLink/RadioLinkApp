package jp.tf_web.radiolink.ncmb.db;

import android.util.Log;

import com.nifty.cloud.mb.core.FetchCallback;
import com.nifty.cloud.mb.core.NCMBAcl;
import com.nifty.cloud.mb.core.NCMBBase;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBFile;
import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
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
            //objectId設定後に fetchInBackground することでサーバからデータをロードできる
            obj.fetchInBackground(new FetchCallback() {
                @Override
                public void done(NCMBBase ncmbBase, NCMBException e) {
                    if(e == null) {
                        user = new User(ncmbBase.getJSONObject(KEY_USER));
                        channelCode = ncmbBase.getString(KEY_CHANNEL_CODE);
                    }
                    else{
                        e.printStackTrace();
                        Log.e(TAG,"Channel(JSONObject) fetchInBackground　error "+e);
                    }
                }
            });
        } catch (JSONException e) {
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
        obj.fetchInBackground(new FetchCallback() {
            @Override
            public void done(NCMBBase ncmbBase, NCMBException e) {
                if (e == null) {
                    user = new User(ncmbBase.getJSONObject(KEY_USER));
                } else {
                    e.printStackTrace();
                    Log.e(TAG, "Channel(NCMBObject) fetchInBackground　error " + e);
                }
            }
        });
        user = new User(src.getJSONObject(KEY_USER));
        channelCode = src.getString(KEY_CHANNEL_CODE);
    }

    /** コンストラクタ
     *
     * @param src
     */
    public Channel(final Channel src){
        obj = src.toNCMBObject();
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
        //自分がチャンネルに登録済みの場合更新する
        boolean flg = true;
        for(ChannelUser cu:this.channelUserList){
            InetSocketAddress tmp = cu.publicSocketAddress;
            String addr = tmp.getAddress().getHostAddress();
            int port = tmp.getPort();
            if((addr.equals(channelUser.publicSocketAddress.getAddress().getHostAddress()))
                &&(port == channelUser.publicSocketAddress.getPort())){
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

        /*
        //パーミッションを設定
        NCMBAcl acl = new NCMBAcl();
        acl.setPublicReadAccess(true);
        acl.setPublicWriteAccess(true);
        obj.setAcl(acl);
*/
        obj.put(KEY_USER, (NCMBUser) user.toNCMBObject());
        obj.put(KEY_CHANNEL_CODE, channelCode);
        return obj;
    }
}
