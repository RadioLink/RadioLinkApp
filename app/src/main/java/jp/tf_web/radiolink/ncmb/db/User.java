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

        //パーミッション設定
        setAcl();
    }

    public User(final JSONObject src){
        this(null, null);
        try {
            String objectId = src.getString("objectId");
            this.user.setObjectId(objectId);
            try {
                this.user.fetch();
            } catch (NCMBException e) {
                e.printStackTrace();
            }
            userName = this.user.getString(KEY_USER_NAME);
            nickName = this.user.getString(KEY_NICK_NAME);
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
        this.user.setObjectId(objectId);
        try{
            this.user.fetch();
        } catch (NCMBException e) {
            e.printStackTrace();
        }
        this.nickName = src.nickName;

        //パーミッション設定
        setAcl();
    }

    /** コンストラクタ
     *
     * @param src
     */
    public User(final NCMBUser src){
        this.user = src;
        this.user.setObjectId(src.getObjectId());
        try{
            this.user.fetch();
        } catch (NCMBException e) {
            e.printStackTrace();
        }

        this.userName = src.getUserName();
        if(src.containsKey(KEY_NICK_NAME)){
            this.nickName = src.getString(KEY_NICK_NAME);
        }
        //パーミッション設定
        setAcl();
    }

    /** ユーザーオブジェクトのID
     *
     * @return
     */
    public String getObjectId(){
        if(user == null) return null;
        return user.getObjectId();
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


    /** パーミッション設定
     *
     */
    private void setAcl(){
        NCMBAcl acl = new NCMBAcl();
        acl.setPublicReadAccess(true);
        acl.setPublicWriteAccess(true);
        this.user.setAcl(acl);
    }

    /** NCMBObject に変換
     *
     * @return
     */
    @Override
    public NCMBObject toNCMBObject(){

        //パーミッション設定
        setAcl();

        return this.user;
    }
}
