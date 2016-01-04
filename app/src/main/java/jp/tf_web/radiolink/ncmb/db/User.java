package jp.tf_web.radiolink.ncmb.db;

import android.util.Log;

import com.nifty.cloud.mb.core.FetchCallback;
import com.nifty.cloud.mb.core.NCMBAcl;
import com.nifty.cloud.mb.core.NCMBBase;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBUser;

import org.json.JSONException;
import org.json.JSONObject;

/** ユーザー情報を保持するクラス
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public class User implements NCMBObjectInterface {
    private static String TAG = "User";

    //オブジェクト名
    public static String OBJ_NAME = "user";

    //追加パラメーターキー ニックネーム
    public static String KEY_USER_NAME = "userName";

    //追加パラメーターキー ニックネーム
    private static String KEY_NICK_NAME = "nickName";

    //ユーザー情報
    private NCMBUser user;

    //ユーザー名
    private String userName;

    //パスワード
    private String password;

    //ニックネーム
    private String nickName;

    /** コンストラクタ
     *
     * @param userName
     * @param password
     */
    public User(final String userName,final String password){
        this.user = new NCMBUser();
        this.user.setUserName(userName);
        this.user.setPassword(password);

        this.userName = userName;
        this.password = password;
    }

    public User(final JSONObject src){
        this(null,null);
        try {
            String objectId = src.getString("objectId");
            this.user.setObjectId(objectId);
            this.user.fetchInBackground(new FetchCallback() {
                @Override
                public void done(NCMBBase ncmbBase, NCMBException e) {
                    if(e == null){
                        userName = ncmbBase.getString(KEY_USER_NAME);
                        nickName = ncmbBase.getString(KEY_NICK_NAME);
                    }
                    else{
                        //エラー
                        e.printStackTrace();
                        Log.e(TAG, "error "+e);
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
    public User(final User src){
        this(src.userName, src.password);
        String objectId = src.user.getObjectId();
        //Log.d(TAG, " objectId:" + objectId);
        this.user.setObjectId(objectId);
        this.user.fetchInBackground(new FetchCallback() {
            @Override
            public void done(NCMBBase ncmbBase, NCMBException e) {
                if(e == null){
                    //成功
                }
                else{
                    e.printStackTrace();
                    Log.e(TAG,"error e:"+e);
                }
            }
        });
        this.nickName = src.nickName;
    }

    /** コンストラクタ
     *
     * @param src
     */
    public User(final NCMBUser src){
        this.user = src;
        this.user.setObjectId(src.getObjectId());
        this.userName = src.getUserName();
        if(src.containsKey(KEY_NICK_NAME)){
            this.nickName = src.getString(KEY_NICK_NAME);
        }
    }

    /** ユーザー名を取得
     *
     * @return ユーザー名
     */
    public String getUserName(){
        return this.userName;
    }

    /** パスワードを取得
     *
     * @return パスワード
     */
    public String getPassword(){
        return this.password;
    }

    /** ニックネームを設定
     *
     * @param nickName ニックネーム
     * @return
     */
    public User setNickName(final String nickName){
        this.user.put(KEY_NICK_NAME,nickName);
        this.nickName = nickName;
        return this;
    }


    /** ニックネームを取得
     *
     * @return ニックネーム
     */
    public String getNickName(){
        return this.nickName;
    }

    /** NCMBObject に変換
     *
     * @return
     */
    @Override
    public NCMBObject toNCMBObject(){

        //パーミッションを設定
        NCMBAcl acl = new NCMBAcl();
        acl.setPublicReadAccess(true);
        this.user.setAcl(acl);

        return this.user;
    }
}
