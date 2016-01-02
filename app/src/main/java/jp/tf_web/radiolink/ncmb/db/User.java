package jp.tf_web.radiolink.ncmb.db;

import android.util.Log;

import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBUser;

/** ユーザー情報を保持するクラス
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public class User implements NCMBObjectInterface {
    private static String TAG = "User";

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

    /** コンストラクタ
     *
     * @param user
     */
    public User(NCMBUser user){
        this.user = user;

        this.userName = user.getUserName();
        if(user.containsKey(KEY_NICK_NAME)){
            this.nickName = user.getString(KEY_NICK_NAME);
        }
    }

    /** NCMBObject に変換
     *
     * @return
     */
    @Override
    public NCMBObject toNCMBObject(){
        return user;
    }
}
